package client.gui.hethong;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import client.gui.components.ChuDeGiaoDien;

public final class KhungDoiMatKhau extends JPanel {
    private static final long serialVersionUID = 1L;
    final JPasswordField oldPassword = new JPasswordField(22), newPassword = new JPasswordField(22),
            confirm = new JPasswordField(22);
    final JButton save = new JButton("Đổi mật khẩu");

    public KhungDoiMatKhau() {
        super(new BorderLayout(0, 20));
        setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        add(ChuDeGiaoDien.heading("Bảo mật tài khoản", "Đổi xong bạn sẽ cần đăng nhập lại."),
                BorderLayout.NORTH);
        JPanel fields = ChuDeGiaoDien.card(new GridLayout(0, 1, 0, 18));
        fields.add(ChuDeGiaoDien.field("Mật khẩu hiện tại", oldPassword));
        fields.add(ChuDeGiaoDien.field("Mật khẩu mới (8–128 ký tự)", newPassword));
        fields.add(ChuDeGiaoDien.field("Nhập lại mật khẩu mới", confirm));
        for (JPasswordField input : new JPasswordField[] { oldPassword, newPassword, confirm }) {
            input.putClientProperty("JPasswordField.showRevealButton", true);
        }
        add(fields, BorderLayout.CENTER);
        ChuDeGiaoDien.primary(save);
        add(save, BorderLayout.SOUTH);
    }
}
