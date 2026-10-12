package client.ui;

import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;
import common.dto.LoginResponse;
import common.enums.VaiTro;

/** Khung điều hướng dùng lại cho view thật và kiểm thử, không tự gọi mạng. */
public final class MainShellPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JPanel content = new JPanel(new BorderLayout()), home = new JPanel(new BorderLayout(0, 24));
    private final JLabel pageTitle = UiTheme.title("Tổng quan", 22);
    private final JButton overview = new JButton("Tổng quan"), productsButton = new JButton("Sản phẩm"),
            salesButton = new JButton("Bán hàng & hóa đơn");
    private final JButton catalogButton = new JButton("Danh mục & sản phẩm");
    private final JButton customersButton = new JButton("Khách hàng"), employeesButton = new JButton("Nhân viên & tài khoản"),
            promotionsButton = new JButton("Khuyến mãi"), reportsButton = new JButton("Thống kê");
    public MainShellPanel(LoginResponse login, Runnable products, Runnable sales,
            Runnable profile, Runnable password, Consumer<JButton> logout) {
        this(login, products, sales, profile, password, logout, null);
    }
    public MainShellPanel(LoginResponse login, Runnable products, Runnable sales,
            Runnable profile, Runnable password, Consumer<JButton> logout, Runnable management) {
        this(login, products, sales, profile, password, logout, management, null, null, null, null);
    }
    public MainShellPanel(LoginResponse login, Runnable products, Runnable sales,
            Runnable profile, Runnable password, Consumer<JButton> logout, Runnable management,
            Runnable customers, Runnable employees, Runnable promotions, Runnable reports) {
        super(new BorderLayout());
        JPanel sidebar = new JPanel(new BorderLayout(0, 18)); sidebar.setBackground(UiTheme.NAV);
        sidebar.setPreferredSize(new Dimension(235, 0)); sidebar.setBorder(BorderFactory.createEmptyBorder(20, 16, 20, 16));
        JPanel brand = new JPanel(new GridLayout(0, 1, 0, 8)); brand.setOpaque(false);
        JLabel logo = new JLabel("THREAD", new BrandIcon(40, Color.WHITE), JLabel.LEFT);
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 22f)); logo.setForeground(Color.WHITE); brand.add(logo);
        JLabel caption = new JLabel("QUẢN LÝ CỬA HÀNG QUẦN ÁO"); caption.setForeground(new Color(0x8DBAA4));
        caption.setFont(caption.getFont().deriveFont(10f)); brand.add(caption); sidebar.add(brand, BorderLayout.NORTH);
        JPanel nav = new JPanel(); nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS)); nav.setOpaque(false);
        for (JButton button : new JButton[]{overview, productsButton, salesButton}) { nav.add(navButton(button)); nav.add(Box.createVerticalStrut(8)); }
        overview.addActionListener(e -> showPage("Tổng quan", home)); productsButton.addActionListener(e -> products.run());
        salesButton.addActionListener(e -> sales.run());
        nav.add(navButton(customersButton)); nav.add(Box.createVerticalStrut(8));
        bind(customersButton, customers);
        nav.add(Box.createVerticalStrut(24));
        if (login.getVaiTro() == VaiTro.NHAN_VIEN_QUAN_LY) {
            JLabel label = new JLabel("QUẢN TRỊ"); label.setForeground(new Color(0x8DBAA4)); label.setAlignmentX(LEFT_ALIGNMENT);
            nav.add(label); nav.add(Box.createVerticalStrut(12));
            nav.add(navButton(catalogButton)); nav.add(Box.createVerticalStrut(6));
            catalogButton.setEnabled(management != null);
            catalogButton.addActionListener(e -> { if (management != null) { management.run(); } });
            JButton[] buttons = {employeesButton, promotionsButton, reportsButton};
            Runnable[] callbacks = {employees, promotions, reports};
            for (int i = 0; i < buttons.length; i++) {
                nav.add(navButton(buttons[i])); nav.add(Box.createVerticalStrut(6)); bind(buttons[i], callbacks[i]);
            }
        }
        JScrollPane navigation = new JScrollPane(nav, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        navigation.setBorder(BorderFactory.createEmptyBorder()); navigation.setOpaque(false);
        navigation.getViewport().setBackground(UiTheme.NAV); navigation.getVerticalScrollBar().setUnitIncrement(20);
        sidebar.add(navigation, BorderLayout.CENTER);
        JPanel account = new JPanel(new GridLayout(0, 1, 0, 6)); account.setOpaque(false);
        JButton me = navButton(new JButton("Thông tin cá nhân")), change = navButton(new JButton("Đổi mật khẩu")),
                signout = navButton(new JButton("Đăng xuất"));
        me.addActionListener(e -> profile.run()); change.addActionListener(e -> password.run()); signout.addActionListener(e -> logout.accept(signout));
        account.add(me); account.add(change); account.add(signout); sidebar.add(account, BorderLayout.SOUTH); add(sidebar, BorderLayout.WEST);
        JPanel work = new JPanel(new BorderLayout()); JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE); header.setBorder(BorderFactory.createEmptyBorder(18, 26, 18, 26)); header.add(pageTitle, BorderLayout.WEST);
        String role = login.getVaiTro() == VaiTro.NHAN_VIEN_QUAN_LY ? "Nhân viên quản lý" : "Nhân viên bán hàng";
        JLabel identity = UiTheme.muted(login.getHoTen() + "  /  " + role); header.add(identity, BorderLayout.EAST);
        work.add(header, BorderLayout.NORTH); work.add(content, BorderLayout.CENTER);
        JLabel footer = UiTheme.muted("  JAVA SWING + FLATLAF     /     CLIENT ↔ SOCKET ↔ SERVER");
        footer.setFont(footer.getFont().deriveFont(11f)); footer.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        work.add(footer, BorderLayout.SOUTH); add(work, BorderLayout.CENTER);
        home.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
        JPanel hero = UiTheme.card(new BorderLayout(30, 0)); hero.setBackground(UiTheme.SOFT);
        hero.add(UiTheme.heading("Một ngày bán hàng hiệu quả", "Chọn công việc bên dưới để bắt đầu."), BorderLayout.CENTER);
        hero.add(new JLabel(new BrandIcon(88, UiTheme.PRIMARY)), BorderLayout.EAST); home.add(hero, BorderLayout.NORTH);
        JPanel tasks = new JPanel(new GridLayout(1, 2, 20, 0)); tasks.setOpaque(false);
        tasks.add(taskCard("01", "Tra cứu sản phẩm", "Tìm sản phẩm, kiểm tra màu / size,<br>tồn kho và hình ảnh.", "Khám phá sản phẩm", products));
        tasks.add(taskCard("02", "Bán hàng & hóa đơn", "Lập nháp, điều chỉnh mặt hàng<br>và thanh toán cho khách.", "Bắt đầu bán hàng", sales));
        JPanel center = new JPanel(new BorderLayout(0, 20)); center.setOpaque(false); center.add(tasks, BorderLayout.NORTH);
        JPanel notes = UiTheme.card(new GridLayout(0, 1, 0, 12));
        notes.add(UiTheme.title("Lưu ý khi làm việc", 17));
        notes.add(UiTheme.muted("Hóa đơn nháp được lưu và có thể mở lại từ danh sách."));
        notes.add(UiTheme.muted("Tồn kho được kiểm tra và trừ khi thanh toán, không giữ chỗ khi lập nháp."));
        notes.add(UiTheme.muted("Dữ liệu thực lấy từ máy chủ; không hiển thị doanh thu hay tồn kho giả."));
        center.add(notes, BorderLayout.CENTER); home.add(center, BorderLayout.CENTER);
        showPage("Tổng quan", home);
    }
    private static void bind(JButton button, Runnable callback) {
        button.setEnabled(callback != null);
        button.addActionListener(e -> { if (callback != null) { callback.run(); } });
    }
    private static JButton navButton(JButton button) {
        button.setHorizontalAlignment(SwingConstants.LEFT); button.setBackground(UiTheme.NAV); button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14)); button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        button.setAlignmentX(LEFT_ALIGNMENT); return button;
    }
    private static JPanel taskCard(String number, String title, String description, String action, Runnable callback) {
        JPanel card = UiTheme.card(new BorderLayout(0, 18));
        JLabel index = UiTheme.title(number, 32); index.setForeground(new Color(0x9AB6A7)); card.add(index, BorderLayout.NORTH);
        JPanel text = new JPanel(new GridLayout(0, 1, 0, 12)); text.setOpaque(false);
        text.add(UiTheme.title(title, 21)); text.add(UiTheme.muted("<html>" + description + "</html>")); card.add(text, BorderLayout.CENTER);
        JButton button = new JButton(action); UiTheme.primary(button); button.addActionListener(e -> callback.run());
        card.add(button, BorderLayout.SOUTH); return card;
    }
    public void showPage(String title, JComponent view) {
        pageTitle.setText(title); content.removeAll();
        if (view instanceof BanHangPanel) {
            JScrollPane scroll = new JScrollPane(view); scroll.setBorder(BorderFactory.createEmptyBorder());
            content.add(scroll, BorderLayout.CENTER);
        } else { content.add(view, BorderLayout.CENTER); }
        content.revalidate(); content.repaint();
        overview.setBackground("Tổng quan".equals(title) ? UiTheme.PRIMARY : UiTheme.NAV);
        productsButton.setBackground("Sản phẩm".equals(title) ? UiTheme.PRIMARY : UiTheme.NAV);
        salesButton.setBackground("Bán hàng & hóa đơn".equals(title) ? UiTheme.PRIMARY : UiTheme.NAV);
        catalogButton.setBackground("Danh mục & sản phẩm".equals(title) ? UiTheme.PRIMARY : UiTheme.NAV);
        customersButton.setBackground("Khách hàng".equals(title) ? UiTheme.PRIMARY : UiTheme.NAV);
        employeesButton.setBackground("Nhân viên & tài khoản".equals(title) ? UiTheme.PRIMARY : UiTheme.NAV);
        promotionsButton.setBackground("Khuyến mãi".equals(title) ? UiTheme.PRIMARY : UiTheme.NAV);
        reportsButton.setBackground("Thống kê".equals(title) ? UiTheme.PRIMARY : UiTheme.NAV);
    }
}
