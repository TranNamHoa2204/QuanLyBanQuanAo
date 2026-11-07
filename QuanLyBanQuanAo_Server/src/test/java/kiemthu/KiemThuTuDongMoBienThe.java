package kiemthu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.junit.Test;
import client.api.GoiDichVuSanPham;
import client.controllers.DieuKhienQuanLySanPham;
import client.gui.components.ChuDeGiaoDien;
import client.gui.sanpham.KhungQuanLyBienThe;
import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.taikhoan.KetQuaDangNhap;
import common.enums.VaiTro;
import server.dispatcher.DieuPhoiYeuCau;
import server.entities.BienThe;
import server.entities.LoaiSanPham;
import server.entities.SanPham;
import server.entities.ThuongHieu;
import server.network.MayChuTcp;
import server.security.BoBamMatKhauPbkdf2;
import server.services.DichVuSanPham;
import server.services.DichVuXacThuc;
import server.session.QuanLyPhienDangNhap;
import server.utils.QuanLyJpa;

public class KiemThuTuDongMoBienThe {
    @Test
    public void openingShowsFirstProductsVariantsAndAllowsChangingProduct() throws Exception {
        try (QuanLyJpa jpa = new QuanLyJpa(HoTroKiemThuDanhMuc.memoryProperties("auto_variants"))) {
            String hash = new BoBamMatKhauPbkdf2().hash("test-password".toCharArray());
            jpa.transaction(em -> {
                HoTroKiemThuDanhMuc.user(em, "manager", "NV001", VaiTro.NHAN_VIEN_QUAN_LY, hash);
                LoaiSanPham category = new LoaiSanPham(); category.setMaLoai("LSP001"); category.setTenLoai("Áo"); em.persist(category);
                ThuongHieu brand = new ThuongHieu(); brand.setMaThuongHieu("TH001"); brand.setTenThuongHieu("Thời trang"); em.persist(brand);
                for (int i = 1; i <= 2; i++) {
                    SanPham product = new SanPham(); product.setMaSP("SP00" + i); product.setTenSP("Áo " + i);
                    product.setLoaiSanPham(category); product.setThuongHieu(brand); product.setGiaBan(new BigDecimal("150000")); em.persist(product);
                    BienThe variant = new BienThe(); variant.setMaBienThe("BT00" + i); variant.setSanPham(product);
                    variant.setMauSac("Xanh"); variant.setKichCo("M"); variant.setSoLuongTon(20); em.persist(variant);
                }
                return null;
            });
            QuanLyPhienDangNhap sessions = new QuanLyPhienDangNhap();
            DichVuXacThuc auth = new DichVuXacThuc(jpa, sessions, new BoBamMatKhauPbkdf2());
            try (MayChuTcp server = new MayChuTcp("127.0.0.1", 0, new DieuPhoiYeuCau(auth, new DichVuSanPham(jpa, auth)))) {
                server.start();
                try (KetNoiMayChu socket = new KetNoiMayChu("127.0.0.1", server.getPort())) {
                    KetQuaDangNhap login = new KetQuaDangNhap(); login.setSessionToken(sessions.create("manager", hash));
                    PhienDangNhap session = new PhienDangNhap(); session.setLogin(login);
                    AtomicReference<KhungQuanLyBienThe> view = new AtomicReference<>();
                    CountDownLatch first = new CountDownLatch(1), second = new CountDownLatch(1);
                    SwingUtilities.invokeAndWait(() -> {
                        ChuDeGiaoDien.install();
                        KhungQuanLyBienThe panel = new KhungQuanLyBienThe(new DieuKhienQuanLySanPham(new GoiDichVuSanPham(socket, session)));
                        view.set(panel);
                        JTable table = find(panel, JTable.class);
                        table.getModel().addTableModelListener(e -> {
                            if (table.getModel().getRowCount() == 1) {
                                if ("BT001".equals(table.getModel().getValueAt(0, 0))) first.countDown();
                                if ("BT002".equals(table.getModel().getValueAt(0, 0))) second.countDown();
                            }
                        });
                        panel.open();
                    });
                    assertTrue("Opening must show a product without an extra selection", first.await(10, TimeUnit.SECONDS));
                    SwingUtilities.invokeAndWait(() -> {
                        JComboBox<?> picker = find(view.get(), JComboBox.class);
                        assertEquals(3, picker.getItemCount());
                        picker.setSelectedIndex(2);
                    });
                    assertTrue("Selecting another product must update variants", second.await(10, TimeUnit.SECONDS));
                }
            }
        }
    }

    private static <T extends Component> T find(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container) {
                T found = find((Container) child, type);
                if (found != null) return found;
            }
        }
        return null;
    }

}
