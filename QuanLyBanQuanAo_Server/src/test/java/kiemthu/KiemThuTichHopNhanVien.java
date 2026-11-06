package kiemthu;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import client.network.KetNoiMayChu;
import common.dto.nhanvien.DuLieuHoSoNhanVien;
import common.dto.nhanvien.DuLieuNhanVien;
import common.dto.nhanvien.KetQuaTraCuuNhanVien;
import common.dto.nhanvien.YeuCauChiTietNhanVien;
import common.dto.nhanvien.YeuCauLuuNhanVien;
import common.dto.nhanvien.YeuCauTaoNhanVien;
import common.dto.nhanvien.YeuCauTraCuuNhanVien;
import common.dto.nhanvien.YeuCauTrangThaiNhanVien;
import common.dto.taikhoan.DuLieuTaiKhoan;
import common.dto.taikhoan.KetQuaDangNhap;
import common.dto.taikhoan.YeuCauDangNhap;
import common.dto.taikhoan.YeuCauTaoTaiKhoan;
import common.dto.taikhoan.YeuCauTrangThaiTaiKhoan;
import common.dto.taikhoan.YeuCauVaiTroTaiKhoan;
import common.enums.VaiTro;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;
import server.dispatcher.DieuPhoiYeuCau;
import server.entities.NhanVien;
import server.entities.TaiKhoan;
import server.network.MayChuTcp;
import server.security.BoBamMatKhauPbkdf2;
import server.services.DichVuNhanVien;
import server.services.DichVuXacThuc;
import server.session.QuanLyPhienDangNhap;
import server.utils.QuanLyJpa;

/** Backend nhân viên/tài khoản qua TCP thật và JPA/H2; không chạm SQL Server. */
public class KiemThuTichHopNhanVien {
    private static final String PASSWORD = "ManagerPass123!";
    private QuanLyJpa jpa;
    private MayChuTcp server;
    private QuanLyPhienDangNhap sessions;
    private BoBamMatKhauPbkdf2 hasher;
    private KetNoiMayChu socket;
    private String managerToken;
    private String secondManagerToken;
    private String salesToken;
    private String passwordHash;

    @Before
    public void start() throws Exception {
        String database = "employee_test_" + Integer.toHexString(System.identityHashCode(this));
        jpa = new QuanLyJpa(HoTroKiemThuDanhMuc.memoryProperties(database));
        hasher = new BoBamMatKhauPbkdf2();
        passwordHash = hasher.hash(PASSWORD.toCharArray());
        jpa.transaction(em -> {
            HoTroKiemThuDanhMuc.user(em, "manager", "NV01", VaiTro.NHAN_VIEN_QUAN_LY, passwordHash);
            HoTroKiemThuDanhMuc.user(em, "manager2", "NV02", VaiTro.NHAN_VIEN_QUAN_LY, passwordHash);
            HoTroKiemThuDanhMuc.user(em, "sales", "NV03", VaiTro.NHAN_VIEN_BAN_HANG, passwordHash);
            em.find(NhanVien.class, "NV03").setSdt("0900000003");
            NhanVien employee = employee("NV04", "0900000004", "Nhân viên chưa có tài khoản");
            em.persist(employee);
            return null;
        });
        sessions = new QuanLyPhienDangNhap();
        DichVuXacThuc auth = new DichVuXacThuc(jpa, sessions, hasher);
        DichVuNhanVien service = new DichVuNhanVien(jpa, auth, sessions, hasher);
        server = new MayChuTcp("127.0.0.1", 0, new DieuPhoiYeuCau(auth, null, null, service));
        server.start();
        managerToken = sessions.create("manager", passwordHash);
        secondManagerToken = sessions.create("manager2", passwordHash);
        salesToken = sessions.create("sales", passwordHash);
        socket = new KetNoiMayChu("127.0.0.1", server.getPort());
    }

    @After
    public void stop() {
        if (socket != null) {
            socket.close();
        }
        if (server != null) {
            server.close();
        }
        if (jpa != null) {
            jpa.close();
        }
    }

    @Test
    public void managerCanSearchCreateAndReadSafeEmployeeAccountData() throws Exception {
        YeuCauTaoNhanVien create = createRequest("NV10", "0900000010", "Nguyễn Thị Demo",
                account("NV10", "demo.user", "NewPass123!", VaiTro.NHAN_VIEN_BAN_HANG));
        PhanHoi<JsonNode> created = send(HanhDong.THEM_NHAN_VIEN, managerToken, create);
        assertTrue(created.getMessage(), created.isSuccess());
        String payload = created.getPayload().toString();
        assertFalse(payload.contains("NewPass123!"));
        assertFalse(payload.toLowerCase(java.util.Locale.ROOT).contains("passwordhash"));
        assertFalse(payload.toLowerCase(java.util.Locale.ROOT).contains("matkhau"));
        assertEquals("NV10", created.getPayload().path("nhanVien").path("maNV").asText());
        assertEquals("demo.user", created.getPayload().path("taiKhoan").path("username").asText());
        assertTrue(jpa.transaction(em -> hasher.verify("NewPass123!".toCharArray(),
                em.find(TaiKhoan.class, "demo.user").getPasswordHash())));

        YeuCauTraCuuNhanVien search = new YeuCauTraCuuNhanVien();
        search.setTuKhoa("DEMO.USER");
        search.setPage(0);
        search.setPageSize(1);
        KetQuaTraCuuNhanVien result = read(send(HanhDong.TRA_CUU_NHAN_VIEN, managerToken, search),
                KetQuaTraCuuNhanVien.class);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getItems().size());
        assertEquals("NV10", result.getItems().get(0).getNhanVien().getMaNV());
        YeuCauTraCuuNhanVien page = new YeuCauTraCuuNhanVien();
        page.setPageSize(2);
        page.setPage(0);
        KetQuaTraCuuNhanVien firstPage = read(send(HanhDong.TRA_CUU_NHAN_VIEN, managerToken, page),
                KetQuaTraCuuNhanVien.class);
        page.setPage(1);
        KetQuaTraCuuNhanVien secondPage = read(send(HanhDong.TRA_CUU_NHAN_VIEN, managerToken, page),
                KetQuaTraCuuNhanVien.class);
        assertEquals(5L, firstPage.getTotal());
        assertEquals(2, firstPage.getItems().size());
        assertEquals(2, secondPage.getItems().size());
        assertNotEquals(firstPage.getItems().get(0).getNhanVien().getMaNV(),
                secondPage.getItems().get(0).getNhanVien().getMaNV());
        YeuCauChiTietNhanVien detail = new YeuCauChiTietNhanVien();
        detail.setMaNV("NV10");
        DuLieuHoSoNhanVien record = read(send(HanhDong.XEM_CHI_TIET_NHAN_VIEN, managerToken, detail),
                DuLieuHoSoNhanVien.class);
        assertEquals("demo.user", record.getTaiKhoan().getUsername());
        assertNull(BoMaHoaJson.mapper().valueToTree(record).get("passwordHash"));
    }

    @Test
    public void salesRoleCannotReadOrMutateEmployeeAccounts() throws Exception {
        YeuCauTraCuuNhanVien search = new YeuCauTraCuuNhanVien();
        assertError(MaLoi.FORBIDDEN, send(HanhDong.TRA_CUU_NHAN_VIEN, salesToken, search));
        assertError(MaLoi.FORBIDDEN, send(HanhDong.THEM_NHAN_VIEN, salesToken,
                createRequest("NV11", "0900000011", "Không được tạo", null)));
        assertError(MaLoi.FORBIDDEN, send(HanhDong.KHOA_TAI_KHOAN, salesToken, status("manager", true)));
        assertFalse(jpa.transaction(em -> em.find(NhanVien.class, "NV11") != null));
    }

    @Test
    public void duplicateAndInvalidCreateRollBackEmployeeAndAccountTogether() throws Exception {
        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.THEM_NHAN_VIEN, managerToken,
                createRequest("NV12", "090-000-0003", "Trùng điện thoại", null)));
        assertFalse(jpa.transaction(em -> em.find(NhanVien.class, "NV12") != null));
        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.THEM_NHAN_VIEN, managerToken,
                createRequest("nv03", "0900000012", "Trùng mã", null)));

        assertError(MaLoi.VALIDATION_ERROR,
                send(HanhDong.THEM_NHAN_VIEN, managerToken,
                        createRequest("NV13", "0900000013", "Rollback username",
                                account("NV13", "MANAGER", "NewPass123!", VaiTro.NHAN_VIEN_BAN_HANG))));
        assertFalse("employee insert must roll back when account creation fails",
                jpa.transaction(em -> em.find(NhanVien.class, "NV13") != null));
        assertFalse(jpa.transaction(em -> em.find(TaiKhoan.class, "MANAGER") != null));

        DuLieuNhanVien invalid = new DuLieuNhanVien();
        invalid.setMaNV("NV14");
        invalid.setSdt("0900000014");
        invalid.setHoTen(" ");
        YeuCauTaoNhanVien bad = new YeuCauTaoNhanVien();
        bad.setNhanVien(invalid);
        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.THEM_NHAN_VIEN, managerToken, bad));
        assertFalse(jpa.transaction(em -> em.find(NhanVien.class, "NV14") != null));
    }

    @Test
    public void profileUpdateRequiresFreshSnapshotAndUniquePhone() throws Exception {
        YeuCauChiTietNhanVien detail = new YeuCauChiTietNhanVien();
        detail.setMaNV("NV04");
        DuLieuHoSoNhanVien record = read(send(HanhDong.XEM_CHI_TIET_NHAN_VIEN, managerToken, detail),
                DuLieuHoSoNhanVien.class);
        DuLieuNhanVien expected = copy(record.getNhanVien());
        DuLieuNhanVien changed = copy(record.getNhanVien());
        changed.setHoTen("Nhân viên đã cập nhật");
        YeuCauLuuNhanVien update = new YeuCauLuuNhanVien();
        update.setExpected(expected);
        update.setValue(changed);
        DuLieuHoSoNhanVien saved = read(send(HanhDong.CAP_NHAT_NHAN_VIEN, managerToken, update),
                DuLieuHoSoNhanVien.class);
        assertEquals("Nhân viên đã cập nhật", saved.getNhanVien().getHoTen());
        assertError(MaLoi.VERSION_CONFLICT, send(HanhDong.CAP_NHAT_NHAN_VIEN, managerToken, update));

        DuLieuNhanVien duplicatePhone = copy(saved.getNhanVien());
        duplicatePhone.setSdt("090-000-0003");
        YeuCauLuuNhanVien duplicate = new YeuCauLuuNhanVien();
        duplicate.setExpected(copy(saved.getNhanVien()));
        duplicate.setValue(duplicatePhone);
        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.CAP_NHAT_NHAN_VIEN, managerToken, duplicate));
        assertEquals("0900000004", jpa.transaction(em -> em.find(NhanVien.class, "NV04").getSdt()));
    }

    @Test
    public void lockUnlockRoleChangeAndEmployeeStateRevokeOldTokens() throws Exception {
        DuLieuTaiKhoan account = read(
                send(HanhDong.TAO_TAI_KHOAN, managerToken,
                        account("NV04", "new.staff", "NewPass123!", VaiTro.NHAN_VIEN_BAN_HANG)),
                DuLieuTaiKhoan.class);
        assertEquals("new.staff", account.getUsername());
        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.TAO_TAI_KHOAN, managerToken,
                account("NV04", "second.staff", "NewPass123!", VaiTro.NHAN_VIEN_BAN_HANG)));
        String firstToken = login("new.staff", "NewPass123!").getSessionToken();

        YeuCauTrangThaiTaiKhoan status = new YeuCauTrangThaiTaiKhoan();
        status.setUsername("new.staff");
        status.setExpectedTrangThai(true);
        read(send(HanhDong.KHOA_TAI_KHOAN, managerToken, status), DuLieuTaiKhoan.class);
        assertError(MaLoi.UNAUTHENTICATED, send(HanhDong.XEM_THONG_TIN_CA_NHAN, firstToken, null));
        assertError(MaLoi.UNAUTHENTICATED, loginResponse("new.staff", "NewPass123!"));

        status.setExpectedTrangThai(false);
        read(send(HanhDong.MO_KHOA_TAI_KHOAN, managerToken, status), DuLieuTaiKhoan.class);
        assertError(MaLoi.UNAUTHENTICATED, send(HanhDong.XEM_THONG_TIN_CA_NHAN, firstToken, null));
        String salesTokenAfterUnlock = login("new.staff", "NewPass123!").getSessionToken();

        YeuCauVaiTroTaiKhoan role = new YeuCauVaiTroTaiKhoan();
        role.setUsername("new.staff");
        role.setExpectedVaiTro(VaiTro.NHAN_VIEN_BAN_HANG);
        role.setVaiTro(VaiTro.NHAN_VIEN_QUAN_LY);
        DuLieuTaiKhoan managerAccount = read(send(HanhDong.CAP_NHAT_VAI_TRO_TAI_KHOAN, managerToken, role),
                DuLieuTaiKhoan.class);
        assertEquals(VaiTro.NHAN_VIEN_QUAN_LY, managerAccount.getVaiTro());
        assertError(MaLoi.UNAUTHENTICATED, send(HanhDong.XEM_THONG_TIN_CA_NHAN, salesTokenAfterUnlock, null));
        String managerTokenAfterPromotion = login("new.staff", "NewPass123!").getSessionToken();

        YeuCauTrangThaiNhanVien employeeStatus = new YeuCauTrangThaiNhanVien();
        employeeStatus.setMaNV("NV04");
        employeeStatus.setExpectedTrangThai(true);
        read(send(HanhDong.NGUNG_HOAT_DONG_NHAN_VIEN, managerToken, employeeStatus),
                DuLieuHoSoNhanVien.class);
        assertError(MaLoi.UNAUTHENTICATED,
                send(HanhDong.XEM_THONG_TIN_CA_NHAN, managerTokenAfterPromotion, null));
        employeeStatus.setExpectedTrangThai(false);
        read(send(HanhDong.KICH_HOAT_NHAN_VIEN, managerToken, employeeStatus), DuLieuHoSoNhanVien.class);
        assertError(MaLoi.UNAUTHENTICATED,
                send(HanhDong.XEM_THONG_TIN_CA_NHAN, managerTokenAfterPromotion, null));
        assertEquals(VaiTro.NHAN_VIEN_QUAN_LY, login("new.staff", "NewPass123!").getVaiTro());
    }

    @Test
    public void cannotDisableDemoteOrDeactivateTheLastActiveManager() throws Exception {
        YeuCauTrangThaiTaiKhoan lockFirst = status("manager", true);
        read(send(HanhDong.KHOA_TAI_KHOAN, managerToken, lockFirst), DuLieuTaiKhoan.class);

        assertError(MaLoi.VALIDATION_ERROR,
                send(HanhDong.KHOA_TAI_KHOAN, secondManagerToken, status("manager2", true)));
        YeuCauVaiTroTaiKhoan demote = new YeuCauVaiTroTaiKhoan();
        demote.setUsername("manager2");
        demote.setExpectedVaiTro(VaiTro.NHAN_VIEN_QUAN_LY);
        demote.setVaiTro(VaiTro.NHAN_VIEN_BAN_HANG);
        assertError(MaLoi.VALIDATION_ERROR,
                send(HanhDong.CAP_NHAT_VAI_TRO_TAI_KHOAN, secondManagerToken, demote));
        YeuCauTrangThaiNhanVien employee = new YeuCauTrangThaiNhanVien();
        employee.setMaNV("NV02");
        employee.setExpectedTrangThai(true);
        assertError(MaLoi.VALIDATION_ERROR,
                send(HanhDong.NGUNG_HOAT_DONG_NHAN_VIEN, secondManagerToken, employee));
        assertEquals(1L, activeManagers());

        lockFirst.setExpectedTrangThai(false);
        read(send(HanhDong.MO_KHOA_TAI_KHOAN, secondManagerToken, lockFirst), DuLieuTaiKhoan.class);
        assertEquals(2L, activeManagers());
    }

    @Test
    public void concurrentRequestsCannotRemoveBothLastManagers() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (KetNoiMayChu first = new KetNoiMayChu("127.0.0.1", server.getPort());
                KetNoiMayChu second = new KetNoiMayChu("127.0.0.1", server.getPort())) {
            Future<PhanHoi<JsonNode>> a = workers.submit(() -> {
                ready.countDown();
                start.await();
                return first.send(HanhDong.KHOA_TAI_KHOAN, managerToken, status("manager", true));
            });
            Future<PhanHoi<JsonNode>> b = workers.submit(() -> {
                ready.countDown();
                start.await();
                return second.send(HanhDong.KHOA_TAI_KHOAN, secondManagerToken, status("manager2", true));
            });
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            PhanHoi<JsonNode> ra = a.get(20, TimeUnit.SECONDS), rb = b.get(20, TimeUnit.SECONDS);
            assertEquals("exactly one request may lock a manager", 1,
                    (ra.isSuccess() ? 1 : 0) + (rb.isSuccess() ? 1 : 0));
            PhanHoi<JsonNode> rejected = ra.isSuccess() ? rb : ra;
            assertEquals(MaLoi.VALIDATION_ERROR, rejected.getErrorCode());
            assertEquals(1L, activeManagers());
        } finally {
            workers.shutdownNow();
        }
    }

    private KetQuaDangNhap login(String username, String password) throws Exception {
        PhanHoi<JsonNode> response = loginResponse(username, password);
        assertTrue(response.getMessage(), response.isSuccess());
        return read(response, KetQuaDangNhap.class);
    }

    private PhanHoi<JsonNode> loginResponse(String username, String password) throws Exception {
        YeuCauDangNhap request = new YeuCauDangNhap();
        request.setUsername(username);
        request.setPassword(password);
        return send(HanhDong.DANG_NHAP, null, request);
    }

    private PhanHoi<JsonNode> send(HanhDong action, String token, Object payload) throws Exception {
        return socket.send(action, token, payload);
    }

    private static <T> T read(PhanHoi<JsonNode> response, Class<T> type) throws Exception {
        assertTrue(response.getMessage(), response.isSuccess());
        return BoMaHoaJson.mapper().treeToValue(response.getPayload(), type);
    }

    private static void assertError(MaLoi expected, PhanHoi<JsonNode> response) {
        assertFalse(
                "Expected " + expected + " but got " + response.getErrorCode() + ": " + response.getMessage(),
                response.isSuccess());
        assertEquals(expected, response.getErrorCode());
    }

    private long activeManagers() {
        return jpa.transaction(em -> em
                .createQuery("select count(t) from TaiKhoan t where t.trangThai = true"
                        + " and t.vaiTro = :role and t.nhanVien.trangThai = true", Long.class)
                .setParameter("role", VaiTro.NHAN_VIEN_QUAN_LY).getSingleResult());
    }

    private static YeuCauTaoNhanVien createRequest(String id, String phone, String name,
            YeuCauTaoTaiKhoan account) {
        YeuCauTaoNhanVien request = new YeuCauTaoNhanVien();
        DuLieuNhanVien data = new DuLieuNhanVien();
        data.setMaNV(id);
        data.setSdt(phone);
        data.setHoTen(name);
        request.setNhanVien(data);
        request.setTaiKhoan(account);
        return request;
    }

    private static YeuCauTaoTaiKhoan account(String employeeId, String username, String password,
            VaiTro role) {
        YeuCauTaoTaiKhoan request = new YeuCauTaoTaiKhoan();
        request.setMaNV(employeeId);
        request.setUsername(username);
        request.setMatKhauBanDau(password);
        request.setVaiTro(role);
        return request;
    }

    private static YeuCauTrangThaiTaiKhoan status(String username, boolean expected) {
        YeuCauTrangThaiTaiKhoan request = new YeuCauTrangThaiTaiKhoan();
        request.setUsername(username);
        request.setExpectedTrangThai(expected);
        return request;
    }

    private static NhanVien employee(String id, String phone, String name) {
        NhanVien employee = new NhanVien();
        employee.setMaNV(id);
        employee.setSdt(phone);
        employee.setHoTen(name);
        return employee;
    }

    private static DuLieuNhanVien copy(DuLieuNhanVien source) {
        DuLieuNhanVien result = new DuLieuNhanVien();
        result.setMaNV(source.getMaNV());
        result.setHoTen(source.getHoTen());
        result.setGioiTinh(source.getGioiTinh());
        result.setNgaySinh(source.getNgaySinh());
        result.setSdt(source.getSdt());
        result.setEmail(source.getEmail());
        result.setDiaChi(source.getDiaChi());
        result.setChucVu(source.getChucVu());
        result.setTrangThai(source.getTrangThai());
        return result;
    }
}
