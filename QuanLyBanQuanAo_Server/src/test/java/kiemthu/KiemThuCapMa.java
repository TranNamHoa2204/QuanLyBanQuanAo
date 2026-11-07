package kiemthu;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import client.api.GoiDichVuXacThuc;
import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import client.utils.SinhMaTuDong;
import common.dto.danhmuc.YeuCauLuuDanhMuc;
import common.dto.hoadon.YeuCauTaoHoaDon;
import common.dto.nhacungcap.DuLieuNhaCungCap;
import common.dto.nhanvien.DuLieuNhanVien;
import common.dto.nhanvien.YeuCauTaoNhanVien;
import common.dto.phieunhap.YeuCauTaoPhieuNhap;
import common.dto.sanpham.DuLieuSanPham;
import common.dto.sanpham.YeuCauLuuSanPham;
import common.dto.taikhoan.KetQuaDangNhap;
import common.enums.LoaiMaTuDong;
import common.enums.VaiTro;
import common.protocol.MaLoi;
import server.dispatcher.DieuPhoiYeuCau;
import server.entities.Thue;
import server.network.MayChuTcp;
import server.security.BoBamMatKhauPbkdf2;
import server.services.*;
import server.session.QuanLyPhienDangNhap;
import server.utils.QuanLyJpa;

public class KiemThuCapMa {
    @Test
    public void codesWorkThroughApisAndRemainUniqueAcrossClients() throws Exception {
        try (QuanLyJpa jpa = new QuanLyJpa(HoTroKiemThuDanhMuc.memoryProperties("code_test"))) {
            BoBamMatKhauPbkdf2 hasher = new BoBamMatKhauPbkdf2();
            String hash = hasher.hash("CodePass123!".toCharArray());
            jpa.transaction(em -> {
                HoTroKiemThuDanhMuc.user(em, "manager", "NV-OLD", VaiTro.NHAN_VIEN_QUAN_LY, hash);
                HoTroKiemThuDanhMuc.user(em, "sales", "NV-SALES", VaiTro.NHAN_VIEN_BAN_HANG, hash);
                for (LoaiMaTuDong type : LoaiMaTuDong.values()) {
                    em.createNativeQuery("CREATE SEQUENCE dbo.Seq_" + type.getPrefix() + " START WITH 1").executeUpdate();
                }
                Thue tax = new Thue();
                tax.setMaThue("THUE001");
                tax.setTenThue("Thuế suất 0%");
                tax.setPhanTram(BigDecimal.ZERO);
                em.persist(tax);
                return null;
            });
            QuanLyPhienDangNhap sessions = new QuanLyPhienDangNhap();
            DichVuXacThuc auth = new DichVuXacThuc(jpa, sessions, hasher);
            DichVuCapMa codes = new DichVuCapMa(jpa, auth);
            DieuPhoiYeuCau dispatcher = new DieuPhoiYeuCau(auth, new DichVuSanPham(jpa, auth),
                    new DichVuHoaDon(jpa, auth), new DichVuNhanVien(jpa, auth, sessions, hasher),
                    new DichVuKhachHang(jpa, auth), new DichVuKhuyenMai(jpa, auth), null, null,
                    new DichVuNhaCungCap(jpa, auth), new DichVuPhieuNhap(jpa, auth), codes);
            String token = sessions.create("manager", hash);
            try (MayChuTcp server = new MayChuTcp("127.0.0.1", 0, dispatcher)) {
                server.start();
                try (KetNoiMayChu socket = new KetNoiMayChu("127.0.0.1", server.getPort())) {
                    GoiDichVuXacThuc api = new GoiDichVuXacThuc(socket, session(token));
                    DuLieuNhaCungCap supplier = new DuLieuNhaCungCap();
                    supplier.setTenNCC("Nhà cung cấp");
                    supplier.setTrangThai(true);
                    assertEquals("NCC001", api.suppliers().create(supplier).getMaNCC());
                    DuLieuNhanVien employee = new DuLieuNhanVien();
                    employee.setHoTen("Nhân viên mới");
                    employee.setSdt("0901234567");
                    employee.setTrangThai(true);
                    YeuCauTaoNhanVien request = new YeuCauTaoNhanVien();
                    request.setNhanVien(employee);
                    assertEquals("NV001", api.employees().create(request).getNhanVien().getMaNV());

                    YeuCauLuuDanhMuc category = new YeuCauLuuDanhMuc();
                    category.setName("Áo");
                    assertEquals("LSP001", api.products().saveCategory(category, true));
                    YeuCauLuuDanhMuc brand = new YeuCauLuuDanhMuc();
                    brand.setName("Thương hiệu");
                    assertEquals("TH001", api.products().saveBrand(brand, true));
                    DuLieuSanPham product = new DuLieuSanPham();
                    product.setTenSP("Áo polo");
                    product.setMaLoai(category.getCode());
                    product.setMaThuongHieu(brand.getCode());
                    product.setGiaBan(new BigDecimal("150000"));
                    product.setTrangThai(true);
                    YeuCauLuuSanPham save = new YeuCauLuuSanPham();
                    save.setValue(product);
                    assertEquals("SP001", api.products().saveProduct(save, 0));

                    YeuCauTaoPhieuNhap receipt = new YeuCauTaoPhieuNhap();
                    receipt.setMaNCC(supplier.getMaNCC());
                    receipt.setOperationId(UUID.randomUUID().toString());
                    assertEquals("PHIEUNHAP001", api.receipts().create(receipt).getPhieuNhap().getMaPhieuNhap());
                    assertEquals("PHIEUNHAP001", api.receipts().create(receipt).getPhieuNhap().getMaPhieuNhap());
                    YeuCauTaoHoaDon invoice = new YeuCauTaoHoaDon();
                    invoice.setMaThue("THUE001");
                    assertEquals("HD001", api.invoices().create(invoice).getHoaDon().getMaHD());
                    assertEquals("HD001", api.invoices().create(invoice).getHoaDon().getMaHD());
                }
                ExecutorService workers = Executors.newFixedThreadPool(4);
                try {
                    List<Callable<String>> tasks = new ArrayList<>();
                    for (int i = 0; i < 16; i++) {
                        tasks.add(() -> {
                            try (KetNoiMayChu socket = new KetNoiMayChu("127.0.0.1", server.getPort())) {
                                return SinhMaTuDong.cap(null, LoaiMaTuDong.NHAN_VIEN, socket, session(token));
                            }
                        });
                    }
                    Set<String> allocated = new HashSet<>();
                    for (Future<String> result : workers.invokeAll(tasks)) {
                        assertTrue(allocated.add(result.get()));
                    }
                    assertTrue(allocated.contains("NV002"));
                    assertTrue(allocated.contains("NV017"));
                    assertEquals("NV018", new DichVuCapMa(jpa, auth).next(token, LoaiMaTuDong.NHAN_VIEN));
                } finally {
                    workers.shutdownNow();
                }
                try {
                    codes.next(sessions.create("sales", hash), LoaiMaTuDong.NHAN_VIEN);
                    fail("Sales account cannot allocate an employee code");
                } catch (NgoaiLeNghiepVu forbidden) {
                    assertEquals(MaLoi.FORBIDDEN, forbidden.getCode());
                }
            }
        }
    }

    private static PhienDangNhap session(String token) {
        KetQuaDangNhap login = new KetQuaDangNhap();
        login.setSessionToken(token);
        PhienDangNhap session = new PhienDangNhap();
        session.setLogin(login);
        return session;
    }
}
