package client.gui.banhang;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import client.api.GoiDichVuHoaDon;
import client.api.GoiDichVuKhachHang;
import client.api.GoiDichVuSanPham;
import client.api.NgoaiLeGoiDichVu;
import client.controllers.DieuKhienBanHang;
import client.gui.components.ChuDeGiaoDien;
import client.gui.components.TienIchBangSanPham;
import client.gui.inchungtu.BanInChungTu;
import client.gui.inchungtu.HopThoaiInChungTu;
import client.utils.TaiAnh;
import common.dto.danhmuc.DuLieuThue;
import common.dto.hoadon.DuLieuChiTietHoaDon;
import common.dto.hoadon.DuLieuDayDuHoaDon;
import common.dto.hoadon.DuLieuHoaDon;
import common.dto.hoadon.YeuCauDongHoaDon;
import common.dto.hoadon.YeuCauGanKhachHangHoaDon;
import common.dto.hoadon.YeuCauPhienBanHoaDon;
import common.dto.hoadon.YeuCauTaoHoaDon;
import common.dto.hoadon.YeuCauThanhToanHoaDon;
import common.dto.hoadon.YeuCauTraCuuHoaDon;
import common.dto.khachhang.DuLieuKhachHang;
import common.dto.khachhang.YeuCauTraCuuKhachHang;
import common.dto.sanpham.DuLieuBienThe;
import common.dto.sanpham.DuLieuAnhSanPham;
import common.dto.sanpham.DuLieuDayDuSanPham;
import common.dto.sanpham.DuLieuSanPham;
import common.dto.sanpham.KetQuaTraCuuSanPham;
import common.dto.sanpham.YeuCauTraCuuSanPham;
import common.enums.NguonDuLieuHoaDon;
import common.enums.PhuongThucThanhToan;
import common.enums.TrangThaiHoaDon;
import common.protocol.MaLoi;
import common.utils.SinhMaHoaDon;

/** SwingWorker cho mọi yêu cầu mạng; không truy cập entity hoặc JPA từ client. */
public final class KhungBanHang extends JPanel implements Scrollable {
    private static final long serialVersionUID = 1L;
    private static final int CATALOG_PAGE_SIZE = 20;
    private final DieuKhienBanHang api;
    private final GoiDichVuSanPham products;
    private final GoiDichVuKhachHang customers;
    private final JTextField promo = new JTextField(10), note = new JTextField(14),
            catalogQuery = new JTextField(), customerQuery = new JTextField(), tendered = new JTextField(10);
    private final JLabel customerSummary = ChuDeGiaoDien.muted("Khách lẻ");
    private final JComboBox<String> tax = new JComboBox<>(), method = new JComboBox<>(
            new String[] { "Tiền mặt", "Chuyển khoản" });
    private final JComboBox<DuLieuBienThe> variant = new JComboBox<>();
    private final JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1));
    private final JCheckBox transferConfirmed = new JCheckBox("Đã nhận tiền chuyển khoản");
    private final JLabel selectedProduct = ChuDeGiaoDien.title("Chọn sản phẩm ở danh sách trên", 14),
            orderCode = ChuDeGiaoDien.muted("Đơn mới"), changeDue = ChuDeGiaoDien.title("—", 15);
    private final JLabel productPhoto = new JLabel("—", SwingConstants.CENTER);
    private final JPanel checkoutFields = new JPanel(new java.awt.CardLayout()),
            orderOptions = new JPanel(new GridLayout(1, 3, 8, 0));
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "Biến thể", "Sản phẩm", "Màu", "Size", "SL", "Đơn giá", "Thành tiền" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = ChuDeGiaoDien.tableWithHorizontalScroll(model, 88, 130, 64, 42, 38, 88, 95);
    private final DefaultTableModel catalogModel = TienIchBangSanPham.model(
            new String[] { "Mã SP", "Sản phẩm", "Đơn giá", "Tồn" },
            new Class<?>[] { String.class, String.class, BigDecimal.class, Long.class });
    private final JTable catalog = ChuDeGiaoDien.tableWithHorizontalScroll(catalogModel, 72, 178, 87, 63);
    private final JLabel catalogStatus = ChuDeGiaoDien.muted("Danh mục sản phẩm"),
            cartTitle = ChuDeGiaoDien.title("Giỏ hàng", 15);
    private final JLabel total = new JLabel("—"),
            status = new JLabel("Chọn sản phẩm, màu và size để thêm vào giỏ.");
    private final JLabel subtotal = new JLabel("—"), discount = new JLabel("—"),
            taxAmount = new JLabel("—"), taxCaption = ChuDeGiaoDien.muted("Thuế"),
            paymentInfo = ChuDeGiaoDien.muted(" ");
    private final JButton fresh = new JButton("Đơn mới"), reload = new JButton("Kiểm tra lại đơn"),
            addLine = new JButton("+ Thêm vào giỏ"), increase = new JButton("+"), decrease = new JButton("−"),
            change = new JButton("Đổi số lượng"), remove = new JButton("Xóa khỏi giỏ"),
            pay = new JButton("Thanh toán (F9)"), cancel = new JButton("Hủy hóa đơn"),
            taxes = new JButton("Thử lại thuế"), options = new JButton("Tùy chọn đơn"),
            history = new JButton("Hóa đơn đã lưu"), printInvoice = new JButton("In hóa đơn…"),
            chooseCustomer = new JButton("Tra cứu"),
            createCustomer = new JButton("+ Khách mới"), clearCustomer = new JButton("Khách lẻ"),
            search = new JButton("Tìm"),
            previous = new JButton("‹"), next = new JButton("›");
    private DuLieuDayDuHoaDon current;
    private YeuCauTaoHoaDon pendingCreate;
    private boolean needsCheck;
    private DuLieuKhachHang selectedCustomer;
    private boolean busy;
    private boolean taxesLoaded;
    private String catalogKeyword = "";
    private int catalogPage;
    private long catalogTotal;
    private boolean catalogLoaded;
    private String selectedProductCode;
    private boolean loadingProduct;

    public KhungBanHang(GoiDichVuHoaDon api, GoiDichVuSanPham products) {
        this(api, products, null);
    }

    public KhungBanHang(GoiDichVuHoaDon api, GoiDichVuSanPham products, GoiDichVuKhachHang customers) {
        super(new BorderLayout(8, 8));
        this.api = new DieuKhienBanHang(api);
        this.products = products;
        this.customers = customers;
        setBorder(BorderFactory.createEmptyBorder(6, 16, 12, 16));
        for (JButton button : new JButton[] { fresh, reload, addLine, increase, decrease, change, remove, pay, cancel,
                options, taxes, history, chooseCustomer, createCustomer, clearCustomer, search, previous, next }) {
            button.setMargin(new Insets(3, 10, 3, 10));
            button.putClientProperty("JComponent.minimumHeight", 32);
        }
        ChuDeGiaoDien.primary(fresh);
        ChuDeGiaoDien.primary(search);
        ChuDeGiaoDien.primary(addLine);
        ChuDeGiaoDien.danger(cancel);
        ChuDeGiaoDien.danger(remove);
        pay.setBackground(new Color(0xFFD34E));
        pay.setForeground(ChuDeGiaoDien.INK);
        pay.setFont(pay.getFont().deriveFont(Font.BOLD, 13f));
        pay.setToolTipText("Kiểm tra và xác nhận thu tiền (F9)");
        increase.getAccessibleContext().setAccessibleName("Tăng số lượng");
        increase.setToolTipText("Tăng số lượng dòng đang chọn");
        decrease.getAccessibleContext().setAccessibleName("Giảm số lượng");
        decrease.setToolTipText("Giảm số lượng dòng đang chọn");
        remove.setToolTipText("Xóa dòng đang chọn khỏi giỏ hàng");
        reload.setToolTipText("Đối chiếu lại đơn với máy chủ sau khi mất phản hồi");
        fresh.setToolTipText("Bắt đầu đơn khác. Hóa đơn đang mở vẫn được lưu.");
        catalogQuery.putClientProperty("JTextField.placeholderText", "Tên hoặc mã sản phẩm");
        customerQuery.putClientProperty("JTextField.placeholderText", "Mã, tên hoặc số điện thoại");
        tendered.putClientProperty("JTextField.placeholderText", "VD: 500.000");
        catalogQuery.getAccessibleContext().setAccessibleName("Tìm theo tên hoặc mã sản phẩm");
        customerQuery.getAccessibleContext().setAccessibleName("Tìm khách hàng");
        promo.putClientProperty("JTextField.placeholderText", "Mã khuyến mãi");
        note.putClientProperty("JTextField.placeholderText", "Tùy chọn");
        variant.setPrototypeDisplayValue(new DuLieuBienThe());
        variant.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                String label = "Chọn màu và size";
                if (value instanceof DuLieuBienThe) {
                    DuLieuBienThe item = (DuLieuBienThe) value;
                    label = item.getMauSac() == null ? "Màu / size · Tồn kho"
                            : item.getMauSac() + " / " + item.getKichCo() + " · "
                                    + (item.getSoLuongTon() == 0 ? "Hết hàng" : "Tồn " + item.getSoLuongTon());
                }
                return super.getListCellRendererComponent(list, label, index, selected, focus);
            }
        });
        tax.setPrototypeDisplayValue("Thuế đang áp dụng");
        qty.setPreferredSize(new Dimension(76, 36));

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints column = new GridBagConstraints();
        column.gridy = 0;
        column.weighty = 1;
        column.fill = GridBagConstraints.BOTH;
        column.gridx = 0;
        column.weightx = 0.42;
        column.insets = new Insets(0, 0, 0, 7);
        content.add(buildCatalog(), column);
        column.gridx = 1;
        column.weightx = 0.58;
        column.insets = new Insets(0, 7, 0, 0);
        content.add(buildOrder(), column);
        add(content, BorderLayout.CENTER);
        status.setForeground(ChuDeGiaoDien.MUTED);
        status.setBorder(BorderFactory.createEmptyBorder(3, 2, 0, 2));
        add(status, BorderLayout.SOUTH);
        registerKeyboardAction(e -> pay.doClick(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), WHEN_IN_FOCUSED_WINDOW);
        search.addActionListener(e -> searchCatalog(catalogQuery.getText().trim(), 0));
        catalogQuery.addActionListener(e -> search.doClick());
        previous.addActionListener(e -> searchCatalog(catalogKeyword, catalogPage - 1));
        next.addActionListener(e -> searchCatalog(catalogKeyword, catalogPage + 1));
        customerQuery.addActionListener(e -> chooseCustomer.doClick());
        variant.addActionListener(e -> controls());
        catalog.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectProduct();
            }
        });
        TienIchBangSanPham.onOpenRow(catalog, () -> variant.requestFocusInWindow());
        taxes.addActionListener(e -> loadTaxes());
        chooseCustomer.addActionListener(e -> chooseCustomer());
        createCustomer.addActionListener(e -> createCustomerInline());
        clearCustomer.addActionListener(e -> assignCustomer(null));
        fresh.addActionListener(e -> newOrder());
        options.addActionListener(e -> {
            boolean visible = !orderOptions.isVisible();
            orderOptions.setVisible(visible);
            options.setText(visible ? "Ẩn tùy chọn" : "Tùy chọn đơn");
            revalidate();
        });
        reload.addActionListener(e -> checkOrder());
        addLine.addActionListener(e -> addSelectedProduct());
        change.addActionListener(e -> changeQuantity());
        decrease.addActionListener(e -> editQuantity(-1));
        increase.addActionListener(e -> editQuantity(1));
        remove.addActionListener(e -> editLine(selectedLine(), 0, 2));
        pay.addActionListener(e -> {
            YeuCauThanhToanHoaDon request = paymentRequest();
            if (request != null) {
                runWrite(() -> api.pay(request), this::showInvoice);
            }
        });
        cancel.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Hủy hóa đơn đang mở?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                YeuCauPhienBanHoaDon request = version();
                runWrite(() -> api.cancel(request), this::showInvoice);
            }
        });
        history.addActionListener(e -> openInvoiceHistory());
        printInvoice.addActionListener(e -> HopThoaiInChungTu.open(this, BanInChungTu.hoaDon(current)));
        table.getSelectionModel().addListSelectionListener(e -> controls());
        TienIchBangSanPham.onOpenRow(table, () -> change.doClick());
        method.addActionListener(e -> updateCheckout());
        tendered.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateChange();
            }
        });
        tendered.addActionListener(e -> pay.doClick());
        controls();
    }

    private JPanel buildCatalog() {
        JPanel card = ChuDeGiaoDien.card(new BorderLayout(0, 10), 14);
        JPanel filter = new JPanel(new BorderLayout(8, 0));
        filter.setOpaque(false);
        filter.add(catalogQuery, BorderLayout.CENTER);
        filter.add(search, BorderLayout.EAST);
        card.add(stack(ChuDeGiaoDien.title("Sản phẩm", 16), filter), BorderLayout.NORTH);

        TienIchBangSanPham.configure(catalog);
        catalog.setAutoCreateRowSorter(false);
        catalog.setDefaultRenderer(Long.class, new javax.swing.table.DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focused, int r, int c) {
                super.getTableCellRendererComponent(t, value, selected, focused, r, c);
                boolean inStock = value instanceof Number && ((Number) value).longValue() > 0;
                setText(inStock ? value.toString() : "Hết hàng");
                setHorizontalAlignment(CENTER);
                if (!selected) {
                    setForeground(inStock ? new Color(0x137356) : new Color(0xA63B48));
                }
                return this;
            }
        });
        card.add(tableScroll(catalog, 410, 170), BorderLayout.CENTER);

        previous.setToolTipText("Trang sản phẩm trước");
        next.setToolTipText("Trang sản phẩm sau");
        previous.getAccessibleContext().setAccessibleName("Trang sản phẩm trước");
        next.getAccessibleContext().setAccessibleName("Trang sản phẩm sau");
        JPanel pagination = new JPanel(new BorderLayout());
        pagination.setOpaque(false);
        pagination.add(catalogStatus, BorderLayout.WEST);
        pagination.add(row(previous, next), BorderLayout.EAST);

        JPanel selection = new JPanel(new BorderLayout(8, 0));
        selection.setOpaque(false);
        selection.add(ChuDeGiaoDien.field("Màu và size", variant), BorderLayout.CENTER);
        selection.add(ChuDeGiaoDien.field("Số lượng", qty), BorderLayout.EAST);
        JPanel picked = ChuDeGiaoDien.card(new BorderLayout(10, 0), 10);
        productPhoto.setPreferredSize(new Dimension(64, 66));
        productPhoto.setOpaque(true);
        productPhoto.setBackground(ChuDeGiaoDien.BACKGROUND);
        picked.add(productPhoto, BorderLayout.WEST);
        JPanel productDetails = new JPanel(new BorderLayout(0, 6));
        productDetails.setOpaque(false);
        productDetails.add(selectedProduct, BorderLayout.NORTH);
        productDetails.add(selection, BorderLayout.CENTER);
        picked.add(productDetails, BorderLayout.CENTER);
        card.add(stack(pagination, picked, addLine), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildOrder() {
        JPanel order = ChuDeGiaoDien.card(new BorderLayout(0, 8), 14);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(ChuDeGiaoDien.title("Đơn hàng", 16), BorderLayout.WEST);
        heading.add(row(printInvoice, history, fresh), BorderLayout.EAST);

        JPanel customerLookup = new JPanel(new BorderLayout(6, 0));
        customerLookup.setOpaque(false);
        customerLookup.add(customerQuery, BorderLayout.CENTER);
        customerLookup.add(row(chooseCustomer, createCustomer, clearCustomer), BorderLayout.EAST);
        JPanel customer = new JPanel(new BorderLayout());
        customer.setBackground(ChuDeGiaoDien.SOFT);
        customer.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
        customer.add(customerSummary);
        orderOptions.setOpaque(false);
        orderOptions.add(ChuDeGiaoDien.field("Thuế áp dụng", tax));
        orderOptions.add(ChuDeGiaoDien.field("Mã khuyến mãi", promo));
        orderOptions.add(ChuDeGiaoDien.field("Ghi chú", note));
        orderOptions.setVisible(false);
        JPanel meta = new JPanel(new BorderLayout());
        meta.setOpaque(false);
        meta.add(orderCode, BorderLayout.CENTER);
        meta.add(row(taxes, reload, options), BorderLayout.EAST);
        order.add(stack(heading, ChuDeGiaoDien.field("Khách hàng", customerLookup), customer, meta,
                orderOptions), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ChuDeGiaoDien.table(table);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focused, int r, int c) {
                super.getTableCellRendererComponent(t, value, selected, focused, r, c);
                setHorizontalAlignment(c >= 4 ? RIGHT : LEFT);
                if (c >= 5 && value instanceof Number) {
                    setText(ChuDeGiaoDien.money((Number) value));
                }
                return this;
            }
        });
        JPanel cart = new JPanel(new BorderLayout(0, 5));
        cart.setOpaque(false);
        JPanel cartHeading = new JPanel(new BorderLayout());
        cartHeading.setOpaque(false);
        cartHeading.add(cartTitle, BorderLayout.CENTER);
        cartHeading.add(row(decrease, increase, change, remove), BorderLayout.EAST);
        cart.add(cartHeading, BorderLayout.NORTH);
        cart.add(tableScroll(table, 548, 116), BorderLayout.CENTER);
        order.add(cart, BorderLayout.CENTER);
        order.add(buildSummary(), BorderLayout.SOUTH);
        return order;
    }

    private JPanel buildSummary() {
        JPanel amounts = new JPanel(new GridLayout(0, 2, 6, 2));
        amounts.setOpaque(false);
        amounts.add(ChuDeGiaoDien.muted("Tạm tính"));
        amounts.add(subtotal);
        amounts.add(ChuDeGiaoDien.muted("Giảm giá"));
        amounts.add(discount);
        amounts.add(taxCaption);
        amounts.add(taxAmount);
        for (JLabel amount : new JLabel[] { subtotal, discount, taxAmount, total }) {
            amount.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        JPanel payable = new JPanel(new BorderLayout(8, 0));
        payable.setBackground(ChuDeGiaoDien.SOFT);
        payable.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        payable.add(ChuDeGiaoDien.title("Khách cần thanh toán", 16), BorderLayout.WEST);
        total.setForeground(ChuDeGiaoDien.PRIMARY);
        total.setFont(total.getFont().deriveFont(Font.BOLD, 24f));
        payable.add(total, BorderLayout.CENTER);
        JPanel cash = new JPanel(new GridLayout(1, 2, 8, 0));
        cash.setOpaque(false);
        cash.add(ChuDeGiaoDien.field("Tiền khách đưa", tendered));
        cash.add(ChuDeGiaoDien.field("Tiền thừa", changeDue));
        JPanel transfer = new JPanel(new BorderLayout());
        transfer.setOpaque(false);
        transfer.add(transferConfirmed, BorderLayout.CENTER);
        checkoutFields.setOpaque(false);
        checkoutFields.add(cash, "cash");
        checkoutFields.add(transfer, "transfer");
        JPanel checkout = new JPanel(new BorderLayout(10, 0));
        checkout.setOpaque(false);
        checkout.add(ChuDeGiaoDien.field("Phương thức", method), BorderLayout.WEST);
        checkout.add(checkoutFields, BorderLayout.CENTER);
        JPanel actions = new JPanel(new BorderLayout(8, 0));
        actions.setOpaque(false);
        actions.add(cancel, BorderLayout.WEST);
        actions.add(pay, BorderLayout.CENTER);
        return stack(amounts, payable, checkout, paymentInfo, actions);
    }

    private static JScrollPane tableScroll(JTable view, int width, int height) {
        JScrollPane pane = ChuDeGiaoDien.tableScroll(view);
        pane.setPreferredSize(new Dimension(width, height));
        return pane;
    }

    private static JPanel stack(JComponent... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                panel.add(Box.createVerticalStrut(6));
            }
            components[i].setAlignmentX(LEFT_ALIGNMENT);
            panel.add(components[i]);
        }
        return panel;
    }

    private static JPanel row(Component... components) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        for (Component c : components) {
            p.add(c);
        }
        return p;
    }

    private void searchCatalog(String keyword, int page) {
        YeuCauTraCuuSanPham request = new YeuCauTraCuuSanPham();
        request.setTenSP(keyword);
        request.setPage(page);
        request.setPageSize(CATALOG_PAGE_SIZE);
        run(() -> findCatalog(request, keyword), result -> {
            catalog.clearSelection();
            catalogModel.setRowCount(0);
            for (DuLieuSanPham item : result.getItems()) {
                catalogModel.addRow(new Object[] { item.getMaSP(), item.getTenSP(), item.getGiaBan(),
                        item.getTongTon() });
            }
            catalogKeyword = keyword;
            catalogPage = page;
            catalogTotal = result.getTotal();
            catalogLoaded = true;
            catalogStatus.setText(catalogTotal == 0 ? "Không tìm thấy sản phẩm"
                    : String.format("Trang %d / %d  ·  %d sản phẩm", page + 1,
                            (catalogTotal - 1) / CATALOG_PAGE_SIZE + 1, catalogTotal));
            catalog.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
            if (!keyword.isEmpty() && catalogModel.getRowCount() == 1) {
                catalog.setRowSelectionInterval(0, 0);
            }
            controls();
        });
    }

    public void loadTaxes() {
        run(api::taxes, rows -> {
            tax.removeAllItems();
            for (DuLieuThue t : rows) {
                tax.addItem(t.getMaThue() + " / " + t.getTenThue() + " / " + t.getPhanTram() + "%");
            }
            taxesLoaded = true;
            if (current != null) {
                selectInvoiceTax(current.getHoaDon());
            }
            if (rows.isEmpty()) {
                status.setText("Chưa có thuế đang áp dụng. Liên hệ quản lý rồi bấm Thử lại thuế.");
            }
            controls();
            if (!catalogLoaded) {
                searchCatalog(catalogQuery.getText().trim(), 0);
            }
        });
    }

    public void loadInitialData() {
        if (!taxesLoaded) {
            loadTaxes();
        } else if (!catalogLoaded) {
            searchCatalog(catalogQuery.getText().trim(), 0);
        }
    }

    private KetQuaTraCuuSanPham findCatalog(YeuCauTraCuuSanPham request, String keyword) throws IOException {
        KetQuaTraCuuSanPham found = products.search(request);
        if (found.getTotal() == 0 && request.getPage() == 0 && keyword.matches("[A-Za-z0-9_-]{1,20}")) {
            try {
                DuLieuSanPham match = products.detail(keyword).getSanPham();
                if (match.getTrangThai()) {
                    found.setItems(Collections.singletonList(match));
                    found.setTotal(1);
                }
            } catch (NgoaiLeGoiDichVu failure) {
                if (failure.getCode() != MaLoi.NOT_FOUND) {
                    throw failure;
                }
            }
        }
        return found;
    }

    private void selectProduct() {
        int row = catalog.getSelectedRow();
        String code = row < 0 ? null : (String) catalog.getValueAt(row, 0);
        if (Objects.equals(code, selectedProductCode)) {
            return;
        }
        selectedProductCode = code;
        if (code == null) {
            selectedProduct.setText("Chọn sản phẩm ở danh sách trên");
            selectedProduct.setToolTipText(null);
            showProductPhoto(null);
            variant.setModel(new DefaultComboBoxModel<>());
        }
        loadSelectedProduct();
        controls();
    }

    private void loadSelectedProduct() {
        if (loadingProduct || selectedProductCode == null) {
            return;
        }
        String code = selectedProductCode;
        loadingProduct = true;
        new SwingWorker<DuLieuDayDuSanPham, Void>() {
            private ImageIcon thumbnail;

            @Override
            protected DuLieuDayDuSanPham doInBackground() throws IOException {
                DuLieuDayDuSanPham detail = products.detail(code);
                thumbnail = productThumbnail(detail);
                return detail;
            }

            @Override
            protected void done() {
                loadingProduct = false;
                if (!code.equals(selectedProductCode)) {
                    loadSelectedProduct();
                } else {
                    try {
                        showProductChoices(get(), thumbnail);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        productLoadFailed();
                    } catch (ExecutionException failure) {
                        productLoadFailed();
                    }
                }
                controls();
            }
        }.execute();
    }

    private void productLoadFailed() {
        catalog.clearSelection();
        status.setText("Chưa tải được sản phẩm. Chọn lại sản phẩm để thử lại.");
    }

    private void showProductChoices(DuLieuDayDuSanPham detail, ImageIcon thumbnail) {
        DefaultComboBoxModel<DuLieuBienThe> choices = new DefaultComboBoxModel<>();
        DuLieuBienThe available = null;
        for (DuLieuBienThe item : detail.getBienThe()) {
            if (item.getTrangThai()) {
                choices.addElement(item);
                if (available == null && item.getSoLuongTon() > 0) {
                    available = item;
                }
            }
        }
        if (available != null) {
            choices.setSelectedItem(available);
        }
        String name = detail.getSanPham().getTenSP();
        selectedProduct.setText(name);
        selectedProduct.setToolTipText(name);
        variant.setModel(choices);
        qty.setValue(1);
        showProductPhoto(thumbnail);
        status.setText(available == null ? "Sản phẩm này hết hàng hoặc chưa có màu và size đang bán."
                : "Chọn màu, size và số lượng, rồi bấm Thêm vào giỏ.");
    }

    private static ImageIcon productThumbnail(DuLieuDayDuSanPham detail) {
        if (detail.getHinhAnh().isEmpty()) {
            return null;
        }
        DuLieuAnhSanPham photo = detail.getHinhAnh().stream().filter(DuLieuAnhSanPham::getAnhChinh)
                .findFirst().orElse(detail.getHinhAnh().get(0));
        try {
            return new ImageIcon(TaiAnh.thumbnail(photo.getDuongDan(), 60, 62));
        } catch (IOException | IllegalArgumentException unavailable) {
            return null;
        }
    }

    private void showProductPhoto(ImageIcon thumbnail) {
        productPhoto.setIcon(thumbnail);
        productPhoto.setText(thumbnail == null ? "—" : "");
    }

    private void addSelectedProduct() {
        DuLieuBienThe item = (DuLieuBienThe) variant.getSelectedItem();
        if (item == null || loadingProduct || needsCheck) {
            return;
        }
        try {
            qty.commitEdit();
        } catch (java.text.ParseException invalid) {
            status.setText("Nhập số lượng từ 1 đến 1.000.000.");
            return;
        }
        int count = ((Number) qty.getValue()).intValue();
        int existing = current == null ? 0 : current.getChiTiet().stream()
                .filter(line -> item.getMaBienThe().equals(line.getMaBienThe()))
                .mapToInt(DuLieuChiTietHoaDon::getSoLuong).findFirst().orElse(0);
        if (count > item.getSoLuongTon() - existing || count + existing > 1000000) {
            status.setText("Số lượng trong giỏ sẽ vượt số lượng có thể bán.");
            return;
        }
        if (current != null) {
            editLine(item.getMaBienThe(), count + existing, existing == 0 ? 0 : 1);
            return;
        }
        if (tax.getSelectedItem() == null) {
            status.setText("Chưa có thuế đang áp dụng. Bấm Thử lại thuế hoặc liên hệ quản lý.");
            return;
        }
        if (pendingCreate == null) {
            pendingCreate = new YeuCauTaoHoaDon();
            pendingCreate.setMaHD(SinhMaHoaDon.newId());
            pendingCreate.setMaThue(((String) tax.getSelectedItem()).split(" / ")[0]);
            pendingCreate.setMaKH(selectedCustomer == null ? null : selectedCustomer.getMaKH());
            pendingCreate.setMaKM(promo.getText());
            pendingCreate.setGhiChu(note.getText());
        }
        YeuCauTaoHoaDon request = pendingCreate;
        run(() -> api.create(request), invoice -> {
            pendingCreate = null;
            showInvoice(invoice);
            if (isDraft() && invoice.getChiTiet().isEmpty()) {
                editLine(item.getMaBienThe(), count, 0);
            } else {
                status.setText("Đã mở đơn có sẵn. Kiểm tra giỏ trước khi thêm tiếp.");
            }
        }, failure -> {
            if (failure instanceof NgoaiLeGoiDichVu && !mustCheck(failure)) {
                pendingCreate = null;
                orderCode.setText("Đơn mới");
            }
            writeFailure(failure);
        });
    }

    private void newOrder() {
        if ((isDraft() || pendingCreate != null)
                && JOptionPane.showConfirmDialog(this, "Đơn hiện tại được giữ nếu đã lưu. Bắt đầu đơn khác?",
                        "Đơn mới", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        current = null;
        pendingCreate = null;
        needsCheck = false;
        selectedCustomer = null;
        updateCustomerLabel();
        orderCode.setText("Đơn mới");
        customerQuery.setText("");
        promo.setText("");
        note.setText("");
        orderOptions.setVisible(false);
        options.setText("Tùy chọn đơn");
        model.setRowCount(0);
        total.setText("—");
        total.setToolTipText(null);
        subtotal.setText("—");
        discount.setText("—");
        taxAmount.setText("—");
        taxCaption.setText("Thuế");
        cartTitle.setText("Giỏ hàng");
        paymentInfo.setText(" ");
        resetPayment();
        catalog.clearSelection();
        status.setText("Chọn sản phẩm, màu và size để thêm vào giỏ.");
        tax.removeAllItems();
        loadTaxes();
    }

    private void checkOrder() {
        String code = current == null ? pendingCreate.getMaHD() : current.getHoaDon().getMaHD();
        run(() -> api.detail(code), invoice -> {
            showInvoice(invoice);
            status.setText("Đã đối chiếu đơn với máy chủ. Kiểm tra giỏ hàng trước khi thao tác tiếp.");
        }, failure -> {
            if (current == null && failure instanceof NgoaiLeGoiDichVu
                    && ((NgoaiLeGoiDichVu) failure).getCode() == MaLoi.NOT_FOUND) {
                needsCheck = false;
                controls();
                status.setText("Đơn chưa được lưu. Bấm Thêm vào giỏ để thử lại.");
            } else {
                showFailure(failure);
            }
        });
    }

    private boolean isDraft() {
        return current != null && current.getHoaDon().getTrangThai() == TrangThaiHoaDon.NHAP;
    }

    private String selectedLine() {
        int index = table.getSelectedRow();
        return index < 0 ? null : (String) model.getValueAt(index, 0);
    }

    private void editQuantity(int delta) {
        int index = table.getSelectedRow();
        if (index >= 0) {
            editLine(selectedLine(), (Integer) model.getValueAt(index, 4) + delta, 1);
        }
    }

    private void changeQuantity() {
        int index = table.getSelectedRow();
        if (index < 0) {
            return;
        }
        JSpinner quantity = new JSpinner(new SpinnerNumberModel(
                ((Integer) model.getValueAt(index, 4)).intValue(), 1, 1000000, 1));
        if (JOptionPane.showConfirmDialog(this, ChuDeGiaoDien.field("Số lượng mới", quantity),
                "Đổi số lượng", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                quantity.commitEdit();
                editLine(selectedLine(), (Integer) quantity.getValue(), 1);
            } catch (java.text.ParseException invalid) {
                status.setText("Nhập số lượng từ 1 đến 1.000.000.");
            }
        }
    }

    private void editLine(String code, int count, int mode) {
        if (code == null || !isDraft() || needsCheck) {
            return;
        }
        if (mode != 2 && (count < 1 || count > 1000000)) {
            status.setText("Nhập số lượng từ 1 đến 1.000.000.");
            return;
        }
        YeuCauDongHoaDon r = new YeuCauDongHoaDon();
        r.setMaHD(current.getHoaDon().getMaHD());
        r.setPhienBan(current.getHoaDon().getPhienBan());
        r.setMaBienThe(code);
        r.setSoLuong(count);
        runWrite(() -> api.line(r, mode), invoice -> {
            showInvoice(invoice);
            if (mode != 2) {
                for (int row = 0; row < model.getRowCount(); row++) {
                    if (code.equals(model.getValueAt(row, 0))) {
                        table.setRowSelectionInterval(row, row);
                        table.scrollRectToVisible(table.getCellRect(row, 0, true));
                        break;
                    }
                }
            }
        });
    }

    private YeuCauPhienBanHoaDon version() {
        YeuCauPhienBanHoaDon r = new YeuCauPhienBanHoaDon();
        r.setMaHD(current.getHoaDon().getMaHD());
        r.setPhienBan(current.getHoaDon().getPhienBan());
        return r;
    }

    private void resetPayment() {
        method.setSelectedIndex(0);
        tendered.setText("");
        transferConfirmed.setSelected(false);
        updateCheckout();
    }

    private void updateCheckout() {
        java.awt.CardLayout layout = (java.awt.CardLayout) checkoutFields.getLayout();
        layout.show(checkoutFields, method.getSelectedIndex() == 0 ? "cash" : "transfer");
        transferConfirmed.setSelected(false);
        updateChange();
    }

    private BigDecimal cashAmount() {
        String amount = tendered.getText().trim();
        if (amount.matches("[0-9]{1,3}(\\.[0-9]{3})+(,[0-9]{1,2})?")) {
            amount = amount.replace(".", "");
        }
        if (!amount.matches("[0-9]+([.,][0-9]{1,2})?")) {
            return null;
        }
        return new BigDecimal(amount.replace(',', '.')).setScale(2);
    }

    private void updateChange() {
        BigDecimal amount = cashAmount();
        if (!isDraft() || model.getRowCount() == 0 || amount == null) {
            changeDue.setText("—");
            return;
        }
        BigDecimal delta = amount.subtract(current.getHoaDon().getTongTien());
        changeDue.setText(delta.signum() < 0 ? "Thiếu " + ChuDeGiaoDien.money(delta.negate())
                : ChuDeGiaoDien.money(delta));
    }

    private YeuCauThanhToanHoaDon paymentRequest() {
        if (!isDraft() || model.getRowCount() == 0 || needsCheck) {
            return null;
        }
        DuLieuHoaDon invoice = current.getHoaDon();
        BigDecimal reviewedTotal = invoice.getTongTien();
        YeuCauThanhToanHoaDon request = new YeuCauThanhToanHoaDon();
        request.setMaHD(invoice.getMaHD());
        request.setPhienBan(invoice.getPhienBan());
        request.setTongTienDaXem(reviewedTotal);
        String confirmation;
        if (method.getSelectedIndex() == 0) {
            BigDecimal amount = cashAmount();
            if (amount == null || amount.compareTo(reviewedTotal) < 0) {
                status.setText("Nhập số tiền khách đưa, ít nhất bằng số tiền cần thanh toán.");
                tendered.requestFocusInWindow();
                return null;
            }
            request.setPhuongThucThanhToan(PhuongThucThanhToan.TIEN_MAT);
            request.setTienKhachDua(amount);
            confirmation = "Thu tiền mặt " + ChuDeGiaoDien.money(reviewedTotal) + "?\nTiền thừa: "
                    + ChuDeGiaoDien.money(amount.subtract(reviewedTotal));
        } else {
            if (!transferConfirmed.isSelected()) {
                status.setText("Đánh dấu Đã nhận tiền chuyển khoản sau khi kiểm tra giao dịch.");
                return null;
            }
            request.setPhuongThucThanhToan(PhuongThucThanhToan.CHUYEN_KHOAN);
            request.setNhanVienXacNhanChuyenKhoan(Boolean.TRUE);
            confirmation = "Xác nhận đã nhận chuyển khoản " + ChuDeGiaoDien.money(reviewedTotal) + "?";
        }
        return JOptionPane.showConfirmDialog(this, confirmation, "Xác nhận thu tiền",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION ? request : null;
    }

    public void showInvoice(DuLieuDayDuHoaDon d) {
        String previousCode = current == null ? null : current.getHoaDon().getMaHD();
        BigDecimal previousTotal = current == null ? null : current.getHoaDon().getTongTien();
        current = d;
        pendingCreate = null;
        needsCheck = false;
        DuLieuHoaDon h = d.getHoaDon();
        if (!h.getMaHD().equals(previousCode) || h.getTrangThai() != TrangThaiHoaDon.NHAP) {
            resetPayment();
        } else if (previousTotal != null && previousTotal.compareTo(h.getTongTien()) != 0) {
            transferConfirmed.setSelected(false);
        }
        orderCode.setText(h.getMaHD() + " · " + invoiceState(h.getTrangThai()));
        model.setRowCount(0);
        for (DuLieuChiTietHoaDon c : d.getChiTiet()) {
            model.addRow(new Object[] { c.getMaBienThe(), c.getTenSP(), c.getMauSac(), c.getKichCo(),
                    c.getSoLuong(), c.getDonGia(), c.getThanhTien() });
        }
        if (h.getMaKH() == null) {
            selectedCustomer = null;
        } else if (selectedCustomer == null || !h.getMaKH().equals(selectedCustomer.getMaKH())) {
            selectedCustomer = new DuLieuKhachHang();
            selectedCustomer.setMaKH(h.getMaKH());
            selectedCustomer.setHoTen(h.getTenKhachHang());
        }
        updateCustomerLabel();
        promo.setText(h.getMaKM());
        note.setText(h.getGhiChu());
        selectInvoiceTax(h);
        cartTitle.setText("Giỏ hàng (" + model.getRowCount() + ")");
        subtotal.setText(ChuDeGiaoDien.money(h.getTamTinh()));
        discount.setText(ChuDeGiaoDien.money(h.getTienGiam()));
        taxCaption.setText("Thuế (" + taxRate(h) + ")");
        taxAmount.setText(ChuDeGiaoDien.money(h.getTienThue()));
        total.setText(ChuDeGiaoDien.money(h.getTongTien()));
        total.setToolTipText(total.getText());
        total.setFont(total.getFont().deriveFont(Font.BOLD, total.getText().length() > 16 ? 16f : 24f));
        paymentInfo.setText("<html><div style='width:430px'>" + paymentSummary(h) + "</div></html>");
        String progress;
        switch (h.getTrangThai()) {
        case DA_THANH_TOAN:
            progress = "Đã thanh toán. Bấm In hóa đơn để xem trước bản in.";
            break;
        case DA_HUY:
            progress = "Đã hủy.";
            break;
        default:
            progress = model.getRowCount() == 0 ? "Chọn sản phẩm ở bên trái rồi thêm vào giỏ."
                    : "Kiểm tra giỏ hàng rồi bấm Thanh toán (F9).";
            break;
        }
        status.setText(progress);
        updateChange();
        controls();
    }

    private static String paymentSummary(DuLieuHoaDon invoice) {
        if (invoice.getTrangThai() != TrangThaiHoaDon.DA_THANH_TOAN) {
            return " ";
        }
        if (invoice.getPhuongThucThanhToan() == null) {
            return "Thông tin thu tiền: chưa có dữ liệu (hóa đơn lịch sử).";
        }
        if (invoice.getPhuongThucThanhToan() == PhuongThucThanhToan.TIEN_MAT) {
            if (invoice.getTienKhachDua() == null || invoice.getTienThua() == null) {
                return "Tiền mặt: dữ liệu thu tiền chưa đầy đủ.";
            }
            return "Tiền mặt | Khách đưa: " + ChuDeGiaoDien.money(invoice.getTienKhachDua())
                    + " | Tiền thừa: " + ChuDeGiaoDien.money(invoice.getTienThua());
        }
        if (Boolean.TRUE.equals(invoice.getNhanVienXacNhanChuyenKhoan())) {
            return "Chuyển khoản thủ công: nhân viên xác nhận đã nhận; ứng dụng không xác minh ngân hàng.";
        }
        return "Chuyển khoản: dữ liệu xác nhận chưa đầy đủ.";
    }

    public void openInvoiceHistory() {
        if (busy) {
            status.setText("Đang xử lý yêu cầu. Mở tra cứu hóa đơn sau khi hoàn tất.");
            return;
        }
        history(0);
    }

    private void history(int page) {
        YeuCauTraCuuHoaDon r = new YeuCauTraCuuHoaDon();
        r.setPage(page);
        r.setPageSize(20);
        run(() -> api.search(r), d -> {
            String[] rows = d.getItems().stream().map(h -> h.getMaHD() + " / " + h.getNgayLap() + " / "
                    + invoiceState(h.getTrangThai()) + " / " + ChuDeGiaoDien.money(h.getTongTien()))
                    .toArray(String[]::new);
            JList<String> list = new JList<>(rows);
            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(650, 350));
            int choice = JOptionPane.showOptionDialog(this, scroll,
                    "Trang " + (page + 1) + " - Tổng " + d.getTotal(), JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, new String[] { "Mở", "Trước", "Sau", "Mở theo mã", "Đóng" }, "Mở");
            if (choice == 0 && list.getSelectedIndex() >= 0) {
                String code = d.getItems().get(list.getSelectedIndex()).getMaHD();
                run(() -> api.detail(code), this::showInvoice);
            } else if (choice == 1 && page > 0) {
                history(page - 1);
            } else if (choice == 2 && (page + 1L) * 20 < d.getTotal()) {
                history(page + 1);
            } else if (choice == 3) {
                String code = JOptionPane.showInputDialog(this, "Nhập mã hóa đơn");
                if (code != null && !code.trim().isEmpty()) {
                    run(() -> api.detail(code.trim()), this::showInvoice);
                }
            }
        });
    }

    private static String invoiceState(TrangThaiHoaDon state) {
        switch (state) {
        case DA_THANH_TOAN:
            return "Đã thanh toán";
        case DA_HUY:
            return "Đã hủy";
        default:
            return "Chưa thanh toán";
        }
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(24, (orientation == SwingConstants.VERTICAL ? visible.height : visible.width) - 24);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport && getParent().getWidth() >= 1000;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport && getParent().getHeight() >= getPreferredSize().height;
    }

    private void selectInvoiceTax(DuLieuHoaDon h) {
        for (int i = 0; i < tax.getItemCount(); i++) {
            if (tax.getItemAt(i).startsWith(h.getMaThue() + " / ")) {
                tax.setSelectedIndex(i);
                return;
            }
        }
        String selected = h.getMaThue() + " / Đã chốt " + taxRate(h);
        tax.addItem(selected);
        tax.setSelectedItem(selected);
    }

    private static String taxRate(DuLieuHoaDon invoice) {
        return invoice.getPhanTramThue().stripTrailingZeros().toPlainString() + "%";
    }

    private void controls() {
        boolean editableCart = !busy && !needsCheck && isDraft();
        boolean selected = editableCart && table.getSelectedRow() >= 0;
        boolean newOrder = current == null && pendingCreate == null && !needsCheck;
        for (JButton button : new JButton[] { fresh, history }) {
            button.setEnabled(!busy && !needsCheck);
        }
        boolean paid = current != null && current.getHoaDon().getTrangThai() == TrangThaiHoaDon.DA_THANH_TOAN;
        printInvoice.setVisible(paid);
        printInvoice.setEnabled(paid && !busy && !needsCheck);
        search.setEnabled(!busy);
        catalog.setEnabled(!busy);
        table.setEnabled(!busy);
        catalogQuery.setEnabled(!busy);
        previous.setEnabled(!busy && catalogPage > 0);
        next.setEnabled(!busy && (catalogPage + 1L) * CATALOG_PAGE_SIZE < catalogTotal);
        reload.setVisible(needsCheck);
        reload.setEnabled(!busy);
        taxes.setVisible(current == null && tax.getItemCount() == 0 && !needsCheck);
        taxes.setEnabled(!busy);
        DuLieuBienThe item = (DuLieuBienThe) variant.getSelectedItem();
        addLine.setEnabled(!busy && !loadingProduct && !needsCheck
                && (isDraft() || current == null && tax.getSelectedItem() != null)
                && item != null && item.getSoLuongTon() > 0);
        boolean hasVariants = variant.getItemCount() > 0;
        variant.setEnabled(!busy && hasVariants);
        qty.setEnabled(!busy && hasVariants);
        change.setEnabled(selected);
        remove.setEnabled(selected);
        increase.setEnabled(selected);
        decrease.setEnabled(selected && (Integer) model.getValueAt(table.getSelectedRow(), 4) > 1);
        boolean canPay = editableCart && model.getRowCount() > 0;
        pay.setEnabled(canPay);
        method.setEnabled(canPay);
        tendered.setEnabled(canPay);
        transferConfirmed.setEnabled(canPay);
        cancel.setEnabled(editableCart);
        for (JTextField field : new JTextField[] { promo, note }) {
            field.setEnabled(!busy && newOrder);
        }
        boolean customerEditable = !busy && customers != null && (newOrder || editableCart);
        chooseCustomer.setEnabled(customerEditable);
        customerQuery.setEnabled(customerEditable);
        createCustomer.setEnabled(customerEditable);
        clearCustomer.setEnabled(customerEditable && selectedCustomer != null);
        tax.setEnabled(!busy && newOrder);
    }

    private <T> void runWrite(Callable<T> job, Consumer<T> success) {
        run(job, success, this::writeFailure);
    }

    private boolean mustCheck(Throwable failure) {
        if (!(failure instanceof NgoaiLeGoiDichVu)) {
            return true;
        }
        MaLoi code = ((NgoaiLeGoiDichVu) failure).getCode();
        return code == MaLoi.SERVER_ERROR || code == MaLoi.VERSION_CONFLICT;
    }

    private void writeFailure(Throwable failure) {
        if (mustCheck(failure) && (current != null || pendingCreate != null)) {
            needsCheck = true;
            if (current == null) {
                orderCode.setText(pendingCreate.getMaHD() + " · Chờ đối chiếu");
            }
            controls();
            status.setText("Chưa xác định được kết quả. Bấm Kiểm tra lại đơn trước khi thao tác tiếp.");
            return;
        }
        controls();
        showFailure(failure);
    }

    private void showFailure(Throwable cause) {
        String message = cause instanceof NgoaiLeGoiDichVu ? cause.getMessage()
                : "Không kết nối được máy chủ. Thử lại sau khi kiểm tra kết nối.";
        status.setText(message);
        JOptionPane.showMessageDialog(this, message, "Thông báo", JOptionPane.WARNING_MESSAGE);
    }

    private <T> void run(Callable<T> job, Consumer<T> success) {
        run(job, success, this::showFailure);
    }

    private <T> void run(Callable<T> job, Consumer<T> success, Consumer<Throwable> failure) {
        if (busy) {
            return;
        }
        busy = true;
        controls();
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return job.call();
            }

            @Override
            protected void done() {
                busy = false;
                controls();
                try {
                    success.accept(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    failure.accept(ex);
                } catch (ExecutionException ex) {
                    failure.accept(ex.getCause());
                }
            }
        }.execute();
    }

    private void chooseCustomer() {
        if (customers == null) {
            return;
        }
        String query = customerQuery.getText().trim();
        YeuCauTraCuuKhachHang request = new YeuCauTraCuuKhachHang();
        request.setTuKhoa(query.isEmpty() ? null : query);
        request.setTrangThai(Boolean.TRUE);
        request.setPage(0);
        request.setPageSize(50);
        run(() -> customers.search(request), result -> {
            List<DuLieuKhachHang> matches = result.getItems() == null
                    ? java.util.Collections.<DuLieuKhachHang>emptyList()
                    : result.getItems();
            if (matches.isEmpty()) {
                if (JOptionPane.showConfirmDialog(this, "Không tìm thấy khách đang hoạt động. Tạo hồ sơ mới?",
                        "Khách hàng", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    createCustomerInline();
                }
                return;
            }
            if (matches.size() == 1) {
                assignCustomer(matches.get(0));
                return;
            }
            DefaultListModel<String> labels = new DefaultListModel<String>();
            for (DuLieuKhachHang row : matches) {
                labels.addElement(row.getMaKH() + " — " + row.getHoTen() + " — " + safe(row.getSdt()));
            }
            JList<String> list = new JList<String>(labels);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(520, 300));
            int choice = JOptionPane.showOptionDialog(this, scroll, "Chọn khách hàng",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                    new String[] { "Chọn", "Đóng" }, "Chọn");
            if (choice == 0 && list.getSelectedIndex() >= 0) {
                assignCustomer(matches.get(list.getSelectedIndex()));
            }
        });
    }

    private void createCustomerInline() {
        if (customers == null) {
            return;
        }
        JTextField code = new JTextField(), name = new JTextField(), phone = new JTextField(),
                email = new JTextField(), address = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Mã khách hàng *"));
        form.add(code);
        form.add(new JLabel("Họ tên *"));
        form.add(name);
        form.add(new JLabel("Số điện thoại (không bắt buộc)"));
        form.add(phone);
        form.add(new JLabel("Email"));
        form.add(email);
        form.add(new JLabel("Địa chỉ"));
        form.add(address);
        if (JOptionPane.showConfirmDialog(this, form, "Tạo khách hàng nhanh", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        DuLieuKhachHang request = new DuLieuKhachHang();
        request.setMaKH(code.getText().trim());
        request.setHoTen(name.getText().trim());
        request.setSdt(phone.getText().trim());
        request.setEmail(blankToNull(email.getText()));
        request.setDiaChi(blankToNull(address.getText()));
        request.setTrangThai(true);
        run(() -> customers.create(request), this::assignCustomer);
    }

    private void assignCustomer(DuLieuKhachHang customer) {
        if (current == null) {
            selectedCustomer = customer;
            updateCustomerLabel();
            controls();
            status.setText(customer == null ? "Đã chọn khách lẻ cho hóa đơn mới."
                    : "Đã chọn khách " + customer.getMaKH() + " cho hóa đơn mới.");
            return;
        }
        if (current.getHoaDon().getTrangThai() != TrangThaiHoaDon.NHAP) {
            return;
        }
        YeuCauGanKhachHangHoaDon request = new YeuCauGanKhachHangHoaDon();
        request.setMaHD(current.getHoaDon().getMaHD());
        request.setPhienBan(current.getHoaDon().getPhienBan());
        request.setMaKH(customer == null ? null : customer.getMaKH());
        runWrite(() -> api.setCustomer(request), invoice -> {
            selectedCustomer = customer;
            showInvoice(invoice);
        });
    }

    private void updateCustomerLabel() {
        if (selectedCustomer == null) {
            customerSummary.setText("Khách lẻ");
            return;
        }
        String name = selectedCustomer.getHoTen() == null ? "" : selectedCustomer.getHoTen();
        String phone = selectedCustomer.getSdt() == null ? "" : " — " + selectedCustomer.getSdt();
        String fallback = current != null
                && current.getHoaDon().getNguonDuLieuHienThi() == NguonDuLieuHoaDon.FALLBACK_DU_LIEU_HIEN_TAI
                        ? " (tên hiện hành)"
                        : "";
        customerSummary.setText("<html>" + escape(selectedCustomer.getMaKH()) + " — " + escape(name + phone)
                + escape(fallback) + "</html>");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
