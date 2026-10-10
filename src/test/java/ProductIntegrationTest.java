import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.*;
import javax.persistence.EntityManager;
import client.api.*;
import client.network.SocketClient;
import client.session.ClientSession;
import common.dto.*;
import common.enums.VaiTro;
import common.protocol.*;
import server.dispatcher.RequestDispatcher;
import server.entity.*;
import server.network.SocketServer;
import server.persistence.JpaManager;
import server.security.Pbkdf2PasswordHasher;
import server.service.*;
import server.session.SessionManager;

public class ProductIntegrationTest {
    private static JpaManager jpa;
    private static SocketServer server;
    private static SessionManager sessions;
    private static String hash;
    private SocketClient socket;
    private ClientSession session;
    private SanPhamApi api;

    @BeforeClass public static void setup() throws Exception {
        jpa = new JpaManager(CatalogTestSupport.memoryProperties("products_test"));
        hash = new Pbkdf2PasswordHasher().hash("ProductPass123!".toCharArray());
        jpa.transaction(em -> {
            CatalogTestSupport.user(em, "sales", "NV01", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            CatalogTestSupport.user(em, "manager", "NV02", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            LoaiSanPham category = new LoaiSanPham(); category.setMaLoai("L1"); category.setTenLoai("Áo"); em.persist(category);
            LoaiSanPham other = new LoaiSanPham(); other.setMaLoai("L2"); other.setTenLoai("Quần"); em.persist(other);
            ThuongHieu brand = new ThuongHieu(); brand.setMaThuongHieu("TH1"); brand.setTenThuongHieu("Canifa"); em.persist(brand);
            ThuongHieu otherBrand = new ThuongHieu(); otherBrand.setMaThuongHieu("TH2"); otherBrand.setTenThuongHieu("Owen"); em.persist(otherBrand);
            SanPham first = product(em, "SP01", "Polo cơ bản", category, brand, "149000.50", true);
            variant(em, "BT01", first, "Đỏ", "M", 3, true);
            variant(em, "BT02", first, "Vàng", "L", 7, true);
            variant(em, "BT03", first, "Nâu", "XS", 50, false);
            image(em, "A01", first, "Đỏ", "red-1", 1, true);
            image(em, "A02", first, "Đỏ", "red-2", 2, false);
            image(em, "A03", first, "Vàng", "yellow-1", 1, true);
            image(em, "A04", first, "Nâu", "brown-1", 1, true);
            product(em, "SP02", "Áo [đặc_biệt] 100%!", other, otherBrand, "250000", true);
            SanPham inactive = product(em, "SP03", "Áo ngừng bán", category, brand, "100000", false);
            variant(em, "BT04", inactive, "Đỏ", "M", 2, true);
            image(em, "A05", inactive, "Đỏ", "red-3", 1, true);
            SanPham huge = product(em, "SP04", "Áo tồn lớn", category, brand, "9999999999999999.99", true);
            variant(em, "BT05", huge, "A", "M", Integer.MAX_VALUE, true);
            variant(em, "BT06", huge, "B", "M", Integer.MAX_VALUE, true);
            return null;
        });
        sessions = new SessionManager();
        AuthService auth = new AuthService(jpa, sessions, new Pbkdf2PasswordHasher());
        server = new SocketServer("127.0.0.1", 0, new RequestDispatcher(auth, new SanPhamService(jpa, auth)));
        server.start();
    }
    @Before public void client() {
        jpa.transaction(em -> { em.find(TaiKhoan.class, "sales").setTrangThai(true); return null; });
        socket = new SocketClient("127.0.0.1", server.getPort());
        session = new ClientSession(); useRole("sales");
        api = new SanPhamApi(socket, session);
    }
    private void useRole(String username) {
        LoginResponse login = new LoginResponse();
        // Fixture cấp token; đăng nhập thật đã được kiểm tra trong AuthIntegrationTest.
        login.setSessionToken(sessions.create(username, hash)); session.setLogin(login);
    }
    @After public void closeClient() { socket.close(); }
    @AfterClass public static void cleanup() { if (server != null) { server.close(); } if (jpa != null) { jpa.close(); } }
    @Test public void paginatedSearchPreservesMoneyStockAndStableOrder() throws Exception {
        SanPhamSearchRequest request = new SanPhamSearchRequest(); request.setPageSize(2);
        SanPhamSearchResult first = api.search(request);
        assertEquals(3, first.getTotal()); assertEquals(2, first.getItems().size());
        assertEquals("SP01", first.getItems().get(0).getMaSP());
        assertEquals(0, new BigDecimal("149000.50").compareTo(first.getItems().get(0).getGiaBan()));
        assertEquals(10, first.getItems().get(0).getTongTon());
        request.setPage(1); SanPhamSearchResult next = api.search(request);
        assertEquals(3, next.getTotal()); assertEquals(1, next.getItems().size());
        assertEquals("SP04", next.getItems().get(0).getMaSP());
        assertEquals(4294967294L, next.getItems().get(0).getTongTon());
        assertEquals(0, new BigDecimal("9999999999999999.99").compareTo(next.getItems().get(0).getGiaBan()));
        request.setPage(99); assertTrue(api.search(request).getItems().isEmpty());
    }
    @Test public void nameCategoryAndBrandFiltersAreCombined() throws Exception {
        SanPhamSearchRequest request = new SanPhamSearchRequest();
        request.setTenSP("  POLO  "); request.setMaLoai("L1"); request.setMaThuongHieu("TH1");
        assertEquals("SP01", api.search(request).getItems().get(0).getMaSP());
        request.setMaThuongHieu("TH2"); assertEquals(0, api.search(request).getTotal());
    }
    @Test public void colorAndSizeMustMatchSameActiveVariant() throws Exception {
        SanPhamSearchRequest request = new SanPhamSearchRequest();
        request.setMauSac("đỏ"); request.setKichCo("L");
        assertEquals(0, api.search(request).getTotal());
        request.setMauSac("Vàng"); assertEquals("SP01", api.search(request).getItems().get(0).getMaSP());
        request.setMauSac("Nâu"); request.setKichCo("XS");
        assertEquals(0, api.search(request).getTotal());
        request.setIncludeInactive(true); expect(ErrorCode.FORBIDDEN, () -> api.search(request));
        useRole("manager"); assertEquals(1, api.search(request).getTotal());
    }
    @Test public void wildcardAndSqlLikeTextAreLiteralNotExecutable() throws Exception {
        for (String text : new String[] {"%", "_", "[", "]", "!"}) {
            SanPhamSearchRequest request = new SanPhamSearchRequest(); request.setTenSP(text);
            assertEquals(text, 1, api.search(request).getTotal());
            assertEquals("SP02", api.search(request).getItems().get(0).getMaSP());
        }
        SanPhamSearchRequest request = new SanPhamSearchRequest(); request.setTenSP("' or 1=1 --");
        assertEquals(0, api.search(request).getTotal());
    }
    @Test public void detailReturnsDtoActiveVariantsAndMatchingImagePaths() throws Exception {
        SanPhamDetailDTO detail = api.detail("SP01");
        assertEquals(2, detail.getBienThe().size()); assertEquals(3, detail.getHinhAnh().size());
        assertEquals(10, detail.getSanPham().getTongTon());
        for (AnhSanPhamDTO image : detail.getHinhAnh()) {
            assertNotNull(getClass().getClassLoader().getResource(image.getDuongDan()));
        }
        String json = JsonCodec.mapper().writeValueAsString(detail);
        assertFalse(json.contains("hibernateLazyInitializer"));
        assertFalse(json.contains("passwordHash"));
        assertNotNull(detail.getBienThe().get(0).getPhienBan());
        useRole("manager");
        assertEquals(3, api.detail("SP01").getBienThe().size());
        assertEquals(4, api.detail("SP01").getHinhAnh().size());
    }
    @Test public void salesCannotReadInactiveProductEvenByDirectId() throws Exception {
        expect(ErrorCode.NOT_FOUND, () -> api.detail("SP03"));
        SanPhamSearchRequest request = new SanPhamSearchRequest(); request.setIncludeInactive(true);
        expect(ErrorCode.FORBIDDEN, () -> api.search(request));
        useRole("manager"); assertEquals(4, api.search(request).getTotal());
        assertFalse(api.detail("SP03").getSanPham().getTrangThai());
    }
    @Test public void productWithNoVariantsOrImagesAndMissingIdAreHandled() throws Exception {
        SanPhamDetailDTO detail = api.detail("SP02");
        assertTrue(detail.getBienThe().isEmpty()); assertTrue(detail.getHinhAnh().isEmpty());
        assertEquals(0, detail.getSanPham().getTongTon());
        expect(ErrorCode.NOT_FOUND, () -> api.detail("missing"));
        expect(ErrorCode.VALIDATION_ERROR, () -> api.detail(" "));
    }
    @Test public void categoryAndBrandLookupsReturnTypedDtos() throws Exception {
        assertEquals(2, api.categories().size()); assertEquals(2, api.brands().size());
        assertTrue(api.brands().get(0) instanceof ThuongHieuDTO);
        assertEquals("TH1", api.brands().get(0).getMaThuongHieu());
    }
    @Test public void allProductActionsRequireActiveSession() throws Exception {
        session.clear();
        expect(ErrorCode.UNAUTHENTICATED, () -> api.search(new SanPhamSearchRequest()));
        expect(ErrorCode.UNAUTHENTICATED, () -> api.detail("SP01"));
        expect(ErrorCode.UNAUTHENTICATED, () -> api.categories());
        expect(ErrorCode.UNAUTHENTICATED, () -> api.brands());
        useRole("sales");
        jpa.transaction(em -> { em.find(TaiKhoan.class, "sales").setTrangThai(false); return null; });
        expect(ErrorCode.UNAUTHENTICATED, () -> api.search(new SanPhamSearchRequest()));
        assertNull(session.getToken());
    }
    @Test public void invalidPaginationAndPayloadAreRejected() throws Exception {
        SanPhamSearchRequest request = new SanPhamSearchRequest(); request.setPage(-1);
        expect(ErrorCode.VALIDATION_ERROR, () -> api.search(request));
        request.setPage(Integer.MAX_VALUE); expect(ErrorCode.VALIDATION_ERROR, () -> api.search(request));
        request.setPage(0); request.setPageSize(101); expect(ErrorCode.VALIDATION_ERROR, () -> api.search(request));
        request.setPageSize(20); request.setMauSac(String.join("", Collections.nCopies(51, "x")));
        expect(ErrorCode.VALIDATION_ERROR, () -> api.search(request));
        assertEquals(ErrorCode.INVALID_REQUEST, socket.send(Action.TRA_CUU_SAN_PHAM, session.getToken(), null).getErrorCode());
    }
    private static SanPham product(EntityManager em, String id, String name, LoaiSanPham category,
            ThuongHieu brand, String price, boolean active) {
        SanPham p = new SanPham(); p.setMaSP(id); p.setTenSP(name); p.setLoaiSanPham(category);
        p.setThuongHieu(brand); p.setGiaBan(new BigDecimal(price)); p.setTrangThai(active); em.persist(p); return p;
    }
    private static void variant(EntityManager em, String id, SanPham p, String color, String size, int stock, boolean active) {
        BienThe b = new BienThe(); b.setMaBienThe(id); b.setSanPham(p); b.setMauSac(color);
        b.setKichCo(size); b.setSoLuongTon(stock); b.setTrangThai(active); em.persist(b);
    }
    private static void image(EntityManager em, String id, SanPham p, String color, String file, int order, boolean primary) {
        AnhSanPham a = new AnhSanPham(); a.setMaAnh(id); a.setSanPham(p); a.setMauSac(color);
        a.setDuongDan("images/products/1001/" + file + ".jpg"); a.setThuTu(order); a.setAnhChinh(primary); em.persist(a);
    }
    private interface Task { void run() throws Exception; }
    private static void expect(ErrorCode code, Task task) throws Exception {
        try { task.run(); fail("Expected " + code); }
        catch (ApiException expected) { assertEquals(code, expected.getCode()); }
    }
}
