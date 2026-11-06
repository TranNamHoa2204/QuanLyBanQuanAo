package client.gui.sanpham;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import client.api.NgoaiLeGoiDichVu;
import client.controllers.DieuKhienSanPham;
import client.gui.components.ChuDeGiaoDien;
import client.gui.components.TienIchBangSanPham;
import common.dto.danhmuc.DuLieuLoaiSanPham;
import common.dto.danhmuc.DuLieuThuongHieu;
import common.dto.sanpham.DuLieuDayDuSanPham;
import common.dto.sanpham.DuLieuSanPham;
import common.dto.sanpham.KetQuaTraCuuSanPham;
import common.dto.sanpham.YeuCauTraCuuSanPham;
import common.enums.VaiTro;

/** Panel tra cứu chỉ đọc, I/O chạy ngoài EDT; phân trang do server thực hiện. */
public final class KhungSanPham extends JPanel {
    private static final long serialVersionUID = 1L;
    private final DieuKhienSanPham controller;
    private final JTextField name = new JTextField(), color = new JTextField(), size = new JTextField();
    private final JComboBox<Option> category = new JComboBox<>(), brand = new JComboBox<>();
    private final JCheckBox inactive = new JCheckBox("Gồm hàng/biến thể ngừng bán");
    private final JComboBox<Integer> pageSize = new JComboBox<>(new Integer[] { 10, 20, 50, 100 });
    private final JButton search = new JButton("Tìm sản phẩm"), reset = new JButton("Xóa bộ lọc"),
            previous = new JButton("Trang trước"), next = new JButton("Trang sau"),
            detail = new JButton("Xem chi tiết");
    private final JLabel status = new JLabel("Chọn một sản phẩm để xem ảnh và chi tiết."),
            previewMessage = new JLabel("Chọn một sản phẩm trong bảng.", SwingConstants.CENTER),
            previewTitle = ChuDeGiaoDien.title("Ảnh sản phẩm", 16), previewSummary = ChuDeGiaoDien.muted(" ");
    private final JPanel previewBody = new JPanel(new BorderLayout());
    private final JButton retryPreview = new JButton("Thử lại");
    private final Timer previewDelay = new Timer(140, e -> loadPreview());
    private final DefaultTableModel model = TienIchBangSanPham.model(
            new String[] { "Mã SP", "Tên sản phẩm", "Loại", "Thương hiệu", "Giá bán", "Tồn đang bán",
                    "Trạng thái" },
            new Class<?>[] { String.class, String.class, String.class, String.class,
                    java.math.BigDecimal.class, Long.class, String.class });
    private final JTable table = ChuDeGiaoDien.tableWithHorizontalScroll(model,
            86, 260, 115, 115, 103, 100, 108);
    private List<DuLieuSanPham> rows = new ArrayList<>();
    private YeuCauTraCuuSanPham lastQuery;
    private KetQuaTraCuuSanPham lastResult;
    private boolean busy, loaded, updatingFilters;
    private boolean previewLoading;
    private String previewId, openDetailId;
    private DuLieuDayDuSanPham previewDetail;

    public KhungSanPham(DieuKhienSanPham controller, VaiTro role) {
        super(new BorderLayout(12, 12));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(8, 16, 14, 16));
        setPreferredSize(new Dimension(1050, 600));
        JPanel filters = new JPanel(new GridLayout(2, 3, 16, 10));
        filters.setOpaque(false);
        category.addItem(new Option(null, "Tất cả loại"));
        brand.addItem(new Option(null, "Tất cả thương hiệu"));
        filters.add(field("Tên sản phẩm", name));
        filters.add(field("Loại sản phẩm", category));
        filters.add(field("Thương hiệu", brand));
        filters.add(field("Màu", color));
        filters.add(field("Size", size));
        filters.add(inactive);
        inactive.setVisible(role == VaiTro.NHAN_VIEN_QUAN_LY);
        pageSize.setSelectedItem(20);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(search);
        actions.add(reset);
        actions.add(new JLabel("Dòng/trang"));
        actions.add(pageSize);
        actions.add(previous);
        actions.add(next);
        JPanel top = ChuDeGiaoDien.card(new BorderLayout(0, 10), 14);
        top.add(filters, BorderLayout.CENTER);
        top.add(ChuDeGiaoDien.heading("Danh sách sản phẩm", "Lọc theo loại, thương hiệu, màu và kích cỡ."),
                BorderLayout.NORTH);
        top.add(actions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        ChuDeGiaoDien.primary(search);
        name.putClientProperty("JTextField.placeholderText", "Nhập tên sản phẩm...");
        color.putClientProperty("JTextField.placeholderText", "Ví dụ: Đỏ");
        size.putClientProperty("JTextField.placeholderText", "Ví dụ: M");
        TienIchBangSanPham.configure(table);
        JScrollPane scroll = ChuDeGiaoDien.tableScroll(table);
        JPanel content = new JPanel(new BorderLayout(10, 0));
        content.setOpaque(false);
        content.add(scroll, BorderLayout.CENTER);
        JPanel preview = ChuDeGiaoDien.card(new BorderLayout(0, 10), 12);
        preview.setPreferredSize(new Dimension(280, 0));
        preview.add(previewTitle, BorderLayout.NORTH);
        previewBody.setOpaque(false);
        preview.add(previewBody, BorderLayout.CENTER);
        JPanel previewActions = new JPanel(new BorderLayout(0, 6));
        previewActions.setOpaque(false);
        previewActions.add(previewSummary, BorderLayout.NORTH);
        previewActions.add(detail, BorderLayout.SOUTH);
        preview.add(previewActions, BorderLayout.SOUTH);
        content.add(preview, BorderLayout.EAST);
        add(content, BorderLayout.CENTER);
        previewDelay.setRepeats(false);
        retryPreview.addActionListener(e -> loadPreview());
        showPreviewMessage("Chọn một sản phẩm trong bảng.", false);
        add(status, BorderLayout.SOUTH);
        status.setForeground(ChuDeGiaoDien.MUTED);
        status.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        search.addActionListener(e -> searchNew());
        name.addActionListener(e -> searchNew());
        color.addActionListener(e -> searchNew());
        size.addActionListener(e -> searchNew());
        for (JComboBox<?> filter : new JComboBox<?>[] { category, brand, pageSize }) {
            filter.addActionListener(e -> searchNew());
        }
        inactive.addActionListener(e -> searchNew());
        reset.addActionListener(e -> {
            updatingFilters = true;
            name.setText("");
            color.setText("");
            size.setText("");
            category.setSelectedIndex(0);
            brand.setSelectedIndex(0);
            inactive.setSelected(false);
            updatingFilters = false;
            searchNew();
        });
        previous.addActionListener(e -> movePage(-1));
        next.addActionListener(e -> movePage(1));
        detail.addActionListener(e -> openDetail());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshButtons();
                updatePreview();
            }
        });
        TienIchBangSanPham.onOpenRow(table, this::openDetail);
        refreshButtons();
    }

    private static JPanel field(String label, JComponent input) {
        return ChuDeGiaoDien.field(label, input);
    }

    public DuLieuSanPham selectedProduct() {
        int index = table.getSelectedRow();
        return index < 0 ? null : rows.get(table.convertRowIndexToModel(index));
    }

    void onSelectionChanged(ListSelectionListener listener) {
        table.getSelectionModel().addListSelectionListener(listener);
    }

    boolean isSearching() {
        return busy;
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
            String selectedCategory = ((Option) category.getSelectedItem()).id;
            String selectedBrand = ((Option) brand.getSelectedItem()).id;
            updatingFilters = true;
            category.removeAllItems();
            category.addItem(new Option(null, "Tất cả loại"));
            for (DuLieuLoaiSanPham row : catalogs.categories) {
                category.addItem(new Option(row.getMaLoai(), row.getTenLoai()));
                if (row.getMaLoai().equals(selectedCategory)) {
                    category.setSelectedIndex(category.getItemCount() - 1);
                }
            }
            brand.removeAllItems();
            brand.addItem(new Option(null, "Tất cả thương hiệu"));
            for (DuLieuThuongHieu row : catalogs.brands) {
                brand.addItem(new Option(row.getMaThuongHieu(), row.getTenThuongHieu()));
                if (row.getMaThuongHieu().equals(selectedBrand)) {
                    brand.setSelectedIndex(brand.getItemCount() - 1);
                }
            }
            updatingFilters = false;
            loaded = true;
            SwingUtilities.invokeLater(this::searchNew);
        });
    }

    private void searchNew() {
        if (busy || updatingFilters) {
            return;
        }
        if (!loaded) {
            initialize();
            return;
        }
        YeuCauTraCuuSanPham request = new YeuCauTraCuuSanPham();
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
        YeuCauTraCuuSanPham request = copy(lastQuery);
        request.setPage(request.getPage() + delta);
        performSearch(request);
    }

    private void performSearch(YeuCauTraCuuSanPham request) {
        runJob(() -> controller.search(request), result -> {
            lastQuery = request;
            showSearchResult(result);
        });
    }

    public void showSearchResult(KetQuaTraCuuSanPham result) {
        lastResult = result;
        table.clearSelection();
        rows = result.getItems();
        model.setRowCount(0);
        for (DuLieuSanPham row : rows) {
            model.addRow(new Object[] { row.getMaSP(), row.getTenSP(), row.getTenLoai(),
                    row.getTenThuongHieu(), row.getGiaBan(), row.getTongTon(),
                    row.getTrangThai() ? "Đang bán" : "Ngừng bán" });
        }
        long pages = Math.max(1L, (result.getTotal() + result.getPageSize() - 1L) / result.getPageSize());
        status.setText(result.getTotal() + " sản phẩm • Trang " + (result.getPage() + 1) + "/" + pages
                + (rows.isEmpty() ? " • Không có sản phẩm phù hợp." : ""));
        refreshButtons();
    }

    private void updatePreview() {
        DuLieuSanPham selected = selectedProduct();
        String id = selected == null ? null : selected.getMaSP();
        if (Objects.equals(id, previewId)) {
            return;
        }
        previewId = id;
        previewDetail = null;
        openDetailId = null;
        previewTitle.setText(selected == null ? "Ảnh sản phẩm" : selected.getTenSP());
        previewTitle.setToolTipText(selected == null ? null : selected.getTenSP());
        previewSummary.setText(selected == null ? " " : ChuDeGiaoDien.money(selected.getGiaBan()) + " · Tồn "
                + selected.getTongTon());
        previewDelay.stop();
        showPreviewMessage(id == null ? "Chọn một sản phẩm trong bảng." : "Đang mở ảnh…", false);
        if (id != null) {
            previewDelay.restart();
        }
    }

    private void loadPreview() {
        if (previewId == null || previewLoading || previewDetail != null) {
            return;
        }
        previewDelay.stop();
        String id = previewId;
        previewLoading = true;
        showPreviewMessage("Đang mở ảnh…", false);
        new SwingWorker<DuLieuDayDuSanPham, Void>() {
            @Override
            protected DuLieuDayDuSanPham doInBackground() throws Exception {
                return controller.detail(id);
            }

            @Override
            protected void done() {
                previewLoading = false;
                if (!Objects.equals(id, previewId)) {
                    if (previewId != null) {
                        loadPreview();
                    }
                    return;
                }
                try {
                    previewDetail = get();
                    previewBody.removeAll();
                    previewBody.add(new KhungAnhSanPham(previewDetail.getHinhAnh(),
                            KhungChiTietSanPham.colors(previewDetail), 244, 230));
                    previewBody.revalidate();
                    previewBody.repaint();
                    if (id.equals(openDetailId)) {
                        openDetailId = null;
                        showDetail();
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException failure) {
                    openDetailId = null;
                    showPreviewMessage("Chưa mở được ảnh sản phẩm.", true);
                }
            }
        }.execute();
    }

    private void showPreviewMessage(String text, boolean retry) {
        previewBody.removeAll();
        previewMessage.setText(text);
        previewBody.add(previewMessage, BorderLayout.CENTER);
        if (retry) {
            previewBody.add(retryPreview, BorderLayout.SOUTH);
        }
        previewBody.revalidate();
        previewBody.repaint();
    }

    private void openDetail() {
        if (busy || previewId == null) {
            return;
        }
        if (previewDetail != null) {
            showDetail();
            return;
        }
        openDetailId = previewId;
        previewDelay.stop();
        loadPreview();
    }

    private void showDetail() {
        if (isShowing()) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            new HopThoaiChiTietSanPham(owner, previewDetail).setVisible(true);
        }
    }

    private <T> void runJob(Callable<T> job, Consumer<T> success) {
        busy = true;
        firePropertyChange("searching", false, true);
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
                firePropertyChange("searching", true, false);
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
                    JOptionPane.showMessageDialog(KhungSanPham.this,
                            cause instanceof NgoaiLeGoiDichVu ? cause.getMessage()
                                    : "Không kết nối/xử lý được máy chủ.",
                            "Tra cứu sản phẩm", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshButtons() {
        search.setEnabled(!busy);
        reset.setEnabled(!busy);
        table.setEnabled(!busy);
        detail.setEnabled(!busy && table.getSelectedRow() >= 0);
        previous.setEnabled(!busy && lastResult != null && lastResult.getPage() > 0);
        next.setEnabled(!busy && lastResult != null && lastResult.getPage() < 1000000
                && ((long) lastResult.getPage() + 1) * lastResult.getPageSize() < lastResult.getTotal());
        for (JComponent control : new JComponent[] { name, color, size, category, brand, inactive,
                pageSize }) {
            control.setEnabled(!busy);
        }
    }

    private static YeuCauTraCuuSanPham copy(YeuCauTraCuuSanPham old) {
        YeuCauTraCuuSanPham result = new YeuCauTraCuuSanPham();
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
        final List<DuLieuLoaiSanPham> categories;
        final List<DuLieuThuongHieu> brands;

        Catalogs(List<DuLieuLoaiSanPham> categories, List<DuLieuThuongHieu> brands) {
            this.categories = categories;
            this.brands = brands;
        }
    }
}
