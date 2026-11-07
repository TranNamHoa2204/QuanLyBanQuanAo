package server.app;

import server.config.CauHinhCoSoDuLieu;
import server.config.ServerConfig;
import server.dispatcher.DieuPhoiYeuCau;
import server.network.MayChuTcp;
import server.security.BoBamMatKhauPbkdf2;
import server.services.DichVuHoaDon;
import server.services.DichVuCapMa;
import server.services.DichVuKhachHang;
import server.services.DichVuKhuyenMai;
import server.services.DichVuLichSuKho;
import server.services.DichVuNhaCungCap;
import server.services.DichVuNhanVien;
import server.services.DichVuPhieuNhap;
import server.services.DichVuSanPham;
import server.services.DichVuThongKe;
import server.services.DichVuXacThuc;
import server.session.QuanLyPhienDangNhap;
import server.utils.QuanLyJpa;

public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.load();
        try (QuanLyJpa jpa = new QuanLyJpa(CauHinhCoSoDuLieu.load());
                MayChuTcp server = new MayChuTcp(config.getHost(), config.getPort(), createDispatcher(jpa))) {
            Thread hook = new Thread(server::close, "server-shutdown");
            Runtime.getRuntime().addShutdownHook(hook);
            server.start();
            System.out.println("Server đang chạy tại " + config.getHost() + ":" + server.getPort());
            System.out.println(
                    "Đã bật: tài khoản, danh mục-sản phẩm-biến thể, khách hàng, nhân viên, khuyến mãi, hóa đơn/thanh toán, lịch sử kho, nhà cung cấp/phiếu nhập và thống kê.");
            try {
                server.await();
            } finally {
                try {
                    Runtime.getRuntime().removeShutdownHook(hook);
                } catch (IllegalStateException shuttingDown) {
                }
            }
        }
    }

    private static DieuPhoiYeuCau createDispatcher(QuanLyJpa jpa) {
        QuanLyPhienDangNhap sessions = new QuanLyPhienDangNhap();
        BoBamMatKhauPbkdf2 hasher = new BoBamMatKhauPbkdf2();
        DichVuXacThuc auth = new DichVuXacThuc(jpa, sessions, hasher);
        return new DieuPhoiYeuCau(auth, new DichVuSanPham(jpa, auth), new DichVuHoaDon(jpa, auth),
                new DichVuNhanVien(jpa, auth, sessions, hasher), new DichVuKhachHang(jpa, auth),
                new DichVuKhuyenMai(jpa, auth), new DichVuThongKe(jpa, auth), new DichVuLichSuKho(jpa, auth),
                new DichVuNhaCungCap(jpa, auth), new DichVuPhieuNhap(jpa, auth), new DichVuCapMa(jpa, auth));
    }
}
