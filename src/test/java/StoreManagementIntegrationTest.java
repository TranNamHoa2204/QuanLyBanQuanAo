import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.*;
import org.junit.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import client.api.*;
import client.api.ManagementApi.Kind;
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

/** Các module còn lại qua Socket thật + Hibernate/H2, tuyệt đối không dùng database thật. */
public class StoreManagementIntegrationTest {
    private static JpaManager jpa;
    private static SocketServer server;
    private static SessionManager sessions;
    private static String hash;
    private SocketClient socket;
    private ClientSession session;
    private ManagementApi api;
    @BeforeClass public static void setup() throws Exception {
        jpa = new JpaManager(CatalogTestSupport.memoryProperties("remaining_modules_test"));
        hash = new Pbkdf2PasswordHasher().hash("ManagementPass123!".toCharArray());
        sessions = new SessionManager(); AuthService auth = new AuthService(jpa, sessions, new Pbkdf2PasswordHasher());
        Clock clock = Clock.fixed(Instant.parse("2026-10-11T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        server = new SocketServer("127.0.0.1", 0, new RequestDispatcher(auth, new SanPhamService(jpa, auth), new HoaDonService(jpa, auth, clock), jpa)); server.start();
    }
    @Before public void reset() {
        jpa.transaction(em -> {
            for (String entity : new String[]{"ChiTietHoaDon", "HoaDon", "AnhSanPham", "BienThe", "SanPham", "LoaiSanPham",
                    "ThuongHieu", "Thue", "KhuyenMai", "KhachHang", "TaiKhoan", "NhanVien"}) { em.createQuery("delete from " + entity).executeUpdate(); }
            CatalogTestSupport.user(em, "manager", "NV1", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            CatalogTestSupport.user(em, "sales", "NV2", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            CatalogTestSupport.user(em, "peer", "NV3", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            em.find(NhanVien.class, "NV1").setSdt("0900000001"); em.find(NhanVien.class, "NV2").setSdt("0900000002"); em.find(NhanVien.class, "NV3").setSdt("0900000003");
            LoaiSanPham l = new LoaiSanPham(); l.setMaLoai("L1"); l.setTenLoai("Áo"); em.persist(l);
            ThuongHieu b = new ThuongHieu(); b.setMaThuongHieu("TH1"); b.setTenThuongHieu("Demo"); em.persist(b);
            for (int i = 1; i <= 2; i++) {
                SanPham p = new SanPham(); p.setMaSP("SP" + i); p.setTenSP("Áo " + i); p.setLoaiSanPham(l); p.setThuongHieu(b); p.setGiaBan(new BigDecimal(i * 100)); em.persist(p);
                BienThe v = new BienThe(); v.setMaBienThe("BT" + i); v.setSanPham(p); v.setMauSac("Đỏ"); v.setKichCo("M"); v.setSoLuongTon(50); em.persist(v);
            }
            Thue t = new Thue(); t.setMaThue("T1"); t.setTenThue("Thuế demo"); t.setPhanTram(new BigDecimal("10")); em.persist(t);
            return null;
        });
        socket = new SocketClient("127.0.0.1", server.getPort()); session = loginSession("manager"); api = new ManagementApi(socket, session);
    }
    @After public void close() { socket.close(); }
    @AfterClass public static void finish() { if (server != null) { server.close(); } if (jpa != null) { jpa.close(); } }
    private static ClientSession loginSession(String user) {
        ClientSession s = new ClientSession(); LoginResponse l = new LoginResponse(); l.setSessionToken(sessions.create(user, hash)); s.setLogin(l); return s;
    }
    private interface Work { void run() throws Exception; }
    private static void error(ErrorCode expected, Work work) throws Exception {
        try { work.run(); fail("Expected " + expected); } catch (ApiException e) { assertEquals(e.getMessage(), expected, e.getCode()); }
    }
    private static void money(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }
    private static ManagementSaveRequest request(Object value, JsonNode expected) {
        ManagementSaveRequest r = new ManagementSaveRequest(); r.setValue(JsonCodec.mapper().valueToTree(value)); r.setExpected(expected); return r;
    }
    private JsonNode save(Kind kind, Object value, JsonNode before, int mode) throws Exception { return api.save(kind, request(value, before), mode); }
    private JsonNode find(Kind kind, String id) throws Exception {
        ManagementSearchRequest r = new ManagementSearchRequest(); r.setIncludeInactive(true); r.setQuery(id);
        return api.search(kind, r).getItems().get(0);
    }
    private static KhachHangDTO customer(String id, String phone) {
        KhachHangDTO v = new KhachHangDTO(); v.setMaKH(id); v.setHoTen("Khách " + id); v.setSdt(phone); return v;
    }
    private static KhuyenMaiDTO promotion(String id) {
        KhuyenMaiDTO v = new KhuyenMaiDTO(); v.setMaKM(id); v.setTenKM("Giảm thử"); v.setPhanTramGiam(new BigDecimal("20"));
        v.setNgayBatDau(LocalDate.of(2026, 10, 1)); v.setNgayKetThuc(LocalDate.of(2026, 10, 31)); return v;
    }
    private static NhanVienAccountDTO employee(String id, String user) {
        NhanVienAccountDTO v = new NhanVienAccountDTO(); v.setMaNV(id); v.setHoTen("Nhân viên mới"); v.setSdt("0901234567");
        v.setUsername(user); v.setVaiTro(VaiTro.NHAN_VIEN_BAN_HANG); return v;
    }
    @Test public void salesCanMaintainCustomersAndNormalizePhone() throws Exception {
        session.setLogin(loginSession("sales").getLogin());
        JsonNode before = save(Kind.CUSTOMER, customer("KH1", "(090) 123-4567"), null, 0);
        assertEquals("0901234567", before.path("sdt").asText());
        KhachHangDTO changed = JsonCodec.mapper().convertValue(before, KhachHangDTO.class); changed.setHoTen("Khách cập nhật");
        JsonNode after = save(Kind.CUSTOMER, changed, before, 1); assertEquals("Khách cập nhật", after.path("hoTen").asText());
        JsonNode stopped = save(Kind.CUSTOMER, after, after, 2); assertFalse(stopped.path("trangThai").asBoolean());
        assertEquals(0, api.search(Kind.CUSTOMER, new ManagementSearchRequest()).getTotal());
        changed.setTrangThai(true); save(Kind.CUSTOMER, changed, stopped, 1);
        assertEquals(1, api.search(Kind.CUSTOMER, new ManagementSearchRequest()).getTotal());
    }
    @Test public void customerOptionalFieldsPhoneUniquenessAndValidation() throws Exception {
        save(Kind.CUSTOMER, customer("KH1", null), null, 0); save(Kind.CUSTOMER, customer("KH2", ""), null, 0);
        save(Kind.CUSTOMER, customer("KH3", "0901234567"), null, 0);
        error(ErrorCode.VALIDATION_ERROR, () -> save(Kind.CUSTOMER, customer("KH4", "090 123-4567"), null, 0));
        KhachHangDTO v = customer("KH4", null); v.setEmail("not-email"); error(ErrorCode.VALIDATION_ERROR, () -> save(Kind.CUSTOMER, v, null, 0));
        error(ErrorCode.VALIDATION_ERROR, () -> save(Kind.CUSTOMER, customer("../bad", null), null, 0));
        error(ErrorCode.VALIDATION_ERROR, () -> save(Kind.CUSTOMER, customer("KH1", null), null, 0));
        assertEquals(3, api.search(Kind.CUSTOMER, new ManagementSearchRequest()).getTotal());
    }
    @Test public void pagingSearchAndBounds() throws Exception {
        for (int i = 0; i < 5; i++) { save(Kind.CUSTOMER, customer("KH" + i, null), null, 0); }
        ManagementSearchRequest r = new ManagementSearchRequest(); r.setPageSize(2); r.setPage(1);
        assertEquals(5, api.search(Kind.CUSTOMER, r).getTotal()); assertEquals("KH2", api.search(Kind.CUSTOMER, r).getItems().get(0).path("maKH").asText());
        r.setQuery("Khách KH4"); assertEquals(1, api.search(Kind.CUSTOMER, r).getTotal());
        r.setPage(-1); error(ErrorCode.VALIDATION_ERROR, () -> api.search(Kind.CUSTOMER, r));
        r.setPage(0); r.setPageSize(101); error(ErrorCode.VALIDATION_ERROR, () -> api.search(Kind.CUSTOMER, r));
    }
    @Test public void staleSnapshotsRejectedAcrossThreeModules() throws Exception {
        JsonNode k = save(Kind.CUSTOMER, customer("KH1", null), null, 0);
        ObjectNode k2 = ((ObjectNode) k).deepCopy(); k2.put("hoTen", "Đã đổi"); save(Kind.CUSTOMER, k2, k, 1);
        error(ErrorCode.VERSION_CONFLICT, () -> save(Kind.CUSTOMER, k, k, 1));
        JsonNode p = save(Kind.PROMOTION, promotion("KM1"), null, 0); ObjectNode p2 = ((ObjectNode) p).deepCopy(); p2.put("tenKM", "Tên mới");
        save(Kind.PROMOTION, p2, p, 1); error(ErrorCode.VERSION_CONFLICT, () -> save(Kind.PROMOTION, p, p, 2));
        JsonNode n = find(Kind.EMPLOYEE, "NV2"); ObjectNode n2 = ((ObjectNode) n).deepCopy(); n2.put("hoTen", "Đã sửa nhân viên");
        save(Kind.EMPLOYEE, n2, n, 1); error(ErrorCode.VERSION_CONFLICT, () -> save(Kind.EMPLOYEE, n, n, 1));
    }
    @Test public void accountCreationLoginAndNoCredentialDisclosure() throws Exception {
        ManagementSaveRequest r = request(employee("NV4", "new.sales"), null); r.setInitialPassword("NewAccount123!");
        JsonNode out = api.save(Kind.EMPLOYEE, r, 0);
        assertNull(r.getInitialPassword()); assertFalse(out.has("passwordHash")); assertFalse(out.has("initialPassword"));
        assertFalse(api.search(Kind.EMPLOYEE, new ManagementSearchRequest()).getItems().toString().contains(hash));
        try (SocketClient other = new SocketClient("127.0.0.1", server.getPort())) {
            ClientSession newSession = new ClientSession(); AuthApi authApi = new AuthApi(other, newSession);
            assertEquals("NV4", authApi.login("new.sales", "NewAccount123!").getMaNV());
            error(ErrorCode.FORBIDDEN, () -> authApi.management().search(Kind.EMPLOYEE, new ManagementSearchRequest()));
        }
        String stored = jpa.transaction(em -> em.find(TaiKhoan.class, "new.sales").getPasswordHash()); assertNotEquals("NewAccount123!", stored);
    }
    @Test public void employeeWithoutAccountCanReceiveOneLater() throws Exception {
        JsonNode before = save(Kind.EMPLOYEE, employee("NV4", null), null, 0); assertTrue(before.path("username").isNull());
        ObjectNode v = ((ObjectNode) before).deepCopy(); v.put("username", "later"); v.put("vaiTro", "NHAN_VIEN_BAN_HANG"); v.put("taiKhoanHoatDong", true);
        ManagementSaveRequest r = request(v, before); r.setInitialPassword("LaterAccount123!");
        assertEquals("later", api.save(Kind.EMPLOYEE, r, 1).path("username").asText());
    }
    @Test public void invalidAccountRollsBackEmployeeAndNoRenamesOrSilentPasswordReset() throws Exception {
        ManagementSaveRequest r = request(employee("NV4", "new.sales"), null); r.setInitialPassword("short");
        error(ErrorCode.VALIDATION_ERROR, () -> api.save(Kind.EMPLOYEE, r, 0));
        assertNull(jpa.transaction(em -> em.find(NhanVien.class, "NV4")));
        ManagementSaveRequest duplicate = request(employee("NV4", "manager"), null); duplicate.setInitialPassword("ValidPassword123!");
        error(ErrorCode.VALIDATION_ERROR, () -> api.save(Kind.EMPLOYEE, duplicate, 0));
        JsonNode before = find(Kind.EMPLOYEE, "NV2"); ObjectNode v = ((ObjectNode) before).deepCopy(); v.put("username", "renamed");
        error(ErrorCode.VALIDATION_ERROR, () -> save(Kind.EMPLOYEE, v, before, 1));
        ManagementSaveRequest reset = request(before, before); reset.setInitialPassword("NoResetPassword123!");
        error(ErrorCode.VALIDATION_ERROR, () -> api.save(Kind.EMPLOYEE, reset, 1));
    }
    @Test public void managersCannotDisableOrDemoteThemselves() throws Exception {
        JsonNode before = find(Kind.EMPLOYEE, "NV1");
        error(ErrorCode.VALIDATION_ERROR, () -> save(Kind.EMPLOYEE, before, before, 2));
        ObjectNode v = ((ObjectNode) before).deepCopy(); v.put("vaiTro", "NHAN_VIEN_BAN_HANG");
        error(ErrorCode.VALIDATION_ERROR, () -> save(Kind.EMPLOYEE, v, before, 1));
        v.put("vaiTro", "NHAN_VIEN_QUAN_LY"); v.put("taiKhoanHoatDong", false);
        error(ErrorCode.VALIDATION_ERROR, () -> save(Kind.EMPLOYEE, v, before, 1));
        assertEquals(2L, (long) jpa.transaction(em -> em.createQuery("select count(t) from TaiKhoan t where t.vaiTro = :r and t.trangThai = true", Long.class).setParameter("r", VaiTro.NHAN_VIEN_QUAN_LY).getSingleResult()));
    }
    @Test public void roleAndStatusChangesRevokeSessionsAndKeepEmployee() throws Exception {
        ClientSession peer = loginSession("peer");
        JsonNode before = find(Kind.EMPLOYEE, "NV3"); ObjectNode v = ((ObjectNode) before).deepCopy(); v.put("vaiTro", "NHAN_VIEN_BAN_HANG");
        save(Kind.EMPLOYEE, v, before, 1);
        try (SocketClient other = new SocketClient("127.0.0.1", server.getPort())) {
            ManagementApi peerApi = new ManagementApi(other, peer);
            error(ErrorCode.UNAUTHENTICATED, () -> peerApi.search(Kind.CUSTOMER, new ManagementSearchRequest())); assertNull(peer.getToken());
        }
        JsonNode stopped = find(Kind.EMPLOYEE, "NV2"); save(Kind.EMPLOYEE, stopped, stopped, 2);
        assertNotNull(jpa.transaction(em -> em.find(NhanVien.class, "NV2")));
        assertFalse(jpa.transaction(em -> em.find(TaiKhoan.class, "sales").getTrangThai()));
    }
    @Test public void concurrentManagersCannotRemoveAllManagers() throws Exception {
        JsonNode one = find(Kind.EMPLOYEE, "NV1"), two = find(Kind.EMPLOYEE, "NV3");
        ObjectNode v1 = ((ObjectNode) one).deepCopy(), v2 = ((ObjectNode) two).deepCopy();
        v1.put("vaiTro", "NHAN_VIEN_BAN_HANG"); v2.put("vaiTro", "NHAN_VIEN_BAN_HANG");
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        try (SocketClient other = new SocketClient("127.0.0.1", server.getPort())) {
            ManagementApi peer = new ManagementApi(other, loginSession("peer"));
            Future<Boolean> a = pool.submit(() -> { start.await(); try { api.save(Kind.EMPLOYEE, request(v2, two), 1); return true; } catch (ApiException e) { assertEquals(ErrorCode.UNAUTHENTICATED, e.getCode()); return false; } });
            Future<Boolean> b = pool.submit(() -> { start.await(); try { peer.save(Kind.EMPLOYEE, request(v1, one), 1); return true; } catch (ApiException e) { assertEquals(ErrorCode.UNAUTHENTICATED, e.getCode()); return false; } });
            start.countDown(); assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(1L, (long) jpa.transaction(em -> em.createQuery("select count(t) from TaiKhoan t where t.vaiTro = :r and t.trangThai = true", Long.class).setParameter("r", VaiTro.NHAN_VIEN_QUAN_LY).getSingleResult()));
        } finally { pool.shutdownNow(); }
    }
    @Test public void allAdministrativeWritesAndReportsRequireManager() throws Exception {
        JsonNode n = find(Kind.EMPLOYEE, "NV2"), p = save(Kind.PROMOTION, promotion("KM1"), null, 0);
        session.setLogin(loginSession("sales").getLogin());
        error(ErrorCode.FORBIDDEN, () -> api.search(Kind.EMPLOYEE, new ManagementSearchRequest()));
        for (int i = 0; i < 3; i++) {
            final int mode = i;
            error(ErrorCode.FORBIDDEN, () -> save(Kind.EMPLOYEE, n, n, mode));
            error(ErrorCode.FORBIDDEN, () -> save(Kind.PROMOTION, p, p, mode));
        }
        error(ErrorCode.FORBIDDEN, () -> api.report(range(), false)); error(ErrorCode.FORBIDDEN, () -> api.report(range(), true));
        assertEquals(1, api.search(Kind.PROMOTION, new ManagementSearchRequest()).getTotal());
        ManagementSearchRequest all = new ManagementSearchRequest(); all.setIncludeInactive(true);
        error(ErrorCode.FORBIDDEN, () -> api.search(Kind.PROMOTION, all));
        session.clear(); error(ErrorCode.UNAUTHENTICATED, () -> api.search(Kind.CUSTOMER, new ManagementSearchRequest()));
    }
    @Test public void promotionBoundsDatesAndSoftStop() throws Exception {
        KhuyenMaiDTO v = promotion("KM1"); v.setPhanTramGiam(new BigDecimal("100.01"));
        error(ErrorCode.VALIDATION_ERROR, () -> save(Kind.PROMOTION, v, null, 0));
        v.setPhanTramGiam(new BigDecimal("2.001")); error(ErrorCode.VALIDATION_ERROR, () -> save(Kind.PROMOTION, v, null, 0));
        v.setPhanTramGiam(BigDecimal.ZERO); v.setNgayKetThuc(v.getNgayBatDau().minusDays(1));
        error(ErrorCode.VALIDATION_ERROR, () -> save(Kind.PROMOTION, v, null, 0));
        v.setNgayKetThuc(v.getNgayBatDau()); JsonNode out = save(Kind.PROMOTION, v, null, 0);
        assertFalse(save(Kind.PROMOTION, out, out, 2).path("trangThai").asBoolean()); assertEquals(0, api.search(Kind.PROMOTION, new ManagementSearchRequest()).getTotal());
    }
    private HoaDonDetailDTO draft(String id, String customer, String promo) throws Exception {
        HoaDonApi invoices = new HoaDonApi(socket, session); HoaDonCreateRequest create = new HoaDonCreateRequest();
        create.setMaHD(id); create.setMaThue("T1"); create.setMaKH(customer); create.setMaKM(promo);
        HoaDonDetailDTO d = invoices.create(create);
        HoaDonLineRequest line = new HoaDonLineRequest(); line.setMaHD(id); line.setMaBienThe("BT1"); line.setSoLuong(2); line.setPhienBan(d.getHoaDon().getPhienBan());
        return invoices.line(line, 0);
    }
    private HoaDonVersionRequest version(HoaDonDetailDTO d) {
        HoaDonVersionRequest r = new HoaDonVersionRequest(); r.setMaHD(d.getHoaDon().getMaHD()); r.setPhienBan(d.getHoaDon().getPhienBan()); return r;
    }
    @Test public void changedPromotionPreventsPaymentUntilDraftIsRecreated() throws Exception {
        JsonNode before = save(Kind.PROMOTION, promotion("KM1"), null, 0); HoaDonDetailDTO draft = draft("HD1", null, "KM1");
        ObjectNode value = ((ObjectNode) before).deepCopy(); value.put("phanTramGiam", 30);
        save(Kind.PROMOTION, value, before, 1);
        error(ErrorCode.VERSION_CONFLICT, () -> new HoaDonApi(socket, session).pay(version(draft)));
        assertEquals(50, (int) jpa.transaction(em -> em.find(BienThe.class, "BT1").getSoLuongTon()));
    }
    @Test public void customerAndPromotionStopPreservePaidHistory() throws Exception {
        JsonNode customer = save(Kind.CUSTOMER, customer("KH1", null), null, 0), promo = save(Kind.PROMOTION, promotion("KM1"), null, 0);
        HoaDonApi invoices = new HoaDonApi(socket, session); HoaDonDetailDTO paid = invoices.pay(version(draft("HD1", "KH1", "KM1")));
        save(Kind.CUSTOMER, customer, customer, 2); save(Kind.PROMOTION, promo, promo, 2);
        money("176.00", invoices.detail("HD1").getHoaDon().getTongTien());
        assertEquals("KH1", paid.getHoaDon().getMaKH()); money("176.00", api.report(range(), false).getTongTien());
    }
    @Test public void stoppedCustomerPreventsDraftPayment() throws Exception {
        JsonNode customer = save(Kind.CUSTOMER, customer("KH1", null), null, 0); HoaDonDetailDTO d = draft("HD1", "KH1", null);
        save(Kind.CUSTOMER, customer, customer, 2);
        error(ErrorCode.VALIDATION_ERROR, () -> new HoaDonApi(socket, session).pay(version(d)));
        assertEquals(50, (int) jpa.transaction(em -> em.find(BienThe.class, "BT1").getSoLuongTon()));
    }
    private static RevenueRequest range() {
        RevenueRequest r = new RevenueRequest(); r.setTuNgay(LocalDate.of(2026, 10, 1)); r.setDenNgay(LocalDate.of(2026, 10, 31)); return r;
    }
    private void invoiceFixture(String id, LocalDate date, TrangThaiHoaDon status, boolean secondLine) {
        jpa.transaction(em -> {
            HoaDon h = new HoaDon(); h.setMaHD(id); h.setNhanVien(em.find(NhanVien.class, "NV2")); h.setThue(em.find(Thue.class, "T1"));
            h.setNgayLap(date.atTime(23, 59, 59)); h.setTrangThai(status); h.setPhanTramGiam(new BigDecimal("20")); h.setPhanTramThue(new BigDecimal("10"));
            BigDecimal subtotal = new BigDecimal(secondLine ? "300.00" : "100.00");
            h.setTamTinh(subtotal); h.setTienGiam(subtotal.multiply(new BigDecimal("0.2"))); h.setTienThue(subtotal.multiply(new BigDecimal("0.08")));
            h.setTongTien(subtotal.multiply(new BigDecimal("0.88"))); em.persist(h);
            for (int i = 1; i <= (secondLine ? 2 : 1); i++) {
                ChiTietHoaDon c = new ChiTietHoaDon(); c.setHoaDon(h); c.setBienThe(em.find(BienThe.class, "BT" + i)); c.setSoLuong(1); c.setDonGia(new BigDecimal(i * 100)); em.persist(c);
            } return null;
        });
    }
    @Test public void revenueFiltersPaidInvoicesAndDoesNotDoubleCountHeaders() throws Exception {
        invoiceFixture("A", LocalDate.of(2026, 10, 10), TrangThaiHoaDon.DA_THANH_TOAN, false);
        invoiceFixture("B", LocalDate.of(2026, 10, 11), TrangThaiHoaDon.DA_THANH_TOAN, true);
        invoiceFixture("C", LocalDate.of(2026, 10, 11), TrangThaiHoaDon.NHAP, true);
        invoiceFixture("D", LocalDate.of(2026, 10, 11), TrangThaiHoaDon.DA_HUY, false);
        RevenueResult time = api.report(range(), false), product = api.report(range(), true);
        assertEquals(2, time.getSoHoaDon()); money("352.00", time.getTongTien()); assertEquals(time.getTongTien(), product.getTongTien());
        money("400.00", product.getTamTinh()); money("80.00", product.getTienGiam()); money("32.00", product.getTienThue());
        assertEquals(2, time.getItems().size()); assertEquals("2026-10-10", time.getItems().get(0).getLabel());
        assertEquals(2, product.getItems().size()); money("200.00", product.getItems().get(0).getAmount());
        RevenueRequest day = range(); day.setTuNgay(LocalDate.of(2026, 10, 11)); day.setDenNgay(day.getTuNgay());
        money("264.00", api.report(day, false).getTongTien());
        day.setGroup(RevenueRequest.Group.MONTH); assertEquals("2026-10", api.report(day, false).getItems().get(0).getLabel());
        day.setGroup(RevenueRequest.Group.YEAR); assertEquals("2026", api.report(day, false).getItems().get(0).getLabel());
        RevenueRequest page = range(); page.setPageSize(1); RevenueResult first = api.report(page, true); assertTrue(first.getHasNext());
        page.setPage(1); RevenueResult last = api.report(page, true); assertFalse(last.getHasNext());
        assertNotEquals(first.getItems().get(0).getCode(), last.getItems().get(0).getCode());
    }
    @Test public void emptyRevenueAndInvalidReportRange() throws Exception {
        RevenueResult empty = api.report(range(), false); assertEquals(0, empty.getSoHoaDon()); money("0.00", empty.getTongTien()); assertTrue(empty.getItems().isEmpty());
        RevenueRequest invalid = range(); invalid.setDenNgay(invalid.getTuNgay().minusDays(1));
        error(ErrorCode.VALIDATION_ERROR, () -> api.report(invalid, false));
        invalid.setDenNgay(invalid.getTuNgay().plusDays(3661)); error(ErrorCode.VALIDATION_ERROR, () -> api.report(invalid, true));
        invalid.setDenNgay(invalid.getTuNgay()); invalid.setPageSize(101); error(ErrorCode.VALIDATION_ERROR, () -> api.report(invalid, true));
    }
}
