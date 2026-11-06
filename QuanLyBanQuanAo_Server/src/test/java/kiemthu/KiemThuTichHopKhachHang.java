package kiemthu;

import static org.junit.Assert.*;

import java.math.BigDecimal;
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
import common.dto.hoadon.DuLieuDayDuHoaDon;
import common.dto.hoadon.YeuCauDongHoaDon;
import common.dto.hoadon.YeuCauPhienBanHoaDon;
import common.dto.hoadon.YeuCauTaoHoaDon;
import common.dto.hoadon.YeuCauThanhToanHoaDon;
import common.dto.khachhang.DuLieuKhachHang;
import common.dto.khachhang.KetQuaTraCuuKhachHang;
import common.dto.khachhang.YeuCauChiTietKhachHang;
import common.dto.khachhang.YeuCauLuuKhachHang;
import common.dto.khachhang.YeuCauTraCuuKhachHang;
import common.dto.khachhang.YeuCauTrangThaiKhachHang;
import common.enums.PhuongThucThanhToan;
import common.enums.VaiTro;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;
import server.dispatcher.DieuPhoiYeuCau;
import server.entities.BienThe;
import server.entities.KhachHang;
import server.entities.LoaiSanPham;
import server.entities.SanPham;
import server.entities.Thue;
import server.entities.ThuongHieu;
import server.network.MayChuTcp;
import server.security.BoBamMatKhauPbkdf2;
import server.services.DichVuHoaDon;
import server.services.DichVuKhachHang;
import server.services.DichVuSanPham;
import server.services.DichVuXacThuc;
import server.session.QuanLyPhienDangNhap;
import server.utils.QuanLyJpa;

/** Khách hàng qua TCP + JPA/H2; không kết nối database SQL Server. */
public class KiemThuTichHopKhachHang {
    private static final String PASSWORD = "CustomerPass123!";
    private QuanLyJpa jpa;
    private MayChuTcp server;
    private KetNoiMayChu socket;
    private QuanLyPhienDangNhap sessions;
    private String managerToken, salesToken, secondSalesToken;

    @Before
    public void start() throws Exception {
        String database = "customer_test_" + Integer.toHexString(System.identityHashCode(this));
        jpa = new QuanLyJpa(HoTroKiemThuDanhMuc.memoryProperties(database));
        String hash = new BoBamMatKhauPbkdf2().hash(PASSWORD.toCharArray());
        jpa.transaction(em -> {
            HoTroKiemThuDanhMuc.user(em, "manager", "NV01", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            HoTroKiemThuDanhMuc.user(em, "sales", "NV02", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            HoTroKiemThuDanhMuc.user(em, "sales2", "NV03", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            LoaiSanPham category = new LoaiSanPham();
            category.setMaLoai("L1");
            category.setTenLoai("Áo");
            em.persist(category);
            ThuongHieu brand = new ThuongHieu();
            brand.setMaThuongHieu("TH1");
            brand.setTenThuongHieu("Demo");
            em.persist(brand);
            SanPham product = new SanPham();
            product.setMaSP("SP1");
            product.setTenSP("Áo thử");
            product.setLoaiSanPham(category);
            product.setThuongHieu(brand);
            product.setGiaBan(new BigDecimal("100"));
            em.persist(product);
            BienThe variant = new BienThe();
            variant.setMaBienThe("BT1");
            variant.setSanPham(product);
            variant.setMauSac("Đen");
            variant.setKichCo("M");
            variant.setSoLuongTon(10);
            em.persist(variant);
            Thue tax = new Thue();
            tax.setMaThue("T1");
            tax.setTenThue("Thuế thử");
            tax.setPhanTram(new BigDecimal("0"));
            em.persist(tax);
            return null;
        });
        sessions = new QuanLyPhienDangNhap();
        DichVuXacThuc auth = new DichVuXacThuc(jpa, sessions, new BoBamMatKhauPbkdf2());
        DichVuKhachHang customers = new DichVuKhachHang(jpa, auth);
        server = new MayChuTcp("127.0.0.1", 0, new DieuPhoiYeuCau(auth, new DichVuSanPham(jpa, auth),
                new DichVuHoaDon(jpa, auth), null, customers));
        server.start();
        managerToken = sessions.create("manager", hash);
        salesToken = sessions.create("sales", hash);
        secondSalesToken = sessions.create("sales2", hash);
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
    public void bothEmployeeRolesCanManageCustomersAndSearchIsPagedByCodeNameAndPhone() throws Exception {
        assertTrue(send(HanhDong.THEM_KHACH_HANG, managerToken, customer("KH03", "Thảo", "0900000003", null))
                .isSuccess());
        assertTrue(
                send(HanhDong.THEM_KHACH_HANG, salesToken, customer("KH01", "Nguyễn An", "0900000001", null))
                        .isSuccess());
        assertTrue(send(HanhDong.THEM_KHACH_HANG, salesToken,
                customer("KH02", "Nguyễn Bình", "0900000002", null)).isSuccess());

        YeuCauTraCuuKhachHang search = new YeuCauTraCuuKhachHang();
        search.setTuKhoa("Nguyễn");
        search.setPageSize(1);
        KetQuaTraCuuKhachHang first = read(send(HanhDong.TRA_CUU_KHACH_HANG, managerToken, search),
                KetQuaTraCuuKhachHang.class);
        assertEquals(2L, first.getTotal());
        assertEquals("KH01", first.getItems().get(0).getMaKH());
        search.setPage(1);
        assertEquals("KH02",
                read(send(HanhDong.TRA_CUU_KHACH_HANG, salesToken, search), KetQuaTraCuuKhachHang.class)
                        .getItems().get(0).getMaKH());
        search.setPage(0);
        search.setPageSize(20);
        search.setTuKhoa("0900000002");
        assertEquals("KH02",
                read(send(HanhDong.TRA_CUU_KHACH_HANG, salesToken, search), KetQuaTraCuuKhachHang.class)
                        .getItems().get(0).getMaKH());
        search.setTuKhoa("kh03");
        assertEquals("KH03",
                read(send(HanhDong.TRA_CUU_KHACH_HANG, salesToken, search), KetQuaTraCuuKhachHang.class)
                        .getItems().get(0).getMaKH());
        assertError(MaLoi.UNAUTHENTICATED,
                send(HanhDong.TRA_CUU_KHACH_HANG, null, new YeuCauTraCuuKhachHang()));
    }

    @Test
    public void createNormalizesUnicodePhoneAndNullsAndRejectsDuplicatesAndInvalidValues() throws Exception {
        DuLieuKhachHang created = read(send(HanhDong.THEM_KHACH_HANG, salesToken, customer(" kh10 ",
                "\u00a0Cafe\u0301 Demo\u00a0", " 090 (123)-4567 ", " user@xn--bro-vla.example ")),
                DuLieuKhachHang.class);
        assertEquals("KH10", created.getMaKH());
        assertEquals("Café Demo", created.getHoTen());
        assertEquals("0901234567", created.getSdt());
        assertEquals("user@xn--bro-vla.example", created.getEmail());
        assertEquals("0901234567", jpa.transaction(em -> em.find(KhachHang.class, "KH10").getSdt()));
        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.THEM_KHACH_HANG, salesToken,
                customer("KH11", "Khách trùng", "090.123.4567", null)));

        DuLieuKhachHang noPhone = read(
                send(HanhDong.THEM_KHACH_HANG, salesToken, customer("KH12", "Khách không SĐT", "   ", null)),
                DuLieuKhachHang.class);
        assertNull(noPhone.getSdt());
        assertNull(jpa.transaction(em -> em.find(KhachHang.class, "KH12").getSdt()));
        assertTrue(send(HanhDong.THEM_KHACH_HANG, managerToken,
                customer("KH13", "Khách không SĐT 2", null, null)).isSuccess());

        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.THEM_KHACH_HANG, salesToken,
                customer("KH14", "Sai email", null, "bad@@example.com")));
        assertError(MaLoi.VALIDATION_ERROR,
                send(HanhDong.THEM_KHACH_HANG, salesToken, customer("KH15", "Sai SĐT", "090-abc", null)));
        assertError(MaLoi.VALIDATION_ERROR,
                send(HanhDong.THEM_KHACH_HANG, salesToken, customer("KH16", " ", null, null)));
        char[] longAddress = new char[256];
        java.util.Arrays.fill(longAddress, 'x');
        DuLieuKhachHang tooLong = customer("KH17", "Tên", null, null);
        tooLong.setDiaChi(new String(longAddress));
        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.THEM_KHACH_HANG, salesToken, tooLong));
    }

    @Test
    public void updateAndStateChangesRejectStaleSnapshotsAndKeepInactiveCustomerReadable() throws Exception {
        DuLieuKhachHang original = create("KH20", "Khách cũ", "0900020", "old@example.com");
        DuLieuKhachHang changed = copy(original);
        changed.setHoTen("Khách mới");
        changed.setEmail("new@example.com");
        YeuCauLuuKhachHang update = new YeuCauLuuKhachHang();
        update.setExpected(original);
        update.setValue(changed);
        DuLieuKhachHang saved = read(send(HanhDong.CAP_NHAT_KHACH_HANG, salesToken, update),
                DuLieuKhachHang.class);
        assertEquals("Khách mới", saved.getHoTen());
        assertEquals("new@example.com", saved.getEmail());
        assertError(MaLoi.VERSION_CONFLICT, send(HanhDong.CAP_NHAT_KHACH_HANG, managerToken, update));

        YeuCauTrangThaiKhachHang status = status("KH20", true);
        DuLieuKhachHang inactive = read(send(HanhDong.NGUNG_HOAT_DONG_KHACH_HANG, salesToken, status),
                DuLieuKhachHang.class);
        assertFalse(inactive.getTrangThai());
        YeuCauChiTietKhachHang detail = new YeuCauChiTietKhachHang();
        detail.setMaKH("KH20");
        assertFalse(read(send(HanhDong.XEM_CHI_TIET_KHACH_HANG, salesToken, detail), DuLieuKhachHang.class)
                .getTrangThai());
        assertError(MaLoi.VERSION_CONFLICT, send(HanhDong.NGUNG_HOAT_DONG_KHACH_HANG, managerToken, status));
        status.setExpectedTrangThai(false);
        assertTrue(read(send(HanhDong.KICH_HOAT_KHACH_HANG, managerToken, status), DuLieuKhachHang.class)
                .getTrangThai());
    }

    @Test
    public void concurrentEditsFromSameSnapshotHaveOneWinner() throws Exception {
        DuLieuKhachHang original = create("KH30", "Cũ", null, null);
        DuLieuKhachHang firstValue = copy(original);
        firstValue.setHoTen("Người sửa 1");
        DuLieuKhachHang secondValue = copy(original);
        secondValue.setHoTen("Người sửa 2");
        YeuCauLuuKhachHang firstRequest = save(original, firstValue),
                secondRequest = save(original, secondValue);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (KetNoiMayChu first = new KetNoiMayChu("127.0.0.1", server.getPort());
                KetNoiMayChu second = new KetNoiMayChu("127.0.0.1", server.getPort())) {
            Future<PhanHoi<JsonNode>> a = workers.submit(() -> {
                ready.countDown();
                start.await();
                return first.send(HanhDong.CAP_NHAT_KHACH_HANG, salesToken, firstRequest);
            });
            Future<PhanHoi<JsonNode>> b = workers.submit(() -> {
                ready.countDown();
                start.await();
                return second.send(HanhDong.CAP_NHAT_KHACH_HANG, secondSalesToken, secondRequest);
            });
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            PhanHoi<JsonNode> ra = a.get(20, TimeUnit.SECONDS), rb = b.get(20, TimeUnit.SECONDS);
            assertEquals(1, (ra.isSuccess() ? 1 : 0) + (rb.isSuccess() ? 1 : 0));
            PhanHoi<JsonNode> rejected = ra.isSuccess() ? rb : ra;
            assertEquals(MaLoi.VERSION_CONFLICT, rejected.getErrorCode());
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    public void inactiveCustomerCannotStartOrPaySaleButPaidInvoiceHistoryRemainsLinked() throws Exception {
        create("KH40", "Khách mua", null, null);
        DuLieuDayDuHoaDon paidDraft = invoice("HD40", "KH40");
        paidDraft = addLine(paidDraft);
        paidDraft = read(send(HanhDong.THANH_TOAN_HOA_DON, salesToken, payment(paidDraft)),
                DuLieuDayDuHoaDon.class);
        assertEquals("DA_THANH_TOAN", paidDraft.getHoaDon().getTrangThai().name());

        DuLieuDayDuHoaDon openDraft = invoice("HD41", "KH40");
        openDraft = addLine(openDraft);
        DuLieuKhachHang inactive = read(
                send(HanhDong.NGUNG_HOAT_DONG_KHACH_HANG, salesToken, status("KH40", true)),
                DuLieuKhachHang.class);
        assertFalse(inactive.getTrangThai());
        assertError(MaLoi.VALIDATION_ERROR,
                send(HanhDong.THANH_TOAN_HOA_DON, salesToken, payment(openDraft)));
        assertError(MaLoi.VALIDATION_ERROR,
                send(HanhDong.LAP_HOA_DON, salesToken, invoiceRequest("HD42", "KH40")));
        YeuCauPhienBanHoaDon detail = new YeuCauPhienBanHoaDon();
        detail.setMaHD("HD40");
        DuLieuDayDuHoaDon historical = read(send(HanhDong.XEM_CHI_TIET_HOA_DON, salesToken, detail),
                DuLieuDayDuHoaDon.class);
        assertEquals("KH40", historical.getHoaDon().getMaKH());
    }

    private DuLieuKhachHang create(String id, String name, String phone, String email) throws Exception {
        return read(send(HanhDong.THEM_KHACH_HANG, salesToken, customer(id, name, phone, email)),
                DuLieuKhachHang.class);
    }

    private DuLieuDayDuHoaDon invoice(String id, String customerId) throws Exception {
        return read(send(HanhDong.LAP_HOA_DON, salesToken, invoiceRequest(id, customerId)),
                DuLieuDayDuHoaDon.class);
    }

    private DuLieuDayDuHoaDon addLine(DuLieuDayDuHoaDon invoice) throws Exception {
        YeuCauDongHoaDon request = new YeuCauDongHoaDon();
        request.setMaHD(invoice.getHoaDon().getMaHD());
        request.setPhienBan(invoice.getHoaDon().getPhienBan());
        request.setMaBienThe("BT1");
        request.setSoLuong(1);
        return read(send(HanhDong.THEM_CHI_TIET_HOA_DON, salesToken, request), DuLieuDayDuHoaDon.class);
    }

    private static YeuCauTaoHoaDon invoiceRequest(String id, String customerId) {
        YeuCauTaoHoaDon request = new YeuCauTaoHoaDon();
        request.setMaHD(id);
        request.setMaThue("T1");
        request.setMaKH(customerId);
        return request;
    }

    private static YeuCauPhienBanHoaDon version(DuLieuDayDuHoaDon detail) {
        YeuCauPhienBanHoaDon request = new YeuCauPhienBanHoaDon();
        request.setMaHD(detail.getHoaDon().getMaHD());
        request.setPhienBan(detail.getHoaDon().getPhienBan());
        return request;
    }

    private static YeuCauThanhToanHoaDon payment(DuLieuDayDuHoaDon detail) {
        YeuCauThanhToanHoaDon request = new YeuCauThanhToanHoaDon();
        request.setMaHD(detail.getHoaDon().getMaHD());
        request.setPhienBan(detail.getHoaDon().getPhienBan());
        request.setTongTienDaXem(detail.getHoaDon().getTongTien());
        request.setPhuongThucThanhToan(PhuongThucThanhToan.TIEN_MAT);
        request.setTienKhachDua(detail.getHoaDon().getTongTien());
        return request;
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

    private static DuLieuKhachHang customer(String id, String name, String phone, String email) {
        DuLieuKhachHang dto = new DuLieuKhachHang();
        dto.setMaKH(id);
        dto.setHoTen(name);
        dto.setSdt(phone);
        dto.setEmail(email);
        return dto;
    }

    private static DuLieuKhachHang copy(DuLieuKhachHang source) {
        DuLieuKhachHang dto = new DuLieuKhachHang();
        dto.setMaKH(source.getMaKH());
        dto.setHoTen(source.getHoTen());
        dto.setSdt(source.getSdt());
        dto.setEmail(source.getEmail());
        dto.setDiaChi(source.getDiaChi());
        dto.setTrangThai(source.getTrangThai());
        return dto;
    }

    private static YeuCauLuuKhachHang save(DuLieuKhachHang expected, DuLieuKhachHang value) {
        YeuCauLuuKhachHang request = new YeuCauLuuKhachHang();
        request.setExpected(expected);
        request.setValue(value);
        return request;
    }

    private static YeuCauTrangThaiKhachHang status(String id, boolean expected) {
        YeuCauTrangThaiKhachHang request = new YeuCauTrangThaiKhachHang();
        request.setMaKH(id);
        request.setExpectedTrangThai(expected);
        return request;
    }
}
