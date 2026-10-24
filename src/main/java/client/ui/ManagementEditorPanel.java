package client.ui;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import client.api.ManagementApi.Kind;
import common.dto.*;
import common.enums.VaiTro;
import common.protocol.JsonCodec;

/** Form dùng chung, danh sách trường cố định theo module; không cho nhập JSON thô. */
public final class ManagementEditorPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Kind kind;
    private final ObjectNode original;
    private final Map<String, JComponent> fields = new LinkedHashMap<>();
    private final JPasswordField password = new JPasswordField();
    public ManagementEditorPanel(Kind kind, JsonNode value, boolean create) {
        super(new BorderLayout(0, 14)); this.kind = kind;
        if (value == null) {
            Object empty;
            if (kind == Kind.CUSTOMER) { empty = new KhachHangDTO(); }
            else if (kind == Kind.TAX) { ThueDTO d = new ThueDTO(); d.setPhanTram(BigDecimal.ZERO); empty = d; }
            else if (kind == Kind.PROMOTION) {
                KhuyenMaiDTO d = new KhuyenMaiDTO(); d.setPhanTramGiam(BigDecimal.ZERO);
                d.setNgayBatDau(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
                d.setNgayKetThuc(d.getNgayBatDau()); empty = d;
            } else {
                NhanVienAccountDTO d = new NhanVienAccountDTO(); d.setVaiTro(VaiTro.NHAN_VIEN_BAN_HANG); empty = d;
            }
            original = JsonCodec.mapper().valueToTree(empty);
        } else { original = ((ObjectNode) value).deepCopy(); }
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel grid = new JPanel(new GridBagLayout()); grid.setOpaque(false);
        String id = kind == Kind.CUSTOMER ? "maKH" : kind == Kind.EMPLOYEE ? "maNV" : kind == Kind.TAX ? "maThue" : "maKM";
        addText(grid, id, "Mã *", false); fields.get(id).setEnabled(create);
        String nameKey = kind == Kind.PROMOTION ? "tenKM" : kind == Kind.TAX ? "tenThue" : "hoTen";
        addText(grid, nameKey, kind == Kind.PROMOTION ? "Tên khuyến mãi *" : kind == Kind.TAX ? "Tên cấu hình *" : "Họ tên *", false);
        if (kind == Kind.PROMOTION) {
            addText(grid, "phanTramGiam", "Giảm (%) * · dấu thập phân .", false);
            addText(grid, "ngayBatDau", "Ngày bắt đầu * · yyyy-MM-dd", false);
            addText(grid, "ngayKetThuc", "Ngày kết thúc * · yyyy-MM-dd", false);
        } else if (kind == Kind.TAX) {
            addText(grid, "phanTram", "Tỷ lệ (%) * · dấu thập phân .", false);
            addText(grid, "moTa", "Mô tả", false);
        } else {
            addText(grid, "sdt", kind == Kind.EMPLOYEE ? "Điện thoại *" : "Điện thoại", false);
            addText(grid, "email", "Email", false); addText(grid, "diaChi", "Địa chỉ", false);
            if (kind == Kind.EMPLOYEE) {
                JComboBox<String> gender = new JComboBox<>(new String[]{"Chưa xác định", "Nam", "Nữ"});
                gender.setSelectedIndex(original.path("gioiTinh").isNull() || original.path("gioiTinh").isMissingNode()
                        ? 0 : original.path("gioiTinh").asBoolean() ? 1 : 2); addField(grid, "gioiTinh", "Giới tính", gender);
                addText(grid, "ngaySinh", "Ngày sinh · yyyy-MM-dd", false); addText(grid, "chucVu", "Chức vụ", false);
                addText(grid, "username", "Tên đăng nhập · để trống nếu chưa cấp", false);
                boolean hasAccount = !original.path("username").asText("").isEmpty();
                fields.get("username").setEnabled(!hasAccount);
                JComboBox<VaiTro> role = new JComboBox<>(VaiTro.values());
                if (!original.path("vaiTro").isNull() && !original.path("vaiTro").isMissingNode()) {
                    role.setSelectedItem(VaiTro.valueOf(original.path("vaiTro").asText()));
                } else { role.setSelectedItem(VaiTro.NHAN_VIEN_BAN_HANG); }
                role.setRenderer(new DefaultListCellRenderer() {
                    @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                        return super.getListCellRendererComponent(list, value == VaiTro.NHAN_VIEN_QUAN_LY ? "Nhân viên quản lý" : "Nhân viên bán hàng", index, selected, focus);
                    }
                }); addField(grid, "vaiTro", "Vai trò tài khoản", role);
                addFlag(grid, "taiKhoanHoatDong", "Cho phép đăng nhập");
                password.setEnabled(!hasAccount); password.setToolTipText("Chỉ dùng khi cấp tài khoản mới; không hiển thị mật khẩu đã lưu.");
                addField(grid, "_password", "Mật khẩu ban đầu · 8–128 ký tự", password);
            }
        }
        addFlag(grid, "trangThai", kind == Kind.PROMOTION || kind == Kind.TAX ? "Đang áp dụng" : "Đang hoạt động");
        add(grid, BorderLayout.CENTER);
        add(UiTheme.muted("<html>* Bắt buộc. Mã không đổi sau khi tạo.<br>Ngừng hoạt động giữ nguyên lịch sử hóa đơn.</html>"), BorderLayout.SOUTH);
    }
    private void addText(JPanel grid, String key, String label, boolean unused) {
        JsonNode v = original.get(key); JTextField field = new JTextField(v == null || v.isNull() ? "" : v.asText(), 26);
        addField(grid, key, label, field);
    }
    private void addFlag(JPanel grid, String key, String label) {
        JCheckBox field = new JCheckBox(label, original.path(key).asBoolean()); field.setOpaque(false); addField(grid, key, "", field);
    }
    private void addField(JPanel grid, String key, String label, JComponent field) {
        int row = fields.size(); fields.put(key, field);
        GridBagConstraints c = new GridBagConstraints(); c.gridy = row; c.gridx = 0; c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 0, 5, 18); grid.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(5, 0, 5, 0); grid.add(field, c);
    }
    public ObjectNode read() {
        ObjectNode out = original.deepCopy();
        fields.forEach((key, field) -> {
            if (key.equals("_password")) { return; }
            if (field instanceof JCheckBox) { out.put(key, ((JCheckBox) field).isSelected()); }
            else if (key.equals("gioiTinh")) {
                int choice = ((JComboBox<?>) field).getSelectedIndex();
                if (choice == 0) { out.putNull(key); } else { out.put(key, choice == 1); }
            } else if (key.equals("vaiTro")) { out.put(key, ((VaiTro) ((JComboBox<?>) field).getSelectedItem()).name()); }
            else {
                String text = ((JTextField) field).getText().trim();
                if (text.isEmpty()) { out.putNull(key); }
                else if (key.equals("phanTramGiam") || key.equals("phanTram")) { out.put(key, new BigDecimal(text)); }
                else if (key.startsWith("ngay")) { out.put(key, LocalDate.parse(text).toString()); }
                else { out.put(key, text); }
            }
        }); return out;
    }
    public String initialPassword() {
        char[] chars = password.getPassword();
        try { return chars.length == 0 ? null : new String(chars); }
        finally { Arrays.fill(chars, '\0'); password.setText(""); }
    }
}
