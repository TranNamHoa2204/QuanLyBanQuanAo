package client.gui.khuyenmai;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import client.api.NgoaiLeGoiDichVu;
import client.controllers.DieuKhienKhuyenMai;
import client.gui.components.BieuTuongCoDien;
import client.gui.components.ChuDeGiaoDien;
import client.gui.components.TienIchBangSanPham;
import common.dto.khuyenmai.DuLieuKhuyenMai;
import common.dto.khuyenmai.YeuCauLuuKhuyenMai;
import common.dto.khuyenmai.YeuCauTraCuuKhuyenMai;
import common.dto.khuyenmai.YeuCauTrangThaiKhuyenMai;
import common.enums.TrangThaiHieuLucKhuyenMai;

/** Manager UI for promotion search, maintenance and state changes. */
public final class ManHinhKhuyenMai extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 20;
    private final DieuKhienKhuyenMai controller;
    private final JTextField keyword = new JTextField(18);
    private final JComboBox<String> activeFilter = new JComboBox<String>(
            new String[] { "Tất cả", "Đang bật", "Đã ngừng" });
    private final JButton search = new JButton("Tìm khuyến mãi", BieuTuongCoDien.TIM_KIEM), add = new JButton("Thêm khuyến mãi", BieuTuongCoDien.THEM),
            edit = new JButton("Sửa khuyến mãi"), toggle = new JButton("Ngừng áp dụng"), previous = new JButton("Trang trước"),
            next = new JButton("Trang sau");
    private final JLabel status = ChuDeGiaoDien.muted("Chưa tải khuyến mãi.");
    private final DefaultTableModel model = TienIchBangSanPham.model(
            new String[] { "Mã KM", "Tên chương trình", "Giảm (%)", "Bắt đầu", "Kết thúc", "Áp dụng",
                    "Hiệu lực hôm nay" },
            new Class<?>[] { String.class, String.class, BigDecimal.class, String.class, String.class,
                    String.class, String.class });
    private final JTable table = ChuDeGiaoDien.tableWithHorizontalScroll(model,
            115, 265, 100, 110, 110, 110, 150);
    private List<DuLieuKhuyenMai> rows = new ArrayList<DuLieuKhuyenMai>();
    private int page;
    private long total;
    private boolean busy;
    private long sequence;

    public ManHinhKhuyenMai(DieuKhienKhuyenMai controller) {
        super(new BorderLayout(0, 14));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(8, 16, 14, 16));
        JPanel top = ChuDeGiaoDien.card(new BorderLayout(0, 10), 14);
        top.add(ChuDeGiaoDien.heading("Quản lý khuyến mãi",
                "Tìm theo mã hoặc tên. Chọn chương trình để sửa hoặc đổi trạng thái áp dụng."),
                BorderLayout.NORTH);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(ChuDeGiaoDien.muted("Mã hoặc tên"));
        filters.add(keyword);
        filters.add(ChuDeGiaoDien.muted("Áp dụng"));
        filters.add(activeFilter);
        filters.add(search);
        filters.add(add);
        ChuDeGiaoDien.danger(toggle);
        ChuDeGiaoDien.primary(add);
        top.add(filters, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        TienIchBangSanPham.configure(table);
        add(ChuDeGiaoDien.tableScroll(table), BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        footer.setOpaque(false);
        footer.add(previous);
        footer.add(next);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(edit);
        actions.add(toggle);
        JPanel bottom = new JPanel(new BorderLayout(0, 5));
        bottom.setOpaque(false);
        bottom.add(status, BorderLayout.NORTH);
        bottom.add(footer, BorderLayout.WEST);
        bottom.add(actions, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        search.addActionListener(e -> {
            page = 0;
            load();
        });
        keyword.addActionListener(e -> {
            page = 0;
            load();
        });
        activeFilter.addActionListener(e -> {
            if (!busy) {
                page = 0;
                load();
            }
        });
        add.addActionListener(e -> create());
        edit.addActionListener(e -> editSelected());
        toggle.addActionListener(e -> toggleSelected());
        previous.addActionListener(e -> {
            if (!busy && page > 0) {
                page--;
                load();
            }
        });
        next.addActionListener(e -> {
            if (!busy && (long) (page + 1) * PAGE_SIZE < total) {
                page++;
                load();
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> refreshControls());
        TienIchBangSanPham.onOpenRow(table, () -> edit.doClick());
        refreshControls();
    }

    public void reload() {
        if (!busy) {
            page = 0;
            load();
        }
    }

    private void load() {
        YeuCauTraCuuKhuyenMai request = new YeuCauTraCuuKhuyenMai();
        String text = keyword.getText() == null ? "" : keyword.getText().trim();
        request.setTuKhoa(text.isEmpty() ? null : text);
        int filter = activeFilter.getSelectedIndex();
        request.setTrangThai(filter == 0 ? null : Boolean.valueOf(filter == 1));
        request.setPage(page);
        request.setPageSize(PAGE_SIZE);
        run(() -> controller.search(request), result -> {
            rows = result.getItems() == null ? new ArrayList<DuLieuKhuyenMai>() : result.getItems();
            total = result.getTotal();
            page = result.getPage();
            model.setRowCount(0);
            for (DuLieuKhuyenMai row : rows) {
                model.addRow(new Object[] { row.getMaKM(), row.getTenKM(), row.getPhanTramGiam(),
                        row.getNgayBatDau(), row.getNgayKetThuc(),
                        row.getTrangThai() ? "Đang bật" : "Đã ngừng",
                        effectiveName(row.getTrangThaiHieuLuc()) });
            }
            long pages = Math.max(1L, (total + PAGE_SIZE - 1L) / PAGE_SIZE);
            status.setText(total + " chương trình • Trang " + (page + 1) + "/" + pages
                    + (rows.isEmpty() ? " • Không có kết quả phù hợp." : ""));
        });
    }

    private DuLieuKhuyenMai selected() {
        int view = table.getSelectedRow();
        return view < 0 ? null : rows.get(table.convertRowIndexToModel(view));
    }

    private void create() {
        DuLieuKhuyenMai value = promotionDialog(null, true);
        if (value == null) {
            return;
        }
        run(() -> controller.create(value), ignored -> {
            message("Đã tạo khuyến mãi.");
            page = 0;
            load();
        });
    }

    private void editSelected() {
        DuLieuKhuyenMai row = selected();
        if (row == null) {
            return;
        }
        run(() -> controller.detail(row.getMaKM()), fresh -> {
            DuLieuKhuyenMai value = promotionDialog(fresh, false);
            if (value == null) {
                return;
            }
            YeuCauLuuKhuyenMai request = new YeuCauLuuKhuyenMai();
            request.setExpected(fresh);
            request.setValue(value);
            run(() -> controller.update(request), ignored -> {
                message("Đã cập nhật khuyến mãi.");
                load();
            });
        });
    }

    private DuLieuKhuyenMai promotionDialog(DuLieuKhuyenMai original, boolean creating) {
        JTextField code = new JTextField(creating ? "" : safe(original.getMaKM()));
        JTextField name = new JTextField(creating ? "" : safe(original.getTenKM()));
        JTextField rate = new JTextField(creating ? "" : String.valueOf(original.getPhanTramGiam()));
        JTextField start = new JTextField(creating ? "" : String.valueOf(original.getNgayBatDau()));
        JTextField end = new JTextField(creating ? "" : String.valueOf(original.getNgayKetThuc()));
        code.setEnabled(creating);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Mã khuyến mãi *"));
        form.add(code);
        form.add(new JLabel("Tên chương trình *"));
        form.add(name);
        form.add(new JLabel("Tỷ lệ giảm (0–100) *"));
        form.add(rate);
        form.add(new JLabel("Ngày bắt đầu (yyyy-MM-dd) *"));
        form.add(start);
        form.add(new JLabel("Ngày kết thúc (yyyy-MM-dd) *"));
        form.add(end);
        if (JOptionPane.showConfirmDialog(this, form, creating ? "Thêm khuyến mãi" : "Sửa khuyến mãi",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return null;
        }
        try {
            DuLieuKhuyenMai value = new DuLieuKhuyenMai();
            value.setMaKM(code.getText().trim());
            value.setTenKM(name.getText().trim());
            value.setPhanTramGiam(new BigDecimal(rate.getText().trim()));
            value.setNgayBatDau(LocalDate.parse(start.getText().trim()));
            value.setNgayKetThuc(LocalDate.parse(end.getText().trim()));
            value.setTrangThai(creating || original.getTrangThai());
            return value;
        } catch (NumberFormatException | DateTimeParseException invalid) {
            warn("Tỷ lệ phải là số; ngày phải theo yyyy-MM-dd.");
            return null;
        }
    }

    private void toggleSelected() {
        DuLieuKhuyenMai row = selected();
        if (row == null) {
            return;
        }
        boolean activate = !row.getTrangThai();
        String message = activate ? "Khôi phục" : "Ngừng";
        if (JOptionPane.showConfirmDialog(this, message + " chương trình " + row.getTenKM() + "?", "Xác nhận",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        YeuCauTrangThaiKhuyenMai request = new YeuCauTrangThaiKhuyenMai();
        request.setExpected(row);
        run(() -> controller.setActive(request, activate), ignored -> {
            message("Đã cập nhật trạng thái lưu.");
            load();
        });
    }

    private <T> void run(Callable<T> work, Consumer<T> success) {
        if (busy) {
            return;
        }
        busy = true;
        long current = ++sequence;
        status.setText("Đang xử lý với máy chủ...");
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
                    status.setText("Thao tác chưa hoàn tất; danh sách chưa được thay đổi.");
                    Throwable cause = failure.getCause();
                    JOptionPane.showMessageDialog(ManHinhKhuyenMai.this,
                            cause instanceof NgoaiLeGoiDichVu ? cause.getMessage()
                                    : "Không kết nối/xử lý được máy chủ.",
                            "Khuyến mãi", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshControls() {
        boolean selected = selected() != null;
        table.setEnabled(!busy);
        search.setEnabled(!busy);
        add.setEnabled(!busy);
        keyword.setEnabled(!busy);
        activeFilter.setEnabled(!busy);
        edit.setEnabled(!busy && selected);
        toggle.setEnabled(!busy && selected);
        if (selected) {
            toggle.setText(selected().getTrangThai() ? "Ngừng áp dụng" : "Bật lại");
        }
        previous.setEnabled(!busy && page > 0);
        next.setEnabled(!busy && (long) (page + 1) * PAGE_SIZE < total);
    }

    private static String effectiveName(TrangThaiHieuLucKhuyenMai state) {
        if (state == null) {
            return "—";
        }
        switch (state) {
        case CHUA_BAT_DAU:
            return "Chưa bắt đầu";
        case DANG_DIEN_RA:
            return "Đang diễn ra";
        case HET_HAN:
            return "Hết hạn";
        default:
            return state.name();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text, "Khuyến mãi", JOptionPane.INFORMATION_MESSAGE);
    }

    private void warn(String text) {
        JOptionPane.showMessageDialog(this, text, "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
    }
}
