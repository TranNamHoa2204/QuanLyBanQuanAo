package client.ui;

import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import com.fasterxml.jackson.databind.JsonNode;
import client.api.*;
import client.api.ManagementApi.Kind;
import common.dto.*;
import common.enums.TrangThaiHoaDon;
import common.util.InvoiceIds;

/** SwingWorker cho mọi yêu cầu mạng; không truy cập entity hoặc JPA từ client. */
public final class BanHangPanel extends JPanel implements Scrollable {
    private static final long serialVersionUID = 1L;
    private final client.controller.BanHangController api;
    private final SanPhamApi products;
    private final JTextField id = new JTextField(20), customer = new JTextField(10),
            note = new JTextField(24), product = new JTextField(18);
    private final ManagementApi management;
    private final JComboBox<String> tax = new JComboBox<>(), variant = new JComboBox<>();
    private final JComboBox<PromotionOption> promo = new JComboBox<>();
    private final JPopupMenu productPopup = new JPopupMenu();
    private final JList<ProductOption> productSuggestions = new JList<>();
    private final JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1));
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Biến thể", "Sản phẩm", "Màu", "Size", "SL", "Đơn giá", "Thành tiền"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel total = new JLabel("—"), status = new JLabel("Tạo nháp hoặc nhập mã hóa đơn để tải lại.");
    private final JLabel subtotal = UiTheme.muted("Tạm tính: —"), discount = UiTheme.muted("Giảm giá: —"),
            taxAmount = UiTheme.muted("Thuế: —");
    private final JButton create = new JButton("Tạo nháp"), fresh = new JButton("Hóa đơn mới"),
            reload = new JButton("Tải lại"), addLine = new JButton("Thêm hàng"), change = new JButton("Đổi SL"),
            remove = new JButton("Bỏ hàng"), pay = new JButton("Thanh toán"), cancel = new JButton("Hủy nháp"),
            lookup = new JButton("Tải biến thể"), taxes = new JButton("Tải thuế"), history = new JButton("Danh sách");
    private HoaDonDetailDTO current;
    private SanPhamDTO selectedProduct;
    private boolean busy;
    private boolean settingProductText;
    private int suggestionSequence;
    private final Timer suggestionTimer;
    public BanHangPanel(HoaDonApi api, SanPhamApi products) {
        this(api, products, null);
    }
    public BanHangPanel(HoaDonApi api, SanPhamApi products, ManagementApi management) {
        super(new BorderLayout(8, 8)); this.api = new client.controller.BanHangController(api); this.products = products;
        this.management = management;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));
        setPreferredSize(new Dimension(1000, 780));
        UiTheme.primary(create); UiTheme.primary(addLine); UiTheme.primary(pay); UiTheme.danger(cancel); UiTheme.danger(remove);
        id.setColumns(15); customer.setColumns(8); note.setColumns(14);
        product.putClientProperty("JTextField.placeholderText", "Nhập mã hoặc tên sản phẩm");
        id.putClientProperty("JTextField.placeholderText", "Tự tạo khi lập nháp");
        customer.putClientProperty("JTextField.placeholderText", "Khách vãng lai");
        promo.addItem(PromotionOption.none());
        promo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value == null ? "Không khuyến mãi" : value.toString(),
                        index, selected, focus);
            }
        });
        productSuggestions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productSuggestions.setVisibleRowCount(8);
        productSuggestions.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value == null ? "" : value.toString(),
                        index, selected, focus);
            }
        });
        productPopup.setFocusable(false); productPopup.add(new JScrollPane(productSuggestions));
        suggestionTimer = new Timer(350, e -> suggestProducts(product.getText().trim()));
        suggestionTimer.setRepeats(false);
        product.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() {
                if (settingProductText) { return; }
                selectedProduct = null;
                String query = product.getText().trim();
                if (query.length() < 2) { suggestionTimer.stop(); productPopup.setVisible(false); return; }
                suggestionTimer.restart();
            }
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        });
        productSuggestions.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 1) { chooseProductSuggestion(); }
            }
        });
        product.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                SwingUtilities.invokeLater(() -> { if (!productPopup.isFocusOwner() && !productSuggestions.isFocusOwner()) { productPopup.setVisible(false); } });
            }
        });
        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.add(UiTheme.heading("Quầy bán hàng", "Nháp được lưu trên máy chủ. Chỉ trừ tồn khi thanh toán."), BorderLayout.NORTH);
        JPanel invoice = UiTheme.card(new BorderLayout(0, 14));
        invoice.add(row(new JLabel("Mã hóa đơn"), id, reload, fresh, history), BorderLayout.NORTH);
        JPanel info = new JPanel(new GridLayout(1, 4, 12, 0)); info.setOpaque(false);
        info.add(UiTheme.field("Thuế áp dụng", tax)); info.add(UiTheme.field("Khách hàng (mã, tùy chọn)", customer));
        info.add(UiTheme.field("Khuyến mãi (tùy chọn)", promo)); info.add(UiTheme.field("Ghi chú", note));
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
        inputs.add(UiTheme.field("Sản phẩm (mã hoặc tên)", product), BorderLayout.WEST);
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
        total.setForeground(UiTheme.PRIMARY); figures.add(total); summary.add(figures, BorderLayout.NORTH);
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
            current = null; id.setText(InvoiceIds.newId()); customer.setText(""); promo.setSelectedIndex(0); note.setText("");
            product.setText(""); selectedProduct = null; variant.removeAllItems();
            model.setRowCount(0); total.setText("—"); subtotal.setText("Tạm tính: —"); discount.setText("Giảm giá: —");
            taxAmount.setText("Thuế: —"); status.setText("Hóa đơn mới: chọn thuế và Tạo nháp."); controls();
        });
        reload.addActionListener(e -> { String code = id.getText().trim(); run(() -> api.detail(code), this::showInvoice); });
        create.addActionListener(e -> {
            if (tax.getSelectedItem() == null) { JOptionPane.showMessageDialog(this, "Cần có loại thuế đang áp dụng trong CSDL."); return; }
            if (id.getText().trim().isEmpty()) { id.setText(InvoiceIds.newId()); }
            HoaDonCreateRequest r = new HoaDonCreateRequest(); r.setMaHD(id.getText().trim());
            r.setMaThue(((String)tax.getSelectedItem()).split(" / ")[0]); r.setMaKH(customer.getText());
            PromotionOption selectedPromo = (PromotionOption) promo.getSelectedItem();
            r.setMaKM(selectedPromo == null ? null : selectedPromo.maKM);
            r.setGhiChu(note.getText()); run(() -> api.create(r), this::showInvoice);
        });
        lookup.addActionListener(e -> {
            if (selectedProduct != null && selectedProduct.getMaSP().equals(product.getText().trim())) {
                loadVariants(selectedProduct.getMaSP());
            } else {
                String query = product.getText().trim();
                if (query.length() < 2) { JOptionPane.showMessageDialog(this, "Nhập ít nhất 2 ký tự của mã hoặc tên sản phẩm."); }
                else { suggestProducts(query); }
            }
        });
        addLine.addActionListener(e -> edit(0)); change.addActionListener(e -> edit(1)); remove.addActionListener(e -> edit(2));
        pay.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Thanh toán hóa đơn " + current.getHoaDon().getMaHD() + ": " + current.getHoaDon().getTongTien()
                    + " đồng? Sau đó không sửa được.", "Thanh toán", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                HoaDonVersionRequest r = version(); run(() -> api.pay(r), this::showInvoice);
            }
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
            tax.removeAllItems(); for (ThueDTO t : rows) { tax.addItem(t.getMaThue() + " / " + t.getTenThue() + " / " + percent(t.getPhanTram()) + "%"); }
            if (current != null) { selectInvoiceTax(current.getHoaDon()); }
            if (rows.isEmpty()) { status.setText("Chưa có thuế. Xem docs/BAN_HANG.md."); }
        });
    }
    public void loadReferenceData() {
        if (management == null) { loadTaxes(); return; }
        ManagementSearchRequest request = new ManagementSearchRequest(); request.setPage(0); request.setPageSize(100);
        run(() -> new ReferenceData(api.taxes(), management.search(Kind.PROMOTION, request)), data -> {
            tax.removeAllItems();
            for (ThueDTO t : data.taxes) {
                tax.addItem(t.getMaThue() + " / " + t.getTenThue() + " / " + percent(t.getPhanTram()) + "%");
            }
            if (current != null) { selectInvoiceTax(current.getHoaDon()); }
            promo.removeAllItems(); promo.addItem(PromotionOption.none());
            for (JsonNode row : data.promotions.getItems()) {
                LocalDate today = LocalDate.now();
                String startText = row.path("ngayBatDau").asText("");
                String endText = row.path("ngayKetThuc").asText("");
                if (!row.path("trangThai").asBoolean(false) || startText.isEmpty() || endText.isEmpty()
                        || today.isBefore(LocalDate.parse(startText)) || today.isAfter(LocalDate.parse(endText))) { continue; }
                promo.addItem(new PromotionOption(row.path("maKM").asText(),
                        row.path("tenKM").asText("Khuyến mãi"), row.path("phanTramGiam").decimalValue()));
            }
            if (current != null) { selectInvoicePromotion(current.getHoaDon()); }
            if (data.taxes.isEmpty()) { status.setText("Chưa có thuế đang áp dụng. Xem docs/BAN_HANG.md."); }
        });
    }
    private void suggestProducts(String query) {
        if (query == null || query.length() < 2 || busy) { productPopup.setVisible(false); return; }
        final int requestId = ++suggestionSequence;
        final String requested = query;
        SanPhamSearchRequest search = new SanPhamSearchRequest();
        search.setMaSP(query); search.setTenSP(query); search.setPage(0); search.setPageSize(8);
        new SwingWorker<SanPhamSearchResult, Void>() {
            @Override protected SanPhamSearchResult doInBackground() throws Exception { return products.search(search); }
            @Override protected void done() {
                if (requestId != suggestionSequence || !requested.equals(product.getText().trim()) || !product.isShowing()) { return; }
                try {
                    SanPhamSearchResult result = get(); DefaultListModel<ProductOption> options = new DefaultListModel<>();
                    for (SanPhamDTO item : result.getItems()) { options.addElement(new ProductOption(item)); }
                    productSuggestions.setModel(options);
                    if (options.isEmpty()) { productPopup.setVisible(false); return; }
                    int width = Math.max(product.getWidth(), 380);
                    productSuggestions.setFixedCellWidth(width - 20);
                    productPopup.setPopupSize(width, Math.min(8, options.size()) * 42 + 8);
                    if (productPopup.isVisible()) { productPopup.setVisible(false); }
                    productPopup.show(product, 0, product.getHeight());
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                catch (ExecutionException ex) {
                    productPopup.setVisible(false);
                    status.setText("Không tải được gợi ý sản phẩm. Kiểm tra kết nối máy chủ.");
                }
            }
        }.execute();
    }
    private void chooseProductSuggestion() {
        ProductOption option = productSuggestions.getSelectedValue();
        if (option == null) {
            Point mouse = productSuggestions.getMousePosition();
            int row = mouse == null ? -1 : productSuggestions.locationToIndex(mouse);
            if (row >= 0) { option = productSuggestions.getModel().getElementAt(row); }
        }
        if (option == null) { return; }
        selectedProduct = option.product; settingProductText = true; product.setText(selectedProduct.getMaSP()); settingProductText = false;
        productPopup.setVisible(false); loadVariants(selectedProduct.getMaSP());
    }
    private void loadVariants(String maSP) {
        run(() -> products.detail(maSP), detail -> {
            selectedProduct = detail.getSanPham(); product.setText(selectedProduct.getMaSP());
            variant.removeAllItems();
            for (BienTheDTO b : detail.getBienThe()) {
                if (b.getTrangThai()) { variant.addItem(b.getMaBienThe() + " / " + b.getMauSac() + " / " + b.getKichCo() + " / tồn " + b.getSoLuongTon()); }
            }
            status.setText("Đã chọn " + selectedProduct.getMaSP() + " / " + selectedProduct.getTenSP()
                    + ". Tồn hiển thị chỉ tham khảo; máy chủ kiểm tra lại khi thanh toán.");
        });
    }
    private void selectInvoicePromotion(HoaDonDTO invoice) {
        if (invoice.getMaKM() == null || invoice.getMaKM().trim().isEmpty()) { promo.setSelectedIndex(0); return; }
        for (int i = 0; i < promo.getItemCount(); i++) {
            if (invoice.getMaKM().equals(promo.getItemAt(i).maKM)) { promo.setSelectedIndex(i); return; }
        }
        PromotionOption snapshot = new PromotionOption(invoice.getMaKM(), "Đã chốt " + invoice.getPhanTramGiam() + "%", invoice.getPhanTramGiam());
        promo.addItem(snapshot); promo.setSelectedItem(snapshot);
    }
    private static String percent(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
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
    public void showInvoice(HoaDonDetailDTO d) {
        current = d; HoaDonDTO h = d.getHoaDon(); id.setText(h.getMaHD()); model.setRowCount(0);
        for (ChiTietHoaDonDTO c : d.getChiTiet()) {
            model.addRow(new Object[]{c.getMaBienThe(), c.getTenSP(), c.getMauSac(), c.getKichCo(), c.getSoLuong(), c.getDonGia(), c.getThanhTien()});
        }
        customer.setText(h.getMaKH()); note.setText(h.getGhiChu());
        selectInvoiceTax(h); selectInvoicePromotion(h);
        subtotal.setText("Tạm tính: " + UiTheme.money(h.getTamTinh()));
        discount.setText("Giảm giá: " + UiTheme.money(h.getTienGiam()));
        taxAmount.setText("Thuế (" + percent(h.getPhanTramThue()) + "%): " + UiTheme.money(h.getTienThue()));
        total.setText(UiTheme.money(h.getTongTien()));
        total.setToolTipText(total.getText());
        total.setFont(total.getFont().deriveFont(Font.BOLD, total.getText().length() > 16 ? 16f : 24f));
        status.setText("Hóa đơn đang mở: " + h.getMaHD() + " | " + h.getTrangThai() + " | Phiên bản " + h.getPhienBan() + " | Nhân viên " + h.getMaNV()); controls();
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
        String selected = h.getMaThue() + " / Đã chốt " + percent(h.getPhanTramThue()) + "%";
        tax.addItem(selected); tax.setSelectedItem(selected);
    }
    private void controls() {
        boolean draft = current != null && current.getHoaDon().getTrangThai() == TrangThaiHoaDon.NHAP;
        for (JButton b : new JButton[]{fresh, reload, lookup, taxes, history}) { b.setEnabled(!busy); }
        create.setEnabled(!busy && current == null);
        for (JButton b : new JButton[]{addLine, change, remove, pay, cancel}) { b.setEnabled(!busy && draft); }
        for (JTextField f : new JTextField[]{customer, note}) { f.setEnabled(!busy && current == null); }
        promo.setEnabled(!busy && current == null);
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
    private static final class PromotionOption {
        final String maKM;
        final String label;
        final BigDecimal percent;
        PromotionOption(String maKM, String label, BigDecimal percent) {
            this.maKM = maKM; this.label = label; this.percent = percent;
        }
        static PromotionOption none() { return new PromotionOption(null, "Không khuyến mãi", null); }
        @Override public String toString() {
            return maKM == null ? label : maKM + " / " + label + " / " + percent(percent) + "%";
        }
    }
    private static final class ProductOption {
        final SanPhamDTO product;
        ProductOption(SanPhamDTO product) { this.product = product; }
        @Override public String toString() { return product.getMaSP() + " / " + product.getTenSP(); }
    }
    private static final class ReferenceData {
        final java.util.List<ThueDTO> taxes;
        final ManagementSearchResult promotions;
        ReferenceData(java.util.List<ThueDTO> taxes, ManagementSearchResult promotions) {
            this.taxes = taxes; this.promotions = promotions;
        }
    }
}
