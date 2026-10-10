package client.ui;

import java.awt.*;
import javax.swing.*;
import client.controller.*;
import common.dto.*;
import common.enums.VaiTro;

public final class QuanLySanPhamPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final QuanLySanPhamController controller;
    private final SanPhamPanel list;
    private final DanhMucPanel categories, brands;
    private final BienTheManagementPanel variants;
    private final JTabbedPane tabs = new JTabbedPane();
    public QuanLySanPhamPanel(QuanLySanPhamController controller) {
        super(new BorderLayout()); this.controller = controller;
        list = new SanPhamPanel(new SanPhamController(controller.products()), VaiTro.NHAN_VIEN_QUAN_LY);
        categories = new DanhMucPanel(controller, false); brands = new DanhMucPanel(controller, true);
        variants = new BienTheManagementPanel(controller);
        JPanel products = new JPanel(new BorderLayout()); products.add(list, BorderLayout.CENTER);
        JButton add = new JButton("Thêm sản phẩm"), edit = new JButton("Sửa sản phẩm"), stop = new JButton("Ngừng bán"),
                variant = new JButton("Biến thể / tồn"), reload = new JButton("Tải lại");
        UiTheme.primary(add); UiTheme.danger(stop);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setBorder(BorderFactory.createEmptyBorder(16, 24, 0, 24));
        for (JButton b : new JButton[]{add, edit, stop, variant, reload}) { actions.add(b); } products.add(actions, BorderLayout.NORTH);
        tabs.addTab("Sản phẩm", products); tabs.addTab("Loại sản phẩm", categories); tabs.addTab("Thương hiệu", brands); tabs.addTab("Biến thể & tồn", variants);
        add(tabs, BorderLayout.CENTER);
        add.addActionListener(e -> openEditor(null, add)); edit.addActionListener(e -> {
            SanPhamDTO selected = selected(); if (selected != null) { openEditor(selected.getMaSP(), edit); }
        });
        stop.addActionListener(e -> {
            SanPhamDTO selected = selected(); if (selected == null) { return; }
            if (JOptionPane.showConfirmDialog(this, "Ngừng bán " + selected.getMaSP() + "? Không xóa dữ liệu hoặc hóa đơn.",
                    "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) { return; }
            SanPhamSaveRequest r = new SanPhamSaveRequest(); r.setValue(selected); r.setExpected(selected);
            UiTask.run(this, stop, () -> controller.product(r, 2), out -> list.reload());
        });
        variant.addActionListener(e -> {
            SanPhamDTO selected = selected(); if (selected != null) { tabs.setSelectedComponent(variants); variants.loadProduct(selected.getMaSP()); }
        });
        reload.addActionListener(e -> list.reload());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == categories) { categories.initialize(); }
            else if (tabs.getSelectedComponent() == brands) { brands.initialize(); }
        });
    }
    public void initialize() { list.reload(); }
    public void showProducts(SanPhamSearchResult result) { list.showSearchResult(result); }
    private SanPhamDTO selected() {
        SanPhamDTO selected = list.selectedProduct();
        if (selected == null) { JOptionPane.showMessageDialog(this, "Chọn sản phẩm trong bảng trước."); } return selected;
    }
    private void openEditor(String id, JButton trigger) {
        UiTask.run(this, trigger, () -> {
            Object[] data = new Object[3]; data[0] = id == null ? null : controller.detail(id).getSanPham();
            data[1] = controller.categories(); data[2] = controller.brands(); return data;
        }, this::editor);
    }
    @SuppressWarnings("unchecked") private void editor(Object[] data) {
        SanPhamDTO original = (SanPhamDTO)data[0];
        SanPhamEditorPanel editor = new SanPhamEditorPanel(original, (java.util.List<LoaiSanPhamDTO>)data[1], (java.util.List<ThuongHieuDTO>)data[2]);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), original == null ? "Thêm sản phẩm" : "Sửa sản phẩm", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel body = new JPanel(new BorderLayout(0, 20)); body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24)); body.add(editor, BorderLayout.CENTER);
        JButton save = new JButton("Lưu sản phẩm"); UiTheme.primary(save); body.add(save, BorderLayout.SOUTH);
        save.addActionListener(e -> {
            SanPhamSaveRequest r;
            try { r = editor.request(); } catch (IllegalArgumentException invalid) { JOptionPane.showMessageDialog(dialog, invalid.getMessage()); return; }
            UiTask.run(dialog, save, () -> controller.product(r, original == null ? 0 : 1), out -> { dialog.dispose(); list.reload(); });
        });
        dialog.add(body); dialog.pack(); dialog.setLocationRelativeTo(this); dialog.getRootPane().setDefaultButton(save); dialog.setVisible(true);
    }
}
