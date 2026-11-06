package server.app;

import java.io.IOException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import common.config.CauHinhUngDung;
import common.enums.VaiTro;
import server.config.CauHinhCoSoDuLieu;
import server.entities.NhanVien;
import server.entities.TaiKhoan;
import server.security.BoBamMatKhauPbkdf2;
import server.utils.QuanLyJpa;

/** Công cụ cục bộ tạo duy nhất tài khoản quản lý đầu tiên; không phải API mạng. */
public final class TaoTaiKhoanQuanLy {
    private TaoTaiKhoanQuanLy() {
    }

    public static void main(String[] args) throws IOException {
        Properties local = CauHinhUngDung.load();
        String username = required(local, "setup.username", "QUANAO_INITIAL_USERNAME", 1, 50);
        String password = required(local, "setup.password", "QUANAO_INITIAL_PASSWORD", 6, 128);
        String maNV = required(local, "setup.employee.id", "QUANAO_INITIAL_EMPLOYEE_ID", 1, 20);
        String hoTen = required(local, "setup.employee.name", "QUANAO_INITIAL_EMPLOYEE_NAME", 1, 100);
        String phone = required(local, "setup.employee.phone", "QUANAO_INITIAL_EMPLOYEE_PHONE", 1, 20);
        if (!username.matches("[A-Za-z0-9_.-]+") || !maNV.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Username/mã nhân viên chỉ dùng ký tự ASCII hợp lệ.");
        }
        char[] chars = password.toCharArray();
        String hash;
        try {
            hash = new BoBamMatKhauPbkdf2().hash(chars);
        } finally {
            Arrays.fill(chars, '\0');
        }
        Map<String, Object> properties = CauHinhCoSoDuLieu.load();
        // Ngăn hai lần setup song song cùng vượt qua phép đếm tài khoản rỗng.
        properties.put("hibernate.connection.isolation", Connection.TRANSACTION_SERIALIZABLE);
        try (QuanLyJpa jpa = new QuanLyJpa(properties)) {
            jpa.transaction(em -> {
                long count = em.createQuery("select count(t) from TaiKhoan t", Long.class).getSingleResult();
                if (count != 0) {
                    throw new IllegalStateException("Đã có tài khoản; công cụ không thay đổi dữ liệu.");
                }
                if (em.find(NhanVien.class, maNV) != null) {
                    throw new IllegalStateException(
                            "Mã nhân viên đã tồn tại; chọn mã mới, không ghi đè hồ sơ.");
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
        System.out.println("Đã tạo tài khoản quản lý đầu tiên.");
    }

    private static String required(Properties properties, String key, String environmentName, int min,
            int max) {
        String value = CauHinhUngDung.required(properties, key, environmentName);
        if (value.length() < min || value.length() > max) {
            throw new IllegalArgumentException("Giá trị cấu hình không hợp lệ: " + key);
        }
        return value;
    }
}
