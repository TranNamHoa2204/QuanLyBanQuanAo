package client.gui.nhapkho;

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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import client.api.NgoaiLeGoiDichVu;
import client.controllers.DieuKhienNhaCungCap;
import client.gui.components.BieuTuongCoDien;
import client.gui.components.ChuDeGiaoDien;
import client.utils.SinhMaTuDong;
import client.gui.components.TienIchBangSanPham;
import common.dto.nhacungcap.DuLieuNhaCungCap;
import common.dto.nhacungcap.YeuCauLuuNhaCungCap;
import common.dto.nhacungcap.YeuCauTraCuuNhaCungCap;
import common.dto.nhacungcap.YeuCauTrangThaiNhaCungCap;

/** Supplier directory with paging, snapshot edits and non-destructive deactivation. */
public final class ManHinhNhaCungCap extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 20;
    private final DieuKhienNhaCungCap controller;
    private final JTextField keyword = new JTextField(18);
    private final JComboBox<String> activeFilter = new JComboBox<String>(
            new String[] { "Tất cả", "Đang hoạt động", "Ngừng hoạt động" });
    private final JButton search = new JButton("Tìm nhà cung cấp", BieuTuongCoDien.TIM_KIEM), add = new JButton("Thêm nhà cung cấp", BieuTuongCoDien.THEM),
            edit = new JButton("Sửa thông tin"), toggle = new JButton("Ngừng hợp tác"), previous = new JButton("Trang trước"),
            next = new JButton("Trang sau");
    private final JLabel status = ChuDeGiaoDien.muted("Chưa tải danh sách nhà cung cấp.");
    private final DefaultTableModel model = TienIchBangSanPham.model(
            new String[] { "Mã NCC", "Tên nhà cung cấp", "Số điện thoại", "Email", "Địa chỉ", "Trạng thái" },
            new Class<?>[] { String.class, String.class, String.class, String.class, String.class,
                    String.class });
    private final JTable table = ChuDeGiaoDien.tableWithHorizontalScroll(model,
            125, 275, 135, 220, 235, 110);
    private List<DuLieuNhaCungCap> rows = new ArrayList<DuLieuNhaCungCap>();
    private int page;
    private long total, sequence;
    private boolean busy;

    public ManHinhNhaCungCap(DieuKhienNhaCungCap controller) {
        super(new BorderLayout(0, 14));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(8, 16, 14, 16));
        JPanel top = ChuDeGiaoDien.card(new BorderLayout(0, 10), 14);
        top.add(ChuDeGiaoDien.heading("Nhà cung cấp",
                "Tìm theo mã, tên hoặc thông tin liên hệ. Chọn nhà cung cấp để sửa hồ sơ."),
                BorderLayout.NORTH);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(ChuDeGiaoDien.muted("Mã, tên hoặc liên hệ"));
        controls.add(keyword);
        controls.add(ChuDeGiaoDien.muted("Trạng thái"));
        controls.add(activeFilter);
        controls.add(search);
        controls.add(add);
        ChuDeGiaoDien.danger(toggle);
        ChuDeGiaoDien.primary(add);
        top.add(controls, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        TienIchBangSanPham.configure(table);
        JScrollPane tableScroll = ChuDeGiaoDien.tableScroll(table);
        add(tableScroll, BorderLayout.CENTER);
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
        YeuCauTraCuuNhaCungCap r = new YeuCauTraCuuNhaCungCap();
        String q = blank(keyword.getText());
        r.setTuKhoa(q);
        int state = activeFilter.getSelectedIndex();
        r.setTrangThai(state == 0 ? null : Boolean.valueOf(state == 1));
        r.setPage(page);
        r.setPageSize(PAGE_SIZE);
        run(() -> controller.search(r), result -> {
            rows = result.getItems() == null ? new ArrayList<DuLieuNhaCungCap>() : result.getItems();
            total = result.getTotal();
            page = result.getPage();
            model.setRowCount(0);
            for (DuLieuNhaCungCap row : rows) {
                model.addRow(new Object[] { row.getMaNCC(), row.getTenNCC(), row.getSdt(), row.getEmail(),
                        row.getDiaChi(), row.getTrangThai() ? "Hoạt động" : "Ngừng" });
            }
            status.setText(total + " nhà cung cấp • Trang " + (page + 1) + "/"
                    + Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE)
                    + (rows.isEmpty() ? " • Không có kết quả." : ""));
        });
    }

    private DuLieuNhaCungCap selected() {
        int r = table.getSelectedRow();
        return r < 0 ? null : rows.get(table.convertRowIndexToModel(r));
    }

    private void create() {
        DuLieuNhaCungCap value = dialog(null, true);
        if (value != null)
            run(() -> controller.create(value), ignored -> {
                message("Đã tạo nhà cung cấp.");
                page = 0;
                load();
            });
    }

    private void editSelected() {
        DuLieuNhaCungCap old = selected();
        if (old == null)
            return;
        DuLieuNhaCungCap value = dialog(old, false);
        if (value == null)
            return;
        YeuCauLuuNhaCungCap r = new YeuCauLuuNhaCungCap();
        r.setExpected(old);
        r.setValue(value);
        run(() -> controller.update(r), ignored -> {
            message("Đã cập nhật nhà cung cấp.");
            load();
        });
    }

    private DuLieuNhaCungCap dialog(DuLieuNhaCungCap old, boolean creating) {
        JTextField code = new JTextField(creating ? SinhMaTuDong.tao("NCC") : safe(old.getMaNCC())),
                name = new JTextField(creating ? "" : safe(old.getTenNCC())),
                phone = new JTextField(creating ? "" : safe(old.getSdt())),
                email = new JTextField(creating ? "" : safe(old.getEmail())),
                address = new JTextField(creating ? "" : safe(old.getDiaChi()));
        code.setEditable(false);
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Mã NCC (tự sinh)"));
        p.add(code);
        p.add(new JLabel("Tên nhà cung cấp *"));
        p.add(name);
        p.add(new JLabel("Số điện thoại"));
        p.add(phone);
        p.add(new JLabel("Email"));
        p.add(email);
        p.add(new JLabel("Địa chỉ"));
        p.add(address);
        if (JOptionPane.showConfirmDialog(this, p, creating ? "Thêm nhà cung cấp" : "Sửa nhà cung cấp",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return null;
        DuLieuNhaCungCap d = new DuLieuNhaCungCap();
        d.setMaNCC(code.getText().trim());
        d.setTenNCC(name.getText().trim());
        d.setSdt(blank(phone.getText()));
        d.setEmail(blank(email.getText()));
        d.setDiaChi(blank(address.getText()));
        d.setTrangThai(creating || old.getTrangThai());
        d.setPhienBan(creating ? null : old.getPhienBan());
        return d;
    }

    private void toggleSelected() {
        DuLieuNhaCungCap row = selected();
        if (row == null)
            return;
        boolean active = !row.getTrangThai();
        if (JOptionPane.showConfirmDialog(this, (active ? "Khôi phục " : "Ngừng ") + row.getTenNCC() + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;
        YeuCauTrangThaiNhaCungCap r = new YeuCauTrangThaiNhaCungCap();
        r.setMaNCC(row.getMaNCC());
        r.setExpectedVersion(row.getPhienBan());
        run(() -> controller.setActive(r, active), ignored -> {
            message("Đã cập nhật trạng thái.");
            load();
        });
    }

    private <T> void run(Callable<T> work, Consumer<T> success) {
        if (busy)
            return;
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
                if (current != sequence)
                    return;
                busy = false;
                refreshControls();
                try {
                    success.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    String m = cause instanceof NgoaiLeGoiDichVu ? cause.getMessage()
                            : "Không tải/lưu được. Kiểm tra kết nối tới máy chủ.";
                    status.setText("Thao tác chưa hoàn tất.");
                    JOptionPane.showMessageDialog(ManHinhNhaCungCap.this, m, "Nhà cung cấp",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshControls() {
        DuLieuNhaCungCap row = selected();
        boolean has = row != null;
        search.setEnabled(!busy);
        keyword.setEnabled(!busy);
        activeFilter.setEnabled(!busy);
        add.setEnabled(!busy);
        edit.setEnabled(!busy && has);
        toggle.setEnabled(!busy && has);
        if (has)
            toggle.setText(row.getTrangThai() ? "Ngừng hợp tác" : "Khôi phục hợp tác");
        previous.setEnabled(!busy && page > 0);
        next.setEnabled(!busy && (long) (page + 1) * PAGE_SIZE < total);
    }

    private static String blank(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private void message(String s) {
        JOptionPane.showMessageDialog(this, s, "Nhà cung cấp", JOptionPane.INFORMATION_MESSAGE);
    }
}
