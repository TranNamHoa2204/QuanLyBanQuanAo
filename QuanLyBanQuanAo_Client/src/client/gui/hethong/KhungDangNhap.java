package client.gui.hethong;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import client.gui.components.BieuTuongCuaHang;
import client.gui.components.ChuDeGiaoDien;

/** View có thể render headless; form bên ngoài nối hành động đăng nhập qua controller. */
public final class KhungDangNhap extends JPanel {
    private static final long serialVersionUID = 1L;
    final JTextField username = new JTextField(22);
    final JPasswordField password = new JPasswordField(22);
    final JButton login = new JButton("Đăng nhập");
    final JButton exit = new JButton("Thoát");

    public KhungDangNhap() {
        super(new BorderLayout(0, 8));
        setPreferredSize(new Dimension(650, 310));
        JLabel heading = ChuDeGiaoDien.sectionBar("HỆ THỐNG QUẢN LÝ CỬA HÀNG QUẦN ÁO");
        heading.setIcon(new BieuTuongCuaHang(30, Color.WHITE));
        heading.setIconTextGap(10);
        add(heading, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(12, 0));
        content.setBorder(BorderFactory.createEmptyBorder(2, 12, 8, 12));
        JPanel brand = ChuDeGiaoDien.group("Thông tin hệ thống", new BorderLayout(0, 7));
        brand.setPreferredSize(new Dimension(205, 0));
        brand.setBackground(ChuDeGiaoDien.SOFT);
        brand.add(ChuDeGiaoDien.title("THREAD", 16), BorderLayout.NORTH);
        brand.add(new JLabel("<html>Quản lý cửa hàng quần áo<br><br>Sản phẩm, bán hàng<br>và quản lý kho.</html>"),
                BorderLayout.CENTER);
        content.add(brand, BorderLayout.WEST);

        JPanel form = ChuDeGiaoDien.group("Đăng nhập hệ thống", new BorderLayout(0, 8));
        JPanel labels = new JPanel(new GridLayout(2, 1, 0, 8));
        JPanel fields = new JPanel(new GridLayout(2, 1, 0, 8));
        labels.setOpaque(false);
        fields.setOpaque(false);
        JLabel userLabel = new JLabel("Tài khoản:");
        userLabel.setLabelFor(username);
        JLabel passwordLabel = new JLabel("Mật khẩu:");
        passwordLabel.setLabelFor(password);
        labels.add(userLabel);
        labels.add(passwordLabel);
        username.putClientProperty("JTextField.placeholderText", "Tên đăng nhập");
        password.putClientProperty("JTextField.placeholderText", "Mật khẩu");
        password.putClientProperty("JPasswordField.showRevealButton", true);
        fields.add(username);
        fields.add(password);
        JPanel credentials = new JPanel(new BorderLayout(10, 0));
        credentials.setOpaque(false);
        credentials.add(labels, BorderLayout.WEST);
        credentials.add(fields, BorderLayout.CENTER);
        form.add(credentials, BorderLayout.NORTH);
        form.add(ChuDeGiaoDien.muted("Quên mật khẩu? Liên hệ nhân viên quản lý."), BorderLayout.CENTER);
        ChuDeGiaoDien.primary(login);
        login.setMnemonic(KeyEvent.VK_N);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(login);
        actions.add(exit);
        form.add(actions, BorderLayout.SOUTH);
        content.add(form, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        JLabel status = ChuDeGiaoDien.muted("Nhập tài khoản được cấp để bắt đầu phiên làm việc.");
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ChuDeGiaoDien.LINE),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        add(status, BorderLayout.SOUTH);
    }
}
