package client.gui.kho;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import client.api.GoiDichVuLichSuKho;
import client.api.NgoaiLeGoiDichVu;
import client.gui.components.BieuTuongCoDien;
import client.gui.components.ChuDeGiaoDien;
import client.gui.components.TienIchBangSanPham;
import common.dto.kho.DuLieuLichSuKho;
import common.dto.kho.KetQuaTraCuuLichSuKho;
import common.dto.kho.YeuCauTraCuuLichSuKho;
import common.enums.NguonBienDongKho;

/** Manager-only ledger browser. Requests run off the Swing event thread. */
public final class KhungLichSuKho extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 50;
    private final GoiDichVuLichSuKho api;
    private final JTextField variant = new JTextField(12), employee = new JTextField(12),
            from = new JTextField(10), to = new JTextField(10);
    private final JComboBox<Object> source = new JComboBox<Object>(
            new Object[] { "Tất cả", NguonBienDongKho.TON_MO_DAU, NguonBienDongKho.KHOI_TAO_BIEN_THE,
                    NguonBienDongKho.BAN_HANG, NguonBienDongKho.NHAP_HANG, NguonBienDongKho.DIEU_CHINH_TON });
    private final JButton search = new JButton("Xem lịch sử", BieuTuongCoDien.TIM_KIEM), previous = new JButton("Trang trước"),
            next = new JButton("Trang sau");
    private final JLabel status = ChuDeGiaoDien.muted("Lịch sử tăng và giảm tồn kho.");
    private final DefaultTableModel model = TienIchBangSanPham.model(
            new String[] { "Thời gian", "Biến thể", "Sản phẩm", "Người thao tác", "Nguồn", "Tham chiếu",
                    "Trước", "Đổi", "Sau", "Lý do" },
            new Class<?>[] { String.class, String.class, String.class, String.class, String.class,
                    String.class, Integer.class, Integer.class, Integer.class, String.class });
    private final JTable table = ChuDeGiaoDien.tableWithHorizontalScroll(model,
            160, 100, 225, 140, 120, 170, 60, 65, 60, 215);
    private int page;
    private long total;
    private long requestSequence;
    private boolean busy;

    public KhungLichSuKho(GoiDichVuLichSuKho api) {
        super(new BorderLayout(0, 16));
        this.api = api;
        setBorder(BorderFactory.createEmptyBorder(8, 16, 14, 16));
        JPanel top = ChuDeGiaoDien.card(new BorderLayout(0, 10), 14);
        top.add(ChuDeGiaoDien.heading("Lịch sử biến động kho",
                "Xem số lượng thay đổi sau mỗi lần bán, nhập hàng hoặc điều chỉnh tồn."), BorderLayout.NORTH);
        source.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list,
                        value instanceof NguonBienDongKho ? sourceName((NguonBienDongKho) value) : value,
                        index, selected, focus);
            }
        });
        JPanel filters = new JPanel(new GridBagLayout());
        filters.setOpaque(false);
        addFilter(filters, 0, 0, "Mã biến thể", variant);
        addFilter(filters, 1, 0, "Loại thay đổi", source);
        addFilter(filters, 2, 0, "Từ ngày (yyyy-MM-dd)", from);
        addFilter(filters, 3, 0, "Đến ngày", to);
        addFilter(filters, 0, 1, "Mã nhân viên", employee);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        ChuDeGiaoDien.primary(search);
        controls.add(search);
        controls.add(previous);
        controls.add(next);
        controls.add(status);
        JPanel filterArea = new JPanel(new BorderLayout(0, 8));
        filterArea.setOpaque(false);
        filterArea.add(filters, BorderLayout.CENTER);
        filterArea.add(controls, BorderLayout.SOUTH);
        top.add(filterArea, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        TienIchBangSanPham.configure(table);
        JScrollPane scroll = ChuDeGiaoDien.tableScroll(table);
        add(scroll, BorderLayout.CENTER);
        search.addActionListener(e -> {
            page = 0;
            loadPage();
        });
        for (JTextField field : new JTextField[] { variant, employee, from, to }) {
            field.addActionListener(e -> search.doClick());
        }
        source.addActionListener(e -> search.doClick());
        previous.addActionListener(e -> {
            if (page > 0) {
                page--;
                loadPage();
            }
        });
        next.addActionListener(e -> {
            if ((long) (page + 1) * PAGE_SIZE < total) {
                page++;
                loadPage();
            }
        });
        refreshControls();
    }

    public void reload() {
        page = 0;
        loadPage();
    }

    private static void addFilter(JPanel panel, int x, int y, String label, JComponent input) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 4, 3, 8);
        panel.add(ChuDeGiaoDien.field(label, input), c);
    }

    private void loadPage() {
        if (busy) {
            return;
        }
        final YeuCauTraCuuLichSuKho request;
        try {
            request = new YeuCauTraCuuLichSuKho();
            request.setMaBienThe(blankToNull(variant.getText()));
            request.setMaNV(blankToNull(employee.getText()));
            request.setTuNgay(parseDate(from.getText()));
            request.setDenNgay(parseDate(to.getText()));
            Object selected = source.getSelectedItem();
            if (selected instanceof NguonBienDongKho) {
                request.setNguon((NguonBienDongKho) selected);
            }
            request.setPage(page);
            request.setPageSize(PAGE_SIZE);
        } catch (DateTimeParseException invalidDate) {
            JOptionPane.showMessageDialog(this, "Ngày phải theo định dạng yyyy-MM-dd.", "Bộ lọc không hợp lệ",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        busy = true;
        long sequence = ++requestSequence;
        refreshControls();
        new SwingWorker<KetQuaTraCuuLichSuKho, Void>() {
            @Override
            protected KetQuaTraCuuLichSuKho doInBackground() throws Exception {
                return api.search(request);
            }

            @Override
            protected void done() {
                if (sequence != requestSequence) {
                    return;
                }
                try {
                    KetQuaTraCuuLichSuKho result = get();
                    total = result.getTotal();
                    model.setRowCount(0);
                    for (DuLieuLichSuKho row : result.getItems()) {
                        model.addRow(new Object[] { String.valueOf(row.getThoiGian()), row.getMaBienThe(),
                                row.getMaSP(),
                                row.getTenNguoiThaoTac() == null ? "(hệ thống / mốc tồn)"
                                        : row.getTenNguoiThaoTac(),
                                sourceName(row.getNguon()), row.getThamChieu(), row.getSoLuongTruoc(),
                                row.getSoLuongThayDoi(), row.getSoLuongSau(), row.getLyDo() });
                    }
                    status.setText("Trang " + (page + 1) + " • " + total + " dòng");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException failure) {
                    Throwable cause = failure.getCause();
                    String message = cause instanceof NgoaiLeGoiDichVu ? cause.getMessage()
                            : "Không tải được lịch sử kho. Kiểm tra kết nối tới máy chủ.";
                    JOptionPane.showMessageDialog(KhungLichSuKho.this, message, "Thông báo",
                            JOptionPane.WARNING_MESSAGE);
                } finally {
                    busy = false;
                    refreshControls();
                }
            }
        }.execute();
    }

    private void refreshControls() {
        search.setEnabled(!busy);
        previous.setEnabled(!busy && page > 0);
        next.setEnabled(!busy && (long) (page + 1) * PAGE_SIZE < total);
        variant.setEnabled(!busy);
        employee.setEnabled(!busy);
        from.setEnabled(!busy);
        to.setEnabled(!busy);
        source.setEnabled(!busy);
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static LocalDate parseDate(String value) {
        String text = blankToNull(value);
        return text == null ? null : LocalDate.parse(text);
    }

    private static String sourceName(NguonBienDongKho source) {
        if (source == null) {
            return "";
        }
        switch (source) {
        case TON_MO_DAU:
            return "Tồn mở đầu";
        case KHOI_TAO_BIEN_THE:
            return "Khởi tạo biến thể";
        case BAN_HANG:
            return "Bán hàng";
        case NHAP_HANG:
            return "Nhập hàng";
        case DIEU_CHINH_TON:
            return "Điều chỉnh tồn";
        default:
            return source.name();
        }
    }
}
