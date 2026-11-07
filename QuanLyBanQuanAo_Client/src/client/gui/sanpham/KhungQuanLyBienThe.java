package client.gui.sanpham;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import client.controllers.DieuKhienQuanLySanPham;
import client.gui.components.ChuDeGiaoDien;
import client.gui.components.TacVuGiaoDien;
import client.gui.components.TienIchBangSanPham;
import common.dto.kho.YeuCauDieuChinhTonKho;
import common.dto.sanpham.DuLieuBienThe;
import common.dto.sanpham.DuLieuDayDuSanPham;
import common.dto.sanpham.DuLieuSanPham;
import common.dto.sanpham.YeuCauTraCuuSanPham;

/** Quản lý thuộc tính biến thể và cung cấp luồng điều chỉnh tồn có audit. */
public final class KhungQuanLyBienThe extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 50;
    private final DieuKhienQuanLySanPham controller;
    private final JTextField keyword = new JTextField(15);
    private final JComboBox<ProductOption> products = new JComboBox<>();
    private final JButton search = new JButton("Tìm sản phẩm"), previous = new JButton("Trang trước"),
            next = new JButton("Trang sau"), retry = new JButton("Thử lại"),
            add = new JButton("Thêm màu / size"), editState = new JButton("Đổi trạng thái bán"),
            adjust = new JButton("Điều chỉnh tồn kho");
    private final JLabel status = ChuDeGiaoDien.muted("Chọn một sản phẩm để xem màu, size và tồn kho.");
    private final DefaultTableModel model = TienIchBangSanPham.model(
            new String[] { "Mã biến thể", "Màu", "Size", "Tồn kho", "Trạng thái" }, new Class<?>[] {
                    String.class, String.class, String.class, Integer.class, String.class });
    private final JTable table = ChuDeGiaoDien.tableWithHorizontalScroll(model,
            110, 135, 55, 75, 110);
    private DuLieuDayDuSanPham current;
    private boolean busy, updatingPicker, choicesLoaded;
    private String appliedKeyword = "", failedProduct, queuedProduct;
    private int page;
    private long total;

    public KhungQuanLyBienThe(DieuKhienQuanLySanPham controller) {
        super(new BorderLayout(0, 16));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(8, 16, 14, 16));
        JPanel top = ChuDeGiaoDien.card(new BorderLayout(0, 10), 14);
        top.add(ChuDeGiaoDien.heading("Màu, size và tồn kho",
                "Chọn sản phẩm, sau đó chọn màu và size cần chỉnh."),
                BorderLayout.NORTH);
        keyword.putClientProperty("JTextField.placeholderText", "Nhập tên sản phẩm");
        products.getAccessibleContext().setAccessibleName("Chọn sản phẩm");
        products.setPrototypeDisplayValue(new ProductOption(null, "Chọn sản phẩm trong danh sách"));
        products.addItem(new ProductOption(null, "Chọn sản phẩm"));
        JPanel picker = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        picker.setOpaque(false);
        for (java.awt.Component control : new java.awt.Component[] { keyword, search, products, previous, next }) {
            picker.add(control);
        }
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(add);
        actions.add(editState);
        actions.add(adjust);
        actions.add(retry);
        JPanel controls = new JPanel(new BorderLayout(0, 8));
        controls.setOpaque(false);
        controls.add(picker, BorderLayout.NORTH);
        controls.add(actions, BorderLayout.SOUTH);
        top.add(controls, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        ChuDeGiaoDien.primary(add);
        ChuDeGiaoDien.primary(search);
        TienIchBangSanPham.configure(table);
        JScrollPane scroll = ChuDeGiaoDien.tableScroll(table);
        add(scroll, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        search.addActionListener(e -> searchProducts(keyword.getText().trim(), 0));
        keyword.addActionListener(e -> search.doClick());
        previous.addActionListener(e -> searchProducts(appliedKeyword, page - 1));
        next.addActionListener(e -> searchProducts(appliedKeyword, page + 1));
        retry.addActionListener(e -> loadProduct(failedProduct));
        products.addActionListener(e -> {
            if (!updatingPicker) {
                ProductOption option = (ProductOption) products.getSelectedItem();
                if (option != null && option.id != null) {
                    if (current == null || !option.id.equals(current.getSanPham().getMaSP())) {
                        loadProduct(option.id);
                    }
                } else {
                    clearDetail();
                    status.setText("Chọn một sản phẩm để xem màu, size và tồn kho.");
                    refreshControls();
                }
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> refreshControls());
        add.addActionListener(e -> editor(null));
        editState.addActionListener(e -> {
            DuLieuBienThe selected = selectedVariant();
            if (selected != null) {
                editor(selected);
            }
        });
        adjust.addActionListener(e -> {
            DuLieuBienThe selected = selectedVariant();
            if (selected != null) {
                adjustStock(selected);
            }
        });
        refreshControls();
    }

    public void open() {
        if (!choicesLoaded && current == null && !busy) {
            searchProducts(keyword.getText().trim(), 0);
        }
    }

    private void searchProducts(String text, int requestedPage) {
        YeuCauTraCuuSanPham request = new YeuCauTraCuuSanPham();
        request.setTenSP(text);
        request.setPage(requestedPage);
        request.setPageSize(PAGE_SIZE);
        request.setIncludeInactive(true);
        read(() -> controller.products().search(request), result -> {
            List<DuLieuSanPham> rows = result.getItems();
            DefaultComboBoxModel<ProductOption> options = new DefaultComboBoxModel<>();
            options.addElement(new ProductOption(null, "Chọn sản phẩm"));
            for (DuLieuSanPham row : rows) {
                options.addElement(new ProductOption(row));
            }
            updatingPicker = true;
            products.setModel(options);
            updatingPicker = false;
            clearDetail();
            appliedKeyword = text;
            page = result.getPage();
            total = result.getTotal();
            choicesLoaded = true;
            status.setText(rows.isEmpty() ? "Không tìm thấy sản phẩm phù hợp."
                    : total + " sản phẩm · Trang " + (page + 1) + " · Chọn một sản phẩm trong danh sách.");
            if (!rows.isEmpty() && queuedProduct == null) {
                products.setSelectedIndex(1);
            }
        }, message -> status.setText("Chưa tìm được sản phẩm. Bấm Tìm sản phẩm để thử lại."));
    }

    public void loadProduct(String id) {
        if (id == null) {
            return;
        }
        if (busy) {
            queuedProduct = id;
            return;
        }
        read(() -> controller.detail(id), this::showProduct, message -> {
            clearDetail();
            failedProduct = id;
            status.setText("Chưa mở được sản phẩm " + id + ". Bấm Thử lại.");
        });
    }

    private DuLieuBienThe selectedVariant() {
        int row = table.getSelectedRow();
        if (row < 0 || current == null) {
            return null;
        }
        return current.getBienThe().get(table.convertRowIndexToModel(row));
    }

    private void clearDetail() {
        current = null;
        failedProduct = null;
        model.setRowCount(0);
    }

    private <T> void read(Callable<T> job, Consumer<T> success, Consumer<String> failure) {
        if (busy) {
            return;
        }
        busy = true;
        status.setText("Đang mở dữ liệu…");
        status.setToolTipText(null);
        refreshControls();
        TacVuGiaoDien.run(search, job, value -> {
            busy = false;
            success.accept(value);
            finishRead();
        }, message -> {
            busy = false;
            status.setToolTipText(message);
            failure.accept(message);
            finishRead();
        });
    }

    private void finishRead() {
        refreshControls();
        if (queuedProduct != null) {
            String id = queuedProduct;
            queuedProduct = null;
            loadProduct(id);
        }
    }

    private void refreshControls() {
        search.setEnabled(!busy);
        keyword.setEnabled(!busy);
        products.setEnabled(!busy && products.getItemCount() > 1);
        previous.setEnabled(!busy && page > 0);
        next.setEnabled(!busy && (page + 1L) * PAGE_SIZE < total);
        retry.setVisible(failedProduct != null);
        retry.setEnabled(!busy);
        add.setEnabled(!busy && current != null && current.getSanPham().getTrangThai());
        boolean selected = !busy && current != null && table.getSelectedRow() >= 0;
        editState.setEnabled(selected);
        adjust.setEnabled(selected);
    }

    public void showProduct(DuLieuDayDuSanPham detail) {
        current = detail;
        updatingPicker = true;
        ProductOption option = null;
        for (int i = 0; i < products.getItemCount(); i++) {
            if (detail.getSanPham().getMaSP().equals(products.getItemAt(i).id)) {
                option = products.getItemAt(i);
                break;
            }
        }
        if (option == null) {
            option = new ProductOption(detail.getSanPham());
            products.addItem(option);
        }
        products.setSelectedItem(option);
        updatingPicker = false;
        model.setRowCount(0);
        for (DuLieuBienThe b : detail.getBienThe()) {
            model.addRow(new Object[] { b.getMaBienThe(), b.getMauSac(), b.getKichCo(), b.getSoLuongTon(),
                    b.getTrangThai() ? "Đang bán" : "Ngừng bán" });
        }
        status.setText(detail.getSanPham().getMaSP() + " — " + detail.getSanPham().getTenSP() + " • "
                + (detail.getBienThe().isEmpty() ? "Chưa có màu / size. Bấm Thêm màu / size để bổ sung."
                        : detail.getBienThe().size() + " lựa chọn màu / size"));
        refreshControls();
    }

    private static final class ProductOption {
        final String id, label;

        ProductOption(String id, String label) {
            this.id = id;
            this.label = label;
        }

        ProductOption(DuLieuSanPham row) {
            this(row.getMaSP(), row.getMaSP() + " — " + row.getTenSP());
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private void editor(DuLieuBienThe original) {
        String parent = current.getSanPham().getMaSP();
        boolean creating = original == null;
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                creating ? "Thêm biến thể" : "Sửa trạng thái biến thể",
                Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JTextField id = new JTextField(creating ? "" : original.getMaBienThe(), 18),
                color = new JTextField(creating ? "" : original.getMauSac(), 18),
                size = new JTextField(creating ? "" : original.getKichCo(), 18);
        id.setEditable(false);
        color.setEditable(creating);
        size.setEditable(creating);
        JPanel fields = ChuDeGiaoDien.card(new GridLayout(0, 2, 14, 16));
        fields.add(ChuDeGiaoDien.field("Mã biến thể", id));
        fields.add(ChuDeGiaoDien.field("Sản phẩm", new JLabel(parent)));
        fields.add(ChuDeGiaoDien.field("Màu", color));
        fields.add(ChuDeGiaoDien.field("Size", size));
        if (creating) {
            JSpinner initialStock = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
            fields.add(ChuDeGiaoDien.field("Tồn khởi tạo", initialStock));
            JCheckBox active = new JCheckBox("Đang bán", true);
            fields.add(active);
            panel.add(fields, BorderLayout.CENTER);
            JButton save = new JButton("Tạo biến thể");
            ChuDeGiaoDien.primary(save);
            panel.add(save, BorderLayout.SOUTH);
            save.addActionListener(e -> {
                try {
                    initialStock.commitEdit();
                } catch (java.text.ParseException invalid) {
                    JOptionPane.showMessageDialog(dialog, "Tồn phải là số nguyên không âm.");
                    return;
                }
                DuLieuBienThe request = new DuLieuBienThe();
                request.setMaSP(parent);
                request.setMaBienThe(id.getText().trim());
                request.setMauSac(color.getText().trim());
                request.setKichCo(size.getText().trim());
                request.setSoLuongTon(((Number) initialStock.getValue()).intValue());
                request.setTrangThai(active.isSelected());
                TacVuGiaoDien.run(dialog, save, () -> controller.variant(request, true), out -> {
                    dialog.dispose();
                    loadProduct(parent);
                });
            });
        } else {
            fields.add(ChuDeGiaoDien.field("Tồn hiện tại",
                    new JLabel(String.valueOf(original.getSoLuongTon()))));
            JCheckBox active = new JCheckBox("Đang bán", original.getTrangThai());
            fields.add(active);
            panel.add(fields, BorderLayout.CENTER);
            JButton save = new JButton("Lưu trạng thái");
            ChuDeGiaoDien.primary(save);
            panel.add(save, BorderLayout.SOUTH);
            save.addActionListener(e -> {
                DuLieuBienThe request = new DuLieuBienThe();
                request.setMaSP(parent);
                request.setMaBienThe(id.getText().trim());
                request.setMauSac(color.getText().trim());
                request.setKichCo(size.getText().trim());
                request.setSoLuongTon(original.getSoLuongTon());
                request.setTrangThai(active.isSelected());
                request.setPhienBan(original.getPhienBan());
                TacVuGiaoDien.run(dialog, save, () -> controller.variant(request, false), out -> {
                    dialog.dispose();
                    loadProduct(parent);
                });
            });
        }
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.getRootPane().setDefaultButton((JButton) panel.getComponent(panel.getComponentCount() - 1));
        dialog.setVisible(true);
    }

    private void adjustStock(DuLieuBienThe variant) {
        String parent = current.getSanPham().getMaSP();
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Điều chỉnh tồn kho",
                Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JSpinner target = new JSpinner(
                new SpinnerNumberModel(variant.getSoLuongTon(), 0, Integer.MAX_VALUE, 1));
        JTextArea reason = new JTextArea(3, 28);
        reason.setLineWrap(true);
        reason.setWrapStyleWord(true);
        JPanel fields = ChuDeGiaoDien.card(new GridLayout(0, 2, 14, 16));
        fields.add(ChuDeGiaoDien.field("Biến thể", new JLabel(variant.getMaBienThe())));
        fields.add(ChuDeGiaoDien.field("Tồn hiện tại", new JLabel(String.valueOf(variant.getSoLuongTon()))));
        fields.add(ChuDeGiaoDien.field("Số lượng tồn mới", target));
        JPanel reasonField = ChuDeGiaoDien.field("Lý do (bắt buộc, tối đa 500 ký tự)",
                new JScrollPane(reason));
        panel.add(fields, BorderLayout.NORTH);
        panel.add(reasonField, BorderLayout.CENTER);
        JButton save = new JButton("Xác nhận điều chỉnh");
        ChuDeGiaoDien.primary(save);
        panel.add(save, BorderLayout.SOUTH);
        String operationId = UUID.randomUUID().toString();
        save.addActionListener(e -> {
            try {
                target.commitEdit();
            } catch (java.text.ParseException invalid) {
                JOptionPane.showMessageDialog(dialog, "Tồn phải là số nguyên không âm.");
                return;
            }
            int amount = ((Number) target.getValue()).intValue();
            String explanation = reason.getText().trim();
            if (amount == variant.getSoLuongTon() || explanation.isEmpty() || explanation.length() > 500) {
                JOptionPane.showMessageDialog(dialog,
                        amount == variant.getSoLuongTon() ? "Số lượng mới phải khác tồn hiện tại."
                                : "Nhập lý do từ 1 đến 500 ký tự.");
                return;
            }
            String message = "Đặt tồn " + variant.getMaBienThe() + " từ " + variant.getSoLuongTon()
                    + " thành " + amount + "?";
            if (JOptionPane.showConfirmDialog(dialog, message, "Xác nhận",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            YeuCauDieuChinhTonKho request = new YeuCauDieuChinhTonKho();
            request.setMaBienThe(variant.getMaBienThe());
            request.setSoLuongMoi(BigDecimal.valueOf(amount));
            request.setExpectedVersion(BigDecimal.valueOf(variant.getPhienBan()));
            request.setLyDo(explanation);
            request.setOperationId(operationId);
            // Cùng request được dùng lại nếu phản hồi mạng bị mất; operationId ổn định suốt dialog.
            target.setEnabled(false);
            reason.setEditable(false);
            TacVuGiaoDien.run(dialog, save, () -> controller.adjustStock(request), out -> {
                dialog.dispose();
                loadProduct(parent);
            });
        });
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.getRootPane().setDefaultButton(save);
        dialog.setVisible(true);
    }
}
