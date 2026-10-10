import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import client.api.*;
import client.network.SocketClient;
import client.session.ClientSession;
import client.ui.BanHangPanel;
import common.dto.*;
import common.enums.TrangThaiHoaDon;

public class InvoiceUiTest {
    @Test public void renderInvoiceAndReadOnlyPaidState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                client.ui.UiTheme.install();
                SocketClient socket = new SocketClient("127.0.0.1", 1); ClientSession session = new ClientSession();
                BanHangPanel panel = new BanHangPanel(new HoaDonApi(socket, session), new SanPhamApi(socket, session));
                HoaDonDetailDTO d = new HoaDonDetailDTO(); HoaDonDTO h = new HoaDonDTO(); d.setHoaDon(h);
                h.setMaHD("HD_DEMO"); h.setMaNV("NV01"); h.setMaThue("T_DEMO"); h.setPhienBan(2L);
                h.setTamTinh(new BigDecimal("298000")); h.setTienGiam(BigDecimal.ZERO);
                h.setPhanTramThue(BigDecimal.ZERO);
                h.setTienThue(BigDecimal.ZERO); h.setTongTien(h.getTamTinh()); h.setTrangThai(TrangThaiHoaDon.NHAP);
                ChiTietHoaDonDTO line = new ChiTietHoaDonDTO(); line.setMaBienThe("BT1001");
                line.setTenSP("Áo polo cơ bản"); line.setMauSac("Đỏ"); line.setKichCo("M"); line.setSoLuong(2);
                line.setDonGia(new BigDecimal("149000")); line.setThanhTien(h.getTamTinh()); d.getChiTiet().add(line);
                panel.showInvoice(d); panel.setSize(1100, 780); layout(panel);
                assertTrue(button(panel, "Thanh toán").isEnabled());
                JTable table = table(panel); assertEquals(1, table.getRowCount()); assertFalse(table.isCellEditable(0, 4));
                BufferedImage image = new BufferedImage(1100, 780, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                panel.printAll(g); g.dispose();
                File dir = new File(".tmp/invoice-ui"); assertTrue(dir.isDirectory() || dir.mkdirs());
                ImageIO.write(image, "png", new File(dir, "invoice-preview.png"));
                h.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN); panel.showInvoice(d);
                assertFalse(button(panel, "Thanh toán").isEnabled()); assertFalse(button(panel, "Bỏ hàng").isEnabled());
                socket.close();
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }
    private static void layout(Container c) { c.doLayout(); for (Component child : c.getComponents()) { if (child instanceof Container) { layout((Container)child); } } }
    private static JButton button(Container c, String text) {
        for (Component child : c.getComponents()) {
            if (child instanceof JButton && text.equals(((JButton)child).getText())) { return (JButton)child; }
            if (child instanceof Container) { JButton b = button((Container)child, text); if (b != null) { return b; } }
        } return null;
    }
    private static JTable table(Container c) {
        for (Component child : c.getComponents()) {
            if (child instanceof JTable) { return (JTable)child; }
            if (child instanceof Container) { JTable t = table((Container)child); if (t != null) { return t; } }
        } return null;
    }
}
