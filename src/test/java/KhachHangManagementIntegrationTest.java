import static org.junit.Assert.*;
import java.math.BigDecimal;
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
import server.service.KhachHangService;
import server.service.SanPhamService;
import server.session.SessionManager;

/** Khách hàng qua TCP + JPA/H2; không kết nối database SQL Server. */
public class KhachHangManagementIntegrationTest {
    private static final String PASSWORD = "CustomerPass123!";
    private JpaManager jpa;
    private SocketServer server;
    private SocketClient socket;
    private SessionManager sessions;
    private String managerToken, salesToken, secondSalesToken;

    @Before public void start() throws Exception {
        String database = "customer_test_" + Integer.toHexString(System.identityHashCode(this));
        jpa = new JpaManager(CatalogTestSupport.memoryProperties(database));
        String hash = new Pbkdf2PasswordHasher().hash(PASSWORD.toCharArray());
        jpa.transaction(em -> {
            CatalogTestSupport.user(em, "manager", "NV01", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            CatalogTestSupport.user(em, "sales", "NV02", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            CatalogTestSupport.user(em, "sales2", "NV03", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            LoaiSanPham category = new LoaiSanPham(); category.setMaLoai("L1"); category.setTenLoai("Áo"); em.persist(category);
            ThuongHieu brand = new ThuongHieu(); brand.setMaThuongHieu("TH1"); brand.setTenThuongHieu("Demo"); em.persist(brand);
            SanPham product = new SanPham(); product.setMaSP("SP1"); product.setTenSP("Áo thử");
            product.setLoaiSanPham(category); product.setThuongHieu(brand); product.setGiaBan(new BigDecimal("100")); em.persist(product);
            BienThe variant = new BienThe(); variant.setMaBienThe("BT1"); variant.setSanPham(product);
            variant.setMauSac("Đen"); variant.setKichCo("M"); variant.setSoLuongTon(10); em.persist(variant);
            Thue tax = new Thue(); tax.setMaThue("T1"); tax.setTenThue("Thuế thử");
            tax.setPhanTram(new BigDecimal("0")); em.persist(tax);
            return null;
        });
        sessions = new SessionManager();
        AuthService auth = new AuthService(jpa, sessions, new Pbkdf2PasswordHasher());
        KhachHangService customers = new KhachHangService(jpa, auth);
        server = new SocketServer("127.0.0.1", 0, new RequestDispatcher(auth,
                new SanPhamService(jpa, auth), new HoaDonService(jpa, auth), null, customers));
        server.start();
        managerToken = sessions.create("manager", hash); salesToken = sessions.create("sales", hash);
        secondSalesToken = sessions.create("sales2", hash);
        socket = new SocketClient("127.0.0.1", server.getPort());
    }

    @After public void stop() {
        if (socket != null) { socket.close(); }
        if (server != null) { server.close(); }
        if (jpa != null) { jpa.close(); }
    }

    @Test public void bothEmployeeRolesCanManageCustomersAndSearchIsPagedByCodeNameAndPhone() throws Exception {
        assertTrue(send(Action.THEM_KHACH_HANG, managerToken, customer("KH03", "Thảo", "0900000003", null)).isSuccess());
        assertTrue(send(Action.THEM_KHACH_HANG, salesToken, customer("KH01", "Nguyễn An", "0900000001", null)).isSuccess());
        assertTrue(send(Action.THEM_KHACH_HANG, salesToken, customer("KH02", "Nguyễn Bình", "0900000002", null)).isSuccess());

        KhachHangSearchRequest search = new KhachHangSearchRequest(); search.setTuKhoa("Nguyễn"); search.setPageSize(1);
        KhachHangSearchResult first = read(send(Action.TRA_CUU_KHACH_HANG, managerToken, search), KhachHangSearchResult.class);
        assertEquals(2L, first.getTotal()); assertEquals("KH01", first.getItems().get(0).getMaKH());
        search.setPage(1);
        assertEquals("KH02", read(send(Action.TRA_CUU_KHACH_HANG, salesToken, search), KhachHangSearchResult.class)
                .getItems().get(0).getMaKH());
        search.setPage(0); search.setPageSize(20); search.setTuKhoa("0900000002");
        assertEquals("KH02", read(send(Action.TRA_CUU_KHACH_HANG, salesToken, search), KhachHangSearchResult.class)
                .getItems().get(0).getMaKH());
        search.setTuKhoa("kh03");
        assertEquals("KH03", read(send(Action.TRA_CUU_KHACH_HANG, salesToken, search), KhachHangSearchResult.class)
                .getItems().get(0).getMaKH());
        assertError(ErrorCode.UNAUTHENTICATED, send(Action.TRA_CUU_KHACH_HANG, null, new KhachHangSearchRequest()));
    }

    @Test public void createNormalizesUnicodePhoneAndNullsAndRejectsDuplicatesAndInvalidValues() throws Exception {
        KhachHangDTO created = read(send(Action.THEM_KHACH_HANG, salesToken,
                customer(" kh10 ", "\u00a0Cafe\u0301 Demo\u00a0", " 090 (123)-4567 ", " user@xn--bro-vla.example ")),
                KhachHangDTO.class);
        assertEquals("KH10", created.getMaKH()); assertEquals("Café Demo", created.getHoTen());
        assertEquals("0901234567", created.getSdt()); assertEquals("user@xn--bro-vla.example", created.getEmail());
        assertEquals("0901234567", jpa.transaction(em -> em.find(KhachHang.class, "KH10").getSdt()));
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_KHACH_HANG, salesToken,
                customer("KH11", "Khách trùng", "090.123.4567", null)));

        KhachHangDTO noPhone = read(send(Action.THEM_KHACH_HANG, salesToken, customer("KH12", "Khách không SĐT", "   ", null)), KhachHangDTO.class);
        assertNull(noPhone.getSdt()); assertNull(jpa.transaction(em -> em.find(KhachHang.class, "KH12").getSdt()));
        assertTrue(send(Action.THEM_KHACH_HANG, managerToken, customer("KH13", "Khách không SĐT 2", null, null)).isSuccess());

        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_KHACH_HANG, salesToken,
                customer("KH14", "Sai email", null, "bad@@example.com")));
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_KHACH_HANG, salesToken,
                customer("KH15", "Sai SĐT", "090-abc", null)));
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_KHACH_HANG, salesToken,
                customer("KH16", " ", null, null)));
        char[] longAddress = new char[256]; java.util.Arrays.fill(longAddress, 'x');
        KhachHangDTO tooLong = customer("KH17", "Tên", null, null); tooLong.setDiaChi(new String(longAddress));
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_KHACH_HANG, salesToken, tooLong));
    }

    @Test public void updateAndStateChangesRejectStaleSnapshotsAndKeepInactiveCustomerReadable() throws Exception {
        KhachHangDTO original = create("KH20", "Khách cũ", "0900020", "old@example.com");
        KhachHangDTO changed = copy(original); changed.setHoTen("Khách mới"); changed.setEmail("new@example.com");
        KhachHangSaveRequest update = new KhachHangSaveRequest(); update.setExpected(original); update.setValue(changed);
        KhachHangDTO saved = read(send(Action.CAP_NHAT_KHACH_HANG, salesToken, update), KhachHangDTO.class);
        assertEquals("Khách mới", saved.getHoTen()); assertEquals("new@example.com", saved.getEmail());
        assertError(ErrorCode.VERSION_CONFLICT, send(Action.CAP_NHAT_KHACH_HANG, managerToken, update));

        KhachHangStatusRequest status = status("KH20", true);
        KhachHangDTO inactive = read(send(Action.NGUNG_HOAT_DONG_KHACH_HANG, salesToken, status), KhachHangDTO.class);
        assertFalse(inactive.getTrangThai());
        KhachHangDetailRequest detail = new KhachHangDetailRequest(); detail.setMaKH("KH20");
        assertFalse(read(send(Action.XEM_CHI_TIET_KHACH_HANG, salesToken, detail), KhachHangDTO.class).getTrangThai());
        assertError(ErrorCode.VERSION_CONFLICT, send(Action.NGUNG_HOAT_DONG_KHACH_HANG, managerToken, status));
        status.setExpectedTrangThai(false);
        assertTrue(read(send(Action.KICH_HOAT_KHACH_HANG, managerToken, status), KhachHangDTO.class).getTrangThai());
    }

    @Test public void concurrentEditsFromSameSnapshotHaveOneWinner() throws Exception {
        KhachHangDTO original = create("KH30", "Cũ", null, null);
        KhachHangDTO firstValue = copy(original); firstValue.setHoTen("Người sửa 1");
        KhachHangDTO secondValue = copy(original); secondValue.setHoTen("Người sửa 2");
        KhachHangSaveRequest firstRequest = save(original, firstValue), secondRequest = save(original, secondValue);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (SocketClient first = new SocketClient("127.0.0.1", server.getPort());
             SocketClient second = new SocketClient("127.0.0.1", server.getPort())) {
            Future<Response<JsonNode>> a = workers.submit(() -> { ready.countDown(); start.await(); return first.send(Action.CAP_NHAT_KHACH_HANG, salesToken, firstRequest); });
            Future<Response<JsonNode>> b = workers.submit(() -> { ready.countDown(); start.await(); return second.send(Action.CAP_NHAT_KHACH_HANG, secondSalesToken, secondRequest); });
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown();
            Response<JsonNode> ra = a.get(20, TimeUnit.SECONDS), rb = b.get(20, TimeUnit.SECONDS);
            assertEquals(1, (ra.isSuccess() ? 1 : 0) + (rb.isSuccess() ? 1 : 0));
            Response<JsonNode> rejected = ra.isSuccess() ? rb : ra;
            assertEquals(ErrorCode.VERSION_CONFLICT, rejected.getErrorCode());
        } finally { workers.shutdownNow(); }
    }

    @Test public void inactiveCustomerCannotStartOrPaySaleButPaidInvoiceHistoryRemainsLinked() throws Exception {
        create("KH40", "Khách mua", null, null);
        HoaDonDetailDTO paidDraft = invoice("HD40", "KH40"); paidDraft = addLine(paidDraft);
        paidDraft = read(send(Action.THANH_TOAN_HOA_DON, salesToken, payment(paidDraft)), HoaDonDetailDTO.class);
        assertEquals("DA_THANH_TOAN", paidDraft.getHoaDon().getTrangThai().name());

        HoaDonDetailDTO openDraft = invoice("HD41", "KH40"); openDraft = addLine(openDraft);
        KhachHangDTO inactive = read(send(Action.NGUNG_HOAT_DONG_KHACH_HANG, salesToken, status("KH40", true)), KhachHangDTO.class);
        assertFalse(inactive.getTrangThai());
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THANH_TOAN_HOA_DON, salesToken, payment(openDraft)));
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.LAP_HOA_DON, salesToken, invoiceRequest("HD42", "KH40")));
        HoaDonVersionRequest detail = new HoaDonVersionRequest(); detail.setMaHD("HD40");
        HoaDonDetailDTO historical = read(send(Action.XEM_CHI_TIET_HOA_DON, salesToken, detail), HoaDonDetailDTO.class);
        assertEquals("KH40", historical.getHoaDon().getMaKH());
    }

    private KhachHangDTO create(String id, String name, String phone, String email) throws Exception {
        return read(send(Action.THEM_KHACH_HANG, salesToken, customer(id, name, phone, email)), KhachHangDTO.class);
    }
    private HoaDonDetailDTO invoice(String id, String customerId) throws Exception {
        return read(send(Action.LAP_HOA_DON, salesToken, invoiceRequest(id, customerId)), HoaDonDetailDTO.class);
    }
    private HoaDonDetailDTO addLine(HoaDonDetailDTO invoice) throws Exception {
        HoaDonLineRequest request = new HoaDonLineRequest(); request.setMaHD(invoice.getHoaDon().getMaHD());
        request.setPhienBan(invoice.getHoaDon().getPhienBan()); request.setMaBienThe("BT1"); request.setSoLuong(1);
        return read(send(Action.THEM_CHI_TIET_HOA_DON, salesToken, request), HoaDonDetailDTO.class);
    }
    private static HoaDonCreateRequest invoiceRequest(String id, String customerId) {
        HoaDonCreateRequest request = new HoaDonCreateRequest(); request.setMaHD(id); request.setMaThue("T1");
        request.setMaKH(customerId); return request;
    }
    private static HoaDonVersionRequest version(HoaDonDetailDTO detail) {
        HoaDonVersionRequest request = new HoaDonVersionRequest(); request.setMaHD(detail.getHoaDon().getMaHD());
        request.setPhienBan(detail.getHoaDon().getPhienBan()); return request;
    }
    private static HoaDonThanhToanRequest payment(HoaDonDetailDTO detail) {
        HoaDonThanhToanRequest request = new HoaDonThanhToanRequest(); request.setMaHD(detail.getHoaDon().getMaHD());
        request.setPhienBan(detail.getHoaDon().getPhienBan()); request.setTongTienDaXem(detail.getHoaDon().getTongTien());
        request.setPhuongThucThanhToan(PhuongThucThanhToan.TIEN_MAT); request.setTienKhachDua(detail.getHoaDon().getTongTien());
        return request;
    }
    private Response<JsonNode> send(Action action, String token, Object payload) throws Exception { return socket.send(action, token, payload); }
    private static <T> T read(Response<JsonNode> response, Class<T> type) throws Exception {
        assertTrue(response.getMessage(), response.isSuccess());
        return JsonCodec.mapper().treeToValue(response.getPayload(), type);
    }
    private static void assertError(ErrorCode expected, Response<JsonNode> response) {
        assertFalse("Expected " + expected + " but got " + response.getErrorCode() + ": " + response.getMessage(), response.isSuccess());
        assertEquals(expected, response.getErrorCode());
    }
    private static KhachHangDTO customer(String id, String name, String phone, String email) {
        KhachHangDTO dto = new KhachHangDTO(); dto.setMaKH(id); dto.setHoTen(name); dto.setSdt(phone); dto.setEmail(email); return dto;
    }
    private static KhachHangDTO copy(KhachHangDTO source) {
        KhachHangDTO dto = new KhachHangDTO(); dto.setMaKH(source.getMaKH()); dto.setHoTen(source.getHoTen());
        dto.setSdt(source.getSdt()); dto.setEmail(source.getEmail()); dto.setDiaChi(source.getDiaChi());
        dto.setTrangThai(source.getTrangThai()); return dto;
    }
    private static KhachHangSaveRequest save(KhachHangDTO expected, KhachHangDTO value) {
        KhachHangSaveRequest request = new KhachHangSaveRequest(); request.setExpected(expected); request.setValue(value); return request;
    }
    private static KhachHangStatusRequest status(String id, boolean expected) {
        KhachHangStatusRequest request = new KhachHangStatusRequest(); request.setMaKH(id); request.setExpectedTrangThai(expected); return request;
    }
}
