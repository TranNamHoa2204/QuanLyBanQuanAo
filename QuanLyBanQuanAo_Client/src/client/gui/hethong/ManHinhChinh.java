package client.gui.hethong;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JFrame;

import client.api.GoiDichVuXacThuc;
import client.controllers.DieuKhienKhachHang;
import client.controllers.DieuKhienKhuyenMai;
import client.controllers.DieuKhienNhaCungCap;
import client.controllers.DieuKhienNhanVien;
import client.controllers.DieuKhienPhieuNhap;
import client.controllers.DieuKhienQuanLySanPham;
import client.controllers.DieuKhienSanPham;
import client.controllers.DieuKhienThongKe;
import client.gui.banhang.KhungBanHang;
import client.gui.components.BieuTuongCuaHang;
import client.gui.components.TacVuGiaoDien;
import client.gui.khachhang.ManHinhKhachHang;
import client.gui.kho.KhungLichSuKho;
import client.gui.khuyenmai.ManHinhKhuyenMai;
import client.gui.nhanvien.ManHinhNhanVien;
import client.gui.nhapkho.ManHinhNhaCungCap;
import client.gui.nhapkho.ManHinhPhieuNhap;
import client.gui.sanpham.KhungQuanLySanPham;
import client.gui.sanpham.KhungSanPham;
import client.gui.thongke.ManHinhThongKe;
import client.session.PhienDangNhap;
import common.dto.taikhoan.KetQuaDangNhap;

public final class ManHinhChinh extends JFrame {
    private static final long serialVersionUID = 1L;
    private KhungSanPham productPanel;
    private KhungBanHang salesPanel;
    private KhungQuanLySanPham managementPanel;
    private KhungLichSuKho stockHistoryPanel;
    private ManHinhNhanVien employeePanel;
    private ManHinhKhachHang customerPanel;
    private ManHinhKhuyenMai promotionPanel;
    private ManHinhThongKe statisticsPanel;
    private ManHinhNhaCungCap supplierPanel;
    private ManHinhPhieuNhap receiptPanel;
    private KhungUngDung shell;

    public ManHinhChinh(GoiDichVuXacThuc api, PhienDangNhap session) {
        super("Quản lý cửa hàng quần áo");
        setIconImage(BieuTuongCuaHang.windowIcon());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        KetQuaDangNhap login = session.getLogin();
        shell = new KhungUngDung(login, () -> {
            if (productPanel == null) {
                productPanel = new KhungSanPham(new DieuKhienSanPham(api.products()), login.getVaiTro());
            }
            shell.showPage("Sản phẩm", productPanel);
            productPanel.reload();
        }, () -> openSales(api, false), () -> new ManHinhThongTinCaNhan(this, api).setVisible(true),
                () -> new ManHinhDoiMatKhau(this, api, () -> {
                    dispose();
                    new ManHinhDangNhap(api, session).setVisible(true);
                }).setVisible(true), logout -> TacVuGiaoDien.run(this, logout, () -> {
                    api.logout();
                    return null;
                }, result -> {
                    dispose();
                    new ManHinhDangNhap(api, session).setVisible(true);
                }), () -> openCatalogSection(api, KhungQuanLySanPham.Section.PRODUCTS), () -> {
                    if (stockHistoryPanel == null) {
                        stockHistoryPanel = new KhungLichSuKho(api.stockHistory());
                    }
                    shell.showPage("Lịch sử kho", stockHistoryPanel);
                    stockHistoryPanel.reload();
                }, () -> {
                    if (employeePanel == null) {
                        employeePanel = new ManHinhNhanVien(new DieuKhienNhanVien(api.employees()));
                    }
                    shell.showPage("Nhân viên", employeePanel);
                    employeePanel.reload();
                }, () -> {
                    if (promotionPanel == null) {
                        promotionPanel = new ManHinhKhuyenMai(new DieuKhienKhuyenMai(api.promotions()));
                    }
                    shell.showPage("Khuyến mãi", promotionPanel);
                    promotionPanel.reload();
                }, () -> {
                    if (statisticsPanel == null) {
                        statisticsPanel = new ManHinhThongKe(new DieuKhienThongKe(api.statistics()));
                    }
                    shell.showPage("Thống kê", statisticsPanel);
                    statisticsPanel.reload();
                }, () -> {
                    if (customerPanel == null) {
                        customerPanel = new ManHinhKhachHang(new DieuKhienKhachHang(api.customers()));
                    }
                    shell.showPage("Khách hàng", customerPanel);
                    customerPanel.reload();
                }, () -> {
                    if (supplierPanel == null) {
                        supplierPanel = new ManHinhNhaCungCap(new DieuKhienNhaCungCap(api.suppliers()));
                    }
                    shell.showPage("Nhà cung cấp", supplierPanel);
                    supplierPanel.reload();
                }, () -> {
                    if (receiptPanel == null) {
                        receiptPanel = new ManHinhPhieuNhap(new DieuKhienPhieuNhap(api.receipts()),
                                api.suppliers(), api.products());
                    }
                    shell.showPage("Phiếu nhập kho", receiptPanel);
                    receiptPanel.reload();
                }, section -> openCatalogSection(api, section), () -> openSales(api, true));
        setJMenuBar(shell.getMenuBar());
        add(shell);
        Rectangle screen = getGraphicsConfiguration().getBounds();
        Insets inset = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        int availableWidth = screen.width - inset.left - inset.right;
        int availableHeight = screen.height - inset.top - inset.bottom;
        setMinimumSize(new Dimension(Math.min(1100, availableWidth), Math.min(700, availableHeight)));
        setSize(Math.min(1380, availableWidth), Math.min(900, availableHeight));
        setLocationRelativeTo(null);
    }

    private void openCatalogSection(GoiDichVuXacThuc api, KhungQuanLySanPham.Section section) {
        if (managementPanel == null) {
            managementPanel = new KhungQuanLySanPham(
                    new DieuKhienQuanLySanPham(api.products(), api.stockHistory()));
        }
        shell.showPage("Danh mục / " + section.getTitle(), managementPanel);
        managementPanel.showSection(section);
    }

    private void openSales(GoiDichVuXacThuc api, boolean invoiceLookup) {
        if (salesPanel == null) {
            salesPanel = new KhungBanHang(api.invoices(), api.products(), api.customers());
        }
        shell.showPage("Bán hàng & hóa đơn", salesPanel);
        if (invoiceLookup) {
            salesPanel.openInvoiceHistory();
        } else {
            salesPanel.loadInitialData();
        }
    }
}
