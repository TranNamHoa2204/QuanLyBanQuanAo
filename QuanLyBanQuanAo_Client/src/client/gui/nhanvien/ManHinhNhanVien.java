package client.gui.nhanvien;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import client.api.NgoaiLeGoiDichVu;
import client.controllers.DieuKhienNhanVien;
import client.gui.components.BieuTuongCoDien;
import client.gui.components.ChuDeGiaoDien;
import client.gui.components.TienIchBangSanPham;
import common.dto.nhanvien.DuLieuHoSoNhanVien;
import common.dto.nhanvien.DuLieuNhanVien;
import common.dto.nhanvien.YeuCauLuuNhanVien;
import common.dto.nhanvien.YeuCauTaoNhanVien;
import common.dto.nhanvien.YeuCauTraCuuNhanVien;
import common.dto.nhanvien.YeuCauTrangThaiNhanVien;
import common.dto.taikhoan.DuLieuTaiKhoan;
import common.dto.taikhoan.YeuCauTaoTaiKhoan;
import common.dto.taikhoan.YeuCauTrangThaiTaiKhoan;
import common.dto.taikhoan.YeuCauVaiTroTaiKhoan;
import common.enums.VaiTro;

/** Manager UI for employee profiles and linked application accounts. */
public final class ManHinhNhanVien extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 20;
    private final DieuKhienNhanVien controller;
    private final JTextField keyword = new JTextField(18);
    private final JComboBox<String> statusFilter = new JComboBox<String>(
            new String[] { "Tất cả", "Đang hoạt động", "Ngừng hoạt động" });
    private final JButton search = new JButton("Tìm nhân viên", BieuTuongCoDien.TIM_KIEM), add = new JButton("Thêm nhân viên", BieuTuongCoDien.THEM),
            edit = new JButton("Sửa hồ sơ"), createAccount = new JButton("Cấp tài khoản", BieuTuongCoDien.KHOA),
            employeeStatus = new JButton("Ngừng hoạt động"), accountStatus = new JButton("Khóa tài khoản"),
            role = new JButton("Đổi quyền"), previous = new JButton("Trang trước"), next = new JButton("Trang sau");
    private final JLabel status = ChuDeGiaoDien.muted("Chưa tải danh sách nhân viên.");
    private final DefaultTableModel model = TienIchBangSanPham.model(
            new String[] { "Mã NV", "Họ tên", "Số điện thoại", "Chức vụ", "Nhân viên", "Username", "Vai trò",
                    "Tài khoản" },
            new Class<?>[] { String.class, String.class, String.class, String.class, String.class,
                    String.class, String.class, String.class });
    private final JTable table = ChuDeGiaoDien.tableWithHorizontalScroll(model,
            110, 210, 125, 135, 100, 130, 165, 100);
    private List<DuLieuHoSoNhanVien> rows = new ArrayList<DuLieuHoSoNhanVien>();
    private int page;
    private long total;
    private boolean busy;
    private long sequence;

    public ManHinhNhanVien(DieuKhienNhanVien controller) {
        super(new BorderLayout(0, 14));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(8, 16, 14, 16));
        JPanel top = ChuDeGiaoDien.card(new BorderLayout(0, 10), 14);
        top.add(ChuDeGiaoDien.heading("Quản lý nhân viên và tài khoản",
                "Tìm theo mã, tên hoặc thông tin liên hệ. Chọn nhân viên để sửa hồ sơ hoặc tài khoản."),
                BorderLayout.NORTH);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(ChuDeGiaoDien.muted("Mã, tên hoặc liên hệ"));
        filters.add(keyword);
        filters.add(ChuDeGiaoDien.muted("Trạng thái nhân viên"));
        filters.add(statusFilter);
        filters.add(search);
        filters.add(add);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        for (JButton button : new JButton[] { edit, createAccount, employeeStatus, accountStatus,
                role }) {
            actions.add(button);
        }
        ChuDeGiaoDien.primary(add);
        top.add(filters, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        TienIchBangSanPham.configure(table);
        add(ChuDeGiaoDien.tableScroll(table), BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        footer.setOpaque(false);
        footer.add(previous);
        footer.add(next);
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(footer, BorderLayout.WEST);
        bottom.add(status, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        search.addActionListener(e -> {
            page = 0;
            load();
        });
        keyword.addActionListener(e -> {
            page = 0;
            load();
        });
        statusFilter.addActionListener(e -> {
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
        add.addActionListener(e -> createEmployee());
        edit.addActionListener(e -> editSelected());
        createAccount.addActionListener(e -> createAccountForSelected());
        employeeStatus.addActionListener(e -> changeEmployeeStatus());
        accountStatus.addActionListener(e -> changeAccountStatus());
        role.addActionListener(e -> changeRole());
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
        YeuCauTraCuuNhanVien request = new YeuCauTraCuuNhanVien();
        String text = keyword.getText() == null ? "" : keyword.getText().trim();
        request.setTuKhoa(text.isEmpty() ? null : text);
        int filter = statusFilter.getSelectedIndex();
        request.setTrangThai(filter == 0 ? null : Boolean.valueOf(filter == 1));
        request.setPage(page);
        request.setPageSize(PAGE_SIZE);
        run(() -> controller.search(request), result -> {
            rows = result.getItems() == null ? new ArrayList<DuLieuHoSoNhanVien>() : result.getItems();
            total = result.getTotal();
            page = result.getPage();
            model.setRowCount(0);
            for (DuLieuHoSoNhanVien row : rows) {
                DuLieuNhanVien employee = row.getNhanVien();
                DuLieuTaiKhoan account = row.getTaiKhoan();
                model.addRow(new Object[] { employee.getMaNV(), employee.getHoTen(), employee.getSdt(),
                        employee.getChucVu(), employee.getTrangThai() ? "Hoạt động" : "Ngừng",
                        account == null ? "—" : account.getUsername(),
                        account == null ? "—" : roleName(account.getVaiTro()),
                        account == null ? "Chưa cấp" : account.getTrangThai() ? "Hoạt động" : "Đã khóa" });
            }
            long pages = Math.max(1L, (total + PAGE_SIZE - 1L) / PAGE_SIZE);
            status.setText(total + " nhân viên • Trang " + (page + 1) + "/" + pages
                    + (rows.isEmpty() ? " • Không có kết quả phù hợp." : ""));
        });
    }

    private DuLieuHoSoNhanVien selected() {
        int view = table.getSelectedRow();
        return view < 0 ? null : rows.get(table.convertRowIndexToModel(view));
    }

    private void createEmployee() {
        EmployeeDraft draft = employeeDialog(null, true);
        if (draft == null) {
            return;
        }
        YeuCauTaoNhanVien request = new YeuCauTaoNhanVien();
        request.setNhanVien(draft.employee);
        request.setTaiKhoan(draft.account);
        run(() -> controller.create(request), result -> {
            message("Đã tạo nhân viên" + (result.getTaiKhoan() == null ? "." : " và tài khoản."));
            page = 0;
            load();
        });
    }

    private void editSelected() {
        final DuLieuHoSoNhanVien current = selected();
        if (current == null) {
            return;
        }
        run(() -> controller.detail(current.getNhanVien().getMaNV()), fresh -> {
            EmployeeDraft draft = employeeDialog(fresh.getNhanVien(), false);
            if (draft == null) {
                return;
            }
            YeuCauLuuNhanVien request = new YeuCauLuuNhanVien();
            request.setExpected(fresh.getNhanVien());
            request.setValue(draft.employee);
            run(() -> controller.update(request), ignored -> {
                message("Đã cập nhật hồ sơ nhân viên.");
                load();
            });
        });
    }

    private EmployeeDraft employeeDialog(DuLieuNhanVien original, boolean creating) {
        JTextField id = new JTextField(creating ? "" : safe(original.getMaNV()));
        JTextField name = new JTextField(creating ? "" : safe(original.getHoTen()));
        JTextField phone = new JTextField(creating ? "" : safe(original.getSdt()));
        JTextField email = new JTextField(creating ? "" : safe(original.getEmail()));
        JTextField address = new JTextField(creating ? "" : safe(original.getDiaChi()));
        JTextField title = new JTextField(creating ? "" : safe(original.getChucVu()));
        JTextField birth = new JTextField(
                creating || original.getNgaySinh() == null ? "" : original.getNgaySinh().toString());
        JComboBox<String> gender = new JComboBox<String>(new String[] { "Chưa chọn", "Nam", "Nữ" });
        if (!creating && original.getGioiTinh() != null) {
            gender.setSelectedIndex(original.getGioiTinh() ? 1 : 2);
        }
        id.setEditable(false);
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.add(new JLabel("Mã nhân viên"));
        fields.add(id);
        fields.add(new JLabel("Họ tên *"));
        fields.add(name);
        fields.add(new JLabel("Số điện thoại *"));
        fields.add(phone);
        fields.add(new JLabel("Email"));
        fields.add(email);
        fields.add(new JLabel("Địa chỉ"));
        fields.add(address);
        fields.add(new JLabel("Chức vụ"));
        fields.add(title);
        fields.add(new JLabel("Ngày sinh (yyyy-MM-dd)"));
        fields.add(birth);
        fields.add(new JLabel("Giới tính"));
        fields.add(gender);
        JCheckBox withAccount = new JCheckBox("Tạo tài khoản cùng nhân viên");
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<VaiTro> accountRole = roleChoice(VaiTro.NHAN_VIEN_BAN_HANG);
        if (creating) {
            fields.add(withAccount);
            fields.add(new JLabel(""));
            fields.add(new JLabel("Username"));
            fields.add(username);
            fields.add(new JLabel("Mật khẩu ban đầu (tối thiểu 8 ký tự)"));
            fields.add(password);
            fields.add(new JLabel("Vai trò"));
            fields.add(accountRole);
            withAccount.addActionListener(e -> {
                username.setEnabled(withAccount.isSelected());
                password.setEnabled(withAccount.isSelected());
                accountRole.setEnabled(withAccount.isSelected());
            });
            username.setEnabled(false);
            password.setEnabled(false);
            accountRole.setEnabled(false);
        }
        int choice = JOptionPane.showConfirmDialog(this, new JScrollPane(fields),
                creating ? "Thêm nhân viên" : "Cập nhật hồ sơ", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }
        DuLieuNhanVien value = new DuLieuNhanVien();
        value.setMaNV(id.getText());
        value.setHoTen(name.getText());
        value.setSdt(phone.getText());
        value.setEmail(blankToNull(email.getText()));
        value.setDiaChi(blankToNull(address.getText()));
        value.setChucVu(blankToNull(title.getText()));
        value.setTrangThai(creating || original.getTrangThai());
        int selectedGender = gender.getSelectedIndex();
        value.setGioiTinh(selectedGender == 0 ? null : Boolean.valueOf(selectedGender == 1));
        try {
            value.setNgaySinh(
                    blankToNull(birth.getText()) == null ? null : LocalDate.parse(birth.getText().trim()));
        } catch (DateTimeParseException invalid) {
            warn("Ngày sinh phải theo định dạng yyyy-MM-dd.");
            return null;
        }
        YeuCauTaoTaiKhoan account = null;
        if (creating && withAccount.isSelected()) {
            char[] chars = password.getPassword();
            try {
                account = new YeuCauTaoTaiKhoan();
                account.setMaNV(value.getMaNV());
                account.setUsername(username.getText());
                account.setMatKhauBanDau(new String(chars));
                account.setVaiTro((VaiTro) accountRole.getSelectedItem());
            } finally {
                java.util.Arrays.fill(chars, '\0');
                password.setText("");
            }
        }
        return new EmployeeDraft(value, account);
    }

    private void createAccountForSelected() {
        final DuLieuHoSoNhanVien row = selected();
        if (row == null || row.getTaiKhoan() != null) {
            return;
        }
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<VaiTro> roleChoice = roleChoice(VaiTro.NHAN_VIEN_BAN_HANG);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Nhân viên"));
        form.add(new JLabel(row.getNhanVien().getMaNV() + " — " + row.getNhanVien().getHoTen()));
        form.add(new JLabel("Username"));
        form.add(username);
        form.add(new JLabel("Mật khẩu ban đầu"));
        form.add(password);
        form.add(new JLabel("Vai trò"));
        form.add(roleChoice);
        if (JOptionPane.showConfirmDialog(this, form, "Tạo tài khoản",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        char[] chars = password.getPassword();
        final YeuCauTaoTaiKhoan request = new YeuCauTaoTaiKhoan();
        try {
            request.setMaNV(row.getNhanVien().getMaNV());
            request.setUsername(username.getText());
            request.setMatKhauBanDau(new String(chars));
            request.setVaiTro((VaiTro) roleChoice.getSelectedItem());
        } finally {
            java.util.Arrays.fill(chars, '\0');
            password.setText("");
        }
        run(() -> controller.createAccount(request), ignored -> {
            message("Đã tạo tài khoản.");
            load();
        });
    }

    private void changeEmployeeStatus() {
        final DuLieuHoSoNhanVien row = selected();
        if (row == null) {
            return;
        }
        final boolean activate = !row.getNhanVien().getTrangThai();
        if (!confirm((activate ? "Khôi phục" : "Ngừng hoạt động") + " nhân viên "
                + row.getNhanVien().getHoTen() + "?")) {
            return;
        }
        YeuCauTrangThaiNhanVien request = new YeuCauTrangThaiNhanVien();
        request.setMaNV(row.getNhanVien().getMaNV());
        request.setExpectedTrangThai(row.getNhanVien().getTrangThai());
        run(() -> controller.employeeActive(request, activate), ignored -> {
            message("Đã cập nhật trạng thái nhân viên.");
            load();
        });
    }

    private void changeAccountStatus() {
        final DuLieuHoSoNhanVien row = selected();
        final DuLieuTaiKhoan account = row == null ? null : row.getTaiKhoan();
        if (account == null) {
            return;
        }
        final boolean activate = !account.getTrangThai();
        if (!confirm((activate ? "Mở khóa" : "Khóa") + " tài khoản " + account.getUsername() + "?")) {
            return;
        }
        YeuCauTrangThaiTaiKhoan request = new YeuCauTrangThaiTaiKhoan();
        request.setUsername(account.getUsername());
        request.setExpectedTrangThai(account.getTrangThai());
        run(() -> controller.accountActive(request, activate), ignored -> {
            message("Đã cập nhật trạng thái tài khoản.");
            load();
        });
    }

    private void changeRole() {
        final DuLieuHoSoNhanVien row = selected();
        final DuLieuTaiKhoan account = row == null ? null : row.getTaiKhoan();
        if (account == null) {
            return;
        }
        JComboBox<VaiTro> choices = roleChoice(account.getVaiTro());
        if (JOptionPane.showConfirmDialog(this,
                ChuDeGiaoDien.field("Quyền của " + account.getUsername(), choices), "Đổi quyền",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        VaiTro target = (VaiTro) choices.getSelectedItem();
        if (target == null || target == account.getVaiTro()) {
            return;
        }
        YeuCauVaiTroTaiKhoan request = new YeuCauVaiTroTaiKhoan();
        request.setUsername(account.getUsername());
        request.setExpectedVaiTro(account.getVaiTro());
        request.setVaiTro(target);
        run(() -> controller.role(request), ignored -> {
            message("Đã cập nhật vai trò.");
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
                    Throwable cause = failure.getCause();
                    status.setText("Thao tác chưa hoàn tất; dữ liệu trên bảng chưa được thay đổi.");
                    JOptionPane.showMessageDialog(ManHinhNhanVien.this,
                            cause instanceof NgoaiLeGoiDichVu ? cause.getMessage()
                                    : "Không kết nối/xử lý được máy chủ.",
                            "Quản lý nhân viên", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshControls() {
        DuLieuHoSoNhanVien row = selected();
        boolean selected = row != null;
        boolean hasAccount = selected && row.getTaiKhoan() != null;
        search.setEnabled(!busy);
        add.setEnabled(!busy);
        keyword.setEnabled(!busy);
        statusFilter.setEnabled(!busy);
        edit.setEnabled(!busy && selected);
        createAccount.setEnabled(!busy && selected && !hasAccount);
        employeeStatus.setEnabled(!busy && selected);
        accountStatus.setEnabled(!busy && hasAccount);
        employeeStatus.setText(selected && !row.getNhanVien().getTrangThai() ? "Khôi phục hoạt động"
                : "Ngừng hoạt động");
        accountStatus.setText(hasAccount && !row.getTaiKhoan().getTrangThai() ? "Mở khóa tài khoản"
                : "Khóa tài khoản");
        role.setEnabled(!busy && hasAccount);
        previous.setEnabled(!busy && page > 0);
        next.setEnabled(!busy && (long) (page + 1) * PAGE_SIZE < total);
    }

    private boolean confirm(String text) {
        return JOptionPane.showConfirmDialog(this, text, "Xác nhận",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text, "Nhân viên", JOptionPane.INFORMATION_MESSAGE);
    }

    private void warn(String text) {
        JOptionPane.showMessageDialog(this, text, "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String roleName(VaiTro role) {
        return role == null ? "—" : role == VaiTro.NHAN_VIEN_QUAN_LY ? "Quản lý" : "Bán hàng";
    }

    private static JComboBox<VaiTro> roleChoice(VaiTro selected) {
        JComboBox<VaiTro> combo = new JComboBox<>(VaiTro.values());
        combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, roleName((VaiTro) value), index, selected, focus);
            }
        });
        combo.setSelectedItem(selected);
        return combo;
    }

    private static final class EmployeeDraft {
        final DuLieuNhanVien employee;
        final YeuCauTaoTaiKhoan account;

        EmployeeDraft(DuLieuNhanVien employee, YeuCauTaoTaiKhoan account) {
            this.employee = employee;
            this.account = account;
        }
    }
}
