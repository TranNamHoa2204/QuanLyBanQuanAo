package server.service;

import java.text.Normalizer;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import common.dto.DieuChinhTonKhoRequest;
import common.dto.LichSuKhoDTO;
import common.dto.LichSuKhoSearchRequest;
import common.dto.LichSuKhoSearchResult;
import common.enums.NguonBienDongKho;
import common.enums.VaiTro;
import common.protocol.ErrorCode;
import server.entity.BienThe;
import server.entity.LichSuKho;
import server.entity.NhanVien;
import server.entity.TaiKhoan;
import server.persistence.JpaManager;
import server.repository.JpaLichSuKhoDao;
import server.repository.LichSuKhoDao;

/** Ghi và tra cứu ledger kho. Mọi ghi tồn từ giao dịch mới đều dùng cùng transaction. */
public final class LichSuKhoService {
    private static final int MAX_PAGE = 1000000;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RANGE_DAYS = 3660;
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9_-]{1,20}");
    private static final Pattern OPERATION_ID = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JpaManager jpa;
    private final AuthService auth;
    private final Clock clock;

    public LichSuKhoService(JpaManager jpa, AuthService auth) {
        this(jpa, auth, Clock.system(BUSINESS_ZONE));
    }

    public LichSuKhoService(JpaManager jpa, AuthService auth, Clock clock) {
        this.jpa = Objects.requireNonNull(jpa, "jpa");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.clock = Objects.requireNonNull(clock, "clock").withZone(BUSINESS_ZONE);
    }

    public LichSuKhoSearchResult search(String token, LichSuKhoSearchRequest input) {
        LichSuKhoSearchRequest request = normalize(input);
        return jpa.transaction(em -> {
            manager(em, token);
            LichSuKhoDao dao = new JpaLichSuKhoDao(em);
            LichSuKhoSearchResult result = new LichSuKhoSearchResult();
            result.setItems(dao.search(request).stream().map(LichSuKhoService::toDto).collect(Collectors.toList()));
            result.setTotal(dao.count(request));
            result.setPage(request.getPage());
            result.setPageSize(request.getPageSize());
            return result;
        });
    }

    public LichSuKhoDTO adjust(String token, DieuChinhTonKhoRequest input) {
        DieuChinhTonKhoRequest request = normalize(input);
        return jpa.transaction(em -> {
            TaiKhoan account = manager(em, token);
            String actorId = account.getNhanVien().getMaNV();
            LichSuKhoDao dao = new JpaLichSuKhoDao(em);
            LichSuKho prior = dao.findByOperationId(request.getOperationId()).orElse(null);
            if (prior != null) { return replay(prior, request, actorId); }

            BienThe variant = em.find(BienThe.class, request.getMaBienThe(), LockModeType.PESSIMISTIC_WRITE);
            if (variant == null) { throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy biến thể."); }
            // Đồng requestId trên cùng biến thể được phân xử sau khi giữ khóa tồn.
            prior = dao.findByOperationId(request.getOperationId()).orElse(null);
            if (prior != null) { return replay(prior, request, actorId); }
            long expectedVersion = request.getExpectedVersion().longValueExact();
            int target = request.getSoLuongMoi().intValueExact();
            if (expectedVersion != variant.getPhienBan().longValue()) { throw conflict(); }
            int before = variant.getSoLuongTon();
            int after = target;
            if (before == after) { throw invalid("Tồn mục tiêu phải khác tồn hiện tại."); }
            int delta = after - before;
            Long versionBefore = variant.getPhienBan();
            LocalDateTime now = now();
            variant.setSoLuongTon(after);
            LichSuKho movement = record(em, variant, account.getNhanVien(), account.getNhanVien().getHoTen(),
                    now, NguonBienDongKho.DIEU_CHINH_TON, request.getMaBienThe(), request.getOperationId(),
                    before, delta, after, versionBefore, request.getLyDo());
            em.flush();
            return toDto(movement);
        });
    }

    static LichSuKho record(EntityManager em, BienThe variant, NhanVien actor, String actorName,
            LocalDateTime timestamp, NguonBienDongKho source, String reference, String operationId,
            int before, int delta, int after, Long versionBefore, String reason) {
        if (variant == null || timestamp == null || source == null || operationId == null
                || operationId.trim().isEmpty() || operationId.length() > 100
                || reference == null || reference.trim().isEmpty() || reference.length() > 100
                || reason == null || reason.trim().isEmpty() || reason.length() > 500
                || (actorName != null && actorName.length() > 100)
                || before < 0 || after < 0 || (long) before + (long) delta != after) {
            throw new IllegalArgumentException("Invalid stock movement.");
        }
        LichSuKho movement = new LichSuKho();
        movement.setBienThe(variant);
        movement.setNguoiThaoTac(actor);
        movement.setTenNguoiThaoTacSnapshot(actorName);
        movement.setThoiGian(timestamp.withNano(0));
        movement.setNguon(source);
        movement.setThamChieu(reference);
        movement.setOperationId(operationId);
        movement.setSoLuongTruoc(before);
        movement.setSoLuongThayDoi(delta);
        movement.setSoLuongSau(after);
        movement.setPhienBanTruoc(versionBefore);
        movement.setLyDo(reason);
        em.persist(movement);
        return movement;
    }

    private TaiKhoan manager(EntityManager em, String token) {
        TaiKhoan account = auth.requireAuthenticated(em, token);
        if (account.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ quản lý được tra cứu hoặc điều chỉnh tồn kho.");
        }
        return account;
    }

    private static LichSuKhoDTO replay(LichSuKho prior, DieuChinhTonKhoRequest request, String actorId) {
        String previousActor = prior.getNguoiThaoTac() == null ? null : prior.getNguoiThaoTac().getMaNV();
        if (prior.getNguon() != NguonBienDongKho.DIEU_CHINH_TON
                || !prior.getBienThe().getMaBienThe().equals(request.getMaBienThe())
                || !Objects.equals(previousActor, actorId)
                || !Objects.equals(prior.getPhienBanTruoc(), exactLong(request.getExpectedVersion()))
                || prior.getSoLuongSau() != exactInt(request.getSoLuongMoi())
                || !Objects.equals(prior.getLyDo(), request.getLyDo())) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "operationId đã được dùng cho thao tác khác hoặc nội dung gửi lại không khớp.");
        }
        return toDto(prior);
    }

    private static LichSuKhoSearchRequest normalize(LichSuKhoSearchRequest input) {
        if (input == null || input.getPage() < 0 || input.getPage() > MAX_PAGE
                || input.getPageSize() < 1 || input.getPageSize() > MAX_PAGE_SIZE
                || (input.getTuNgay() != null && input.getTuNgay().equals(LocalDate.MAX))
                || (input.getDenNgay() != null && input.getDenNgay().equals(LocalDate.MAX))
                || (input.getTuNgay() != null && input.getDenNgay() != null
                    && input.getTuNgay().isAfter(input.getDenNgay()))) {
            throw invalid("Bộ lọc hoặc phân trang lịch sử kho không hợp lệ.");
        }
        if (input.getTuNgay() != null && input.getDenNgay() != null
                && ChronoUnit.DAYS.between(input.getTuNgay(), input.getDenNgay()) + 1 > MAX_RANGE_DAYS) {
            throw invalid("Khoảng tra cứu lịch sử tối đa 10 năm.");
        }
        LichSuKhoSearchRequest request = new LichSuKhoSearchRequest();
        request.setMaBienThe(code(input.getMaBienThe(), "Mã biến thể"));
        request.setMaNV(code(input.getMaNV(), "Mã nhân viên"));
        request.setTuNgay(input.getTuNgay()); request.setDenNgay(input.getDenNgay());
        request.setNguon(input.getNguon()); request.setPage(input.getPage()); request.setPageSize(input.getPageSize());
        return request;
    }

    private static DieuChinhTonKhoRequest normalize(DieuChinhTonKhoRequest input) {
        if (input == null || input.getSoLuongMoi() == null || input.getExpectedVersion() == null) {
            throw invalid("Biến thể, tồn mục tiêu và expectedVersion phải hợp lệ.");
        }
        int target;
        long expectedVersion;
        try {
            target = input.getSoLuongMoi().intValueExact();
            expectedVersion = input.getExpectedVersion().longValueExact();
        } catch (ArithmeticException fractionalOrOverflow) {
            throw invalid("Tồn mục tiêu và expectedVersion phải là số nguyên trong giới hạn hỗ trợ.");
        }
        if (target < 0 || expectedVersion < 0) { throw invalid("Tồn mục tiêu và expectedVersion không được âm."); }
        String operationId = input.getOperationId() == null ? null : input.getOperationId().trim();
        if (operationId == null || !OPERATION_ID.matcher(operationId).matches()) {
            throw invalid("operationId phải là UUID để hỗ trợ gửi lại an toàn.");
        }
        try {
            operationId = UUID.fromString(operationId).toString();
        } catch (IllegalArgumentException malformed) { throw invalid("operationId không hợp lệ."); }
        String variantId = code(input.getMaBienThe(), "Mã biến thể");
        if (variantId == null) { throw invalid("Mã biến thể là bắt buộc."); }
        String reason = text(input.getLyDo(), 500, "Lý do");
        DieuChinhTonKhoRequest request = new DieuChinhTonKhoRequest();
        request.setMaBienThe(variantId);
        request.setSoLuongMoi(BigDecimal.valueOf(target)); request.setExpectedVersion(BigDecimal.valueOf(expectedVersion));
        request.setLyDo(reason); request.setOperationId(operationId);
        return request;
    }

    private static LichSuKhoDTO toDto(LichSuKho movement) {
        LichSuKhoDTO dto = new LichSuKhoDTO();
        dto.setMaLichSuKho(movement.getMaLichSuKho());
        dto.setMaBienThe(movement.getBienThe().getMaBienThe());
        dto.setMaSP(movement.getBienThe().getSanPham().getMaSP());
        dto.setMaNV(movement.getNguoiThaoTac() == null ? null : movement.getNguoiThaoTac().getMaNV());
        dto.setTenNguoiThaoTac(movement.getTenNguoiThaoTacSnapshot());
        dto.setThoiGian(movement.getThoiGian()); dto.setNguon(movement.getNguon());
        dto.setThamChieu(movement.getThamChieu()); dto.setOperationId(movement.getOperationId());
        dto.setSoLuongTruoc(movement.getSoLuongTruoc()); dto.setSoLuongThayDoi(movement.getSoLuongThayDoi());
        dto.setSoLuongSau(movement.getSoLuongSau()); dto.setPhienBanTruoc(movement.getPhienBanTruoc());
        dto.setLyDo(movement.getLyDo());
        return dto;
    }

    private LocalDateTime now() { return LocalDateTime.now(clock).withNano(0); }

    private static String code(String value, String label) {
        if (value == null || value.trim().isEmpty()) { return null; }
        String result = value.trim();
        if (!CODE.matcher(result).matches()) { throw invalid(label + " không hợp lệ."); }
        return result;
    }

    private static String text(String value, int maximum, String label) {
        if (value == null) { throw invalid(label + " là bắt buộc."); }
        String result = Normalizer.normalize(value, Normalizer.Form.NFC).trim();
        if (result.isEmpty() || result.length() > maximum
                || result.chars().anyMatch(Character::isISOControl)) { throw invalid(label + " trống, quá dài hoặc không hợp lệ."); }
        return result;
    }

    private static BusinessException conflict() {
        return new BusinessException(ErrorCode.VERSION_CONFLICT, "Tồn đã thay đổi. Tải lại trước khi điều chỉnh.");
    }
    private static int exactInt(BigDecimal value) {
        try { return value.intValueExact(); }
        catch (ArithmeticException invalid) { return Integer.MIN_VALUE; }
    }
    private static long exactLong(BigDecimal value) {
        try { return value.longValueExact(); }
        catch (ArithmeticException invalid) { return Long.MIN_VALUE; }
    }
    private static BusinessException invalid(String message) { return new BusinessException(ErrorCode.VALIDATION_ERROR, message); }
}
