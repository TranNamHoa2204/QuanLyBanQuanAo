package client.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import client.controller.QuanLySanPhamController;
import common.dto.*;

public final class BienTheManagementPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final QuanLySanPhamController controller;
    private final JTextField productId = new JTextField(16);
    private final JButton load = new JButton("Tải biến thể"), add = new JButton("Thêm biến thể"), edit = new JButton("Sửa tồn / trạng thái");
    private final JLabel status = UiTheme.muted("Nhập mã sản phẩm để quản lý các biến thể.");
    private final DefaultTableModel model = ProductTableSupport.model(
            new String[]{"Mã biến thể", "Màu", "Size", "Tồn", "Trạng thái", "Phiên bản"},
            new Class<?>[]{String.class, String.class, String.class, Integer.class, String.class, Long.class});
    private final JTable table = new JTable(model);
    private SanPhamDetailDTO current;
    public BienTheManagementPanel(QuanLySanPhamController controller) {
        super(new BorderLayout(0, 16)); this.controller = controller; setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.add(UiTheme.heading("Biến thể & tồn kho", "Màu, size và sản phẩm cố định sau khi tạo. Điều chỉnh tồn có kiểm tra phiên bản."), BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(new JLabel("Mã sản phẩm")); actions.add(productId); actions.add(load); actions.add(add); actions.add(edit);
        top.add(actions, BorderLayout.SOUTH); add(top, BorderLayout.NORTH); UiTheme.primary(add); ProductTableSupport.configure(table);
        JScrollPane scroll = new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader());
        add(scroll, BorderLayout.CENTER); add(status, BorderLayout.SOUTH);
        add.setEnabled(false); edit.setEnabled(false); load.addActionListener(e -> initialize()); productId.addActionListener(e -> initialize());
        add.addActionListener(e -> editor(null));
        edit.addActionListener(e -> {
            int i = table.getSelectedRow(); if (i < 0) { JOptionPane.showMessageDialog(this, "Chọn một dòng trước."); return; }
            editor(current.getBienThe().get(table.convertRowIndexToModel(i)));
        });
    }
    public void loadProduct(String id) { productId.setText(id); initialize(); }
    private void initialize() {
        String id = productId.getText().trim(); add.setEnabled(false); edit.setEnabled(false); current = null; model.setRowCount(0);
        UiTask.run(this, load, () -> controller.detail(id), this::showProduct);
    }
    public void showProduct(SanPhamDetailDTO detail) {
        current = detail; productId.setText(detail.getSanPham().getMaSP()); model.setRowCount(0);
        for (BienTheDTO b : detail.getBienThe()) { model.addRow(new Object[]{b.getMaBienThe(), b.getMauSac(), b.getKichCo(), b.getSoLuongTon(), b.getTrangThai() ? "Đang bán" : "Ngừng bán", b.getPhienBan()}); }
        add.setEnabled(detail.getSanPham().getTrangThai()); edit.setEnabled(!detail.getBienThe().isEmpty());
        status.setText(detail.getSanPham().getMaSP() + " — " + detail.getSanPham().getTenSP() + " • " + detail.getBienThe().size() + " biến thể");
    }
    private void editor(BienTheDTO original) {
        String parent = current.getSanPham().getMaSP();
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), original == null ? "Thêm biến thể" : "Điều chỉnh biến thể", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new BorderLayout(0, 16)); panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JTextField id = new JTextField(original == null ? "" : original.getMaBienThe(), 18),
                color = new JTextField(original == null ? "" : original.getMauSac(), 18), size = new JTextField(original == null ? "" : original.getKichCo(), 18);
        id.setEditable(original == null); color.setEditable(original == null); size.setEditable(original == null);
        JSpinner stock = new JSpinner(new SpinnerNumberModel(original == null ? 0 : original.getSoLuongTon(), 0, Integer.MAX_VALUE, 1));
        JCheckBox active = new JCheckBox("Đang bán", original == null || original.getTrangThai());
        JPanel fields = UiTheme.card(new GridLayout(0, 2, 14, 16)); fields.add(UiTheme.field("Mã biến thể", id));
        fields.add(UiTheme.field("Sản phẩm", new JLabel(parent))); fields.add(UiTheme.field("Màu", color)); fields.add(UiTheme.field("Size", size));
        fields.add(UiTheme.field("Tồn mới (số lượng tuyệt đối)", stock)); fields.add(active); panel.add(fields, BorderLayout.CENTER);
        JButton save = new JButton("Lưu"); UiTheme.primary(save); panel.add(save, BorderLayout.SOUTH);
        save.addActionListener(e -> {
            try { stock.commitEdit(); } catch (java.text.ParseException invalid) { JOptionPane.showMessageDialog(dialog, "Tồn phải là số nguyên không âm."); return; }
            int amount = ((Number)stock.getValue()).intValue();
            if (original != null && amount != original.getSoLuongTon()
                    && JOptionPane.showConfirmDialog(dialog, "Đặt tồn từ " + original.getSoLuongTon() + " thành " + amount
                    + "? Đây không phải cộng thêm hàng.", "Xác nhận điều chỉnh tồn", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) { return; }
            BienTheDTO r = new BienTheDTO(); r.setMaSP(parent); r.setMaBienThe(id.getText().trim()); r.setMauSac(color.getText().trim());
            r.setKichCo(size.getText().trim()); r.setSoLuongTon(amount); r.setTrangThai(active.isSelected()); r.setPhienBan(original == null ? null : original.getPhienBan());
            UiTask.run(dialog, save, () -> controller.variant(r, original == null), out -> { dialog.dispose(); loadProduct(parent); });
        });
        dialog.add(panel); dialog.pack(); dialog.setLocationRelativeTo(this); dialog.getRootPane().setDefaultButton(save); dialog.setVisible(true);
    }
}
