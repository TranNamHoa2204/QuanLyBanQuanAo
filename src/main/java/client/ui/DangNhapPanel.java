package client.ui;

import java.awt.*;
import javax.swing.*;

/** View có thể render headless; form bên ngoài nối hành động đăng nhập qua controller. */
public final class DangNhapPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final JTextField username = new JTextField(22);
    final JPasswordField password = new JPasswordField(22);
    final JButton login = new JButton("Đăng nhập");

    public DangNhapPanel() {
        super(new GridLayout(1, 2));
        setPreferredSize(new Dimension(920, 560));
        JPanel brand = new JPanel(new BorderLayout());
        brand.setBackground(UiTheme.NAV);
        brand.setBorder(BorderFactory.createEmptyBorder(38, 38, 38, 38));
        JLabel wordmark = new JLabel("THREAD / QUẢN LÝ CỬA HÀNG", new BrandIcon(38, Color.WHITE), JLabel.LEFT);
        wordmark.setForeground(Color.WHITE);
        wordmark.setFont(wordmark.getFont().deriveFont(Font.BOLD, 13f));
        brand.add(wordmark, BorderLayout.NORTH);
        JPanel message = new JPanel(new GridLayout(0, 1, 0, 16));
        message.setOpaque(false);
        JLabel art = new JLabel(new BrandIcon(100, new Color(0x8DBAA4)));
        message.add(art);
        JLabel headline = new JLabel("<html>Bán hàng gọn gàng.<br>Quản lý dễ dàng.</html>");
        headline.setForeground(Color.WHITE);
        headline.setFont(headline.getFont().deriveFont(Font.BOLD, 30f));
        message.add(headline);
        JLabel sub = new JLabel("<html>Sản phẩm, màu sắc và kích cỡ.<br>Một không gian làm việc cho cửa hàng.</html>");
        sub.setForeground(new Color(0xB6CFC2));
        message.add(sub);
        brand.add(message, BorderLayout.CENTER);
        JLabel technology = new JLabel("ĐỒ ÁN JAVA  /  JPA + SOCKET");
        technology.setForeground(new Color(0x8DBAA4));
        technology.setFont(technology.getFont().deriveFont(11f));
        brand.add(technology, BorderLayout.SOUTH);
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Color.WHITE);
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        JLabel welcome = UiTheme.title("Chào mừng trở lại", 27);
        welcome.setAlignmentX(LEFT_ALIGNMENT);
        form.add(welcome);
        form.add(Box.createVerticalStrut(10));
        JLabel hint = UiTheme.muted("Đăng nhập bằng tài khoản được cấp.");
        hint.setAlignmentX(LEFT_ALIGNMENT);
        form.add(hint);
        form.add(Box.createVerticalStrut(32));
        username.putClientProperty("JTextField.placeholderText", "Tên đăng nhập");
        password.putClientProperty("JTextField.placeholderText", "Mật khẩu");
        password.putClientProperty("JPasswordField.showRevealButton", true);
        JPanel user = UiTheme.field("TÊN ĐĂNG NHẬP", username), pass = UiTheme.field("MẬT KHẨU", password);
        user.setAlignmentX(LEFT_ALIGNMENT);
        pass.setAlignmentX(LEFT_ALIGNMENT);
        form.add(user);
        form.add(Box.createVerticalStrut(18));
        form.add(pass);
        form.add(Box.createVerticalStrut(28));
        UiTheme.primary(login);
        login.setAlignmentX(LEFT_ALIGNMENT);
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        form.add(login);
        form.add(Box.createVerticalStrut(24));
        JLabel help = UiTheme.muted(
                "<html>Quên mật khẩu? Liên hệ nhân viên quản lý.<br>Không có chức năng tự đăng ký tài khoản.</html>");
        help.setFont(help.getFont().deriveFont(12f));
        help.setAlignmentX(LEFT_ALIGNMENT);
        form.add(help);
        right.add(form);
        add(brand);
        add(right);
    }
}
