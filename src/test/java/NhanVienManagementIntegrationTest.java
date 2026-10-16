import static org.junit.Assert.*;
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
import common.enums.VaiTro;
import common.protocol.Action;
import common.protocol.ErrorCode;
import common.protocol.JsonCodec;
import common.protocol.Response;
import server.dispatcher.RequestDispatcher;
import server.entity.NhanVien;
import server.entity.TaiKhoan;
import server.network.SocketServer;
import server.persistence.JpaManager;
import server.security.Pbkdf2PasswordHasher;
import server.service.AuthService;
import server.service.NhanVienService;
import server.session.SessionManager;

/** Backend nhân viên/tài khoản qua TCP thật và JPA/H2; không chạm SQL Server. */
public class NhanVienManagementIntegrationTest {
    private static final String PASSWORD = "ManagerPass123!";
    private JpaManager jpa;
    private SocketServer server;
    private SessionManager sessions;
    private Pbkdf2PasswordHasher hasher;
    private SocketClient socket;
    private String managerToken;
    private String secondManagerToken;
    private String salesToken;
    private String passwordHash;

    @Before public void start() throws Exception {
        String database = "employee_test_" + Integer.toHexString(System.identityHashCode(this));
        jpa = new JpaManager(CatalogTestSupport.memoryProperties(database));
        hasher = new Pbkdf2PasswordHasher();
        passwordHash = hasher.hash(PASSWORD.toCharArray());
        jpa.transaction(em -> {
            CatalogTestSupport.user(em, "manager", "NV01", VaiTro.NHAN_VIEN_QUAN_LY, passwordHash);
            CatalogTestSupport.user(em, "manager2", "NV02", VaiTro.NHAN_VIEN_QUAN_LY, passwordHash);
            CatalogTestSupport.user(em, "sales", "NV03", VaiTro.NHAN_VIEN_BAN_HANG, passwordHash);
            em.find(NhanVien.class, "NV03").setSdt("0900000003");
            NhanVien employee = employee("NV04", "0900000004", "Nhân viên chưa có tài khoản");
            em.persist(employee);
            return null;
        });
        sessions = new SessionManager();
        AuthService auth = new AuthService(jpa, sessions, hasher);
        NhanVienService service = new NhanVienService(jpa, auth, sessions, hasher);
        server = new SocketServer("127.0.0.1", 0, new RequestDispatcher(auth, null, null, service));
        server.start();
        managerToken = sessions.create("manager", passwordHash);
        secondManagerToken = sessions.create("manager2", passwordHash);
        salesToken = sessions.create("sales", passwordHash);
        socket = new SocketClient("127.0.0.1", server.getPort());
    }

    @After public void stop() {
        if (socket != null) { socket.close(); }
        if (server != null) { server.close(); }
        if (jpa != null) { jpa.close(); }
    }

    @Test public void managerCanSearchCreateAndReadSafeEmployeeAccountData() throws Exception {
        NhanVienCreateRequest create = createRequest("NV10", "0900000010", "Nguyễn Thị Demo",
                account("NV10", "demo.user", "NewPass123!", VaiTro.NHAN_VIEN_BAN_HANG));
        Response<JsonNode> created = send(Action.THEM_NHAN_VIEN, managerToken, create);
        assertTrue(created.getMessage(), created.isSuccess());
        String payload = created.getPayload().toString();
        assertFalse(payload.contains("NewPass123!"));
        assertFalse(payload.toLowerCase(java.util.Locale.ROOT).contains("passwordhash"));
        assertFalse(payload.toLowerCase(java.util.Locale.ROOT).contains("matkhau"));
        assertEquals("NV10", created.getPayload().path("nhanVien").path("maNV").asText());
        assertEquals("demo.user", created.getPayload().path("taiKhoan").path("username").asText());
        assertTrue(jpa.transaction(em -> hasher.verify("NewPass123!".toCharArray(),
                em.find(TaiKhoan.class, "demo.user").getPasswordHash())));

        NhanVienSearchRequest search = new NhanVienSearchRequest();
        search.setTuKhoa("DEMO.USER"); search.setPage(0); search.setPageSize(1);
        NhanVienSearchResult result = read(send(Action.TRA_CUU_NHAN_VIEN, managerToken, search), NhanVienSearchResult.class);
        assertEquals(1L, result.getTotal()); assertEquals(1, result.getItems().size());
        assertEquals("NV10", result.getItems().get(0).getNhanVien().getMaNV());
        NhanVienSearchRequest page = new NhanVienSearchRequest(); page.setPageSize(2); page.setPage(0);
        NhanVienSearchResult firstPage = read(send(Action.TRA_CUU_NHAN_VIEN, managerToken, page), NhanVienSearchResult.class);
        page.setPage(1);
        NhanVienSearchResult secondPage = read(send(Action.TRA_CUU_NHAN_VIEN, managerToken, page), NhanVienSearchResult.class);
        assertEquals(5L, firstPage.getTotal()); assertEquals(2, firstPage.getItems().size());
        assertEquals(2, secondPage.getItems().size());
        assertNotEquals(firstPage.getItems().get(0).getNhanVien().getMaNV(),
                secondPage.getItems().get(0).getNhanVien().getMaNV());
        NhanVienDetailRequest detail = new NhanVienDetailRequest(); detail.setMaNV("NV10");
        NhanVienRecordDTO record = read(send(Action.XEM_CHI_TIET_NHAN_VIEN, managerToken, detail), NhanVienRecordDTO.class);
        assertEquals("demo.user", record.getTaiKhoan().getUsername());
        assertNull(JsonCodec.mapper().valueToTree(record).get("passwordHash"));
    }

    @Test public void salesRoleCannotReadOrMutateEmployeeAccounts() throws Exception {
        NhanVienSearchRequest search = new NhanVienSearchRequest();
        assertError(ErrorCode.FORBIDDEN, send(Action.TRA_CUU_NHAN_VIEN, salesToken, search));
        assertError(ErrorCode.FORBIDDEN, send(Action.THEM_NHAN_VIEN, salesToken,
                createRequest("NV11", "0900000011", "Không được tạo", null)));
        assertError(ErrorCode.FORBIDDEN, send(Action.KHOA_TAI_KHOAN, salesToken, status("manager", true)));
        assertFalse(jpa.transaction(em -> em.find(NhanVien.class, "NV11") != null));
    }

    @Test public void duplicateAndInvalidCreateRollBackEmployeeAndAccountTogether() throws Exception {
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_NHAN_VIEN, managerToken,
                createRequest("NV12", "090-000-0003", "Trùng điện thoại", null)));
        assertFalse(jpa.transaction(em -> em.find(NhanVien.class, "NV12") != null));
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_NHAN_VIEN, managerToken,
                createRequest("nv03", "0900000012", "Trùng mã", null)));

        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_NHAN_VIEN, managerToken,
                createRequest("NV13", "0900000013", "Rollback username", account("NV13", "MANAGER", "NewPass123!",
                        VaiTro.NHAN_VIEN_BAN_HANG))));
        assertFalse("employee insert must roll back when account creation fails",
                jpa.transaction(em -> em.find(NhanVien.class, "NV13") != null));
        assertFalse(jpa.transaction(em -> em.find(TaiKhoan.class, "MANAGER") != null));

        NhanVienDTO invalid = new NhanVienDTO(); invalid.setMaNV("NV14"); invalid.setSdt("0900000014"); invalid.setHoTen(" ");
        NhanVienCreateRequest bad = new NhanVienCreateRequest(); bad.setNhanVien(invalid);
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.THEM_NHAN_VIEN, managerToken, bad));
        assertFalse(jpa.transaction(em -> em.find(NhanVien.class, "NV14") != null));
    }

    @Test public void profileUpdateRequiresFreshSnapshotAndUniquePhone() throws Exception {
        NhanVienDetailRequest detail = new NhanVienDetailRequest(); detail.setMaNV("NV04");
        NhanVienRecordDTO record = read(send(Action.XEM_CHI_TIET_NHAN_VIEN, managerToken, detail), NhanVienRecordDTO.class);
        NhanVienDTO expected = copy(record.getNhanVien());
        NhanVienDTO changed = copy(record.getNhanVien()); changed.setHoTen("Nhân viên đã cập nhật");
        NhanVienSaveRequest update = new NhanVienSaveRequest(); update.setExpected(expected); update.setValue(changed);
        NhanVienRecordDTO saved = read(send(Action.CAP_NHAT_NHAN_VIEN, managerToken, update), NhanVienRecordDTO.class);
        assertEquals("Nhân viên đã cập nhật", saved.getNhanVien().getHoTen());
        assertError(ErrorCode.VERSION_CONFLICT, send(Action.CAP_NHAT_NHAN_VIEN, managerToken, update));

        NhanVienDTO duplicatePhone = copy(saved.getNhanVien()); duplicatePhone.setSdt("090-000-0003");
        NhanVienSaveRequest duplicate = new NhanVienSaveRequest(); duplicate.setExpected(copy(saved.getNhanVien())); duplicate.setValue(duplicatePhone);
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.CAP_NHAT_NHAN_VIEN, managerToken, duplicate));
        assertEquals("0900000004", jpa.transaction(em -> em.find(NhanVien.class, "NV04").getSdt()));
    }

    @Test public void lockUnlockRoleChangeAndEmployeeStateRevokeOldTokens() throws Exception {
        TaiKhoanDTO account = read(send(Action.TAO_TAI_KHOAN, managerToken,
                account("NV04", "new.staff", "NewPass123!", VaiTro.NHAN_VIEN_BAN_HANG)), TaiKhoanDTO.class);
        assertEquals("new.staff", account.getUsername());
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.TAO_TAI_KHOAN, managerToken,
                account("NV04", "second.staff", "NewPass123!", VaiTro.NHAN_VIEN_BAN_HANG)));
        String firstToken = login("new.staff", "NewPass123!").getSessionToken();

        TaiKhoanStatusRequest status = new TaiKhoanStatusRequest(); status.setUsername("new.staff"); status.setExpectedTrangThai(true);
        read(send(Action.KHOA_TAI_KHOAN, managerToken, status), TaiKhoanDTO.class);
        assertError(ErrorCode.UNAUTHENTICATED, send(Action.XEM_THONG_TIN_CA_NHAN, firstToken, null));
        assertError(ErrorCode.UNAUTHENTICATED, loginResponse("new.staff", "NewPass123!"));

        status.setExpectedTrangThai(false);
        read(send(Action.MO_KHOA_TAI_KHOAN, managerToken, status), TaiKhoanDTO.class);
        assertError(ErrorCode.UNAUTHENTICATED, send(Action.XEM_THONG_TIN_CA_NHAN, firstToken, null));
        String salesTokenAfterUnlock = login("new.staff", "NewPass123!").getSessionToken();

        TaiKhoanRoleRequest role = new TaiKhoanRoleRequest(); role.setUsername("new.staff");
        role.setExpectedVaiTro(VaiTro.NHAN_VIEN_BAN_HANG); role.setVaiTro(VaiTro.NHAN_VIEN_QUAN_LY);
        TaiKhoanDTO managerAccount = read(send(Action.CAP_NHAT_VAI_TRO_TAI_KHOAN, managerToken, role), TaiKhoanDTO.class);
        assertEquals(VaiTro.NHAN_VIEN_QUAN_LY, managerAccount.getVaiTro());
        assertError(ErrorCode.UNAUTHENTICATED, send(Action.XEM_THONG_TIN_CA_NHAN, salesTokenAfterUnlock, null));
        String managerTokenAfterPromotion = login("new.staff", "NewPass123!").getSessionToken();

        NhanVienStatusRequest employeeStatus = new NhanVienStatusRequest(); employeeStatus.setMaNV("NV04");
        employeeStatus.setExpectedTrangThai(true);
        read(send(Action.NGUNG_HOAT_DONG_NHAN_VIEN, managerToken, employeeStatus), NhanVienRecordDTO.class);
        assertError(ErrorCode.UNAUTHENTICATED, send(Action.XEM_THONG_TIN_CA_NHAN, managerTokenAfterPromotion, null));
        employeeStatus.setExpectedTrangThai(false);
        read(send(Action.KICH_HOAT_NHAN_VIEN, managerToken, employeeStatus), NhanVienRecordDTO.class);
        assertError(ErrorCode.UNAUTHENTICATED, send(Action.XEM_THONG_TIN_CA_NHAN, managerTokenAfterPromotion, null));
        assertEquals(VaiTro.NHAN_VIEN_QUAN_LY, login("new.staff", "NewPass123!").getVaiTro());
    }

    @Test public void cannotDisableDemoteOrDeactivateTheLastActiveManager() throws Exception {
        TaiKhoanStatusRequest lockFirst = status("manager", true);
        read(send(Action.KHOA_TAI_KHOAN, managerToken, lockFirst), TaiKhoanDTO.class);

        assertError(ErrorCode.VALIDATION_ERROR, send(Action.KHOA_TAI_KHOAN, secondManagerToken, status("manager2", true)));
        TaiKhoanRoleRequest demote = new TaiKhoanRoleRequest(); demote.setUsername("manager2");
        demote.setExpectedVaiTro(VaiTro.NHAN_VIEN_QUAN_LY); demote.setVaiTro(VaiTro.NHAN_VIEN_BAN_HANG);
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.CAP_NHAT_VAI_TRO_TAI_KHOAN, secondManagerToken, demote));
        NhanVienStatusRequest employee = new NhanVienStatusRequest(); employee.setMaNV("NV02"); employee.setExpectedTrangThai(true);
        assertError(ErrorCode.VALIDATION_ERROR, send(Action.NGUNG_HOAT_DONG_NHAN_VIEN, secondManagerToken, employee));
        assertEquals(1L, activeManagers());

        lockFirst.setExpectedTrangThai(false);
        read(send(Action.MO_KHOA_TAI_KHOAN, secondManagerToken, lockFirst), TaiKhoanDTO.class);
        assertEquals(2L, activeManagers());
    }

    @Test public void concurrentRequestsCannotRemoveBothLastManagers() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (SocketClient first = new SocketClient("127.0.0.1", server.getPort());
             SocketClient second = new SocketClient("127.0.0.1", server.getPort())) {
            Future<Response<JsonNode>> a = workers.submit(() -> {
                ready.countDown(); start.await();
                return first.send(Action.KHOA_TAI_KHOAN, managerToken, status("manager", true));
            });
            Future<Response<JsonNode>> b = workers.submit(() -> {
                ready.countDown(); start.await();
                return second.send(Action.KHOA_TAI_KHOAN, secondManagerToken, status("manager2", true));
            });
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown();
            Response<JsonNode> ra = a.get(20, TimeUnit.SECONDS), rb = b.get(20, TimeUnit.SECONDS);
            assertEquals("exactly one request may lock a manager", 1, (ra.isSuccess() ? 1 : 0) + (rb.isSuccess() ? 1 : 0));
            Response<JsonNode> rejected = ra.isSuccess() ? rb : ra;
            assertEquals(ErrorCode.VALIDATION_ERROR, rejected.getErrorCode());
            assertEquals(1L, activeManagers());
        } finally { workers.shutdownNow(); }
    }

    private LoginResponse login(String username, String password) throws Exception {
        Response<JsonNode> response = loginResponse(username, password);
        assertTrue(response.getMessage(), response.isSuccess());
        return read(response, LoginResponse.class);
    }
    private Response<JsonNode> loginResponse(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest(); request.setUsername(username); request.setPassword(password);
        return send(Action.DANG_NHAP, null, request);
    }
    private Response<JsonNode> send(Action action, String token, Object payload) throws Exception {
        return socket.send(action, token, payload);
    }
    private static <T> T read(Response<JsonNode> response, Class<T> type) throws Exception {
        assertTrue(response.getMessage(), response.isSuccess());
        return JsonCodec.mapper().treeToValue(response.getPayload(), type);
    }
    private static void assertError(ErrorCode expected, Response<JsonNode> response) {
        assertFalse("Expected " + expected + " but got " + response.getErrorCode() + ": " + response.getMessage(), response.isSuccess());
        assertEquals(expected, response.getErrorCode());
    }
    private long activeManagers() {
        return jpa.transaction(em -> em.createQuery("select count(t) from TaiKhoan t where t.trangThai = true"
                + " and t.vaiTro = :role and t.nhanVien.trangThai = true", Long.class)
                .setParameter("role", VaiTro.NHAN_VIEN_QUAN_LY).getSingleResult());
    }
    private static NhanVienCreateRequest createRequest(String id, String phone, String name, TaiKhoanCreateRequest account) {
        NhanVienCreateRequest request = new NhanVienCreateRequest();
        NhanVienDTO data = new NhanVienDTO(); data.setMaNV(id); data.setSdt(phone); data.setHoTen(name);
        request.setNhanVien(data); request.setTaiKhoan(account); return request;
    }
    private static TaiKhoanCreateRequest account(String employeeId, String username, String password, VaiTro role) {
        TaiKhoanCreateRequest request = new TaiKhoanCreateRequest(); request.setMaNV(employeeId);
        request.setUsername(username); request.setMatKhauBanDau(password); request.setVaiTro(role); return request;
    }
    private static TaiKhoanStatusRequest status(String username, boolean expected) {
        TaiKhoanStatusRequest request = new TaiKhoanStatusRequest(); request.setUsername(username);
        request.setExpectedTrangThai(expected); return request;
    }
    private static NhanVien employee(String id, String phone, String name) {
        NhanVien employee = new NhanVien(); employee.setMaNV(id); employee.setSdt(phone); employee.setHoTen(name);
        return employee;
    }
    private static NhanVienDTO copy(NhanVienDTO source) {
        NhanVienDTO result = new NhanVienDTO(); result.setMaNV(source.getMaNV()); result.setHoTen(source.getHoTen());
        result.setGioiTinh(source.getGioiTinh()); result.setNgaySinh(source.getNgaySinh()); result.setSdt(source.getSdt());
        result.setEmail(source.getEmail()); result.setDiaChi(source.getDiaChi()); result.setChucVu(source.getChucVu());
        result.setTrangThai(source.getTrangThai()); return result;
    }
}
