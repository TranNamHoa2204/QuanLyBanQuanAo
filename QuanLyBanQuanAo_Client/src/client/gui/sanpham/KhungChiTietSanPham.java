package client.gui.sanpham;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.LinkedHashSet;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import client.gui.components.ChuDeGiaoDien;
import client.gui.components.TienIchBangSanPham;
import common.dto.sanpham.DuLieuBienThe;
import common.dto.sanpham.DuLieuDayDuSanPham;
import common.dto.sanpham.DuLieuSanPham;

public final class KhungChiTietSanPham extends JPanel {
    private static final long serialVersionUID = 1L;

    public KhungChiTietSanPham(DuLieuDayDuSanPham detail) {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setPreferredSize(new Dimension(1000, 610));
        DuLieuSanPham product = detail.getSanPham();
        JPanel header = ChuDeGiaoDien.card(new GridLayout(0, 1, 6, 6));
        JLabel title = new JLabel(product.getMaSP() + " — " + product.getTenSP());
        title.setToolTipText(product.getTenSP());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        header.add(title);
        header.add(new JLabel(
                "Loại: " + product.getTenLoai() + " • Thương hiệu: " + product.getTenThuongHieu()));
        header.add(new JLabel("Giá: " + TienIchBangSanPham.money(product.getGiaBan()) + " • Tồn đang bán: "
                + product.getTongTon() + " • " + (product.getTrangThai() ? "Đang bán" : "Ngừng bán")));
        add(header, BorderLayout.NORTH);
        KhungAnhSanPham images = new KhungAnhSanPham(detail.getHinhAnh(), colors(detail));
        DefaultTableModel model = TienIchBangSanPham.model(
                new String[] { "Mã biến thể", "Màu", "Size", "Tồn", "Trạng thái" },
                new Class<?>[] { String.class, String.class, String.class, Integer.class, String.class });
        for (DuLieuBienThe variant : detail.getBienThe()) {
            model.addRow(new Object[] { variant.getMaBienThe(), variant.getMauSac(), variant.getKichCo(),
                    variant.getSoLuongTon(), variant.getTrangThai() ? "Đang bán" : "Ngừng bán" });
        }
        JTable table = ChuDeGiaoDien.tableWithHorizontalScroll(model, 100, 112, 50, 52, 95);
        TienIchBangSanPham.configure(table);
        table.getSelectionModel().addListSelectionListener(e -> {
            int selected = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && selected >= 0) {
                images.selectColor(
                        detail.getBienThe().get(table.convertRowIndexToModel(selected)).getMauSac());
            }
        });
        JPanel variants = ChuDeGiaoDien.card(new BorderLayout(8, 12));
        variants.add(new JLabel("Màu, size và tồn kho · Chọn một dòng để xem ảnh cùng màu"),
                BorderLayout.NORTH);
        JScrollPane scroll = ChuDeGiaoDien.tableScroll(table);
        variants.add(scroll, BorderLayout.CENTER);
        variants.add(new JLabel(detail.getBienThe().isEmpty() ? "Sản phẩm chưa có màu hoặc size."
                : "Tồn 0 nghĩa là hết hàng."), BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, images, variants);
        split.setResizeWeight(0.40);
        split.setDividerLocation(390);
        add(split, BorderLayout.CENTER);
    }

    static LinkedHashSet<String> colors(DuLieuDayDuSanPham detail) {
        LinkedHashSet<String> colors = new LinkedHashSet<>();
        for (DuLieuBienThe variant : detail.getBienThe()) {
            colors.add(variant.getMauSac());
        }
        return colors;
    }
}
