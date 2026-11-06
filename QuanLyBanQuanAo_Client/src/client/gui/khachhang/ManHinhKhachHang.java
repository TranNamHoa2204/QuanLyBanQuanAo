package client.gui.khachhang;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
import client.controllers.DieuKhienKhachHang;
import client.gui.components.BieuTuongCoDien;
import client.gui.components.ChuDeGiaoDien;
import client.gui.components.TienIchBangSanPham;
import common.dto.khachhang.DuLieuKhachHang;
import common.dto.khachhang.YeuCauLuuKhachHang;
import common.dto.khachhang.YeuCauTraCuuKhachHang;
import common.dto.khachhang.YeuCauTrangThaiKhachHang;

/** Customer profile lookup and non-destructive status management. */
public final class ManHinhKhachHang extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 20;
    private final DieuKhienKhachHang controller;
    private final JTextField keyword = new JTextField(18);
    private final JComboBox<String> activeFilter = new JComboBox<String>(
            new String[] { "Tất cả", "Đang hoạt động", "Ngừng hoạt động" });
    private final JButton search = new JButton("Tìm khách hàng", BieuTuongCoDien.TIM_KIEM), add = new JButton("Thêm khách hàng", BieuTuongCoDien.THEM),
            edit = new JButton("Sửa thông tin"), toggle = new JButton("Ngừng hoạt động"), previous = new JButton("Trang trước"),
            next = new JButton("Trang sau");
    private final JLabel status = ChuDeGiaoDien.muted("Chưa tải danh sách khách hàng.");
    private final DefaultTableModel model = TienIchBangSanPham.model(
            new String[] { "Mã KH", "Họ tên", "Số điện thoại", "Email", "Địa chỉ", "Trạng thái" },
            new Class<?>[] { String.class, String.class, String.class, String.class, String.class,
                    String.class });
    private final JTable table = ChuDeGiaoDien.tableWithHorizontalScroll(model,
            125, 210, 135, 210, 235, 110);
    private List<DuLieuKhachHang> rows = new ArrayList<DuLieuKhachHang>();
    private int page;
    private long total;
    private boolean busy;
    private long sequence;

    public ManHinhKhachHang(DieuKhienKhachHang controller) {
        super(new BorderLayout(0, 14));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(8, 16, 14, 16));
        JPanel top = ChuDeGiaoDien.card(new BorderLayout(0, 10), 14);
        top.add(ChuDeGiaoDien.heading("Quản lý khách hàng",
                "Tìm theo mã, tên hoặc số điện thoại. Chọn khách hàng để sửa thông tin."),
                BorderLayout.NORTH);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(ChuDeGiaoDien.muted("Mã, tên hoặc số điện thoại"));
        filters.add(keyword);
        filters.add(ChuDeGiaoDien.muted("Trạng thái"));
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
        add.addActionListener(e -> create());
        edit.addActionListener(e -> editSelected());
        toggle.addActionListener(e -> toggleSelected());
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
        YeuCauTraCuuKhachHang request = new YeuCauTraCuuKhachHang();
        String text = keyword.getText() == null ? "" : keyword.getText().trim();
        request.setTuKhoa(text.isEmpty() ? null : text);
        int filter = activeFilter.getSelectedIndex();
        request.setTrangThai(filter == 0 ? null : Boolean.valueOf(filter == 1));
        request.setPage(page);
        request.setPageSize(PAGE_SIZE);
        run(() -> controller.search(request), result -> {
            rows = result.getItems() == null ? new ArrayList<DuLieuKhachHang>() : result.getItems();
            total = result.getTotal();
            page = result.getPage();
            model.setRowCount(0);
            for (DuLieuKhachHang row : rows) {
                model.addRow(new Object[] { row.getMaKH(), row.getHoTen(), row.getSdt(), row.getEmail(),
                        row.getDiaChi(), row.getTrangThai() ? "Hoạt động" : "Ngừng" });
            }
            long pages = Math.max(1L, (total + PAGE_SIZE - 1L) / PAGE_SIZE);
            status.setText(total + " khách hàng • Trang " + (page + 1) + "/" + pages
                    + (rows.isEmpty() ? " • Không có kết quả phù hợp." : ""));
        });
    }

    private DuLieuKhachHang selected() {
        int view = table.getSelectedRow();
        return view < 0 ? null : rows.get(table.convertRowIndexToModel(view));
    }

    private void create() {
        DuLieuKhachHang value = customerDialog(null, true);
        if (value == null) {
            return;
        }
        run(() -> controller.create(value), ignored -> {
            message("Đã tạo khách hàng.");
            page = 0;
            load();
        });
    }

    private void editSelected() {
        DuLieuKhachHang row = selected();
        if (row == null) {
            return;
        }
        run(() -> controller.detail(row.getMaKH()), fresh -> {
            DuLieuKhachHang value = customerDialog(fresh, false);
            if (value == null) {
                return;
            }
            YeuCauLuuKhachHang request = new YeuCauLuuKhachHang();
            request.setExpected(fresh);
            request.setValue(value);
            run(() -> controller.update(request), ignored -> {
                message("Đã cập nhật khách hàng.");
                load();
            });
        });
    }

    private DuLieuKhachHang customerDialog(DuLieuKhachHang original, boolean creating) {
        JTextField code = new JTextField(creating ? "" : safe(original.getMaKH()));
        JTextField name = new JTextField(creating ? "" : safe(original.getHoTen()));
        JTextField phone = new JTextField(creating ? "" : safe(original.getSdt()));
        JTextField email = new JTextField(creating ? "" : safe(original.getEmail()));
        JTextField address = new JTextField(creating ? "" : safe(original.getDiaChi()));
        code.setEnabled(creating);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Mã khách hàng *"));
        form.add(code);
        form.add(new JLabel("Họ tên *"));
        form.add(name);
        form.add(new JLabel("Số điện thoại *"));
        form.add(phone);
        form.add(new JLabel("Email"));
        form.add(email);
        form.add(new JLabel("Địa chỉ"));
        form.add(address);
        if (JOptionPane.showConfirmDialog(this, form, creating ? "Thêm khách hàng" : "Sửa khách hàng",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return null;
        }
        DuLieuKhachHang value = new DuLieuKhachHang();
        value.setMaKH(code.getText());
        value.setHoTen(name.getText());
        value.setSdt(phone.getText());
        value.setEmail(blankToNull(email.getText()));
        value.setDiaChi(blankToNull(address.getText()));
        value.setTrangThai(creating || original.getTrangThai());
        return value;
    }

    private void toggleSelected() {
        DuLieuKhachHang row = selected();
        if (row == null) {
            return;
        }
        boolean activate = !row.getTrangThai();
        if (JOptionPane.showConfirmDialog(this, (activate ? "Khôi phục " : "Ngừng ") + row.getHoTen() + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        YeuCauTrangThaiKhachHang request = new YeuCauTrangThaiKhachHang();
        request.setMaKH(row.getMaKH());
        request.setExpectedTrangThai(row.getTrangThai());
        run(() -> controller.setActive(request, activate), ignored -> {
            message("Đã cập nhật trạng thái khách hàng.");
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
                    JOptionPane.showMessageDialog(ManHinhKhachHang.this,
                            cause instanceof NgoaiLeGoiDichVu ? cause.getMessage()
                                    : "Không kết nối/xử lý được máy chủ.",
                            "Khách hàng", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshControls() {
        DuLieuKhachHang row = selected();
        boolean hasSelection = row != null;
        table.setEnabled(!busy);
        search.setEnabled(!busy);
        add.setEnabled(!busy);
        keyword.setEnabled(!busy);
        activeFilter.setEnabled(!busy);
        edit.setEnabled(!busy && hasSelection);
        toggle.setEnabled(!busy && hasSelection);
        if (hasSelection) {
            toggle.setText(row.getTrangThai() ? "Ngừng hoạt động" : "Khôi phục hoạt động");
        }
        previous.setEnabled(!busy && page > 0);
        next.setEnabled(!busy && (long) (page + 1) * PAGE_SIZE < total);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text, "Khách hàng", JOptionPane.INFORMATION_MESSAGE);
    }
}
