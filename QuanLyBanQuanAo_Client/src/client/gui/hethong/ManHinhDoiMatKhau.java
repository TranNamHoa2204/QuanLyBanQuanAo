package client.gui.hethong;

import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import client.api.GoiDichVuXacThuc;
import client.controllers.DieuKhienDoiMatKhau;
import client.gui.components.TacVuGiaoDien;

public final class ManHinhDoiMatKhau extends JDialog {
    private static final long serialVersionUID = 1L;

    public ManHinhDoiMatKhau(JFrame owner, GoiDichVuXacThuc api, Runnable changed) {
        super(owner, "Đổi mật khẩu", true);
        KhungDoiMatKhau panel = new KhungDoiMatKhau();
        JPasswordField oldPassword = panel.oldPassword, newPassword = panel.newPassword,
                confirm = panel.confirm;
        JButton save = panel.save;
        DieuKhienDoiMatKhau controller = new DieuKhienDoiMatKhau(api);
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
                TacVuGiaoDien.run(this, save, () -> {
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
