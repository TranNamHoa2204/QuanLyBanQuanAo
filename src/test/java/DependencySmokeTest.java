import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.persistence.Persistence;
import org.hibernate.Version;
import org.junit.Test;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import common.dto.NhanVienDTO;
import common.dto.SanPhamDTO;
import common.protocol.Action;
import common.protocol.Request;

/** Kiểm tra dependency/DTO mà không mở Socket hoặc kết nối SQL Server. */
public class DependencySmokeTest {
    @Test
    public void jpaHibernateAndSqlServerDriverAreAvailable() throws Exception {
        assertNotNull(Persistence.class);
        assertEquals("5.6.15.Final", Version.getVersionString());
        assertNotNull(Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver"));
    }

    @Test
    public void localDateSurvivesJsonRoundTrip() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        NhanVienDTO employee = new NhanVienDTO();
        employee.setMaNV("NV001");
        employee.setNgaySinh(LocalDate.of(2000, 1, 2));
        NhanVienDTO restored = mapper.readValue(mapper.writeValueAsString(employee), NhanVienDTO.class);
        assertEquals(employee.getNgaySinh(), restored.getNgaySinh());
    }

    @Test
    public void requestPayloadPreservesItsTypeAndMoney() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        SanPhamDTO product = new SanPhamDTO();
        product.setMaSP("SP001");
        product.setGiaBan(new BigDecimal("250000.00"));
        Request<SanPhamDTO> request = new Request<SanPhamDTO>(
                "r1", Action.TRA_CUU_SAN_PHAM, null, product);
        Request<SanPhamDTO> restored = mapper.readValue(mapper.writeValueAsString(request),
                new TypeReference<Request<SanPhamDTO>>() { });
        assertEquals("SP001", restored.getPayload().getMaSP());
        assertEquals(0, product.getGiaBan().compareTo(restored.getPayload().getGiaBan()));
    }
}
