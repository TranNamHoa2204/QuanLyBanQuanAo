package client.ui;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import client.controller.QuanLySanPhamController;
import common.dto.*;

/** Loại và thương hiệu không có cột trạng thái: chỉ thêm/sửa, không giả lập xóa mềm. */
public final class DanhMucPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final QuanLySanPhamController controller;
    private final boolean brand;
    private final DefaultTableModel model = ProductTableSupport.model(new String[] { "Mã", "Tên" },
            new Class<?>[] { String.class, String.class });
    private final JTable table = new JTable(model);
    private final JButton reload = new JButton("Tải lại"), add = new JButton("Thêm mới"), edit = new JButton("Sửa tên");
    private final JLabel status = UiTheme.muted("Chưa tải dữ liệu.");

    public DanhMucPanel(QuanLySanPhamController controller, boolean brand) {
        super(new BorderLayout(0, 16));
        this.controller = controller;
        this.brand = brand;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.add(UiTheme.heading(brand ? "Thương hiệu" : "Loại sản phẩm",
                "Thêm hoặc đổi tên danh mục; không xóa dữ liệu liên quan đến sản phẩm."), BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(reload);
        actions.add(add);
        actions.add(edit);
        top.add(actions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        UiTheme.primary(add);
        ProductTableSupport.configure(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        add(scroll, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        reload.addActionListener(e -> initialize());
        add.addActionListener(e -> editor(null, null));
        edit.addActionListener(e -> {
            int i = table.getSelectedRow();
            if (i < 0) {
                JOptionPane.showMessageDialog(this, "Chọn một dòng trước.");
                return;
            }
            i = table.convertRowIndexToModel(i);
            editor((String) model.getValueAt(i, 0), (String) model.getValueAt(i, 1));
        });
    }

    public void initialize() {
        UiTask.run(this, reload, () -> {
            java.util.List<String[]> rows = new ArrayList<>();
            if (brand) {
                for (ThuongHieuDTO b : controller.brands()) {
                    rows.add(new String[] { b.getMaThuongHieu(), b.getTenThuongHieu() });
                }
            } else {
                for (LoaiSanPhamDTO c : controller.categories()) {
                    rows.add(new String[] { c.getMaLoai(), c.getTenLoai() });
                }
            }
            return rows;
        }, this::showRows);
    }

    public void showRows(java.util.List<String[]> rows) {
        model.setRowCount(0);
        rows.forEach(model::addRow);
        status.setText(rows.size() + " dòng • Mã danh mục không đổi khi cập nhật.");
    }

    private void editor(String code, String before) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), code == null ? "Thêm mới" : "Sửa tên",
                Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JTextField id = new JTextField(code == null ? "" : code, 20),
                name = new JTextField(before == null ? "" : before, 24);
        id.setEditable(code == null);
        JPanel fields = UiTheme.card(new GridLayout(0, 1, 0, 16));
        fields.add(UiTheme.field("Mã (tối đa 20 ký tự)", id));
        fields.add(UiTheme.field("Tên", name));
        panel.add(fields, BorderLayout.CENTER);
        JButton save = new JButton("Lưu");
        UiTheme.primary(save);
        panel.add(save, BorderLayout.SOUTH);
        save.addActionListener(e -> {
            DanhMucSaveRequest r = new DanhMucSaveRequest();
            r.setCode(id.getText().trim());
            r.setName(name.getText().trim());
            r.setExpectedName(before);
            UiTask.run(dialog, save, () -> controller.catalog(r, brand, code == null), out -> {
                dialog.dispose();
                initialize();
            });
        });
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.getRootPane().setDefaultButton(save);
        dialog.setVisible(true);
    }
}
