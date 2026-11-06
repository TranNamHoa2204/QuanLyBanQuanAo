package kiemthu;

import static org.junit.Assert.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.Test;

import client.api.GoiDichVuHoaDon;
import client.api.GoiDichVuKhachHang;
import client.api.GoiDichVuSanPham;
import client.gui.banhang.KhungBanHang;
import client.gui.components.ChuDeGiaoDien;
import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.hoadon.DuLieuChiTietHoaDon;
import common.dto.hoadon.DuLieuDayDuHoaDon;
import common.dto.hoadon.DuLieuHoaDon;
import common.enums.PhuongThucThanhToan;
import common.enums.TrangThaiHoaDon;

public class KiemThuGiaoDienHoaDon {
    @Test
    public void renderInvoiceAndReadOnlyPaidState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                ChuDeGiaoDien.install();
                KetNoiMayChu socket = new KetNoiMayChu("127.0.0.1", 1);
                PhienDangNhap session = new PhienDangNhap();
                KhungBanHang panel = new KhungBanHang(new GoiDichVuHoaDon(socket, session),
                        new GoiDichVuSanPham(socket, session), new GoiDichVuKhachHang(socket, session));
                assertNull(button(panel, "Lập hóa đơn"));
                assertTrue(button(panel, "Đơn mới").isVisible());
                assertFalse(button(panel, "+ Thêm vào giỏ").isEnabled());
                DuLieuDayDuHoaDon d = new DuLieuDayDuHoaDon();
                DuLieuHoaDon h = new DuLieuHoaDon();
                d.setHoaDon(h);
                h.setMaHD("HD_DEMO");
                h.setMaNV("NV01");
                h.setMaThue("T_DEMO");
                h.setPhienBan(2L);
                h.setTamTinh(new BigDecimal("298000"));
                h.setTienGiam(BigDecimal.ZERO);
                h.setPhanTramThue(BigDecimal.ZERO);
                h.setTienThue(BigDecimal.ZERO);
                h.setTongTien(h.getTamTinh());
                h.setTrangThai(TrangThaiHoaDon.NHAP);
                DuLieuChiTietHoaDon line = new DuLieuChiTietHoaDon();
                line.setMaBienThe("BT1001");
                line.setTenSP("Áo polo cơ bản");
                line.setMauSac("Đỏ");
                line.setKichCo("M");
                line.setSoLuong(2);
                line.setDonGia(new BigDecimal("149000"));
                line.setThanhTien(h.getTamTinh());
                d.getChiTiet().add(line);
                panel.showInvoice(d);
                panel.setSize(1100, 780);
                layout(panel);
                assertTrue(button(panel, "Đơn mới").isVisible());
                assertTrue(button(panel, "Thanh toán (F9)").isEnabled());
                assertTrue(button(panel, "Tra cứu").isEnabled());
                assertTrue(button(panel, "+ Khách mới").isEnabled());
                assertTrue(labelTextContains(panel, "Tiền khách đưa"));
                assertNotNull(table(panel, "Mã SP"));
                JTable table = table(panel, "Biến thể");
                assertNotNull(table);
                assertEquals(1, table.getRowCount());
                assertFalse(table.isCellEditable(0, 4));
                assertFalse(button(panel, "Xóa khỏi giỏ").isEnabled());
                table.setRowSelectionInterval(0, 0);
                assertTrue(button(panel, "+").isEnabled());
                assertTrue(button(panel, "−").isEnabled());
                assertTrue(button(panel, "Xóa khỏi giỏ").isEnabled());
                BufferedImage image = new BufferedImage(1100, 780, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                panel.printAll(g);
                g.dispose();
                File dir = new File(".tmp/invoice-ui");
                assertTrue(dir.isDirectory() || dir.mkdirs());
                ImageIO.write(image, "png", new File(dir, "invoice-preview.png"));
                h.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN);
                panel.showInvoice(d);
                assertFalse(button(panel, "Thanh toán (F9)").isEnabled());
                assertFalse(button(panel, "Xóa khỏi giỏ").isEnabled());
                assertFalse(button(panel, "Tra cứu").isEnabled());
                assertFalse(button(panel, "+ Khách mới").isEnabled());
                assertTrue(labelTextContains(panel, "chưa có dữ liệu"));
                h.setPhuongThucThanhToan(PhuongThucThanhToan.TIEN_MAT);
                h.setTienKhachDua(new BigDecimal("300000"));
                h.setTienThua(new BigDecimal("2000"));
                panel.showInvoice(d);
                assertTrue(labelTextContains(panel, "Tiền thừa"));
                h.setPhuongThucThanhToan(PhuongThucThanhToan.CHUYEN_KHOAN);
                h.setTienKhachDua(null);
                h.setTienThua(null);
                h.setNhanVienXacNhanChuyenKhoan(Boolean.TRUE);
                panel.showInvoice(d);
                assertTrue(labelTextContains(panel, "không xác minh ngân hàng"));
                assertFalse(labelTextContains(panel, "đã xác minh ngân hàng"));
                socket.close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component child : c.getComponents()) {
            if (child instanceof Container) {
                layout((Container) child);
            }
        }
    }

    private static JButton button(Container c, String text) {
        for (Component child : c.getComponents()) {
            if (child instanceof JButton && text.equals(((JButton) child).getText())) {
                return (JButton) child;
            }
            if (child instanceof Container) {
                JButton b = button((Container) child, text);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    private static JTable table(Container c, String firstColumn) {
        for (Component child : c.getComponents()) {
            if (child instanceof JTable && firstColumn.equals(((JTable) child).getColumnName(0))) {
                return (JTable) child;
            }
            if (child instanceof Container) {
                JTable t = table((Container) child, firstColumn);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    private static boolean labelTextContains(Container c, String value) {
        for (Component child : c.getComponents()) {
            if (child instanceof JLabel && ((JLabel) child).getText().contains(value)) {
                return true;
            }
            if (child instanceof Container && labelTextContains((Container) child, value)) {
                return true;
            }
        }
        return false;
    }
}
