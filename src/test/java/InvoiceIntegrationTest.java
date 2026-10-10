import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import org.junit.*;
import client.api.*;
import client.network.SocketClient;
import client.session.ClientSession;
import common.dto.*;
import common.enums.*;
import common.protocol.ErrorCode;
import server.entity.*;
import server.service.*;
import server.persistence.JpaManager;
import server.security.Pbkdf2PasswordHasher;
import server.session.SessionManager;
import server.dispatcher.RequestDispatcher;
import server.network.SocketServer;

/** Real TCP + JPA/H2. Không kết nối hoặc sửa SQL Server của người dùng. */
public class InvoiceIntegrationTest {
    private static JpaManager jpa;
    private static SocketServer server;
    private static SessionManager sessions;
    private static String hash;
    private SocketClient socket;
    private HoaDonApi api;
    @BeforeClass public static void setup() throws Exception {
        jpa = new JpaManager(CatalogTestSupport.memoryProperties("invoice_test"));
        hash = new Pbkdf2PasswordHasher().hash("InvoicePass123!".toCharArray());
        jpa.transaction(em -> {
            CatalogTestSupport.user(em, "sales", "NV1", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            CatalogTestSupport.user(em, "other", "NV2", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            CatalogTestSupport.user(em, "manager", "NV3", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            LoaiSanPham l = new LoaiSanPham(); l.setMaLoai("L1"); l.setTenLoai("Áo"); em.persist(l);
            ThuongHieu t = new ThuongHieu(); t.setMaThuongHieu("TH1"); t.setTenThuongHieu("Demo"); em.persist(t);
            SanPham p = new SanPham(); p.setMaSP("SP1"); p.setTenSP("Áo thử"); p.setLoaiSanPham(l);
            p.setThuongHieu(t); p.setGiaBan(new BigDecimal("100")); em.persist(p);
            for (String id : new String[]{"BT1", "BT2"}) {
                BienThe b = new BienThe(); b.setMaBienThe(id); b.setSanPham(p); b.setMauSac(id);
                b.setKichCo("M"); b.setSoLuongTon(5); em.persist(b);
            }
            Thue tax = new Thue(); tax.setMaThue("T1"); tax.setTenThue("Thuế thử"); tax.setPhanTram(new BigDecimal("10")); em.persist(tax);
            KhuyenMai km = new KhuyenMai(); km.setMaKM("KM1"); km.setTenKM("Giảm thử"); km.setPhanTramGiam(new BigDecimal("10"));
            km.setNgayBatDau(java.time.LocalDate.of(2020, 1, 1)); km.setNgayKetThuc(java.time.LocalDate.of(2099, 1, 1)); em.persist(km);
            return null;
        });
        sessions = new SessionManager(); AuthService auth = new AuthService(jpa, sessions, new Pbkdf2PasswordHasher());
        server = new SocketServer("127.0.0.1", 0, new RequestDispatcher(auth, new SanPhamService(jpa, auth), new HoaDonService(jpa, auth)));
        server.start();
    }
    @Before public void reset() {
        jpa.transaction(em -> {
            em.createQuery("delete from ChiTietHoaDon").executeUpdate(); em.createQuery("delete from HoaDon").executeUpdate();
            em.find(BienThe.class, "BT1").setSoLuongTon(5); em.find(BienThe.class, "BT2").setSoLuongTon(5);
            em.find(SanPham.class, "SP1").setGiaBan(new BigDecimal("100"));
            em.find(Thue.class, "T1").setPhanTram(new BigDecimal("10")); return null;
        });
        socket = new SocketClient("127.0.0.1", server.getPort()); api = api(socket, "sales");
    }
    private static HoaDonApi api(SocketClient s, String user) {
        ClientSession session = new ClientSession(); LoginResponse login = new LoginResponse();
        login.setSessionToken(sessions.create(user, hash)); session.setLogin(login); return new HoaDonApi(s, session);
    }
    @After public void close() { socket.close(); }
    @AfterClass public static void finish() { if (server != null) { server.close(); } if (jpa != null) { jpa.close(); } }
    private HoaDonDetailDTO create(String id) throws Exception {
        HoaDonCreateRequest r = new HoaDonCreateRequest(); r.setMaHD(id); r.setMaThue("T1"); r.setMaKM("KM1"); return api.create(r);
    }
    private static HoaDonVersionRequest version(HoaDonDetailDTO d) {
        HoaDonVersionRequest r = new HoaDonVersionRequest(); r.setMaHD(d.getHoaDon().getMaHD()); r.setPhienBan(d.getHoaDon().getPhienBan()); return r;
    }
    private HoaDonDetailDTO line(HoaDonDetailDTO d, String id, int qty, int mode) throws Exception {
        HoaDonLineRequest r = new HoaDonLineRequest(); r.setMaHD(d.getHoaDon().getMaHD()); r.setPhienBan(d.getHoaDon().getPhienBan());
        r.setMaBienThe(id); r.setSoLuong(qty); return api.line(r, mode);
    }
    private int stock(String id) { return jpa.transaction(em -> em.find(BienThe.class, id).getSoLuongTon()); }
    private interface Work { void run() throws Exception; }
    private static void error(ErrorCode code, Work work) throws Exception {
        try { work.run(); fail("Expected " + code); } catch (ApiException e) { assertEquals(code, e.getCode()); }
    }
    @Test public void draftTotalsAndIdempotentPayment() throws Exception {
        HoaDonDetailDTO d = create("HD1"); assertEquals("HD1", create("HD1").getHoaDon().getMaHD());
        Long old = d.getHoaDon().getPhienBan(); d = line(d, "BT1", 2, 0);
        assertTrue(d.getHoaDon().getPhienBan() > old); assertEquals(5, stock("BT1"));
        assertEquals(0, new BigDecimal("198").compareTo(d.getHoaDon().getTongTien()));
        HoaDonVersionRequest payment = version(d);
        assertEquals(TrangThaiHoaDon.DA_THANH_TOAN, api.pay(payment).getHoaDon().getTrangThai());
        assertEquals(TrangThaiHoaDon.DA_THANH_TOAN, api.pay(payment).getHoaDon().getTrangThai());
        assertEquals(3, stock("BT1")); error(ErrorCode.VALIDATION_ERROR, () -> api.cancel(payment));
    }
    @Test public void absoluteQuantityRemovalAndStaleVersion() throws Exception {
        HoaDonDetailDTO initial = create("HD1"), first = line(initial, "BT1", 1, 0);
        error(ErrorCode.VERSION_CONFLICT, () -> line(initial, "BT2", 1, 0));
        HoaDonDetailDTO changed = line(first, "BT1", 3, 1); assertEquals(3, changed.getChiTiet().get(0).getSoLuong());
        HoaDonDetailDTO empty = line(changed, "BT1", 0, 2); assertTrue(empty.getChiTiet().isEmpty());
        error(ErrorCode.VALIDATION_ERROR, () -> api.pay(version(empty))); assertEquals(5, stock("BT1"));
    }
    @Test public void authorizationAndSearch() throws Exception {
        create("HD1");
        try (SocketClient other = new SocketClient("127.0.0.1", server.getPort())) {
            HoaDonApi foreign = api(other, "other");
            error(ErrorCode.FORBIDDEN, () -> foreign.detail("HD1")); assertEquals(0, foreign.search(new HoaDonSearchRequest()).getTotal());
            assertEquals(1, api(other, "manager").search(new HoaDonSearchRequest()).getTotal());
        }
        assertEquals(1, api.search(new HoaDonSearchRequest()).getTotal()); assertEquals(1, api.taxes().size());
    }
    @Test public void stockFailureRollsBackEntireInvoice() throws Exception {
        HoaDonDetailDTO d = line(line(create("HD1"), "BT1", 2, 0), "BT2", 2, 0);
        jpa.transaction(em -> { em.find(BienThe.class, "BT2").setSoLuongTon(0); return null; });
        error(ErrorCode.OUT_OF_STOCK, () -> api.pay(version(d)));
        assertEquals(5, stock("BT1")); assertEquals(TrangThaiHoaDon.NHAP, api.detail("HD1").getHoaDon().getTrangThai());
    }
    @Test public void snapshotPriceAndChangedTax() throws Exception {
        HoaDonDetailDTO d = line(create("HD1"), "BT1", 1, 0);
        jpa.transaction(em -> { em.find(SanPham.class, "SP1").setGiaBan(new BigDecimal("200")); return null; });
        assertEquals(0, new BigDecimal("99").compareTo(api.pay(version(d)).getHoaDon().getTongTien()));
        HoaDonDetailDTO next = line(create("HD2"), "BT1", 1, 0);
        jpa.transaction(em -> { em.find(Thue.class, "T1").setPhanTram(new BigDecimal("20")); return null; });
        error(ErrorCode.VERSION_CONFLICT, () -> api.pay(version(next))); assertEquals(4, stock("BT1"));
    }
    @Test public void cancelledDraftDoesNotChangeStock() throws Exception {
        HoaDonDetailDTO d = line(create("HD1"), "BT1", 1, 0);
        assertEquals(TrangThaiHoaDon.DA_HUY, api.cancel(version(d)).getHoaDon().getTrangThai());
        assertEquals(5, stock("BT1")); error(ErrorCode.VALIDATION_ERROR, () -> api.pay(version(d)));
    }
    @Test public void invalidQuantityAndDuplicateLine() throws Exception {
        HoaDonDetailDTO initial = create("HD1");
        error(ErrorCode.VALIDATION_ERROR, () -> line(initial, "BT1", 0, 0));
        error(ErrorCode.OUT_OF_STOCK, () -> line(initial, "BT1", 6, 0));
        HoaDonDetailDTO d = line(initial, "BT1", 1, 0);
        error(ErrorCode.VALIDATION_ERROR, () -> line(d, "BT1", 1, 0));
    }
    @Test public void simultaneousClientsCannotOversell() throws Exception {
        jpa.transaction(em -> { em.find(BienThe.class, "BT1").setSoLuongTon(1); return null; });
        HoaDonDetailDTO a = line(create("HD1"), "BT1", 1, 0), b = line(create("HD2"), "BT1", 1, 0);
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> f1 = pool.submit(() -> attempt(a, start)); Future<Boolean> f2 = pool.submit(() -> attempt(b, start));
            start.countDown(); assertEquals(1, (f1.get(15, TimeUnit.SECONDS) ? 1 : 0) + (f2.get(15, TimeUnit.SECONDS) ? 1 : 0));
            assertEquals(0, stock("BT1"));
        } finally { pool.shutdownNow(); }
    }
    @Test public void overflowIsRejectedAndLineInsertRollsBack() throws Exception {
        HoaDonDetailDTO d = create("HD1");
        jpa.transaction(em -> { em.find(SanPham.class, "SP1").setGiaBan(new BigDecimal("9999999999999999.99")); return null; });
        error(ErrorCode.VALIDATION_ERROR, () -> line(d, "BT1", 2, 0));
        assertTrue(api.detail("HD1").getChiTiet().isEmpty()); assertEquals(5, stock("BT1"));
    }
    @Test public void inactiveVariantAndExpiredPromotionAreRejected() throws Exception {
        HoaDonDetailDTO d = create("HD1");
        jpa.transaction(em -> { em.find(BienThe.class, "BT1").setTrangThai(false); return null; });
        try { error(ErrorCode.VALIDATION_ERROR, () -> line(d, "BT1", 1, 0)); }
        finally { jpa.transaction(em -> { em.find(BienThe.class, "BT1").setTrangThai(true); return null; }); }
        jpa.transaction(em -> { em.find(KhuyenMai.class, "KM1").setNgayKetThuc(java.time.LocalDate.of(2021, 1, 1)); return null; });
        try { error(ErrorCode.VALIDATION_ERROR, () -> create("HD2")); }
        finally { jpa.transaction(em -> { em.find(KhuyenMai.class, "KM1").setNgayKetThuc(java.time.LocalDate.of(2099, 1, 1)); return null; }); }
    }
    private static boolean attempt(HoaDonDetailDTO d, CountDownLatch start) throws Exception {
        try (SocketClient s = new SocketClient("127.0.0.1", server.getPort())) {
            start.await(); try { api(s, "sales").pay(version(d)); return true; }
            catch (ApiException e) {
                assertTrue(e.getCode() == ErrorCode.OUT_OF_STOCK || e.getCode() == ErrorCode.VERSION_CONFLICT); return false;
            }
        }
    }
}
