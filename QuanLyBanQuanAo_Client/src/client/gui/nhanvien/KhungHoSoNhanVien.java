package client.gui.nhanvien;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;

import client.gui.components.ChuDeGiaoDien;
import common.dto.nhanvien.DuLieuNhanVien;

public final class KhungHoSoNhanVien extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Map<String, JTextField> fields = new LinkedHashMap<>();

    public KhungHoSoNhanVien() {
        super(new BorderLayout(0, 22));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        add(ChuDeGiaoDien.heading("Hồ sơ của tôi", "Thông tin cá nhân theo tài khoản đang đăng nhập."),
                BorderLayout.NORTH);
        JPanel grid = ChuDeGiaoDien.card(new GridLayout(4, 2, 18, 18));
        for (String label : new String[] { "Mã nhân viên", "Họ tên", "Giới tính", "Ngày sinh", "Điện thoại",
                "Email", "Địa chỉ", "Chức vụ" }) {
            JTextField input = new JTextField(22);
            input.setEditable(false);
            fields.put(label, input);
            grid.add(ChuDeGiaoDien.field(label, input));
        }
        add(grid, BorderLayout.CENTER);
    }

    public void showEmployee(DuLieuNhanVien e) {
        Object[] values = { e.getMaNV(), e.getHoTen(),
                e.getGioiTinh() == null ? null : e.getGioiTinh() ? "Nam" : "Nữ", e.getNgaySinh(), e.getSdt(),
                e.getEmail(), e.getDiaChi(), e.getChucVu() };
        int index = 0;
        for (JTextField input : fields.values()) {
            Object value = values[index++];
            input.setText(value == null ? "Chưa khai báo" : value.toString());
            input.setCaretPosition(0);
        }
    }
}
