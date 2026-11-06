package client.gui.hethong;

import java.util.Arrays;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import client.api.GoiDichVuXacThuc;
import client.controllers.DieuKhienDangNhap;
import client.gui.components.TacVuGiaoDien;
import client.session.PhienDangNhap;

public final class ManHinhDangNhap extends JFrame {
    private static final long serialVersionUID = 1L;

    public ManHinhDangNhap(GoiDichVuXacThuc api, PhienDangNhap session) {
        super("Cửa hàng quần áo — Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        KhungDangNhap panel = new KhungDangNhap();
        JTextField username = panel.username;
        JPasswordField password = panel.password;
        JButton login = panel.login;
        add(panel);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(login);
        panel.exit.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        SwingUtilities.invokeLater(username::requestFocusInWindow);
        DieuKhienDangNhap controller = new DieuKhienDangNhap(api);
        login.addActionListener(e -> {
            String user = username.getText().trim();
            char[] chars = password.getPassword();
            String secret = new String(chars);
            Arrays.fill(chars, '\0');
            password.setText("");
            TacVuGiaoDien.run(this, login, () -> controller.login(user, secret), result -> {
                new ManHinhChinh(api, session).setVisible(true);
                dispose();
            });
        });
    }
}
