package client.ui;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import client.api.ApiException;
import client.api.LichSuKhoApi;
import common.dto.LichSuKhoDTO;
import common.dto.LichSuKhoSearchRequest;
import common.dto.LichSuKhoSearchResult;
import common.enums.NguonBienDongKho;

/** Manager-only ledger browser. Requests run off the Swing event thread. */
public final class LichSuKhoPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 50;
    private final LichSuKhoApi api;
    private final JTextField variant = new JTextField(12), employee = new JTextField(12),
            from = new JTextField(10), to = new JTextField(10);
    private final JComboBox<Object> source = new JComboBox<Object>(new Object[]{"Tất cả", NguonBienDongKho.TON_MO_DAU,
            NguonBienDongKho.KHOI_TAO_BIEN_THE, NguonBienDongKho.BAN_HANG, NguonBienDongKho.DIEU_CHINH_TON});
    private final JButton search = new JButton("Lọc"), previous = new JButton("Trước"), next = new JButton("Sau");
    private final JLabel status = UiTheme.muted("Chưa tải lịch sử kho.");
    private final DefaultTableModel model = ProductTableSupport.model(
            new String[]{"Thời gian", "Biến thể", "Sản phẩm", "Người thao tác", "Nguồn", "Tham chiếu", "Trước", "Đổi", "Sau", "Lý do"},
            new Class<?>[]{String.class, String.class, String.class, String.class, String.class,
                    String.class, Integer.class, Integer.class, Integer.class, String.class});
    private final JTable table = new JTable(model);
    private int page;
    private long total;
    private long requestSequence;
    private boolean busy;

    public LichSuKhoPanel(LichSuKhoApi api) {
        super(new BorderLayout(0, 16)); this.api = api;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.add(UiTheme.heading("Lịch sử biến động kho",
                "Mỗi dòng là giao dịch đã ghi cùng transaction với thay đổi tồn."), BorderLayout.NORTH);
        JPanel filters = new JPanel(new GridBagLayout()); filters.setOpaque(false);
        addFilter(filters, 0, 0, "Biến thể", variant); addFilter(filters, 1, 0, "Nguồn", source);
        addFilter(filters, 2, 0, "Từ ngày (yyyy-MM-dd)", from); addFilter(filters, 3, 0, "Đến ngày", to);
        addFilter(filters, 0, 1, "Mã nhân viên", employee);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); controls.setOpaque(false);
        controls.add(search); controls.add(previous); controls.add(next); controls.add(status);
        JPanel filterArea = new JPanel(new BorderLayout(0, 8)); filterArea.setOpaque(false);
        filterArea.add(filters, BorderLayout.CENTER); filterArea.add(controls, BorderLayout.SOUTH);
        top.add(filterArea, BorderLayout.SOUTH); add(top, BorderLayout.NORTH);
        ProductTableSupport.configure(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {155, 90, 100, 145, 130, 110, 65, 65, 65, 260};
        for (int i = 0; i < widths.length; i++) { table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]); }
        JScrollPane scroll = new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader()); add(scroll, BorderLayout.CENTER);
        search.addActionListener(e -> { page = 0; loadPage(); });
        previous.addActionListener(e -> { if (page > 0) { page--; loadPage(); } });
        next.addActionListener(e -> { if ((long) (page + 1) * PAGE_SIZE < total) { page++; loadPage(); } });
        refreshControls();
    }

    public void reload() { page = 0; loadPage(); }

    private static void addFilter(JPanel panel, int x, int y, String label, JComponent input) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx = x; c.gridy = y; c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(3, 4, 3, 8);
        panel.add(UiTheme.field(label, input), c);
    }

    private void loadPage() {
        if (busy) { return; }
        final LichSuKhoSearchRequest request;
        try {
            request = new LichSuKhoSearchRequest();
            request.setMaBienThe(blankToNull(variant.getText())); request.setMaNV(blankToNull(employee.getText()));
            request.setTuNgay(parseDate(from.getText())); request.setDenNgay(parseDate(to.getText()));
            Object selected = source.getSelectedItem();
            if (selected instanceof NguonBienDongKho) { request.setNguon((NguonBienDongKho) selected); }
            request.setPage(page); request.setPageSize(PAGE_SIZE);
        } catch (DateTimeParseException invalidDate) {
            JOptionPane.showMessageDialog(this, "Ngày phải theo định dạng yyyy-MM-dd.", "Bộ lọc không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        busy = true; long sequence = ++requestSequence; refreshControls();
        new SwingWorker<LichSuKhoSearchResult, Void>() {
            @Override protected LichSuKhoSearchResult doInBackground() throws Exception { return api.search(request); }
            @Override protected void done() {
                if (sequence != requestSequence) { return; }
                try {
                    LichSuKhoSearchResult result = get(); total = result.getTotal();
                    model.setRowCount(0);
                    for (LichSuKhoDTO row : result.getItems()) {
                        model.addRow(new Object[]{String.valueOf(row.getThoiGian()), row.getMaBienThe(), row.getMaSP(),
                                row.getTenNguoiThaoTac() == null ? "(hệ thống / mốc tồn)" : row.getTenNguoiThaoTac(),
                                sourceName(row.getNguon()), row.getThamChieu(), row.getSoLuongTruoc(),
                                row.getSoLuongThayDoi(), row.getSoLuongSau(), row.getLyDo()});
                    }
                    status.setText("Trang " + (page + 1) + " • " + total + " dòng");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException failure) {
                    Throwable cause = failure.getCause();
                    String message = cause instanceof ApiException ? cause.getMessage()
                            : "Không tải được lịch sử kho. Kiểm tra kết nối tới máy chủ.";
                    JOptionPane.showMessageDialog(LichSuKhoPanel.this, message, "Thông báo", JOptionPane.WARNING_MESSAGE);
                } finally { busy = false; refreshControls(); }
            }
        }.execute();
    }

    private void refreshControls() {
        search.setEnabled(!busy); previous.setEnabled(!busy && page > 0);
        next.setEnabled(!busy && (long) (page + 1) * PAGE_SIZE < total);
        variant.setEnabled(!busy); employee.setEnabled(!busy); from.setEnabled(!busy); to.setEnabled(!busy); source.setEnabled(!busy);
    }

    private static String blankToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private static LocalDate parseDate(String value) {
        String text = blankToNull(value); return text == null ? null : LocalDate.parse(text);
    }
    private static String sourceName(NguonBienDongKho source) {
        if (source == null) { return ""; }
        switch (source) {
        case TON_MO_DAU: return "Tồn mở đầu";
        case KHOI_TAO_BIEN_THE: return "Khởi tạo biến thể";
        case BAN_HANG: return "Bán hàng";
        case DIEU_CHINH_TON: return "Điều chỉnh tồn";
        default: return source.name();
        }
    }
}
