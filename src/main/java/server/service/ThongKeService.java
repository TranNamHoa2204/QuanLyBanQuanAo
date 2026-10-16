package server.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.persistence.EntityManager;
import common.dto.ThongKeDoanhThuDTO;
import common.dto.ThongKeDoanhThuDongDTO;
import common.dto.ThongKeDoanhThuRequest;
import common.enums.NhomThongKe;
import common.enums.VaiTro;
import common.protocol.ErrorCode;
import server.entity.TaiKhoan;
import server.persistence.JpaManager;
import server.repository.JpaThongKeDao;
import server.repository.ThongKeDao;

/** Tổng hợp doanh thu hóa đơn đã thanh toán từ các số tiền đã chốt trên chứng từ. */
public final class ThongKeService {
    private static final int MAX_SOURCE_ROWS = 100000;
    private static final int MAX_PAGE = 100000;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_DAILY_DAYS = 366;
    private static final int MAX_PRODUCT_DAYS = 366;
    private static final int MAX_MONTHS = 120;
    private static final int MAX_YEARS = 20;
    private static final BigInteger ONE_CENT = BigInteger.ONE;

    private final JpaManager jpa;
    private final AuthService auth;

    public ThongKeService(JpaManager jpa, AuthService auth) {
        if (jpa == null || auth == null) { throw new IllegalArgumentException("Statistics dependencies are required."); }
        this.jpa = jpa;
        this.auth = auth;
    }

    public ThongKeDoanhThuDTO byTime(String token, ThongKeDoanhThuRequest request) {
        validate(request, false);
        return jpa.transaction(em -> {
            authorize(em, token);
            ThongKeDao dao = new JpaThongKeDao(em);
            List<Invoice> invoices = invoices(dao.paidInvoiceTotals(from(request), until(request), MAX_SOURCE_ROWS + 1));
            ensureSourceLimit(invoices.size());
            long legacyCount = dao.countPaidWithoutPaymentTime();

            Map<String, Amounts> groups = new TreeMap<String, Amounts>();
            Amounts overall = new Amounts();
            for (Invoice invoice : invoices) {
                overall.add(invoice.amounts);
                String key = periodKey(invoice.paidAt, request.getNhomThoiGian());
                groups.computeIfAbsent(key, ignored -> new Amounts()).add(invoice.amounts);
            }
            List<ThongKeDoanhThuDongDTO> items = new ArrayList<ThongKeDoanhThuDongDTO>();
            for (Map.Entry<String, Amounts> group : groups.entrySet()) {
                ThongKeDoanhThuDongDTO row = row(group.getKey(), group.getKey(), group.getValue());
                items.add(row);
            }
            return response(request, request.getNhomThoiGian(), overall, items, legacyCount);
        });
    }

    public ThongKeDoanhThuDTO byProduct(String token, ThongKeDoanhThuRequest request) {
        validate(request, true);
        return jpa.transaction(em -> {
            authorize(em, token);
            ThongKeDao dao = new JpaThongKeDao(em);
            List<Invoice> invoices = invoices(dao.paidInvoiceTotals(from(request), until(request), MAX_SOURCE_ROWS + 1));
            ensureSourceLimit(invoices.size());
            long legacyCount = dao.countPaidWithoutPaymentTime();
            Map<String, Invoice> invoiceById = new HashMap<String, Invoice>();
            Amounts overall = new Amounts();
            for (Invoice invoice : invoices) {
                invoiceById.put(invoice.id, invoice);
                overall.add(invoice.amounts);
            }

            List<Object[]> lines = dao.paidInvoiceLines(from(request), until(request), MAX_SOURCE_ROWS + 1);
            ensureSourceLimit(lines.size());
            Map<String, Map<String, ProductSlice>> byInvoice = new HashMap<String, Map<String, ProductSlice>>();
            for (Object[] source : lines) {
                String invoiceId = (String) source[0];
                Invoice invoice = invoiceById.get(invoiceId);
                if (invoice == null) { throw inconsistent(); }
                String productId = (String) source[1];
                String snapshotName = (String) source[2];
                String currentName = (String) source[3];
                BigDecimal unitPrice = (BigDecimal) source[4];
                int quantity = ((Number) source[5]).intValue();
                if (productId == null || unitPrice == null || unitPrice.signum() < 0 || quantity <= 0) {
                    throw inconsistent();
                }
                BigInteger lineGross = toCents(unitPrice.multiply(BigDecimal.valueOf(quantity)));
                Map<String, ProductSlice> products = byInvoice.computeIfAbsent(invoiceId,
                        ignored -> new TreeMap<String, ProductSlice>());
                ProductSlice product = products.computeIfAbsent(productId, ignored -> new ProductSlice(productId));
                product.gross = product.gross.add(lineGross);
                if (snapshotName != null && !snapshotName.trim().isEmpty()) { product.snapshotName = snapshotName; }
                if (currentName != null && !currentName.trim().isEmpty()) { product.currentName = currentName; }
            }

            Map<String, ProductTotals> productTotals = new TreeMap<String, ProductTotals>();
            for (Invoice invoice : invoices) {
                Map<String, ProductSlice> products = byInvoice.get(invoice.id);
                if (products == null || products.isEmpty()) { throw inconsistent(); }
                BigInteger grossSum = BigInteger.ZERO;
                Map<String, BigInteger> grossWeights = new TreeMap<String, BigInteger>();
                for (ProductSlice product : products.values()) {
                    grossSum = grossSum.add(product.gross);
                    grossWeights.put(product.productId, product.gross);
                }
                if (!grossSum.equals(invoice.amounts.gross) || invoice.amounts.discount.compareTo(grossSum) > 0) {
                    throw inconsistent();
                }
                Map<String, BigInteger> discounts = allocate(invoice.amounts.discount, grossWeights);
                Map<String, BigInteger> netWeights = new TreeMap<String, BigInteger>();
                for (ProductSlice product : products.values()) {
                    product.discount = discounts.get(product.productId);
                    product.net = product.gross.subtract(product.discount);
                    netWeights.put(product.productId, product.net);
                }
                Map<String, BigInteger> taxes = allocate(invoice.amounts.tax, netWeights);
                for (ProductSlice product : products.values()) {
                    ProductTotals total = productTotals.computeIfAbsent(product.productId,
                            ignored -> new ProductTotals(product.productId));
                    product.tax = taxes.get(product.productId);
                    total.amounts.add(product.gross, product.discount, product.tax);
                    total.amounts.invoiceCount++;
                    // Cập nhật cả null: thiếu snapshot ở lần bán gần nhất phải kích hoạt fallback,
                    // không nên hiển thị snapshot cũ như thể nó thuộc giao dịch mới hơn.
                    total.snapshotName = product.snapshotName;
                    if (product.currentName != null) { total.currentName = product.currentName; }
                }
            }

            Amounts productOverall = new Amounts();
            List<ThongKeDoanhThuDongDTO> items = new ArrayList<ThongKeDoanhThuDongDTO>();
            for (ProductTotals product : productTotals.values()) {
                productOverall.add(product.amounts);
                String name = product.snapshotName;
                boolean currentFallback = name == null;
                if (currentFallback) { name = product.currentName; }
                if (name == null || name.trim().isEmpty()) { name = "Sản phẩm " + product.productId; }
                ThongKeDoanhThuDongDTO row = row(product.productId, name, product.amounts);
                row.setMaSP(product.productId);
                row.setTenSP(name);
                row.setTenSPHienHanhFallback(currentFallback);
                items.add(row);
            }
            if (!overall.sameAmounts(productOverall)) { throw inconsistent(); }
            Collections.sort(items, new Comparator<ThongKeDoanhThuDongDTO>() {
                @Override public int compare(ThongKeDoanhThuDongDTO left, ThongKeDoanhThuDongDTO right) {
                    int amountOrder = right.getTongKhachTra().compareTo(left.getTongKhachTra());
                    return amountOrder != 0 ? amountOrder : left.getMaSP().compareTo(right.getMaSP());
                }
            });
            return response(request, null, overall, items, legacyCount);
        });
    }

    private void authorize(EntityManager em, String token) {
        TaiKhoan account = auth.requireAuthenticated(em, token);
        if (account.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ quản lý được xem thống kê.");
        }
    }

    private static void validate(ThongKeDoanhThuRequest request, boolean productReport) {
        if (request == null || request.getTuNgay() == null || request.getDenNgay() == null
                || request.getTuNgay().isAfter(request.getDenNgay()) || request.getDenNgay().equals(LocalDate.MAX)
                || request.getPage() < 0 || request.getPage() > MAX_PAGE
                || request.getPageSize() < 1 || request.getPageSize() > MAX_PAGE_SIZE) {
            throw invalidRequest();
        }
        if (productReport) {
            long days = ChronoUnit.DAYS.between(request.getTuNgay(), request.getDenNgay()) + 1;
            if (days > MAX_PRODUCT_DAYS) { throw rangeLimit("Báo cáo sản phẩm giới hạn tối đa 366 ngày."); }
        } else {
            NhomThongKe group = request.getNhomThoiGian();
            if (group == null) { throw invalidRequest(); }
            long count;
            switch (group) {
            case NGAY:
                count = ChronoUnit.DAYS.between(request.getTuNgay(), request.getDenNgay()) + 1;
                if (count > MAX_DAILY_DAYS) { throw rangeLimit("Báo cáo theo ngày giới hạn tối đa 366 ngày."); }
                break;
            case THANG:
                count = ChronoUnit.MONTHS.between(YearMonth.from(request.getTuNgay()), YearMonth.from(request.getDenNgay())) + 1;
                if (count > MAX_MONTHS) { throw rangeLimit("Báo cáo theo tháng giới hạn tối đa 120 tháng."); }
                break;
            case NAM:
                count = (long) request.getDenNgay().getYear() - request.getTuNgay().getYear() + 1;
                if (count > MAX_YEARS) { throw rangeLimit("Báo cáo theo năm giới hạn tối đa 20 năm."); }
                break;
            default: throw invalidRequest();
            }
        }
    }

    private static ThongKeDoanhThuDTO response(ThongKeDoanhThuRequest request, NhomThongKe group,
            Amounts totals, List<ThongKeDoanhThuDongDTO> allItems, long legacyCount) {
        ThongKeDoanhThuDTO dto = new ThongKeDoanhThuDTO();
        dto.setTuNgay(request.getTuNgay());
        dto.setDenNgay(request.getDenNgay());
        dto.setNhomThoiGian(group);
        dto.setSoHoaDon(totals.invoiceCount);
        dto.setHangBanTruocGiam(fromCents(totals.gross));
        dto.setTienGiam(fromCents(totals.discount));
        dto.setTienThue(fromCents(totals.tax));
        dto.setDoanhThuSauGiam(fromCents(totals.gross.subtract(totals.discount)));
        dto.setTongKhachTra(fromCents(totals.gross.subtract(totals.discount).add(totals.tax)));
        dto.setSoHoaDonThieuThoiDiemThanhToan(legacyCount);
        dto.setCanhBaoDuLieuCu(legacyCount == 0 ? null : legacyWarning(legacyCount));
        dto.setPage(request.getPage());
        dto.setPageSize(request.getPageSize());
        dto.setTotalItems(allItems.size());
        long offsetLong = (long) request.getPage() * request.getPageSize();
        if (offsetLong < allItems.size()) {
            int offset = (int) offsetLong;
            int end = Math.min(allItems.size(), offset + request.getPageSize());
            dto.setItems(new ArrayList<ThongKeDoanhThuDongDTO>(allItems.subList(offset, end)));
        }
        return dto;
    }

    private static String legacyWarning(long count) {
        return count + " hóa đơn đã thanh toán trên toàn hệ thống thiếu thời điểm thanh toán; "
                + "đã loại khỏi báo cáo vì không thể xác định thuộc khoảng ngày.";
    }

    private static List<Invoice> invoices(List<Object[]> rows) {
        List<Invoice> result = new ArrayList<Invoice>(rows.size());
        for (Object[] row : rows) {
            if (row == null || row.length < 6 || !(row[0] instanceof String) || !(row[1] instanceof LocalDateTime)) {
                throw inconsistent();
            }
            Invoice invoice = new Invoice((String) row[0], (LocalDateTime) row[1]);
            invoice.amounts.set(toCents((BigDecimal) row[2]), toCents((BigDecimal) row[3]),
                    toCents((BigDecimal) row[4]), toCents((BigDecimal) row[5]));
            result.add(invoice);
        }
        return result;
    }

    private static void ensureSourceLimit(int size) {
        if (size > MAX_SOURCE_ROWS) {
            throw rangeLimit("Khoảng báo cáo có quá nhiều dữ liệu; hãy thu hẹp thời gian.");
        }
    }

    private static String periodKey(LocalDateTime paidAt, NhomThongKe group) {
        switch (group) {
        case NGAY: return paidAt.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        case THANG: return YearMonth.from(paidAt).format(DateTimeFormatter.ofPattern("uuuu-MM"));
        case NAM: return Integer.toString(paidAt.getYear());
        default: throw invalidRequest();
        }
    }

    private static Map<String, BigInteger> allocate(BigInteger amount, Map<String, BigInteger> weights) {
        Map<String, BigInteger> result = new TreeMap<String, BigInteger>();
        BigInteger totalWeight = BigInteger.ZERO;
        for (BigInteger weight : weights.values()) {
            if (weight.signum() < 0) { throw inconsistent(); }
            totalWeight = totalWeight.add(weight);
        }
        for (String key : weights.keySet()) { result.put(key, BigInteger.ZERO); }
        if (amount.signum() < 0) { throw inconsistent(); }
        if (amount.signum() == 0) { return result; }
        if (totalWeight.signum() <= 0) { throw inconsistent(); }

        List<Remainder> remainders = new ArrayList<Remainder>();
        BigInteger allocated = BigInteger.ZERO;
        for (Map.Entry<String, BigInteger> entry : weights.entrySet()) {
            BigInteger[] division = amount.multiply(entry.getValue()).divideAndRemainder(totalWeight);
            result.put(entry.getKey(), division[0]);
            allocated = allocated.add(division[0]);
            remainders.add(new Remainder(entry.getKey(), division[1]));
        }
        int pennies = amount.subtract(allocated).intValueExact();
        Collections.sort(remainders, new Comparator<Remainder>() {
            @Override public int compare(Remainder left, Remainder right) {
                int byRemainder = right.value.compareTo(left.value);
                return byRemainder != 0 ? byRemainder : left.key.compareTo(right.key);
            }
        });
        for (int i = 0; i < pennies; i++) {
            String key = remainders.get(i).key;
            result.put(key, result.get(key).add(ONE_CENT));
        }
        return result;
    }

    private static ThongKeDoanhThuDongDTO row(String key, String label, Amounts amounts) {
        ThongKeDoanhThuDongDTO row = new ThongKeDoanhThuDongDTO();
        row.setKhoa(key);
        row.setNhan(label);
        row.setSoHoaDon(amounts.invoiceCount);
        row.setHangBanTruocGiam(fromCents(amounts.gross));
        row.setTienGiam(fromCents(amounts.discount));
        row.setTienThue(fromCents(amounts.tax));
        row.setDoanhThuSauGiam(fromCents(amounts.gross.subtract(amounts.discount)));
        row.setTongKhachTra(fromCents(amounts.gross.subtract(amounts.discount).add(amounts.tax)));
        return row;
    }

    private static LocalDateTime from(ThongKeDoanhThuRequest request) { return request.getTuNgay().atStartOfDay(); }
    private static LocalDateTime until(ThongKeDoanhThuRequest request) { return request.getDenNgay().plusDays(1).atStartOfDay(); }

    private static BigInteger toCents(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) { throw inconsistent(); }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).toBigIntegerExact();
        } catch (ArithmeticException invalid) {
            throw inconsistent();
        }
    }

    private static BigDecimal fromCents(BigInteger cents) { return new BigDecimal(cents, 2); }
    private static BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, "Khoảng ngày, nhóm thống kê hoặc phân trang không hợp lệ.");
    }
    private static BusinessException rangeLimit(String message) { return new BusinessException(ErrorCode.VALIDATION_ERROR, message); }
    private static BusinessException inconsistent() {
        return new BusinessException(ErrorCode.SERVER_ERROR, "Dữ liệu hóa đơn không nhất quán; không thể lập thống kê chính xác.");
    }

    private static final class Invoice {
        final String id;
        final LocalDateTime paidAt;
        final Amounts amounts = new Amounts();
        Invoice(String id, LocalDateTime paidAt) { this.id = id; this.paidAt = paidAt; }
    }

    private static final class Amounts {
        BigInteger gross = BigInteger.ZERO;
        BigInteger discount = BigInteger.ZERO;
        BigInteger tax = BigInteger.ZERO;
        long invoiceCount;
        void set(BigInteger gross, BigInteger discount, BigInteger tax, BigInteger paid) {
            if (discount.compareTo(gross) > 0 || !gross.subtract(discount).add(tax).equals(paid)) { throw inconsistent(); }
            this.gross = gross;
            this.discount = discount;
            this.tax = tax;
            this.invoiceCount = 1;
        }
        void add(Amounts other) { add(other.gross, other.discount, other.tax); invoiceCount += other.invoiceCount; }
        void add(BigInteger gross, BigInteger discount, BigInteger tax) {
            this.gross = this.gross.add(gross);
            this.discount = this.discount.add(discount);
            this.tax = this.tax.add(tax);
        }
        boolean sameAmounts(Amounts other) {
            return gross.equals(other.gross) && discount.equals(other.discount) && tax.equals(other.tax);
        }
    }

    private static final class ProductSlice {
        final String productId;
        BigInteger gross = BigInteger.ZERO;
        BigInteger discount = BigInteger.ZERO;
        BigInteger net = BigInteger.ZERO;
        BigInteger tax = BigInteger.ZERO;
        String snapshotName;
        String currentName;
        ProductSlice(String productId) { this.productId = productId; }
    }

    private static final class ProductTotals {
        final String productId;
        final Amounts amounts = new Amounts();
        String snapshotName;
        String currentName;
        ProductTotals(String productId) { this.productId = productId; }
    }

    private static final class Remainder {
        final String key;
        final BigInteger value;
        Remainder(String key, BigInteger value) { this.key = key; this.value = value; }
    }
}
