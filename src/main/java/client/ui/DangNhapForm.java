package client.ui;

import java.awt.*;
import java.util.Arrays;
import javax.swing.*;
import client.api.AuthApi;
import client.controller.DangNhapController;
import client.session.ClientSession;

public final class DangNhapForm extends JFrame {
    private static final long serialVersionUID = 1L;
    public DangNhapForm(AuthApi api, ClientSession session) {
        super("Cửa hàng quần áo — Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        DangNhapPanel panel = new DangNhapPanel();
        JTextField username = panel.username;
        JPasswordField password = panel.password;
        JButton login = panel.login;
        add(panel); pack(); setResizable(false); setLocationRelativeTo(null);
        getRootPane().setDefaultButton(login);
        SwingUtilities.invokeLater(username::requestFocusInWindow);
        DangNhapController controller = new DangNhapController(api);
        login.addActionListener(e -> {
            String user = username.getText().trim();
            char[] chars = password.getPassword();
            String secret = new String(chars);
            Arrays.fill(chars, '\0'); password.setText("");
            UiTask.run(this, login, () -> controller.login(user, secret), result -> {
                new MainForm(api, session).setVisible(true); dispose();
            });
        });
    }
}
