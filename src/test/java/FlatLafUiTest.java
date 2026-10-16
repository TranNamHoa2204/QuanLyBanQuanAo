import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import client.ui.*;
import common.dto.*;
import common.enums.VaiTro;
import client.api.*;
import client.network.SocketClient;
import client.session.ClientSession;

/** View thực, không mock ảnh: render headless bằng FlatLaf và chạy hành động điều hướng. */
public class FlatLafUiTest {
    @Test public void loginAndDashboardRenderAndNavigate() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                UiTheme.install(); assertEquals("FlatLaf Light", UIManager.getLookAndFeel().getName());
                File dir = new File(".tmp/flatlaf-ui"); assertTrue(dir.isDirectory() || dir.mkdirs());
                render(new DangNhapPanel(), new File(dir, "login.png"), 920, 560);
                LoginResponse login = new LoginResponse(); login.setHoTen("Nguyễn Minh Anh"); login.setVaiTro(VaiTro.NHAN_VIEN_QUAN_LY);
                AtomicInteger products = new AtomicInteger(), sales = new AtomicInteger(), management = new AtomicInteger(), history = new AtomicInteger();
                MainShellPanel shell = new MainShellPanel(login, products::incrementAndGet, sales::incrementAndGet, () -> {}, () -> {}, b -> {}, management::incrementAndGet, history::incrementAndGet);
                render(shell, new File(dir, "dashboard.png"), 1320, 860);
                findButton(shell, "Sản phẩm").doClick(); assertEquals(1, products.get());
                findButton(shell, "Bán hàng & hóa đơn").doClick(); assertEquals(1, sales.get());
                findButton(shell, "Danh mục & sản phẩm").doClick(); assertEquals(1, management.get());
                findButton(shell, "Lịch sử kho").doClick(); assertEquals(1, history.get());
                assertFalse(findButton(shell, "Nhân viên  ·  Sắp có").isEnabled());
                login.setVaiTro(VaiTro.NHAN_VIEN_BAN_HANG);
                MainShellPanel staff = new MainShellPanel(login, () -> {}, () -> {}, () -> {}, () -> {}, b -> {});
                assertNull(findButton(staff, "Nhân viên  ·  Sắp có"));
                assertNull(findButton(staff, "Danh mục & sản phẩm"));
                JPanel view = new JPanel(); shell.showPage("Sản phẩm", view); assertTrue(SwingUtilities.isDescendingFrom(view, shell));
                NhanVienProfilePanel profile = new NhanVienProfilePanel(); NhanVienDTO e = new NhanVienDTO();
                e.setMaNV("NV01"); e.setHoTen(login.getHoTen()); profile.showEmployee(e);
                render(profile, new File(dir, "profile.png"), 700, 490);
                render(new DoiMatKhauPanel(), new File(dir, "password.png"), 540, 550);
                SocketClient socket = new SocketClient("127.0.0.1", 1); ClientSession session = new ClientSession();
                BanHangPanel salesView = new BanHangPanel(new HoaDonApi(socket, session), new SanPhamApi(socket, session));
                shell.showPage("Bán hàng & hóa đơn", salesView);
                render(shell, new File(dir, "sales-small-screen.png"), 1280, 720);
                assertTrue(salesView.getHeight() >= 780);
                JScrollPane scroller = (JScrollPane)salesView.getParent().getParent();
                assertTrue(scroller.getVerticalScrollBar().isVisible());
                socket.close();
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }
    private static JButton findButton(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton && text.equals(((JButton)c).getText())) { return (JButton)c; }
            if (c instanceof Container) { JButton b = findButton((Container)c, text); if (b != null) { return b; } }
        } return null;
    }
    private static void layout(Container root) {
        root.doLayout(); for (Component c : root.getComponents()) { if (c instanceof Container) { layout((Container)c); } }
    }
    private static void render(JPanel panel, File file, int width, int height) throws Exception {
        panel.setSize(width, height); layout(panel); layout(panel);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        try { panel.printAll(g); } finally { g.dispose(); }
        ImageIO.write(image, "png", file);
    }
}
