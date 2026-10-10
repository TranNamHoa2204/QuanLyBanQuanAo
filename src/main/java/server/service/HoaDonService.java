package server.service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.*;
import common.dto.*;
import common.enums.*;
import common.protocol.ErrorCode;
import common.util.MoneyUtils;
import server.entity.*;
import server.persistence.JpaManager;
import server.repository.*;

public final class HoaDonService {
    private static final BigDecimal MAX_MONEY = new BigDecimal("9999999999999999.99");
    private final JpaManager jpa;
    private final AuthService auth;
    private final Clock clock;
    public HoaDonService(JpaManager jpa, AuthService auth) { this(jpa, auth, Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"))); }
    public HoaDonService(JpaManager jpa, AuthService auth, Clock clock) { this.jpa = jpa; this.auth = auth; this.clock = clock; }

    public HoaDonDetailDTO create(String token, HoaDonCreateRequest request) {
        return jpa.transaction(em -> {
            TaiKhoan account = auth.requireAuthenticated(em, token);
            if (request == null) { throw invalid("Thiếu thông tin tạo hóa đơn."); }
            String id = code(request.getMaHD(), true), taxId = code(request.getMaThue(), true);
            String customerId = code(request.getMaKH(), false), promoId = code(request.getMaKM(), false);
            String note = request.getGhiChu();
            if (note != null && note.length() > 500) { throw invalid("Ghi chú tối đa 500 ký tự."); }
            HoaDon existing = em.find(HoaDon.class, id);
            if (existing != null) {
                access(existing, account);
                if (!existing.getNhanVien().getMaNV().equals(account.getNhanVien().getMaNV())
                        || !Objects.equals(taxId, existing.getThue().getMaThue())
                        || !Objects.equals(customerId, existing.getKhachHang() == null ? null : existing.getKhachHang().getMaKH())
                        || !Objects.equals(promoId, existing.getKhuyenMai() == null ? null : existing.getKhuyenMai().getMaKM())
                        || !Objects.equals(note, existing.getGhiChu())) {
                    throw new BusinessException(ErrorCode.VERSION_CONFLICT, "Mã hóa đơn đã được dùng với thông tin khác.");
                }
                return detailDto(em, existing); // cùng mã/cùng thông tin: không tạo lần thứ hai
            }
            HoaDon invoice = new HoaDon(); invoice.setMaHD(id); invoice.setNhanVien(account.getNhanVien());
            invoice.setThue(em.find(Thue.class, taxId));
            if (customerId != null) {
                KhachHang customer = em.find(KhachHang.class, customerId);
                if (customer == null || !customer.getTrangThai()) { throw invalid("Khách hàng không tồn tại hoặc ngừng hoạt động."); }
                invoice.setKhachHang(customer);
            }
            if (promoId != null) {
                KhuyenMai promo = em.find(KhuyenMai.class, promoId);
                if (promo == null) { throw invalid("Không tìm thấy khuyến mãi."); }
                invoice.setKhuyenMai(promo);
            }
            validRates(invoice);
            invoice.setPhanTramThue(invoice.getThue().getPhanTram());
            invoice.setPhanTramGiam(invoice.getKhuyenMai() == null ? BigDecimal.ZERO : invoice.getKhuyenMai().getPhanTramGiam());
            invoice.setNgayLap(LocalDateTime.now(clock).withNano(0)); invoice.setGhiChu(note);
            totals(invoice, Collections.emptyList()); em.persist(invoice); em.flush();
            return detailDto(em, invoice);
        });
    }
    public HoaDonDetailDTO detail(String token, String id) {
        return jpa.transaction(em -> {
            TaiKhoan account = auth.requireAuthenticated(em, token);
            HoaDon invoice = find(em, code(id, true), false); access(invoice, account); return detailDto(em, invoice);
        });
    }
    public HoaDonSearchResult search(String token, HoaDonSearchRequest request) {
        return jpa.transaction(em -> {
            TaiKhoan account = auth.requireAuthenticated(em, token);
            if (request == null || request.getPage() < 0 || request.getPage() > 1000000
                    || request.getPageSize() < 1 || request.getPageSize() > 100) { throw invalid("Phân trang không hợp lệ."); }
            code(request.getMaHD(), false);
            String employee = account.getVaiTro() == VaiTro.NHAN_VIEN_QUAN_LY ? null : account.getNhanVien().getMaNV();
            JpaHoaDonDao dao = new JpaHoaDonDao(em);
            HoaDonSearchResult result = new HoaDonSearchResult();
            result.setItems(dao.search(request, employee).stream().map(HoaDonService::headerDto).collect(Collectors.toList()));
            result.setTotal(dao.count(request, employee)); result.setPage(request.getPage()); result.setPageSize(request.getPageSize());
            return result;
        });
    }
    public List<ThueDTO> taxes(String token) {
        return jpa.transaction(em -> {
            auth.requireAuthenticated(em, token);
            List<Thue> rows = em.createQuery("select t from Thue t where t.trangThai = true order by t.maThue", Thue.class)
                    .setMaxResults(1001).getResultList();
            if (rows.size() > 1000) { throw invalid("Cần phân trang danh mục thuế."); }
            return rows.stream().map(t -> {
                ThueDTO dto = new ThueDTO(); dto.setMaThue(t.getMaThue()); dto.setTenThue(t.getTenThue());
                dto.setPhanTram(t.getPhanTram()); dto.setMoTa(t.getMoTa()); dto.setTrangThai(t.getTrangThai()); return dto;
            }).collect(Collectors.toList());
        });
    }
    /** mode 0: thêm mới, 1: số lượng tuyệt đối, 2: bỏ dòng. Không nhận giá từ client. */
    public HoaDonDetailDTO editLine(String token, HoaDonLineRequest request, int mode) {
        return jpa.transaction(em -> {
            TaiKhoan account = auth.requireAuthenticated(em, token);
            if (request == null || mode < 0 || mode > 2) { throw invalid("Thiếu yêu cầu chi tiết."); }
            HoaDon invoice = find(em, code(request.getMaHD(), true), true);
            access(invoice, account); draft(invoice, request.getPhienBan());
            String variantId = code(request.getMaBienThe(), true);
            List<ChiTietHoaDon> lines = lines(em, invoice);
            ChiTietHoaDon target = lines.stream().filter(c -> c.getBienThe().getMaBienThe().equals(variantId)).findFirst().orElse(null);
            if (mode == 0 && target != null) { throw invalid("Mặt hàng đã có; dùng Đổi số lượng."); }
            if (mode != 0 && target == null) { throw new BusinessException(ErrorCode.NOT_FOUND, "Dòng hàng không tồn tại."); }
            if (mode == 2) {
                em.remove(target); lines.remove(target);
            } else {
                if (request.getSoLuong() < 1 || request.getSoLuong() > 1000000) { throw invalid("Số lượng phải từ 1 đến 1.000.000."); }
                BienThe variant = em.find(BienThe.class, variantId);
                saleable(variant);
                if (request.getSoLuong() > variant.getSoLuongTon()) {
                    throw new BusinessException(ErrorCode.OUT_OF_STOCK, "Số lượng vượt tồn kho hiện tại.");
                }
                if (mode == 0) {
                    if (lines.size() >= 200) { throw invalid("Một hóa đơn tối đa 200 mặt hàng."); }
                    target = new ChiTietHoaDon(); target.setHoaDon(invoice); target.setBienThe(variant);
                    target.setDonGia(money(variant.getSanPham().getGiaBan())); target.setSoLuong(request.getSoLuong());
                    // Kiểm tra trước INSERT IDENTITY: SQL Server tính thanhTien ngay khi chèn.
                    money(MoneyUtils.lineTotal(target.getDonGia(), target.getSoLuong()));
                    em.persist(target); lines.add(target);
                } else { target.setSoLuong(request.getSoLuong()); }
            }
            totals(invoice, lines);
            // Dòng 0 đồng cũng phải làm tăng phiên bản header.
            em.lock(invoice, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            em.flush(); return detailDto(em, invoice);
        });
    }
    public HoaDonDetailDTO pay(String token, HoaDonVersionRequest request) {
        return jpa.transaction(em -> {
            TaiKhoan account = auth.requireAuthenticated(em, token);
            if (request == null) { throw invalid("Thiếu yêu cầu thanh toán."); }
            HoaDon invoice = find(em, code(request.getMaHD(), true), true); access(invoice, account);
            if (invoice.getTrangThai() == TrangThaiHoaDon.DA_THANH_TOAN) { return detailDto(em, invoice); }
            draft(invoice, request.getPhienBan());
            List<ChiTietHoaDon> lines = lines(em, invoice);
            if (lines.isEmpty()) { throw invalid("Hóa đơn phải có ít nhất một mặt hàng."); }
            validRates(invoice);
            if (invoice.getPhanTramThue().compareTo(invoice.getThue().getPhanTram()) != 0
                    || invoice.getPhanTramGiam().compareTo(invoice.getKhuyenMai() == null ? BigDecimal.ZERO
                        : invoice.getKhuyenMai().getPhanTramGiam()) != 0) {
                throw new BusinessException(ErrorCode.VERSION_CONFLICT, "Thuế/khuyến mãi đã đổi. Hủy nháp và tạo hóa đơn mới.");
            }
            if (invoice.getKhachHang() != null && !invoice.getKhachHang().getTrangThai()) {
                throw invalid("Khách hàng đã ngừng hoạt động.");
            }
            Map<String, BienThe> locked = new TreeMap<>();
            // Quản lý thêm biến thể cũng khóa sản phẩm trước, tránh vòng khóa product ↔ variant.
            java.util.SortedSet<String> productIds = new java.util.TreeSet<>();
            for (ChiTietHoaDon line : lines) { productIds.add(line.getBienThe().getSanPham().getMaSP()); }
            for (String productId : productIds) {
                SanPham product = em.find(SanPham.class, productId, LockModeType.PESSIMISTIC_WRITE);
                if (product != null) { em.refresh(product, LockModeType.PESSIMISTIC_WRITE); }
            }
            // lines() đã ORDER BY maBienThe. Khóa tồn theo cùng thứ tự ở mọi client.
            for (ChiTietHoaDon line : lines) {
                String id = line.getBienThe().getMaBienThe();
                // Có thể đã đọc proxy để xác định productIds; refresh khóa và đọc lại phiên bản mới.
                BienThe variant = em.find(BienThe.class, id);
                if (variant != null) { em.refresh(variant, LockModeType.PESSIMISTIC_WRITE); }
                saleable(variant);
                if (line.getSoLuong() > variant.getSoLuongTon()) {
                    throw new BusinessException(ErrorCode.OUT_OF_STOCK, "Không đủ tồn cho " + id + ". Chưa thanh toán.");
                }
                locked.put(id, variant);
            }
            for (ChiTietHoaDon line : lines) {
                BienThe variant = locked.get(line.getBienThe().getMaBienThe());
                variant.setSoLuongTon(variant.getSoLuongTon() - line.getSoLuong());
            }
            totals(invoice, lines); invoice.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN);
            em.flush(); return detailDto(em, invoice);
        });
    }
    public HoaDonDetailDTO cancel(String token, HoaDonVersionRequest request) {
        return jpa.transaction(em -> {
            TaiKhoan account = auth.requireAuthenticated(em, token);
            if (request == null) { throw invalid("Thiếu yêu cầu hủy."); }
            HoaDon invoice = find(em, code(request.getMaHD(), true), true); access(invoice, account);
            if (invoice.getTrangThai() == TrangThaiHoaDon.DA_HUY) { return detailDto(em, invoice); }
            draft(invoice, request.getPhienBan()); invoice.setTrangThai(TrangThaiHoaDon.DA_HUY);
            em.flush(); return detailDto(em, invoice);
        });
    }
    private void validRates(HoaDon invoice) {
        Thue tax = invoice.getThue();
        if (tax == null || !tax.getTrangThai()) { throw invalid("Thuế không tồn tại hoặc đã ngừng áp dụng."); }
        rate(tax.getPhanTram());
        KhuyenMai promo = invoice.getKhuyenMai();
        if (promo != null) {
            LocalDate today = LocalDate.now(clock);
            if (!promo.getTrangThai() || promo.getNgayBatDau() == null || promo.getNgayKetThuc() == null
                    || today.isBefore(promo.getNgayBatDau()) || today.isAfter(promo.getNgayKetThuc())) {
                throw invalid("Khuyến mãi chưa bắt đầu, hết hạn hoặc ngừng áp dụng.");
            }
            rate(promo.getPhanTramGiam());
        }
    }
    private static void totals(HoaDon invoice, List<ChiTietHoaDon> lines) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ChiTietHoaDon line : lines) { subtotal = money(subtotal.add(money(MoneyUtils.lineTotal(line.getDonGia(), line.getSoLuong())))); }
        BigDecimal discount = money(MoneyUtils.percentage(subtotal, rate(invoice.getPhanTramGiam())));
        BigDecimal tax = money(MoneyUtils.percentage(subtotal.subtract(discount), rate(invoice.getPhanTramThue())));
        invoice.setTamTinh(money(subtotal)); invoice.setTienGiam(discount); invoice.setTienThue(tax);
        invoice.setTongTien(money(subtotal.subtract(discount).add(tax)));
    }
    private static HoaDon find(EntityManager em, String id, boolean lock) {
        return (lock ? new JpaHoaDonDao(em).locked(id) : new JpaHoaDonDao(em).findById(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy hóa đơn."));
    }
    private static List<ChiTietHoaDon> lines(EntityManager em, HoaDon invoice) {
        List<ChiTietHoaDon> rows = new JpaChiTietHoaDonDao(em).byInvoice(invoice.getMaHD());
        if (rows.size() > 200) { throw invalid("Hóa đơn vượt giới hạn 200 dòng."); } return rows;
    }
    private static void access(HoaDon invoice, TaiKhoan account) {
        if (account.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY
                && !invoice.getNhanVien().getMaNV().equals(account.getNhanVien().getMaNV())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền với hóa đơn này.");
        }
    }
    private static void draft(HoaDon invoice, Long expected) {
        if (invoice.getTrangThai() != TrangThaiHoaDon.NHAP) { throw invalid("Chỉ được sửa/hủy/thanh toán hóa đơn nháp."); }
        if (expected == null || !expected.equals(invoice.getPhienBan())) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT, "Hóa đơn đã đổi. Tải lại trước khi thao tác.");
        }
    }
    private static void saleable(BienThe variant) {
        if (variant == null || !variant.getTrangThai() || !variant.getSanPham().getTrangThai()) {
            throw invalid("Sản phẩm hoặc biến thể không tồn tại/đã ngừng bán.");
        }
    }
    private static String code(String value, boolean required) {
        if (value == null || value.trim().isEmpty()) {
            if (required) { throw invalid("Mã không được trống."); } return null;
        }
        if (value.length() > 20) { throw invalid("Mã tối đa 20 ký tự."); } return value.trim();
    }
    private static BigDecimal rate(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw invalid("Tỷ lệ không hợp lệ."); } return value;
    }
    private static BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX_MONEY) > 0) { throw invalid("Số tiền vượt miền DECIMAL(18,2)."); }
        BigDecimal rounded = MoneyUtils.round(value);
        if (rounded.compareTo(MAX_MONEY) > 0) { throw invalid("Số tiền vượt miền DECIMAL(18,2)."); } return rounded;
    }
    private static BusinessException invalid(String message) { return new BusinessException(ErrorCode.VALIDATION_ERROR, message); }
    private static HoaDonDetailDTO detailDto(EntityManager em, HoaDon h) {
        HoaDonDetailDTO result = new HoaDonDetailDTO(); result.setHoaDon(headerDto(h));
        result.setChiTiet(lines(em, h).stream().map(c -> {
            ChiTietHoaDonDTO dto = new ChiTietHoaDonDTO();
            dto.setMaCTHD(c.getMaCTHD()); dto.setMaHD(h.getMaHD()); dto.setMaBienThe(c.getBienThe().getMaBienThe());
            dto.setTenSP(c.getBienThe().getSanPham().getTenSP()); dto.setMauSac(c.getBienThe().getMauSac());
            dto.setKichCo(c.getBienThe().getKichCo()); dto.setSoLuong(c.getSoLuong()); dto.setDonGia(c.getDonGia());
            dto.setThanhTien(MoneyUtils.lineTotal(c.getDonGia(), c.getSoLuong())); return dto;
        }).collect(Collectors.toList())); return result;
    }
    private static HoaDonDTO headerDto(HoaDon h) {
        HoaDonDTO dto = new HoaDonDTO(); dto.setMaHD(h.getMaHD()); dto.setMaNV(h.getNhanVien().getMaNV());
        dto.setMaKH(h.getKhachHang() == null ? null : h.getKhachHang().getMaKH());
        dto.setMaKM(h.getKhuyenMai() == null ? null : h.getKhuyenMai().getMaKM());
        dto.setMaThue(h.getThue().getMaThue()); dto.setNgayLap(h.getNgayLap()); dto.setTamTinh(h.getTamTinh());
        dto.setPhanTramGiam(h.getPhanTramGiam()); dto.setTienGiam(h.getTienGiam());
        dto.setPhanTramThue(h.getPhanTramThue()); dto.setTienThue(h.getTienThue());
        dto.setTongTien(h.getTongTien()); dto.setTrangThai(h.getTrangThai()); dto.setGhiChu(h.getGhiChu());
        dto.setPhienBan(h.getPhienBan()); return dto;
    }
}
