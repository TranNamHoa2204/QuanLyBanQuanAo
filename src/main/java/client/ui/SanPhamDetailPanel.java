package client.ui;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import common.dto.*;

public final class SanPhamDetailPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    public SanPhamDetailPanel(SanPhamDetailDTO detail) {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setPreferredSize(new Dimension(1000, 610));
        SanPhamDTO product = detail.getSanPham();
        JPanel header = UiTheme.card(new GridLayout(0, 1, 6, 6));
        JLabel title = new JLabel(product.getMaSP() + " — " + product.getTenSP());
        title.setToolTipText(product.getTenSP());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f)); header.add(title);
        header.add(new JLabel("Loại: " + product.getTenLoai() + " • Thương hiệu: " + product.getTenThuongHieu()));
        header.add(new JLabel("Giá: " + ProductTableSupport.money(product.getGiaBan()) + " • Tồn đang bán: "
                + product.getTongTon() + " • " + (product.getTrangThai() ? "Đang bán" : "Ngừng bán")));
        add(header, BorderLayout.NORTH);
        LinkedHashSet<String> colors = new LinkedHashSet<>();
        for (BienTheDTO variant : detail.getBienThe()) { colors.add(variant.getMauSac()); }
        ProductImagePanel images = new ProductImagePanel(detail.getHinhAnh(), colors);
        DefaultTableModel model = ProductTableSupport.model(
                new String[] {"Mã biến thể", "Màu", "Size", "Tồn", "Trạng thái"},
                new Class<?>[] {String.class, String.class, String.class, Integer.class, String.class});
        for (BienTheDTO variant : detail.getBienThe()) {
            model.addRow(new Object[] {variant.getMaBienThe(), variant.getMauSac(), variant.getKichCo(),
                    variant.getSoLuongTon(), variant.getTrangThai() ? "Đang bán" : "Ngừng bán"});
        }
        JTable table = new JTable(model); ProductTableSupport.configure(table);
        table.getSelectionModel().addListSelectionListener(e -> {
            int selected = table.getSelectedRow();
            if (selected >= 0) { images.selectColor(detail.getBienThe().get(table.convertRowIndexToModel(selected)).getMauSac()); }
        });
        JPanel variants = UiTheme.card(new BorderLayout(8, 12));
        variants.add(new JLabel("Biến thể theo màu/size — chọn một dòng để xem ảnh cùng màu"), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        variants.add(scroll, BorderLayout.CENTER);
        variants.add(new JLabel(detail.getBienThe().isEmpty() ? "Chưa có biến thể đang bán." : "Tồn 0: hết hàng; màn hình này không trừ tồn."), BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, images, variants);
        split.setResizeWeight(0.40); split.setDividerLocation(390); add(split, BorderLayout.CENTER);
    }
}
