package server.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import common.dto.KhuyenMaiDTO;
import common.dto.KhuyenMaiDetailRequest;
import common.dto.KhuyenMaiSaveRequest;
import common.dto.KhuyenMaiSearchRequest;
import common.dto.KhuyenMaiSearchResult;
import common.dto.KhuyenMaiStatusRequest;
import common.enums.TrangThaiHieuLucKhuyenMai;
import common.enums.VaiTro;
import common.protocol.ErrorCode;
import server.entity.KhuyenMai;
import server.entity.TaiKhoan;
import server.persistence.JpaManager;
import server.repository.JpaKhuyenMaiDao;
import server.repository.JpaTaiKhoanDao;

/** Nghiệp vụ khuyến mãi; tỷ lệ và trạng thái hiệu lực luôn do server quyết định. */
public final class KhuyenMaiService {
    private static final BigDecimal MIN_RATE = BigDecimal.ZERO;
    private static final BigDecimal MAX_RATE = new BigDecimal("100.00");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final JpaManager jpa;
    private final AuthService auth;
    private final Clock clock;

    public KhuyenMaiService(JpaManager jpa, AuthService auth) {
        this(jpa, auth, Clock.system(BUSINESS_ZONE));
    }

    public KhuyenMaiService(JpaManager jpa, AuthService auth, Clock clock) {
        this.jpa = Objects.requireNonNull(jpa, "jpa");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.clock = Objects.requireNonNull(clock, "clock").withZone(BUSINESS_ZONE);
    }

    /** Quản lý xem toàn bộ; nhân viên bán hàng chỉ tra cứu chương trình có hiệu lực hôm nay. */
    public KhuyenMaiSearchResult search(String token, KhuyenMaiSearchRequest input) {
        KhuyenMaiSearchRequest request = normalizeSearch(input);
        return jpa.transaction(em -> {
            TaiKhoan account = auth.requireAuthenticated(em, token);
            boolean manager = account.getVaiTro() == VaiTro.NHAN_VIEN_QUAN_LY;
            if (!manager && account.getVaiTro() != VaiTro.NHAN_VIEN_BAN_HANG) { throw forbidden(); }
            LocalDate today = LocalDate.now(clock);
            Boolean savedActive = manager ? request.getTrangThai() : Boolean.TRUE;
            boolean effectiveOnly = !manager;
            JpaKhuyenMaiDao promotions = new JpaKhuyenMaiDao(em);
            List<KhuyenMaiDTO> items = new ArrayList<>();
            for (KhuyenMai row : promotions.search(request, savedActive, effectiveOnly, today)) {
                items.add(toDto(row, today));
            }
            KhuyenMaiSearchResult result = new KhuyenMaiSearchResult();
            result.setItems(items); result.setTotal(promotions.count(request, savedActive, effectiveOnly, today));
            result.setPage(request.getPage()); result.setPageSize(request.getPageSize());
            return result;
        });
    }

    /** Người bán chỉ được xem chi tiết một khuyến mãi hiện đang có hiệu lực. */
    public KhuyenMaiDTO detail(String token, KhuyenMaiDetailRequest request) {
        if (request == null) { throw invalid("Thiếu mã khuyến mãi."); }
        String id = code(request.getMaKM());
        return jpa.transaction(em -> {
            TaiKhoan account = auth.requireAuthenticated(em, token);
            boolean manager = account.getVaiTro() == VaiTro.NHAN_VIEN_QUAN_LY;
            if (!manager && account.getVaiTro() != VaiTro.NHAN_VIEN_BAN_HANG) { throw forbidden(); }
            KhuyenMai row = new JpaKhuyenMaiDao(em).findById(id)
                    .orElseThrow(() -> missing("Không tìm thấy khuyến mãi."));
            LocalDate today = LocalDate.now(clock);
            if (!manager && !effective(row, today)) { throw missing("Không tìm thấy chương trình đang có hiệu lực."); }
            return toDto(row, today);
        });
    }

    public KhuyenMaiDTO create(String token, KhuyenMaiDTO input) {
        PromotionData data = createData(input);
        return jpa.transaction(em -> {
            requireManagerForWrite(em, token);
            JpaKhuyenMaiDao promotions = new JpaKhuyenMaiDao(em);
            if (promotions.codeInUseIgnoreCase(data.maKM)) { throw invalid("Mã khuyến mãi đã tồn tại."); }
            KhuyenMai row = new KhuyenMai(); apply(row, data); em.persist(row); em.flush();
            return toDto(row, LocalDate.now(clock));
        });
    }

    /** Snapshot so sánh dưới khóa ghi thay cho cột @Version không có trong baseline. */
    public KhuyenMaiDTO update(String token, KhuyenMaiSaveRequest request) {
        if (request == null || request.getExpected() == null || request.getValue() == null) {
            throw invalid("Thiếu snapshot hiện tại hoặc dữ liệu khuyến mãi cần lưu.");
        }
        KhuyenMaiDTO expected = request.getExpected(), value = request.getValue();
        String id = code(expected.getMaKM());
        if (!id.equals(code(value.getMaKM()))) { throw invalid("Không thể đổi mã khuyến mãi."); }
        return jpa.transaction(em -> {
            requireManagerForWrite(em, token);
            JpaKhuyenMaiDao promotions = new JpaKhuyenMaiDao(em);
            KhuyenMai row = promotions.findForUpdate(id)
                    .orElseThrow(() -> missing("Không tìm thấy khuyến mãi."));
            em.refresh(row, LockModeType.PESSIMISTIC_WRITE);
            if (!matches(expected, row)) { throw conflict(); }
            if (value.getTrangThai() != row.getTrangThai()) {
                throw invalid("Cờ hoạt động phải đổi bằng thao tác ngừng/khôi phục.");
            }
            PromotionData data = updateData(value, row);
            apply(row, data); em.flush();
            return toDto(row, LocalDate.now(clock));
        });
    }

    public KhuyenMaiDTO setActive(String token, KhuyenMaiStatusRequest request, boolean active) {
        if (request == null || request.getExpected() == null) { throw invalid("Thiếu snapshot khuyến mãi hiện tại."); }
        KhuyenMaiDTO expected = request.getExpected();
        String id = code(expected.getMaKM());
        return jpa.transaction(em -> {
            requireManagerForWrite(em, token);
            KhuyenMai row = new JpaKhuyenMaiDao(em).findForUpdate(id)
                    .orElseThrow(() -> missing("Không tìm thấy khuyến mãi."));
            em.refresh(row, LockModeType.PESSIMISTIC_WRITE);
            if (!matches(expected, row)) { throw conflict(); }
            row.setTrangThai(active); em.flush();
            return toDto(row, LocalDate.now(clock));
        });
    }

    private TaiKhoan requireManagerForWrite(EntityManager em, String token) {
        TaiKhoan initial = auth.requireAuthenticated(em, token);
        JpaTaiKhoanDao accounts = new JpaTaiKhoanDao(em);
        TaiKhoan locked = accounts.findForUpdate(initial.getUsername()).orElse(null);
        if (locked == null) { throw unauthenticated(); }
        em.refresh(locked, LockModeType.PESSIMISTIC_WRITE);
        // Kiểm tra lại phiên/quyền sau khi lấy khóa, tránh ghi bằng phiên vừa bị đổi hoặc thu hồi.
        TaiKhoan current = auth.requireAuthenticated(em, token);
        if (!current.getUsername().equals(locked.getUsername())) { throw unauthenticated(); }
        if (current.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY) { throw forbidden(); }
        return current;
    }

    private static PromotionData createData(KhuyenMaiDTO input) {
        if (input == null) { throw invalid("Thiếu dữ liệu khuyến mãi."); }
        if (!input.getTrangThai()) { throw invalid("Khuyến mãi mới phải ở trạng thái hoạt động."); }
        return normalize(input, true);
    }

    private static PromotionData updateData(KhuyenMaiDTO input, KhuyenMai current) {
        String name = Objects.equals(input.getTenKM(), current.getTenKM())
                ? current.getTenKM() : requiredText(input.getTenKM(), 200, "Tên khuyến mãi");
        BigDecimal rate = rate(input.getPhanTramGiam());
        LocalDate start = input.getNgayBatDau(), end = input.getNgayKetThuc();
        validateDates(start, end);
        return new PromotionData(current.getMaKM(), name, rate, start, end, current.getTrangThai());
    }

    private static PromotionData normalize(KhuyenMaiDTO input, boolean requireActive) {
        String id = code(input.getMaKM());
        String name = requiredText(input.getTenKM(), 200, "Tên khuyến mãi");
        BigDecimal rate = rate(input.getPhanTramGiam());
        LocalDate start = input.getNgayBatDau(), end = input.getNgayKetThuc();
        validateDates(start, end);
        if (requireActive && !input.getTrangThai()) { throw invalid("Khuyến mãi mới phải ở trạng thái hoạt động."); }
        return new PromotionData(id, name, rate, start, end, input.getTrangThai());
    }

    private static void apply(KhuyenMai row, PromotionData data) {
        row.setMaKM(data.maKM); row.setTenKM(data.tenKM); row.setPhanTramGiam(data.rate);
        row.setNgayBatDau(data.start); row.setNgayKetThuc(data.end); row.setTrangThai(data.active);
    }

    private static KhuyenMaiSearchRequest normalizeSearch(KhuyenMaiSearchRequest input) {
        if (input == null || input.getPage() < 0 || input.getPage() > 1000000
                || input.getPageSize() < 1 || input.getPageSize() > 100) {
            throw invalid("Phân trang phải có trang từ 0 đến 1.000.000 và 1–100 dòng mỗi trang.");
        }
        KhuyenMaiSearchRequest result = new KhuyenMaiSearchRequest();
        result.setTuKhoa(optionalText(input.getTuKhoa(), 100, "Từ khóa"));
        result.setTrangThai(input.getTrangThai()); result.setPage(input.getPage()); result.setPageSize(input.getPageSize());
        return result;
    }

    private static String code(String value) {
        String result = requiredText(value, 20, "Mã khuyến mãi");
        if (!result.matches("[A-Za-z0-9_-]{1,20}")) {
            throw invalid("Mã khuyến mãi chỉ gồm chữ Latin, số, _ hoặc -.");
        }
        return result.toUpperCase(Locale.ROOT);
    }

    private static BigDecimal rate(BigDecimal value) {
        if (value == null) { throw invalid("Tỷ lệ giảm không được để trống."); }
        final BigDecimal normalized;
        try { normalized = value.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException excessScale) { throw invalid("Tỷ lệ giảm chỉ được có tối đa 2 chữ số thập phân."); }
        if (normalized.compareTo(MIN_RATE) < 0 || normalized.compareTo(MAX_RATE) > 0) {
            throw invalid("Tỷ lệ giảm phải trong khoảng 0–100%.");
        }
        return normalized;
    }

    private static void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) { throw invalid("Ngày bắt đầu và kết thúc không được để trống."); }
        if (end.isBefore(start)) { throw invalid("Ngày kết thúc phải bằng hoặc sau ngày bắt đầu."); }
    }

    private static String requiredText(String value, int max, String field) {
        String result = optionalText(value, max, field);
        if (result == null) { throw invalid(field + " không được để trống."); }
        return result;
    }

    private static String optionalText(String value, int max, String field) {
        if (value == null) { return null; }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
        String result = stripUnicodeSpace(normalized);
        if (result.isEmpty()) { return null; }
        if (result.length() > max || result.codePoints().anyMatch(Character::isISOControl)) {
            throw invalid(field + " quá dài hoặc chứa ký tự điều khiển.");
        }
        return result;
    }

    private static String stripUnicodeSpace(String value) {
        int start = 0, end = value.length();
        while (start < end) {
            int cp = value.codePointAt(start);
            if (!Character.isWhitespace(cp) && !Character.isSpaceChar(cp)) { break; }
            start += Character.charCount(cp);
        }
        while (start < end) {
            int cp = value.codePointBefore(end);
            if (!Character.isWhitespace(cp) && !Character.isSpaceChar(cp)) { break; }
            end -= Character.charCount(cp);
        }
        return value.substring(start, end);
    }

    private static boolean matches(KhuyenMaiDTO expected, KhuyenMai row) {
        return Objects.equals(expected.getMaKM(), row.getMaKM())
                && Objects.equals(expected.getTenKM(), row.getTenKM())
                && sameRate(expected.getPhanTramGiam(), row.getPhanTramGiam())
                && Objects.equals(expected.getNgayBatDau(), row.getNgayBatDau())
                && Objects.equals(expected.getNgayKetThuc(), row.getNgayKetThuc())
                && expected.getTrangThai() == row.getTrangThai();
    }

    private static boolean sameRate(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private static boolean effective(KhuyenMai row, LocalDate today) {
        return row.getTrangThai() && row.getNgayBatDau() != null && row.getNgayKetThuc() != null
                && !today.isBefore(row.getNgayBatDau()) && !today.isAfter(row.getNgayKetThuc());
    }

    private static TrangThaiHieuLucKhuyenMai derivedState(KhuyenMai row, LocalDate today) {
        if (today.isBefore(row.getNgayBatDau())) { return TrangThaiHieuLucKhuyenMai.CHUA_BAT_DAU; }
        if (today.isAfter(row.getNgayKetThuc())) { return TrangThaiHieuLucKhuyenMai.HET_HAN; }
        return TrangThaiHieuLucKhuyenMai.DANG_DIEN_RA;
    }

    private static KhuyenMaiDTO toDto(KhuyenMai row, LocalDate today) {
        KhuyenMaiDTO dto = new KhuyenMaiDTO(); dto.setMaKM(row.getMaKM()); dto.setTenKM(row.getTenKM());
        dto.setPhanTramGiam(row.getPhanTramGiam()); dto.setNgayBatDau(row.getNgayBatDau());
        dto.setNgayKetThuc(row.getNgayKetThuc()); dto.setTrangThai(row.getTrangThai());
        dto.setTrangThaiHieuLuc(derivedState(row, today)); return dto;
    }

    private static BusinessException invalid(String message) { return new BusinessException(ErrorCode.VALIDATION_ERROR, message); }
    private static BusinessException missing(String message) { return new BusinessException(ErrorCode.NOT_FOUND, message); }
    private static BusinessException conflict() { return new BusinessException(ErrorCode.VERSION_CONFLICT, "Khuyến mãi đã thay đổi. Tải lại trước khi lưu."); }
    private static BusinessException forbidden() { return new BusinessException(ErrorCode.FORBIDDEN, "Chỉ nhân viên quản lý được thay đổi khuyến mãi."); }
    private static BusinessException unauthenticated() { return new BusinessException(ErrorCode.UNAUTHENTICATED, "Phiên đăng nhập đã thay đổi. Hãy đăng nhập lại."); }

    private static final class PromotionData {
        final String maKM, tenKM;
        final BigDecimal rate;
        final LocalDate start, end;
        final boolean active;
        PromotionData(String maKM, String tenKM, BigDecimal rate, LocalDate start, LocalDate end, boolean active) {
            this.maKM = maKM; this.tenKM = tenKM; this.rate = rate; this.start = start; this.end = end; this.active = active;
        }
    }
}
