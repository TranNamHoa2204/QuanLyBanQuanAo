package client.ui;

import java.awt.*;
import javax.swing.*;
import client.api.AuthApi;
import client.session.ClientSession;
import common.dto.LoginResponse;
import common.enums.VaiTro;

public final class MainForm extends JFrame {
    private static final long serialVersionUID = 1L;
    private SanPhamPanel productPanel;
    private BanHangPanel salesPanel;
    private QuanLySanPhamPanel managementPanel;
    private MainShellPanel shell;
    public MainForm(AuthApi api, ClientSession session) {
        super("Quản lý cửa hàng quần áo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        LoginResponse login = session.getLogin();
        shell = new MainShellPanel(login, () -> {
            if (productPanel == null) { productPanel = new SanPhamPanel(
                    new client.controller.SanPhamController(api.products()), login.getVaiTro()); }
            shell.showPage("Sản phẩm", productPanel); productPanel.reload();
        }, () -> {
            boolean first = salesPanel == null;
            if (first) { salesPanel = new BanHangPanel(api.invoices(), api.products()); }
            shell.showPage("Bán hàng & hóa đơn", salesPanel);
            if (first) { salesPanel.loadTaxes(); }
        }, () -> new ThongTinCaNhanForm(this, api).setVisible(true),
        () -> new DoiMatKhauForm(this, api, () -> {
            dispose(); new DangNhapForm(api, session).setVisible(true);
        }).setVisible(true), logout -> UiTask.run(this, logout, () -> {
            api.logout(); return null;
        }, result -> { dispose(); new DangNhapForm(api, session).setVisible(true); }), () -> {
            if (managementPanel == null) {
                managementPanel = new QuanLySanPhamPanel(new client.controller.QuanLySanPhamController(api.products()));
            }
            shell.showPage("Danh mục & sản phẩm", managementPanel); managementPanel.initialize();
        });
        add(shell);
        Rectangle screen = getGraphicsConfiguration().getBounds();
        Insets inset = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        int availableWidth = screen.width - inset.left - inset.right;
        int availableHeight = screen.height - inset.top - inset.bottom;
        setMinimumSize(new Dimension(Math.min(1100, availableWidth), Math.min(700, availableHeight)));
        setSize(Math.min(1380, availableWidth), Math.min(900, availableHeight)); setLocationRelativeTo(null);
    }
}
