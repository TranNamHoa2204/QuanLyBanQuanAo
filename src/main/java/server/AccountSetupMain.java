package server;

import java.util.Arrays;
import java.util.Map;
import java.sql.Connection;
import common.enums.VaiTro;
import server.config.DatabaseConfig;
import server.entity.NhanVien;
import server.entity.TaiKhoan;
import server.persistence.JpaManager;
import server.security.Pbkdf2PasswordHasher;

/** Công cụ cục bộ tạo duy nhất tài khoản quản lý đầu tiên; không phải API mạng. */
public final class AccountSetupMain {
    private AccountSetupMain() {
    }

    public static void main(String[] args) {
        String username = required("QUANAO_INITIAL_USERNAME", 1, 50);
        String password = required("QUANAO_INITIAL_PASSWORD", 8, 128);
        String maNV = required("QUANAO_INITIAL_EMPLOYEE_ID", 1, 20);
        String hoTen = required("QUANAO_INITIAL_EMPLOYEE_NAME", 1, 100);
        String phone = required("QUANAO_INITIAL_EMPLOYEE_PHONE", 1, 20);
        if (!username.matches("[A-Za-z0-9_.-]+") || !maNV.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Username/mã nhân viên chỉ dùng ký tự ASCII hợp lệ.");
        }
        char[] chars = password.toCharArray();
        String hash;
        try {
            hash = new Pbkdf2PasswordHasher().hash(chars);
        } finally {
            Arrays.fill(chars, '\0');
        }
        Map<String, Object> properties = DatabaseConfig.fromEnvironment();
        // Ngăn hai lần setup song song cùng vượt qua phép đếm tài khoản rỗng.
        properties.put("hibernate.connection.isolation", Connection.TRANSACTION_SERIALIZABLE);
        try (JpaManager jpa = new JpaManager(properties)) {
            jpa.transaction(em -> {
                long count = em.createQuery("select count(t) from TaiKhoan t", Long.class).getSingleResult();
                if (count != 0) {
                    throw new IllegalStateException("Đã có tài khoản; công cụ không thay đổi dữ liệu.");
                }
                if (em.find(NhanVien.class, maNV) != null) {
                    throw new IllegalStateException("Mã nhân viên đã tồn tại; chọn mã mới, không ghi đè hồ sơ.");
                }
                NhanVien employee = new NhanVien();
                employee.setMaNV(maNV);
                employee.setHoTen(hoTen);
                employee.setSdt(phone);
                employee.setChucVu("Quản lý");
                em.persist(employee);
                TaiKhoan account = new TaiKhoan();
                account.setUsername(username);
                account.setPasswordHash(hash);
                account.setNhanVien(employee);
                account.setVaiTro(VaiTro.NHAN_VIEN_QUAN_LY);
                em.persist(account);
                return null;
            });
        }
        System.out
                .println("Đã tạo tài khoản quản lý đầu tiên. Xóa biến QUANAO_INITIAL_PASSWORD khỏi Run Configuration.");
    }

    private static String required(String name, int min, int max) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty() || value.length() < min || value.length() > max) {
            throw new IllegalArgumentException("Chưa cấu hình hoặc giá trị không hợp lệ: " + name);
        }
        return value;
    }
}
