import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import client.ui.*;
import client.api.ManagementApi;
import client.api.ManagementApi.Kind;
import client.network.SocketClient;
import client.session.ClientSession;
import common.dto.*;
import common.enums.VaiTro;
import common.protocol.JsonCodec;

public class StoreManagementUiTest {
    @Test public void editorsRoundTripAndRenderWithoutNetwork() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                UiTheme.install();
                for (Kind kind : Kind.values()) {
                    ManagementEditorPanel editor = new ManagementEditorPanel(kind, JsonCodec.mapper().valueToTree(fixture(kind)), false);
                    assertEquals(JsonCodec.mapper().valueToTree(fixture(kind)), editor.read());
                    JScrollPane scroll = new JScrollPane(editor); render(scroll, kind.name().toLowerCase() + "-editor.png", 700, 530);
                    if (kind == Kind.EMPLOYEE) { assertFalse(findField(editor, "sales.demo").isEnabled()); assertTrue(scroll.getVerticalScrollBar().isVisible()); }
                }
                ManagementEditorPanel create = new ManagementEditorPanel(Kind.EMPLOYEE, null, true);
                assertNull(create.initialPassword()); assertEquals("NHAN_VIEN_BAN_HANG", create.read().path("vaiTro").asText());
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
    @Test public void tablesAndReportRenderWithRealComponents() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try (SocketClient socket = new SocketClient("127.0.0.1", 1)) {
                UiTheme.install(); ManagementApi api = new ManagementApi(socket, new ClientSession());
                for (Kind kind : Kind.values()) {
                    ManagementPanel panel = new ManagementPanel(api, kind); ManagementSearchResult r = new ManagementSearchResult();
                    r.setTotal(1); r.getItems().add(JsonCodec.mapper().valueToTree(fixture(kind))); panel.showResult(r);
                    JTable table = findTable(panel); assertEquals(1, table.getRowCount()); assertFalse(table.isCellEditable(0, 0));
                    render(panel, kind.name().toLowerCase() + ".png", 1000, 660);
                }
                RevenueResult r = new RevenueResult(); r.setSoHoaDon(3); r.setTamTinh(new BigDecimal("1000000"));
                r.setTienGiam(new BigDecimal("100000")); r.setTienThue(new BigDecimal("90000")); r.setTongTien(new BigDecimal("990000"));
                RevenueResult.Row row = new RevenueResult.Row(); row.setCode("SP01"); row.setLabel("Áo sơ mi nam"); row.setQuantity(5); row.setAmount(new BigDecimal("1000000")); r.getItems().add(row);
                ThongKePanel panel = new ThongKePanel(api); panel.showResult(r, true); render(panel, "revenue-products.png", 1000, 660);
                assertEquals(4, findTable(panel).getColumnCount()); assertTrue(containsLabel(panel, "chưa phân bổ"));
                row.setLabel("2026-10-11"); panel.showResult(r, false); render(panel, "revenue-time.png", 1000, 660);
                assertEquals(3, findTable(panel).getColumnCount());
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
    @Test public void managerNavigationEnabledAndSalesCannotSeeAdministrativeEntries() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                UiTheme.install(); LoginResponse login = new LoginResponse(); login.setHoTen("Nguyễn Minh Anh"); login.setVaiTro(VaiTro.NHAN_VIEN_QUAN_LY);
                AtomicInteger calls = new AtomicInteger(); Runnable action = calls::incrementAndGet;
                MainShellPanel shell = new MainShellPanel(login, action, action, action, action, b -> {}, action, action, action, action, action);
                for (String label : new String[]{"Khách hàng", "Nhân viên & tài khoản", "Khuyến mãi", "Thống kê"}) {
                    JButton button = findButton(shell, label); assertTrue(button.isEnabled()); button.doClick();
                }
                assertEquals(4, calls.get()); render(shell, "manager-1280x720.png", 1280, 720);
                login.setVaiTro(VaiTro.NHAN_VIEN_BAN_HANG);
                MainShellPanel sales = new MainShellPanel(login, action, action, action, action, b -> {}, null, action, null, null, null);
                assertTrue(findButton(sales, "Khách hàng").isEnabled()); assertNull(findButton(sales, "Nhân viên & tài khoản"));
                assertNull(findButton(sales, "Khuyến mãi")); assertNull(findButton(sales, "Thống kê"));
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
    private static Object fixture(Kind kind) {
        if (kind == Kind.CUSTOMER) {
            KhachHangDTO d = new KhachHangDTO(); d.setMaKH("KH01"); d.setHoTen("Nguyễn Thảo Vy"); d.setSdt("0901234567");
            d.setEmail("vy@example.test"); d.setDiaChi("Quận Gò Vấp, TP. Hồ Chí Minh"); return d;
        }
        if (kind == Kind.PROMOTION) {
            KhuyenMaiDTO d = new KhuyenMaiDTO(); d.setMaKM("KM01"); d.setTenKM("Ưu đãi tháng 10");
            d.setPhanTramGiam(new BigDecimal("15")); d.setNgayBatDau(LocalDate.of(2026, 10, 1)); d.setNgayKetThuc(LocalDate.of(2026, 10, 31)); return d;
        }
        NhanVienAccountDTO d = new NhanVienAccountDTO(); d.setMaNV("NV02"); d.setHoTen("Trần Minh Anh");
        d.setSdt("0901234567"); d.setEmail("anh@example.test"); d.setDiaChi("TP. Hồ Chí Minh");
        d.setGioiTinh(false); d.setNgaySinh(LocalDate.of(2004, 8, 12)); d.setChucVu("Nhân viên bán hàng");
        d.setUsername("sales.demo"); d.setVaiTro(VaiTro.NHAN_VIEN_BAN_HANG); return d;
    }
    private static JTextField findField(Container root, String value) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTextField && ((JTextField) c).getText().equals(value)) { return (JTextField) c; }
            if (c instanceof Container) { JTextField f = findField((Container) c, value); if (f != null) { return f; } }
        } return null;
    }
    private static JButton findButton(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton && ((JButton)c).getText().equals(text)) { return (JButton)c; }
            if (c instanceof Container) { JButton b = findButton((Container)c, text); if (b != null) { return b; } }
        } return null;
    }
    private static JTable findTable(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTable) { return (JTable)c; }
            if (c instanceof Container) { JTable t = findTable((Container)c); if (t != null) { return t; } }
        } return null;
    }
    private static boolean containsLabel(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel && ((JLabel)c).getText().contains(text)) { return true; }
            if (c instanceof Container && containsLabel((Container)c, text)) { return true; }
        } return false;
    }
    private static void layout(Container root) { root.doLayout(); for (Component c : root.getComponents()) { if (c instanceof Container) { layout((Container)c); } } }
    private static void render(JComponent component, String name, int width, int height) throws Exception {
        File dir = new File(".tmp/store-management-ui"); assertTrue(dir.isDirectory() || dir.mkdirs());
        component.setSize(width, height); layout(component); layout(component);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        try { component.printAll(g); } finally { g.dispose(); } ImageIO.write(image, "png", new File(dir, name));
    }
}
