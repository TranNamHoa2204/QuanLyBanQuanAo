package kiemthu;

import static org.junit.Assert.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import org.junit.Test;

import client.api.GoiDichVuHoaDon;
import client.api.GoiDichVuKhachHang;
import client.api.GoiDichVuKhuyenMai;
import client.api.GoiDichVuNhaCungCap;
import client.api.GoiDichVuNhanVien;
import client.api.GoiDichVuPhieuNhap;
import client.api.GoiDichVuSanPham;
import client.api.GoiDichVuThongKe;
import client.controllers.DieuKhienKhachHang;
import client.controllers.DieuKhienKhuyenMai;
import client.controllers.DieuKhienNhaCungCap;
import client.controllers.DieuKhienNhanVien;
import client.controllers.DieuKhienPhieuNhap;
import client.controllers.DieuKhienThongKe;
import client.gui.banhang.KhungBanHang;
import client.gui.components.ChuDeGiaoDien;
import client.gui.hethong.KhungDangNhap;
import client.gui.hethong.KhungDoiMatKhau;
import client.gui.hethong.KhungUngDung;
import client.gui.khachhang.ManHinhKhachHang;
import client.gui.khuyenmai.ManHinhKhuyenMai;
import client.gui.nhanvien.KhungHoSoNhanVien;
import client.gui.nhanvien.ManHinhNhanVien;
import client.gui.nhapkho.ManHinhNhaCungCap;
import client.gui.nhapkho.ManHinhPhieuNhap;
import client.gui.sanpham.KhungQuanLySanPham;
import client.gui.thongke.ManHinhThongKe;
import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.nhanvien.DuLieuNhanVien;
import common.dto.taikhoan.KetQuaDangNhap;
import common.enums.VaiTro;

/** View thực, không mock ảnh: render headless bằng FlatLaf và chạy hành động điều hướng. */
public class KiemThuGiaoDienChung {
    @Test
    public void loginAndDashboardRenderAndNavigate() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                ChuDeGiaoDien.install();
                assertEquals("FlatLaf Light", UIManager.getLookAndFeel().getName());
                File dir = new File(".tmp/flatlaf-ui");
                assertTrue(dir.isDirectory() || dir.mkdirs());
                render(new KhungDangNhap(), new File(dir, "login.png"), 920, 560);
                KetQuaDangNhap login = new KetQuaDangNhap();
                login.setHoTen("Nguyễn Minh Anh");
                login.setVaiTro(VaiTro.NHAN_VIEN_QUAN_LY);
                AtomicInteger products = new AtomicInteger(), sales = new AtomicInteger(),
                        management = new AtomicInteger(), history = new AtomicInteger();
                AtomicInteger employees = new AtomicInteger(), promotions = new AtomicInteger(),
                        statistics = new AtomicInteger(), customers = new AtomicInteger();
                AtomicInteger suppliers = new AtomicInteger(), receipts = new AtomicInteger();
                AtomicInteger invoices = new AtomicInteger(), profileOpens = new AtomicInteger(),
                        passwordOpens = new AtomicInteger(), logouts = new AtomicInteger();
                Map<KhungQuanLySanPham.Section, AtomicInteger> sections = new EnumMap<>(
                        KhungQuanLySanPham.Section.class);
                for (KhungQuanLySanPham.Section section : KhungQuanLySanPham.Section.values()) {
                    sections.put(section, new AtomicInteger());
                }
                KhungUngDung shell = new KhungUngDung(login, products::incrementAndGet,
                        sales::incrementAndGet, profileOpens::incrementAndGet, passwordOpens::incrementAndGet,
                        b -> logouts.incrementAndGet(), management::incrementAndGet, history::incrementAndGet,
                        employees::incrementAndGet, promotions::incrementAndGet, statistics::incrementAndGet,
                        customers::incrementAndGet, suppliers::incrementAndGet, receipts::incrementAndGet,
                        section -> sections.get(section).incrementAndGet(), invoices::incrementAndGet);
                render(shell, new File(dir, "dashboard.png"), 1320, 860);
                renderMenu(shell, new File(dir, "functional-menu.png"));
                render(shell, new File(dir, "dashboard-small-screen.png"), 1100, 700);
                String[] groups = { "Hệ thống", "Danh mục", "Cập nhật", "Xử lý", "Tìm kiếm", "Báo biểu" };
                assertEquals(groups.length, shell.getMenuBar().getMenuCount());
                for (int i = 0; i < groups.length; i++) {
                    assertEquals(groups[i], shell.getMenuBar().getMenu(i).getText());
                }
                findMenu(shell, "Tra cứu sản phẩm").doClick();
                assertEquals(1, products.get());
                findMenu(shell, "Bán hàng & thanh toán").doClick();
                assertEquals(1, sales.get());
                for (KhungQuanLySanPham.Section section : sections.keySet()) {
                    findMenu(shell, section.getTitle()).doClick();
                    assertEquals(1, sections.get(section).get());
                }
                findMenu(shell, "Tra cứu hóa đơn").doClick();
                assertEquals(1, invoices.get());
                findMenu(shell, "Lịch sử kho").doClick();
                assertEquals(1, history.get());
                findMenu(shell, "Nhân viên & tài khoản").doClick();
                assertEquals(1, employees.get());
                findMenu(shell, "Khách hàng").doClick();
                assertEquals(1, customers.get());
                findMenu(shell, "Khuyến mãi").doClick();
                assertEquals(1, promotions.get());
                findMenu(shell, "Thống kê doanh thu").doClick();
                assertEquals(1, statistics.get());
                findMenu(shell, "Nhà cung cấp").doClick();
                assertEquals(1, suppliers.get());
                findMenu(shell, "Phiếu nhập kho").doClick();
                assertEquals(1, receipts.get());
                findMenu(shell, "Thông tin cá nhân").doClick();
                assertEquals(1, profileOpens.get());
                findMenu(shell, "Đổi mật khẩu").doClick();
                assertEquals(1, passwordOpens.get());
                findMenu(shell, "Đăng xuất").doClick();
                assertEquals(1, logouts.get());
                assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0),
                        findMenu(shell, "Bán hàng & thanh toán").getAccelerator());
                assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0),
                        findMenu(shell, "Tra cứu sản phẩm").getAccelerator());
                assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0),
                        findMenu(shell, "Tra cứu hóa đơn").getAccelerator());
                findButton(shell, "Mở danh mục").doClick();
                assertEquals(1, management.get());
                login.setVaiTro(VaiTro.NHAN_VIEN_BAN_HANG);
                KhungUngDung staff = new KhungUngDung(login, () -> {
                }, () -> {
                }, () -> {
                }, () -> {
                }, b -> {
                }, () -> {
                }, () -> {
                }, () -> {
                }, () -> {
                }, () -> {
                }, () -> {
                }, () -> {
                }, () -> {
                }, section -> {
                }, () -> {
                });
                assertEquals(3, staff.getMenuBar().getMenuCount());
                for (String label : new String[] { "Danh mục", "Cập nhật", "Báo biểu",
                        "Nhân viên & tài khoản", "Khách hàng", "Phiếu nhập kho", "Lịch sử kho" }) {
                    assertNull(findMenu(staff, label));
                }
                assertNotNull(findMenu(staff, "Tra cứu hóa đơn"));
                render(staff, new File(dir, "staff-home.png"), 1280, 720);
                JPanel view = new JPanel();
                shell.showPage("Sản phẩm", view);
                assertTrue(SwingUtilities.isDescendingFrom(view, shell));
                findMenu(shell, "Trang chủ").doClick();
                assertFalse(SwingUtilities.isDescendingFrom(view, shell));
                KhungHoSoNhanVien profile = new KhungHoSoNhanVien();
                DuLieuNhanVien e = new DuLieuNhanVien();
                e.setMaNV("NV01");
                e.setHoTen(login.getHoTen());
                profile.showEmployee(e);
                render(profile, new File(dir, "profile.png"), 700, 490);
                render(new KhungDoiMatKhau(), new File(dir, "password.png"), 540, 550);
                KetNoiMayChu socket = new KetNoiMayChu("127.0.0.1", 1);
                PhienDangNhap session = new PhienDangNhap();
                render(new ManHinhNhanVien(new DieuKhienNhanVien(new GoiDichVuNhanVien(socket, session))),
                        new File(dir, "employees.png"), 1280, 720);
                render(new ManHinhKhachHang(new DieuKhienKhachHang(new GoiDichVuKhachHang(socket, session))),
                        new File(dir, "customers.png"), 1280, 720);
                render(new ManHinhKhuyenMai(new DieuKhienKhuyenMai(new GoiDichVuKhuyenMai(socket, session))),
                        new File(dir, "promotions.png"), 1280, 720);
                render(new ManHinhThongKe(new DieuKhienThongKe(new GoiDichVuThongKe(socket, session))),
                        new File(dir, "statistics.png"), 1280, 720);
                ManHinhNhaCungCap supplierView = new ManHinhNhaCungCap(
                        new DieuKhienNhaCungCap(new GoiDichVuNhaCungCap(socket, session)));
                JScrollPane supplierScroll = (JScrollPane) ((BorderLayout) supplierView.getLayout())
                        .getLayoutComponent(BorderLayout.CENTER);
                JTable supplierTable = (JTable) supplierScroll.getViewport().getView();
                supplierView.setSize(900, 600);
                layout(supplierView);
                assertTrue(supplierScroll.getHorizontalScrollBar().isVisible());
                assertEquals(supplierTable.getPreferredSize().width,
                        supplierTable.getColumnModel().getTotalColumnWidth());
                render(supplierView, new File(dir, "suppliers.png"), 1280, 720);
                assertFalse(supplierScroll.getHorizontalScrollBar().isVisible());
                assertEquals(supplierScroll.getViewport().getWidth(),
                        supplierTable.getColumnModel().getTotalColumnWidth());
                String longAddress = "15 đường Nguyễn Thị Minh Khai, khu nhà xưởng phía Đông, "
                        + "phường Bến Thành, Thành phố Hồ Chí Minh";
                ((DefaultTableModel) supplierTable.getModel()).addRow(new Object[] {
                        "NCC01", "Nhà cung cấp thử", "0900000001", "ncc@example.com", longAddress, "Hoạt động" });
                Point addressCell = supplierTable.getCellRect(0, 4, false).getLocation();
                assertEquals(longAddress, supplierTable.getToolTipText(new MouseEvent(supplierTable,
                        MouseEvent.MOUSE_MOVED, 0, 0, addressCell.x + 5, addressCell.y + 5, 0, false)));
                assertNull(supplierTable.getToolTipText(new MouseEvent(supplierTable,
                        MouseEvent.MOUSE_MOVED, 0, 0, 5, 5, 0, false)));
                render(new ManHinhPhieuNhap(new DieuKhienPhieuNhap(new GoiDichVuPhieuNhap(socket, session)),
                        new GoiDichVuNhaCungCap(socket, session), new GoiDichVuSanPham(socket, session)),
                        new File(dir, "receipts.png"), 1280, 720);
                KhungBanHang salesView = new KhungBanHang(new GoiDichVuHoaDon(socket, session),
                        new GoiDichVuSanPham(socket, session), new GoiDichVuKhachHang(socket, session));
                shell.showPage("Bán hàng & hóa đơn", salesView);
                render(shell, new File(dir, "sales-small-screen.png"), 1100, 600);
                assertTrue(salesView.getHeight() >= salesView.getPreferredSize().height);
                JScrollPane scroller = (JScrollPane) salesView.getParent().getParent();
                assertTrue(scroller.getVerticalScrollBar().isVisible());
                assertFalse(scroller.getHorizontalScrollBar().isVisible());
                socket.close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private static JMenuItem findMenu(KhungUngDung shell, String text) {
        for (int i = 0; i < shell.getMenuBar().getMenuCount(); i++) {
            JMenuItem item = findMenu(shell.getMenuBar().getMenu(i), text);
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    private static JMenuItem findMenu(JMenu root, String text) {
        if (text.equals(root.getText())) {
            return root;
        }
        for (Component c : root.getMenuComponents()) {
            if (c instanceof JMenu) {
                JMenuItem item = findMenu((JMenu) c, text);
                if (item != null) {
                    return item;
                }
            } else if (c instanceof JMenuItem && text.equals(((JMenuItem) c).getText())) {
                return (JMenuItem) c;
            }
        }
        return null;
    }

    private static void renderMenu(KhungUngDung shell, File file) throws Exception {
        BufferedImage image = new BufferedImage(shell.getWidth(), shell.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            shell.printAll(g);
            JMenu catalog = shell.getMenuBar().getMenu(1);
            Point origin = SwingUtilities.convertPoint(catalog, 0, catalog.getHeight(), shell);
            JPopupMenu popup = catalog.getPopupMenu();
            popup.setSize(popup.getPreferredSize());
            layout(popup);
            Graphics2D p = (Graphics2D) g.create(origin.x, origin.y, popup.getWidth(), popup.getHeight());
            popup.printAll(p);
            p.dispose();
            JMenu product = (JMenu) catalog.getItem(0);
            JPopupMenu nested = product.getPopupMenu();
            nested.setSize(nested.getPreferredSize());
            layout(nested);
            Graphics2D n = (Graphics2D) g.create(origin.x + popup.getWidth(), origin.y + product.getY(),
                    nested.getWidth(), nested.getHeight());
            nested.printAll(n);
            n.dispose();
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", file);
    }

    private static JButton findButton(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton && text.equals(((JButton) c).getText())) {
                return (JButton) c;
            }
            if (c instanceof Container) {
                JButton b = findButton((Container) c, text);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component c : root.getComponents()) {
            if (c instanceof Container) {
                layout((Container) c);
            }
        }
    }

    private static void render(JPanel panel, File file, int width, int height) throws Exception {
        panel.setSize(width, height);
        layout(panel);
        layout(panel);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        try {
            panel.printAll(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", file);
    }
}
