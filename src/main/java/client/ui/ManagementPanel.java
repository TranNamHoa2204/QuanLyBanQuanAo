package client.ui;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import com.fasterxml.jackson.databind.JsonNode;
import client.api.*;
import client.api.ManagementApi.Kind;
import common.dto.*;

/** CRUD phân trang chạy mạng ở worker, không khóa EDT. */
public class ManagementPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final ManagementApi api;
    private final Kind kind;
    private final JTextField query = new JTextField(23);
    private final JCheckBox inactive = new JCheckBox("Cả ngừng hoạt động");
    private final JButton load = new JButton("Tìm / tải lại"), add = new JButton("Thêm mới"), edit = new JButton("Sửa / kích hoạt"),
            stop = new JButton("Ngừng hoạt động"), previous = new JButton("← Trước"), next = new JButton("Sau →");
    private final JLabel pages = UiTheme.muted("Chưa tải dữ liệu");
    private final DefaultTableModel model;
    private final JTable table;
    private List<JsonNode> rows = new ArrayList<>();
    private int page;
    private long total;
    private boolean busy;
    public ManagementPanel(ManagementApi api, Kind kind) {
        super(new BorderLayout(0, 16)); this.api = api; this.kind = kind;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        String title = kind == Kind.CUSTOMER ? "Khách hàng" : kind == Kind.EMPLOYEE ? "Nhân viên & tài khoản" : "Khuyến mãi";
        JPanel top = new JPanel(new BorderLayout(0, 16)); top.setOpaque(false);
        top.add(UiTheme.heading(title, "Tìm theo mã, tên" + (kind == Kind.PROMOTION ? "." : " hoặc số điện thoại.")), BorderLayout.NORTH);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); filters.setOpaque(false);
        filters.add(query); filters.add(inactive); filters.add(load); filters.add(add); top.add(filters, BorderLayout.CENTER); add(top, BorderLayout.NORTH);
        String[] columns = kind == Kind.CUSTOMER ? new String[]{"Mã", "Họ tên", "Điện thoại", "Email", "Hoạt động"}
                : kind == Kind.EMPLOYEE ? new String[]{"Mã", "Họ tên", "Điện thoại", "Tài khoản", "Vai trò", "Hoạt động", "Đăng nhập"}
                : new String[]{"Mã", "Khuyến mãi", "Giảm (%)", "Bắt đầu", "Kết thúc", "Áp dụng"};
        model = new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
        table = new JTable(model); UiTheme.table(table); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(table); tableScroll.setColumnHeaderView(table.getTableHeader());
        add(tableScroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout()); bottom.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setOpaque(false); actions.add(edit); actions.add(stop);
        JPanel paging = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); paging.setOpaque(false); paging.add(pages); paging.add(previous); paging.add(next);
        bottom.add(actions, BorderLayout.WEST); bottom.add(paging, BorderLayout.EAST); add(bottom, BorderLayout.SOUTH);
        UiTheme.primary(add); UiTheme.danger(stop); table.getSelectionModel().addListSelectionListener(e -> controls());
        load.addActionListener(e -> { page = 0; reload(); }); query.addActionListener(e -> { if (!busy) { page = 0; reload(); } });
        previous.addActionListener(e -> { page--; reload(); }); next.addActionListener(e -> { page++; reload(); });
        add.addActionListener(e -> editor(true)); edit.addActionListener(e -> editor(false)); stop.addActionListener(e -> stop());
        controls();
    }
    public void initialize() { if (!busy) { reload(); } }
    private void reload() {
        ManagementSearchRequest r = new ManagementSearchRequest(); r.setQuery(query.getText()); r.setIncludeInactive(inactive.isSelected());
        r.setPage(page); r.setPageSize(25);
        work(() -> api.search(kind, r), this::showResult);
    }
    public void showResult(ManagementSearchResult result) {
        rows = new ArrayList<>(result.getItems()); total = result.getTotal(); model.setRowCount(0);
        for (JsonNode d : rows) {
            if (kind == Kind.CUSTOMER) { model.addRow(new Object[]{text(d, "maKH"), text(d, "hoTen"), text(d, "sdt"), text(d, "email"), flag(d, "trangThai")}); }
            else if (kind == Kind.EMPLOYEE) { model.addRow(new Object[]{text(d, "maNV"), text(d, "hoTen"), text(d, "sdt"), text(d, "username"),
                    text(d, "vaiTro").equals("NHAN_VIEN_QUAN_LY") ? "Quản lý" : text(d, "username").isEmpty() ? "—" : "Bán hàng",
                    flag(d, "trangThai"), flag(d, "taiKhoanHoatDong")}); }
            else { model.addRow(new Object[]{text(d, "maKM"), text(d, "tenKM"), text(d, "phanTramGiam"), text(d, "ngayBatDau"), text(d, "ngayKetThuc"), flag(d, "trangThai")}); }
        }
        pages.setText("Trang " + (page + 1) + " · " + total + " bản ghi"); controls();
    }
    private JsonNode selected() {
        int index = table.getSelectedRow();
        if (index < 0) { return null; }
        int row = table.convertRowIndexToModel(index); return row >= rows.size() ? null : rows.get(row);
    }
    private void editor(boolean create) {
        JsonNode before = create ? null : selected(); if (!create && before == null) { return; }
        ManagementEditorPanel form = new ManagementEditorPanel(kind, before, create);
        JScrollPane scroll = new JScrollPane(form); scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(650, kind == Kind.EMPLOYEE ? 530 : 390));
        while (JOptionPane.showConfirmDialog(this, scroll, create ? "Thêm mới" : "Cập nhật thông tin", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                ManagementSaveRequest r = new ManagementSaveRequest(); r.setValue(form.read()); r.setExpected(before);
                r.setInitialPassword(form.initialPassword());
                work(() -> api.save(kind, r, create ? 0 : 1), value -> {
                    if (create) { page = 0; }
                    JOptionPane.showMessageDialog(this, "Đã lưu thông tin."); reload();
                }); return;
            } catch (IllegalArgumentException | java.time.format.DateTimeParseException invalid) {
                JOptionPane.showMessageDialog(this, "Kiểm tra số phần trăm và ngày (yyyy-MM-dd).", "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            }
        }
        form.initialPassword(); // xóa mật khẩu trên form khi hủy
    }
    private void stop() {
        JsonNode before = selected(); if (before == null) { return; }
        if (JOptionPane.showConfirmDialog(this, "Ngừng hoạt động bản ghi này? Lịch sử hóa đơn vẫn được giữ.",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) { return; }
        ManagementSaveRequest r = new ManagementSaveRequest(); r.setValue(before); r.setExpected(before);
        work(() -> api.save(kind, r, 2), value -> reload());
    }
    private <T> void work(Callable<T> task, Consumer<T> result) {
        if (busy) { return; } busy = true; controls();
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                busy = false; controls();
                try { result.accept(get()); }
                catch (Exception e) {
                    Throwable cause = e instanceof java.util.concurrent.ExecutionException ? e.getCause() : e;
                    JOptionPane.showMessageDialog(ManagementPanel.this, cause instanceof ApiException ? cause.getMessage()
                            : "Không xử lý được yêu cầu. Kiểm tra kết nối máy chủ và tải lại.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }
    private void controls() {
        load.setEnabled(!busy); add.setEnabled(!busy); query.setEnabled(!busy); inactive.setEnabled(!busy); table.setEnabled(!busy);
        edit.setEnabled(!busy && selected() != null); stop.setEnabled(!busy && selected() != null && selected().path("trangThai").asBoolean());
        previous.setEnabled(!busy && page > 0); next.setEnabled(!busy && (long) (page + 1) * 25 < total);
    }
    private static String text(JsonNode d, String key) { return d.path(key).isNull() ? "" : d.path(key).asText(""); }
    private static String flag(JsonNode d, String key) { return d.path(key).asBoolean() ? "Có" : "Không"; }
}
