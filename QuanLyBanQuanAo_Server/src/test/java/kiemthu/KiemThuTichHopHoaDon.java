package kiemthu;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import client.api.GoiDichVuHoaDon;
import client.api.NgoaiLeGoiDichVu;
import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.hoadon.DuLieuDayDuHoaDon;
import common.dto.hoadon.DuLieuHoaDon;
import common.dto.hoadon.YeuCauDongHoaDon;
import common.dto.hoadon.YeuCauGanKhachHangHoaDon;
import common.dto.hoadon.YeuCauPhienBanHoaDon;
import common.dto.hoadon.YeuCauTaoHoaDon;
import common.dto.hoadon.YeuCauThanhToanHoaDon;
import common.dto.hoadon.YeuCauTraCuuHoaDon;
import common.dto.taikhoan.KetQuaDangNhap;
import common.enums.NguonBienDongKho;
import common.enums.NguonDuLieuHoaDon;
import common.enums.PhuongThucThanhToan;
import common.enums.TrangThaiHoaDon;
import common.enums.VaiTro;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;
import server.dispatcher.DieuPhoiYeuCau;
import server.entities.BienThe;
import server.entities.ChiTietHoaDon;
import server.entities.HoaDon;
import server.entities.KhachHang;
import server.entities.KhuyenMai;
import server.entities.LichSuKho;
import server.entities.LoaiSanPham;
import server.entities.NhanVien;
import server.entities.SanPham;
import server.entities.Thue;
import server.entities.ThuongHieu;
import server.network.MayChuTcp;
import server.security.BoBamMatKhauPbkdf2;
import server.services.DichVuHoaDon;
import server.services.DichVuSanPham;
import server.services.DichVuXacThuc;
import server.session.QuanLyPhienDangNhap;
import server.utils.QuanLyJpa;

/** Real TCP + JPA/H2. Không kết nối hoặc sửa SQL Server của người dùng. */
public class KiemThuTichHopHoaDon {
    private static QuanLyJpa jpa;
    private static MayChuTcp server;
    private static QuanLyPhienDangNhap sessions;
    private static String hash;
    private KetNoiMayChu socket;
    private GoiDichVuHoaDon api;

    @BeforeClass
    public static void setup() throws Exception {
        Map<String, Object> properties = HoTroKiemThuDanhMuc.memoryProperties("invoice_test");
        jpa = new QuanLyJpa(properties);
        jpa.transaction(em -> {
            em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics()
                    .setStatisticsEnabled(true);
            return null;
        });
        hash = new BoBamMatKhauPbkdf2().hash("InvoicePass123!".toCharArray());
        jpa.transaction(em -> {
            HoTroKiemThuDanhMuc.user(em, "sales", "NV1", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            HoTroKiemThuDanhMuc.user(em, "other", "NV2", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            HoTroKiemThuDanhMuc.user(em, "manager", "NV3", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            KhachHang customer = new KhachHang();
            customer.setMaKH("KH1");
            customer.setHoTen("Khách hàng gốc");
            em.persist(customer);
            LoaiSanPham l = new LoaiSanPham();
            l.setMaLoai("L1");
            l.setTenLoai("Áo");
            em.persist(l);
            ThuongHieu t = new ThuongHieu();
            t.setMaThuongHieu("TH1");
            t.setTenThuongHieu("Demo");
            em.persist(t);
            SanPham p = new SanPham();
            p.setMaSP("SP1");
            p.setTenSP("Áo thử");
            p.setLoaiSanPham(l);
            p.setThuongHieu(t);
            p.setGiaBan(new BigDecimal("100"));
            em.persist(p);
            for (String id : new String[] { "BT1", "BT2" }) {
                BienThe b = new BienThe();
                b.setMaBienThe(id);
                b.setSanPham(p);
                b.setMauSac(id);
                b.setKichCo("M");
                b.setSoLuongTon(5);
                em.persist(b);
            }
            Thue tax = new Thue();
            tax.setMaThue("T1");
            tax.setTenThue("Thuế thử");
            tax.setPhanTram(new BigDecimal("10"));
            em.persist(tax);
            KhuyenMai km = new KhuyenMai();
            km.setMaKM("KM1");
            km.setTenKM("Giảm thử");
            km.setPhanTramGiam(new BigDecimal("10"));
            km.setNgayBatDau(java.time.LocalDate.of(2020, 1, 1));
            km.setNgayKetThuc(java.time.LocalDate.of(2099, 1, 1));
            em.persist(km);
            return null;
        });
        sessions = new QuanLyPhienDangNhap();
        DichVuXacThuc auth = new DichVuXacThuc(jpa, sessions, new BoBamMatKhauPbkdf2());
        Clock fixedUtc = Clock.fixed(Instant.parse("2026-10-11T02:34:56Z"), ZoneOffset.UTC);
        server = new MayChuTcp("127.0.0.1", 0, new DieuPhoiYeuCau(auth, new DichVuSanPham(jpa, auth),
                new DichVuHoaDon(jpa, auth, fixedUtc)));
        server.start();
    }

    @Before
    public void reset() {
        jpa.transaction(em -> {
            em.createQuery("delete from LichSuKho").executeUpdate();
            em.createQuery("delete from ChiTietHoaDon").executeUpdate();
            em.createQuery("delete from HoaDon").executeUpdate();
            em.find(BienThe.class, "BT1").setSoLuongTon(5);
            em.find(BienThe.class, "BT2").setSoLuongTon(5);
            em.find(BienThe.class, "BT1").setMauSac("BT1");
            em.find(BienThe.class, "BT1").setKichCo("M");
            em.find(SanPham.class, "SP1").setTenSP("Áo thử");
            em.find(SanPham.class, "SP1").setGiaBan(new BigDecimal("100"));
            em.find(NhanVien.class, "NV1").setHoTen("Nhân viên thử sales");
            em.find(KhachHang.class, "KH1").setHoTen("Khách hàng gốc");
            em.find(KhachHang.class, "KH1").setTrangThai(true);
            em.find(Thue.class, "T1").setPhanTram(new BigDecimal("10"));
            return null;
        });
        socket = new KetNoiMayChu("127.0.0.1", server.getPort());
        api = api(socket, "sales");
    }

    private static GoiDichVuHoaDon api(KetNoiMayChu s, String user) {
        PhienDangNhap session = new PhienDangNhap();
        KetQuaDangNhap login = new KetQuaDangNhap();
        login.setSessionToken(sessions.create(user, hash));
        session.setLogin(login);
        return new GoiDichVuHoaDon(s, session);
    }

    @After
    public void close() {
        socket.close();
    }

    @AfterClass
    public static void finish() {
        if (server != null) {
            server.close();
        }
        if (jpa != null) {
            jpa.close();
        }
    }

    private DuLieuDayDuHoaDon create(String id) throws Exception {
        return create(id, null);
    }

    @Test
    public void autoPromotionUsesBestEffectiveRateAndKeepsItOnRetry() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 11);
        jpa.transaction(em -> {
            em.persist(promotion("KM_BEST", "25", today, today, true));
            em.persist(promotion("KM_TIE", "25", today, today, true));
            em.persist(promotion("KM_EXPIRED", "90", today.minusDays(2), today.minusDays(1), true));
            em.persist(promotion("KM_DISABLED", "80", today, today.plusDays(1), false));
            em.persist(promotion("KM_FUTURE", "95", today.plusDays(1), today.plusDays(2), true));
            return null;
        });
        YeuCauTaoHoaDon request = new YeuCauTaoHoaDon();
        request.setMaHD("HD_AUTO");
        request.setMaThue("T1");
        request.setTuDongChonKhuyenMai(true);
        DuLieuDayDuHoaDon draft = api.create(request);
        assertEquals("KM_BEST", draft.getHoaDon().getMaKM());

        jpa.transaction(em -> {
            em.persist(promotion("KM_NEW_BEST", "35", today, today.plusDays(1), true));
            return null;
        });
        assertEquals("KM_BEST", api.create(request).getHoaDon().getMaKM());
        DuLieuDayDuHoaDon withItem = line(draft, "BT1", 1, 0);
        assertEquals(0, new BigDecimal("25").compareTo(withItem.getHoaDon().getTienGiam()));
        assertEquals(0, new BigDecimal("82.50").compareTo(withItem.getHoaDon().getTongTien()));

        request.setMaHD("HD_MANUAL");
        request.setMaKM("KM1");
        assertEquals("KM1", api.create(request).getHoaDon().getMaKM());
    }

    private static KhuyenMai promotion(String code, String rate, LocalDate start, LocalDate end, boolean active) {
        KhuyenMai promotion = new KhuyenMai();
        promotion.setMaKM(code);
        promotion.setTenKM(code);
        promotion.setPhanTramGiam(new BigDecimal(rate));
        promotion.setNgayBatDau(start);
        promotion.setNgayKetThuc(end);
        promotion.setTrangThai(active);
        return promotion;
    }

    private DuLieuDayDuHoaDon create(String id, String customerId) throws Exception {
        YeuCauTaoHoaDon r = new YeuCauTaoHoaDon();
        r.setMaHD(id);
        r.setMaThue("T1");
        r.setMaKM("KM1");
        r.setMaKH(customerId);
        return api.create(r);
    }

    private static YeuCauPhienBanHoaDon version(DuLieuDayDuHoaDon d) {
        YeuCauPhienBanHoaDon r = new YeuCauPhienBanHoaDon();
        r.setMaHD(d.getHoaDon().getMaHD());
        r.setPhienBan(d.getHoaDon().getPhienBan());
        return r;
    }

    private static YeuCauGanKhachHangHoaDon customerRequest(DuLieuDayDuHoaDon d, String customerId) {
        YeuCauGanKhachHangHoaDon r = new YeuCauGanKhachHangHoaDon();
        r.setMaHD(d.getHoaDon().getMaHD());
        r.setPhienBan(d.getHoaDon().getPhienBan());
        r.setMaKH(customerId);
        return r;
    }

    private static YeuCauThanhToanHoaDon cashPayment(DuLieuDayDuHoaDon d, BigDecimal tendered) {
        YeuCauThanhToanHoaDon r = new YeuCauThanhToanHoaDon();
        r.setMaHD(d.getHoaDon().getMaHD());
        r.setPhienBan(d.getHoaDon().getPhienBan());
        r.setTongTienDaXem(d.getHoaDon().getTongTien());
        r.setPhuongThucThanhToan(PhuongThucThanhToan.TIEN_MAT);
        r.setTienKhachDua(tendered);
        return r;
    }

    private static YeuCauThanhToanHoaDon exactCashPayment(DuLieuDayDuHoaDon d) {
        return cashPayment(d, d.getHoaDon().getTongTien());
    }

    private DuLieuDayDuHoaDon line(DuLieuDayDuHoaDon d, String id, int qty, int mode) throws Exception {
        YeuCauDongHoaDon r = new YeuCauDongHoaDon();
        r.setMaHD(d.getHoaDon().getMaHD());
        r.setPhienBan(d.getHoaDon().getPhienBan());
        r.setMaBienThe(id);
        r.setSoLuong(qty);
        return api.line(r, mode);
    }

    private int stock(String id) {
        return jpa.transaction(em -> em.find(BienThe.class, id).getSoLuongTon());
    }

    @Test
    public void detailQueryCountRemainsBoundedForManyInvoiceLines() throws Exception {
        final int manyLines = 50;
        jpa.transaction(em -> {
            SanPham product = em.find(SanPham.class, "SP1");
            for (int i = 0; i < manyLines; i++) {
                BienThe variant = new BienThe();
                variant.setMaBienThe("PERF" + i);
                variant.setSanPham(product);
                variant.setMauSac("Perf-" + i);
                variant.setKichCo("M");
                variant.setSoLuongTon(5);
                em.persist(variant);
            }
            return null;
        });
        create("HD_PERF_1");
        create("HD_PERF_50");
        jpa.transaction(em -> {
            for (int i = 0; i < manyLines; i++) {
                ChiTietHoaDon line = new ChiTietHoaDon();
                line.setHoaDon(em.getReference(HoaDon.class, "HD_PERF_50"));
                line.setBienThe(em.getReference(BienThe.class, "PERF" + i));
                line.setSoLuong(1);
                line.setDonGia(new BigDecimal("100"));
                em.persist(line);
            }
            ChiTietHoaDon single = new ChiTietHoaDon();
            single.setHoaDon(em.getReference(HoaDon.class, "HD_PERF_1"));
            single.setBienThe(em.getReference(BienThe.class, "PERF0"));
            single.setSoLuong(1);
            single.setDonGia(new BigDecimal("100"));
            em.persist(single);
            return null;
        });

        Statistics statistics = jpa
                .transaction(em -> em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics());
        api.detail("HD_PERF_1");
        api.detail("HD_PERF_50"); // warm the same request path before timing
        long[] one = measureDetails(statistics, "HD_PERF_1", 5);
        long[] many = measureDetails(statistics, "HD_PERF_50", 5);
        System.out.println("[S24 H2 detail] lines=1 statements/request=" + one[0] + " average-ms/request="
                + one[1] + "; lines=" + manyLines + " statements/request=" + many[0] + " average-ms/request="
                + many[1] + "; H2 only, not a SQL Server performance claim.");
        assertEquals("detail statements must not grow with the number of lines", one[0], many[0]);
    }

    @Test
    public void invoiceSearchPageQueryCountRemainsBoundedForFiftyCustomers() throws Exception {
        final int manyInvoices = 50;
        jpa.transaction(em -> {
            NhanVien employee = em.find(NhanVien.class, "NV1");
            Thue tax = em.find(Thue.class, "T1");
            for (int i = 0; i < manyInvoices; i++) {
                String suffix = String.format(java.util.Locale.ROOT, "%03d", i);
                KhachHang customer = new KhachHang();
                customer.setMaKH("KH-PAGE-" + suffix);
                customer.setHoTen("Khách phân trang " + suffix);
                em.persist(customer);
                HoaDon invoice = new HoaDon();
                invoice.setMaHD("HD-PAGE-" + suffix);
                invoice.setNhanVien(employee);
                invoice.setKhachHang(customer);
                invoice.setThue(tax);
                invoice.setNgayLap(LocalDateTime.of(2026, 4, 1, 10, 0).plusSeconds(i));
                invoice.setTamTinh(BigDecimal.ZERO);
                invoice.setPhanTramGiam(BigDecimal.ZERO);
                invoice.setTienGiam(BigDecimal.ZERO);
                invoice.setPhanTramThue(BigDecimal.ZERO);
                invoice.setTienThue(BigDecimal.ZERO);
                invoice.setTongTien(BigDecimal.ZERO);
                invoice.setTrangThai(TrangThaiHoaDon.NHAP);
                em.persist(invoice);
            }
            return null;
        });
        Statistics statistics = jpa
                .transaction(em -> em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics());
        YeuCauTraCuuHoaDon onePage = new YeuCauTraCuuHoaDon();
        onePage.setPageSize(1);
        YeuCauTraCuuHoaDon fiftyPage = new YeuCauTraCuuHoaDon();
        fiftyPage.setPageSize(manyInvoices);
        api.search(onePage);
        api.search(fiftyPage);
        long[] one = measureInvoicePage(statistics, onePage, 5);
        long[] fifty = measureInvoicePage(statistics, fiftyPage, 5);
        System.out.println("[S24 H2 invoice search] page=1 statements/request=" + one[0]
                + ", average-ms/request=" + one[1] + "; page=50 statements/request=" + fifty[0]
                + ", average-ms/request=" + fifty[1] + "; H2 only.");
        assertEquals("invoice page statements must not grow with the number of customers", one[0], fifty[0]);
    }

    private long[] measureInvoicePage(Statistics statistics, YeuCauTraCuuHoaDon request, int repetitions)
            throws Exception {
        statistics.clear();
        long start = System.nanoTime();
        for (int i = 0; i < repetitions; i++) {
            api.search(request);
        }
        long averageMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) / repetitions;
        return new long[] { statistics.getPrepareStatementCount() / repetitions, averageMillis };
    }

    private long[] measureDetails(Statistics statistics, String invoiceId, int repetitions) throws Exception {
        statistics.clear();
        long start = System.nanoTime();
        for (int i = 0; i < repetitions; i++) {
            api.detail(invoiceId);
        }
        long elapsedMillisPerRequest = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) / repetitions;
        return new long[] { statistics.getPrepareStatementCount() / repetitions, elapsedMillisPerRequest };
    }

    private long movementCount(String operationId) {
        return jpa.transaction(
                em -> em.createQuery("select count(l) from LichSuKho l where l.operationId = :id", Long.class)
                        .setParameter("id", operationId).getSingleResult());
    }

    private interface Work {
        void run() throws Exception;
    }

    private static void error(MaLoi code, Work work) throws Exception {
        try {
            work.run();
            fail("Expected " + code);
        } catch (NgoaiLeGoiDichVu e) {
            assertEquals(code, e.getCode());
        }
    }

    @Test
    public void draftTotalsAndIdempotentPayment() throws Exception {
        DuLieuDayDuHoaDon d = create("HD1");
        assertEquals("HD1", create("HD1").getHoaDon().getMaHD());
        Long old = d.getHoaDon().getPhienBan();
        d = line(d, "BT1", 2, 0);
        assertTrue(d.getHoaDon().getPhienBan() > old);
        assertEquals(5, stock("BT1"));
        assertEquals(0, new BigDecimal("198").compareTo(d.getHoaDon().getTongTien()));
        YeuCauThanhToanHoaDon payment = cashPayment(d, new BigDecimal("200"));
        assertEquals(TrangThaiHoaDon.DA_THANH_TOAN, api.pay(payment).getHoaDon().getTrangThai());
        YeuCauThanhToanHoaDon changedRetry = new YeuCauThanhToanHoaDon();
        changedRetry.setMaHD(payment.getMaHD());
        changedRetry.setPhienBan(payment.getPhienBan());
        changedRetry.setPhuongThucThanhToan(PhuongThucThanhToan.CHUYEN_KHOAN);
        changedRetry.setNhanVienXacNhanChuyenKhoan(Boolean.TRUE);
        assertEquals(TrangThaiHoaDon.DA_THANH_TOAN, api.pay(changedRetry).getHoaDon().getTrangThai());
        DuLieuHoaDon savedPayment = api.detail("HD1").getHoaDon();
        assertEquals(PhuongThucThanhToan.TIEN_MAT, savedPayment.getPhuongThucThanhToan());
        assertEquals(0, new BigDecimal("200").compareTo(savedPayment.getTienKhachDua()));
        assertEquals(0, new BigDecimal("2").compareTo(savedPayment.getTienThua()));
        YeuCauPhienBanHoaDon cancelRequest = version(d);
        assertEquals(3, stock("BT1"));
        error(MaLoi.VALIDATION_ERROR, () -> api.cancel(cancelRequest));
        assertEquals(1, movementCount("SALE:HD1:BT1"));
        LichSuKho movement = jpa.transaction(
                em -> em.createQuery("select l from LichSuKho l where l.operationId = :id", LichSuKho.class)
                        .setParameter("id", "SALE:HD1:BT1").getSingleResult());
        assertEquals(NguonBienDongKho.BAN_HANG, movement.getNguon());
        assertEquals(5, movement.getSoLuongTruoc());
        assertEquals(-2, movement.getSoLuongThayDoi());
        assertEquals(3, movement.getSoLuongSau());
    }

    @Test
    public void fractionalInvoiceQuantityIsRejectedAtSocketBoundary() throws Exception {
        DuLieuDayDuHoaDon draft = create("HD_FRACTIONAL_QTY");
        Map<String, Object> payload = new java.util.HashMap<String, Object>();
        payload.put("maHD", "HD_FRACTIONAL_QTY");
        payload.put("phienBan", draft.getHoaDon().getPhienBan());
        payload.put("maBienThe", "BT1");
        payload.put("soLuong", new BigDecimal("1.9"));
        PhanHoi<com.fasterxml.jackson.databind.JsonNode> response = socket
                .send(HanhDong.THEM_CHI_TIET_HOA_DON, sessions.create("sales", hash), payload);
        assertFalse(response.isSuccess());
        assertEquals(MaLoi.INVALID_REQUEST, response.getErrorCode());
        assertTrue(api.detail("HD_FRACTIONAL_QTY").getChiTiet().isEmpty());
        assertEquals(5, stock("BT1"));
    }

    @Test
    public void draftCustomerCanBeChangedOnlyByOwnerWithCurrentVersion() throws Exception {
        DuLieuDayDuHoaDon draft = create("HD_CUSTOMER", "KH1");
        assertEquals("KH1", draft.getHoaDon().getMaKH());
        assertEquals("Khách hàng gốc", draft.getHoaDon().getTenKhachHang());

        YeuCauGanKhachHangHoaDon stale = customerRequest(draft, null);
        DuLieuDayDuHoaDon updated = api.setCustomer(customerRequest(draft, null));
        assertNull(updated.getHoaDon().getMaKH());
        assertTrue(updated.getHoaDon().getPhienBan() > draft.getHoaDon().getPhienBan());
        error(MaLoi.VERSION_CONFLICT, () -> api.setCustomer(stale));

        KetNoiMayChu otherSocket = new KetNoiMayChu("127.0.0.1", server.getPort());
        try {
            GoiDichVuHoaDon other = api(otherSocket, "other");
            error(MaLoi.FORBIDDEN, () -> other.setCustomer(customerRequest(updated, "KH1")));
        } finally {
            otherSocket.close();
        }
    }

    @Test
    public void inactiveCustomerCannotBeAttachedOrPaidAndPaidCustomerIsReadOnly() throws Exception {
        DuLieuDayDuHoaDon attached = create("HD_INACTIVE_CUSTOMER", "KH1");
        jpa.transaction(em -> {
            em.find(KhachHang.class, "KH1").setTrangThai(false);
            return null;
        });
        error(MaLoi.VALIDATION_ERROR, () -> api.setCustomer(customerRequest(attached, "KH1")));
        DuLieuDayDuHoaDon withLine = line(attached, "BT1", 1, 0);
        error(MaLoi.VALIDATION_ERROR, () -> api.pay(exactCashPayment(withLine)));
        assertEquals(TrangThaiHoaDon.NHAP, api.detail("HD_INACTIVE_CUSTOMER").getHoaDon().getTrangThai());
        assertEquals(5, stock("BT1"));

        jpa.transaction(em -> {
            em.find(KhachHang.class, "KH1").setTrangThai(true);
            return null;
        });
        DuLieuDayDuHoaDon payable = api.detail("HD_INACTIVE_CUSTOMER");
        DuLieuDayDuHoaDon paid = api.pay(exactCashPayment(payable));
        error(MaLoi.VALIDATION_ERROR, () -> api.setCustomer(customerRequest(paid, null)));
        assertEquals("KH1", api.detail("HD_INACTIVE_CUSTOMER").getHoaDon().getMaKH());
    }

    @Test
    public void cashRequiresExactMoneyAndServerCalculatesChange() throws Exception {
        DuLieuDayDuHoaDon draft = line(create("HD1"), "BT1", 1, 0);
        error(MaLoi.VALIDATION_ERROR, () -> api.pay(cashPayment(draft, null)));
        error(MaLoi.VALIDATION_ERROR, () -> api.pay(cashPayment(draft, new BigDecimal("98.99"))));
        error(MaLoi.VALIDATION_ERROR, () -> api.pay(cashPayment(draft, new BigDecimal("99.001"))));
        error(MaLoi.VALIDATION_ERROR,
                () -> api.pay(cashPayment(draft, new BigDecimal("10000000000000000.00"))));
        assertEquals(TrangThaiHoaDon.NHAP, api.detail("HD1").getHoaDon().getTrangThai());
        assertEquals(5, stock("BT1"));
        assertEquals(0, movementCount("SALE:HD1:BT1"));

        DuLieuDayDuHoaDon paid = api.pay(cashPayment(draft, new BigDecimal("100.05")));
        assertEquals(0, new BigDecimal("100.05").compareTo(paid.getHoaDon().getTienKhachDua()));
        assertEquals(0, new BigDecimal("1.05").compareTo(paid.getHoaDon().getTienThua()));
        assertEquals(4, stock("BT1"));
        assertEquals(1, movementCount("SALE:HD1:BT1"));
    }

    @Test
    public void manualTransferRequiresEmployeeConfirmationAndStoresNoCashAmount() throws Exception {
        DuLieuDayDuHoaDon draft = line(create("HD1"), "BT1", 1, 0);
        YeuCauThanhToanHoaDon request = new YeuCauThanhToanHoaDon();
        request.setMaHD("HD1");
        request.setPhienBan(draft.getHoaDon().getPhienBan());
        request.setTongTienDaXem(draft.getHoaDon().getTongTien());
        request.setPhuongThucThanhToan(PhuongThucThanhToan.CHUYEN_KHOAN);
        error(MaLoi.VALIDATION_ERROR, () -> api.pay(request));
        assertEquals(5, stock("BT1"));

        request.setNhanVienXacNhanChuyenKhoan(Boolean.TRUE);
        DuLieuHoaDon paid = api.pay(request).getHoaDon();
        assertEquals(PhuongThucThanhToan.CHUYEN_KHOAN, paid.getPhuongThucThanhToan());
        assertEquals(Boolean.TRUE, paid.getNhanVienXacNhanChuyenKhoan());
        assertNull(paid.getTienKhachDua());
        assertNull(paid.getTienThua());
        assertEquals(4, stock("BT1"));
    }

    @Test
    public void changedTotalRequiresNewReviewAndCashTenderAgainstNewTotal() throws Exception {
        DuLieuDayDuHoaDon firstView = line(create("HD1"), "BT1", 1, 0);
        DuLieuDayDuHoaDon changed = line(firstView, "BT1", 2, 1);
        YeuCauThanhToanHoaDon staleReview = cashPayment(changed, new BigDecimal("100"));
        staleReview.setTongTienDaXem(firstView.getHoaDon().getTongTien());
        error(MaLoi.VERSION_CONFLICT, () -> api.pay(staleReview));
        assertEquals(TrangThaiHoaDon.NHAP, api.detail("HD1").getHoaDon().getTrangThai());
        assertEquals(5, stock("BT1"));

        error(MaLoi.VALIDATION_ERROR, () -> api.pay(cashPayment(changed, new BigDecimal("100"))));
        assertEquals(5, stock("BT1"));
        assertEquals(0, movementCount("SALE:HD1:BT1"));
        DuLieuHoaDon paid = api.pay(cashPayment(changed, new BigDecimal("200"))).getHoaDon();
        assertEquals(0, new BigDecimal("198.00").compareTo(paid.getTongTien()));
        assertEquals(0, new BigDecimal("2.00").compareTo(paid.getTienThua()));
        assertEquals(3, stock("BT1"));
        assertEquals(1, movementCount("SALE:HD1:BT1"));
    }

    @Test
    public void paymentSnapshotsCurrentNamesAndVietnameseTimestampOnlyOnce() throws Exception {
        DuLieuDayDuHoaDon draft = create("HD1", "KH1");
        assertNull(draft.getHoaDon().getNgayThanhToan());
        assertEquals(NguonDuLieuHoaDon.DU_LIEU_HIEN_TAI, draft.getHoaDon().getNguonDuLieuHienThi());
        assertEquals("Nhân viên thử sales", draft.getHoaDon().getTenNhanVien());
        assertEquals("Khách hàng gốc", draft.getHoaDon().getTenKhachHang());
        draft = line(draft, "BT1", 1, 0);
        assertEquals(NguonDuLieuHoaDon.DU_LIEU_HIEN_TAI, draft.getChiTiet().get(0).getNguonDuLieuHienThi());

        jpa.transaction(em -> {
            em.find(SanPham.class, "SP1").setTenSP("Áo tại thời điểm thanh toán");
            em.find(BienThe.class, "BT1").setMauSac("Đỏ thanh toán");
            em.find(BienThe.class, "BT1").setKichCo("L");
            em.find(NhanVien.class, "NV1").setHoTen("Nhân viên tại thanh toán");
            em.find(KhachHang.class, "KH1").setHoTen("Khách tại thanh toán");
            return null;
        });
        DuLieuDayDuHoaDon refreshedDraft = api.detail("HD1");
        assertNull(refreshedDraft.getHoaDon().getNgayThanhToan());
        assertEquals("Nhân viên tại thanh toán", refreshedDraft.getHoaDon().getTenNhanVien());
        assertEquals("Áo tại thời điểm thanh toán", refreshedDraft.getChiTiet().get(0).getTenSP());
        assertEquals(NguonDuLieuHoaDon.DU_LIEU_HIEN_TAI,
                refreshedDraft.getChiTiet().get(0).getNguonDuLieuHienThi());

        YeuCauThanhToanHoaDon payment = exactCashPayment(refreshedDraft);
        DuLieuDayDuHoaDon paid = api.pay(payment);
        LocalDateTime expectedPaymentTime = LocalDateTime.of(2026, 10, 11, 9, 34, 56);
        assertEquals(expectedPaymentTime, paid.getHoaDon().getNgayThanhToan());
        assertEquals(NguonDuLieuHoaDon.SNAPSHOT_THANH_TOAN, paid.getHoaDon().getNguonDuLieuHienThi());
        assertEquals(NguonDuLieuHoaDon.SNAPSHOT_THANH_TOAN, paid.getChiTiet().get(0).getNguonDuLieuHienThi());
        assertEquals("Nhân viên tại thanh toán", paid.getHoaDon().getTenNhanVien());
        assertEquals("Khách tại thanh toán", paid.getHoaDon().getTenKhachHang());
        assertEquals("Áo tại thời điểm thanh toán", paid.getChiTiet().get(0).getTenSP());
        assertEquals("Đỏ thanh toán", paid.getChiTiet().get(0).getMauSac());
        assertEquals("L", paid.getChiTiet().get(0).getKichCo());
        assertEquals(0, new BigDecimal("99.00").compareTo(paid.getHoaDon().getTongTien()));
        assertEquals(PhuongThucThanhToan.TIEN_MAT, paid.getHoaDon().getPhuongThucThanhToan());
        assertEquals(0, new BigDecimal("99.00").compareTo(paid.getHoaDon().getTienKhachDua()));
        assertEquals(0, BigDecimal.ZERO.compareTo(paid.getHoaDon().getTienThua()));

        jpa.transaction(em -> {
            em.find(SanPham.class, "SP1").setTenSP("Tên sản phẩm hiện hành mới");
            em.find(BienThe.class, "BT1").setMauSac("Xanh hiện hành");
            em.find(BienThe.class, "BT1").setKichCo("XL");
            em.find(NhanVien.class, "NV1").setHoTen("Tên nhân viên mới");
            em.find(KhachHang.class, "KH1").setHoTen("Tên khách mới");
            return null;
        });
        DuLieuDayDuHoaDon afterCatalogEdit = api.detail("HD1");
        DuLieuDayDuHoaDon retried = api.pay(payment);
        for (DuLieuDayDuHoaDon result : new DuLieuDayDuHoaDon[] { afterCatalogEdit, retried }) {
            assertEquals(expectedPaymentTime, result.getHoaDon().getNgayThanhToan());
            assertEquals("Nhân viên tại thanh toán", result.getHoaDon().getTenNhanVien());
            assertEquals("Khách tại thanh toán", result.getHoaDon().getTenKhachHang());
            assertEquals("Áo tại thời điểm thanh toán", result.getChiTiet().get(0).getTenSP());
            assertEquals("Đỏ thanh toán", result.getChiTiet().get(0).getMauSac());
            assertEquals("L", result.getChiTiet().get(0).getKichCo());
            assertEquals(NguonDuLieuHoaDon.SNAPSHOT_THANH_TOAN, result.getHoaDon().getNguonDuLieuHienThi());
        }
        assertEquals(4, stock("BT1"));
    }

    @Test
    public void legacyPaidInvoiceReportsCurrentDataFallbackWithoutInventedTime() throws Exception {
        jpa.transaction(em -> {
            HoaDon invoice = new HoaDon();
            invoice.setMaHD("OLD1");
            invoice.setNhanVien(em.find(NhanVien.class, "NV1"));
            invoice.setKhachHang(em.find(KhachHang.class, "KH1"));
            invoice.setThue(em.find(Thue.class, "T1"));
            invoice.setNgayLap(LocalDateTime.of(2020, 4, 5, 12, 0));
            invoice.setTamTinh(new BigDecimal("100"));
            invoice.setPhanTramGiam(BigDecimal.ZERO);
            invoice.setTienGiam(BigDecimal.ZERO);
            invoice.setPhanTramThue(new BigDecimal("10"));
            invoice.setTienThue(new BigDecimal("10"));
            invoice.setTongTien(new BigDecimal("110"));
            invoice.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN);
            em.persist(invoice);
            ChiTietHoaDon line = new ChiTietHoaDon();
            line.setHoaDon(invoice);
            line.setBienThe(em.find(BienThe.class, "BT1"));
            line.setSoLuong(1);
            line.setDonGia(new BigDecimal("100"));
            em.persist(line);
            return null;
        });
        jpa.transaction(em -> {
            em.find(NhanVien.class, "NV1").setHoTen("Nhân viên hiện hành sau lịch sử");
            em.find(KhachHang.class, "KH1").setHoTen("Khách hiện hành sau lịch sử");
            em.find(SanPham.class, "SP1").setTenSP("Áo hiện hành sau lịch sử");
            em.find(BienThe.class, "BT1").setMauSac("Màu hiện hành");
            em.find(BienThe.class, "BT1").setKichCo("S");
            return null;
        });

        DuLieuDayDuHoaDon legacy = api.detail("OLD1");
        assertNull(legacy.getHoaDon().getNgayThanhToan());
        assertEquals(NguonDuLieuHoaDon.FALLBACK_DU_LIEU_HIEN_TAI, legacy.getHoaDon().getNguonDuLieuHienThi());
        assertEquals("Nhân viên hiện hành sau lịch sử", legacy.getHoaDon().getTenNhanVien());
        assertEquals("Khách hiện hành sau lịch sử", legacy.getHoaDon().getTenKhachHang());
        assertEquals(NguonDuLieuHoaDon.FALLBACK_DU_LIEU_HIEN_TAI,
                legacy.getChiTiet().get(0).getNguonDuLieuHienThi());
        assertEquals("Áo hiện hành sau lịch sử", legacy.getChiTiet().get(0).getTenSP());
        assertEquals("Màu hiện hành", legacy.getChiTiet().get(0).getMauSac());
        assertEquals("S", legacy.getChiTiet().get(0).getKichCo());
        assertNull(legacy.getHoaDon().getPhuongThucThanhToan());
        assertNull(legacy.getHoaDon().getTienKhachDua());
        assertNull(legacy.getHoaDon().getTienThua());
    }

    @Test
    public void absoluteQuantityRemovalAndStaleVersion() throws Exception {
        DuLieuDayDuHoaDon initial = create("HD1"), first = line(initial, "BT1", 1, 0);
        error(MaLoi.VERSION_CONFLICT, () -> line(initial, "BT2", 1, 0));
        DuLieuDayDuHoaDon changed = line(first, "BT1", 3, 1);
        assertEquals(3, changed.getChiTiet().get(0).getSoLuong());
        DuLieuDayDuHoaDon empty = line(changed, "BT1", 0, 2);
        assertTrue(empty.getChiTiet().isEmpty());
        error(MaLoi.VALIDATION_ERROR, () -> api.pay(exactCashPayment(empty)));
        assertEquals(5, stock("BT1"));
    }

    @Test
    public void authorizationAndSearch() throws Exception {
        DuLieuDayDuHoaDon owned = line(create("HD1"), "BT1", 1, 0);
        try (KetNoiMayChu other = new KetNoiMayChu("127.0.0.1", server.getPort())) {
            GoiDichVuHoaDon foreign = api(other, "other");
            error(MaLoi.FORBIDDEN, () -> foreign.detail("HD1"));
            assertEquals(0, foreign.search(new YeuCauTraCuuHoaDon()).getTotal());
            error(MaLoi.FORBIDDEN, () -> foreign.pay(exactCashPayment(owned)));
            assertEquals(1, api(other, "manager").search(new YeuCauTraCuuHoaDon()).getTotal());
        }
        assertEquals(1, api.search(new YeuCauTraCuuHoaDon()).getTotal());
        assertEquals(1, api.taxes().size());
    }

    @Test
    public void stockFailureRollsBackEntireInvoice() throws Exception {
        DuLieuDayDuHoaDon d = line(line(create("HD1"), "BT1", 2, 0), "BT2", 2, 0);
        jpa.transaction(em -> {
            em.find(BienThe.class, "BT2").setSoLuongTon(0);
            return null;
        });
        error(MaLoi.OUT_OF_STOCK, () -> api.pay(cashPayment(d, new BigDecimal("400"))));
        assertEquals(5, stock("BT1"));
        DuLieuDayDuHoaDon failed = api.detail("HD1");
        assertEquals(TrangThaiHoaDon.NHAP, failed.getHoaDon().getTrangThai());
        assertNull(failed.getHoaDon().getNgayThanhToan());
        assertNull(failed.getHoaDon().getPhuongThucThanhToan());
        assertNull(failed.getHoaDon().getTienKhachDua());
        assertNull(failed.getHoaDon().getTienThua());
        assertEquals(0,
                jpa.transaction(em -> em
                        .createQuery("select count(l) from LichSuKho l where l.thamChieu = :id", Long.class)
                        .setParameter("id", "HD1").getSingleResult()).longValue());
        assertEquals(NguonDuLieuHoaDon.DU_LIEU_HIEN_TAI, failed.getChiTiet().get(0).getNguonDuLieuHienThi());
    }

    @Test
    public void snapshotPriceAndChangedTax() throws Exception {
        DuLieuDayDuHoaDon d = line(create("HD1"), "BT1", 1, 0);
        jpa.transaction(em -> {
            em.find(SanPham.class, "SP1").setGiaBan(new BigDecimal("200"));
            return null;
        });
        assertEquals(0,
                new BigDecimal("99").compareTo(api.pay(exactCashPayment(d)).getHoaDon().getTongTien()));
        DuLieuDayDuHoaDon next = line(create("HD2"), "BT1", 1, 0);
        jpa.transaction(em -> {
            em.find(Thue.class, "T1").setPhanTram(new BigDecimal("20"));
            return null;
        });
        error(MaLoi.VERSION_CONFLICT, () -> api.pay(exactCashPayment(next)));
        assertEquals(4, stock("BT1"));
    }

    @Test
    public void cancelledDraftDoesNotChangeStock() throws Exception {
        DuLieuDayDuHoaDon d = line(create("HD1"), "BT1", 1, 0);
        assertEquals(TrangThaiHoaDon.DA_HUY, api.cancel(version(d)).getHoaDon().getTrangThai());
        assertEquals(5, stock("BT1"));
        error(MaLoi.VALIDATION_ERROR, () -> api.pay(exactCashPayment(d)));
    }

    @Test
    public void invalidQuantityAndDuplicateLine() throws Exception {
        DuLieuDayDuHoaDon initial = create("HD1");
        error(MaLoi.VALIDATION_ERROR, () -> line(initial, "BT1", 0, 0));
        error(MaLoi.OUT_OF_STOCK, () -> line(initial, "BT1", 6, 0));
        DuLieuDayDuHoaDon d = line(initial, "BT1", 1, 0);
        error(MaLoi.VALIDATION_ERROR, () -> line(d, "BT1", 1, 0));
    }

    @Test
    public void simultaneousClientsCannotOversell() throws Exception {
        jpa.transaction(em -> {
            em.find(BienThe.class, "BT1").setSoLuongTon(1);
            return null;
        });
        DuLieuDayDuHoaDon a = line(create("HD1"), "BT1", 1, 0), b = line(create("HD2"), "BT1", 1, 0);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> f1 = pool.submit(() -> attempt(a, start));
            Future<Boolean> f2 = pool.submit(() -> attempt(b, start));
            start.countDown();
            assertEquals(1, (f1.get(15, TimeUnit.SECONDS) ? 1 : 0) + (f2.get(15, TimeUnit.SECONDS) ? 1 : 0));
            assertEquals(0, stock("BT1"));
            assertEquals(1, jpa.transaction(em -> em
                    .createQuery("select count(l) from LichSuKho l where l.thamChieu in (:a, :b)", Long.class)
                    .setParameter("a", "HD1").setParameter("b", "HD2").getSingleResult()).longValue());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void overflowIsRejectedAndLineInsertRollsBack() throws Exception {
        DuLieuDayDuHoaDon d = create("HD1");
        jpa.transaction(em -> {
            em.find(SanPham.class, "SP1").setGiaBan(new BigDecimal("9999999999999999.99"));
            return null;
        });
        error(MaLoi.VALIDATION_ERROR, () -> line(d, "BT1", 2, 0));
        assertTrue(api.detail("HD1").getChiTiet().isEmpty());
        assertEquals(5, stock("BT1"));
    }

    @Test
    public void inactiveVariantAndExpiredPromotionAreRejected() throws Exception {
        DuLieuDayDuHoaDon d = create("HD1");
        jpa.transaction(em -> {
            em.find(BienThe.class, "BT1").setTrangThai(false);
            return null;
        });
        try {
            error(MaLoi.VALIDATION_ERROR, () -> line(d, "BT1", 1, 0));
        } finally {
            jpa.transaction(em -> {
                em.find(BienThe.class, "BT1").setTrangThai(true);
                return null;
            });
        }
        jpa.transaction(em -> {
            em.find(KhuyenMai.class, "KM1").setNgayKetThuc(java.time.LocalDate.of(2021, 1, 1));
            return null;
        });
        try {
            error(MaLoi.VALIDATION_ERROR, () -> create("HD2"));
        } finally {
            jpa.transaction(em -> {
                em.find(KhuyenMai.class, "KM1").setNgayKetThuc(java.time.LocalDate.of(2099, 1, 1));
                return null;
            });
        }
    }

    private static boolean attempt(DuLieuDayDuHoaDon d, CountDownLatch start) throws Exception {
        try (KetNoiMayChu s = new KetNoiMayChu("127.0.0.1", server.getPort())) {
            start.await();
            try {
                api(s, "sales").pay(exactCashPayment(d));
                return true;
            } catch (NgoaiLeGoiDichVu e) {
                assertTrue(e.getCode() == MaLoi.OUT_OF_STOCK || e.getCode() == MaLoi.VERSION_CONFLICT);
                return false;
            }
        }
    }
}
