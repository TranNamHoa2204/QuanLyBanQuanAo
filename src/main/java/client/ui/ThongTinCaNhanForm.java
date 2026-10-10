package client.ui;

import java.awt.*;
import javax.swing.*;
import client.api.AuthApi;
import client.controller.ThongTinCaNhanController;

public final class ThongTinCaNhanForm extends JDialog {
    private static final long serialVersionUID = 1L;
    public ThongTinCaNhanForm(JFrame owner, AuthApi api) {
        super(owner, "Thông tin cá nhân", true);
        NhanVienProfilePanel profile = new NhanVienProfilePanel();
        JButton load = new JButton("Tải thông tin");
        ThongTinCaNhanController controller = new ThongTinCaNhanController(api);
        load.addActionListener(e -> UiTask.run(this, load, controller::load, profile::showEmployee));
        UiTheme.primary(load);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.setBorder(BorderFactory.createEmptyBorder(0, 20, 16, 20));
        actions.add(load); add(profile, BorderLayout.CENTER); add(actions, BorderLayout.SOUTH);
        pack(); setLocationRelativeTo(owner);
    }
    private static String value(Object value) { return value == null ? "" : value.toString(); }
}
