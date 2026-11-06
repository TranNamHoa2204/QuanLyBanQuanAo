package client.gui.nhapkho;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import client.api.GoiDichVuSanPham;
import client.gui.components.ChuDeGiaoDien;
import client.gui.components.TacVuGiaoDien;
import common.dto.sanpham.DuLieuBienThe;
import common.dto.sanpham.DuLieuSanPham;
import common.dto.sanpham.YeuCauTraCuuSanPham;

public final class HopThoaiThemHangNhap extends JDialog {
    private static final long serialVersionUID = 1L;
    private final GoiDichVuSanPham api;
    private final JTextField keyword = new JTextField(28), price = new JTextField(12);
    private final JComboBox<DuLieuSanPham> products = new JComboBox<>();
    private final JComboBox<DuLieuBienThe> variants = new JComboBox<>();
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1));
    private final JButton search = new JButton("Tìm sản phẩm"), add = new JButton("Thêm vào phiếu");
    private final JLabel status = ChuDeGiaoDien.muted(" ");
    private boolean busy;
    private Selection result;

    public HopThoaiThemHangNhap(Window owner, GoiDichVuSanPham api) {
        super(owner, "Thêm hàng vào phiếu nhập", Dialog.ModalityType.APPLICATION_MODAL);
        this.api = api;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        body.add(ChuDeGiaoDien.heading("Thêm hàng vào phiếu nhập", "Chọn sản phẩm rồi chọn màu và size."),
                BorderLayout.NORTH);
        JPanel find = new JPanel(new BorderLayout(8, 0));
        find.add(keyword, BorderLayout.CENTER);
        find.add(search, BorderLayout.EAST);
        keyword.putClientProperty("JTextField.placeholderText", "Nhập tên sản phẩm");
        price.putClientProperty("JTextField.placeholderText", "Ví dụ: 95000");
        products.setRenderer(renderer(DuLieuSanPham.class, p -> p.getMaSP() + " — " + p.getTenSP(),
                "Chọn sản phẩm"));
        variants.setRenderer(renderer(DuLieuBienThe.class, v -> v.getMauSac() + " / " + v.getKichCo()
                + " · Tồn " + v.getSoLuongTon(), "Chọn màu và size"));
        JPanel amounts = new JPanel(new GridLayout(1, 2, 8, 0));
        amounts.add(ChuDeGiaoDien.field("Số lượng nhập", quantity));
        amounts.add(ChuDeGiaoDien.field("Đơn giá nhập (đ)", price));
        JPanel fields = ChuDeGiaoDien.card(new GridLayout(0, 1, 0, 9), 14);
        fields.add(find);
        fields.add(ChuDeGiaoDien.field("Sản phẩm", products));
        fields.add(ChuDeGiaoDien.field("Màu và size", variants));
        fields.add(amounts);
        body.add(fields, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(status, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton close = new JButton("Đóng");
        actions.add(close);
        actions.add(add);
        ChuDeGiaoDien.primary(add);
        footer.add(actions, BorderLayout.SOUTH);
        body.add(footer, BorderLayout.SOUTH);
        add(body);
        search.addActionListener(e -> searchProducts());
        keyword.addActionListener(e -> search.doClick());
        products.addActionListener(e -> loadVariants());
        variants.addActionListener(e -> refreshControls());
        add.addActionListener(e -> submit());
        close.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(add);
        pack();
        setSize(Math.max(540, getWidth()), getHeight());
        setLocationRelativeTo(owner);
        searchProducts();
    }

    Selection getSelection() {
        return result;
    }

    private void searchProducts() {
        YeuCauTraCuuSanPham request = new YeuCauTraCuuSanPham();
        request.setTenSP(keyword.getText().trim());
        request.setPageSize(50);
        fetch(() -> api.search(request), found -> {
            products.setModel(new DefaultComboBoxModel<>(found.getItems().toArray(new DuLieuSanPham[0])));
            products.setSelectedIndex(-1);
            variants.removeAllItems();
            status.setText(found.getItems().isEmpty() ? "Không tìm thấy sản phẩm đang bán."
                    : found.getTotal() > found.getItems().size() ? "Nhập thêm tên để thu hẹp danh sách."
                            : "Chọn một sản phẩm để xem màu và size.");
            if (found.getItems().size() == 1) {
                products.setSelectedIndex(0);
                SwingUtilities.invokeLater(this::loadVariants);
            }
        });
    }

    private void loadVariants() {
        DuLieuSanPham product = (DuLieuSanPham) products.getSelectedItem();
        if (busy || product == null || !isDisplayable()) {
            return;
        }
        variants.removeAllItems();
        fetch(() -> api.detail(product.getMaSP()), detail -> {
            for (DuLieuBienThe row : detail.getBienThe()) {
                if (row.getTrangThai()) {
                    variants.addItem(row);
                }
            }
            status.setText(variants.getItemCount() == 0 ? "Sản phẩm này chưa có màu hoặc size đang bán." : " ");
        });
    }

    private <T> void fetch(Callable<T> query, Consumer<T> success) {
        if (busy) {
            return;
        }
        busy = true;
        status.setText("Đang tìm hàng…");
        status.setToolTipText(null);
        refreshControls();
        TacVuGiaoDien.run(search, query, value -> {
            success.accept(value);
            busy = false;
            refreshControls();
        }, message -> {
            status.setText("Chưa lấy được dữ liệu. Bấm Tìm sản phẩm để thử lại.");
            status.setToolTipText(message);
            busy = false;
            refreshControls();
        });
    }

    private void refreshControls() {
        search.setEnabled(!busy);
        keyword.setEnabled(!busy);
        products.setEnabled(!busy && products.getItemCount() > 0);
        variants.setEnabled(!busy && variants.getItemCount() > 0);
        add.setEnabled(!busy && variants.getSelectedItem() != null);
    }

    private void submit() {
        DuLieuBienThe variant = (DuLieuBienThe) variants.getSelectedItem();
        if (busy || variant == null) {
            return;
        }
        try {
            quantity.commitEdit();
            BigDecimal amount = new BigDecimal(price.getText().trim());
            if (amount.signum() < 0) {
                status.setText("Đơn giá nhập phải từ 0 đồng trở lên.");
                return;
            }
            result = new Selection(variant.getMaBienThe(), ((Number) quantity.getValue()).intValue(), amount);
            dispose();
        } catch (ParseException | NumberFormatException invalid) {
            status.setText("Kiểm tra số lượng và đơn giá nhập, ví dụ 95000.");
        }
    }

    private static <T> DefaultListCellRenderer renderer(Class<T> type, Function<T, String> text, String prompt) {
        return new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list,
                        type.isInstance(value) ? text.apply(type.cast(value)) : prompt, index, selected, focus);
            }
        };
    }

    static final class Selection {
        final String variant;
        final int quantity;
        final BigDecimal price;

        Selection(String variant, int quantity, BigDecimal price) {
            this.variant = variant;
            this.quantity = quantity;
            this.price = price;
        }
    }
}
