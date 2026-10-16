package client.ui;

import java.awt.*;
import java.math.BigDecimal;
import java.util.UUID;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import client.controller.QuanLySanPhamController;
import common.dto.BienTheDTO;
import common.dto.DieuChinhTonKhoRequest;
import common.dto.SanPhamDetailDTO;

/** Quản lý thuộc tính biến thể và cung cấp luồng điều chỉnh tồn có audit. */
public final class BienTheManagementPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final QuanLySanPhamController controller;
    private final JTextField productId = new JTextField(16);
    private final JButton load = new JButton("Tải biến thể"), add = new JButton("Thêm biến thể"),
            editState = new JButton("Sửa trạng thái"), adjust = new JButton("Điều chỉnh tồn");
    private final JLabel status = UiTheme.muted("Nhập mã sản phẩm để quản lý các biến thể.");
    private final DefaultTableModel model = ProductTableSupport.model(
            new String[]{"Mã biến thể", "Màu", "Size", "Tồn", "Trạng thái", "Phiên bản"},
            new Class<?>[]{String.class, String.class, String.class, Integer.class, String.class, Long.class});
    private final JTable table = new JTable(model);
    private SanPhamDetailDTO current;

    public BienTheManagementPanel(QuanLySanPhamController controller) {
        super(new BorderLayout(0, 16)); this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.add(UiTheme.heading("Biến thể & tồn kho",
                "Tồn khởi tạo được ghi nhận; mọi điều chỉnh tồn cần lý do và version hiện hành."), BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(new JLabel("Mã sản phẩm")); actions.add(productId); actions.add(load);
        actions.add(add); actions.add(editState); actions.add(adjust);
        top.add(actions, BorderLayout.SOUTH); add(top, BorderLayout.NORTH);
        UiTheme.primary(add); UiTheme.primary(adjust); ProductTableSupport.configure(table);
        JScrollPane scroll = new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader());
        add(scroll, BorderLayout.CENTER); add(status, BorderLayout.SOUTH);
        add.setEnabled(false); editState.setEnabled(false); adjust.setEnabled(false);
        load.addActionListener(e -> initialize()); productId.addActionListener(e -> initialize());
        add.addActionListener(e -> editor(null));
        editState.addActionListener(e -> {
            BienTheDTO selected = selectedVariant(); if (selected != null) { editor(selected); }
        });
        adjust.addActionListener(e -> {
            BienTheDTO selected = selectedVariant(); if (selected != null) { adjustStock(selected); }
        });
    }

    public void loadProduct(String id) { productId.setText(id); initialize(); }

    private BienTheDTO selectedVariant() {
        int row = table.getSelectedRow();
        if (row < 0 || current == null) {
            JOptionPane.showMessageDialog(this, "Chọn một biến thể trước."); return null;
        }
        return current.getBienThe().get(table.convertRowIndexToModel(row));
    }

    private void initialize() {
        String id = productId.getText().trim(); add.setEnabled(false); editState.setEnabled(false);
        adjust.setEnabled(false); current = null; model.setRowCount(0);
        UiTask.run(this, load, () -> controller.detail(id), this::showProduct);
    }

    public void showProduct(SanPhamDetailDTO detail) {
        current = detail; productId.setText(detail.getSanPham().getMaSP()); model.setRowCount(0);
        for (BienTheDTO b : detail.getBienThe()) {
            model.addRow(new Object[]{b.getMaBienThe(), b.getMauSac(), b.getKichCo(), b.getSoLuongTon(),
                    b.getTrangThai() ? "Đang bán" : "Ngừng bán", b.getPhienBan()});
        }
        add.setEnabled(detail.getSanPham().getTrangThai());
        editState.setEnabled(!detail.getBienThe().isEmpty()); adjust.setEnabled(!detail.getBienThe().isEmpty());
        status.setText(detail.getSanPham().getMaSP() + " — " + detail.getSanPham().getTenSP()
                + " • " + detail.getBienThe().size() + " biến thể");
    }

    private void editor(BienTheDTO original) {
        String parent = current.getSanPham().getMaSP();
        boolean creating = original == null;
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                creating ? "Thêm biến thể" : "Sửa trạng thái biến thể", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JTextField id = new JTextField(creating ? "" : original.getMaBienThe(), 18),
                color = new JTextField(creating ? "" : original.getMauSac(), 18),
                size = new JTextField(creating ? "" : original.getKichCo(), 18);
        id.setEditable(creating); color.setEditable(creating); size.setEditable(creating);
        JPanel fields = UiTheme.card(new GridLayout(0, 2, 14, 16));
        fields.add(UiTheme.field("Mã biến thể", id)); fields.add(UiTheme.field("Sản phẩm", new JLabel(parent)));
        fields.add(UiTheme.field("Màu", color)); fields.add(UiTheme.field("Size", size));
        if (creating) {
            JSpinner initialStock = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
            fields.add(UiTheme.field("Tồn khởi tạo", initialStock));
            JCheckBox active = new JCheckBox("Đang bán", true); fields.add(active);
            panel.add(fields, BorderLayout.CENTER);
            JButton save = new JButton("Tạo biến thể"); UiTheme.primary(save); panel.add(save, BorderLayout.SOUTH);
            save.addActionListener(e -> {
                try { initialStock.commitEdit(); }
                catch (java.text.ParseException invalid) {
                    JOptionPane.showMessageDialog(dialog, "Tồn phải là số nguyên không âm."); return;
                }
                BienTheDTO request = new BienTheDTO(); request.setMaSP(parent); request.setMaBienThe(id.getText().trim());
                request.setMauSac(color.getText().trim()); request.setKichCo(size.getText().trim());
                request.setSoLuongTon(((Number) initialStock.getValue()).intValue()); request.setTrangThai(active.isSelected());
                UiTask.run(dialog, save, () -> controller.variant(request, true), out -> {
                    dialog.dispose(); loadProduct(parent);
                });
            });
        } else {
            fields.add(UiTheme.field("Tồn hiện tại", new JLabel(String.valueOf(original.getSoLuongTon()))));
            JCheckBox active = new JCheckBox("Đang bán", original.getTrangThai()); fields.add(active);
            panel.add(fields, BorderLayout.CENTER);
            JButton save = new JButton("Lưu trạng thái"); UiTheme.primary(save); panel.add(save, BorderLayout.SOUTH);
            save.addActionListener(e -> {
                BienTheDTO request = new BienTheDTO(); request.setMaSP(parent); request.setMaBienThe(id.getText().trim());
                request.setMauSac(color.getText().trim()); request.setKichCo(size.getText().trim());
                request.setSoLuongTon(original.getSoLuongTon()); request.setTrangThai(active.isSelected());
                request.setPhienBan(original.getPhienBan());
                UiTask.run(dialog, save, () -> controller.variant(request, false), out -> {
                    dialog.dispose(); loadProduct(parent);
                });
            });
        }
        dialog.add(panel); dialog.pack(); dialog.setLocationRelativeTo(this);
        dialog.getRootPane().setDefaultButton((JButton) panel.getComponent(panel.getComponentCount() - 1));
        dialog.setVisible(true);
    }

    private void adjustStock(BienTheDTO variant) {
        String parent = current.getSanPham().getMaSP();
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Điều chỉnh tồn kho", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new BorderLayout(0, 16)); panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JSpinner target = new JSpinner(new SpinnerNumberModel(variant.getSoLuongTon(), 0, Integer.MAX_VALUE, 1));
        JTextArea reason = new JTextArea(3, 28); reason.setLineWrap(true); reason.setWrapStyleWord(true);
        JPanel fields = UiTheme.card(new GridLayout(0, 2, 14, 16));
        fields.add(UiTheme.field("Biến thể", new JLabel(variant.getMaBienThe())));
        fields.add(UiTheme.field("Tồn hiện tại", new JLabel(String.valueOf(variant.getSoLuongTon()))));
        fields.add(UiTheme.field("Tồn mục tiêu", target)); fields.add(UiTheme.field("Version", new JLabel(String.valueOf(variant.getPhienBan()))));
        JPanel reasonField = UiTheme.field("Lý do (bắt buộc, tối đa 500 ký tự)", new JScrollPane(reason));
        panel.add(fields, BorderLayout.NORTH); panel.add(reasonField, BorderLayout.CENTER);
        JButton save = new JButton("Xác nhận điều chỉnh"); UiTheme.primary(save); panel.add(save, BorderLayout.SOUTH);
        String operationId = UUID.randomUUID().toString();
        save.addActionListener(e -> {
            try { target.commitEdit(); }
            catch (java.text.ParseException invalid) {
                JOptionPane.showMessageDialog(dialog, "Tồn phải là số nguyên không âm."); return;
            }
            int amount = ((Number) target.getValue()).intValue(); String explanation = reason.getText().trim();
            if (amount == variant.getSoLuongTon() || explanation.isEmpty() || explanation.length() > 500) {
                JOptionPane.showMessageDialog(dialog, amount == variant.getSoLuongTon()
                        ? "Tồn mục tiêu phải khác tồn hiện tại."
                        : "Nhập lý do từ 1 đến 500 ký tự."); return;
            }
            String message = "Đặt tồn " + variant.getMaBienThe() + " từ " + variant.getSoLuongTon() + " thành " + amount + "?";
            if (JOptionPane.showConfirmDialog(dialog, message, "Xác nhận", JOptionPane.YES_NO_OPTION)
                    != JOptionPane.YES_OPTION) { return; }
            DieuChinhTonKhoRequest request = new DieuChinhTonKhoRequest(); request.setMaBienThe(variant.getMaBienThe());
            request.setSoLuongMoi(BigDecimal.valueOf(amount)); request.setExpectedVersion(BigDecimal.valueOf(variant.getPhienBan()));
            request.setLyDo(explanation); request.setOperationId(operationId);
            // Cùng request được dùng lại nếu phản hồi mạng bị mất; operationId ổn định suốt dialog.
            target.setEnabled(false); reason.setEditable(false);
            UiTask.run(dialog, save, () -> controller.adjustStock(request), out -> {
                dialog.dispose(); loadProduct(parent);
            });
        });
        dialog.add(panel); dialog.pack(); dialog.setLocationRelativeTo(this);
        dialog.getRootPane().setDefaultButton(save); dialog.setVisible(true);
    }
}
