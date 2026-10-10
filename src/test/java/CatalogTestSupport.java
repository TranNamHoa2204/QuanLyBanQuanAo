import java.util.*;
import javax.persistence.EntityManager;
import com.fasterxml.jackson.databind.JsonNode;
import common.enums.VaiTro;
import server.entity.*;

/** Dữ liệu fixture chỉ dùng trong H2; không phải trình import database thật. */
public final class CatalogTestSupport {
    private CatalogTestSupport() { }
    public static Map<String, Object> memoryProperties(String database) {
        Map<String, Object> p = new HashMap<>();
        p.put("javax.persistence.jdbc.driver", "org.h2.Driver");
        p.put("javax.persistence.jdbc.url", "jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS dbo");
        p.put("javax.persistence.jdbc.user", "sa"); p.put("javax.persistence.jdbc.password", "");
        p.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect"); p.put("hibernate.hbm2ddl.auto", "create-drop");
        return p;
    }
    public static void user(EntityManager em, String username, String id, VaiTro role, String hash) {
        NhanVien employee = new NhanVien(); employee.setMaNV(id); employee.setHoTen("Nhân viên thử " + username);
        employee.setSdt(id); em.persist(employee);
        TaiKhoan account = new TaiKhoan(); account.setUsername(username); account.setNhanVien(employee);
        account.setPasswordHash(hash); account.setVaiTro(role); em.persist(account);
    }
    public static void catalog(EntityManager em, JsonNode root) {
        for (JsonNode row : root.get("categories")) {
            LoaiSanPham l = new LoaiSanPham(); l.setMaLoai(row.get("maLoai").asText()); l.setTenLoai(row.get("tenLoai").asText()); em.persist(l);
        }
        for (JsonNode row : root.get("brands")) {
            ThuongHieu b = new ThuongHieu(); b.setMaThuongHieu(row.get("maThuongHieu").asText());
            b.setTenThuongHieu(row.get("tenThuongHieu").asText()); em.persist(b);
        }
        for (JsonNode row : root.get("products")) {
            SanPham p = new SanPham(); p.setMaSP(row.get("maSP").asText()); p.setTenSP(row.get("tenSP").asText());
            p.setLoaiSanPham(em.find(LoaiSanPham.class, row.get("maLoai").asText()));
            p.setThuongHieu(em.find(ThuongHieu.class, row.get("maThuongHieu").asText()));
            p.setGiaBan(row.get("giaBan").decimalValue()); p.setTrangThai(row.get("trangThai").asBoolean()); em.persist(p);
        }
        for (JsonNode row : root.get("variants")) {
            BienThe b = new BienThe(); b.setMaBienThe(row.get("maBienThe").asText());
            b.setSanPham(em.find(SanPham.class, row.get("maSP").asText())); b.setMauSac(row.get("mauSac").asText());
            b.setKichCo(row.get("kichCo").asText()); b.setSoLuongTon(row.get("soLuongTon").asInt());
            b.setTrangThai(row.get("trangThai").asBoolean()); em.persist(b);
        }
        for (JsonNode row : root.get("images")) {
            AnhSanPham a = new AnhSanPham(); a.setMaAnh(row.get("maAnh").asText());
            a.setSanPham(em.find(SanPham.class, row.get("maSP").asText())); a.setMauSac(row.get("mauSac").asText());
            a.setDuongDan(row.get("duongDan").asText()); a.setThuTu(row.get("thuTu").asInt());
            a.setAnhChinh(row.get("anhChinh").asBoolean()); em.persist(a);
        }
    }
}
