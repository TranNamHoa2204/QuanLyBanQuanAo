package client.gui.hethong;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
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
    private final JPanel home = new JPanel(new BorderLayout(0, 24));
    private final JLabel pageTitle = ChuDeGiaoDien.title("Trang chủ", 19);
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
        JPanel identityBar = new JPanel(new BorderLayout(20, 0));
        identityBar.setBackground(Color.WHITE);
        identityBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ChuDeGiaoDien.LINE),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
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
        JLabel name = ChuDeGiaoDien.title(login.getHoTen(), 14);
        name.setHorizontalAlignment(SwingConstants.RIGHT);
        JLabel role = ChuDeGiaoDien.muted(manager ? "Nhân viên quản lý" : "Nhân viên bán hàng");
        role.setHorizontalAlignment(SwingConstants.RIGHT);
        account.add(name);
        account.add(role);
        JPanel tools = new JPanel(new BorderLayout(16, 0));
        tools.setOpaque(false);
        homeButton.setToolTipText("Về trang chủ (Ctrl+Home)");
        homeButton.addActionListener(e -> showHome());
        tools.add(homeButton, BorderLayout.WEST);
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
        menuBar.setBackground(Color.WHITE);
        menuBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ChuDeGiaoDien.LINE),
                BorderFactory.createEmptyBorder(3, 16, 3, 16)));
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
        menu.setFont(menu.getFont().deriveFont(Font.BOLD));
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
        home.setBorder(BorderFactory.createEmptyBorder(12, 24, 28, 24));
        JPanel greeting = ChuDeGiaoDien.card(new BorderLayout(24, 0));
        greeting.setBackground(ChuDeGiaoDien.SOFT);
        greeting.add(
                ChuDeGiaoDien.heading("Xin chào, " + login.getHoTen(),
                        "Chọn chức năng trên thanh menu hoặc sử dụng các lối tắt bên dưới."),
                BorderLayout.CENTER);
        greeting.add(new JLabel(new BieuTuongCuaHang(66, ChuDeGiaoDien.PRIMARY)), BorderLayout.EAST);
        home.add(greeting, BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(ChuDeGiaoDien.title("Lối tắt công việc", 19), BorderLayout.NORTH);
        JPanel tasks = new JPanel(new GridLayout(0, manager ? 3 : 2, 16, 16));
        tasks.setOpaque(false);
        tasks.add(taskCard("01", "Bán hàng", "Lập hóa đơn, chọn khách hàng<br>và thu tiền tại quầy.",
                "Mở quầy bán hàng", sales));
        tasks.add(taskCard("02", "Tra cứu sản phẩm",
                "Tìm sản phẩm, xem hình ảnh,<br>màu sắc, kích cỡ và tồn kho.", "Tìm sản phẩm", products));
        if (manager) {
            if (management != null) {
                tasks.add(taskCard("03", "Danh mục sản phẩm",
                        "Quản lý sản phẩm, loại hàng,<br>thương hiệu và biến thể.", "Mở danh mục",
                        management));
            }
            if (receipts != null) {
                tasks.add(taskCard("04", "Nhập hàng",
                        "Lập phiếu nhập từ nhà cung cấp<br>và xác nhận hàng vào kho.", "Mở phiếu nhập",
                        receipts));
            }
            if (customers != null) {
                tasks.add(taskCard("05", "Khách hàng",
                        "Tra cứu và cập nhật hồ sơ<br>khách hàng của cửa hàng.", "Mở khách hàng", customers));
            }
            if (statistics != null) {
                tasks.add(taskCard("06", "Báo cáo doanh thu",
                        "Xem kết quả bán hàng theo<br>thời gian hoặc sản phẩm.", "Xem thống kê", statistics));
            }
        }
        body.add(tasks, BorderLayout.CENTER);
        JLabel note = ChuDeGiaoDien.muted(
                "Hóa đơn nháp được giữ khi chuyển màn hình. Tồn kho được cập nhật khi thanh toán hoặc xác nhận nhập hàng.");
        note.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        body.add(note, BorderLayout.SOUTH);
        home.add(body, BorderLayout.CENTER);
    }

    private static JPanel taskCard(String number, String title, String description, String action,
            Runnable callback) {
        JPanel card = ChuDeGiaoDien.card(new BorderLayout(0, 16));
        JPanel heading = new JPanel(new BorderLayout(10, 0));
        heading.setOpaque(false);
        JLabel index = ChuDeGiaoDien.title(number, 14);
        index.setForeground(ChuDeGiaoDien.PRIMARY);
        heading.add(index, BorderLayout.WEST);
        heading.add(ChuDeGiaoDien.title(title, 18), BorderLayout.CENTER);
        card.add(heading, BorderLayout.NORTH);
        card.add(ChuDeGiaoDien.muted("<html>" + description + "</html>"), BorderLayout.CENTER);
        JButton button = new JButton(action);
        ChuDeGiaoDien.primary(button);
        button.setEnabled(callback != null);
        button.addActionListener(e -> {
            if (callback != null) {
                callback.run();
            }
        });
        card.add(button, BorderLayout.SOUTH);
        return card;
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
