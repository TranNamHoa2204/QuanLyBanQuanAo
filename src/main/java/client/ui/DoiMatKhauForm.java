package client.ui;

import java.awt.*;
import java.util.Arrays;
import javax.swing.*;
import client.api.AuthApi;
import client.controller.DoiMatKhauController;

public final class DoiMatKhauForm extends JDialog {
    private static final long serialVersionUID = 1L;

    public DoiMatKhauForm(JFrame owner, AuthApi api, Runnable changed) {
        super(owner, "Đổi mật khẩu", true);
        DoiMatKhauPanel panel = new DoiMatKhauPanel();
        JPasswordField oldPassword = panel.oldPassword, newPassword = panel.newPassword, confirm = panel.confirm;
        JButton save = panel.save;
        DoiMatKhauController controller = new DoiMatKhauController(api);
        save.addActionListener(e -> {
            char[] oldChars = oldPassword.getPassword(), newChars = newPassword.getPassword(),
                    confirmChars = confirm.getPassword();
            try {
                if (!Arrays.equals(newChars, confirmChars)) {
                    JOptionPane.showMessageDialog(this, "Mật khẩu mới và xác nhận không khớp.");
                    return;
                }
                String oldText = new String(oldChars), newText = new String(newChars);
                oldPassword.setText("");
                newPassword.setText("");
                confirm.setText("");
                UiTask.run(this, save, () -> {
                    controller.change(oldText, newText);
                    return null;
                }, ignored -> {
                    dispose();
                    changed.run();
                });
            } finally {
                Arrays.fill(oldChars, '\0');
                Arrays.fill(newChars, '\0');
                Arrays.fill(confirmChars, '\0');
            }
        });
        add(panel);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(save);
    }
}
