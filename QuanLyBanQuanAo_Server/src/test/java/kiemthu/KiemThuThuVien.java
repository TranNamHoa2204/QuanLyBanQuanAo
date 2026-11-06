package kiemthu;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.Persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.Version;
import org.junit.Test;

import common.dto.nhanvien.DuLieuNhanVien;
import common.dto.sanpham.DuLieuSanPham;
import common.protocol.HanhDong;
import common.protocol.YeuCau;

/** Kiểm tra dependency/DTO mà không mở Socket hoặc kết nối SQL Server. */
public class KiemThuThuVien {
    @Test
    public void jpaHibernateAndSqlServerDriverAreAvailable() throws Exception {
        assertNotNull(Persistence.class);
        assertEquals("5.6.15.Final", Version.getVersionString());
        assertNotNull(Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver"));
    }

    @Test
    public void localDateSurvivesJsonRoundTrip() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        DuLieuNhanVien employee = new DuLieuNhanVien();
        employee.setMaNV("NV001");
        employee.setNgaySinh(LocalDate.of(2000, 1, 2));
        DuLieuNhanVien restored = mapper.readValue(mapper.writeValueAsString(employee), DuLieuNhanVien.class);
        assertEquals(employee.getNgaySinh(), restored.getNgaySinh());
    }

    @Test
    public void requestPayloadPreservesItsTypeAndMoney() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        DuLieuSanPham product = new DuLieuSanPham();
        product.setMaSP("SP001");
        product.setGiaBan(new BigDecimal("250000.00"));
        YeuCau<DuLieuSanPham> request = new YeuCau<DuLieuSanPham>("r1", HanhDong.TRA_CUU_SAN_PHAM, null,
                product);
        YeuCau<DuLieuSanPham> restored = mapper.readValue(mapper.writeValueAsString(request),
                new TypeReference<YeuCau<DuLieuSanPham>>() {
                });
        assertEquals("SP001", restored.getPayload().getMaSP());
        assertEquals(0, product.getGiaBan().compareTo(restored.getPayload().getGiaBan()));
    }
}
