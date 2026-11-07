package client.gui.danhmuc;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import client.controllers.DieuKhienQuanLySanPham;
import client.gui.components.ChuDeGiaoDien;
import client.utils.SinhMaTuDong;
import client.gui.components.TacVuGiaoDien;
import client.gui.components.TienIchBangSanPham;
import common.dto.danhmuc.DuLieuLoaiSanPham;
import common.dto.danhmuc.DuLieuThuongHieu;
import common.dto.danhmuc.YeuCauLuuDanhMuc;

/** Loại và thương hiệu không có cột trạng thái: chỉ thêm/sửa, không giả lập xóa mềm. */
public final class KhungDanhMuc extends JPanel {
    private static final long serialVersionUID = 1L;
    private final DieuKhienQuanLySanPham controller;
    private final boolean brand;
    private final DefaultTableModel model = TienIchBangSanPham.model(new String[] { "Mã", "Tên" },
            new Class<?>[] { String.class, String.class });
    private final JTable table = ChuDeGiaoDien.tableWithHorizontalScroll(model,
            125, 285);
    private final JButton reload = new JButton("Làm mới"), add = new JButton(),
            edit = new JButton("Sửa tên");
    private final JLabel status = ChuDeGiaoDien.muted("Chọn một dòng trong bảng để sửa tên.");

    public KhungDanhMuc(DieuKhienQuanLySanPham controller, boolean brand) {
        super(new BorderLayout(0, 16));
        this.controller = controller;
        this.brand = brand;
        add.setText(brand ? "Thêm thương hiệu" : "Thêm loại sản phẩm");
        setBorder(BorderFactory.createEmptyBorder(8, 16, 14, 16));
        JPanel top = ChuDeGiaoDien.card(new BorderLayout(0, 10), 14);
        top.add(ChuDeGiaoDien.heading(brand ? "Thương hiệu" : "Loại sản phẩm",
                "Chọn một dòng để đổi tên, hoặc thêm một mục mới."), BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(add);
        actions.add(edit);
        actions.add(reload);
        top.add(actions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        ChuDeGiaoDien.primary(add);
        TienIchBangSanPham.configure(table);
        JScrollPane scroll = ChuDeGiaoDien.tableScroll(table);
        add(scroll, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        edit.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> edit.setEnabled(table.getSelectedRow() >= 0));
        TienIchBangSanPham.onOpenRow(table, () -> edit.doClick());
        reload.addActionListener(e -> initialize());
        add.addActionListener(e -> editor(null, null));
        edit.addActionListener(e -> {
            int i = table.getSelectedRow();
            if (i >= 0) {
                i = table.convertRowIndexToModel(i);
                editor((String) model.getValueAt(i, 0), (String) model.getValueAt(i, 1));
            }
        });
    }

    public void initialize() {
        TacVuGiaoDien.run(this, reload, () -> {
            java.util.List<String[]> rows = new ArrayList<>();
            if (brand) {
                for (DuLieuThuongHieu b : controller.brands()) {
                    rows.add(new String[] { b.getMaThuongHieu(), b.getTenThuongHieu() });
                }
            } else {
                for (DuLieuLoaiSanPham c : controller.categories()) {
                    rows.add(new String[] { c.getMaLoai(), c.getTenLoai() });
                }
            }
            return rows;
        }, this::showRows);
    }

    public void showRows(java.util.List<String[]> rows) {
        model.setRowCount(0);
        rows.forEach(model::addRow);
        status.setText(rows.isEmpty() ? "Danh sách còn trống. Bấm " + add.getText() + " để bắt đầu."
                : rows.size() + " mục · Chọn một dòng để sửa tên.");
    }

    private void editor(String code, String before) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                code == null ? "Thêm mới" : "Sửa tên", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JTextField id = new JTextField(code == null ? SinhMaTuDong.tao(brand ? "TH" : "LSP") : code, 20),
                name = new JTextField(before == null ? "" : before, 24);
        id.setEditable(false);
        JPanel fields = ChuDeGiaoDien.card(new GridLayout(0, 1, 0, 16));
        fields.add(ChuDeGiaoDien.field("Mã (tự sinh)", id));
        fields.add(ChuDeGiaoDien.field("Tên", name));
        panel.add(fields, BorderLayout.CENTER);
        JButton save = new JButton("Lưu");
        ChuDeGiaoDien.primary(save);
        panel.add(save, BorderLayout.SOUTH);
        save.addActionListener(e -> {
            YeuCauLuuDanhMuc r = new YeuCauLuuDanhMuc();
            r.setCode(id.getText().trim());
            r.setName(name.getText().trim());
            r.setExpectedName(before);
            TacVuGiaoDien.run(dialog, save, () -> controller.catalog(r, brand, code == null), out -> {
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
