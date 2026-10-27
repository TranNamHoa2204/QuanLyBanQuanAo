package client.ui;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import common.dto.NhanVienDTO;

public final class NhanVienProfilePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Map<String, JTextField> fields = new LinkedHashMap<>();

    public NhanVienProfilePanel() {
        super(new BorderLayout(0, 22));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        add(UiTheme.heading("Hồ sơ của tôi", "Thông tin cá nhân theo tài khoản đang đăng nhập."), BorderLayout.NORTH);
        JPanel grid = UiTheme.card(new GridLayout(4, 2, 18, 18));
        for (String label : new String[] { "Mã nhân viên", "Họ tên", "Giới tính", "Ngày sinh", "Điện thoại", "Email",
                "Địa chỉ", "Chức vụ" }) {
            JTextField input = new JTextField(22);
            input.setEditable(false);
            fields.put(label, input);
            grid.add(UiTheme.field(label, input));
        }
        add(grid, BorderLayout.CENTER);
    }

    public void showEmployee(NhanVienDTO e) {
        Object[] values = { e.getMaNV(), e.getHoTen(), e.getGioiTinh() == null ? null : e.getGioiTinh() ? "Nam" : "Nữ",
                e.getNgaySinh(), e.getSdt(), e.getEmail(), e.getDiaChi(), e.getChucVu() };
        int index = 0;
        for (JTextField input : fields.values()) {
            Object value = values[index++];
            input.setText(value == null ? "Chưa khai báo" : value.toString());
            input.setCaretPosition(0);
        }
    }
}
