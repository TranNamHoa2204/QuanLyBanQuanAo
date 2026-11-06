package server.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import common.dto.phieunhap.DuLieuChiTietPhieuNhap;
import common.dto.phieunhap.DuLieuDayDuPhieuNhap;
import common.dto.phieunhap.DuLieuPhieuNhap;
import common.dto.phieunhap.KetQuaTraCuuPhieuNhap;
import common.dto.phieunhap.YeuCauDongPhieuNhap;
import common.dto.phieunhap.YeuCauPhienBanPhieuNhap;
import common.dto.phieunhap.YeuCauTaoPhieuNhap;
import common.dto.phieunhap.YeuCauTraCuuPhieuNhap;
import common.dto.phieunhap.YeuCauXacNhanPhieuNhap;
import common.enums.NguonBienDongKho;
import common.enums.TrangThaiPhieuNhap;
import common.enums.VaiTro;
import common.protocol.MaLoi;
import server.entities.BienThe;
import server.entities.ChiTietPhieuNhap;
import server.entities.NhaCungCap;
import server.entities.PhieuNhap;
import server.entities.PhieuNhapThaoTac;
import server.entities.SanPham;
import server.entities.TaiKhoan;
import server.repositories.impl.LuuTruPhieuNhapJpa;
import server.utils.QuanLyJpa;

/** Purchase receipts. Inventory changes only in the atomic confirmation transaction. */
public final class DichVuPhieuNhap {
    private static final BigDecimal MAX_MONEY = new BigDecimal("9999999999999999.99");
    private static final int MAX_PAGE = 1000000, MAX_LINES = 200;
    private static final Pattern UUID = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final QuanLyJpa jpa;
    private final DichVuXacThuc auth;
    private final Clock clock;

    public DichVuPhieuNhap(QuanLyJpa jpa, DichVuXacThuc auth) {
        this(jpa, auth, Clock.system(BUSINESS_ZONE));
    }

    public DichVuPhieuNhap(QuanLyJpa jpa, DichVuXacThuc auth, Clock clock) {
        this.jpa = Objects.requireNonNull(jpa, "jpa");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.clock = Objects.requireNonNull(clock, "clock").withZone(BUSINESS_ZONE);
    }

    public KetQuaTraCuuPhieuNhap search(String token, YeuCauTraCuuPhieuNhap input) {
        YeuCauTraCuuPhieuNhap r = input == null ? new YeuCauTraCuuPhieuNhap() : input;
        validateSearch(r);
        return jpa.transaction(em -> {
            manager(em, token);
            LuuTruPhieuNhapJpa dao = new LuuTruPhieuNhapJpa(em);
            KetQuaTraCuuPhieuNhap out = new KetQuaTraCuuPhieuNhap();
            List<DuLieuPhieuNhap> items = new ArrayList<DuLieuPhieuNhap>();
            for (PhieuNhap row : dao.search(r)) {
                items.add(header(row));
            }
            out.setItems(items);
            out.setTotal(dao.count(r));
            out.setPage(r.getPage());
            out.setPageSize(r.getPageSize());
            return out;
        });
    }

    public DuLieuDayDuPhieuNhap detail(String token, String rawId) {
        String id = code(rawId, "Mã phiếu nhập");
        return jpa.transaction(em -> {
            manager(em, token);
            return detail(em, find(em, id, false));
        });
    }

    public DuLieuDayDuPhieuNhap create(String token, YeuCauTaoPhieuNhap request) {
        if (request == null) {
            throw invalid("Thiếu dữ liệu tạo phiếu nhập.");
        }
        String id = code(request.getMaPhieuNhap(), "Mã phiếu nhập");
        String supplierId = code(request.getMaNCC(), "Mã nhà cung cấp");
        String operationId = operation(request.getOperationId());
        return jpa.transaction(em -> {
            TaiKhoan account = manager(em, token);
            String actorId = account.getNhanVien().getMaNV();
            // Same manager retries serialize before checking the global operation id.
            em.lock(account, LockModeType.PESSIMISTIC_WRITE);
            String hash = fingerprint("CREATE", id, supplierId, actorId);
            PhieuNhapThaoTac previous = operation(em, operationId);
            if (previous != null) {
                return replay(em, previous, id, "CREATE", hash, null);
            }
            LuuTruPhieuNhapJpa dao = new LuuTruPhieuNhapJpa(em);
            PhieuNhap existingCode = dao.findById(id).orElse(null);
            if (existingCode != null) {
                throw conflict("Mã phiếu nhập đã được sử dụng.");
            }
            NhaCungCap supplier = em.find(NhaCungCap.class, supplierId, LockModeType.PESSIMISTIC_READ);
            if (supplier == null || !supplier.getTrangThai()) {
                throw invalid("Nhà cung cấp không tồn tại hoặc đã ngừng hoạt động.");
            }
            PhieuNhap row = new PhieuNhap();
            row.setMaPhieuNhap(id);
            row.setNhaCungCap(supplier);
            row.setNhanVienTao(account.getNhanVien());
            row.setNgayLap(LocalDateTime.now(clock).withNano(0));
            row.setTrangThai(TrangThaiPhieuNhap.NHAP);
            row.setTongTien(BigDecimal.ZERO);
            row.setOperationIdTao(operationId);
            em.persist(row);
            em.flush();
            recordOperation(em, row, operationId, "CREATE", hash, null);
            em.flush();
            return detail(em, row);
        });
    }

    /** mode 0=add, 1=update, 2=remove. Draft writes are versioned and replay-protected. */
    public DuLieuDayDuPhieuNhap editLine(String token, YeuCauDongPhieuNhap request, int mode) {
        if (request == null || mode < 0 || mode > 2) {
            throw invalid("Thiếu yêu cầu dòng phiếu nhập.");
        }
        String id = code(request.getMaPhieuNhap(), "Mã phiếu nhập");
        String variantId = code(request.getMaBienThe(), "Mã biến thể");
        String opId = operation(request.getOperationId());
        Long expected = request.getPhienBan();
        if (expected == null || expected < 0) {
            throw invalid("Thiếu phiên bản phiếu nhập.");
        }
        Integer quantity = request.getSoLuong();
        BigDecimal price = request.getDonGiaNhap();
        if (mode != 2) {
            if (quantity == null || quantity < 1 || quantity > 1000000) {
                throw invalid("Số lượng nhập phải từ 1 đến 1.000.000.");
            }
            price = exactMoney(price, "Đơn giá nhập");
        } else if (quantity != null || price != null) {
            throw invalid("Yêu cầu bỏ dòng không được gửi số lượng hoặc đơn giá.");
        }
        final BigDecimal inputPrice = price;
        final int inputQuantity = quantity == null ? 0 : quantity;
        final String kind = mode == 0 ? "ADD_LINE" : mode == 1 ? "UPDATE_LINE" : "REMOVE_LINE";
        final String hash = fingerprint(kind, id, variantId, String.valueOf(expected),
                String.valueOf(inputQuantity), inputPrice == null ? "" : inputPrice.toPlainString());
        return jpa.transaction(em -> {
            manager(em, token);
            PhieuNhap receipt = find(em, id, true);
            PhieuNhapThaoTac previous = operation(em, opId);
            if (previous != null) {
                return replay(em, previous, id, kind, hash, expected);
            }
            draft(receipt, expected);
            LuuTruPhieuNhapJpa dao = new LuuTruPhieuNhapJpa(em);
            List<ChiTietPhieuNhap> lines = dao.lines(id);
            ChiTietPhieuNhap target = null;
            for (ChiTietPhieuNhap line : lines) {
                if (line.getBienThe().getMaBienThe().equals(variantId)) {
                    target = line;
                    break;
                }
            }
            if (mode == 0 && target != null) {
                throw invalid("Biến thể đã có trong phiếu; hãy sửa dòng hiện tại.");
            }
            if (mode != 0 && target == null) {
                throw new NgoaiLeNghiepVu(MaLoi.NOT_FOUND, "Dòng biến thể không tồn tại trong phiếu.");
            }
            if (mode == 2) {
                em.remove(target);
                lines.remove(target);
            } else {
                BienThe variant = em.find(BienThe.class, variantId);
                if (variant == null || !variant.getTrangThai() || !variant.getSanPham().getTrangThai()) {
                    throw invalid("Biến thể hoặc sản phẩm không tồn tại/đã ngừng hoạt động.");
                }
                if (mode == 0) {
                    if (lines.size() >= MAX_LINES) {
                        throw invalid("Một phiếu nhập tối đa " + MAX_LINES + " biến thể.");
                    }
                    target = new ChiTietPhieuNhap();
                    target.setPhieuNhap(receipt);
                    target.setBienThe(variant);
                    target.setSoLuong(inputQuantity);
                    target.setDonGiaNhap(inputPrice);
                    em.persist(target);
                    lines.add(target);
                } else {
                    target.setSoLuong(inputQuantity);
                    target.setDonGiaNhap(inputPrice);
                }
            }
            receipt.setTongTien(total(lines));
            em.lock(receipt, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            em.flush();
            recordOperation(em, receipt, opId, kind, hash, expected);
            em.flush();
            return detail(em, receipt);
        });
    }

    public DuLieuDayDuPhieuNhap confirm(String token, YeuCauXacNhanPhieuNhap request) {
        if (request == null || request.getPhienBan() == null || request.getPhienBan() < 0) {
            throw invalid("Thiếu phiên bản xác nhận phiếu nhập.");
        }
        String id = code(request.getMaPhieuNhap(), "Mã phiếu nhập");
        String opId = operation(request.getOperationId());
        long expected = request.getPhienBan();
        final String hash = fingerprint("CONFIRM", id, String.valueOf(expected));
        return jpa.transaction(em -> {
            TaiKhoan account = manager(em, token);
            LuuTruPhieuNhapJpa dao = new LuuTruPhieuNhapJpa(em);
            PhieuNhap receipt = find(em, id, true);
            PhieuNhapThaoTac previous = operation(em, opId);
            if (previous != null) {
                return replay(em, previous, id, "CONFIRM", hash, expected);
            }
            if (receipt.getTrangThai() == TrangThaiPhieuNhap.DA_XAC_NHAN) {
                throw conflict("Phiếu nhập đã được xác nhận bằng thao tác khác; tồn kho không bị cộng lại.");
            }
            draft(receipt, expected);
            NhaCungCap supplier = em.find(NhaCungCap.class, receipt.getNhaCungCap().getMaNCC(),
                    LockModeType.PESSIMISTIC_READ);
            if (supplier == null || !supplier.getTrangThai()) {
                throw invalid("Nhà cung cấp đã ngừng hoạt động; chưa thể xác nhận phiếu.");
            }
            List<ChiTietPhieuNhap> lines = dao.lines(id);
            if (lines.isEmpty()) {
                throw invalid("Phiếu nhập phải có ít nhất một biến thể.");
            }
            BigDecimal amount = total(lines);
            if (receipt.getTongTien() == null || receipt.getTongTien().compareTo(amount) != 0) {
                throw conflict("Tổng tiền phiếu nhập đã thay đổi; tải lại trước khi xác nhận.");
            }
            Set<String> productIds = new TreeSet<String>();
            for (ChiTietPhieuNhap line : lines) {
                productIds.add(line.getBienThe().getSanPham().getMaSP());
            }
            for (String productId : productIds) {
                SanPham product = em.find(SanPham.class, productId, LockModeType.PESSIMISTIC_WRITE);
                if (product == null || !product.getTrangThai()) {
                    throw invalid("Sản phẩm trong phiếu nhập đã ngừng hoạt động.");
                }
                em.refresh(product, LockModeType.PESSIMISTIC_WRITE);
                if (!product.getTrangThai()) {
                    throw invalid("Sản phẩm trong phiếu nhập đã ngừng hoạt động.");
                }
            }
            List<BienThe> locked = new ArrayList<BienThe>();
            for (ChiTietPhieuNhap line : lines) {
                String variantId = line.getBienThe().getMaBienThe();
                BienThe variant = em.find(BienThe.class, variantId, LockModeType.PESSIMISTIC_WRITE);
                if (variant == null) {
                    throw invalid("Không tìm thấy biến thể " + variantId + ".");
                }
                em.refresh(variant, LockModeType.PESSIMISTIC_WRITE);
                if (!variant.getTrangThai() || !variant.getSanPham().getTrangThai()) {
                    throw invalid("Biến thể trong phiếu nhập đã ngừng hoạt động.");
                }
                if ((long) variant.getSoLuongTon() + line.getSoLuong() > Integer.MAX_VALUE) {
                    throw invalid("Tồn kho vượt giới hạn số nguyên cho biến thể " + variantId + ".");
                }
                locked.add(variant);
            }
            LocalDateTime now = LocalDateTime.now(clock).withNano(0);
            for (int i = 0; i < lines.size(); i++) {
                ChiTietPhieuNhap line = lines.get(i);
                BienThe variant = locked.get(i);
                int before = variant.getSoLuongTon(), delta = line.getSoLuong(), after = before + delta;
                Long versionBefore = variant.getPhienBan();
                variant.setSoLuongTon(after);
                String movementId = "PN:" + id + ":" + variant.getMaBienThe();
                DichVuLichSuKho.record(em, variant, account.getNhanVien(), account.getNhanVien().getHoTen(),
                        now, NguonBienDongKho.NHAP_HANG, id, movementId, before, delta, after, versionBefore,
                        "Nhập hàng theo phiếu " + id);
            }
            receipt.setTrangThai(TrangThaiPhieuNhap.DA_XAC_NHAN);
            receipt.setNhanVienXacNhan(account.getNhanVien());
            receipt.setNgayXacNhan(now);
            receipt.setOperationIdXacNhan(opId);
            receipt.setPhienBanTruocXacNhan(expected);
            em.flush();
            recordOperation(em, receipt, opId, "CONFIRM", hash, expected);
            em.flush();
            return detail(em, receipt);
        });
    }

    public DuLieuDayDuPhieuNhap cancel(String token, YeuCauPhienBanPhieuNhap request) {
        if (request == null || request.getPhienBan() == null || request.getPhienBan() < 0) {
            throw invalid("Thiếu phiên bản hủy phiếu.");
        }
        String id = code(request.getMaPhieuNhap(), "Mã phiếu nhập"),
                opId = operation(request.getOperationId());
        long expected = request.getPhienBan();
        final String hash = fingerprint("CANCEL", id, String.valueOf(expected));
        return jpa.transaction(em -> {
            manager(em, token);
            PhieuNhap receipt = find(em, id, true);
            PhieuNhapThaoTac previous = operation(em, opId);
            if (previous != null) {
                return replay(em, previous, id, "CANCEL", hash, expected);
            }
            draft(receipt, expected);
            receipt.setTrangThai(TrangThaiPhieuNhap.DA_HUY);
            em.lock(receipt, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            em.flush();
            recordOperation(em, receipt, opId, "CANCEL", hash, expected);
            em.flush();
            return detail(em, receipt);
        });
    }

    private PhieuNhapThaoTac operation(EntityManager em, String id) {
        return em.find(PhieuNhapThaoTac.class, id, LockModeType.PESSIMISTIC_READ);
    }

    private DuLieuDayDuPhieuNhap replay(EntityManager em, PhieuNhapThaoTac previous, String receiptId,
            String kind, String hash, Long expectedVersion) {
        if (!previous.getPhieuNhap().getMaPhieuNhap().equals(receiptId)
                || !previous.getLoaiThaoTac().equals(kind) || !previous.getFingerprint().equals(hash)
                || !Objects.equals(previous.getPhienBanTruoc(), expectedVersion)) {
            throw conflict("operationId đã được dùng với phiếu hoặc nội dung khác.");
        }
        PhieuNhap receipt = find(em, receiptId, false);
        if (!Objects.equals(receipt.getPhienBan(), previous.getPhienBanSau())) {
            throw conflict("Thao tác đã thành công nhưng phiếu đã đổi tiếp; tải lại trước khi thao tác.");
        }
        if ("CONFIRM".equals(kind) && receipt.getTrangThai() != TrangThaiPhieuNhap.DA_XAC_NHAN) {
            throw conflict("Phiếu không còn ở trạng thái đã xác nhận.");
        }
        return detail(em, receipt);
    }

    private static void recordOperation(EntityManager em, PhieuNhap receipt, String id, String kind,
            String hash, Long before) {
        PhieuNhapThaoTac row = new PhieuNhapThaoTac();
        row.setOperationId(id);
        row.setPhieuNhap(receipt);
        row.setLoaiThaoTac(kind);
        row.setFingerprint(hash);
        row.setPhienBanTruoc(before);
        row.setPhienBanSau(receipt.getPhienBan());
        row.setThoiGian(LocalDateTime.now(BUSINESS_ZONE));
        em.persist(row);
    }

    private TaiKhoan manager(EntityManager em, String token) {
        TaiKhoan account = auth.requireAuthenticated(em, token);
        if (account.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY) {
            throw new NgoaiLeNghiepVu(MaLoi.FORBIDDEN, "Chỉ quản lý được lập/xác nhận phiếu nhập.");
        }
        return account;
    }

    private static PhieuNhap find(EntityManager em, String id, boolean lock) {
        PhieuNhap row = lock ? new LuuTruPhieuNhapJpa(em).findForUpdate(id).orElse(null)
                : em.find(PhieuNhap.class, id);
        if (row == null) {
            throw new NgoaiLeNghiepVu(MaLoi.NOT_FOUND, "Không tìm thấy phiếu nhập.");
        }
        return row;
    }

    private static void draft(PhieuNhap row, Long expected) {
        if (row.getTrangThai() != TrangThaiPhieuNhap.NHAP) {
            throw invalid("Chỉ được sửa/hủy/xác nhận phiếu nhập nháp.");
        }
        if (expected == null || !expected.equals(row.getPhienBan())) {
            throw conflict("Phiếu nhập đã đổi. Tải lại trước khi thao tác.");
        }
    }

    private static BigDecimal total(List<ChiTietPhieuNhap> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (ChiTietPhieuNhap line : lines) {
            BigDecimal lineTotal = line.getDonGiaNhap().multiply(BigDecimal.valueOf(line.getSoLuong()))
                    .setScale(2, RoundingMode.UNNECESSARY);
            total = money(total.add(lineTotal), "Tổng tiền phiếu nhập");
        }
        return total;
    }

    private static DuLieuDayDuPhieuNhap detail(EntityManager em, PhieuNhap row) {
        DuLieuDayDuPhieuNhap dto = new DuLieuDayDuPhieuNhap();
        dto.setPhieuNhap(header(row));
        List<DuLieuChiTietPhieuNhap> lines = new ArrayList<DuLieuChiTietPhieuNhap>();
        for (ChiTietPhieuNhap line : new LuuTruPhieuNhapJpa(em).lines(row.getMaPhieuNhap())) {
            DuLieuChiTietPhieuNhap value = new DuLieuChiTietPhieuNhap();
            BienThe variant = line.getBienThe();
            value.setMaBienThe(variant.getMaBienThe());
            value.setTenSP(variant.getSanPham().getTenSP());
            value.setMauSac(variant.getMauSac());
            value.setKichCo(variant.getKichCo());
            value.setSoLuong(line.getSoLuong());
            value.setDonGiaNhap(line.getDonGiaNhap());
            value.setThanhTien(line.getDonGiaNhap().multiply(BigDecimal.valueOf(line.getSoLuong()))
                    .setScale(2, RoundingMode.UNNECESSARY));
            lines.add(value);
        }
        dto.setChiTiet(lines);
        return dto;
    }

    private static DuLieuPhieuNhap header(PhieuNhap row) {
        DuLieuPhieuNhap dto = new DuLieuPhieuNhap();
        dto.setMaPhieuNhap(row.getMaPhieuNhap());
        dto.setMaNCC(row.getNhaCungCap().getMaNCC());
        dto.setTenNCC(row.getNhaCungCap().getTenNCC());
        dto.setMaNVTao(row.getNhanVienTao().getMaNV());
        dto.setTenNVTao(row.getNhanVienTao().getHoTen());
        if (row.getNhanVienXacNhan() != null) {
            dto.setMaNVXacNhan(row.getNhanVienXacNhan().getMaNV());
            dto.setTenNVXacNhan(row.getNhanVienXacNhan().getHoTen());
        }
        dto.setNgayLap(row.getNgayLap());
        dto.setNgayXacNhan(row.getNgayXacNhan());
        dto.setTrangThai(row.getTrangThai());
        dto.setTongTien(row.getTongTien());
        dto.setPhienBan(row.getPhienBan());
        return dto;
    }

    private static void validateSearch(YeuCauTraCuuPhieuNhap r) {
        if (r.getPage() < 0 || r.getPage() > MAX_PAGE || r.getPageSize() < 1 || r.getPageSize() > 100) {
            throw invalid("Phân trang phiếu nhập không hợp lệ.");
        }
        if (r.getTuNgay() != null && r.getDenNgay() != null) {
            long days;
            try {
                days = java.time.temporal.ChronoUnit.DAYS.between(r.getTuNgay(), r.getDenNgay());
            } catch (RuntimeException ex) {
                throw invalid("Khoảng ngày phiếu nhập không hợp lệ.");
            }
            if (days < 0 || days > 3660) {
                throw invalid("Khoảng tìm kiếm tối đa 10 năm.");
            }
            try {
                r.getDenNgay().plusDays(1);
            } catch (RuntimeException ex) {
                throw invalid("Ngày kết thúc không hợp lệ.");
            }
        }
        if (r.getTuKhoa() != null && r.getTuKhoa().length() > 100) {
            throw invalid("Từ khóa tối đa 100 ký tự.");
        }
    }

    private static BigDecimal exactMoney(BigDecimal amount, String label) {
        if (amount == null) {
            throw invalid(label + " không được để trống.");
        }
        return money(amount, label);
    }

    private static BigDecimal money(BigDecimal value, String label) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX_MONEY) > 0) {
            throw invalid(label + " nằm ngoài miền hợp lệ.");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw invalid(label + " chỉ hỗ trợ tối đa 2 chữ số thập phân.");
        }
    }

    private static String code(String value, String label) {
        if (value == null || !value.trim().matches("[A-Za-z0-9_-]{1,20}")) {
            throw invalid(label + " không hợp lệ.");
        }
        return value.trim();
    }

    private static String operation(String value) {
        if (value == null || !UUID.matcher(value).matches()) {
            throw invalid("operationId phải là UUID hợp lệ và được giữ lại khi thử lại.");
        }
        return value.toLowerCase(java.util.Locale.ROOT);
    }

    private static String fingerprint(String... fields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String field : fields) {
                digest.update((field == null ? "<null>" : field).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            byte[] bytes = digest.digest();
            StringBuilder out = new StringBuilder(64);
            for (byte b : bytes) {
                out.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
            }
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static NgoaiLeNghiepVu invalid(String message) {
        return new NgoaiLeNghiepVu(MaLoi.VALIDATION_ERROR, message);
    }

    private static NgoaiLeNghiepVu conflict(String message) {
        return new NgoaiLeNghiepVu(MaLoi.VERSION_CONFLICT, message);
    }
}
