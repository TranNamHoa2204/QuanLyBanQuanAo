package client.ui;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import client.api.ApiException;
import client.api.HoaDonApi;
import common.dto.*;
import common.enums.TrangThaiHoaDon;

/** Tra cứu danh sách hóa đơn trong phạm vi được server cấp quyền và xem chi tiết chỉ đọc. */
public final class HoaDonPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final HoaDonApi api;
    private final JTextField query = new JTextField(22);
    private final JComboBox<StatusOption> status = new JComboBox<>();
    private final JButton reload = new JButton("Tìm / tải lại"), detail = new JButton("Xem chi tiết"),
            previous = new JButton("← Trước"), next = new JButton("Sau →");
    private final JLabel paging = UiTheme.muted("Chưa tải dữ liệu");
    private final DefaultTableModel model = new DefaultTableModel(new String[] { "Mã hóa đơn", "Ngày lập", "Nhân viên",
            "Khách hàng", "Khuyến mãi", "Tổng thanh toán", "Trạng thái" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private List<HoaDonDTO> rows = new ArrayList<>();
    private int page;
    private long total;
    private boolean busy;
    private boolean loaded;

    public HoaDonPanel(HoaDonApi api) {
        super(new BorderLayout(0, 16));
        this.api = api;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(UiTheme.heading("Hóa đơn", "Tra cứu hóa đơn đã bán và xem lại thông tin từng mặt hàng."),
                BorderLayout.NORTH);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        query.putClientProperty("JTextField.placeholderText", "Mã hóa đơn");
        status.addItem(new StatusOption(null, "Tất cả trạng thái"));
        status.addItem(new StatusOption(TrangThaiHoaDon.DA_THANH_TOAN, "Đã thanh toán"));
        status.addItem(new StatusOption(TrangThaiHoaDon.NHAP, "Nháp"));
        status.addItem(new StatusOption(TrangThaiHoaDon.DA_HUY, "Đã hủy"));
        status.setSelectedIndex(1);
        filters.add(new JLabel("Mã hóa đơn"));
        filters.add(query);
        filters.add(status);
        filters.add(reload);
        top.add(filters, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiTheme.table(table);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(5).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object value) {
                setText(value instanceof Number ? UiTheme.money((Number) value) : "");
            }

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus,
                    int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setValue(value);
                return this;
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(detail);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(paging);
        right.add(previous);
        right.add(next);
        bottom.add(left, BorderLayout.WEST);
        bottom.add(right, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        reload.addActionListener(e -> {
            page = 0;
            reload();
        });
        query.addActionListener(e -> {
            page = 0;
            reload();
        });
        status.addActionListener(e -> {
            if (loaded && !busy) {
                page = 0;
                reload();
            }
        });
        previous.addActionListener(e -> {
            if (page > 0) {
                page--;
                reload();
            }
        });
        next.addActionListener(e -> {
            if ((long) (page + 1) * 20 < total) {
                page++;
                reload();
            }
        });
        detail.addActionListener(e -> openSelected());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelected();
                }
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> controls());
        controls();
    }

    public void initialize() {
        if (!loaded && !busy) {
            reload();
        }
    }

    private void reload() {
        HoaDonSearchRequest request = new HoaDonSearchRequest();
        request.setMaHD(query.getText().trim());
        StatusOption selected = (StatusOption) status.getSelectedItem();
        request.setTrangThai(selected == null ? null : selected.value);
        request.setPage(page);
        request.setPageSize(20);
        busy = true;
        controls();
        new SwingWorker<HoaDonSearchResult, Void>() {
            @Override
            protected HoaDonSearchResult doInBackground() throws Exception {
                return api.search(request);
            }

            @Override
            protected void done() {
                busy = false;
                loaded = true;
                try {
                    showResult(get());
                } catch (Exception ex) {
                    showError(ex);
                }
                controls();
            }
        }.execute();
    }

    private void showResult(HoaDonSearchResult result) {
        rows = new ArrayList<>(result.getItems());
        total = result.getTotal();
        model.setRowCount(0);
        for (HoaDonDTO invoice : rows) {
            model.addRow(
                    new Object[] { invoice.getMaHD(), invoice.getNgayLap(), invoice.getMaNV(), blank(invoice.getMaKH()),
                            blank(invoice.getMaKM()), invoice.getTongTien(), statusLabel(invoice.getTrangThai()) });
        }
        paging.setText("Trang " + (page + 1) + " · " + total + " hóa đơn");
    }

    private void openSelected() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            return;
        }
        int index = table.convertRowIndexToModel(selected);
        if (index < 0 || index >= rows.size() || busy) {
            return;
        }
        String id = rows.get(index).getMaHD();
        busy = true;
        controls();
        new SwingWorker<HoaDonDetailDTO, Void>() {
            @Override
            protected HoaDonDetailDTO doInBackground() throws Exception {
                return api.detail(id);
            }

            @Override
            protected void done() {
                busy = false;
                try {
                    showDetail(get());
                } catch (Exception ex) {
                    showError(ex);
                }
                controls();
            }
        }.execute();
    }

    private void showDetail(HoaDonDetailDTO result) {
        HoaDonDTO h = result.getHoaDon();
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setPreferredSize(new Dimension(820, 460));
        JPanel summary = new JPanel(new GridLayout(0, 2, 10, 6));
        summary.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        addSummary(summary, "Mã hóa đơn", h.getMaHD());
        addSummary(summary, "Ngày lập", String.valueOf(h.getNgayLap()));
        addSummary(summary, "Nhân viên", blank(h.getMaNV()));
        addSummary(summary, "Khách hàng", blank(h.getMaKH()));
        addSummary(summary, "Khuyến mãi", blank(h.getMaKM()) + " · giảm " + percent(h.getPhanTramGiam()) + "%");
        addSummary(summary, "Thuế", blank(h.getMaThue()) + " · " + percent(h.getPhanTramThue()) + "%");
        addSummary(summary, "Trạng thái", statusLabel(h.getTrangThai()));
        panel.add(summary, BorderLayout.NORTH);
        DefaultTableModel lines = new DefaultTableModel(
                new String[] { "Mã biến thể", "Sản phẩm", "Màu", "Size", "Số lượng", "Đơn giá", "Thành tiền" }, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (ChiTietHoaDonDTO line : result.getChiTiet()) {
            lines.addRow(new Object[] { line.getMaBienThe(), line.getTenSP(), line.getMauSac(), line.getKichCo(),
                    line.getSoLuong(), line.getDonGia(), line.getThanhTien() });
        }
        JTable lineTable = new JTable(lines);
        UiTheme.table(lineTable);
        panel.add(new JScrollPane(lineTable), BorderLayout.CENTER);
        JPanel totals = new JPanel(new GridLayout(0, 2, 10, 4));
        totals.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        addSummary(totals, "Tạm tính", UiTheme.money(h.getTamTinh()));
        addSummary(totals, "Giảm giá", UiTheme.money(h.getTienGiam()));
        addSummary(totals, "Tiền thuế", UiTheme.money(h.getTienThue()));
        addSummary(totals, "Tổng thanh toán", UiTheme.money(h.getTongTien()));
        panel.add(totals, BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, panel, "Chi tiết hóa đơn " + h.getMaHD(), JOptionPane.INFORMATION_MESSAGE);
    }

    private static void addSummary(JPanel panel, String label, String value) {
        JLabel key = UiTheme.muted(label);
        JLabel text = new JLabel(value == null ? "—" : value);
        panel.add(key);
        panel.add(text);
    }

    private void showError(Exception ex) {
        Throwable cause = ex instanceof ExecutionException ? ex.getCause() : ex;
        JOptionPane.showMessageDialog(this,
                cause instanceof ApiException ? cause.getMessage()
                        : "Không tải được dữ liệu hóa đơn. Kiểm tra kết nối máy chủ rồi thử lại.",
                "Thông báo", JOptionPane.WARNING_MESSAGE);
    }

    private void controls() {
        reload.setEnabled(!busy);
        query.setEnabled(!busy);
        status.setEnabled(!busy);
        detail.setEnabled(!busy && table.getSelectedRow() >= 0);
        previous.setEnabled(!busy && page > 0);
        next.setEnabled(!busy && (long) (page + 1) * 20 < total);
    }

    private static String blank(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }

    private static String percent(java.math.BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static String statusLabel(TrangThaiHoaDon value) {
        if (value == TrangThaiHoaDon.DA_THANH_TOAN) {
            return "Đã thanh toán";
        }
        if (value == TrangThaiHoaDon.DA_HUY) {
            return "Đã hủy";
        }
        return "Nháp";
    }

    private static final class StatusOption {
        final TrangThaiHoaDon value;
        final String label;

        StatusOption(TrangThaiHoaDon value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
