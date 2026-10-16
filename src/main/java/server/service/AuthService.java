package server.service;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import common.dto.*;
import common.protocol.ErrorCode;
import server.entity.NhanVien;
import server.entity.TaiKhoan;
import server.persistence.JpaManager;
import server.repository.JpaTaiKhoanDao;
import server.security.PasswordHasher;
import server.session.SessionManager;
import server.session.SessionManager.Session;

public final class AuthService {
    private final JpaManager jpa;
    private final SessionManager sessions;
    private final PasswordHasher hasher;
    private final String dummyHash;
    public AuthService(JpaManager jpa, SessionManager sessions, PasswordHasher hasher) {
        this.jpa = jpa; this.sessions = sessions; this.hasher = hasher;
        // Vẫn thực hiện phép băm khi username không tồn tại.
        dummyHash = hasher.hash("timing-placeholder-not-an-account".toCharArray());
    }
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty()
                || request.getUsername().length() > 50 || !validPasswordLength(request.getPassword(), 1)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nhập tài khoản và mật khẩu hợp lệ.");
        }
        String username = request.getUsername().trim();
        char[] password = request.getPassword().toCharArray();
        AtomicReference<String> issuedToken = new AtomicReference<>();
        try {
            return jpa.transaction(em -> {
                TaiKhoan account = new JpaTaiKhoanDao(em).findForUpdate(username).orElse(null);
                boolean correct = hasher.verify(password, account == null ? dummyHash : account.getPasswordHash());
                if (!correct || account == null) {
                    throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Tài khoản hoặc mật khẩu không hợp lệ.");
                }
                NhanVien employee = account.getNhanVien();
                if (employee != null) { em.refresh(employee, LockModeType.PESSIMISTIC_WRITE); }
                if (!active(account)) {
                    throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Tài khoản hoặc mật khẩu không hợp lệ.");
                }
                LoginResponse dto = new LoginResponse();
                dto.setUsername(account.getUsername());
                dto.setMaNV(employee.getMaNV());
                dto.setHoTen(employee.getHoTen());
                dto.setVaiTro(account.getVaiTro());
                // Đăng ký token trong khi khóa DB vẫn được giữ. Thao tác khóa/đổi quyền phải
                // hoặc đứng trước việc kiểm tra này, hoặc thu hồi token sau khi login commit.
                String token = sessions.create(account.getUsername(), account.getPasswordHash());
                issuedToken.set(token);
                dto.setSessionToken(token);
                return dto;
            });
        } catch (RuntimeException failure) {
            // Nếu commit thất bại sau khi token đã được cấp, token không được phép sống sót.
            sessions.revoke(issuedToken.get());
            throw failure;
        } finally { Arrays.fill(password, '\0'); }
    }
    public void logout(String token) { sessions.require(token); sessions.revoke(token); }
    public NhanVienDTO profile(String token) {
        return jpa.transaction(em -> toDto(authenticated(em, token, false).getNhanVien()));
    }
    public void changePassword(String token, DoiMatKhauRequest request) {
        if (request == null || !validPasswordLength(request.getMatKhauCu(), 1)
                || !validPasswordLength(request.getMatKhauMoi(), 8)
                || request.getMatKhauCu().equals(request.getMatKhauMoi())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Mật khẩu mới phải dài 8–128 ký tự và khác mật khẩu cũ.");
        }
        char[] oldPassword = request.getMatKhauCu().toCharArray();
        char[] newPassword = request.getMatKhauMoi().toCharArray();
        try {
            String username = jpa.transaction(em -> {
                TaiKhoan account = authenticated(em, token, true);
                if (!hasher.verify(oldPassword, account.getPasswordHash())) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mật khẩu hiện tại không đúng.");
                }
                account.setPasswordHash(hasher.hash(newPassword));
                return account.getUsername();
            });
            // Chỉ thu hồi sau khi commit thành công; tất cả thiết bị phải đăng nhập lại.
            sessions.revokeUser(username);
        } finally { Arrays.fill(oldPassword, '\0'); Arrays.fill(newPassword, '\0'); }
    }
    /** Cho service khác kiểm tra phiên trong chính transaction đang xử lý. */
    public TaiKhoan requireAuthenticated(EntityManager em, String token) {
        return authenticated(em, token, false);
    }
    private TaiKhoan authenticated(EntityManager em, String token, boolean lock) {
        Session session = sessions.require(token);
        JpaTaiKhoanDao dao = new JpaTaiKhoanDao(em);
        TaiKhoan account = (lock ? dao.findForUpdate(session.getUsername()) : dao.findById(session.getUsername())).orElse(null);
        if (!active(account) || !session.matchesCredential(account.getPasswordHash())) {
            sessions.revoke(token);
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Tài khoản đã thay đổi hoặc bị khóa. Hãy đăng nhập lại.");
        }
        return account;
    }
    private static boolean active(TaiKhoan account) {
        return account != null && account.getTrangThai() && account.getVaiTro() != null
                && account.getNhanVien() != null && account.getNhanVien().getTrangThai();
    }
    private static boolean validPasswordLength(String password, int min) {
        return password != null && password.length() >= min && password.length() <= 128;
    }
    private static NhanVienDTO toDto(NhanVien employee) {
        NhanVienDTO dto = new NhanVienDTO();
        dto.setMaNV(employee.getMaNV()); dto.setHoTen(employee.getHoTen());
        dto.setGioiTinh(employee.getGioiTinh()); dto.setNgaySinh(employee.getNgaySinh());
        dto.setSdt(employee.getSdt()); dto.setEmail(employee.getEmail()); dto.setDiaChi(employee.getDiaChi());
        dto.setChucVu(employee.getChucVu()); dto.setTrangThai(employee.getTrangThai());
        return dto;
    }
}
