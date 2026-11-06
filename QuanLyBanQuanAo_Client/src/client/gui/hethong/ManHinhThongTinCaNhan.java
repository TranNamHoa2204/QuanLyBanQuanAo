package client.gui.hethong;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import client.api.GoiDichVuXacThuc;
import client.controllers.DieuKhienThongTinCaNhan;
import client.gui.components.ChuDeGiaoDien;
import client.gui.components.TacVuGiaoDien;
import client.gui.nhanvien.KhungHoSoNhanVien;

public final class ManHinhThongTinCaNhan extends JDialog {
    private static final long serialVersionUID = 1L;

    public ManHinhThongTinCaNhan(JFrame owner, GoiDichVuXacThuc api) {
        super(owner, "Thông tin cá nhân", true);
        KhungHoSoNhanVien profile = new KhungHoSoNhanVien();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JButton load = new JButton("Làm mới"), close = new JButton("Đóng");
        JLabel status = ChuDeGiaoDien.muted(" ");
        DieuKhienThongTinCaNhan controller = new DieuKhienThongTinCaNhan(api);
        load.addActionListener(e -> {
            status.setText("Đang mở hồ sơ…");
            TacVuGiaoDien.run(load, controller::load, employee -> {
                profile.showEmployee(employee);
                status.setText(" ");
                status.setToolTipText(null);
                load.setText("Làm mới");
            }, message -> {
                status.setText("Chưa mở được hồ sơ.");
                status.setToolTipText(message);
                load.setText("Thử lại");
            });
        });
        close.addActionListener(e -> dispose());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                load.doClick();
            }
        });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.add(load);
        actions.add(close);
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setBorder(BorderFactory.createEmptyBorder(0, 20, 16, 20));
        footer.add(status, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.EAST);
        add(profile, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }
}
