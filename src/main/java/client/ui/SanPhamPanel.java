package client.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import client.api.ApiException;
import client.controller.SanPhamController;
import common.dto.*;
import common.enums.VaiTro;

/** Panel tra cứu chỉ đọc, I/O chạy ngoài EDT; phân trang do server thực hiện. */
public final class SanPhamPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final SanPhamController controller;
    private final JTextField name = new JTextField(), color = new JTextField(), size = new JTextField();
    private final JComboBox<Option> category = new JComboBox<>(), brand = new JComboBox<>();
    private final JCheckBox inactive = new JCheckBox("Gồm hàng/biến thể ngừng bán");
    private final JComboBox<Integer> pageSize = new JComboBox<>(new Integer[] { 10, 20, 50, 100 });
    private final JButton search = new JButton("Tra cứu"), reset = new JButton("Xóa bộ lọc"),
            previous = new JButton("Trang trước"), next = new JButton("Trang sau"),
            detail = new JButton("Xem chi tiết");
    private final JLabel status = new JLabel("Chưa tải dữ liệu.");
    private final DefaultTableModel model = ProductTableSupport.model(
            new String[] { "Mã SP", "Tên sản phẩm", "Loại", "Thương hiệu", "Giá bán", "Tồn đang bán", "Trạng thái" },
            new Class<?>[] { String.class, String.class, String.class, String.class, java.math.BigDecimal.class,
                    Long.class, String.class });
    private final JTable table = new JTable(model);
    private List<SanPhamDTO> rows = new ArrayList<>();
    private SanPhamSearchRequest lastQuery;
    private SanPhamSearchResult lastResult;
    private boolean busy, loaded;

    public SanPhamPanel(SanPhamController controller, VaiTro role) {
        super(new BorderLayout(12, 12));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));
        setPreferredSize(new Dimension(1050, 600));
        JPanel filters = UiTheme.card(new GridLayout(2, 3, 16, 14));
        category.addItem(new Option(null, "Tất cả loại"));
        brand.addItem(new Option(null, "Tất cả thương hiệu"));
        filters.add(field("Tên sản phẩm (chứa)", name));
        filters.add(field("Loại sản phẩm", category));
        filters.add(field("Thương hiệu", brand));
        filters.add(field("Màu (đúng tên)", color));
        filters.add(field("Size (đúng tên)", size));
        filters.add(inactive);
        inactive.setVisible(role == VaiTro.NHAN_VIEN_QUAN_LY);
        pageSize.setSelectedItem(20);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.add(search);
        actions.add(reset);
        actions.add(new JLabel("Dòng/trang"));
        actions.add(pageSize);
        actions.add(previous);
        actions.add(next);
        actions.add(detail);
        JPanel top = new JPanel(new BorderLayout(10, 12));
        top.add(filters, BorderLayout.CENTER);
        top.add(UiTheme.heading("Danh sách sản phẩm", "Lọc theo loại, thương hiệu, màu và kích cỡ."),
                BorderLayout.NORTH);
        top.add(actions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        UiTheme.primary(search);
        UiTheme.primary(detail);
        name.putClientProperty("JTextField.placeholderText", "Nhập tên sản phẩm...");
        color.putClientProperty("JTextField.placeholderText", "Ví dụ: Đỏ");
        size.putClientProperty("JTextField.placeholderText", "Ví dụ: M");
        ProductTableSupport.configure(table);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        add(scroll, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        status.setForeground(UiTheme.MUTED);
        status.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        search.addActionListener(e -> searchNew());
        name.addActionListener(e -> searchNew());
        color.addActionListener(e -> searchNew());
        size.addActionListener(e -> searchNew());
        reset.addActionListener(e -> {
            name.setText("");
            color.setText("");
            size.setText("");
            category.setSelectedIndex(0);
            brand.setSelectedIndex(0);
            inactive.setSelected(false);
            searchNew();
        });
        previous.addActionListener(e -> movePage(-1));
        next.addActionListener(e -> movePage(1));
        detail.addActionListener(e -> openDetail());
        table.getSelectionModel().addListSelectionListener(e -> refreshButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !busy) {
                    openDetail();
                }
            }
        });
        refreshButtons();
    }

    private static JPanel field(String label, JComponent input) {
        return UiTheme.field(label, input);
    }

    public SanPhamDTO selectedProduct() {
        int index = table.getSelectedRow();
        return index < 0 ? null : rows.get(table.convertRowIndexToModel(index));
    }

    public void reload() {
        if (busy) {
            return;
        }
        loaded = false;
        initialize();
    }

    public void initialize() {
        if (loaded || busy) {
            return;
        }
        runJob(() -> new Catalogs(controller.categories(), controller.brands()), catalogs -> {
            category.removeAllItems();
            category.addItem(new Option(null, "Tất cả loại"));
            for (LoaiSanPhamDTO row : catalogs.categories) {
                category.addItem(new Option(row.getMaLoai(), row.getTenLoai()));
            }
            brand.removeAllItems();
            brand.addItem(new Option(null, "Tất cả thương hiệu"));
            for (ThuongHieuDTO row : catalogs.brands) {
                brand.addItem(new Option(row.getMaThuongHieu(), row.getTenThuongHieu()));
            }
            loaded = true;
            SwingUtilities.invokeLater(this::searchNew);
        });
    }

    private void searchNew() {
        if (busy) {
            return;
        }
        if (!loaded) {
            initialize();
            return;
        }
        SanPhamSearchRequest request = new SanPhamSearchRequest();
        request.setTenSP(name.getText());
        request.setMauSac(color.getText());
        request.setKichCo(size.getText());
        request.setMaLoai(((Option) category.getSelectedItem()).id);
        request.setMaThuongHieu(((Option) brand.getSelectedItem()).id);
        request.setIncludeInactive(inactive.isSelected());
        request.setPageSize((Integer) pageSize.getSelectedItem());
        performSearch(request);
    }

    private void movePage(int delta) {
        if (busy || lastQuery == null) {
            return;
        }
        SanPhamSearchRequest request = copy(lastQuery);
        request.setPage(request.getPage() + delta);
        performSearch(request);
    }

    private void performSearch(SanPhamSearchRequest request) {
        runJob(() -> controller.search(request), result -> {
            lastQuery = request;
            showSearchResult(result);
        });
    }

    public void showSearchResult(SanPhamSearchResult result) {
        lastResult = result;
        rows = result.getItems();
        model.setRowCount(0);
        for (SanPhamDTO row : rows) {
            model.addRow(new Object[] { row.getMaSP(), row.getTenSP(), row.getTenLoai(), row.getTenThuongHieu(),
                    row.getGiaBan(), row.getTongTon(), row.getTrangThai() ? "Đang bán" : "Ngừng bán" });
        }
        long pages = Math.max(1L, (result.getTotal() + result.getPageSize() - 1L) / result.getPageSize());
        status.setText(result.getTotal() + " sản phẩm • Trang " + (result.getPage() + 1) + "/" + pages
                + (rows.isEmpty() ? " • Không có sản phẩm phù hợp." : ""));
        refreshButtons();
    }

    private void openDetail() {
        int selected = table.getSelectedRow();
        if (busy || selected < 0) {
            return;
        }
        String id = rows.get(table.convertRowIndexToModel(selected)).getMaSP();
        runJob(() -> controller.detail(id), dto -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            new SanPhamDetailForm(owner, dto).setVisible(true);
        });
    }

    private <T> void runJob(Callable<T> job, Consumer<T> success) {
        busy = true;
        refreshButtons();
        status.setText("Đang tải dữ liệu từ máy chủ...");
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return job.call();
            }

            @Override
            protected void done() {
                busy = false;
                refreshButtons();
                if (!isShowing()) {
                    return;
                }
                try {
                    success.accept(get());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException failure) {
                    status.setText("Không tải được dữ liệu. Kết quả trước đó (nếu có) chưa được cập nhật.");
                    Throwable cause = failure.getCause();
                    JOptionPane.showMessageDialog(SanPhamPanel.this,
                            cause instanceof ApiException ? cause.getMessage() : "Không kết nối/xử lý được máy chủ.",
                            "Tra cứu sản phẩm", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshButtons() {
        search.setEnabled(!busy);
        reset.setEnabled(!busy);
        detail.setEnabled(!busy && table.getSelectedRow() >= 0);
        previous.setEnabled(!busy && lastResult != null && lastResult.getPage() > 0);
        next.setEnabled(!busy && lastResult != null && lastResult.getPage() < 1000000
                && ((long) lastResult.getPage() + 1) * lastResult.getPageSize() < lastResult.getTotal());
        for (JComponent control : new JComponent[] { name, color, size, category, brand, inactive, pageSize }) {
            control.setEnabled(!busy);
        }
    }

    private static SanPhamSearchRequest copy(SanPhamSearchRequest old) {
        SanPhamSearchRequest result = new SanPhamSearchRequest();
        result.setTenSP(old.getTenSP());
        result.setMauSac(old.getMauSac());
        result.setKichCo(old.getKichCo());
        result.setMaLoai(old.getMaLoai());
        result.setMaThuongHieu(old.getMaThuongHieu());
        result.setIncludeInactive(old.getIncludeInactive());
        result.setPage(old.getPage());
        result.setPageSize(old.getPageSize());
        return result;
    }

    private static final class Option {
        final String id, label;

        Option(String id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class Catalogs {
        final List<LoaiSanPhamDTO> categories;
        final List<ThuongHieuDTO> brands;

        Catalogs(List<LoaiSanPhamDTO> categories, List<ThuongHieuDTO> brands) {
            this.categories = categories;
            this.brands = brands;
        }
    }
}
