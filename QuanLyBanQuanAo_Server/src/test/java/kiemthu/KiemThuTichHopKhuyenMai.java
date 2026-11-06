package kiemthu;

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
import common.dto.khuyenmai.DuLieuKhuyenMai;
import common.dto.khuyenmai.KetQuaTraCuuKhuyenMai;
import common.dto.khuyenmai.YeuCauChiTietKhuyenMai;
import common.dto.khuyenmai.YeuCauLuuKhuyenMai;
import common.dto.khuyenmai.YeuCauTraCuuKhuyenMai;
import common.dto.khuyenmai.YeuCauTrangThaiKhuyenMai;
import common.enums.PhuongThucThanhToan;
import common.enums.TrangThaiHieuLucKhuyenMai;
import common.enums.TrangThaiHoaDon;
import common.enums.VaiTro;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;
import server.dispatcher.DieuPhoiYeuCau;
import server.entities.BienThe;
import server.entities.LoaiSanPham;
import server.entities.SanPham;
import server.entities.Thue;
import server.entities.ThuongHieu;
import server.network.MayChuTcp;
import server.security.BoBamMatKhauPbkdf2;
import server.services.DichVuHoaDon;
import server.services.DichVuKhuyenMai;
import server.services.DichVuSanPham;
import server.services.DichVuXacThuc;
import server.session.QuanLyPhienDangNhap;
import server.utils.QuanLyJpa;

/** Khuyến mãi qua TCP + JPA/H2; không kết nối SQL Server. */
public class KiemThuTichHopKhuyenMai {
    private static final String PASSWORD = "PromotionPass123!";
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private QuanLyJpa jpa;
    private MayChuTcp server;
    private KetNoiMayChu socket;
    private String managerToken, salesToken, secondManagerToken;
    private LocalDate today;

    @Before
    public void start() throws Exception {
        today = LocalDate.now(ZONE);
        Clock clock = Clock.fixed(today.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
        String database = "promotion_test_" + Integer.toHexString(System.identityHashCode(this));
        jpa = new QuanLyJpa(HoTroKiemThuDanhMuc.memoryProperties(database));
        String hash = new BoBamMatKhauPbkdf2().hash(PASSWORD.toCharArray());
        jpa.transaction(em -> {
            HoTroKiemThuDanhMuc.user(em, "manager", "NV01", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            HoTroKiemThuDanhMuc.user(em, "manager2", "NV02", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            HoTroKiemThuDanhMuc.user(em, "sales", "NV03", VaiTro.NHAN_VIEN_BAN_HANG, hash);
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
            product.setGiaBan(new BigDecimal("100.00"));
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
            tax.setPhanTram(new BigDecimal("0.00"));
            em.persist(tax);
            return null;
        });
        QuanLyPhienDangNhap sessions = new QuanLyPhienDangNhap();
        DichVuXacThuc auth = new DichVuXacThuc(jpa, sessions, new BoBamMatKhauPbkdf2());
        DichVuKhuyenMai promotions = new DichVuKhuyenMai(jpa, auth, clock);
        server = new MayChuTcp("127.0.0.1", 0, new DieuPhoiYeuCau(auth, new DichVuSanPham(jpa, auth),
                new DichVuHoaDon(jpa, auth), null, null, promotions));
        server.start();
        managerToken = sessions.create("manager", hash);
        secondManagerToken = sessions.create("manager2", hash);
        salesToken = sessions.create("sales", hash);
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
    public void salesSeesInclusiveStartAndEndDaysWhileManagerSeesDerivedStatesAndPagination()
            throws Exception {
        create("KM01", new BigDecimal("10"), today, today.plusDays(1));
        create("KM02", new BigDecimal("20"), today.minusDays(1), today);
        create("KM03", new BigDecimal("30"), today.plusDays(1), today.plusDays(2));
        create("KM04", new BigDecimal("40"), today.minusDays(2), today.minusDays(1));
        DuLieuKhuyenMai disabled = create("KM05", new BigDecimal("50"), today.minusDays(1),
                today.plusDays(1));
        read(send(HanhDong.NGUNG_HOAT_DONG_KHUYEN_MAI, managerToken, status(disabled)),
                DuLieuKhuyenMai.class);

        YeuCauTraCuuKhuyenMai search = new YeuCauTraCuuKhuyenMai();
        search.setPageSize(1);
        KetQuaTraCuuKhuyenMai first = read(send(HanhDong.TRA_CUU_KHUYEN_MAI, salesToken, search),
                KetQuaTraCuuKhuyenMai.class);
        assertEquals(2L, first.getTotal());
        assertEquals("KM01", first.getItems().get(0).getMaKM());
        assertEquals(TrangThaiHieuLucKhuyenMai.DANG_DIEN_RA, first.getItems().get(0).getTrangThaiHieuLuc());
        search.setPage(1);
        KetQuaTraCuuKhuyenMai second = read(send(HanhDong.TRA_CUU_KHUYEN_MAI, salesToken, search),
                KetQuaTraCuuKhuyenMai.class);
        assertEquals("KM02", second.getItems().get(0).getMaKM());

        search.setPage(0);
        search.setPageSize(20);
        KetQuaTraCuuKhuyenMai all = read(send(HanhDong.TRA_CUU_KHUYEN_MAI, managerToken, search),
                KetQuaTraCuuKhuyenMai.class);
        assertEquals(5L, all.getTotal());
        assertEquals(TrangThaiHieuLucKhuyenMai.CHUA_BAT_DAU, find(all, "KM03").getTrangThaiHieuLuc());
        assertEquals(TrangThaiHieuLucKhuyenMai.HET_HAN, find(all, "KM04").getTrangThaiHieuLuc());
        assertFalse(find(all, "KM05").getTrangThai());
        assertEquals(TrangThaiHieuLucKhuyenMai.DANG_DIEN_RA, find(all, "KM05").getTrangThaiHieuLuc());
        search.setTrangThai(false);
        assertEquals(1L,
                read(send(HanhDong.TRA_CUU_KHUYEN_MAI, managerToken, search), KetQuaTraCuuKhuyenMai.class)
                        .getTotal());
        assertError(MaLoi.UNAUTHENTICATED,
                send(HanhDong.TRA_CUU_KHUYEN_MAI, null, new YeuCauTraCuuKhuyenMai()));
    }

    @Test
    public void managerCreateValidatesScaleRangeDatesUnicodeLengthAndDuplicateCode() throws Exception {
        DuLieuKhuyenMai zero = read(
                send(HanhDong.THEM_KHUYEN_MAI, managerToken,
                        promo("KM10", "  Cafe\u0301 Ưu đãi  ", new BigDecimal("0"), today, today)),
                DuLieuKhuyenMai.class);
        assertEquals("Café Ưu đãi", zero.getTenKM());
        assertEquals(0, zero.getPhanTramGiam().compareTo(BigDecimal.ZERO));
        assertTrue(java.text.Normalizer.isNormalized(zero.getTenKM(), java.text.Normalizer.Form.NFC));
        DuLieuKhuyenMai hundred = create("KM11", new BigDecimal("100.00"), today, today.plusDays(2));
        assertEquals(0, hundred.getPhanTramGiam().compareTo(new BigDecimal("100.00")));

        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.THEM_KHUYEN_MAI, managerToken,
                promo("km10", "Trùng mã", BigDecimal.ONE, today, today)));
        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.THEM_KHUYEN_MAI, managerToken,
                promo("KM12", "Âm", new BigDecimal("-0.01"), today, today)));
        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.THEM_KHUYEN_MAI, managerToken,
                promo("KM13", "Quá 100", new BigDecimal("100.01"), today, today)));
        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.THEM_KHUYEN_MAI, managerToken,
                promo("KM14", "Sai scale", new BigDecimal("10.001"), today, today)));
        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.THEM_KHUYEN_MAI, managerToken,
                promo("KM15", "Sai ngày", BigDecimal.TEN, today.plusDays(1), today)));
        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.THEM_KHUYEN_MAI, managerToken,
                promo("KM16", "   ", BigDecimal.TEN, today, today)));
        char[] longName = new char[201];
        java.util.Arrays.fill(longName, 'x');
        assertError(MaLoi.VALIDATION_ERROR, send(HanhDong.THEM_KHUYEN_MAI, managerToken,
                promo("KM17", new String(longName), BigDecimal.TEN, today, today)));
        assertError(MaLoi.FORBIDDEN, send(HanhDong.THEM_KHUYEN_MAI, salesToken,
                promo("KM18", "Không đủ quyền", BigDecimal.TEN, today, today)));
    }

    @Test
    public void updateAndStatusTransitionsRequireFreshFullSnapshot() throws Exception {
        DuLieuKhuyenMai original = create("KM20", BigDecimal.TEN, today, today.plusDays(5));
        DuLieuKhuyenMai changed = copy(original);
        changed.setTenKM("Tên mới");
        changed.setPhanTramGiam(new BigDecimal("12.50"));
        YeuCauLuuKhuyenMai update = save(original, changed);
        DuLieuKhuyenMai saved = read(send(HanhDong.CAP_NHAT_KHUYEN_MAI, managerToken, update),
                DuLieuKhuyenMai.class);
        assertEquals("Tên mới", saved.getTenKM());
        assertEquals(0, saved.getPhanTramGiam().compareTo(new BigDecimal("12.50")));
        assertError(MaLoi.VERSION_CONFLICT, send(HanhDong.CAP_NHAT_KHUYEN_MAI, secondManagerToken, update));
        assertError(MaLoi.FORBIDDEN, send(HanhDong.CAP_NHAT_KHUYEN_MAI, salesToken, save(saved, changed)));

        DuLieuKhuyenMai inactive = read(
                send(HanhDong.NGUNG_HOAT_DONG_KHUYEN_MAI, managerToken, status(saved)),
                DuLieuKhuyenMai.class);
        assertFalse(inactive.getTrangThai());
        assertError(MaLoi.VERSION_CONFLICT,
                send(HanhDong.KICH_HOAT_KHUYEN_MAI, secondManagerToken, status(saved)));
        YeuCauChiTietKhuyenMai detail = new YeuCauChiTietKhuyenMai();
        detail.setMaKM("KM20");
        assertError(MaLoi.NOT_FOUND, send(HanhDong.XEM_CHI_TIET_KHUYEN_MAI, salesToken, detail));
        DuLieuKhuyenMai active = read(
                send(HanhDong.KICH_HOAT_KHUYEN_MAI, secondManagerToken, status(inactive)),
                DuLieuKhuyenMai.class);
        assertTrue(active.getTrangThai());
        assertEquals("KM20",
                read(send(HanhDong.XEM_CHI_TIET_KHUYEN_MAI, salesToken, detail), DuLieuKhuyenMai.class)
                        .getMaKM());
    }

    @Test
    public void simultaneousUpdatesFromOneSnapshotHaveOneWinner() throws Exception {
        DuLieuKhuyenMai original = create("KM30", BigDecimal.TEN, today, today.plusDays(1));
        DuLieuKhuyenMai firstValue = copy(original);
        firstValue.setTenKM("Sửa 1");
        DuLieuKhuyenMai secondValue = copy(original);
        secondValue.setTenKM("Sửa 2");
        YeuCauLuuKhuyenMai firstRequest = save(original, firstValue),
                secondRequest = save(original, secondValue);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (KetNoiMayChu first = new KetNoiMayChu("127.0.0.1", server.getPort());
                KetNoiMayChu second = new KetNoiMayChu("127.0.0.1", server.getPort())) {
            Future<PhanHoi<JsonNode>> a = workers.submit(() -> {
                ready.countDown();
                start.await();
                return first.send(HanhDong.CAP_NHAT_KHUYEN_MAI, managerToken, firstRequest);
            });
            Future<PhanHoi<JsonNode>> b = workers.submit(() -> {
                ready.countDown();
                start.await();
                return second.send(HanhDong.CAP_NHAT_KHUYEN_MAI, secondManagerToken, secondRequest);
            });
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            PhanHoi<JsonNode> ra = a.get(20, TimeUnit.SECONDS), rb = b.get(20, TimeUnit.SECONDS);
            assertEquals(1, (ra.isSuccess() ? 1 : 0) + (rb.isSuccess() ? 1 : 0));
            assertEquals(MaLoi.VERSION_CONFLICT, (ra.isSuccess() ? rb : ra).getErrorCode());
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    public void editingPromotionDoesNotRewritePaidInvoiceDiscountSnapshot() throws Exception {
        DuLieuKhuyenMai promo = create("KM40", new BigDecimal("10.00"), today, today.plusDays(1));
        YeuCauTaoHoaDon createInvoice = new YeuCauTaoHoaDon();
        createInvoice.setMaHD("HD40");
        createInvoice.setMaThue("T1");
        createInvoice.setMaKM("KM40");
        DuLieuDayDuHoaDon invoice = read(send(HanhDong.LAP_HOA_DON, salesToken, createInvoice),
                DuLieuDayDuHoaDon.class);
        YeuCauDongHoaDon line = new YeuCauDongHoaDon();
        line.setMaHD("HD40");
        line.setPhienBan(invoice.getHoaDon().getPhienBan());
        line.setMaBienThe("BT1");
        line.setSoLuong(1);
        invoice = read(send(HanhDong.THEM_CHI_TIET_HOA_DON, salesToken, line), DuLieuDayDuHoaDon.class);
        YeuCauThanhToanHoaDon payment = new YeuCauThanhToanHoaDon();
        payment.setMaHD("HD40");
        payment.setPhienBan(invoice.getHoaDon().getPhienBan());
        payment.setTongTienDaXem(invoice.getHoaDon().getTongTien());
        payment.setPhuongThucThanhToan(PhuongThucThanhToan.TIEN_MAT);
        payment.setTienKhachDua(invoice.getHoaDon().getTongTien());
        invoice = read(send(HanhDong.THANH_TOAN_HOA_DON, salesToken, payment), DuLieuDayDuHoaDon.class);
        assertEquals(TrangThaiHoaDon.DA_THANH_TOAN, invoice.getHoaDon().getTrangThai());
        assertEquals(0, invoice.getHoaDon().getPhanTramGiam().compareTo(new BigDecimal("10.00")));
        assertEquals(0, invoice.getHoaDon().getTienGiam().compareTo(new BigDecimal("10.00")));

        DuLieuKhuyenMai changed = copy(promo);
        changed.setPhanTramGiam(new BigDecimal("25.00"));
        DuLieuKhuyenMai saved = read(send(HanhDong.CAP_NHAT_KHUYEN_MAI, managerToken, save(promo, changed)),
                DuLieuKhuyenMai.class);
        read(send(HanhDong.NGUNG_HOAT_DONG_KHUYEN_MAI, managerToken, status(saved)), DuLieuKhuyenMai.class);
        YeuCauPhienBanHoaDon detail = new YeuCauPhienBanHoaDon();
        detail.setMaHD("HD40");
        DuLieuDayDuHoaDon history = read(send(HanhDong.XEM_CHI_TIET_HOA_DON, salesToken, detail),
                DuLieuDayDuHoaDon.class);
        assertEquals("KM40", history.getHoaDon().getMaKM());
        assertEquals(0, history.getHoaDon().getPhanTramGiam().compareTo(new BigDecimal("10.00")));
        assertEquals(0, history.getHoaDon().getTienGiam().compareTo(new BigDecimal("10.00")));
    }

    private DuLieuKhuyenMai create(String id, BigDecimal rate, LocalDate start, LocalDate end)
            throws Exception {
        return read(send(HanhDong.THEM_KHUYEN_MAI, managerToken,
                promo(id, "Chương trình " + id, rate, start, end)), DuLieuKhuyenMai.class);
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

    private static DuLieuKhuyenMai promo(String id, String name, BigDecimal rate, LocalDate start,
            LocalDate end) {
        DuLieuKhuyenMai dto = new DuLieuKhuyenMai();
        dto.setMaKM(id);
        dto.setTenKM(name);
        dto.setPhanTramGiam(rate);
        dto.setNgayBatDau(start);
        dto.setNgayKetThuc(end);
        return dto;
    }

    private static DuLieuKhuyenMai find(KetQuaTraCuuKhuyenMai result, String id) {
        return result.getItems().stream().filter(row -> row.getMaKM().equals(id)).findFirst()
                .orElseThrow(AssertionError::new);
    }

    private static DuLieuKhuyenMai copy(DuLieuKhuyenMai source) {
        DuLieuKhuyenMai dto = promo(source.getMaKM(), source.getTenKM(), source.getPhanTramGiam(),
                source.getNgayBatDau(), source.getNgayKetThuc());
        dto.setTrangThai(source.getTrangThai());
        dto.setTrangThaiHieuLuc(source.getTrangThaiHieuLuc());
        return dto;
    }

    private static YeuCauLuuKhuyenMai save(DuLieuKhuyenMai expected, DuLieuKhuyenMai value) {
        YeuCauLuuKhuyenMai request = new YeuCauLuuKhuyenMai();
        request.setExpected(expected);
        request.setValue(value);
        return request;
    }

    private static YeuCauTrangThaiKhuyenMai status(DuLieuKhuyenMai expected) {
        YeuCauTrangThaiKhuyenMai request = new YeuCauTrangThaiKhuyenMai();
        request.setExpected(expected);
        return request;
    }
}
