package client.gui.thongke;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import client.api.NgoaiLeGoiDichVu;
import client.controllers.DieuKhienThongKe;
import client.gui.components.BieuTuongCoDien;
import client.gui.components.ChuDeGiaoDien;
import client.gui.components.TienIchBangSanPham;
import common.dto.thongke.DuLieuThongKeDoanhThu;
import common.dto.thongke.DuLieuThongKeDoanhThuDong;
import common.dto.thongke.YeuCauThongKeDoanhThu;
import common.enums.NhomThongKe;

/** Bảng và biểu đồ hiển thị cùng báo cáo doanh thu từ máy chủ. */
public final class ManHinhThongKe extends JPanel implements Scrollable {
    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 50;
    private final DieuKhienThongKe controller;
    private final JTextField from = new JTextField(LocalDate.now().minusDays(29).toString(), 10);
    private final JTextField to = new JTextField(LocalDate.now().toString(), 10);
    private final JComboBox<String> quickRange = new JComboBox<>(
            new String[] { "Chọn nhanh khoảng ngày", "Hôm nay", "7 ngày gần đây", "30 ngày gần đây" });
    private final JComboBox<Mode> mode = new JComboBox<Mode>(new Mode[] {
            new Mode("Theo ngày", NhomThongKe.NGAY), new Mode("Theo tháng", NhomThongKe.THANG),
            new Mode("Theo năm", NhomThongKe.NAM), new Mode("Theo sản phẩm", null) });
    private final JButton search = new JButton("Xem báo cáo", BieuTuongCoDien.BAO_CAO), previous = new JButton("Trang trước"),
            next = new JButton("Trang sau");
    private final JLabel status = ChuDeGiaoDien.muted("Chưa tải thống kê."),
            legacyWarning = ChuDeGiaoDien.muted(""), invoices = new JLabel("—"), gross = new JLabel("—"),
            discount = new JLabel("—"), tax = new JLabel("—"), revenue = new JLabel("—"),
            paid = new JLabel("—");
    private final DefaultTableModel model = TienIchBangSanPham.model(
            new String[] { "Kỳ / Sản phẩm", "Số hóa đơn", "Hàng bán trước giảm", "Giảm giá", "Thuế",
                    "Doanh thu sau giảm", "Khách trả" },
            new Class<?>[] { String.class, Long.class, BigDecimal.class, BigDecimal.class, BigDecimal.class,
                    BigDecimal.class, BigDecimal.class });
    private final JTable table = ChuDeGiaoDien.tableWithHorizontalScroll(model,
            285, 110, 175, 120, 110, 170, 150);
    private final BieuDoThongKe.TheoNhom revenueChart = new BieuDoThongKe.TheoNhom(true),
            invoiceChart = new BieuDoThongKe.TheoNhom(false);
    private final BieuDoThongKe.CoCau mixChart = new BieuDoThongKe.CoCau();
    private final JLabel revenueChartTitle = ChuDeGiaoDien.title("Doanh thu theo kỳ", 16),
            invoiceChartTitle = ChuDeGiaoDien.title("Hóa đơn theo kỳ", 16);
    private DuLieuThongKeDoanhThu result;
    private int page;
    private boolean busy;
    private long sequence;

    public ManHinhThongKe(DieuKhienThongKe controller) {
        super(new BorderLayout(0, 10));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(8, 16, 14, 16));
        JPanel top = ChuDeGiaoDien.card(new BorderLayout(0, 8), 12);
        top.add(ChuDeGiaoDien.heading("Báo biểu doanh thu",
                "Số liệu dựa trên hóa đơn đã thanh toán, theo khoảng ngày đã chọn."),
                BorderLayout.NORTH);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        from.setToolTipText("Nhập ngày theo định dạng yyyy-MM-dd");
        to.setToolTipText("Nhập ngày theo định dạng yyyy-MM-dd");
        filters.add(quickRange);
        filters.add(ChuDeGiaoDien.muted("Từ ngày"));
        filters.add(from);
        filters.add(ChuDeGiaoDien.muted("Đến ngày"));
        filters.add(to);
        filters.add(mode);
        filters.add(search);
        ChuDeGiaoDien.primary(search);
        top.add(filters, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel metrics = new JPanel(new GridLayout(1, 6, 8, 0));
        metrics.setOpaque(false);
        metrics.add(metric("Hóa đơn đã trả", invoices));
        metrics.add(metric("Trước giảm giá", gross));
        metrics.add(metric("Giảm giá", discount));
        metrics.add(metric("Thuế", tax));
        metrics.add(metric("Doanh thu sau giảm", revenue));
        revenue.setForeground(ChuDeGiaoDien.PRIMARY);
        metrics.add(metric("Tổng khách trả", paid));
        TienIchBangSanPham.configure(table);
        JPanel charts = new JPanel(new GridLayout(1, 3, 8, 0));
        charts.setOpaque(false);
        charts.setPreferredSize(new Dimension(880, 226));
        charts.add(chartCard(revenueChartTitle, "Sau giảm giá · đơn vị đồng · trang hiện tại", revenueChart));
        charts.add(chartCard(invoiceChartTitle, "Số hóa đơn trong từng nhóm · trang hiện tại", invoiceChart));
        charts.add(chartCard(ChuDeGiaoDien.title("Cơ cấu tổng khách trả", 16), "Toàn bộ khoảng đã lọc",
                mixChart));

        JPanel data = ChuDeGiaoDien.card(new BorderLayout(0, 6), 10);
        data.add(ChuDeGiaoDien.title("Dữ liệu chi tiết", 15), BorderLayout.NORTH);
        JScrollPane detail = ChuDeGiaoDien.tableScroll(table);
        detail.setPreferredSize(new Dimension(880, 138));
        data.add(detail, BorderLayout.CENTER);
        JPanel dashboard = new JPanel(new BorderLayout(0, 8));
        dashboard.setOpaque(false);
        dashboard.add(charts, BorderLayout.NORTH);
        dashboard.add(data, BorderLayout.CENTER);
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(metrics, BorderLayout.NORTH);
        center.add(dashboard, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 5));
        footer.setOpaque(false);
        legacyWarning.setForeground(new Color(0x8A5A00));
        footer.add(legacyWarning, BorderLayout.NORTH);
        JPanel paging = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        paging.setOpaque(false);
        paging.add(previous);
        paging.add(next);
        paging.add(status);
        footer.add(paging, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        search.addActionListener(this::search);
        from.addActionListener(this::search);
        to.addActionListener(this::search);
        quickRange.addActionListener(e -> applyQuickRange());
        mode.addActionListener(e -> reload());
        previous.addActionListener(e -> loadPage(page - 1));
        next.addActionListener(e -> loadPage(page + 1));
        refreshControls();
    }

    public void reload() {
        search(null);
    }

    private void applyQuickRange() {
        int choice = quickRange.getSelectedIndex();
        if (choice <= 0 || busy) {
            return;
        }
        LocalDate today = LocalDate.now();
        int days = new int[] { 0, 1, 7, 30 }[choice];
        from.setText(today.minusDays(days - 1L).toString());
        to.setText(today.toString());
        quickRange.setSelectedIndex(0);
        reload();
    }

    private void search(ActionEvent ignored) {
        if (busy) {
            return;
        }
        YeuCauThongKeDoanhThu request = new YeuCauThongKeDoanhThu();
        try {
            request.setTuNgay(LocalDate.parse(from.getText().trim()));
            request.setDenNgay(LocalDate.parse(to.getText().trim()));
        } catch (DateTimeParseException invalid) {
            JOptionPane.showMessageDialog(this, "Ngày phải theo định dạng yyyy-MM-dd.", "Bộ lọc không hợp lệ",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Mode selected = (Mode) mode.getSelectedItem();
        request.setNhomThoiGian(selected.group);
        load(request, 0);
    }

    private void loadPage(int requestedPage) {
        if (busy || result == null || requestedPage < 0
                || (long) requestedPage * PAGE_SIZE >= result.getTotalItems()) {
            return;
        }
        YeuCauThongKeDoanhThu request = new YeuCauThongKeDoanhThu();
        request.setTuNgay(result.getTuNgay());
        request.setDenNgay(result.getDenNgay());
        request.setNhomThoiGian(result.getNhomThoiGian());
        load(request, requestedPage);
    }

    private void load(YeuCauThongKeDoanhThu request, int requestedPage) {
        request.setPage(requestedPage);
        request.setPageSize(PAGE_SIZE);
        run(() -> request.getNhomThoiGian() == null ? controller.byProduct(request)
                : controller.byTime(request), value -> {
            result = value;
            page = value.getPage();
            render(value);
        });
    }

    private void render(DuLieuThongKeDoanhThu value) {
        invoices.setText(Long.toString(value.getSoHoaDon()));
        gross.setText(TienIchBangSanPham.money(value.getHangBanTruocGiam()));
        discount.setText(TienIchBangSanPham.money(value.getTienGiam()));
        tax.setText(TienIchBangSanPham.money(value.getTienThue()));
        revenue.setText(TienIchBangSanPham.money(value.getDoanhThuSauGiam()));
        paid.setText(TienIchBangSanPham.money(value.getTongKhachTra()));
        legacyWarning.setText(value.getCanhBaoDuLieuCu() == null ? "" : value.getCanhBaoDuLieuCu());
        model.setRowCount(0);
        List<DuLieuThongKeDoanhThuDong> items = value.getItems() == null
                ? Collections.emptyList() : value.getItems();
        boolean productReport = value.getNhomThoiGian() == null;
        for (DuLieuThongKeDoanhThuDong row : items) {
            String label = productReport
                    ? (row.getMaSP() + " — " + row.getTenSP()
                            + (row.isTenSPHienHanhFallback() ? " (tên hiện tại)" : ""))
                    : row.getNhan();
            model.addRow(new Object[] { label, row.getSoHoaDon(), row.getHangBanTruocGiam(),
                    row.getTienGiam(), row.getTienThue(), row.getDoanhThuSauGiam(), row.getTongKhachTra() });
        }
        long pages = Math.max(1L, (value.getTotalItems() + PAGE_SIZE - 1L) / PAGE_SIZE);
        status.setText(value.getTotalItems() + " nhóm • Trang " + (page + 1) + "/" + pages
                + (items.isEmpty() ? " • Không có dữ liệu trong khoảng đã chọn." : ""));
        revenueChartTitle.setText(productReport ? "Doanh thu theo sản phẩm" : "Doanh thu theo kỳ");
        invoiceChartTitle.setText(productReport ? "Hóa đơn theo sản phẩm" : "Hóa đơn theo kỳ");
        revenueChart.setData(items, productReport);
        invoiceChart.setData(items, productReport);
        mixChart.setData(value);
        refreshControls();
    }

    private static JPanel metric(String label, JLabel value) {
        JPanel card = ChuDeGiaoDien.card(new BorderLayout(0, 4), 10);
        card.add(ChuDeGiaoDien.muted(label), BorderLayout.NORTH);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 17f));
        value.addPropertyChangeListener("text", e -> value.setToolTipText(value.getText()));
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private static JPanel chartCard(JLabel title, String description, JPanel plot) {
        JPanel card = ChuDeGiaoDien.card(new BorderLayout(0, 8), 12);
        JPanel header = new JPanel(new GridLayout(0, 1, 0, 2));
        header.setOpaque(false);
        header.add(title);
        header.add(ChuDeGiaoDien.muted(description));
        card.add(header, BorderLayout.NORTH);
        card.add(plot, BorderLayout.CENTER);
        return card;
    }

    private <T> void run(Callable<T> work, Consumer<T> success) {
        if (busy) {
            return;
        }
        busy = true;
        long current = ++sequence;
        status.setText("Đang tải số liệu từ máy chủ...");
        refreshControls();
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                if (current != sequence) {
                    return;
                }
                busy = false;
                refreshControls();
                try {
                    success.accept(get());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException failure) {
                    status.setText("Không tải được báo cáo; số liệu cũ vẫn được giữ.");
                    Throwable cause = failure.getCause();
                    JOptionPane.showMessageDialog(ManHinhThongKe.this,
                            cause instanceof NgoaiLeGoiDichVu ? cause.getMessage()
                                    : "Không kết nối/xử lý được máy chủ.",
                            "Thống kê", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshControls() {
        search.setEnabled(!busy);
        from.setEnabled(!busy);
        to.setEnabled(!busy);
        mode.setEnabled(!busy);
        quickRange.setEnabled(!busy);
        previous.setEnabled(!busy && page > 0);
        next.setEnabled(!busy && result != null && (long) (page + 1) * PAGE_SIZE < result.getTotalItems());
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
        return getParent() instanceof JViewport && getParent().getWidth() >= 950;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport && getParent().getHeight() >= getPreferredSize().height;
    }

    private static final class Mode {
        final String label;
        final NhomThongKe group;

        Mode(String label, NhomThongKe group) {
            this.label = label;
            this.group = group;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
