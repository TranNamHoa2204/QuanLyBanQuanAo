import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import com.fasterxml.jackson.databind.JsonNode;
import client.network.SocketClient;
import common.dto.ThongKeDoanhThuDTO;
import common.dto.ThongKeDoanhThuDongDTO;
import common.dto.ThongKeDoanhThuRequest;
import common.enums.NhomThongKe;
import common.enums.TrangThaiHoaDon;
import common.enums.VaiTro;
import common.protocol.Action;
import common.protocol.ErrorCode;
import common.protocol.JsonCodec;
import common.protocol.Response;
import server.dispatcher.RequestDispatcher;
import server.entity.BienThe;
import server.entity.ChiTietHoaDon;
import server.entity.HoaDon;
import server.entity.KhuyenMai;
import server.entity.LoaiSanPham;
import server.entity.SanPham;
import server.entity.Thue;
import server.entity.ThuongHieu;
import server.network.SocketServer;
import server.persistence.JpaManager;
import server.security.Pbkdf2PasswordHasher;
import server.service.AuthService;
import server.service.ThongKeService;
import server.session.SessionManager;

/** Thống kê thật qua TCP/JPA/H2; số liệu được dựng để có thể tính tay. */
public class ThongKeIntegrationTest {
    private static JpaManager jpa;
    private static SocketServer server;
    private static SessionManager sessions;
    private static String managerToken;
    private static String salesToken;

    @BeforeClass public static void setup() throws Exception {
        Map<String, Object> properties = CatalogTestSupport.memoryProperties("statistics_test");
        jpa = new JpaManager(properties);
        String hash = new Pbkdf2PasswordHasher().hash("StatsPass123!".toCharArray());
        jpa.transaction(em -> {
            CatalogTestSupport.user(em, "stats-manager", "NV1", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            CatalogTestSupport.user(em, "stats-sales", "NV2", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            LoaiSanPham category = new LoaiSanPham(); category.setMaLoai("L1"); category.setTenLoai("Áo quần"); em.persist(category);
            ThuongHieu brand = new ThuongHieu(); brand.setMaThuongHieu("TH1"); brand.setTenThuongHieu("Demo"); em.persist(brand);
            SanPham shirt = product("SP1", "Áo snapshot gốc", category, brand, "30.00"); em.persist(shirt);
            SanPham pants = product("SP2", "Quần snapshot gốc", category, brand, "70.00"); em.persist(pants);
            em.persist(variant("BT11", shirt));
            em.persist(variant("BT12", shirt));
            em.persist(variant("BT2", pants));
            Thue tax = new Thue(); tax.setMaThue("T1"); tax.setTenThue("Thuế demo"); tax.setPhanTram(new BigDecimal("10.00")); em.persist(tax);
            KhuyenMai promo = new KhuyenMai(); promo.setMaKM("KM1"); promo.setTenKM("Khuyến mãi demo");
            promo.setPhanTramGiam(new BigDecimal("10.00")); promo.setNgayBatDau(LocalDate.of(2020, 1, 1));
            promo.setNgayKetThuc(LocalDate.of(2099, 12, 31)); em.persist(promo);

            saveInvoice(em, "HD-DEC", LocalDateTime.of(2025, 12, 31, 23, 59, 59), TrangThaiHoaDon.DA_THANH_TOAN,
                    "100.00", "10.00", "9.00", "99.00", promo, tax,
                    line("BT11", "30.00", "Áo snapshot tháng 12"), line("BT2", "70.00", "Quần snapshot tháng 12"));
            saveInvoice(em, "HD-JAN", LocalDateTime.of(2026, 1, 1, 0, 0), TrangThaiHoaDon.DA_THANH_TOAN,
                    "100.00", "15.01", "8.50", "93.49", promo, tax,
                    line("BT11", "10.00", "Áo snapshot tháng 1"), line("BT12", "20.00", "Áo snapshot tháng 1"),
                    line("BT2", "70.00", "Quần snapshot tháng 1"));
            saveInvoice(em, "HD-PENNY", LocalDateTime.of(2026, 1, 15, 12, 0), TrangThaiHoaDon.DA_THANH_TOAN,
                    "2.00", "0.01", "0.00", "1.99", promo, tax,
                    line("BT11", "1.00", "Áo snapshot ngày 15"), line("BT2", "1.00", "Quần snapshot ngày 15"));
            saveInvoice(em, "HD-END", LocalDateTime.of(2026, 1, 31, 23, 59, 59), TrangThaiHoaDon.DA_THANH_TOAN,
                    "50.00", "0.00", "0.00", "50.00", promo, tax,
                    line("BT2", "50.00", "Quần snapshot cuối tháng"));
            saveInvoice(em, "HD-DRAFT", LocalDateTime.of(2026, 1, 20, 12, 0), TrangThaiHoaDon.NHAP,
                    "999.00", "0.00", "0.00", "999.00", promo, tax);
            saveInvoice(em, "HD-CANCEL", LocalDateTime.of(2026, 1, 21, 12, 0), TrangThaiHoaDon.DA_HUY,
                    "888.00", "0.00", "0.00", "888.00", promo, tax);
            saveInvoice(em, "HD-LEGACY", null, TrangThaiHoaDon.DA_THANH_TOAN,
                    "10.00", "0.00", "0.00", "10.00", promo, tax,
                    line("BT11", "10.00", null));
            saveInvoice(em, "HD-OLD-SNAPSHOT", LocalDateTime.of(2024, 4, 15, 10, 0), TrangThaiHoaDon.DA_THANH_TOAN,
                    "5.00", "0.00", "0.00", "5.00", promo, tax,
                    line("BT11", "5.00", null));
            saveInvoice(em, "HD-OLD-START", LocalDateTime.of(2024, 4, 1, 10, 0), TrangThaiHoaDon.DA_THANH_TOAN,
                    "4.00", "0.00", "0.00", "4.00", promo, tax,
                    line("BT11", "4.00", "Áo snapshot cũ hơn"));

            // Danh mục hiện tại đổi sau khi chứng từ đã chốt; thống kê không dùng giá/tỷ lệ mới.
            shirt.setTenSP("Áo tên danh mục mới"); shirt.setGiaBan(new BigDecimal("999.00"));
            pants.setTenSP("Quần tên danh mục mới"); pants.setGiaBan(new BigDecimal("888.00"));
            tax.setPhanTram(new BigDecimal("99.00")); promo.setPhanTramGiam(new BigDecimal("90.00"));
            return null;
        });

        jpa.transaction(em -> {
            em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics().setStatisticsEnabled(true);
            return null;
        });

        sessions = new SessionManager();
        AuthService auth = new AuthService(jpa, sessions, new Pbkdf2PasswordHasher());
        managerToken = sessions.create("stats-manager", hash);
        salesToken = sessions.create("stats-sales", hash);
        RequestDispatcher dispatcher = new RequestDispatcher(auth, null, null, null, null, null,
                new ThongKeService(jpa, auth));
        server = new SocketServer("127.0.0.1", 0, dispatcher);
        server.start();
    }

    @AfterClass public static void finish() {
        if (server != null) { server.close(); }
        if (jpa != null) { jpa.close(); }
    }

    @Test public void timeGroupsUsePaymentTimeAndExcludeDraftCancelledAndLegacyRows() throws Exception {
        ThongKeDoanhThuDTO daily = query(Action.THONG_KE_DOANH_THU_THEO_THOI_GIAN, managerToken,
                request("2025-12-31", "2026-01-31", NhomThongKe.NGAY));
        amounts(daily, "252.00", "25.02", "17.50", "226.98", "244.48");
        assertEquals(4L, daily.getSoHoaDon());
        assertEquals(4L, daily.getTotalItems());
        assertEquals(Arrays.asList("2025-12-31", "2026-01-01", "2026-01-15", "2026-01-31"), keys(daily.getItems()));
        assertEquals(1L, daily.getSoHoaDonThieuThoiDiemThanhToan());
        assertTrue(daily.getCanhBaoDuLieuCu().contains("đã loại khỏi báo cáo"));

        ThongKeDoanhThuDTO monthly = query(Action.THONG_KE_DOANH_THU_THEO_THOI_GIAN, managerToken,
                request("2025-12-31", "2026-01-31", NhomThongKe.THANG));
        assertEquals(Arrays.asList("2025-12", "2026-01"), keys(monthly.getItems()));
        amounts(monthly.getItems().get(1), "152.00", "15.02", "8.50", "136.98", "145.48");
        assertEquals(3L, monthly.getItems().get(1).getSoHoaDon());

        ThongKeDoanhThuDTO yearly = query(Action.THONG_KE_DOANH_THU_THEO_THOI_GIAN, managerToken,
                request("2025-12-31", "2026-01-31", NhomThongKe.NAM));
        assertEquals(Arrays.asList("2025", "2026"), keys(yearly.getItems()));
        assertEquals(1L, yearly.getItems().get(0).getSoHoaDon());
        assertEquals(3L, yearly.getItems().get(1).getSoHoaDon());
    }

    @Test public void productDiscountAndTaxAllocationMatchesInvoiceTotalsToTheCent() throws Exception {
        ThongKeDoanhThuDTO result = query(Action.THONG_KE_DOANH_THU_THEO_SAN_PHAM, managerToken,
                request("2025-12-31", "2026-01-31", null));
        amounts(result, "252.00", "25.02", "17.50", "226.98", "244.48");
        assertEquals(4L, result.getSoHoaDon());
        assertEquals(2L, result.getTotalItems());
        ThongKeDoanhThuDongDTO shirt = product(result.getItems(), "SP1");
        amounts(shirt, "61.00", "7.51", "5.25", "53.49", "58.74");
        assertEquals(3L, shirt.getSoHoaDon());
        assertEquals("Áo snapshot ngày 15", shirt.getTenSP());
        assertFalse(shirt.isTenSPHienHanhFallback());
        ThongKeDoanhThuDongDTO pants = product(result.getItems(), "SP2");
        amounts(pants, "191.00", "17.51", "12.25", "173.49", "185.74");
        assertEquals(4L, pants.getSoHoaDon());
        assertEquals("Quần snapshot cuối tháng", pants.getTenSP());
        assertFalse(pants.isTenSPHienHanhFallback());
    }

    @Test public void emptyRangePaginationAndValidationAreExplicit() throws Exception {
        ThongKeDoanhThuDTO empty = query(Action.THONG_KE_DOANH_THU_THEO_THOI_GIAN, managerToken,
                request("2023-04-01", "2023-04-30", NhomThongKe.NGAY));
        amounts(empty, "0.00", "0.00", "0.00", "0.00", "0.00");
        assertEquals(0L, empty.getSoHoaDon());
        assertTrue(empty.getItems().isEmpty());
        assertEquals(1L, empty.getSoHoaDonThieuThoiDiemThanhToan());

        ThongKeDoanhThuRequest firstPageRequest = request("2025-12-31", "2026-01-31", NhomThongKe.THANG);
        firstPageRequest.setPageSize(1);
        ThongKeDoanhThuDTO firstPage = query(Action.THONG_KE_DOANH_THU_THEO_THOI_GIAN, managerToken, firstPageRequest);
        assertEquals(2L, firstPage.getTotalItems());
        assertEquals(1, firstPage.getItems().size());
        assertEquals("2025-12", firstPage.getItems().get(0).getKhoa());
        firstPageRequest.setPage(1);
        ThongKeDoanhThuDTO secondPage = query(Action.THONG_KE_DOANH_THU_THEO_THOI_GIAN, managerToken, firstPageRequest);
        assertEquals("2026-01", secondPage.getItems().get(0).getKhoa());
        amounts(secondPage, "252.00", "25.02", "17.50", "226.98", "244.48");

        firstPageRequest.setPage(100001);
        assertError(ErrorCode.VALIDATION_ERROR, Action.THONG_KE_DOANH_THU_THEO_THOI_GIAN, managerToken, firstPageRequest);
        assertError(ErrorCode.VALIDATION_ERROR, Action.THONG_KE_DOANH_THU_THEO_SAN_PHAM, managerToken,
                request("2025-01-01", "2026-01-02", null));
    }

    @Test public void missingProductSnapshotIsMarkedWhenCurrentNameIsShownAsFallback() throws Exception {
        ThongKeDoanhThuDTO result = query(Action.THONG_KE_DOANH_THU_THEO_SAN_PHAM, managerToken,
                request("2024-04-01", "2024-04-30", null));
        assertEquals(1L, result.getTotalItems());
        ThongKeDoanhThuDongDTO shirt = product(result.getItems(), "SP1");
        assertEquals("Áo tên danh mục mới", shirt.getTenSP());
        assertTrue(shirt.isTenSPHienHanhFallback());
        amounts(shirt, "9.00", "0.00", "0.00", "9.00", "9.00");
    }

    @Test public void onlyManagersMayRequestStatistics() throws Exception {
        ThongKeDoanhThuRequest request = request("2026-01-01", "2026-01-31", NhomThongKe.NGAY);
        assertError(ErrorCode.FORBIDDEN, Action.THONG_KE_DOANH_THU_THEO_THOI_GIAN, salesToken, request);
        assertError(ErrorCode.UNAUTHENTICATED, Action.THONG_KE_DOANH_THU_THEO_SAN_PHAM, null,
                request("2026-01-01", "2026-01-31", null));
    }

    @Test public void statisticsQueryCountStaysBoundedForFiftyInvoices() throws Exception {
        jpa.transaction(em -> {
            KhuyenMai promo = em.find(KhuyenMai.class, "KM1");
            Thue tax = em.find(Thue.class, "T1");
            for (int i = 0; i < 50; i++) {
                String id = "HD-S24-" + String.format(java.util.Locale.ROOT, "%03d", i);
                saveInvoice(em, id, LocalDateTime.of(2030, 1, 1, 10, 0).plusSeconds(i),
                        TrangThaiHoaDon.DA_THANH_TOAN, "1.00", "0.00", "0.00", "1.00", promo, tax,
                        line("BT11", "1.00", "S24 benchmark"));
            }
            return null;
        });

        Statistics statistics = jpa.transaction(em ->
                em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics());
        ThongKeDoanhThuRequest timeRequest = request("2030-01-01", "2030-01-01", NhomThongKe.NGAY);
        ThongKeDoanhThuRequest productRequest = request("2030-01-01", "2030-01-01", null);
        query(Action.THONG_KE_DOANH_THU_THEO_THOI_GIAN, managerToken, timeRequest);
        query(Action.THONG_KE_DOANH_THU_THEO_SAN_PHAM, managerToken, productRequest);
        long[] byTime = measureStatistics(statistics, Action.THONG_KE_DOANH_THU_THEO_THOI_GIAN, timeRequest, 5);
        long[] byProduct = measureStatistics(statistics, Action.THONG_KE_DOANH_THU_THEO_SAN_PHAM, productRequest, 5);
        System.out.println("[S24 H2 stats] 50 invoices/50 lines: by-time statements/request=" + byTime[0]
                + ", average-ms/request=" + byTime[1] + "; by-product statements/request=" + byProduct[0]
                + ", average-ms/request=" + byProduct[1] + "; H2 only.");
        assertEquals("by-time uses bounded auth and projection queries", 4, byTime[0]);
        assertEquals("by-product uses bounded auth and projection queries", 5, byProduct[0]);
    }

    private static long[] measureStatistics(Statistics statistics, Action action,
            ThongKeDoanhThuRequest request, int repetitions) throws Exception {
        statistics.clear();
        long start = System.nanoTime();
        for (int i = 0; i < repetitions; i++) { query(action, managerToken, request); }
        long averageMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) / repetitions;
        return new long[] {statistics.getPrepareStatementCount() / repetitions, averageMillis};
    }

    private static ThongKeDoanhThuRequest request(String from, String until, NhomThongKe group) {
        ThongKeDoanhThuRequest request = new ThongKeDoanhThuRequest();
        request.setTuNgay(LocalDate.parse(from)); request.setDenNgay(LocalDate.parse(until));
        request.setNhomThoiGian(group); return request;
    }

    private static ThongKeDoanhThuDTO query(Action action, String token, ThongKeDoanhThuRequest request) throws Exception {
        try (SocketClient socket = new SocketClient("127.0.0.1", server.getPort())) {
            Response<JsonNode> response = socket.send(action, token, request);
            assertTrue(response.getMessage(), response.isSuccess());
            return JsonCodec.mapper().treeToValue(response.getPayload(), ThongKeDoanhThuDTO.class);
        }
    }

    private static void assertError(ErrorCode expected, Action action, String token, ThongKeDoanhThuRequest request) throws Exception {
        try (SocketClient socket = new SocketClient("127.0.0.1", server.getPort())) {
            Response<JsonNode> response = socket.send(action, token, request);
            assertFalse(response.isSuccess());
            assertEquals(expected, response.getErrorCode());
        }
    }

    private static void amounts(ThongKeDoanhThuDTO actual, String gross, String discount, String tax, String net, String paid) {
        assertMoney(gross, actual.getHangBanTruocGiam()); assertMoney(discount, actual.getTienGiam());
        assertMoney(tax, actual.getTienThue()); assertMoney(net, actual.getDoanhThuSauGiam());
        assertMoney(paid, actual.getTongKhachTra());
    }

    private static void amounts(ThongKeDoanhThuDongDTO actual, String gross, String discount, String tax, String net, String paid) {
        assertMoney(gross, actual.getHangBanTruocGiam()); assertMoney(discount, actual.getTienGiam());
        assertMoney(tax, actual.getTienThue()); assertMoney(net, actual.getDoanhThuSauGiam());
        assertMoney(paid, actual.getTongKhachTra());
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual); assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private static List<String> keys(List<ThongKeDoanhThuDongDTO> rows) {
        java.util.ArrayList<String> result = new java.util.ArrayList<String>();
        for (ThongKeDoanhThuDongDTO row : rows) { result.add(row.getKhoa()); }
        return result;
    }

    private static ThongKeDoanhThuDongDTO product(List<ThongKeDoanhThuDongDTO> rows, String id) {
        for (ThongKeDoanhThuDongDTO row : rows) { if (id.equals(row.getMaSP())) { return row; } }
        fail("Missing product " + id); return null;
    }

    private static SanPham product(String id, String name, LoaiSanPham category, ThuongHieu brand, String price) {
        SanPham product = new SanPham(); product.setMaSP(id); product.setTenSP(name);
        product.setLoaiSanPham(category); product.setThuongHieu(brand); product.setGiaBan(new BigDecimal(price));
        return product;
    }

    private static BienThe variant(String id, SanPham product) {
        BienThe variant = new BienThe(); variant.setMaBienThe(id); variant.setSanPham(product);
        variant.setMauSac(id); variant.setKichCo("M"); variant.setSoLuongTon(10); return variant;
    }

    private static Line line(String variantId, String price, String productNameSnapshot) {
        return new Line(variantId, new BigDecimal(price), productNameSnapshot);
    }

    private static void saveInvoice(javax.persistence.EntityManager em, String id, LocalDateTime paidAt,
            TrangThaiHoaDon status, String gross, String discount, String taxAmount, String paid,
            KhuyenMai promo, Thue tax, Line... lines) {
        HoaDon invoice = new HoaDon(); invoice.setMaHD(id);
        invoice.setNhanVien(em.find(server.entity.NhanVien.class, "NV1"));
        invoice.setThue(tax); invoice.setKhuyenMai(promo);
        invoice.setNgayLap(paidAt == null ? LocalDateTime.of(2020, 1, 1, 0, 0) : paidAt.minusMinutes(1));
        invoice.setNgayThanhToan(paidAt);
        invoice.setTamTinh(new BigDecimal(gross)); invoice.setPhanTramGiam(new BigDecimal("10.00"));
        invoice.setTienGiam(new BigDecimal(discount)); invoice.setPhanTramThue(new BigDecimal("10.00"));
        invoice.setTienThue(new BigDecimal(taxAmount)); invoice.setTongTien(new BigDecimal(paid));
        invoice.setTrangThai(status); em.persist(invoice);
        for (Line source : lines) {
            ChiTietHoaDon detail = new ChiTietHoaDon(); detail.setHoaDon(invoice);
            detail.setBienThe(em.find(BienThe.class, source.variantId)); detail.setSoLuong(1);
            detail.setDonGia(source.price); detail.setTenSPSnapshot(source.productNameSnapshot);
            detail.setMauSacSnapshot("snapshot-color"); detail.setKichCoSnapshot("M"); em.persist(detail);
        }
    }

    private static final class Line {
        final String variantId;
        final BigDecimal price;
        final String productNameSnapshot;
        Line(String variantId, BigDecimal price, String productNameSnapshot) {
            this.variantId = variantId; this.price = price; this.productNameSnapshot = productNameSnapshot;
        }
    }
}
