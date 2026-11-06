package server.services;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import common.dto.nhanvien.DuLieuHoSoNhanVien;
import common.dto.nhanvien.DuLieuNhanVien;
import common.dto.nhanvien.KetQuaTraCuuNhanVien;
import common.dto.nhanvien.YeuCauChiTietNhanVien;
import common.dto.nhanvien.YeuCauLuuNhanVien;
import common.dto.nhanvien.YeuCauTaoNhanVien;
import common.dto.nhanvien.YeuCauTraCuuNhanVien;
import common.dto.nhanvien.YeuCauTrangThaiNhanVien;
import common.dto.taikhoan.DuLieuTaiKhoan;
import common.dto.taikhoan.YeuCauTaoTaiKhoan;
import common.dto.taikhoan.YeuCauTrangThaiTaiKhoan;
import common.dto.taikhoan.YeuCauVaiTroTaiKhoan;
import common.enums.VaiTro;
import common.protocol.MaLoi;
import server.entities.NhanVien;
import server.entities.TaiKhoan;
import server.repositories.impl.LuuTruNhanVienJpa;
import server.repositories.impl.LuuTruTaiKhoanJpa;
import server.security.BoBamMatKhau;
import server.session.QuanLyPhienDangNhap.Session;
import server.session.QuanLyPhienDangNhap;
import server.utils.QuanLyJpa;

/** Nghiệp vụ nhân viên/tài khoản. Entity và hash chỉ tồn tại phía server. */
public final class DichVuNhanVien {
    private final QuanLyJpa jpa;
    private final DichVuXacThuc auth;
    private final QuanLyPhienDangNhap sessions;
    private final BoBamMatKhau hasher;

    public DichVuNhanVien(QuanLyJpa jpa, DichVuXacThuc auth, QuanLyPhienDangNhap sessions,
            BoBamMatKhau hasher) {
        this.jpa = Objects.requireNonNull(jpa, "jpa");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
    }

    public KetQuaTraCuuNhanVien search(String token, YeuCauTraCuuNhanVien request) {
        validateSearch(request);
        return jpa.transaction(em -> {
            requireManager(em, token);
            LuuTruNhanVienJpa employees = new LuuTruNhanVienJpa(em);
            List<NhanVien> page = employees.search(request);
            KetQuaTraCuuNhanVien result = new KetQuaTraCuuNhanVien();
            result.setTotal(employees.count(request));
            result.setPage(request.getPage());
            result.setPageSize(request.getPageSize());
            List<String> ids = new ArrayList<>();
            for (NhanVien employee : page) {
                ids.add(employee.getMaNV());
            }
            Map<String, TaiKhoan> accounts = accountMap(new LuuTruTaiKhoanJpa(em).findByEmployeeIds(ids));
            List<DuLieuHoSoNhanVien> items = new ArrayList<>();
            for (NhanVien employee : page) {
                items.add(record(employee, accounts.get(employee.getMaNV())));
            }
            result.setItems(items);
            return result;
        });
    }

    public DuLieuHoSoNhanVien detail(String token, YeuCauChiTietNhanVien request) {
        if (request == null) {
            throw invalid("Thiếu mã nhân viên.");
        }
        String id = code(request.getMaNV());
        return jpa.transaction(em -> {
            requireManager(em, token);
            NhanVien employee = new LuuTruNhanVienJpa(em).findById(id)
                    .orElseThrow(() -> missing("Không tìm thấy nhân viên."));
            TaiKhoan account = new LuuTruTaiKhoanJpa(em).findByEmployee(id).orElse(null);
            return record(employee, account);
        });
    }

    /** Tạo nhân viên và tài khoản tùy chọn cùng transaction; nếu có tài khoản thì không thể lưu riêng lẻ. */
    public DuLieuHoSoNhanVien create(String token, YeuCauTaoNhanVien request) {
        if (request == null) {
            throw invalid("Thiếu dữ liệu nhân viên.");
        }
        YeuCauTaoTaiKhoan accountRequest = request.getTaiKhoan();
        char[] password = copyAndClearPassword(accountRequest);
        try {
            DuLieuNhanVien employeeData = employeeData(request.getNhanVien());
            if (!employeeData.getTrangThai()) {
                throw invalid("Nhân viên mới phải ở trạng thái hoạt động.");
            }
            AccountData accountData = accountRequest == null ? null
                    : accountData(accountRequest, employeeData.getMaNV(), password);
            if (accountData != null) {
                preflightManager(token);
            }
            String hash = accountData == null ? null : hash(password);
            return jpa.transaction(em -> {
                managerForWrite(em, token);
                LuuTruNhanVienJpa employeeDao = new LuuTruNhanVienJpa(em);
                if (employeeDao.codeInUseIgnoreCase(employeeData.getMaNV())) {
                    throw invalid("Mã nhân viên đã tồn tại.");
                }
                if (employeeDao.phoneInUse(employeeData.getSdt(), null)) {
                    throw invalid("Số điện thoại đã được sử dụng.");
                }
                NhanVien employee = toEntity(employeeData);
                em.persist(employee);
                // Buộc lỗi PK/unique nhân viên xảy ra trước khi tạo tài khoản; mọi lỗi sau flush vẫn rollback
                // chung.
                em.flush();

                TaiKhoan account = null;
                if (accountData != null) {
                    LuuTruTaiKhoanJpa accountDao = new LuuTruTaiKhoanJpa(em);
                    if (accountDao.usernameExistsIgnoreCase(accountData.username)) {
                        throw invalid("Tên đăng nhập đã tồn tại.");
                    }
                    if (accountDao.findByEmployee(employee.getMaNV()).isPresent()) {
                        throw invalid("Nhân viên đã có tài khoản.");
                    }
                    account = new TaiKhoan();
                    account.setUsername(accountData.username);
                    account.setNhanVien(employee);
                    account.setPasswordHash(hash);
                    account.setVaiTro(accountData.role);
                    account.setTrangThai(true);
                    em.persist(account);
                    em.flush();
                }
                return record(employee, account);
            });
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public DuLieuHoSoNhanVien update(String token, YeuCauLuuNhanVien request) {
        if (request == null || request.getValue() == null || request.getExpected() == null) {
            throw invalid("Thiếu dữ liệu hiện tại để kiểm tra trước khi lưu.");
        }
        DuLieuNhanVien value = employeeData(request.getValue());
        DuLieuNhanVien expected = request.getExpected();
        if (!Objects.equals(value.getMaNV(), expected.getMaNV())) {
            throw conflict();
        }
        return jpa.transaction(em -> {
            managerForWrite(em, token);
            LuuTruNhanVienJpa employeeDao = new LuuTruNhanVienJpa(em);
            NhanVien employee = employeeDao.findForUpdate(value.getMaNV())
                    .orElseThrow(() -> missing("Không tìm thấy nhân viên."));
            if (!matches(expected, employee)) {
                throw conflict();
            }
            if (value.getTrangThai() != employee.getTrangThai()) {
                throw invalid("Trạng thái nhân viên phải đổi bằng thao tác khóa/mở khóa.");
            }
            if (employeeDao.phoneInUse(value.getSdt(), value.getMaNV())) {
                throw invalid("Số điện thoại đã được sử dụng.");
            }
            applyProfile(employee, value);
            em.flush();
            TaiKhoan account = new LuuTruTaiKhoanJpa(em).findByEmployee(value.getMaNV()).orElse(null);
            return record(employee, account);
        });
    }

    public DuLieuHoSoNhanVien setEmployeeActive(String token, YeuCauTrangThaiNhanVien request,
            boolean active) {
        if (request == null || request.getExpectedTrangThai() == null) {
            throw invalid("Thiếu trạng thái hiện tại.");
        }
        String id = code(request.getMaNV());
        EmployeeChange change = jpa.transaction(em -> {
            ManagerContext context = managerForWrite(em, token);
            LuuTruTaiKhoanJpa accountDao = new LuuTruTaiKhoanJpa(em);
            // Đọc username trước khi khóa nhân viên để giữ thứ tự khóa đồng nhất với đăng nhập (tài khoản →
            // nhân viên).
            TaiKhoan linkedBeforeLock = accountDao.findByEmployee(id).orElse(null);
            LuuTruNhanVienJpa employeeDao = new LuuTruNhanVienJpa(em);
            NhanVien employee = employeeDao.findForUpdate(id)
                    .orElseThrow(() -> missing("Không tìm thấy nhân viên."));
            if (employee.getTrangThai() != request.getExpectedTrangThai()) {
                throw conflict();
            }
            boolean changed = employee.getTrangThai() != active;
            if (changed && !active && isActiveManager(linkedBeforeLock)
                    && context.activeManagers.size() <= 1) {
                throw invalid("Không thể ngừng hoạt động quản lý cuối cùng đang có quyền truy cập.");
            }
            if (changed) {
                employee.setTrangThai(active);
                em.flush();
            }
            TaiKhoan linked = accountDao.findByEmployee(id).orElse(null);
            return new EmployeeChange(record(employee, linked),
                    changed && linked != null ? linked.getUsername() : null);
        });
        if (change.usernameToRevoke != null) {
            sessions.revokeUser(change.usernameToRevoke);
        }
        return change.record;
    }

    public DuLieuTaiKhoan createAccount(String token, YeuCauTaoTaiKhoan request) {
        if (request == null) {
            throw invalid("Thiếu thông tin tạo tài khoản.");
        }
        char[] password = copyAndClearPassword(request);
        try {
            String id = code(request.getMaNV());
            AccountData data = accountData(request, id, password);
            preflightManager(token);
            String hash = hash(password);
            return jpa.transaction(em -> {
                managerForWrite(em, token);
                NhanVien employee = new LuuTruNhanVienJpa(em).findForUpdate(id)
                        .orElseThrow(() -> missing("Không tìm thấy nhân viên."));
                if (!employee.getTrangThai()) {
                    throw invalid("Không thể tạo tài khoản cho nhân viên đã ngừng hoạt động.");
                }
                LuuTruTaiKhoanJpa accountDao = new LuuTruTaiKhoanJpa(em);
                if (accountDao.findByEmployee(id).isPresent()) {
                    throw invalid("Nhân viên đã có tài khoản.");
                }
                if (accountDao.usernameExistsIgnoreCase(data.username)) {
                    throw invalid("Tên đăng nhập đã tồn tại.");
                }
                TaiKhoan account = new TaiKhoan();
                account.setUsername(data.username);
                account.setNhanVien(employee);
                account.setPasswordHash(hash);
                account.setVaiTro(data.role);
                account.setTrangThai(true);
                em.persist(account);
                em.flush();
                return accountDto(account);
            });
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public DuLieuTaiKhoan setAccountActive(String token, YeuCauTrangThaiTaiKhoan request, boolean active) {
        if (request == null || request.getExpectedTrangThai() == null) {
            throw invalid("Thiếu trạng thái tài khoản hiện tại.");
        }
        String username = username(request.getUsername());
        AccountChange change = jpa.transaction(em -> {
            ManagerContext context = managerForWrite(em, token);
            TaiKhoan account = lockAccountAndEmployee(em, username);
            if (account.getTrangThai() != request.getExpectedTrangThai()) {
                throw conflict();
            }
            boolean changed = account.getTrangThai() != active;
            if (changed && !active && isActiveManager(account) && context.activeManagers.size() <= 1) {
                throw invalid("Không thể khóa quản lý cuối cùng đang có quyền truy cập.");
            }
            if (changed) {
                account.setTrangThai(active);
                em.flush();
            }
            return new AccountChange(accountDto(account), changed ? account.getUsername() : null);
        });
        if (change.usernameToRevoke != null) {
            sessions.revokeUser(change.usernameToRevoke);
        }
        return change.dto;
    }

    public DuLieuTaiKhoan changeRole(String token, YeuCauVaiTroTaiKhoan request) {
        if (request == null || request.getExpectedVaiTro() == null || request.getVaiTro() == null) {
            throw invalid("Thiếu vai trò hiện tại hoặc vai trò mới.");
        }
        String username = username(request.getUsername());
        AccountChange change = jpa.transaction(em -> {
            ManagerContext context = managerForWrite(em, token);
            TaiKhoan account = lockAccountAndEmployee(em, username);
            if (account.getVaiTro() != request.getExpectedVaiTro()) {
                throw conflict();
            }
            boolean changed = account.getVaiTro() != request.getVaiTro();
            if (changed && request.getExpectedVaiTro() == VaiTro.NHAN_VIEN_QUAN_LY && account.getTrangThai()
                    && account.getNhanVien().getTrangThai() && context.activeManagers.size() <= 1) {
                throw invalid("Không thể hạ quyền quản lý cuối cùng đang có quyền truy cập.");
            }
            if (changed) {
                account.setVaiTro(request.getVaiTro());
                em.flush();
            }
            return new AccountChange(accountDto(account), changed ? account.getUsername() : null);
        });
        if (change.usernameToRevoke != null) {
            sessions.revokeUser(change.usernameToRevoke);
        }
        return change.dto;
    }

    private ManagerContext managerForWrite(EntityManager em, String token) {
        TaiKhoan firstRead = requireManager(em, token);
        LuuTruTaiKhoanJpa accounts = new LuuTruTaiKhoanJpa(em);
        List<TaiKhoan> activeManagers = accounts.lockActiveManagers();
        Session session = sessions.require(token);
        TaiKhoan current = accounts.findForUpdate(firstRead.getUsername()).orElse(null);
        if (current == null) {
            sessions.revoke(token);
            throw unauthenticated();
        }
        em.refresh(current, LockModeType.PESSIMISTIC_WRITE);
        NhanVien employee = current.getNhanVien();
        if (employee != null) {
            em.refresh(employee, LockModeType.PESSIMISTIC_WRITE);
        }
        if (!isActive(current) || !session.getUsername().equals(current.getUsername())
                || !session.matchesCredential(current.getPasswordHash())) {
            sessions.revoke(token);
            throw unauthenticated();
        }
        if (current.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY) {
            throw forbidden();
        }
        return new ManagerContext(activeManagers);
    }

    private TaiKhoan requireManager(EntityManager em, String token) {
        TaiKhoan account = auth.requireAuthenticated(em, token);
        if (account.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY) {
            throw forbidden();
        }
        return account;
    }

    /** Tránh tiêu tốn PBKDF2 cho request tạo tài khoản trái quyền; quyền vẫn được kiểm tra lại khi ghi. */
    private void preflightManager(String token) {
        jpa.transaction(em -> {
            requireManager(em, token);
            return null;
        });
    }

    private static TaiKhoan lockAccountAndEmployee(EntityManager em, String username) {
        TaiKhoan account = new LuuTruTaiKhoanJpa(em).findForUpdate(username)
                .orElseThrow(() -> missing("Không tìm thấy tài khoản."));
        em.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        NhanVien employee = account.getNhanVien();
        if (employee != null) {
            em.refresh(employee, LockModeType.PESSIMISTIC_WRITE);
        }
        return account;
    }

    private static DuLieuNhanVien employeeData(DuLieuNhanVien input) {
        if (input == null) {
            throw invalid("Thiếu hồ sơ nhân viên.");
        }
        DuLieuNhanVien result = new DuLieuNhanVien();
        result.setMaNV(code(input.getMaNV()));
        result.setHoTen(requiredText(input.getHoTen(), 100, "Họ tên"));
        result.setSdt(phone(input.getSdt()));
        result.setEmail(optionalText(input.getEmail(), 254, "Email"));
        result.setDiaChi(optionalText(input.getDiaChi(), 255, "Địa chỉ"));
        result.setChucVu(optionalText(input.getChucVu(), 100, "Chức vụ"));
        result.setGioiTinh(input.getGioiTinh());
        result.setNgaySinh(input.getNgaySinh());
        result.setTrangThai(input.getTrangThai());
        return result;
    }

    private static AccountData accountData(YeuCauTaoTaiKhoan request, String employeeId, char[] password) {
        String requestedEmployee = request.getMaNV();
        if (requestedEmployee != null && !code(requestedEmployee).equals(employeeId)) {
            throw invalid("Tài khoản phải liên kết với đúng nhân viên đang được tạo.");
        }
        String username = username(request.getUsername());
        if (request.getVaiTro() == null) {
            throw invalid("Chọn vai trò cho tài khoản.");
        }
        if (password == null || password.length < 8 || password.length > 128) {
            throw invalid("Mật khẩu ban đầu phải dài 8–128 ký tự.");
        }
        return new AccountData(username, request.getVaiTro());
    }

    private static char[] copyAndClearPassword(YeuCauTaoTaiKhoan request) {
        if (request == null) {
            return new char[0];
        }
        String value = request.getMatKhauBanDau();
        request.setMatKhauBanDau(null);
        return value == null ? new char[0] : value.toCharArray();
    }

    private String hash(char[] password) {
        String value = hasher.hash(password);
        if (value == null || value.trim().isEmpty() || value.length() > 255) {
            throw new IllegalStateException("Bộ băm tạo kết quả không hợp lệ.");
        }
        return value;
    }

    private static void validateSearch(YeuCauTraCuuNhanVien request) {
        if (request == null || request.getPage() < 0 || request.getPage() > 1000000
                || request.getPageSize() < 1 || request.getPageSize() > 100) {
            throw invalid("Bộ lọc hoặc phân trang không hợp lệ.");
        }
        if (request.getTuKhoa() != null) {
            String key = Normalizer.normalize(request.getTuKhoa(), Normalizer.Form.NFC).trim();
            if (key.length() > 100 || key.chars().anyMatch(Character::isISOControl)) {
                throw invalid("Từ khóa tối đa 100 ký tự và không chứa ký tự điều khiển.");
            }
            request.setTuKhoa(key);
        }
    }

    private static String code(String value) {
        String result = requiredText(value, 20, "Mã nhân viên");
        if (!result.matches("[A-Za-z0-9_-]{1,20}")) {
            throw invalid("Mã nhân viên chỉ gồm chữ Latin, số, _ hoặc -.");
        }
        return result;
    }

    private static String username(String value) {
        String result = requiredText(value, 50, "Tên đăng nhập");
        if (!result.matches("[A-Za-z0-9_.-]{1,50}")) {
            throw invalid("Tên đăng nhập chỉ gồm chữ Latin, số, dấu chấm, _ hoặc -.");
        }
        return result;
    }

    private static String phone(String value) {
        String result = requiredText(value, 20, "Số điện thoại");
        if (!result.matches("\\+?[0-9][0-9 ()-]{4,18}[0-9]")) {
            throw invalid("Số điện thoại chỉ gồm chữ số, dấu + ở đầu, khoảng trắng, ngoặc hoặc gạch ngang.");
        }
        long digits = result.chars().filter(Character::isDigit).count();
        if (digits < 6) {
            throw invalid("Số điện thoại phải có ít nhất 6 chữ số.");
        }
        return result;
    }

    private static String requiredText(String value, int max, String field) {
        if (value == null) {
            throw invalid(field + " không được để trống.");
        }
        String result = Normalizer.normalize(value, Normalizer.Form.NFC).trim();
        if (result.isEmpty() || result.length() > max || result.chars().anyMatch(Character::isISOControl)) {
            throw invalid(field + " trống, quá dài hoặc chứa ký tự điều khiển.");
        }
        return result;
    }

    private static String optionalText(String value, int max, String field) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return requiredText(value, max, field);
    }

    private static NhanVien toEntity(DuLieuNhanVien data) {
        NhanVien employee = new NhanVien();
        employee.setMaNV(data.getMaNV());
        applyProfile(employee, data);
        employee.setTrangThai(data.getTrangThai());
        return employee;
    }

    private static void applyProfile(NhanVien employee, DuLieuNhanVien data) {
        employee.setHoTen(data.getHoTen());
        employee.setGioiTinh(data.getGioiTinh());
        employee.setNgaySinh(data.getNgaySinh());
        employee.setSdt(data.getSdt());
        employee.setEmail(data.getEmail());
        employee.setDiaChi(data.getDiaChi());
        employee.setChucVu(data.getChucVu());
    }

    private static boolean matches(DuLieuNhanVien expected, NhanVien employee) {
        return Objects.equals(expected.getMaNV(), employee.getMaNV())
                && Objects.equals(expected.getHoTen(), employee.getHoTen())
                && Objects.equals(expected.getGioiTinh(), employee.getGioiTinh())
                && Objects.equals(expected.getNgaySinh(), employee.getNgaySinh())
                && Objects.equals(expected.getSdt(), employee.getSdt())
                && Objects.equals(expected.getEmail(), employee.getEmail())
                && Objects.equals(expected.getDiaChi(), employee.getDiaChi())
                && Objects.equals(expected.getChucVu(), employee.getChucVu())
                && expected.getTrangThai() == employee.getTrangThai();
    }

    private static DuLieuHoSoNhanVien record(NhanVien employee, TaiKhoan account) {
        DuLieuHoSoNhanVien result = new DuLieuHoSoNhanVien();
        result.setNhanVien(employeeDto(employee));
        result.setTaiKhoan(account == null ? null : accountDto(account));
        return result;
    }

    private static DuLieuNhanVien employeeDto(NhanVien employee) {
        DuLieuNhanVien result = new DuLieuNhanVien();
        result.setMaNV(employee.getMaNV());
        result.setHoTen(employee.getHoTen());
        result.setGioiTinh(employee.getGioiTinh());
        result.setNgaySinh(employee.getNgaySinh());
        result.setSdt(employee.getSdt());
        result.setEmail(employee.getEmail());
        result.setDiaChi(employee.getDiaChi());
        result.setChucVu(employee.getChucVu());
        result.setTrangThai(employee.getTrangThai());
        return result;
    }

    private static DuLieuTaiKhoan accountDto(TaiKhoan account) {
        DuLieuTaiKhoan result = new DuLieuTaiKhoan();
        result.setUsername(account.getUsername());
        result.setMaNV(account.getNhanVien().getMaNV());
        result.setVaiTro(account.getVaiTro());
        result.setTrangThai(account.getTrangThai());
        return result;
    }

    private static Map<String, TaiKhoan> accountMap(List<TaiKhoan> accounts) {
        Map<String, TaiKhoan> result = new HashMap<>();
        for (TaiKhoan account : accounts) {
            result.put(account.getNhanVien().getMaNV(), account);
        }
        return result;
    }

    private static boolean isActive(TaiKhoan account) {
        return account != null && account.getTrangThai() && account.getVaiTro() != null
                && account.getNhanVien() != null && account.getNhanVien().getTrangThai();
    }

    private static boolean isActiveManager(TaiKhoan account) {
        return isActive(account) && account.getVaiTro() == VaiTro.NHAN_VIEN_QUAN_LY;
    }

    private static NgoaiLeNghiepVu invalid(String message) {
        return new NgoaiLeNghiepVu(MaLoi.VALIDATION_ERROR, message);
    }

    private static NgoaiLeNghiepVu missing(String message) {
        return new NgoaiLeNghiepVu(MaLoi.NOT_FOUND, message);
    }

    private static NgoaiLeNghiepVu conflict() {
        return new NgoaiLeNghiepVu(MaLoi.VERSION_CONFLICT, "Dữ liệu đã đổi. Tải lại trước khi lưu.");
    }

    private static NgoaiLeNghiepVu forbidden() {
        return new NgoaiLeNghiepVu(MaLoi.FORBIDDEN,
                "Chỉ nhân viên quản lý được quản trị nhân viên/tài khoản.");
    }

    private static NgoaiLeNghiepVu unauthenticated() {
        return new NgoaiLeNghiepVu(MaLoi.UNAUTHENTICATED,
                "Tài khoản hoặc nhân viên đã bị khóa/thay đổi. Hãy đăng nhập lại.");
    }

    private static final class AccountData {
        final String username;
        final VaiTro role;

        AccountData(String username, VaiTro role) {
            this.username = username;
            this.role = role;
        }
    }

    private static final class ManagerContext {
        final List<TaiKhoan> activeManagers;

        ManagerContext(List<TaiKhoan> activeManagers) {
            this.activeManagers = activeManagers;
        }
    }

    private static final class EmployeeChange {
        final DuLieuHoSoNhanVien record;
        final String usernameToRevoke;

        EmployeeChange(DuLieuHoSoNhanVien record, String usernameToRevoke) {
            this.record = record;
            this.usernameToRevoke = usernameToRevoke;
        }
    }

    private static final class AccountChange {
        final DuLieuTaiKhoan dto;
        final String usernameToRevoke;

        AccountChange(DuLieuTaiKhoan dto, String usernameToRevoke) {
            this.dto = dto;
            this.usernameToRevoke = usernameToRevoke;
        }
    }
}
