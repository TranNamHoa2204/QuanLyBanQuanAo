package client.ui;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import client.api.*;
import common.dto.*;
import common.enums.PhuongThucThanhToan;
import common.enums.TrangThaiHoaDon;
import common.util.InvoiceIds;

/** SwingWorker cho mọi yêu cầu mạng; không truy cập entity hoặc JPA từ client. */
public final class BanHangPanel extends JPanel implements Scrollable {
    private static final long serialVersionUID = 1L;
    private final client.controller.BanHangController api;
    private final SanPhamApi products;
    private final JTextField id = new JTextField(20), customer = new JTextField(10),
            promo = new JTextField(10), note = new JTextField(24), product = new JTextField(12);
    private final JComboBox<String> tax = new JComboBox<>(), variant = new JComboBox<>();
    private final JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1));
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Biến thể", "Sản phẩm", "Màu", "Size", "SL", "Đơn giá", "Thành tiền"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel total = new JLabel("—"), status = new JLabel("Tạo nháp hoặc nhập mã hóa đơn để tải lại.");
    private final JLabel subtotal = UiTheme.muted("Tạm tính: —"), discount = UiTheme.muted("Giảm giá: —"),
            taxAmount = UiTheme.muted("Thuế: —"), paymentInfo = UiTheme.muted("Chưa ghi nhận thu tiền.");
    private final JButton create = new JButton("Tạo nháp"), fresh = new JButton("Hóa đơn mới"),
            reload = new JButton("Tải lại"), addLine = new JButton("Thêm hàng"), change = new JButton("Đổi SL"),
            remove = new JButton("Bỏ hàng"), pay = new JButton("Thanh toán"), cancel = new JButton("Hủy nháp"),
            lookup = new JButton("Tải biến thể"), taxes = new JButton("Tải thuế"), history = new JButton("Danh sách");
    private HoaDonDetailDTO current;
    private boolean busy;
    public BanHangPanel(HoaDonApi api, SanPhamApi products) {
        super(new BorderLayout(8, 8)); this.api = new client.controller.BanHangController(api); this.products = products;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));
        setPreferredSize(new Dimension(1000, 780));
        UiTheme.primary(create); UiTheme.primary(addLine); UiTheme.primary(pay); UiTheme.danger(cancel); UiTheme.danger(remove);
        id.setColumns(15); customer.setColumns(8); promo.setColumns(8); note.setColumns(14); product.setColumns(8);
        product.putClientProperty("JTextField.placeholderText", "VD: SP1001");
        id.putClientProperty("JTextField.placeholderText", "Tự tạo khi lập nháp");
        customer.putClientProperty("JTextField.placeholderText", "Khách vãng lai");
        promo.putClientProperty("JTextField.placeholderText", "Không khuyến mãi");
        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.add(UiTheme.heading("Quầy bán hàng", "Nháp được lưu trên máy chủ. Chỉ trừ tồn khi thanh toán."), BorderLayout.NORTH);
        JPanel invoice = UiTheme.card(new BorderLayout(0, 14));
        invoice.add(row(new JLabel("Mã hóa đơn"), id, reload, fresh, history), BorderLayout.NORTH);
        JPanel info = new JPanel(new GridLayout(1, 4, 12, 0)); info.setOpaque(false);
        info.add(UiTheme.field("Thuế áp dụng", tax)); info.add(UiTheme.field("Khách hàng (mã, tùy chọn)", customer));
        info.add(UiTheme.field("Khuyến mãi (mã, tùy chọn)", promo)); info.add(UiTheme.field("Ghi chú", note));
        invoice.add(info, BorderLayout.CENTER); invoice.add(row(taxes, create), BorderLayout.SOUTH);
        top.add(invoice, BorderLayout.CENTER); add(top, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); UiTheme.table(table);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                setHorizontalAlignment(c >= 4 ? RIGHT : LEFT);
                if (c >= 5 && v instanceof Number) { setText(UiTheme.money((Number)v)); } return this;
            }
        });
        JScrollPane scroll = new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader());
        JPanel cart = new JPanel(new BorderLayout(0, 12));
        JPanel entry = UiTheme.card(new BorderLayout(0, 12));
        JPanel inputs = new JPanel(new BorderLayout(12, 0)); inputs.setOpaque(false);
        inputs.add(UiTheme.field("Mã sản phẩm", product), BorderLayout.WEST);
        inputs.add(UiTheme.field("Biến thể / màu / size / tồn", variant), BorderLayout.CENTER);
        qty.setPreferredSize(new Dimension(80, 38)); inputs.add(UiTheme.field("Số lượng", qty), BorderLayout.EAST);
        entry.add(inputs, BorderLayout.CENTER); entry.add(row(lookup, addLine, change, remove), BorderLayout.SOUTH);
        cart.add(entry, BorderLayout.NORTH); cart.add(scroll, BorderLayout.CENTER);
        JPanel summary = UiTheme.card(new BorderLayout(0, 20)); summary.setPreferredSize(new Dimension(270, 0));
        JPanel figures = new JPanel(); figures.setLayout(new BoxLayout(figures, BoxLayout.Y_AXIS)); figures.setOpaque(false);
        figures.add(UiTheme.title("Tóm tắt hóa đơn", 19)); figures.add(Box.createVerticalStrut(18));
        for (JLabel l : new JLabel[]{subtotal, discount, taxAmount}) { figures.add(l); figures.add(Box.createVerticalStrut(12)); }
        figures.add(new JSeparator()); figures.add(Box.createVerticalStrut(12)); figures.add(UiTheme.muted("TỔNG THANH TOÁN"));
        figures.add(Box.createVerticalStrut(8)); total.setFont(total.getFont().deriveFont(Font.BOLD, 24f));
        total.setForeground(UiTheme.PRIMARY); figures.add(total); figures.add(Box.createVerticalStrut(12));
        paymentInfo.setText("Chưa ghi nhận thu tiền."); figures.add(paymentInfo); summary.add(figures, BorderLayout.NORTH);
        summary.setToolTipText("Giá và tổng tiền do server tính. Hóa đơn đã thanh toán không thể chỉnh sửa.");
        JPanel checkout = new JPanel(new GridLayout(0, 1, 0, 10)); checkout.setOpaque(false);
        checkout.add(pay); checkout.add(cancel); summary.add(checkout, BorderLayout.SOUTH);
        JPanel center = new JPanel(new BorderLayout(16, 0)); center.add(cart, BorderLayout.CENTER); center.add(summary, BorderLayout.EAST);
        add(center, BorderLayout.CENTER); status.setForeground(UiTheme.MUTED); status.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        add(status, BorderLayout.SOUTH);
        taxes.addActionListener(e -> loadTaxes());
        fresh.addActionListener(e -> {
            if (current != null && current.getHoaDon().getTrangThai() == TrangThaiHoaDon.NHAP
                    && JOptionPane.showConfirmDialog(this, "Nháp hiện tại vẫn được lưu. Mở hóa đơn mới?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) { return; }
            current = null; id.setText(InvoiceIds.newId()); customer.setText(""); promo.setText(""); note.setText("");
            model.setRowCount(0); total.setText("—"); subtotal.setText("Tạm tính: —"); discount.setText("Giảm giá: —");
            taxAmount.setText("Thuế: —"); paymentInfo.setText("Chưa ghi nhận thu tiền.");
            status.setText("Hóa đơn mới: chọn thuế và Tạo nháp."); controls();
        });
        reload.addActionListener(e -> { String code = id.getText().trim(); run(() -> api.detail(code), this::showInvoice); });
        create.addActionListener(e -> {
            if (tax.getSelectedItem() == null) { JOptionPane.showMessageDialog(this, "Cần có loại thuế đang áp dụng trong CSDL."); return; }
            if (id.getText().trim().isEmpty()) { id.setText(InvoiceIds.newId()); }
            HoaDonCreateRequest r = new HoaDonCreateRequest(); r.setMaHD(id.getText().trim());
            r.setMaThue(((String)tax.getSelectedItem()).split(" / ")[0]); r.setMaKH(customer.getText());
            r.setMaKM(promo.getText()); r.setGhiChu(note.getText()); run(() -> api.create(r), this::showInvoice);
        });
        lookup.addActionListener(e -> { String code = product.getText().trim(); run(() -> products.detail(code), d -> {
            variant.removeAllItems();
            for (BienTheDTO b : d.getBienThe()) {
                if (b.getTrangThai()) { variant.addItem(b.getMaBienThe() + " / " + b.getMauSac() + " / " + b.getKichCo() + " / tồn " + b.getSoLuongTon()); }
            }
            status.setText("Tồn hiển thị chỉ tham khảo; server kiểm tra lại khi thanh toán.");
        }); });
        addLine.addActionListener(e -> edit(0)); change.addActionListener(e -> edit(1)); remove.addActionListener(e -> edit(2));
        pay.addActionListener(e -> {
            HoaDonThanhToanRequest r = paymentRequest();
            if (r != null) { run(() -> api.pay(r), this::showInvoice); }
        });
        cancel.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Hủy nháp?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                HoaDonVersionRequest r = version(); run(() -> api.cancel(r), this::showInvoice);
            }
        });
        history.addActionListener(e -> history(0));
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow(); if (row >= 0) { qty.setValue(model.getValueAt(row, 4)); }
        });
        controls();
    }
    private static JPanel row(Component... components) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); p.setOpaque(false);
        for (Component c : components) { p.add(c); } return p;
    }
    public void loadTaxes() {
        run(api::taxes, rows -> {
            tax.removeAllItems(); for (ThueDTO t : rows) { tax.addItem(t.getMaThue() + " / " + t.getTenThue() + " / " + t.getPhanTram() + "%"); }
            if (current != null) { selectInvoiceTax(current.getHoaDon()); }
            if (rows.isEmpty()) { status.setText("Chưa có thuế. Xem docs/BAN_HANG.md."); }
        });
    }
    private void edit(int mode) {
        String code;
        if (mode == 0) {
            if (variant.getSelectedItem() == null) { return; } code = ((String)variant.getSelectedItem()).split(" / ")[0];
        } else {
            int row = table.getSelectedRow(); if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn dòng hàng trước."); return; }
            code = (String)model.getValueAt(row, 0);
        }
        HoaDonLineRequest r = new HoaDonLineRequest(); r.setMaHD(current.getHoaDon().getMaHD());
        r.setPhienBan(current.getHoaDon().getPhienBan()); r.setMaBienThe(code); r.setSoLuong(((Number)qty.getValue()).intValue());
        run(() -> api.line(r, mode), this::showInvoice);
    }
    private HoaDonVersionRequest version() {
        HoaDonVersionRequest r = new HoaDonVersionRequest(); r.setMaHD(current.getHoaDon().getMaHD());
        r.setPhienBan(current.getHoaDon().getPhienBan()); return r;
    }
    private HoaDonThanhToanRequest paymentRequest() {
        if (current == null || current.getHoaDon().getTrangThai() != TrangThaiHoaDon.NHAP) { return null; }
        HoaDonDTO invoice = current.getHoaDon();
        BigDecimal reviewedTotal = invoice.getTongTien();
        JRadioButton cash = new JRadioButton("Tiền mặt", true);
        JRadioButton transfer = new JRadioButton("Chuyển khoản");
        ButtonGroup methods = new ButtonGroup(); methods.add(cash); methods.add(transfer);
        JTextField tendered = new JTextField(16);
        JCheckBox transferConfirmed = new JCheckBox("Nhân viên xác nhận đã nhận tiền chuyển khoản");
        JLabel totalPreview = new JLabel("Tổng server đã tính: " + UiTheme.money(reviewedTotal));
        JLabel transferNotice = UiTheme.muted("<html><div style='width:340px'>Ứng dụng chỉ ghi nhận xác nhận của nhân viên; không xác minh giao dịch ngân hàng.</div></html>");
        Runnable updateMode = () -> {
            tendered.setEnabled(cash.isSelected()); transferConfirmed.setEnabled(transfer.isSelected());
            transferNotice.setEnabled(transfer.isSelected());
        };
        cash.addActionListener(e -> updateMode.run()); transfer.addActionListener(e -> updateMode.run());
        JPanel methodRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        methodRow.setOpaque(false); methodRow.add(cash); methodRow.add(transfer);
        JPanel form = new JPanel(); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS)); form.setOpaque(false);
        form.add(totalPreview); form.add(Box.createVerticalStrut(10)); form.add(methodRow);
        form.add(UiTheme.field("Tiền khách đưa (đồng)", tendered));
        form.add(transferConfirmed); form.add(transferNotice);
        Object[] options = {"Xác nhận thu tiền", "Hủy"};
        while (true) {
            updateMode.run();
            int choice = JOptionPane.showOptionDialog(this, form, "Thu tiền hóa đơn " + invoice.getMaHD(),
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (choice != 0) { return null; }
            HoaDonThanhToanRequest request = new HoaDonThanhToanRequest();
            request.setMaHD(invoice.getMaHD()); request.setPhienBan(invoice.getPhienBan());
            request.setTongTienDaXem(reviewedTotal);
            if (cash.isSelected()) {
                BigDecimal amount;
                try {
                    amount = new BigDecimal(tendered.getText().trim());
                    amount.setScale(2, RoundingMode.UNNECESSARY);
                } catch (NumberFormatException | ArithmeticException ex) {
                    JOptionPane.showMessageDialog(this, "Nhập tiền mặt hợp lệ, tối đa 2 chữ số thập phân.",
                            "Thông tin chưa hợp lệ", JOptionPane.WARNING_MESSAGE); continue;
                }
                if (amount.compareTo(reviewedTotal) < 0) {
                    JOptionPane.showMessageDialog(this, "Tiền khách đưa phải lớn hơn hoặc bằng tổng thanh toán.",
                            "Thông tin chưa hợp lệ", JOptionPane.WARNING_MESSAGE); continue;
                }
                request.setPhuongThucThanhToan(PhuongThucThanhToan.TIEN_MAT);
                request.setTienKhachDua(amount);
            } else {
                if (!transferConfirmed.isSelected()) {
                    JOptionPane.showMessageDialog(this, "Cần xác nhận đã nhận tiền chuyển khoản. Ứng dụng không xác minh ngân hàng.",
                            "Chưa xác nhận", JOptionPane.WARNING_MESSAGE); continue;
                }
                request.setPhuongThucThanhToan(PhuongThucThanhToan.CHUYEN_KHOAN);
                request.setNhanVienXacNhanChuyenKhoan(Boolean.TRUE);
            }
            return request;
        }
    }
    public void showInvoice(HoaDonDetailDTO d) {
        current = d; HoaDonDTO h = d.getHoaDon(); id.setText(h.getMaHD()); model.setRowCount(0);
        for (ChiTietHoaDonDTO c : d.getChiTiet()) {
            model.addRow(new Object[]{c.getMaBienThe(), c.getTenSP(), c.getMauSac(), c.getKichCo(), c.getSoLuong(), c.getDonGia(), c.getThanhTien()});
        }
        customer.setText(h.getMaKH()); promo.setText(h.getMaKM()); note.setText(h.getGhiChu());
        selectInvoiceTax(h);
        subtotal.setText("Tạm tính: " + UiTheme.money(h.getTamTinh()));
        discount.setText("Giảm giá: " + UiTheme.money(h.getTienGiam()));
        taxAmount.setText("Thuế (" + h.getPhanTramThue() + "%): " + UiTheme.money(h.getTienThue()));
        total.setText(UiTheme.money(h.getTongTien()));
        total.setToolTipText(total.getText());
        total.setFont(total.getFont().deriveFont(Font.BOLD, total.getText().length() > 16 ? 16f : 24f));
        paymentInfo.setText("<html><div style='width:230px'>" + paymentSummary(h) + "</div></html>");
        status.setText("Hóa đơn đang mở: " + h.getMaHD() + " | " + h.getTrangThai() + " | Phiên bản " + h.getPhienBan() + " | Nhân viên " + h.getMaNV()); controls();
    }
    private static String paymentSummary(HoaDonDTO invoice) {
        if (invoice.getTrangThai() != TrangThaiHoaDon.DA_THANH_TOAN) { return "Chưa ghi nhận thu tiền."; }
        if (invoice.getPhuongThucThanhToan() == null) { return "Thông tin thu tiền: chưa có dữ liệu (hóa đơn lịch sử)."; }
        if (invoice.getPhuongThucThanhToan() == PhuongThucThanhToan.TIEN_MAT) {
            if (invoice.getTienKhachDua() == null || invoice.getTienThua() == null) {
                return "Tiền mặt: dữ liệu thu tiền chưa đầy đủ.";
            }
            return "Tiền mặt | Khách đưa: " + UiTheme.money(invoice.getTienKhachDua())
                    + " | Tiền thừa: " + UiTheme.money(invoice.getTienThua());
        }
        if (Boolean.TRUE.equals(invoice.getNhanVienXacNhanChuyenKhoan())) {
            return "Chuyển khoản thủ công: nhân viên xác nhận đã nhận; ứng dụng không xác minh ngân hàng.";
        }
        return "Chuyển khoản: dữ liệu xác nhận chưa đầy đủ.";
    }
    private void history(int page) {
        HoaDonSearchRequest r = new HoaDonSearchRequest(); r.setPage(page); r.setPageSize(20);
        run(() -> api.search(r), d -> {
            String[] rows = d.getItems().stream().map(h -> h.getMaHD() + " / " + h.getNgayLap() + " / " + h.getTrangThai() + " / " + h.getTongTien()).toArray(String[]::new);
            JList<String> list = new JList<>(rows); JScrollPane scroll = new JScrollPane(list); scroll.setPreferredSize(new Dimension(650, 350));
            int choice = JOptionPane.showOptionDialog(this, scroll, "Trang " + (page + 1) + " - Tổng " + d.getTotal(),
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"Mở", "Trước", "Sau", "Đóng"}, "Mở");
            if (choice == 0 && list.getSelectedIndex() >= 0) {
                String code = d.getItems().get(list.getSelectedIndex()).getMaHD(); run(() -> api.detail(code), this::showInvoice);
            } else if (choice == 1 && page > 0) { history(page - 1); }
            else if (choice == 2 && (page + 1L) * 20 < d.getTotal()) { history(page + 1); }
        });
    }
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 24; }
    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(24, (orientation == SwingConstants.VERTICAL ? visible.height : visible.width) - 24);
    }
    @Override public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport && getParent().getWidth() >= 900;
    }
    @Override public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport && getParent().getHeight() >= 780;
    }
    private void selectInvoiceTax(HoaDonDTO h) {
        for (int i = 0; i < tax.getItemCount(); i++) {
            if (tax.getItemAt(i).startsWith(h.getMaThue() + " / ")) { tax.setSelectedIndex(i); return; }
        }
        String selected = h.getMaThue() + " / Đã chốt " + h.getPhanTramThue() + "%";
        tax.addItem(selected); tax.setSelectedItem(selected);
    }
    private void controls() {
        boolean draft = current != null && current.getHoaDon().getTrangThai() == TrangThaiHoaDon.NHAP;
        for (JButton b : new JButton[]{fresh, reload, lookup, taxes, history}) { b.setEnabled(!busy); }
        create.setEnabled(!busy && current == null);
        for (JButton b : new JButton[]{addLine, change, remove, pay, cancel}) { b.setEnabled(!busy && draft); }
        for (JTextField f : new JTextField[]{customer, promo, note}) { f.setEnabled(!busy && current == null); }
        tax.setEnabled(!busy && current == null); id.setEnabled(!busy); product.setEnabled(!busy);
        variant.setEnabled(!busy); qty.setEnabled(!busy);
    }
    private <T> void run(Callable<T> job, Consumer<T> success) {
        if (busy) { return; } busy = true; controls();
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return job.call(); }
            @Override protected void done() {
                busy = false; controls();
                try { success.accept(get()); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    String message = cause instanceof ApiException ? cause.getMessage()
                            : "Không nhận được phản hồi. Giữ mã hóa đơn và Tải lại trước khi thử tiếp.";
                    status.setText(message); JOptionPane.showMessageDialog(BanHangPanel.this, message, "Thông báo", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }
}
