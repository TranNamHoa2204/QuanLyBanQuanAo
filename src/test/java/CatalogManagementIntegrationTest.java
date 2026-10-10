import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import org.junit.*;
import client.api.*;
import client.network.SocketClient;
import client.session.ClientSession;
import common.dto.*;
import common.enums.*;
import common.protocol.*;
import server.entity.*;
import server.service.*;
import server.persistence.JpaManager;
import server.security.Pbkdf2PasswordHasher;
import server.session.SessionManager;
import server.dispatcher.RequestDispatcher;
import server.network.SocketServer;

/** CRUD qua TCP + JPA/H2, không sửa SQL Server thật. */
public class CatalogManagementIntegrationTest {
    private static JpaManager jpa;
    private static SocketServer server;
    private static SessionManager sessions;
    private static String hash;
    private SocketClient socket;
    private ClientSession session;
    private SanPhamApi api;
    @BeforeClass public static void setup() throws Exception {
        jpa = new JpaManager(CatalogTestSupport.memoryProperties("catalog_management_test"));
        hash = new Pbkdf2PasswordHasher().hash("ManagementPass123!".toCharArray());
        jpa.transaction(em -> {
            CatalogTestSupport.user(em, "manager", "NV1", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            CatalogTestSupport.user(em, "sales", "NV2", VaiTro.NHAN_VIEN_BAN_HANG, hash); return null;
        });
        sessions = new SessionManager(); AuthService auth = new AuthService(jpa, sessions, new Pbkdf2PasswordHasher());
        server = new SocketServer("127.0.0.1", 0, new RequestDispatcher(auth, new SanPhamService(jpa, auth), new HoaDonService(jpa, auth))); server.start();
    }
    @Before public void reset() {
        jpa.transaction(em -> {
            for (String entity : new String[]{"ChiTietHoaDon", "HoaDon", "AnhSanPham", "BienThe", "SanPham", "LoaiSanPham", "ThuongHieu", "Thue"}) {
                em.createQuery("delete from " + entity).executeUpdate();
            }
            LoaiSanPham l = new LoaiSanPham(); l.setMaLoai("L1"); l.setTenLoai("Áo"); em.persist(l);
            ThuongHieu b = new ThuongHieu(); b.setMaThuongHieu("TH1"); b.setTenThuongHieu("Demo"); em.persist(b);
            SanPham p = new SanPham(); p.setMaSP("SP1"); p.setTenSP("Áo cơ bản"); p.setLoaiSanPham(l); p.setThuongHieu(b);
            p.setGiaBan(new BigDecimal("100")); em.persist(p);
            BienThe v = new BienThe(); v.setMaBienThe("BT1"); v.setSanPham(p); v.setMauSac("Đỏ"); v.setKichCo("M"); v.setSoLuongTon(5); em.persist(v);
            Thue tax = new Thue(); tax.setMaThue("T1"); tax.setTenThue("Thuế demo"); tax.setPhanTram(BigDecimal.ZERO); em.persist(tax);
            return null;
        });
        socket = new SocketClient("127.0.0.1", server.getPort()); session = session("manager"); api = new SanPhamApi(socket, session);
    }
    private static ClientSession session(String user) {
        ClientSession s = new ClientSession(); LoginResponse l = new LoginResponse(); l.setSessionToken(sessions.create(user, hash)); s.setLogin(l); return s;
    }
    @After public void close() { socket.close(); }
    @AfterClass public static void finish() { if (server != null) { server.close(); } if (jpa != null) { jpa.close(); } }
    private interface Work { void run() throws Exception; }
    private static void error(ErrorCode code, Work work) throws Exception {
        try { work.run(); fail("Expected " + code); } catch (ApiException e) { assertEquals(code, e.getCode()); }
    }
    private static DanhMucSaveRequest catalog(String id, String name, String before) {
        DanhMucSaveRequest r = new DanhMucSaveRequest(); r.setCode(id); r.setName(name); r.setExpectedName(before); return r;
    }
    private static SanPhamDTO product(String id, String name, String price) {
        SanPhamDTO p = new SanPhamDTO(); p.setMaSP(id); p.setTenSP(name); p.setMaLoai("L1"); p.setMaThuongHieu("TH1");
        p.setGiaBan(new BigDecimal(price)); return p;
    }
    private static SanPhamSaveRequest request(SanPhamDTO value, SanPhamDTO before) {
        SanPhamSaveRequest r = new SanPhamSaveRequest(); r.setValue(value); r.setExpected(before); return r;
    }
    private static BienTheDTO variant(String id, String color, String size, int stock) {
        BienTheDTO b = new BienTheDTO(); b.setMaBienThe(id); b.setMaSP("SP1"); b.setMauSac(color); b.setKichCo(size); b.setSoLuongTon(stock); return b;
    }
    @Test public void createsAndRenamesBothCatalogTypes() throws Exception {
        assertEquals("L2", api.saveCategory(catalog("L2", "Quần", null), true));
        assertEquals("TH2", api.saveBrand(catalog("TH2", "Thương hiệu mới", null), true));
        api.saveCategory(catalog("L2", "Quần dài", "Quần"), false);
        api.saveBrand(catalog("TH2", "Tên mới", "Thương hiệu mới"), false);
        assertTrue(api.categories().stream().anyMatch(c -> c.getTenLoai().equals("Quần dài")));
        assertTrue(api.brands().stream().anyMatch(c -> c.getTenThuongHieu().equals("Tên mới")));
        error(ErrorCode.VERSION_CONFLICT, () -> api.saveCategory(catalog("L2", "Tên khác", "Quần"), false));
        assertEquals("Áo", api.detail("SP1").getSanPham().getTenLoai());
    }
    @Test public void duplicateIdsNamesAndInvalidCodesAreRejected() throws Exception {
        error(ErrorCode.VALIDATION_ERROR, () -> api.saveCategory(catalog("L1", "Tên khác", null), true));
        error(ErrorCode.VALIDATION_ERROR, () -> api.saveCategory(catalog("L2", "ÁO", null), true));
        error(ErrorCode.VALIDATION_ERROR, () -> api.saveBrand(catalog("TH2", "DEMO", null), true));
        error(ErrorCode.VALIDATION_ERROR, () -> api.saveBrand(catalog("../id", "Tên", null), true));
        error(ErrorCode.VALIDATION_ERROR, () -> api.saveBrand(catalog("TH2", "   ", null), true));
        assertEquals(1, api.categories().size()); assertEquals(1, api.brands().size());
    }
    @Test public void createAndUpdateProductPreservesIdentifiers() throws Exception {
        assertEquals("SP2", api.saveProduct(request(product("SP2", "Áo mới", "149000.50"), null), 0));
        SanPhamDTO before = api.detail("SP2").getSanPham(); SanPhamDTO value = product("SP2", "Áo cập nhật", "159000");
        api.saveProduct(request(value, before), 1);
        assertEquals("Áo cập nhật", api.detail("SP2").getSanPham().getTenSP());
        error(ErrorCode.VERSION_CONFLICT, () -> api.saveProduct(request(value, before), 1));
    }
    @Test public void invalidProductAndMissingParentsRollBack() throws Exception {
        error(ErrorCode.VALIDATION_ERROR, () -> api.saveProduct(request(product("SP2", "Tên", "-1"), null), 0));
        error(ErrorCode.VALIDATION_ERROR, () -> api.saveProduct(request(product("SP2", "Tên", "1.001"), null), 0));
        SanPhamDTO v = product("SP2", "Tên", "100"); v.setMaLoai("MISSING");
        error(ErrorCode.VALIDATION_ERROR, () -> api.saveProduct(request(v, null), 0));
        error(ErrorCode.NOT_FOUND, () -> api.detail("SP2"));
        assertEquals(1, api.search(new SanPhamSearchRequest()).getTotal());
    }
    @Test public void stoppingProductIsSoftAndCanBeReactivated() throws Exception {
        SanPhamDTO before = api.detail("SP1").getSanPham(); api.saveProduct(request(before, before), 2);
        assertFalse(api.detail("SP1").getSanPham().getTrangThai());
        SanPhamSearchRequest all = new SanPhamSearchRequest(); all.setIncludeInactive(true);
        assertEquals(1, api.search(all).getTotal()); assertEquals(0, api.search(new SanPhamSearchRequest()).getTotal());
        SanPhamDTO stopped = api.detail("SP1").getSanPham(); api.saveProduct(request(product("SP1", stopped.getTenSP(), "100"), stopped), 1);
        assertTrue(api.detail("SP1").getSanPham().getTrangThai()); assertEquals(1, api.detail("SP1").getBienThe().size());
    }
    @Test public void managerRoleIsRequiredForEveryWrite() throws Exception {
        SanPhamDTO before = api.detail("SP1").getSanPham(); session.setLogin(session("sales").getLogin());
        error(ErrorCode.FORBIDDEN, () -> api.saveCategory(catalog("L2", "Quần", null), true));
        error(ErrorCode.FORBIDDEN, () -> api.saveCategory(catalog("L1", "Quần", "Áo"), false));
        error(ErrorCode.FORBIDDEN, () -> api.saveBrand(catalog("TH2", "Tên", null), true));
        error(ErrorCode.FORBIDDEN, () -> api.saveBrand(catalog("TH1", "Tên", "Demo"), false));
        error(ErrorCode.FORBIDDEN, () -> api.saveProduct(request(product("SP2", "Tên", "100"), null), 0));
        error(ErrorCode.FORBIDDEN, () -> api.saveProduct(request(before, before), 1));
        error(ErrorCode.FORBIDDEN, () -> api.saveProduct(request(before, before), 2));
        error(ErrorCode.FORBIDDEN, () -> api.saveVariant(variant("BT2", "Đỏ", "L", 1), true));
        error(ErrorCode.FORBIDDEN, () -> api.saveVariant(variant("BT1", "Đỏ", "M", 1), false));
    }
    @Test public void revokedManagerAndUnauthenticatedWritesAreRejected() throws Exception {
        jpa.transaction(em -> { em.find(TaiKhoan.class, "manager").setTrangThai(false); return null; });
        try { error(ErrorCode.UNAUTHENTICATED, () -> api.saveCategory(catalog("L2", "Quần", null), true)); }
        finally { jpa.transaction(em -> { em.find(TaiKhoan.class, "manager").setTrangThai(true); return null; }); }
        assertNull(session.getToken()); error(ErrorCode.UNAUTHENTICATED, () -> api.saveBrand(catalog("TH2", "Tên", null), true));
    }
    @Test public void variantCreationStockUpdateAndDisable() throws Exception {
        BienTheDTO created = api.saveVariant(variant("BT2", "Vàng", "L", 0), true);
        assertEquals(0, created.getSoLuongTon()); created.setSoLuongTon(7);
        BienTheDTO changed = api.saveVariant(created, false); assertEquals(7, changed.getSoLuongTon());
        assertTrue(changed.getPhienBan() > created.getPhienBan());
        error(ErrorCode.VERSION_CONFLICT, () -> api.saveVariant(created, false));
        changed.setTrangThai(false); api.saveVariant(changed, false);
        assertEquals(2, api.detail("SP1").getBienThe().size()); assertEquals(5, api.detail("SP1").getSanPham().getTongTon());
    }
    @Test public void duplicateVariantAndImmutableIdentity() throws Exception {
        error(ErrorCode.VALIDATION_ERROR, () -> api.saveVariant(variant("BT2", "đỏ", "m", 1), true));
        error(ErrorCode.VALIDATION_ERROR, () -> api.saveVariant(variant("BT2", "Vàng", "L", -1), true));
        BienTheDTO b = api.detail("SP1").getBienThe().get(0); b.setMauSac("Xanh");
        error(ErrorCode.VALIDATION_ERROR, () -> api.saveVariant(b, false)); assertEquals(1, api.detail("SP1").getBienThe().size());
    }
    @Test public void twoEditorsCannotOverwriteEachOther() throws Exception {
        SanPhamDTO before = api.detail("SP1").getSanPham();
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> a = pool.submit(() -> attemptProduct(before, "Tên A", start));
            Future<Boolean> b = pool.submit(() -> attemptProduct(before, "Tên B", start));
            start.countDown(); assertEquals(1, (a.get(15, TimeUnit.SECONDS) ? 1 : 0) + (b.get(15, TimeUnit.SECONDS) ? 1 : 0));
        } finally { pool.shutdownNow(); }
    }
    private static boolean attemptProduct(SanPhamDTO before, String name, CountDownLatch start) throws Exception {
        try (SocketClient s = new SocketClient("127.0.0.1", server.getPort())) {
            start.await(); try { new SanPhamApi(s, session("manager")).saveProduct(request(product("SP1", name, "100"), before), 1); return true; }
            catch (ApiException e) { assertEquals(ErrorCode.VERSION_CONFLICT, e.getCode()); return false; }
        }
    }
    @Test public void historicalPriceAndRowsSurviveProductEdits() throws Exception {
        HoaDonApi invoices = new HoaDonApi(socket, session);
        HoaDonDetailDTO d = invoice(invoices);
        SanPhamDTO before = api.detail("SP1").getSanPham(); api.saveProduct(request(product("SP1", "Tên mới", "200"), before), 1);
        HoaDonDetailDTO paid = invoices.pay(version(d)); assertEquals(0, new BigDecimal("100").compareTo(paid.getHoaDon().getTongTien()));
        SanPhamDTO now = api.detail("SP1").getSanPham(); api.saveProduct(request(now, now), 2);
        assertEquals(1, invoices.detail("HD1").getChiTiet().size()); assertEquals(4, api.detail("SP1").getBienThe().get(0).getSoLuongTon());
    }
    @Test public void stockAdjustmentRacingCheckoutNeverLosesSale() throws Exception {
        HoaDonApi invoices = new HoaDonApi(socket, session); HoaDonDetailDTO d = invoice(invoices);
        BienTheDTO before = api.detail("SP1").getBienThe().get(0); before.setSoLuongTon(7);
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> changed = pool.submit(() -> {
                try (SocketClient s = new SocketClient("127.0.0.1", server.getPort())) {
                    start.await(); try { new SanPhamApi(s, session("manager")).saveVariant(before, false); return true; }
                    catch (ApiException ex) { assertEquals(ErrorCode.VERSION_CONFLICT, ex.getCode()); return false; }
                }
            });
            Future<HoaDonDetailDTO> paid = pool.submit(() -> {
                try (SocketClient s = new SocketClient("127.0.0.1", server.getPort())) {
                    start.await(); return new HoaDonApi(s, session("manager")).pay(version(d));
                }
            });
            start.countDown(); boolean didChange = changed.get(15, TimeUnit.SECONDS);
            assertEquals(TrangThaiHoaDon.DA_THANH_TOAN, paid.get(15, TimeUnit.SECONDS).getHoaDon().getTrangThai());
            assertEquals(didChange ? 6 : 4, api.detail("SP1").getBienThe().get(0).getSoLuongTon());
        } finally { pool.shutdownNow(); }
    }
    private static HoaDonVersionRequest version(HoaDonDetailDTO d) {
        HoaDonVersionRequest r = new HoaDonVersionRequest(); r.setMaHD(d.getHoaDon().getMaHD()); r.setPhienBan(d.getHoaDon().getPhienBan()); return r;
    }
    private static HoaDonDetailDTO invoice(HoaDonApi api) throws Exception {
        HoaDonCreateRequest create = new HoaDonCreateRequest(); create.setMaHD("HD1"); create.setMaThue("T1");
        HoaDonDetailDTO d = api.create(create); HoaDonLineRequest line = new HoaDonLineRequest();
        line.setMaHD("HD1"); line.setPhienBan(d.getHoaDon().getPhienBan()); line.setMaBienThe("BT1"); line.setSoLuong(1); return api.line(line, 0);
    }
}
