package kiemthu;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import client.api.GoiDichVuNhaCungCap;
import client.api.GoiDichVuPhieuNhap;
import client.api.NgoaiLeGoiDichVu;
import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.nhacungcap.DuLieuNhaCungCap;
import common.dto.nhacungcap.YeuCauLuuNhaCungCap;
import common.dto.nhacungcap.YeuCauTraCuuNhaCungCap;
import common.dto.nhacungcap.YeuCauTrangThaiNhaCungCap;
import common.dto.phieunhap.DuLieuDayDuPhieuNhap;
import common.dto.phieunhap.YeuCauDongPhieuNhap;
import common.dto.phieunhap.YeuCauTaoPhieuNhap;
import common.dto.phieunhap.YeuCauXacNhanPhieuNhap;
import common.dto.taikhoan.KetQuaDangNhap;
import common.enums.NguonBienDongKho;
import common.enums.TrangThaiPhieuNhap;
import common.enums.VaiTro;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;
import server.dispatcher.DieuPhoiYeuCau;
import server.entities.BienThe;
import server.entities.LichSuKho;
import server.entities.LoaiSanPham;
import server.entities.SanPham;
import server.entities.ThuongHieu;
import server.network.MayChuTcp;
import server.security.BoBamMatKhauPbkdf2;
import server.services.DichVuNhaCungCap;
import server.services.DichVuPhieuNhap;
import server.services.DichVuXacThuc;
import server.session.QuanLyPhienDangNhap;
import server.utils.QuanLyJpa;

/** End-to-end TCP/JPA/H2 tests. No SQL Server connection is opened. */
public class KiemThuTichHopPhieuNhap {
    private static final AtomicInteger ids = new AtomicInteger();
    private static QuanLyJpa jpa;
    private static MayChuTcp server;
    private static QuanLyPhienDangNhap sessions;
    private static String hash;
    private KetNoiMayChu socket;
    private GoiDichVuNhaCungCap suppliers;
    private GoiDichVuPhieuNhap receipts;

    @BeforeClass
    public static void setupClass() throws Exception {
        jpa = new QuanLyJpa(HoTroKiemThuDanhMuc.memoryProperties("receipt_test"));
        hash = new BoBamMatKhauPbkdf2().hash("ReceiptPass123!".toCharArray());
        jpa.transaction(em -> {
            HoTroKiemThuDanhMuc.user(em, "manager", "NV-RM", VaiTro.NHAN_VIEN_QUAN_LY, hash);
            HoTroKiemThuDanhMuc.user(em, "sales", "NV-RS", VaiTro.NHAN_VIEN_BAN_HANG, hash);
            LoaiSanPham type = new LoaiSanPham();
            type.setMaLoai("L-R");
            type.setTenLoai("Áo nhập");
            em.persist(type);
            ThuongHieu brand = new ThuongHieu();
            brand.setMaThuongHieu("TH-R");
            brand.setTenThuongHieu("Demo nhập");
            em.persist(brand);
            SanPham product = new SanPham();
            product.setMaSP("SP-R");
            product.setTenSP("Áo nhập test");
            product.setLoaiSanPham(type);
            product.setThuongHieu(brand);
            product.setGiaBan(new BigDecimal("100"));
            em.persist(product);
            for (String id : new String[] { "BT-R1", "BT-R2" }) {
                BienThe variant = new BienThe();
                variant.setMaBienThe(id);
                variant.setSanPham(product);
                variant.setMauSac("Đen-" + id);
                variant.setKichCo("M");
                variant.setSoLuongTon(5);
                em.persist(variant);
            }
            return null;
        });
        sessions = new QuanLyPhienDangNhap();
        DichVuXacThuc auth = new DichVuXacThuc(jpa, sessions, new BoBamMatKhauPbkdf2());
        DieuPhoiYeuCau dispatcher = new DieuPhoiYeuCau(auth, null, null, null, null, null, null, null,
                new DichVuNhaCungCap(jpa, auth), new DichVuPhieuNhap(jpa, auth));
        server = new MayChuTcp("127.0.0.1", 0, dispatcher);
        server.start();
    }

    @Before
    public void reset() {
        jpa.transaction(em -> {
            em.createQuery("delete from PhieuNhapThaoTac").executeUpdate();
            em.createQuery("delete from ChiTietPhieuNhap").executeUpdate();
            em.createQuery("delete from PhieuNhap").executeUpdate();
            em.createQuery("delete from NhaCungCap").executeUpdate();
            em.createQuery("delete from LichSuKho").executeUpdate();
            em.find(BienThe.class, "BT-R1").setSoLuongTon(5);
            em.find(BienThe.class, "BT-R2").setSoLuongTon(5);
            return null;
        });
        socket = new KetNoiMayChu("127.0.0.1", server.getPort());
        PhienDangNhap session = new PhienDangNhap();
        KetQuaDangNhap login = new KetQuaDangNhap();
        login.setSessionToken(sessions.create("manager", hash));
        session.setLogin(login);
        suppliers = new GoiDichVuNhaCungCap(socket, session);
        receipts = new GoiDichVuPhieuNhap(socket, session);
    }

    @After
    public void close() {
        if (socket != null) {
            socket.close();
        }
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

    @Test
    public void draftReceiptDoesNotChangeStockAndConfirmationAddsStockAndLedgerOnce() throws Exception {
        String vendor = "NCC" + ids.incrementAndGet(), code = "PN" + ids.incrementAndGet();
        DuLieuNhaCungCap supplier = suppliers.create(supplier(vendor, "Nhà cung cấp thử"));
        assertTrue(supplier.getTrangThai());
        YeuCauTaoPhieuNhap create = new YeuCauTaoPhieuNhap();
        create.setMaPhieuNhap(code);
        create.setMaNCC(vendor);
        create.setOperationId(uuid());
        DuLieuDayDuPhieuNhap draft = receipts.create(create);
        assertEquals(TrangThaiPhieuNhap.NHAP, draft.getPhieuNhap().getTrangThai());
        DuLieuDayDuPhieuNhap createRetry = receipts.create(create);
        assertEquals(code, createRetry.getPhieuNhap().getMaPhieuNhap());

        YeuCauDongPhieuNhap first = line(draft, "BT-R1", 2, "25.50", uuid());
        draft = receipts.addLine(first);
        DuLieuDayDuPhieuNhap addRetry = receipts.addLine(first);
        assertEquals(1, addRetry.getChiTiet().size());
        assertEquals(5, stock("BT-R1"));
        YeuCauDongPhieuNhap second = line(draft, "BT-R2", 1, "40.00", uuid());
        draft = receipts.addLine(second);
        assertEquals(91, draft.getPhieuNhap().getTongTien().intValue());
        assertEquals(5, stock("BT-R1"));
        assertEquals(5, stock("BT-R2"));
        assertEquals(0, movementCount("PN:" + code + ":BT-R1"));

        YeuCauXacNhanPhieuNhap confirm = confirm(draft, uuid());
        DuLieuDayDuPhieuNhap confirmed = receipts.confirm(confirm);
        assertEquals(TrangThaiPhieuNhap.DA_XAC_NHAN, confirmed.getPhieuNhap().getTrangThai());
        assertNotNull(confirmed.getPhieuNhap().getNgayXacNhan());
        assertEquals(7, stock("BT-R1"));
        assertEquals(6, stock("BT-R2"));
        assertEquals(1, movementCount("PN:" + code + ":BT-R1"));
        assertEquals(1, movementCount("PN:" + code + ":BT-R2"));
        assertEquals(NguonBienDongKho.NHAP_HANG, movement("PN:" + code + ":BT-R1").getNguon());
        assertEquals(5, movement("PN:" + code + ":BT-R1").getSoLuongTruoc());
        assertEquals(2, movement("PN:" + code + ":BT-R1").getSoLuongThayDoi());
        assertEquals(7, movement("PN:" + code + ":BT-R1").getSoLuongSau());

        DuLieuDayDuPhieuNhap retry = receipts.confirm(confirm);
        assertEquals(confirmed.getPhieuNhap().getNgayXacNhan(), retry.getPhieuNhap().getNgayXacNhan());
        assertEquals(7, stock("BT-R1"));
        assertEquals(1, movementCount("PN:" + code + ":BT-R1"));
        YeuCauXacNhanPhieuNhap changedId = confirm(draft, uuid());
        error(MaLoi.VERSION_CONFLICT, () -> receipts.confirm(changedId));
    }

    @Test
    public void lineOperationIdRejectsChangedPayloadAndStaleVersion() throws Exception {
        String vendor = makeSupplier();
        DuLieuDayDuPhieuNhap draft = createReceipt(vendor);
        YeuCauDongPhieuNhap first = line(draft, "BT-R1", 2, "10.00", uuid());
        DuLieuDayDuPhieuNhap saved = receipts.addLine(first);
        YeuCauDongPhieuNhap mismatchedRetry = line(draft, "BT-R1", 3, "10.00", first.getOperationId());
        error(MaLoi.VERSION_CONFLICT, () -> receipts.addLine(mismatchedRetry));
        YeuCauDongPhieuNhap stale = line(draft, "BT-R2", 1, "10.00", uuid());
        error(MaLoi.VERSION_CONFLICT, () -> receipts.addLine(stale));
        assertEquals(1, receipts.detail(draft.getPhieuNhap().getMaPhieuNhap()).getChiTiet().size());
        assertEquals(saved.getPhieuNhap().getPhienBan().longValue(), receipts
                .detail(draft.getPhieuNhap().getMaPhieuNhap()).getPhieuNhap().getPhienBan().longValue());
        assertEquals(5, stock("BT-R1"));
    }

    @Test
    public void fractionalReceiptQuantityIsRejectedBeforePersistence() throws Exception {
        DuLieuDayDuPhieuNhap draft = createReceipt(makeSupplier());
        Map<String, Object> payload = new java.util.HashMap<String, Object>();
        payload.put("maPhieuNhap", draft.getPhieuNhap().getMaPhieuNhap());
        payload.put("phienBan", draft.getPhieuNhap().getPhienBan());
        payload.put("maBienThe", "BT-R1");
        payload.put("soLuong", new BigDecimal("1.9"));
        payload.put("donGiaNhap", new BigDecimal("10.00"));
        payload.put("operationId", uuid());
        PhanHoi<com.fasterxml.jackson.databind.JsonNode> response = socket.send(HanhDong.THEM_DONG_PHIEU_NHAP,
                null, payload);
        assertFalse(response.isSuccess());
        assertEquals(MaLoi.INVALID_REQUEST, response.getErrorCode());
        assertTrue(receipts.detail(draft.getPhieuNhap().getMaPhieuNhap()).getChiTiet().isEmpty());
        assertEquals(5, stock("BT-R1"));
    }

    @Test
    public void failedOverflowConfirmationRollsBackAllInventoryAndLedger() throws Exception {
        String vendor = makeSupplier();
        DuLieuDayDuPhieuNhap draft = createReceipt(vendor);
        draft = receipts.addLine(line(draft, "BT-R1", 2, "1.00", uuid()));
        draft = receipts.addLine(line(draft, "BT-R2", 1, "1.00", uuid()));
        jpa.transaction(em -> {
            em.find(BienThe.class, "BT-R2").setSoLuongTon(Integer.MAX_VALUE);
            return null;
        });
        YeuCauXacNhanPhieuNhap request = confirm(draft, uuid());
        error(MaLoi.VALIDATION_ERROR, () -> receipts.confirm(request));
        assertEquals(5, stock("BT-R1"));
        assertEquals(Integer.MAX_VALUE, stock("BT-R2"));
        assertEquals(0, movementCount("PN:" + draft.getPhieuNhap().getMaPhieuNhap() + ":BT-R1"));
        assertEquals(TrangThaiPhieuNhap.NHAP,
                receipts.detail(draft.getPhieuNhap().getMaPhieuNhap()).getPhieuNhap().getTrangThai());
    }

    @Test
    public void onlyManagerCanManageSuppliersAndReceipts() throws Exception {
        KetNoiMayChu salesSocket = new KetNoiMayChu("127.0.0.1", server.getPort());
        PhienDangNhap session = new PhienDangNhap();
        KetQuaDangNhap login = new KetQuaDangNhap();
        login.setSessionToken(sessions.create("sales", hash));
        session.setLogin(login);
        GoiDichVuNhaCungCap salesSuppliers = new GoiDichVuNhaCungCap(salesSocket, session);
        GoiDichVuPhieuNhap salesReceipts = new GoiDichVuPhieuNhap(salesSocket, session);
        try {
            YeuCauTraCuuNhaCungCap search = new YeuCauTraCuuNhaCungCap();
            error(MaLoi.FORBIDDEN, () -> salesSuppliers.search(search));
            YeuCauTaoPhieuNhap create = new YeuCauTaoPhieuNhap();
            create.setMaPhieuNhap("PN_DENY");
            create.setMaNCC("NCC_DENY");
            create.setOperationId(uuid());
            error(MaLoi.FORBIDDEN, () -> salesReceipts.create(create));
        } finally {
            salesSocket.close();
        }
    }

    @Test
    public void vendorProfileAndStaleEditAreValidated() throws Exception {
        String code = "NCC" + ids.incrementAndGet();
        DuLieuNhaCungCap saved = suppliers.create(supplier(code, "  Nhà cung cấp Café  "));
        assertEquals("Nhà cung cấp Café", saved.getTenNCC());
        DuLieuNhaCungCap changed = supplier(code, "Nhà cung cấp mới");
        changed.setPhienBan(saved.getPhienBan());
        YeuCauLuuNhaCungCap update = new YeuCauLuuNhaCungCap();
        update.setExpected(saved);
        update.setValue(changed);
        DuLieuNhaCungCap after = suppliers.update(update);
        assertEquals("Nhà cung cấp mới", after.getTenNCC());
        error(MaLoi.VERSION_CONFLICT, () -> suppliers.update(update));
        YeuCauTrangThaiNhaCungCap status = new YeuCauTrangThaiNhaCungCap();
        status.setMaNCC(code);
        status.setExpectedVersion(after.getPhienBan());
        DuLieuNhaCungCap inactive = suppliers.setActive(status, false);
        assertFalse(inactive.getTrangThai());
        error(MaLoi.VALIDATION_ERROR, () -> createWithSupplier(code));
    }

    @Test
    public void concurrentSameConfirmationIsAppliedOnce() throws Exception {
        String vendor = makeSupplier();
        DuLieuDayDuPhieuNhap draft = createReceipt(vendor);
        draft = receipts.addLine(line(draft, "BT-R1", 1, "10.00", uuid()));
        YeuCauXacNhanPhieuNhap same = confirm(draft, uuid());
        KetNoiMayChu secondSocket = new KetNoiMayChu("127.0.0.1", server.getPort());
        PhienDangNhap session = new PhienDangNhap();
        KetQuaDangNhap login = new KetQuaDangNhap();
        login.setSessionToken(sessions.create("manager", hash));
        session.setLogin(login);
        GoiDichVuPhieuNhap second = new GoiDichVuPhieuNhap(secondSocket, session);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        try {
            Future<DuLieuDayDuPhieuNhap> one = workers.submit(() -> {
                gate.await();
                return receipts.confirm(same);
            });
            Future<DuLieuDayDuPhieuNhap> two = workers.submit(() -> {
                gate.await();
                return second.confirm(same);
            });
            gate.countDown();
            one.get(10, TimeUnit.SECONDS);
            two.get(10, TimeUnit.SECONDS);
            assertEquals(6, stock("BT-R1"));
            assertEquals(1, movementCount("PN:" + draft.getPhieuNhap().getMaPhieuNhap() + ":BT-R1"));
        } finally {
            workers.shutdownNow();
            secondSocket.close();
        }
    }

    private DuLieuNhaCungCap createWithSupplier(String code) throws Exception {
        YeuCauTaoPhieuNhap r = new YeuCauTaoPhieuNhap();
        r.setMaPhieuNhap("PN" + ids.incrementAndGet());
        r.setMaNCC(code);
        r.setOperationId(uuid());
        return receipts.create(r).getPhieuNhap() == null ? null
                : suppliers.search(new YeuCauTraCuuNhaCungCap()).getItems().get(0);
    }

    private String makeSupplier() throws Exception {
        String id = "NCC" + ids.incrementAndGet();
        suppliers.create(supplier(id, "Nhà cung cấp"));
        return id;
    }

    private DuLieuDayDuPhieuNhap createReceipt(String supplier) throws Exception {
        YeuCauTaoPhieuNhap r = new YeuCauTaoPhieuNhap();
        r.setMaPhieuNhap("PN" + ids.incrementAndGet());
        r.setMaNCC(supplier);
        r.setOperationId(uuid());
        return receipts.create(r);
    }

    private static DuLieuNhaCungCap supplier(String id, String name) {
        DuLieuNhaCungCap dto = new DuLieuNhaCungCap();
        dto.setMaNCC(id);
        dto.setTenNCC(name);
        dto.setSdt("0900000000");
        dto.setTrangThai(true);
        return dto;
    }

    private static YeuCauDongPhieuNhap line(DuLieuDayDuPhieuNhap d, String variant, int qty, String price,
            String op) {
        YeuCauDongPhieuNhap r = new YeuCauDongPhieuNhap();
        r.setMaPhieuNhap(d.getPhieuNhap().getMaPhieuNhap());
        r.setPhienBan(d.getPhieuNhap().getPhienBan());
        r.setMaBienThe(variant);
        r.setSoLuong(qty);
        r.setDonGiaNhap(new BigDecimal(price));
        r.setOperationId(op);
        return r;
    }

    private static YeuCauXacNhanPhieuNhap confirm(DuLieuDayDuPhieuNhap d, String op) {
        YeuCauXacNhanPhieuNhap r = new YeuCauXacNhanPhieuNhap();
        r.setMaPhieuNhap(d.getPhieuNhap().getMaPhieuNhap());
        r.setPhienBan(d.getPhieuNhap().getPhienBan());
        r.setOperationId(op);
        return r;
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }

    private int stock(String id) {
        return jpa.transaction(em -> em.find(BienThe.class, id).getSoLuongTon());
    }

    private long movementCount(String id) {
        return jpa.transaction(
                em -> em.createQuery("select count(l) from LichSuKho l where l.operationId=:id", Long.class)
                        .setParameter("id", id).getSingleResult());
    }

    private LichSuKho movement(String id) {
        return jpa.transaction(
                em -> em.createQuery("select l from LichSuKho l where l.operationId=:id", LichSuKho.class)
                        .setParameter("id", id).getSingleResult());
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
}
