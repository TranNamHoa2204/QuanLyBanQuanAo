import static org.junit.Assert.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import com.fasterxml.jackson.databind.JsonNode;
import common.dto.*;
import common.enums.VaiTro;
import common.protocol.JsonCodec;
import client.api.SanPhamApi;
import client.controller.SanPhamController;
import client.network.SocketClient;
import client.session.ClientSession;
import client.ui.*;

/** Render Swing vào PNG trong headless để QA; không mở UI hoặc kết nối DB thật. */
public class ProductUiTest {
    @Test public void productPanelsRenderWithRealCatalogData() throws Exception {
        JsonNode root;
        try (InputStream input = getClass().getResourceAsStream("/data/catalog.json")) {
            root = JsonCodec.mapper().readTree(input);
        }
        Map<String, String> categories = new HashMap<>(), brands = new HashMap<>();
        for (JsonNode row : root.get("categories")) { categories.put(row.get("maLoai").asText(), row.get("tenLoai").asText()); }
        for (JsonNode row : root.get("brands")) { brands.put(row.get("maThuongHieu").asText(), row.get("tenThuongHieu").asText()); }
        SanPhamSearchResult result = new SanPhamSearchResult(); result.setTotal(40); result.setPageSize(20);
        for (JsonNode row : root.get("products")) {
            SanPhamDTO p = new SanPhamDTO();
            p.setMaSP(row.get("maSP").asText()); p.setTenSP(row.get("tenSP").asText());
            p.setGiaBan(row.get("giaBan").decimalValue());
            p.setTenLoai(categories.get(row.get("maLoai").asText()));
            p.setTenThuongHieu(brands.get(row.get("maThuongHieu").asText()));
            long stock = 0;
            for (JsonNode b : root.get("variants")) { if (b.get("maSP").asText().equals(p.getMaSP())) { stock += b.get("soLuongTon").asInt(); } }
            p.setTongTon(stock); result.getItems().add(p);
            if (result.getItems().size() == 20) { break; }
        }
        SanPhamDetailDTO detail = new SanPhamDetailDTO(); detail.setSanPham(result.getItems().get(0));
        for (JsonNode row : root.get("variants")) {
            if (!row.get("maSP").asText().equals("SP1001")) { continue; }
            BienTheDTO b = new BienTheDTO(); b.setMaBienThe(row.get("maBienThe").asText());
            b.setMauSac(row.get("mauSac").asText()); b.setKichCo(row.get("kichCo").asText());
            b.setSoLuongTon(row.get("soLuongTon").asInt()); detail.getBienThe().add(b);
        }
        for (JsonNode row : root.get("images")) {
            if (!row.get("maSP").asText().equals("SP1001")) { continue; }
            AnhSanPhamDTO a = new AnhSanPhamDTO(); a.setMaAnh(row.get("maAnh").asText());
            a.setMauSac(row.get("mauSac").asText()); a.setDuongDan(row.get("duongDan").asText());
            a.setThuTu(row.get("thuTu").asInt()); a.setAnhChinh(row.get("anhChinh").asBoolean());
            detail.getHinhAnh().add(a);
        }
        JPanel[] panels = new JPanel[2];
        SwingUtilities.invokeAndWait(() -> {
            UiTheme.install();
            SanPhamPanel search = new SanPhamPanel(new SanPhamController(
                    new SanPhamApi(new SocketClient("127.0.0.1", 1), new ClientSession())), VaiTro.NHAN_VIEN_QUAN_LY);
            search.showSearchResult(result); panels[0] = search;
            panels[1] = new SanPhamDetailPanel(detail);
            JTable table = firstTable(search);
            assertEquals(20, table.getRowCount());
            assertEquals(java.math.BigDecimal.class, table.getColumnClass(4));
            assertFalse(table.isCellEditable(0, 0));
            assertEquals(6, firstTable(panels[1]).getRowCount());
        });
        long deadline = System.currentTimeMillis() + 5000;
        boolean[] hasImage = new boolean[1];
        while (!hasImage[0] && System.currentTimeMillis() < deadline) {
            SwingUtilities.invokeAndWait(() -> hasImage[0] = hasIcon(panels[1]));
            if (!hasImage[0]) { Thread.sleep(50); }
        }
        assertTrue("Ảnh sản phẩm phải tải được trong background", hasImage[0]);
        File directory = new File(".tmp/product-ui"); assertTrue(directory.isDirectory() || directory.mkdirs());
        SwingUtilities.invokeAndWait(() -> {
            try {
                render(panels[0], new File(directory, "search-preview.png"));
                render(panels[1], new File(directory, "detail-preview.png"));
            } catch (IOException failure) { throw new RuntimeException(failure); }
        });
    }
    private static JTable firstTable(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTable) { return (JTable) child; }
            if (child instanceof Container) {
                JTable result = firstTable((Container) child); if (result != null) { return result; }
            }
        }
        return null;
    }
    private static boolean hasIcon(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel && ((JLabel) child).getIcon() != null) { return true; }
            if (child instanceof Container && hasIcon((Container) child)) { return true; }
        }
        return false;
    }
    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
    private static void render(JPanel panel, File file) throws IOException {
        Dimension size = panel.getPreferredSize(); panel.setSize(size); layout(panel); layout(panel);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        try { panel.printAll(g); } finally { g.dispose(); }
        ImageIO.write(image, "png", file);
    }
}
