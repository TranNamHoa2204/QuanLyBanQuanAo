import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import client.ui.*;
import client.api.SanPhamApi;
import client.api.LichSuKhoApi;
import client.controller.QuanLySanPhamController;
import client.network.SocketClient;
import client.session.ClientSession;
import common.dto.*;
import common.enums.VaiTro;

public class CatalogManagementUiTest {
    @Test public void managementPanelsAndEditorRenderWithoutNetwork() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                UiTheme.install(); File dir = new File(".tmp/catalog-management-ui"); assertTrue(dir.isDirectory() || dir.mkdirs());
                SocketClient socket = new SocketClient("127.0.0.1", 1);
                QuanLySanPhamController controller = new QuanLySanPhamController(new SanPhamApi(socket, new ClientSession()));
                SanPhamDTO p = new SanPhamDTO(); p.setMaSP("SP1001"); p.setTenSP("Áo phông nữ basic cổ tim");
                p.setMaLoai("L1"); p.setMaThuongHieu("TH1"); p.setTenLoai("Áo phông"); p.setTenThuongHieu("Canifa");
                p.setGiaBan(new BigDecimal("149000.50")); p.setTongTon(65);
                SanPhamSearchResult result = new SanPhamSearchResult(); result.setTotal(1); result.setPageSize(20); result.getItems().add(p);
                QuanLySanPhamPanel view = new QuanLySanPhamPanel(controller); view.showProducts(result);
                assertEquals(4, tabs(view).getTabCount()); assertEquals(1, table(view).getRowCount());
                assertFalse(table(view).isCellEditable(0, 4));
                LoginResponse login = new LoginResponse(); login.setHoTen("Nguyễn Minh Anh"); login.setVaiTro(VaiTro.NHAN_VIEN_QUAN_LY);
                MainShellPanel shell = new MainShellPanel(login, () -> {}, () -> {}, () -> {}, () -> {}, b -> {}, () -> {});
                shell.showPage("Danh mục & sản phẩm", view); render(shell, new File(dir, "management.png"), 1360, 900);
                DanhMucPanel catalog = new DanhMucPanel(controller, false);
                catalog.showRows(Arrays.asList(new String[]{"L1", "Áo phông"}, new String[]{"L2", "Quần shorts"}));
                render(catalog, new File(dir, "categories.png"), 900, 560);
                SanPhamDetailDTO detail = new SanPhamDetailDTO(); detail.setSanPham(p);
                BienTheDTO b = new BienTheDTO(); b.setMaBienThe("BT000001"); b.setMaSP(p.getMaSP()); b.setMauSac("Đỏ");
                b.setKichCo("M"); b.setSoLuongTon(10); b.setPhienBan(3L); detail.getBienThe().add(b);
                BienTheManagementPanel variants = new BienTheManagementPanel(controller); variants.showProduct(detail);
                render(variants, new File(dir, "variants.png"), 1000, 560);
                LichSuKhoPanel history = new LichSuKhoPanel(new LichSuKhoApi(socket, new ClientSession()));
                render(history, new File(dir, "stock-history.png"), 1000, 560);
                LoaiSanPhamDTO l = new LoaiSanPhamDTO(); l.setMaLoai("L1"); l.setTenLoai("Áo phông");
                ThuongHieuDTO brand = new ThuongHieuDTO(); brand.setMaThuongHieu("TH1"); brand.setTenThuongHieu("Canifa");
                SanPhamEditorPanel editor = new SanPhamEditorPanel(p, Collections.singletonList(l), Collections.singletonList(brand));
                assertSame(p, editor.request().getExpected()); assertNotSame(p, editor.request().getValue());
                assertEquals(p.getGiaBan(), editor.request().getValue().getGiaBan());
                render(editor, new File(dir, "product-editor.png"), 820, 420); socket.close();
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }
    private static JTabbedPane tabs(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTabbedPane) { return (JTabbedPane)c; }
            if (c instanceof Container) { JTabbedPane t = tabs((Container)c); if (t != null) { return t; } }
        } return null;
    }
    private static JTable table(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTable) { return (JTable)c; }
            if (c instanceof Container) { JTable t = table((Container)c); if (t != null) { return t; } }
        } return null;
    }
    private static void layout(Container c) { c.doLayout(); for (Component child : c.getComponents()) { if (child instanceof Container) { layout((Container)child); } } }
    private static void render(JPanel p, File file, int w, int h) throws Exception {
        p.setSize(w, h); layout(p); layout(p); BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        try { p.printAll(g); } finally { g.dispose(); } ImageIO.write(image, "png", file);
    }
}
