package client.gui.sanpham;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import client.gui.components.ChuDeGiaoDien;
import common.dto.danhmuc.DuLieuLoaiSanPham;
import common.dto.danhmuc.DuLieuThuongHieu;
import common.dto.sanpham.DuLieuSanPham;
import common.dto.sanpham.YeuCauLuuSanPham;

/** Tạo DTO mới; không sửa ảnh chụp expected của dữ liệu đã đọc. */
public final class KhungChinhSuaSanPham extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField id = new JTextField(16), name = new JTextField(22), price = new JTextField(16);
    private final JComboBox<Choice> category = new JComboBox<>(), brand = new JComboBox<>();
    private final JCheckBox active = new JCheckBox("Đang kinh doanh", true);
    private final DuLieuSanPham original;

    public KhungChinhSuaSanPham(DuLieuSanPham original, List<DuLieuLoaiSanPham> categories,
            List<DuLieuThuongHieu> brands) {
        super(new BorderLayout(0, 16));
        this.original = original;
        id.setText(original == null ? "" : original.getMaSP());
        id.setEditable(false);
        add(ChuDeGiaoDien.heading(original == null ? "Thêm sản phẩm" : "Cập nhật sản phẩm",
                "Mã cố định. Giá được áp dụng cho lần thêm hàng mới."), BorderLayout.NORTH);
        JPanel fields = ChuDeGiaoDien.card(new GridLayout(0, 2, 14, 16));
        categories.forEach(c -> category.addItem(new Choice(c.getMaLoai(), c.getTenLoai())));
        brands.forEach(b -> brand.addItem(new Choice(b.getMaThuongHieu(), b.getTenThuongHieu())));
        fields.add(ChuDeGiaoDien.field("Mã sản phẩm", id));
        fields.add(ChuDeGiaoDien.field("Tên sản phẩm", name));
        fields.add(ChuDeGiaoDien.field("Loại sản phẩm", category));
        fields.add(ChuDeGiaoDien.field("Thương hiệu", brand));
        fields.add(ChuDeGiaoDien.field("Giá bán (VD: 149000 hoặc 149000.50)", price));
        fields.add(active);
        add(fields, BorderLayout.CENTER);
        if (original != null) {
            id.setText(original.getMaSP());
            id.setEditable(false);
            name.setText(original.getTenSP());
            price.setText(original.getGiaBan().toPlainString());
            active.setSelected(original.getTrangThai());
            select(category, original.getMaLoai());
            select(brand, original.getMaThuongHieu());
        }
    }

    public YeuCauLuuSanPham request() {
        DuLieuSanPham v = new DuLieuSanPham();
        v.setMaSP(id.getText().trim());
        v.setTenSP(name.getText().trim());
        try {
            v.setGiaBan(new BigDecimal(price.getText().trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá phải là số; không nhập dấu phân cách hàng nghìn.");
        }
        if (category.getSelectedItem() == null || brand.getSelectedItem() == null) {
            throw new IllegalArgumentException("Cần tạo loại và thương hiệu trước.");
        }
        v.setMaLoai(((Choice) category.getSelectedItem()).id);
        v.setMaThuongHieu(((Choice) brand.getSelectedItem()).id);
        v.setTrangThai(active.isSelected());
        YeuCauLuuSanPham r = new YeuCauLuuSanPham();
        r.setValue(v);
        r.setExpected(original);
        return r;
    }

    private static void select(JComboBox<Choice> combo, String id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id.equals(id)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private static final class Choice {
        private final String id, name;

        Choice(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String toString() {
            return id + " — " + name;
        }
    }
}
