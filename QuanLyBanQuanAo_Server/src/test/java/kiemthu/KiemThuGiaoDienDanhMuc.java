package kiemthu;

import static org.junit.Assert.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.Test;

import client.api.GoiDichVuLichSuKho;
import client.api.GoiDichVuSanPham;
import client.controllers.DieuKhienQuanLySanPham;
import client.gui.components.ChuDeGiaoDien;
import client.gui.danhmuc.KhungDanhMuc;
import client.gui.hethong.KhungUngDung;
import client.gui.kho.KhungLichSuKho;
import client.gui.sanpham.KhungChinhSuaSanPham;
import client.gui.sanpham.KhungQuanLyBienThe;
import client.gui.sanpham.KhungQuanLySanPham;
import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.danhmuc.DuLieuLoaiSanPham;
import common.dto.danhmuc.DuLieuThuongHieu;
import common.dto.sanpham.DuLieuBienThe;
import common.dto.sanpham.DuLieuDayDuSanPham;
import common.dto.sanpham.DuLieuSanPham;
import common.dto.sanpham.KetQuaTraCuuSanPham;
import common.dto.taikhoan.KetQuaDangNhap;
import common.enums.VaiTro;

public class KiemThuGiaoDienDanhMuc {
    @Test
    public void managementPanelsAndEditorRenderWithoutNetwork() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                ChuDeGiaoDien.install();
                File dir = new File(".tmp/catalog-management-ui");
                assertTrue(dir.isDirectory() || dir.mkdirs());
                KetNoiMayChu socket = new KetNoiMayChu("127.0.0.1", 1);
                DieuKhienQuanLySanPham controller = new DieuKhienQuanLySanPham(
                        new GoiDichVuSanPham(socket, new PhienDangNhap()));
                DuLieuSanPham p = new DuLieuSanPham();
                p.setMaSP("SP1001");
                p.setTenSP("Áo phông nữ basic cổ tim");
                p.setMaLoai("L1");
                p.setMaThuongHieu("TH1");
                p.setTenLoai("Áo phông");
                p.setTenThuongHieu("Canifa");
                p.setGiaBan(new BigDecimal("149000.50"));
                p.setTongTon(65);
                KetQuaTraCuuSanPham result = new KetQuaTraCuuSanPham();
                result.setTotal(1);
                result.setPageSize(20);
                result.getItems().add(p);
                KhungQuanLySanPham view = new KhungQuanLySanPham(controller);
                view.showProducts(result);
                assertEquals(4, tabs(view).getTabCount());
                assertEquals(1, table(view).getRowCount());
                assertFalse(table(view).isCellEditable(0, 4));
                KetQuaDangNhap login = new KetQuaDangNhap();
                login.setHoTen("Nguyễn Minh Anh");
                login.setVaiTro(VaiTro.NHAN_VIEN_QUAN_LY);
                KhungUngDung shell = new KhungUngDung(login, () -> {
                }, () -> {
                }, () -> {
                }, () -> {
                }, b -> {
                }, () -> {
                });
                shell.showPage("Danh mục & sản phẩm", view);
                render(shell, new File(dir, "management.png"), 1360, 900);
                KhungDanhMuc catalog = new KhungDanhMuc(controller, false);
                catalog.showRows(Arrays.asList(new String[] { "L1", "Áo phông" },
                        new String[] { "L2", "Quần shorts" }));
                render(catalog, new File(dir, "categories.png"), 900, 560);
                DuLieuDayDuSanPham detail = new DuLieuDayDuSanPham();
                detail.setSanPham(p);
                DuLieuBienThe b = new DuLieuBienThe();
                b.setMaBienThe("BT000001");
                b.setMaSP(p.getMaSP());
                b.setMauSac("Đỏ");
                b.setKichCo("M");
                b.setSoLuongTon(10);
                b.setPhienBan(3L);
                detail.getBienThe().add(b);
                KhungQuanLyBienThe variants = new KhungQuanLyBienThe(controller);
                variants.showProduct(detail);
                render(variants, new File(dir, "variants.png"), 1000, 560);
                KhungLichSuKho history = new KhungLichSuKho(
                        new GoiDichVuLichSuKho(socket, new PhienDangNhap()));
                render(history, new File(dir, "stock-history.png"), 1000, 560);
                DuLieuLoaiSanPham l = new DuLieuLoaiSanPham();
                l.setMaLoai("L1");
                l.setTenLoai("Áo phông");
                DuLieuThuongHieu brand = new DuLieuThuongHieu();
                brand.setMaThuongHieu("TH1");
                brand.setTenThuongHieu("Canifa");
                KhungChinhSuaSanPham editor = new KhungChinhSuaSanPham(p, Collections.singletonList(l),
                        Collections.singletonList(brand));
                assertSame(p, editor.request().getExpected());
                assertNotSame(p, editor.request().getValue());
                assertEquals(p.getGiaBan(), editor.request().getValue().getGiaBan());
                render(editor, new File(dir, "product-editor.png"), 820, 420);
                socket.close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private static JTabbedPane tabs(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTabbedPane) {
                return (JTabbedPane) c;
            }
            if (c instanceof Container) {
                JTabbedPane t = tabs((Container) c);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    private static JTable table(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTable) {
                return (JTable) c;
            }
            if (c instanceof Container) {
                JTable t = table((Container) c);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component child : c.getComponents()) {
            if (child instanceof Container) {
                layout((Container) child);
            }
        }
    }

    private static void render(JPanel p, File file, int w, int h) throws Exception {
        p.setSize(w, h);
        layout(p);
        layout(p);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        try {
            p.printAll(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", file);
    }
}
