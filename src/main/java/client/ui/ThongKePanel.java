package client.ui;

import java.awt.*;
import java.time.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import client.api.*;
import common.dto.*;

/** Doanh thu tổng = tổng tiền header; tiền hàng từng sản phẩm chưa phân bổ giảm/thuế. */
public class ThongKePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final ManagementApi api;
    private final JTextField from = new JTextField(10), to = new JTextField(10);
    private final JComboBox<String> group = new JComboBox<>(
            new String[] { "Theo ngày", "Theo tháng", "Theo năm", "Theo sản phẩm" });
    private final JButton load = new JButton("Xem thống kê"), previous = new JButton("← Trước"),
            next = new JButton("Sau →");
    private final JLabel summary = new JLabel(), pageLabel = UiTheme.muted("Chưa tải dữ liệu"),
            note = UiTheme.muted("Chỉ tính hóa đơn đã thanh toán, lọc theo ngày lập hóa đơn.");
    private final DefaultTableModel model = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private RevenueRequest applied;
    private boolean byProduct, busy, hasNext;
    private int page;

    public ThongKePanel(ManagementApi api) {
        super(new BorderLayout(0, 18));
        this.api = api;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        LocalDate now = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        from.setText(now.withDayOfMonth(1).toString());
        to.setText(now.toString());
        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        top.add(UiTheme.heading("Thống kê doanh thu", "Khoảng ngày bao gồm cả ngày bắt đầu và kết thúc · yyyy-MM-dd"),
                BorderLayout.NORTH);
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filter.setOpaque(false);
        filter.add(new JLabel("Từ"));
        filter.add(from);
        filter.add(new JLabel("Đến"));
        filter.add(to);
        filter.add(group);
        filter.add(load);
        UiTheme.primary(load);
        top.add(filter, BorderLayout.CENTER);
        summary.setFont(summary.getFont().deriveFont(16f));
        summary.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        summary.setOpaque(true);
        summary.setBackground(UiTheme.SOFT);
        summary.setText("Chọn khoảng ngày để xem dữ liệu từ máy chủ.");
        top.add(summary, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        UiTheme.table(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setColumnHeaderView(table.getTableHeader());
        add(tableScroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        bottom.add(note, BorderLayout.NORTH);
        JPanel paging = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        paging.setOpaque(false);
        paging.add(pageLabel);
        paging.add(previous);
        paging.add(next);
        bottom.add(paging, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        load.addActionListener(e -> {
            try {
                RevenueRequest r = new RevenueRequest();
                r.setTuNgay(LocalDate.parse(from.getText().trim()));
                r.setDenNgay(LocalDate.parse(to.getText().trim()));
                r.setGroup(RevenueRequest.Group.values()[Math.min(group.getSelectedIndex(), 2)]);
                r.setPageSize(50);
                applied = r;
                byProduct = group.getSelectedIndex() == 3;
                page = 0;
                reload();
            } catch (java.time.format.DateTimeParseException invalid) {
                JOptionPane.showMessageDialog(this, "Nhập ngày đúng định dạng yyyy-MM-dd.", "Ngày không hợp lệ",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
        previous.addActionListener(e -> {
            page--;
            reload();
        });
        next.addActionListener(e -> {
            page++;
            reload();
        });
        controls();
    }

    private void reload() {
        if (busy || applied == null) {
            return;
        }
        applied.setPage(page);
        // Snapshot request; không đọc Swing trên thread nền.
        RevenueRequest r = new RevenueRequest();
        r.setTuNgay(applied.getTuNgay());
        r.setDenNgay(applied.getDenNgay());
        r.setGroup(applied.getGroup());
        r.setPage(page);
        r.setPageSize(applied.getPageSize());
        boolean products = byProduct;
        busy = true;
        controls();
        new SwingWorker<RevenueResult, Void>() {
            @Override
            protected RevenueResult doInBackground() throws Exception {
                return api.report(r, products);
            }

            @Override
            protected void done() {
                busy = false;
                try {
                    showResult(get(), products);
                } catch (Exception ex) {
                    model.setRowCount(0);
                    hasNext = false;
                    pageLabel.setText("Chưa tải dữ liệu");
                    summary.setText("Không tải được thống kê cho khoảng ngày đã chọn.");
                    Throwable cause = ex instanceof java.util.concurrent.ExecutionException ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(ThongKePanel.this,
                            cause instanceof ApiException ? cause.getMessage()
                                    : "Không tải được thống kê. Kiểm tra máy chủ rồi thử lại.",
                            "Thông báo", JOptionPane.WARNING_MESSAGE);
                } finally {
                    controls();
                }
            }
        }.execute();
    }

    public void showResult(RevenueResult r, boolean products) {
        if (products) {
            group.setSelectedIndex(3);
        } else if (group.getSelectedIndex() == 3) {
            group.setSelectedIndex(0);
        }
        hasNext = r.getHasNext();
        model.setDataVector(new Object[0][0],
                products ? new Object[] { "Mã sản phẩm", "Tên sản phẩm", "Số lượng bán", "Tiền hàng trước giảm/thuế" }
                        : new Object[] { "Thời gian", "Số hóa đơn", "Tổng thanh toán" });
        for (RevenueResult.Row row : r.getItems()) {
            if (products) {
                model.addRow(new Object[] { row.getCode(), row.getLabel(), row.getQuantity(),
                        UiTheme.money(row.getAmount()) });
            } else {
                model.addRow(new Object[] { row.getLabel(), row.getQuantity(), UiTheme.money(row.getAmount()) });
            }
        }
        summary.setText("<html><b>Tổng thanh toán: " + UiTheme.money(r.getTongTien()) + "</b> &nbsp; · &nbsp; "
                + r.getSoHoaDon() + " hóa đơn<br>Tiền hàng: " + UiTheme.money(r.getTamTinh())
                + " &nbsp; | &nbsp; Giảm: " + UiTheme.money(r.getTienGiam()) + " &nbsp; | &nbsp; Thuế: "
                + UiTheme.money(r.getTienThue()) + "</html>");
        note.setText(products
                ? "<html>Chỉ hóa đơn đã thanh toán · ngày lập. Tiền hàng theo sản phẩm chưa phân bổ giảm/thuế; không phải tổng thanh toán.</html>"
                : "Chỉ hóa đơn đã thanh toán · nhóm theo ngày lập. Không hiển thị khoảng thời gian không có giao dịch.");
        pageLabel.setText("Trang " + (page + 1));
        controls();
    }

    private void controls() {
        load.setEnabled(!busy);
        from.setEnabled(!busy);
        to.setEnabled(!busy);
        group.setEnabled(!busy);
        previous.setEnabled(!busy && applied != null && page > 0);
        next.setEnabled(!busy && applied != null && hasNext);
    }
}
