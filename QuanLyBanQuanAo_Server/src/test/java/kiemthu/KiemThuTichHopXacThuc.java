package kiemthu;

import static org.junit.Assert.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import client.api.GoiDichVuXacThuc;
import client.api.NgoaiLeGoiDichVu;
import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.nhanvien.DuLieuNhanVien;
import common.dto.taikhoan.KetQuaDangNhap;
import common.enums.VaiTro;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;
import common.protocol.TruyenKhungTin;
import server.dispatcher.DieuPhoiYeuCau;
import server.entities.NhanVien;
import server.entities.TaiKhoan;
import server.network.MayChuTcp;
import server.security.BoBamMatKhauPbkdf2;
import server.services.DichVuXacThuc;
import server.session.QuanLyPhienDangNhap;
import server.utils.QuanLyJpa;

/** JPA thật + Socket thật trên loopback; không kết nối SQL Server người dùng. */
public class KiemThuTichHopXacThuc {
    private static QuanLyJpa jpa;
    private static String hash;
    private static MayChuTcp server;
    private static QuanLyPhienDangNhap sessions;
    private KetNoiMayChu socket;
    private PhienDangNhap session;
    private GoiDichVuXacThuc api;

    @BeforeClass
    public static void startServer() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("javax.persistence.jdbc.driver", "org.h2.Driver");
        properties.put("javax.persistence.jdbc.url",
                "jdbc:h2:mem:auth_test;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS dbo");
        properties.put("javax.persistence.jdbc.user", "sa");
        properties.put("javax.persistence.jdbc.password", "");
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        jpa = new QuanLyJpa(properties);
        hash = new BoBamMatKhauPbkdf2().hash("CorrectPass123!".toCharArray());
        seed("sales", "NV001", "0900000001", VaiTro.NHAN_VIEN_BAN_HANG);
        seed("manager", "NV002", "0900000002", VaiTro.NHAN_VIEN_QUAN_LY);
        sessions = new QuanLyPhienDangNhap();
        server = new MayChuTcp("127.0.0.1", 0,
                new DieuPhoiYeuCau(new DichVuXacThuc(jpa, sessions, new BoBamMatKhauPbkdf2())));
        server.start();
    }

    private static void seed(String username, String id, String phone, VaiTro role) {
        jpa.transaction(em -> {
            NhanVien employee = new NhanVien();
            employee.setMaNV(id);
            employee.setHoTen("Nguyễn Văn " + username);
            employee.setSdt(phone);
            employee.setNgaySinh(LocalDate.of(2000, 1, 2));
            em.persist(employee);
            TaiKhoan account = new TaiKhoan();
            account.setUsername(username);
            account.setNhanVien(employee);
            account.setPasswordHash(hash);
            account.setVaiTro(role);
            em.persist(account);
            return null;
        });
    }

    @Before
    public void before() {
        jpa.transaction(em -> {
            for (TaiKhoan account : em.createQuery("select t from TaiKhoan t", TaiKhoan.class)
                    .getResultList()) {
                account.setTrangThai(true);
                account.getNhanVien().setTrangThai(true);
                account.setPasswordHash(hash);
            }
            return null;
        });
        sessions.revokeUser("sales");
        sessions.revokeUser("manager");
        socket = new KetNoiMayChu("127.0.0.1", server.getPort());
        session = new PhienDangNhap();
        api = new GoiDichVuXacThuc(socket, session);
    }

    @After
    public void closeClient() {
        socket.close();
    }

    @AfterClass
    public static void stopServer() {
        if (server != null) {
            server.close();
        }
        if (jpa != null) {
            jpa.close();
        }
    }

    @Test
    public void loginProfileAndLogoutThroughSocket() throws Exception {
        KetQuaDangNhap login = api.login("sales", "CorrectPass123!");
        assertEquals(VaiTro.NHAN_VIEN_BAN_HANG, login.getVaiTro());
        assertNotNull(login.getSessionToken());
        DuLieuNhanVien profile = api.profile();
        assertEquals("NV001", profile.getMaNV());
        assertEquals(LocalDate.of(2000, 1, 2), profile.getNgaySinh());
        String json = BoMaHoaJson.mapper().writeValueAsString(profile);
        assertFalse(json.contains("passwordHash"));
        api.logout();
        assertNull(session.getToken());
        assertApiError(MaLoi.UNAUTHENTICATED, () -> api.profile());
    }

    @Test
    public void managerRoleComesFromDatabase() throws Exception {
        assertEquals(VaiTro.NHAN_VIEN_QUAN_LY, api.login("manager", "CorrectPass123!").getVaiTro());
    }

    @Test
    public void wrongPasswordUnknownAndDisabledAreRejected() throws Exception {
        assertApiError(MaLoi.UNAUTHENTICATED, () -> api.login("sales", "wrong"));
        assertApiError(MaLoi.UNAUTHENTICATED, () -> api.login("missing", "wrong"));
        jpa.transaction(em -> {
            em.find(TaiKhoan.class, "sales").setTrangThai(false);
            return null;
        });
        assertApiError(MaLoi.UNAUTHENTICATED, () -> api.login("sales", "CorrectPass123!"));
    }

    @Test
    public void revokedEmployeeInvalidatesExistingSession() throws Exception {
        api.login("sales", "CorrectPass123!");
        jpa.transaction(em -> {
            em.find(NhanVien.class, "NV001").setTrangThai(false);
            return null;
        });
        assertApiError(MaLoi.UNAUTHENTICATED, () -> api.profile());
        assertNull(session.getToken());
    }

    @Test
    public void passwordChangePersistsAndInvalidatesEverySession() throws Exception {
        api.login("sales", "CorrectPass123!");
        try (KetNoiMayChu secondSocket = new KetNoiMayChu("127.0.0.1", server.getPort())) {
            GoiDichVuXacThuc second = new GoiDichVuXacThuc(secondSocket, new PhienDangNhap());
            second.login("sales", "CorrectPass123!");
            assertApiError(MaLoi.VALIDATION_ERROR, () -> api.changePassword("wrong", "NewPass123!"));
            assertEquals("NV001", api.profile().getMaNV()); // đổi sai không làm mất phiên
            api.changePassword("CorrectPass123!", "NewPass123!");
            assertNull(session.getToken());
            assertApiError(MaLoi.UNAUTHENTICATED, () -> second.profile());
        }
        assertApiError(MaLoi.UNAUTHENTICATED, () -> api.login("sales", "CorrectPass123!"));
        assertEquals("NV001", api.login("sales", "NewPass123!").getMaNV());
        String stored = jpa.transaction(em -> em.find(TaiKhoan.class, "sales").getPasswordHash());
        assertNotEquals("NewPass123!", stored);
        assertTrue(stored.startsWith("pbkdf2-sha256$"));
    }

    @Test
    public void invalidPayloadDoesNotDisconnectOtherRequests() throws Exception {
        PhanHoi<?> result = socket.send(HanhDong.DANG_NHAP, null, "not an object");
        assertEquals(MaLoi.INVALID_REQUEST, result.getErrorCode());
        assertNotNull(api.login("sales", "CorrectPass123!"));
    }

    @Test
    public void forgedTokenAndEmployeeIdCannotSelectAnotherProfile() throws Exception {
        assertEquals(MaLoi.UNAUTHENTICATED,
                socket.send(HanhDong.XEM_THONG_TIN_CA_NHAN, "forged-token", null).getErrorCode());
        KetQuaDangNhap login = api.login("sales", "CorrectPass123!");
        Map<String, String> forgedPayload = new HashMap<>();
        forgedPayload.put("maNV", "NV002");
        com.fasterxml.jackson.databind.JsonNode profile = socket
                .send(HanhDong.XEM_THONG_TIN_CA_NHAN, login.getSessionToken(), forgedPayload).getPayload();
        assertEquals("NV001", profile.get("maNV").asText());
        sessions.revokeUser("sales");
        api.logout(); // phiên hết hạn vẫn trở về giao diện đăng nhập được
        assertNull(session.getToken());
    }

    @Test
    public void concurrentClientsHaveIndependentEntityManagers() throws Exception {
        api.login("sales", "CorrectPass123!");
        try (KetNoiMayChu otherSocket = new KetNoiMayChu("127.0.0.1", server.getPort())) {
            GoiDichVuXacThuc other = new GoiDichVuXacThuc(otherSocket, new PhienDangNhap());
            other.login("manager", "CorrectPass123!");
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors
                    .newFixedThreadPool(2);
            try {
                java.util.concurrent.Future<String> first = executor.submit(() -> api.profile().getMaNV());
                java.util.concurrent.Future<String> second = executor.submit(() -> other.profile().getMaNV());
                assertEquals("NV001", first.get(5, java.util.concurrent.TimeUnit.SECONDS));
                assertEquals("NV002", second.get(5, java.util.concurrent.TimeUnit.SECONDS));
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void transactionRollbackPreservesData() {
        try {
            jpa.transaction(em -> {
                em.find(NhanVien.class, "NV001").setHoTen("Must rollback");
                throw new IllegalStateException("test");
            });
            fail("Should rollback");
        } catch (IllegalStateException expected) {
        }
        assertEquals("Nguyễn Văn sales", jpa.transaction(em -> em.find(NhanVien.class, "NV001").getHoTen()));
    }

    @Test
    public void oversizedFrameClosesOnlyBadConnection() throws Exception {
        try (Socket bad = new Socket("127.0.0.1", server.getPort())) {
            bad.setSoTimeout(3000);
            DataOutputStream output = new DataOutputStream(bad.getOutputStream());
            output.writeInt(TruyenKhungTin.MAX_FRAME_BYTES + 1);
            output.flush();
            assertEquals(-1, bad.getInputStream().read());
        }
        assertNotNull(api.login("sales", "CorrectPass123!"));
    }

    @Test
    public void boundedWorkerQueueRejectsExcessConnectionsAndServerRecovers() throws Exception {
        List<Socket> idleClients = new ArrayList<Socket>();
        boolean observedRejection = false;
        try {
            // 16 workers + queue capacity 64; the excess accepted sockets must be closed.
            for (int i = 0; i < 96; i++) {
                idleClients.add(new Socket("127.0.0.1", server.getPort()));
            }
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
            while (!observedRejection && System.nanoTime() < deadline) {
                for (int i = 80; i < idleClients.size(); i++) {
                    Socket client = idleClients.get(i);
                    try {
                        client.setSoTimeout(20);
                        if (client.getInputStream().read() == -1) {
                            observedRejection = true;
                            break;
                        }
                    } catch (SocketTimeoutException stillOpen) {
                    }
                }
            }
            assertTrue("excess idle clients should be shed when the bounded queue is full",
                    observedRejection);
        } finally {
            for (Socket client : idleClients) {
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }

        try (KetNoiMayChu recovered = new KetNoiMayChu("127.0.0.1", server.getPort())) {
            GoiDichVuXacThuc recoveryApi = new GoiDichVuXacThuc(recovered, new PhienDangNhap());
            assertNotNull("server should keep accepting after shedding excess clients",
                    recoveryApi.login("sales", "CorrectPass123!"));
        }
    }

    @Test
    public void splitFrameIsReadCorrectlyAndMalformedJsonIsRejected() throws Exception {
        try (Socket raw = new Socket("127.0.0.1", server.getPort())) {
            raw.setSoTimeout(3000);
            byte[] frame = "{bad json}".getBytes("UTF-8");
            DataOutputStream output = new DataOutputStream(raw.getOutputStream());
            output.writeInt(frame.length);
            for (byte b : frame) {
                output.writeByte(b);
                output.flush();
            }
            byte[] response = TruyenKhungTin.read(new DataInputStream(raw.getInputStream()));
            assertTrue(new String(response, "UTF-8").contains("INVALID_REQUEST"));
        }
    }

    private interface Task {
        void run() throws Exception;
    }

    private static void assertApiError(MaLoi code, Task task) throws Exception {
        try {
            task.run();
            fail("Expected " + code);
        } catch (NgoaiLeGoiDichVu expected) {
            assertEquals(code, expected.getCode());
        }
    }
}
