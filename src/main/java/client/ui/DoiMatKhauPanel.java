package client.ui;

import java.awt.*;
import javax.swing.*;

public final class DoiMatKhauPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final JPasswordField oldPassword = new JPasswordField(22), newPassword = new JPasswordField(22),
            confirm = new JPasswordField(22);
    final JButton save = new JButton("Đổi mật khẩu");

    public DoiMatKhauPanel() {
        super(new BorderLayout(0, 20));
        setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        add(UiTheme.heading("Bảo mật tài khoản", "Đổi xong bạn sẽ cần đăng nhập lại."), BorderLayout.NORTH);
        JPanel fields = UiTheme.card(new GridLayout(0, 1, 0, 18));
        fields.add(UiTheme.field("Mật khẩu hiện tại", oldPassword));
        fields.add(UiTheme.field("Mật khẩu mới (8–128 ký tự)", newPassword));
        fields.add(UiTheme.field("Nhập lại mật khẩu mới", confirm));
        for (JPasswordField input : new JPasswordField[] { oldPassword, newPassword, confirm }) {
            input.putClientProperty("JPasswordField.showRevealButton", true);
        }
        add(fields, BorderLayout.CENTER);
        UiTheme.primary(save);
        add(save, BorderLayout.SOUTH);
    }
}
