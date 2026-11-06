package kiemthu;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Collections;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import client.api.GoiDichVuSanPham;
import client.api.NgoaiLeGoiDichVu;
import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.danhmuc.DuLieuThuongHieu;
import common.dto.sanpham.DuLieuAnhSanPham;
import common.dto.sanpham.DuLieuDayDuSanPham;
import common.dto.sanpham.KetQuaTraCuuSanPham;
import common.dto.sanpham.YeuCauTraCuuSanPham;
import common.dto.taikhoan.KetQuaDangNhap;
import common.enums.VaiTro;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import server.dispatcher.DieuPhoiYeuCau;
import server.entities.AnhSanPham;
import server.entities.BienThe;
import server.entities.LoaiSanPham;
import server.entities.SanPham;
import server.entities.TaiKhoan;
import server.entities.ThuongHieu;
import server.network.MayChuTcp;
import server.security.BoBamMatKhauPbkdf2;
import server.services.DichVuSanPham;
import server.services.DichVuXacThuc;
import server.session.QuanLyPhienDangNhap;
import server.utils.QuanLyJpa;

public class KiemThuTichHopSanPham {
    private static QuanLyJpa jpa;
    private static MayChuTcp server;
    private static QuanLyPhienDangNhap sessions;
    private static String hash;
    private KetNoiMayChu socket;
    private PhienDangNhap session;
    private GoiDichVuSanPham api;

    @BeforeClass
    public static void setup() throws Exception {
        jpa = new QuanLyJpa(HoTroKiemThuDanhMuc.memoryProperties("products_test"));
        hash = new BoBamMatKhauPbkdf2().hash("ProductPass123!".toCharArray());
        jpa.transaction(em -> {
            HoTroKiemThuDanhMuc.user(em, "sales", "NV01", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            HoTroKiemThuDanhMuc.user(em, "manager", "NV02", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            LoaiSanPham category = new LoaiSanPham();
            category.setMaLoai("L1");
            category.setTenLoai("Áo");
            em.persist(category);
            LoaiSanPham other = new LoaiSanPham();
            other.setMaLoai("L2");
            other.setTenLoai("Quần");
            em.persist(other);
            ThuongHieu brand = new ThuongHieu();
            brand.setMaThuongHieu("TH1");
            brand.setTenThuongHieu("Canifa");
            em.persist(brand);
            ThuongHieu otherBrand = new ThuongHieu();
            otherBrand.setMaThuongHieu("TH2");
            otherBrand.setTenThuongHieu("Owen");
            em.persist(otherBrand);
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
        sessions = new QuanLyPhienDangNhap();
        DichVuXacThuc auth = new DichVuXacThuc(jpa, sessions, new BoBamMatKhauPbkdf2());
        server = new MayChuTcp("127.0.0.1", 0, new DieuPhoiYeuCau(auth, new DichVuSanPham(jpa, auth)));
        server.start();
    }

    @Before
    public void client() {
        jpa.transaction(em -> {
            em.find(TaiKhoan.class, "sales").setTrangThai(true);
            return null;
        });
        socket = new KetNoiMayChu("127.0.0.1", server.getPort());
        session = new PhienDangNhap();
        useRole("sales");
        api = new GoiDichVuSanPham(socket, session);
    }

    private void useRole(String username) {
        KetQuaDangNhap login = new KetQuaDangNhap();
        // Fixture cấp token; đăng nhập thật đã được kiểm tra trong KiemThuTichHopXacThuc.
        login.setSessionToken(sessions.create(username, hash));
        session.setLogin(login);
    }

    @After
    public void closeClient() {
        socket.close();
    }

    @AfterClass
    public static void cleanup() {
        if (server != null) {
            server.close();
        }
        if (jpa != null) {
            jpa.close();
        }
    }

    @Test
    public void paginatedSearchPreservesMoneyStockAndStableOrder() throws Exception {
        YeuCauTraCuuSanPham request = new YeuCauTraCuuSanPham();
        request.setPageSize(2);
        KetQuaTraCuuSanPham first = api.search(request);
        assertEquals(3, first.getTotal());
        assertEquals(2, first.getItems().size());
        assertEquals("SP01", first.getItems().get(0).getMaSP());
        assertEquals(0, new BigDecimal("149000.50").compareTo(first.getItems().get(0).getGiaBan()));
        assertEquals(10, first.getItems().get(0).getTongTon());
        request.setPage(1);
        KetQuaTraCuuSanPham next = api.search(request);
        assertEquals(3, next.getTotal());
        assertEquals(1, next.getItems().size());
        assertEquals("SP04", next.getItems().get(0).getMaSP());
        assertEquals(4294967294L, next.getItems().get(0).getTongTon());
        assertEquals(0, new BigDecimal("9999999999999999.99").compareTo(next.getItems().get(0).getGiaBan()));
        request.setPage(99);
        assertTrue(api.search(request).getItems().isEmpty());
    }

    @Test
    public void nameCategoryAndBrandFiltersAreCombined() throws Exception {
        YeuCauTraCuuSanPham request = new YeuCauTraCuuSanPham();
        request.setTenSP("  POLO  ");
        request.setMaLoai("L1");
        request.setMaThuongHieu("TH1");
        assertEquals("SP01", api.search(request).getItems().get(0).getMaSP());
        request.setMaThuongHieu("TH2");
        assertEquals(0, api.search(request).getTotal());
    }

    @Test
    public void colorAndSizeMustMatchSameActiveVariant() throws Exception {
        YeuCauTraCuuSanPham request = new YeuCauTraCuuSanPham();
        request.setMauSac("đỏ");
        request.setKichCo("L");
        assertEquals(0, api.search(request).getTotal());
        request.setMauSac("Vàng");
        assertEquals("SP01", api.search(request).getItems().get(0).getMaSP());
        request.setMauSac("Nâu");
        request.setKichCo("XS");
        assertEquals(0, api.search(request).getTotal());
        request.setIncludeInactive(true);
        expect(MaLoi.FORBIDDEN, () -> api.search(request));
        useRole("manager");
        assertEquals(1, api.search(request).getTotal());
    }

    @Test
    public void wildcardAndSqlLikeTextAreLiteralNotExecutable() throws Exception {
        for (String text : new String[] { "%", "_", "[", "]", "!" }) {
            YeuCauTraCuuSanPham request = new YeuCauTraCuuSanPham();
            request.setTenSP(text);
            assertEquals(text, 1, api.search(request).getTotal());
            assertEquals("SP02", api.search(request).getItems().get(0).getMaSP());
        }
        YeuCauTraCuuSanPham request = new YeuCauTraCuuSanPham();
        request.setTenSP("' or 1=1 --");
        assertEquals(0, api.search(request).getTotal());
    }

    @Test
    public void detailReturnsDtoActiveVariantsAndMatchingImagePaths() throws Exception {
        DuLieuDayDuSanPham detail = api.detail("SP01");
        assertEquals(2, detail.getBienThe().size());
        assertEquals(3, detail.getHinhAnh().size());
        assertEquals(10, detail.getSanPham().getTongTon());
        for (DuLieuAnhSanPham image : detail.getHinhAnh()) {
            assertNotNull(getClass().getClassLoader().getResource(image.getDuongDan()));
        }
        String json = BoMaHoaJson.mapper().writeValueAsString(detail);
        assertFalse(json.contains("hibernateLazyInitializer"));
        assertFalse(json.contains("passwordHash"));
        assertNotNull(detail.getBienThe().get(0).getPhienBan());
        useRole("manager");
        assertEquals(3, api.detail("SP01").getBienThe().size());
        assertEquals(4, api.detail("SP01").getHinhAnh().size());
    }

    @Test
    public void salesCannotReadInactiveProductEvenByDirectId() throws Exception {
        expect(MaLoi.NOT_FOUND, () -> api.detail("SP03"));
        YeuCauTraCuuSanPham request = new YeuCauTraCuuSanPham();
        request.setIncludeInactive(true);
        expect(MaLoi.FORBIDDEN, () -> api.search(request));
        useRole("manager");
        assertEquals(4, api.search(request).getTotal());
        assertFalse(api.detail("SP03").getSanPham().getTrangThai());
    }

    @Test
    public void productWithNoVariantsOrImagesAndMissingIdAreHandled() throws Exception {
        DuLieuDayDuSanPham detail = api.detail("SP02");
        assertTrue(detail.getBienThe().isEmpty());
        assertTrue(detail.getHinhAnh().isEmpty());
        assertEquals(0, detail.getSanPham().getTongTon());
        expect(MaLoi.NOT_FOUND, () -> api.detail("missing"));
        expect(MaLoi.VALIDATION_ERROR, () -> api.detail(" "));
    }

    @Test
    public void categoryAndBrandLookupsReturnTypedDtos() throws Exception {
        assertEquals(2, api.categories().size());
        assertEquals(2, api.brands().size());
        assertTrue(api.brands().get(0) instanceof DuLieuThuongHieu);
        assertEquals("TH1", api.brands().get(0).getMaThuongHieu());
    }

    @Test
    public void allProductActionsRequireActiveSession() throws Exception {
        session.clear();
        expect(MaLoi.UNAUTHENTICATED, () -> api.search(new YeuCauTraCuuSanPham()));
        expect(MaLoi.UNAUTHENTICATED, () -> api.detail("SP01"));
        expect(MaLoi.UNAUTHENTICATED, () -> api.categories());
        expect(MaLoi.UNAUTHENTICATED, () -> api.brands());
        useRole("sales");
        jpa.transaction(em -> {
            em.find(TaiKhoan.class, "sales").setTrangThai(false);
            return null;
        });
        expect(MaLoi.UNAUTHENTICATED, () -> api.search(new YeuCauTraCuuSanPham()));
        assertNull(session.getToken());
    }

    @Test
    public void invalidPaginationAndPayloadAreRejected() throws Exception {
        YeuCauTraCuuSanPham request = new YeuCauTraCuuSanPham();
        request.setPage(-1);
        expect(MaLoi.VALIDATION_ERROR, () -> api.search(request));
        request.setPage(Integer.MAX_VALUE);
        expect(MaLoi.VALIDATION_ERROR, () -> api.search(request));
        request.setPage(0);
        request.setPageSize(101);
        expect(MaLoi.VALIDATION_ERROR, () -> api.search(request));
        request.setPageSize(20);
        request.setMauSac(String.join("", Collections.nCopies(51, "x")));
        expect(MaLoi.VALIDATION_ERROR, () -> api.search(request));
        assertEquals(MaLoi.INVALID_REQUEST,
                socket.send(HanhDong.TRA_CUU_SAN_PHAM, session.getToken(), null).getErrorCode());
    }

    private static SanPham product(EntityManager em, String id, String name, LoaiSanPham category,
            ThuongHieu brand, String price, boolean active) {
        SanPham p = new SanPham();
        p.setMaSP(id);
        p.setTenSP(name);
        p.setLoaiSanPham(category);
        p.setThuongHieu(brand);
        p.setGiaBan(new BigDecimal(price));
        p.setTrangThai(active);
        em.persist(p);
        return p;
    }

    private static void variant(EntityManager em, String id, SanPham p, String color, String size, int stock,
            boolean active) {
        BienThe b = new BienThe();
        b.setMaBienThe(id);
        b.setSanPham(p);
        b.setMauSac(color);
        b.setKichCo(size);
        b.setSoLuongTon(stock);
        b.setTrangThai(active);
        em.persist(b);
    }

    private static void image(EntityManager em, String id, SanPham p, String color, String file, int order,
            boolean primary) {
        AnhSanPham a = new AnhSanPham();
        a.setMaAnh(id);
        a.setSanPham(p);
        a.setMauSac(color);
        a.setDuongDan("images/products/1001/" + file + ".jpg");
        a.setThuTu(order);
        a.setAnhChinh(primary);
        em.persist(a);
    }

    private interface Task {
        void run() throws Exception;
    }

    private static void expect(MaLoi code, Task task) throws Exception {
        try {
            task.run();
            fail("Expected " + code);
        } catch (NgoaiLeGoiDichVu expected) {
            assertEquals(code, expected.getCode());
        }
    }
}
