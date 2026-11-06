package kiemthu;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
import client.api.GoiDichVuLichSuKho;
import client.api.GoiDichVuSanPham;
import client.api.NgoaiLeGoiDichVu;
import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.danhmuc.YeuCauLuuDanhMuc;
import common.dto.hoadon.DuLieuDayDuHoaDon;
import common.dto.hoadon.YeuCauDongHoaDon;
import common.dto.hoadon.YeuCauPhienBanHoaDon;
import common.dto.hoadon.YeuCauTaoHoaDon;
import common.dto.hoadon.YeuCauThanhToanHoaDon;
import common.dto.kho.DuLieuLichSuKho;
import common.dto.kho.YeuCauDieuChinhTonKho;
import common.dto.kho.YeuCauTraCuuLichSuKho;
import common.dto.sanpham.DuLieuBienThe;
import common.dto.sanpham.DuLieuSanPham;
import common.dto.sanpham.YeuCauLuuSanPham;
import common.dto.sanpham.YeuCauTraCuuSanPham;
import common.dto.taikhoan.KetQuaDangNhap;
import common.enums.NguonBienDongKho;
import common.enums.PhuongThucThanhToan;
import common.enums.TrangThaiHoaDon;
import common.enums.VaiTro;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;
import server.dispatcher.DieuPhoiYeuCau;
import server.entities.BienThe;
import server.entities.LichSuKho;
import server.entities.LoaiSanPham;
import server.entities.SanPham;
import server.entities.TaiKhoan;
import server.entities.Thue;
import server.entities.ThuongHieu;
import server.network.MayChuTcp;
import server.security.BoBamMatKhauPbkdf2;
import server.services.DichVuHoaDon;
import server.services.DichVuLichSuKho;
import server.services.DichVuSanPham;
import server.services.DichVuXacThuc;
import server.session.QuanLyPhienDangNhap;
import server.utils.QuanLyJpa;

/** CRUD qua TCP + JPA/H2, không sửa SQL Server thật. */
public class KiemThuTichHopDanhMuc {
    private static QuanLyJpa jpa;
    private static MayChuTcp server;
    private static QuanLyPhienDangNhap sessions;
    private static String hash;
    private KetNoiMayChu socket;
    private PhienDangNhap session;
    private GoiDichVuSanPham api;
    private GoiDichVuLichSuKho stockApi;

    @BeforeClass
    public static void setup() throws Exception {
        jpa = new QuanLyJpa(HoTroKiemThuDanhMuc.memoryProperties("catalog_management_test"));
        jpa.transaction(em -> {
            em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics()
                    .setStatisticsEnabled(true);
            return null;
        });
        hash = new BoBamMatKhauPbkdf2().hash("ManagementPass123!".toCharArray());
        jpa.transaction(em -> {
            HoTroKiemThuDanhMuc.user(em, "manager", "NV1", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            HoTroKiemThuDanhMuc.user(em, "sales", "NV2", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            return null;
        });
        sessions = new QuanLyPhienDangNhap();
        DichVuXacThuc auth = new DichVuXacThuc(jpa, sessions, new BoBamMatKhauPbkdf2());
        server = new MayChuTcp("127.0.0.1", 0, new DieuPhoiYeuCau(auth, new DichVuSanPham(jpa, auth),
                new DichVuHoaDon(jpa, auth), null, null, null, null, new DichVuLichSuKho(jpa, auth)));
        server.start();
    }

    @Before
    public void reset() {
        jpa.transaction(em -> {
            for (String entity : new String[] { "LichSuKho", "ChiTietHoaDon", "HoaDon", "AnhSanPham",
                    "BienThe", "SanPham", "LoaiSanPham", "ThuongHieu", "Thue" }) {
                em.createQuery("delete from " + entity).executeUpdate();
            }
            LoaiSanPham l = new LoaiSanPham();
            l.setMaLoai("L1");
            l.setTenLoai("Áo");
            em.persist(l);
            ThuongHieu b = new ThuongHieu();
            b.setMaThuongHieu("TH1");
            b.setTenThuongHieu("Demo");
            em.persist(b);
            SanPham p = new SanPham();
            p.setMaSP("SP1");
            p.setTenSP("Áo cơ bản");
            p.setLoaiSanPham(l);
            p.setThuongHieu(b);
            p.setGiaBan(new BigDecimal("100"));
            em.persist(p);
            BienThe v = new BienThe();
            v.setMaBienThe("BT1");
            v.setSanPham(p);
            v.setMauSac("Đỏ");
            v.setKichCo("M");
            v.setSoLuongTon(5);
            em.persist(v);
            LichSuKho opening = new LichSuKho();
            opening.setBienThe(v);
            opening.setThoiGian(LocalDateTime.of(2026, 1, 1, 0, 0));
            opening.setNguon(NguonBienDongKho.TON_MO_DAU);
            opening.setThamChieu("TEST:BASELINE:BT1");
            opening.setOperationId("TEST:BASELINE:BT1");
            opening.setSoLuongTruoc(0);
            opening.setSoLuongThayDoi(5);
            opening.setSoLuongSau(5);
            opening.setPhienBanTruoc(0L);
            opening.setLyDo("Mốc tồn fixture H2.");
            em.persist(opening);
            Thue tax = new Thue();
            tax.setMaThue("T1");
            tax.setTenThue("Thuế demo");
            tax.setPhanTram(BigDecimal.ZERO);
            em.persist(tax);
            return null;
        });
        socket = new KetNoiMayChu("127.0.0.1", server.getPort());
        session = session("manager");
        api = new GoiDichVuSanPham(socket, session);
        stockApi = new GoiDichVuLichSuKho(socket, session);
    }

    private static PhienDangNhap session(String user) {
        PhienDangNhap s = new PhienDangNhap();
        KetQuaDangNhap l = new KetQuaDangNhap();
        l.setSessionToken(sessions.create(user, hash));
        s.setLogin(l);
        return s;
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

    private static YeuCauLuuDanhMuc catalog(String id, String name, String before) {
        YeuCauLuuDanhMuc r = new YeuCauLuuDanhMuc();
        r.setCode(id);
        r.setName(name);
        r.setExpectedName(before);
        return r;
    }

    private static DuLieuSanPham product(String id, String name, String price) {
        DuLieuSanPham p = new DuLieuSanPham();
        p.setMaSP(id);
        p.setTenSP(name);
        p.setMaLoai("L1");
        p.setMaThuongHieu("TH1");
        p.setGiaBan(new BigDecimal(price));
        return p;
    }

    private static YeuCauLuuSanPham request(DuLieuSanPham value, DuLieuSanPham before) {
        YeuCauLuuSanPham r = new YeuCauLuuSanPham();
        r.setValue(value);
        r.setExpected(before);
        return r;
    }

    private static DuLieuBienThe variant(String id, String color, String size, int stock) {
        DuLieuBienThe b = new DuLieuBienThe();
        b.setMaBienThe(id);
        b.setMaSP("SP1");
        b.setMauSac(color);
        b.setKichCo(size);
        b.setSoLuongTon(stock);
        return b;
    }

    private static YeuCauDieuChinhTonKho adjustment(String id, int target, long version, String reason,
            String operationId) {
        YeuCauDieuChinhTonKho r = new YeuCauDieuChinhTonKho();
        r.setMaBienThe(id);
        r.setSoLuongMoi(BigDecimal.valueOf(target));
        r.setExpectedVersion(BigDecimal.valueOf(version));
        r.setLyDo(reason);
        r.setOperationId(operationId);
        return r;
    }

    @Test
    public void createsAndRenamesBothCatalogTypes() throws Exception {
        assertEquals("L2", api.saveCategory(catalog("L2", "Quần", null), true));
        assertEquals("TH2", api.saveBrand(catalog("TH2", "Thương hiệu mới", null), true));
        api.saveCategory(catalog("L2", "Quần dài", "Quần"), false);
        api.saveBrand(catalog("TH2", "Tên mới", "Thương hiệu mới"), false);
        assertTrue(api.categories().stream().anyMatch(c -> c.getTenLoai().equals("Quần dài")));
        assertTrue(api.brands().stream().anyMatch(c -> c.getTenThuongHieu().equals("Tên mới")));
        error(MaLoi.VERSION_CONFLICT, () -> api.saveCategory(catalog("L2", "Tên khác", "Quần"), false));
        assertEquals("Áo", api.detail("SP1").getSanPham().getTenLoai());
    }

    @Test
    public void stockLedgerPageUsesBoundedQueriesForFiftyRows() throws Exception {
        jpa.transaction(em -> {
            BienThe variant = em.getReference(BienThe.class, "BT1");
            for (int i = 0; i < 50; i++) {
                LichSuKho movement = new LichSuKho();
                movement.setBienThe(variant);
                movement.setThoiGian(LocalDateTime.of(2026, 2, 1, 0, 0).plusSeconds(i));
                movement.setNguon(NguonBienDongKho.DIEU_CHINH_TON);
                movement.setThamChieu("S24:LEDGER:" + i);
                movement.setOperationId("S24:LEDGER:" + i);
                movement.setSoLuongTruoc(i);
                movement.setSoLuongThayDoi(1);
                movement.setSoLuongSau(i + 1);
                movement.setPhienBanTruoc((long) i);
                movement.setTenNguoiThaoTacSnapshot("Quản lý thử");
                movement.setLyDo("Fixture hiệu năng H2.");
                em.persist(movement);
            }
            return null;
        });
        Statistics statistics = jpa
                .transaction(em -> em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics());
        YeuCauTraCuuLichSuKho small = new YeuCauTraCuuLichSuKho();
        small.setPageSize(1);
        YeuCauTraCuuLichSuKho page = new YeuCauTraCuuLichSuKho();
        page.setPageSize(50);
        stockApi.search(small);
        stockApi.search(page);
        long[] one = measureLedgerPage(statistics, small, 5);
        long[] fifty = measureLedgerPage(statistics, page, 5);
        System.out.println("[S24 H2 ledger] page=1 statements/request=" + one[0] + ", average-ms/request="
                + one[1] + "; page=50 statements/request=" + fifty[0] + ", average-ms/request=" + fifty[1]
                + "; H2 only.");
        assertEquals("auth, page query and count are constant for small pages", one[0], fifty[0]);
        assertEquals(4, fifty[0]);
    }

    private long[] measureLedgerPage(Statistics statistics, YeuCauTraCuuLichSuKho request, int repetitions)
            throws Exception {
        statistics.clear();
        long start = System.nanoTime();
        for (int i = 0; i < repetitions; i++) {
            stockApi.search(request);
        }
        long averageMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) / repetitions;
        return new long[] { statistics.getPrepareStatementCount() / repetitions, averageMillis };
    }

    @Test
    public void duplicateIdsNamesAndInvalidCodesAreRejected() throws Exception {
        error(MaLoi.VALIDATION_ERROR, () -> api.saveCategory(catalog("L1", "Tên khác", null), true));
        error(MaLoi.VALIDATION_ERROR, () -> api.saveCategory(catalog("L2", "ÁO", null), true));
        error(MaLoi.VALIDATION_ERROR, () -> api.saveBrand(catalog("TH2", "DEMO", null), true));
        error(MaLoi.VALIDATION_ERROR, () -> api.saveBrand(catalog("../id", "Tên", null), true));
        error(MaLoi.VALIDATION_ERROR, () -> api.saveBrand(catalog("TH2", "   ", null), true));
        assertEquals(1, api.categories().size());
        assertEquals(1, api.brands().size());
    }

    @Test
    public void createAndUpdateProductPreservesIdentifiers() throws Exception {
        assertEquals("SP2", api.saveProduct(request(product("SP2", "Áo mới", "149000.50"), null), 0));
        DuLieuSanPham before = api.detail("SP2").getSanPham();
        DuLieuSanPham value = product("SP2", "Áo cập nhật", "159000");
        api.saveProduct(request(value, before), 1);
        assertEquals("Áo cập nhật", api.detail("SP2").getSanPham().getTenSP());
        error(MaLoi.VERSION_CONFLICT, () -> api.saveProduct(request(value, before), 1));
    }

    @Test
    public void invalidProductAndMissingParentsRollBack() throws Exception {
        error(MaLoi.VALIDATION_ERROR, () -> api.saveProduct(request(product("SP2", "Tên", "-1"), null), 0));
        error(MaLoi.VALIDATION_ERROR,
                () -> api.saveProduct(request(product("SP2", "Tên", "1.001"), null), 0));
        DuLieuSanPham v = product("SP2", "Tên", "100");
        v.setMaLoai("MISSING");
        error(MaLoi.VALIDATION_ERROR, () -> api.saveProduct(request(v, null), 0));
        error(MaLoi.NOT_FOUND, () -> api.detail("SP2"));
        assertEquals(1, api.search(new YeuCauTraCuuSanPham()).getTotal());
    }

    @Test
    public void stoppingProductIsSoftAndCanBeReactivated() throws Exception {
        DuLieuSanPham before = api.detail("SP1").getSanPham();
        api.saveProduct(request(before, before), 2);
        assertFalse(api.detail("SP1").getSanPham().getTrangThai());
        YeuCauTraCuuSanPham all = new YeuCauTraCuuSanPham();
        all.setIncludeInactive(true);
        assertEquals(1, api.search(all).getTotal());
        assertEquals(0, api.search(new YeuCauTraCuuSanPham()).getTotal());
        DuLieuSanPham stopped = api.detail("SP1").getSanPham();
        api.saveProduct(request(product("SP1", stopped.getTenSP(), "100"), stopped), 1);
        assertTrue(api.detail("SP1").getSanPham().getTrangThai());
        assertEquals(1, api.detail("SP1").getBienThe().size());
    }

    @Test
    public void managerRoleIsRequiredForEveryWrite() throws Exception {
        DuLieuSanPham before = api.detail("SP1").getSanPham();
        session.setLogin(session("sales").getLogin());
        error(MaLoi.FORBIDDEN, () -> api.saveCategory(catalog("L2", "Quần", null), true));
        error(MaLoi.FORBIDDEN, () -> api.saveCategory(catalog("L1", "Quần", "Áo"), false));
        error(MaLoi.FORBIDDEN, () -> api.saveBrand(catalog("TH2", "Tên", null), true));
        error(MaLoi.FORBIDDEN, () -> api.saveBrand(catalog("TH1", "Tên", "Demo"), false));
        error(MaLoi.FORBIDDEN, () -> api.saveProduct(request(product("SP2", "Tên", "100"), null), 0));
        error(MaLoi.FORBIDDEN, () -> api.saveProduct(request(before, before), 1));
        error(MaLoi.FORBIDDEN, () -> api.saveProduct(request(before, before), 2));
        error(MaLoi.FORBIDDEN, () -> api.saveVariant(variant("BT2", "Đỏ", "L", 1), true));
        error(MaLoi.FORBIDDEN, () -> api.saveVariant(variant("BT1", "Đỏ", "M", 1), false));
        error(MaLoi.FORBIDDEN, () -> stockApi.search(new YeuCauTraCuuLichSuKho()));
        error(MaLoi.FORBIDDEN,
                () -> stockApi.adjust(adjustment("BT1", 6, 0, "cycle count", UUID.randomUUID().toString())));
    }

    @Test
    public void revokedManagerAndUnauthenticatedWritesAreRejected() throws Exception {
        jpa.transaction(em -> {
            em.find(TaiKhoan.class, "manager").setTrangThai(false);
            return null;
        });
        try {
            error(MaLoi.UNAUTHENTICATED, () -> api.saveCategory(catalog("L2", "Quần", null), true));
        } finally {
            jpa.transaction(em -> {
                em.find(TaiKhoan.class, "manager").setTrangThai(true);
                return null;
            });
        }
        assertNull(session.getToken());
        error(MaLoi.UNAUTHENTICATED, () -> api.saveBrand(catalog("TH2", "Tên", null), true));
    }

    @Test
    public void variantCreationStockUpdateAndDisable() throws Exception {
        YeuCauTraCuuLichSuKho openingFilter = new YeuCauTraCuuLichSuKho();
        openingFilter.setNguon(NguonBienDongKho.TON_MO_DAU);
        openingFilter.setTuNgay(java.time.LocalDate.of(2026, 1, 1));
        openingFilter.setDenNgay(java.time.LocalDate.of(2026, 1, 1));
        assertEquals(1, stockApi.search(openingFilter).getTotal());
        openingFilter.setMaNV("NV1");
        assertEquals(0, stockApi.search(openingFilter).getTotal());
        DuLieuBienThe created = api.saveVariant(variant("BT2", "Vàng", "L", 0), true);
        assertEquals(0, created.getSoLuongTon());
        YeuCauTraCuuLichSuKho filter = new YeuCauTraCuuLichSuKho();
        filter.setMaBienThe("BT2");
        DuLieuLichSuKho initialized = stockApi.search(filter).getItems().get(0);
        assertEquals(NguonBienDongKho.KHOI_TAO_BIEN_THE, initialized.getNguon());
        String operationId = UUID.randomUUID().toString();
        YeuCauDieuChinhTonKho request = adjustment("BT2", 7, created.getPhienBan(), "Kiểm kê thực tế.",
                operationId);
        DuLieuLichSuKho changed = stockApi.adjust(request);
        assertEquals(7, changed.getSoLuongSau());
        YeuCauTraCuuLichSuKho actorFilter = new YeuCauTraCuuLichSuKho();
        actorFilter.setNguon(NguonBienDongKho.DIEU_CHINH_TON);
        actorFilter.setMaNV("NV1");
        assertEquals(1, stockApi.search(actorFilter).getTotal());
        assertEquals(7, api.detail("SP1").getBienThe().stream().filter(b -> "BT2".equals(b.getMaBienThe()))
                .findFirst().get().getSoLuongTon());
        DuLieuLichSuKho replay = stockApi.adjust(request);
        assertEquals(changed.getMaLichSuKho(), replay.getMaLichSuKho());
        assertEquals(2, stockApi.search(filter).getTotal());
        assertEquals(7, changed.getSoLuongSau());
        assertEquals(7, changed.getSoLuongTruoc() + changed.getSoLuongThayDoi());
        error(MaLoi.VERSION_CONFLICT, () -> api.saveVariant(created, false));
        error(MaLoi.VERSION_CONFLICT, () -> stockApi
                .adjust(adjustment("BT2", 8, created.getPhienBan(), "Kiểm kê thực tế.", operationId)));
        error(MaLoi.VERSION_CONFLICT, () -> stockApi.adjust(
                adjustment("BT2", 8, created.getPhienBan(), "Lý do khác.", UUID.randomUUID().toString())));
        DuLieuBienThe current = api.detail("SP1").getBienThe().stream()
                .filter(b -> "BT2".equals(b.getMaBienThe())).findFirst().get();
        DuLieuBienThe unloggedStockEdit = new DuLieuBienThe();
        unloggedStockEdit.setMaSP(current.getMaSP());
        unloggedStockEdit.setMaBienThe(current.getMaBienThe());
        unloggedStockEdit.setMauSac(current.getMauSac());
        unloggedStockEdit.setKichCo(current.getKichCo());
        unloggedStockEdit.setSoLuongTon(8);
        unloggedStockEdit.setTrangThai(current.getTrangThai());
        unloggedStockEdit.setPhienBan(current.getPhienBan());
        error(MaLoi.VALIDATION_ERROR, () -> api.saveVariant(unloggedStockEdit, false));
        assertEquals(7, api.detail("SP1").getBienThe().stream().filter(b -> "BT2".equals(b.getMaBienThe()))
                .findFirst().get().getSoLuongTon());
        YeuCauDieuChinhTonKho fractional = adjustment("BT2", 8, current.getPhienBan(),
                "Số lượng phải nguyên.", UUID.randomUUID().toString());
        fractional.setSoLuongMoi(new BigDecimal("7.5"));
        error(MaLoi.INVALID_REQUEST, () -> stockApi.adjust(fractional));
        YeuCauDieuChinhTonKho fractionalVersion = adjustment("BT2", 8, current.getPhienBan(),
                "Version phải nguyên.", UUID.randomUUID().toString());
        fractionalVersion.setExpectedVersion(new BigDecimal("1.5"));
        error(MaLoi.INVALID_REQUEST, () -> stockApi.adjust(fractionalVersion));
        current.setTrangThai(false);
        api.saveVariant(current, false);
        assertEquals(2, api.detail("SP1").getBienThe().size());
        assertEquals(5, api.detail("SP1").getSanPham().getTongTon());
    }

    @Test
    public void duplicateVariantAndImmutableIdentity() throws Exception {
        error(MaLoi.VALIDATION_ERROR, () -> api.saveVariant(variant("BT2", "đỏ", "m", 1), true));
        error(MaLoi.VALIDATION_ERROR, () -> api.saveVariant(variant("BT2", "Vàng", "L", -1), true));
        DuLieuBienThe b = api.detail("SP1").getBienThe().get(0);
        b.setMauSac("Xanh");
        error(MaLoi.VALIDATION_ERROR, () -> api.saveVariant(b, false));
        assertEquals(1, api.detail("SP1").getBienThe().size());
    }

    @Test
    public void fractionalInitialStockIsRejectedAtSocketBoundary() throws Exception {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("maBienThe", "BT_FRACTIONAL");
        payload.put("maSP", "SP1");
        payload.put("mauSac", "Tím");
        payload.put("kichCo", "M");
        payload.put("soLuongTon", new BigDecimal("1.5"));
        payload.put("trangThai", true);
        PhanHoi<com.fasterxml.jackson.databind.JsonNode> response = socket.send(HanhDong.THEM_BIEN_THE,
                session.getToken(), payload);
        assertFalse(response.isSuccess());
        assertEquals(MaLoi.INVALID_REQUEST, response.getErrorCode());
        assertTrue(api.detail("SP1").getBienThe().stream()
                .noneMatch(b -> "BT_FRACTIONAL".equals(b.getMaBienThe())));
    }

    @Test
    public void twoEditorsCannotOverwriteEachOther() throws Exception {
        DuLieuSanPham before = api.detail("SP1").getSanPham();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> a = pool.submit(() -> attemptProduct(before, "Tên A", start));
            Future<Boolean> b = pool.submit(() -> attemptProduct(before, "Tên B", start));
            start.countDown();
            assertEquals(1, (a.get(15, TimeUnit.SECONDS) ? 1 : 0) + (b.get(15, TimeUnit.SECONDS) ? 1 : 0));
        } finally {
            pool.shutdownNow();
        }
    }

    private static boolean attemptProduct(DuLieuSanPham before, String name, CountDownLatch start)
            throws Exception {
        try (KetNoiMayChu s = new KetNoiMayChu("127.0.0.1", server.getPort())) {
            start.await();
            try {
                new GoiDichVuSanPham(s, session("manager"))
                        .saveProduct(request(product("SP1", name, "100"), before), 1);
                return true;
            } catch (NgoaiLeGoiDichVu e) {
                assertEquals(MaLoi.VERSION_CONFLICT, e.getCode());
                return false;
            }
        }
    }

    @Test
    public void historicalPriceAndRowsSurviveProductEdits() throws Exception {
        GoiDichVuHoaDon invoices = new GoiDichVuHoaDon(socket, session);
        DuLieuDayDuHoaDon d = invoice(invoices);
        DuLieuSanPham before = api.detail("SP1").getSanPham();
        api.saveProduct(request(product("SP1", "Tên mới", "200"), before), 1);
        DuLieuDayDuHoaDon paid = invoices.pay(payment(d));
        assertEquals(0, new BigDecimal("100").compareTo(paid.getHoaDon().getTongTien()));
        DuLieuSanPham now = api.detail("SP1").getSanPham();
        api.saveProduct(request(now, now), 2);
        assertEquals(1, invoices.detail("HD1").getChiTiet().size());
        assertEquals(4, api.detail("SP1").getBienThe().get(0).getSoLuongTon());
    }

    @Test
    public void stockAdjustmentRacingCheckoutNeverLosesSale() throws Exception {
        GoiDichVuHoaDon invoices = new GoiDichVuHoaDon(socket, session);
        DuLieuDayDuHoaDon d = invoice(invoices);
        DuLieuBienThe before = api.detail("SP1").getBienThe().get(0);
        before.setSoLuongTon(7);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> changed = pool.submit(() -> {
                try (KetNoiMayChu s = new KetNoiMayChu("127.0.0.1", server.getPort())) {
                    start.await();
                    try {
                        new GoiDichVuLichSuKho(s, session("manager")).adjust(adjustment("BT1", 7,
                                before.getPhienBan(), "Kiểm kê đồng thời.", UUID.randomUUID().toString()));
                        return true;
                    } catch (NgoaiLeGoiDichVu ex) {
                        assertEquals(MaLoi.VERSION_CONFLICT, ex.getCode());
                        return false;
                    }
                }
            });
            Future<DuLieuDayDuHoaDon> paid = pool.submit(() -> {
                try (KetNoiMayChu s = new KetNoiMayChu("127.0.0.1", server.getPort())) {
                    start.await();
                    return new GoiDichVuHoaDon(s, session("manager")).pay(payment(d));
                }
            });
            start.countDown();
            boolean didChange = changed.get(15, TimeUnit.SECONDS);
            assertEquals(TrangThaiHoaDon.DA_THANH_TOAN,
                    paid.get(15, TimeUnit.SECONDS).getHoaDon().getTrangThai());
            assertEquals(didChange ? 6 : 4, api.detail("SP1").getBienThe().get(0).getSoLuongTon());
            long adjustmentCount = jpa.transaction(em -> em
                    .createQuery("select count(l) from LichSuKho l where l.nguon = :source", Long.class)
                    .setParameter("source", NguonBienDongKho.DIEU_CHINH_TON).getSingleResult());
            assertEquals(didChange ? 1 : 0, adjustmentCount);
        } finally {
            pool.shutdownNow();
        }
    }

    private static YeuCauPhienBanHoaDon version(DuLieuDayDuHoaDon d) {
        YeuCauPhienBanHoaDon r = new YeuCauPhienBanHoaDon();
        r.setMaHD(d.getHoaDon().getMaHD());
        r.setPhienBan(d.getHoaDon().getPhienBan());
        return r;
    }

    private static YeuCauThanhToanHoaDon payment(DuLieuDayDuHoaDon d) {
        YeuCauThanhToanHoaDon r = new YeuCauThanhToanHoaDon();
        r.setMaHD(d.getHoaDon().getMaHD());
        r.setPhienBan(d.getHoaDon().getPhienBan());
        r.setTongTienDaXem(d.getHoaDon().getTongTien());
        r.setPhuongThucThanhToan(PhuongThucThanhToan.TIEN_MAT);
        r.setTienKhachDua(d.getHoaDon().getTongTien());
        return r;
    }

    private static DuLieuDayDuHoaDon invoice(GoiDichVuHoaDon api) throws Exception {
        YeuCauTaoHoaDon create = new YeuCauTaoHoaDon();
        create.setMaHD("HD1");
        create.setMaThue("T1");
        DuLieuDayDuHoaDon d = api.create(create);
        YeuCauDongHoaDon line = new YeuCauDongHoaDon();
        line.setMaHD("HD1");
        line.setPhienBan(d.getHoaDon().getPhienBan());
        line.setMaBienThe("BT1");
        line.setSoLuong(1);
        return api.line(line, 0);
    }
}
