package client.gui.hethong;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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

    public KhungDangNhap() {
        super(new GridLayout(1, 2, 14, 0));
        setPreferredSize(new Dimension(920, 560));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel brand = ChuDeGiaoDien.card(new BorderLayout(), 32);
        brand.setBackground(ChuDeGiaoDien.SOFT);
        JLabel wordmark = new JLabel("THREAD  ·  QUẢN LÝ CỬA HÀNG",
                new BieuTuongCuaHang(38, ChuDeGiaoDien.PRIMARY),
                JLabel.LEFT);
        wordmark.setForeground(ChuDeGiaoDien.PRIMARY);
        wordmark.setFont(wordmark.getFont().deriveFont(Font.BOLD, 13f));
        brand.add(wordmark, BorderLayout.NORTH);
        JPanel message = new JPanel(new GridLayout(0, 1, 0, 16));
        message.setOpaque(false);
        JLabel art = new JLabel(new BieuTuongCuaHang(100, ChuDeGiaoDien.PRIMARY));
        message.add(art);
        JLabel headline = new JLabel("<html>Bán hàng gọn gàng.<br>Quản lý dễ dàng.</html>");
        headline.setFont(headline.getFont().deriveFont(Font.BOLD, 30f));
        message.add(headline);
        JLabel sub = ChuDeGiaoDien.muted(
                "<html>Sản phẩm, màu sắc và kích cỡ.<br>Một không gian làm việc cho cửa hàng.</html>");
        message.add(sub);
        brand.add(message, BorderLayout.CENTER);
        brand.add(ChuDeGiaoDien.muted("SẢN PHẨM  ·  BÁN HÀNG  ·  BÁO BIỂU"), BorderLayout.SOUTH);
        JPanel right = ChuDeGiaoDien.card(new GridBagLayout(), 28);
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        JLabel welcome = ChuDeGiaoDien.title("Chào mừng trở lại", 27);
        welcome.setAlignmentX(LEFT_ALIGNMENT);
        form.add(welcome);
        form.add(Box.createVerticalStrut(10));
        JLabel hint = ChuDeGiaoDien.muted("Đăng nhập bằng tài khoản được cấp.");
        hint.setAlignmentX(LEFT_ALIGNMENT);
        form.add(hint);
        form.add(Box.createVerticalStrut(32));
        username.putClientProperty("JTextField.placeholderText", "Tên đăng nhập");
        password.putClientProperty("JTextField.placeholderText", "Mật khẩu");
        password.putClientProperty("JPasswordField.showRevealButton", true);
        JPanel user = ChuDeGiaoDien.field("TÊN ĐĂNG NHẬP", username),
                pass = ChuDeGiaoDien.field("MẬT KHẨU", password);
        user.setAlignmentX(LEFT_ALIGNMENT);
        pass.setAlignmentX(LEFT_ALIGNMENT);
        form.add(user);
        form.add(Box.createVerticalStrut(18));
        form.add(pass);
        form.add(Box.createVerticalStrut(28));
        ChuDeGiaoDien.primary(login);
        login.setAlignmentX(LEFT_ALIGNMENT);
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        form.add(login);
        form.add(Box.createVerticalStrut(24));
        JLabel help = ChuDeGiaoDien.muted(
                "<html>Quên mật khẩu? Liên hệ nhân viên quản lý.<br>Không có chức năng tự đăng ký tài khoản.</html>");
        help.setFont(help.getFont().deriveFont(12f));
        help.setAlignmentX(LEFT_ALIGNMENT);
        form.add(help);
        right.add(form);
        add(brand);
        add(right);
    }
}
