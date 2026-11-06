package client.gui.hethong;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import client.gui.components.BieuTuongCuaHang;
import client.gui.components.ChuDeGiaoDien;
import client.gui.sanpham.KhungQuanLySanPham;
import common.dto.taikhoan.KetQuaDangNhap;
import common.enums.VaiTro;

/** Điều hướng theo nhóm chức năng; chỉ gọi callback, không tự thực hiện I/O. */
public final class KhungUngDung extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JPanel content = new JPanel(new BorderLayout());
    private final JPanel home = new JPanel(new BorderLayout(0, 8));
    private final JLabel pageTitle = ChuDeGiaoDien.title("Trang chủ", 16);
    private final JLabel date = ChuDeGiaoDien.muted("");
    private final JMenuBar menuBar = new JMenuBar();
    private final JButton homeButton = new JButton("Trang chủ");

    public KhungUngDung(KetQuaDangNhap login, Runnable products, Runnable sales, Runnable profile,
            Runnable password, Consumer<AbstractButton> logout) {
        this(login, products, sales, profile, password, logout, null);
    }

    public KhungUngDung(KetQuaDangNhap login, Runnable products, Runnable sales, Runnable profile,
            Runnable password, Consumer<AbstractButton> logout, Runnable management) {
        this(login, products, sales, profile, password, logout, management, null);
    }

    public KhungUngDung(KetQuaDangNhap login, Runnable products, Runnable sales, Runnable profile,
            Runnable password, Consumer<AbstractButton> logout, Runnable management, Runnable stockHistory) {
        this(login, products, sales, profile, password, logout, management, stockHistory, null, null, null);
    }

    public KhungUngDung(KetQuaDangNhap login, Runnable products, Runnable sales, Runnable profile,
            Runnable password, Consumer<AbstractButton> logout, Runnable management, Runnable stockHistory,
            Runnable employees, Runnable promotions, Runnable statistics) {
        this(login, products, sales, profile, password, logout, management, stockHistory, employees,
                promotions, statistics, null);
    }

    public KhungUngDung(KetQuaDangNhap login, Runnable products, Runnable sales, Runnable profile,
            Runnable password, Consumer<AbstractButton> logout, Runnable management, Runnable stockHistory,
            Runnable employees, Runnable promotions, Runnable statistics, Runnable customers) {
        this(login, products, sales, profile, password, logout, management, stockHistory, employees,
                promotions, statistics, customers, null, null);
    }

    public KhungUngDung(KetQuaDangNhap login, Runnable products, Runnable sales, Runnable profile,
            Runnable password, Consumer<AbstractButton> logout, Runnable management, Runnable stockHistory,
            Runnable employees, Runnable promotions, Runnable statistics, Runnable customers,
            Runnable suppliers, Runnable receipts) {
        this(login, products, sales, profile, password, logout, management, stockHistory, employees,
                promotions, statistics, customers, suppliers, receipts, null, null);
    }

    public KhungUngDung(KetQuaDangNhap login, Runnable products, Runnable sales, Runnable profile,
            Runnable password, Consumer<AbstractButton> logout, Runnable management, Runnable stockHistory,
            Runnable employees, Runnable promotions, Runnable statistics, Runnable customers,
            Runnable suppliers, Runnable receipts, Consumer<KhungQuanLySanPham.Section> catalogSection,
            Runnable invoices) {
        super(new BorderLayout());
        boolean manager = login.getVaiTro() == VaiTro.NHAN_VIEN_QUAN_LY;
        buildMenus(manager, products, sales, profile, password, logout, management, stockHistory, employees,
                promotions, statistics, customers, suppliers, receipts, catalogSection, invoices);

        JPanel top = new JPanel(new BorderLayout());
        top.add(menuBar, BorderLayout.NORTH);
        JPanel identityBar = new JPanel(new BorderLayout(20, 0)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setPaint(new GradientPaint(0, 0, new Color(0xF8FAFF), 0, getHeight(), ChuDeGiaoDien.SOFT));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } finally {
                    g.dispose();
                }
            }
        };
        pageTitle.setForeground(ChuDeGiaoDien.NAV);
        identityBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ChuDeGiaoDien.LINE),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        JPanel identity = new JPanel(new BorderLayout(12, 0));
        identity.setOpaque(false);
        identity.add(new JLabel(new BieuTuongCuaHang(36, ChuDeGiaoDien.PRIMARY)), BorderLayout.WEST);
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 2));
        heading.setOpaque(false);
        heading.add(pageTitle);
        heading.add(ChuDeGiaoDien.muted("THREAD  ·  Quản lý cửa hàng quần áo"));
        identity.add(heading, BorderLayout.CENTER);
        identityBar.add(identity, BorderLayout.CENTER);
        JPanel account = new JPanel(new GridLayout(0, 1, 0, 3));
        account.setOpaque(false);
        JLabel name = ChuDeGiaoDien.title(login.getHoTen(), 12);
        name.setHorizontalAlignment(SwingConstants.RIGHT);
        JLabel role = ChuDeGiaoDien.muted(manager ? "Nhân viên quản lý" : "Nhân viên bán hàng");
        role.setHorizontalAlignment(SwingConstants.RIGHT);
        account.add(name);
        account.add(role);
        JPanel tools = new JPanel(new BorderLayout(16, 0));
        tools.setOpaque(false);
        homeButton.setToolTipText("Về trang chủ (Ctrl+Home)");
        homeButton.addActionListener(e -> showHome());
        JPanel homeCommands = new JPanel(new GridBagLayout());
        homeCommands.setOpaque(false);
        homeCommands.add(homeButton);
        tools.add(homeCommands, BorderLayout.WEST);
        tools.add(date, BorderLayout.CENTER);
        tools.add(account, BorderLayout.EAST);
        identityBar.add(tools, BorderLayout.EAST);
        top.add(identityBar, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel work = new JPanel(new BorderLayout());
        work.add(content, BorderLayout.CENTER);
        JLabel footer = ChuDeGiaoDien.muted("  THREAD  •  Quản lý cửa hàng quần áo");
        footer.setFont(footer.getFont().deriveFont(11f));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ChuDeGiaoDien.LINE),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        work.add(footer, BorderLayout.SOUTH);
        add(work, BorderLayout.CENTER);
        buildHome(login, manager, products, sales, management, customers, receipts, statistics);
        showHome();
    }

    private void buildMenus(boolean manager, Runnable products, Runnable sales, Runnable profile,
            Runnable password, Consumer<AbstractButton> logout, Runnable management, Runnable stockHistory,
            Runnable employees, Runnable promotions, Runnable statistics, Runnable customers,
            Runnable suppliers, Runnable receipts, Consumer<KhungQuanLySanPham.Section> catalogSection,
            Runnable invoices) {
        menuBar.setBackground(ChuDeGiaoDien.BACKGROUND);
        menuBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ChuDeGiaoDien.LINE),
                BorderFactory.createEmptyBorder(0, 6, 0, 6)));
        JMenu system = menu("Hệ thống", KeyEvent.VK_H);
        item(system, "Trang chủ", this::showHome,
                KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_DOWN_MASK));
        system.addSeparator();
        item(system, "Thông tin cá nhân", profile, null);
        item(system, "Đổi mật khẩu", password, null);
        system.addSeparator();
        JMenuItem signout = new JMenuItem("Đăng xuất");
        signout.addActionListener(e -> logout.accept(signout));
        system.add(signout);

        if (manager) {
            JMenu catalog = menu("Danh mục", KeyEvent.VK_D);
            if (management != null) {
                JMenu product = new JMenu("Sản phẩm");
                if (catalogSection == null) {
                    item(product, "Danh mục & sản phẩm", management, null);
                } else {
                    for (KhungQuanLySanPham.Section section : KhungQuanLySanPham.Section.values()) {
                        item(product, section.getTitle(), () -> catalogSection.accept(section), null);
                    }
                }
                catalog.add(product);
            }
            JMenu update = menu("Cập nhật", KeyEvent.VK_C);
            item(update, "Nhân viên & tài khoản", employees, null);
            item(update, "Khách hàng", customers, null);
            item(update, "Khuyến mãi", promotions, null);
            item(update, "Nhà cung cấp", suppliers, null);
        }
        JMenu operations = menu("Xử lý", KeyEvent.VK_X);
        item(operations, "Bán hàng & thanh toán", sales, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        if (manager) {
            item(operations, "Phiếu nhập kho", receipts, KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        }
        JMenu lookup = menu("Tìm kiếm", KeyEvent.VK_T);
        item(lookup, "Tra cứu sản phẩm", products, KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        item(lookup, "Tra cứu hóa đơn", invoices, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
        if (manager) {
            item(lookup, "Lịch sử kho", stockHistory, null);
        }
        if (manager) {
            JMenu reports = menu("Báo biểu", KeyEvent.VK_B);
            item(reports, "Thống kê doanh thu", statistics, null);
        }
        for (int i = menuBar.getMenuCount() - 1; i >= 0; i--) {
            if (menuBar.getMenu(i).getItemCount() == 0) {
                menuBar.remove(i);
            }
        }
    }

    private JMenu menu(String title, int mnemonic) {
        JMenu menu = new JMenu(title);
        menu.setMnemonic(mnemonic);
        if (menuBar.getMenuCount() == 0) {
            menu.setFont(menu.getFont().deriveFont(Font.BOLD));
        }
        menuBar.add(menu);
        return menu;
    }

    private static void item(JMenu menu, String title, Runnable callback, KeyStroke shortcut) {
        if (callback == null) {
            return;
        }
        JMenuItem item = new JMenuItem(title);
        item.setAccelerator(shortcut);
        item.addActionListener(e -> callback.run());
        menu.add(item);
    }

    private void buildHome(KetQuaDangNhap login, boolean manager, Runnable products, Runnable sales,
            Runnable management, Runnable customers, Runnable receipts, Runnable statistics) {
        home.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));
        JPanel workspace = new JPanel(new BorderLayout(0, 8));
        workspace.setOpaque(false);
        workspace.add(ChuDeGiaoDien.heading("Xin chào, " + login.getHoTen(),
                "Chọn nghiệp vụ bên dưới hoặc mở chức năng từ thanh menu."), BorderLayout.NORTH);

        JPanel shortcuts = new JPanel(new BorderLayout());
        shortcuts.setBorder(BorderFactory.createLineBorder(ChuDeGiaoDien.LINE));
        shortcuts.setBackground(ChuDeGiaoDien.SURFACE);
        shortcuts.add(ChuDeGiaoDien.sectionBar("Nghiệp vụ thường dùng"), BorderLayout.NORTH);
        JPanel tasks = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 8));
        tasks.setOpaque(false);
        tasks.add(task("Mở quầy bán hàng", "Lập hóa đơn và thu tiền tại quầy.", sales));
        tasks.add(task("Tìm sản phẩm", "Xem sản phẩm, màu sắc, kích cỡ và tồn kho.", products));
        if (manager) {
            if (management != null) {
                tasks.add(task("Mở danh mục", "Quản lý sản phẩm, loại hàng, thương hiệu và biến thể.", management));
            }
            if (receipts != null) {
                tasks.add(task("Mở phiếu nhập", "Lập phiếu nhập và xác nhận hàng vào kho.", receipts));
            }
            if (customers != null) {
                tasks.add(task("Mở khách hàng", "Tra cứu và cập nhật hồ sơ khách hàng.", customers));
            }
            if (statistics != null) {
                tasks.add(task("Xem thống kê", "Xem doanh thu theo thời gian hoặc sản phẩm.", statistics));
            }
        }
        shortcuts.add(tasks, BorderLayout.CENTER);
        workspace.add(shortcuts, BorderLayout.CENTER);

        JPanel notes = ChuDeGiaoDien.group("Thông tin làm việc", new GridLayout(0, 1, 0, 5));
        notes.add(new JLabel("Hóa đơn nháp được giữ khi chuyển màn hình."));
        notes.add(new JLabel("Tồn kho được cập nhật khi thanh toán hoặc xác nhận nhập hàng."));
        workspace.add(notes, BorderLayout.SOUTH);
        home.add(workspace, BorderLayout.NORTH);
    }

    private static JButton task(String action, String description, Runnable callback) {
        JButton button = new JButton(action);
        button.setToolTipText(description);
        button.setEnabled(callback != null);
        button.addActionListener(e -> {
            if (callback != null) {
                callback.run();
            }
        });
        return button;
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    public void showHome() {
        showPage("Trang chủ", home);
    }

    public void showPage(String title, JComponent view) {
        pageTitle.setText(title);
        homeButton.setEnabled(view != home);
        content.removeAll();
        date.setText(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("vi", "VN"))));
        if (view instanceof Scrollable || view == home) {
            JScrollPane scroll = new JScrollPane(view);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getVerticalScrollBar().setUnitIncrement(24);
            content.add(scroll, BorderLayout.CENTER);
        } else {
            content.add(view, BorderLayout.CENTER);
        }
        content.revalidate();
        content.repaint();
    }
}
