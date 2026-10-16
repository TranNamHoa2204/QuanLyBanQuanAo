import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.JsonNode;
import client.network.SocketClient;
import common.dto.*;
import common.enums.TrangThaiHieuLucKhuyenMai;
import common.enums.TrangThaiHoaDon;
import common.enums.PhuongThucThanhToan;
import common.enums.VaiTro;
import common.protocol.Action;
import common.protocol.ErrorCode;
import common.protocol.JsonCodec;
import common.protocol.Response;
import server.dispatcher.RequestDispatcher;
import server.entity.*;
import server.network.SocketServer;
import server.persistence.JpaManager;
import server.security.Pbkdf2PasswordHasher;
import server.service.AuthService;
import server.service.HoaDonService;
import server.service.KhuyenMaiService;
import server.service.SanPhamService;
import server.session.SessionManager;

/** Khuyến mãi qua TCP + JPA/H2; không kết nối SQL Server. */
public class KhuyenMaiManagementIntegrationTest {
    private static final String PASSWORD = "PromotionPass123!";
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private JpaManager jpa;
    private SocketServer server;
    private SocketClient socket;
    private String managerToken, salesToken, secondManagerToken;
    private LocalDate today;

    @Before public void start() throws Exception {
        today = LocalDate.now(ZONE);
        Clock clock = Clock.fixed(today.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
        String database = "promotion_test_" + Integer.toHexString(System.identityHashCode(this));
        jpa = new JpaManager(CatalogTestSupport.memoryProperties(database));
        String hash = new Pbkdf2PasswordHasher().hash(PASSWORD.toCharArray());
        jpa.transaction(em -> {
            CatalogTestSupport.user(em, "manager", "NV01", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            CatalogTestSupport.user(em, "manager2", "NV02", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            CatalogTestSupport.user(em, "sales", "NV03", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            LoaiSanPham category = new LoaiSanPham(); category.setMaLoai("L1"); category.setTenLoai("Áo"); em.persist(category);
            ThuongHieu brand = new ThuongHieu(); brand.setMaThuongHieu("TH1"); brand.setTenThuongHieu("Demo"); em.persist(brand);
            SanPham product = new SanPham(); product.setMaSP("SP1"); product.setTenSP("Áo thử");
            product.setLoaiSanPham(category); product.setThuongHieu(brand); product.setGiaBan(new BigDecimal("100.00")); em.persist(product);
            BienThe variant = new BienThe(); variant.setMaBienThe("BT1"); variant.setSanPham(product);
            variant.setMauSac("Đen"); variant.setKichCo("M"); variant.setSoLuongTon(10); em.persist(variant);
            Thue tax = new Thue(); tax.setMaThue("T1"); tax.setTenThue("Thuế thử");
            tax.setPhanTram(new BigDecimal("0.00")); em.persist(tax);
            return null;
        });
        SessionManager sessions = new SessionManager();
        AuthService auth = new AuthService(jpa, sessions, new Pbkdf2PasswordHasher());
        KhuyenMaiService promotions = new KhuyenMaiService(jpa, auth, clock);
        server = new SocketServer("127.0.0.1", 0, new RequestDispatcher(auth,
                new SanPhamService(jpa, auth), new HoaDonService(jpa, auth), null, null, promotions));
        server.start();
        managerToken = sessions.create("manager", hash); secondManagerToken = sessions.create("manager2", hash);
        salesToken = sessions.create("sales", hash); socket = new SocketClient("127.0.0.1", server.getPort());
    }

    @After public void stop() {
        if (socket != null) { socket.close(); }
        if (server != null) { server.close(); }
        if (jpa != null) { jpa.close(); }
    }

    @Test public void salesSeesInclusiveStartAndEndDaysWhileManagerSeesDerivedStatesAndPagination() throws Exception {
        create("KM01", new BigDecimal("10"), today, today.plusDays(1));
        create("KM02", new BigDecimal("20"), today.minusDays(1), today);
        create("KM03", new BigDecimal("30"), today.plusDays(1), today.plusDays(2));
        create("KM04", new BigDecimal("40"), today.minusDays(2), today.minusDays(1));
        KhuyenMaiDTO disabled = create("KM05", new BigDecimal("50"), today.minusDays(1), today.plusDays(1));
        read(send(Action.NGUNG_HOAT_DONG_KHUYEN_MAI, managerToken, status(disabled)), KhuyenMaiDTO.class);

        KhuyenMaiSearchRequest search = new KhuyenMaiSearchRequest(); search.setPageSize(1);
        KhuyenMaiSearchResult first = read(send(Action.TRA_CUU_KHUYEN_MAI, salesToken, search), KhuyenMaiSearchResult.class);
        assertEquals(2L, first.getTotal()); assertEquals("KM01", first.getItems().get(0).getMaKM());
        assertEquals(TrangThaiHieuLucKhuyenMai.DANG_DIEN_RA, first.getItems().get(0).getTrangThaiHieuLuc());
        search.setPage(1);
        KhuyenMaiSearchResult second = read(send(Action.TRA_CUU_KHUYEN_MAI, salesToken, search), KhuyenMaiSearchResult.class);
        assertEquals("KM02", second.getItems().get(0).getMaKM());

        search.setPage(0); search.setPageSize(20);
        KhuyenMaiSearchResult all = read(send(Action.TRA_CUU_KHUYEN_MAI, managerToken, search), KhuyenMaiSearchResult.class);
        assertEquals(5L, all.getTotal());
        assertEquals(TrangThaiHieuLucKhuyenMai.CHUA_BAT_DAU, find(all, "KM03").getTrangThaiHieuLuc());
        assertEquals(TrangThaiHieuLucKhuyenMai.HET_HAN, find(all, "KM04").getTrangThaiHieuLuc());
        assertFalse(find(all, "KM05").getTrangThai());
        assertEquals(TrangThaiHieuLucKhuyenMai.DANG_DIEN_RA, find(all, "KM05").getTrangThaiHieuLuc());
        search.setTrangThai(false);
        assertEquals(1L, read(send(Action.TRA_CUU_KHUYEN_MAI, managerToken, search), KhuyenMaiSearchResult.class).getTotal());
        assertError(ErrorCode.UNAUTHENTICATED, send(Action.TRA_CUU_KHUYEN_MAI, null, new KhuyenMaiSearchRequest()));
    }

    @Test public void managerCreateValidatesScaleRangeDatesUnicodeLengthAndDuplicateCode() throws Exception {
        KhuyenMaiDTO zero = read(send(Action.THEM_KHUYEN_MAI, managerToken,
                promo("KM10", "  Cafe\u0301 Ưu đãi  ", new BigDecimal("0"), today, today)), KhuyenMaiDTO.class);
        assertEquals("Café Ưu đãi", zero.getTenKM()); assertEquals(0, zero.getPhanTramGiam().compareTo(BigDecimal.ZERO));
        assertTrue(java.text.Normalizer.isNormalized(zero.getTenKM(), java.text.Normalizer.Form.NFC));
        KhuyenMaiDTO hundred = create("KM11", new BigDecimal("100.00"), today, today.plusDays(2));
        assertEquals(0, hundred.getPhanTramGiam().compareTo(new BigDecimal("100.00")));

        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_KHUYEN_MAI, managerToken,
                promo("km10", "Trùng mã", BigDecimal.ONE, today, today)));
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_KHUYEN_MAI, managerToken,
                promo("KM12", "Âm", new BigDecimal("-0.01"), today, today)));
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_KHUYEN_MAI, managerToken,
                promo("KM13", "Quá 100", new BigDecimal("100.01"), today, today)));
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_KHUYEN_MAI, managerToken,
                promo("KM14", "Sai scale", new BigDecimal("10.001"), today, today)));
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_KHUYEN_MAI, managerToken,
                promo("KM15", "Sai ngày", BigDecimal.TEN, today.plusDays(1), today)));
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_KHUYEN_MAI, managerToken,
                promo("KM16", "   ", BigDecimal.TEN, today, today)));
        char[] longName = new char[201]; java.util.Arrays.fill(longName, 'x');
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_KHUYEN_MAI, managerToken,
                promo("KM17", new String(longName), BigDecimal.TEN, today, today)));
        assertError(ErrorCode.FORBIDDEN, send(Action.THEM_KHUYEN_MAI, salesToken,
                promo("KM18", "Không đủ quyền", BigDecimal.TEN, today, today)));
    }

    @Test public void updateAndStatusTransitionsRequireFreshFullSnapshot() throws Exception {
        KhuyenMaiDTO original = create("KM20", BigDecimal.TEN, today, today.plusDays(5));
        KhuyenMaiDTO changed = copy(original); changed.setTenKM("Tên mới"); changed.setPhanTramGiam(new BigDecimal("12.50"));
        KhuyenMaiSaveRequest update = save(original, changed);
        KhuyenMaiDTO saved = read(send(Action.CAP_NHAT_KHUYEN_MAI, managerToken, update), KhuyenMaiDTO.class);
        assertEquals("Tên mới", saved.getTenKM()); assertEquals(0, saved.getPhanTramGiam().compareTo(new BigDecimal("12.50")));
        assertError(ErrorCode.VERSION_CONFLICT, send(Action.CAP_NHAT_KHUYEN_MAI, secondManagerToken, update));
        assertError(ErrorCode.FORBIDDEN, send(Action.CAP_NHAT_KHUYEN_MAI, salesToken, save(saved, changed)));

        KhuyenMaiDTO inactive = read(send(Action.NGUNG_HOAT_DONG_KHUYEN_MAI, managerToken, status(saved)), KhuyenMaiDTO.class);
        assertFalse(inactive.getTrangThai());
        assertError(ErrorCode.VERSION_CONFLICT, send(Action.KICH_HOAT_KHUYEN_MAI, secondManagerToken, status(saved)));
        KhuyenMaiDetailRequest detail = new KhuyenMaiDetailRequest(); detail.setMaKM("KM20");
        assertError(ErrorCode.NOT_FOUND, send(Action.XEM_CHI_TIET_KHUYEN_MAI, salesToken, detail));
        KhuyenMaiDTO active = read(send(Action.KICH_HOAT_KHUYEN_MAI, secondManagerToken, status(inactive)), KhuyenMaiDTO.class);
        assertTrue(active.getTrangThai());
        assertEquals("KM20", read(send(Action.XEM_CHI_TIET_KHUYEN_MAI, salesToken, detail), KhuyenMaiDTO.class).getMaKM());
    }

    @Test public void simultaneousUpdatesFromOneSnapshotHaveOneWinner() throws Exception {
        KhuyenMaiDTO original = create("KM30", BigDecimal.TEN, today, today.plusDays(1));
        KhuyenMaiDTO firstValue = copy(original); firstValue.setTenKM("Sửa 1");
        KhuyenMaiDTO secondValue = copy(original); secondValue.setTenKM("Sửa 2");
        KhuyenMaiSaveRequest firstRequest = save(original, firstValue), secondRequest = save(original, secondValue);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (SocketClient first = new SocketClient("127.0.0.1", server.getPort());
             SocketClient second = new SocketClient("127.0.0.1", server.getPort())) {
            Future<Response<JsonNode>> a = workers.submit(() -> { ready.countDown(); start.await(); return first.send(Action.CAP_NHAT_KHUYEN_MAI, managerToken, firstRequest); });
            Future<Response<JsonNode>> b = workers.submit(() -> { ready.countDown(); start.await(); return second.send(Action.CAP_NHAT_KHUYEN_MAI, secondManagerToken, secondRequest); });
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown();
            Response<JsonNode> ra = a.get(20, TimeUnit.SECONDS), rb = b.get(20, TimeUnit.SECONDS);
            assertEquals(1, (ra.isSuccess() ? 1 : 0) + (rb.isSuccess() ? 1 : 0));
            assertEquals(ErrorCode.VERSION_CONFLICT, (ra.isSuccess() ? rb : ra).getErrorCode());
        } finally { workers.shutdownNow(); }
    }

    @Test public void editingPromotionDoesNotRewritePaidInvoiceDiscountSnapshot() throws Exception {
        KhuyenMaiDTO promo = create("KM40", new BigDecimal("10.00"), today, today.plusDays(1));
        HoaDonCreateRequest createInvoice = new HoaDonCreateRequest(); createInvoice.setMaHD("HD40");
        createInvoice.setMaThue("T1"); createInvoice.setMaKM("KM40");
        HoaDonDetailDTO invoice = read(send(Action.LAP_HOA_DON, salesToken, createInvoice), HoaDonDetailDTO.class);
        HoaDonLineRequest line = new HoaDonLineRequest(); line.setMaHD("HD40");
        line.setPhienBan(invoice.getHoaDon().getPhienBan()); line.setMaBienThe("BT1"); line.setSoLuong(1);
        invoice = read(send(Action.THEM_CHI_TIET_HOA_DON, salesToken, line), HoaDonDetailDTO.class);
        HoaDonThanhToanRequest payment = new HoaDonThanhToanRequest(); payment.setMaHD("HD40");
        payment.setPhienBan(invoice.getHoaDon().getPhienBan()); payment.setTongTienDaXem(invoice.getHoaDon().getTongTien());
        payment.setPhuongThucThanhToan(PhuongThucThanhToan.TIEN_MAT); payment.setTienKhachDua(invoice.getHoaDon().getTongTien());
        invoice = read(send(Action.THANH_TOAN_HOA_DON, salesToken, payment), HoaDonDetailDTO.class);
        assertEquals(TrangThaiHoaDon.DA_THANH_TOAN, invoice.getHoaDon().getTrangThai());
        assertEquals(0, invoice.getHoaDon().getPhanTramGiam().compareTo(new BigDecimal("10.00")));
        assertEquals(0, invoice.getHoaDon().getTienGiam().compareTo(new BigDecimal("10.00")));

        KhuyenMaiDTO changed = copy(promo); changed.setPhanTramGiam(new BigDecimal("25.00"));
        KhuyenMaiDTO saved = read(send(Action.CAP_NHAT_KHUYEN_MAI, managerToken, save(promo, changed)), KhuyenMaiDTO.class);
        read(send(Action.NGUNG_HOAT_DONG_KHUYEN_MAI, managerToken, status(saved)), KhuyenMaiDTO.class);
        HoaDonVersionRequest detail = new HoaDonVersionRequest(); detail.setMaHD("HD40");
        HoaDonDetailDTO history = read(send(Action.XEM_CHI_TIET_HOA_DON, salesToken, detail), HoaDonDetailDTO.class);
        assertEquals("KM40", history.getHoaDon().getMaKM());
        assertEquals(0, history.getHoaDon().getPhanTramGiam().compareTo(new BigDecimal("10.00")));
        assertEquals(0, history.getHoaDon().getTienGiam().compareTo(new BigDecimal("10.00")));
    }

    private KhuyenMaiDTO create(String id, BigDecimal rate, LocalDate start, LocalDate end) throws Exception {
        return read(send(Action.THEM_KHUYEN_MAI, managerToken, promo(id, "Chương trình " + id, rate, start, end)), KhuyenMaiDTO.class);
    }
    private Response<JsonNode> send(Action action, String token, Object payload) throws Exception { return socket.send(action, token, payload); }
    private static <T> T read(Response<JsonNode> response, Class<T> type) throws Exception {
        assertTrue(response.getMessage(), response.isSuccess()); return JsonCodec.mapper().treeToValue(response.getPayload(), type);
    }
    private static void assertError(ErrorCode expected, Response<JsonNode> response) {
        assertFalse("Expected " + expected + " but got " + response.getErrorCode() + ": " + response.getMessage(), response.isSuccess());
        assertEquals(expected, response.getErrorCode());
    }
    private static KhuyenMaiDTO promo(String id, String name, BigDecimal rate, LocalDate start, LocalDate end) {
        KhuyenMaiDTO dto = new KhuyenMaiDTO(); dto.setMaKM(id); dto.setTenKM(name); dto.setPhanTramGiam(rate);
        dto.setNgayBatDau(start); dto.setNgayKetThuc(end); return dto;
    }
    private static KhuyenMaiDTO find(KhuyenMaiSearchResult result, String id) {
        return result.getItems().stream().filter(row -> row.getMaKM().equals(id)).findFirst().orElseThrow(AssertionError::new);
    }
    private static KhuyenMaiDTO copy(KhuyenMaiDTO source) {
        KhuyenMaiDTO dto = promo(source.getMaKM(), source.getTenKM(), source.getPhanTramGiam(), source.getNgayBatDau(), source.getNgayKetThuc());
        dto.setTrangThai(source.getTrangThai()); dto.setTrangThaiHieuLuc(source.getTrangThaiHieuLuc()); return dto;
    }
    private static KhuyenMaiSaveRequest save(KhuyenMaiDTO expected, KhuyenMaiDTO value) {
        KhuyenMaiSaveRequest request = new KhuyenMaiSaveRequest(); request.setExpected(expected); request.setValue(value); return request;
    }
    private static KhuyenMaiStatusRequest status(KhuyenMaiDTO expected) {
        KhuyenMaiStatusRequest request = new KhuyenMaiStatusRequest(); request.setExpected(expected); return request;
    }
}
