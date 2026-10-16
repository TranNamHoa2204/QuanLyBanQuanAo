package server;

import server.config.ServerConfig;
import server.config.DatabaseConfig;
import server.persistence.JpaManager;
import server.session.SessionManager;
import server.security.Pbkdf2PasswordHasher;
import server.service.AuthService;
import server.service.SanPhamService;
import server.dispatcher.RequestDispatcher;
import server.network.SocketServer;

public final class ServerMain {
    private ServerMain() { }
    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.load();
        try (JpaManager jpa = new JpaManager(DatabaseConfig.fromEnvironment());
             SocketServer server = new SocketServer(config.getHost(), config.getPort(),
                     createDispatcher(jpa))) {
            Thread hook = new Thread(server::close, "server-shutdown");
            Runtime.getRuntime().addShutdownHook(hook);
            server.start();
            System.out.println("Server đang chạy tại " + config.getHost() + ":" + server.getPort());
            System.out.println("Đã bật: tài khoản, danh mục-sản phẩm-biến thể, khách hàng, nhân viên, khuyến mãi, hóa đơn/thanh toán, lịch sử kho và thống kê.");
            try { server.await(); }
            finally {
                try { Runtime.getRuntime().removeShutdownHook(hook); }
                catch (IllegalStateException shuttingDown) { }
            }
        }
    }
    private static RequestDispatcher createDispatcher(JpaManager jpa) {
        SessionManager sessions = new SessionManager();
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        AuthService auth = new AuthService(jpa, sessions, hasher);
        return new RequestDispatcher(auth, new SanPhamService(jpa, auth),
                new server.service.HoaDonService(jpa, auth), new server.service.NhanVienService(jpa, auth, sessions, hasher),
                new server.service.KhachHangService(jpa, auth), new server.service.KhuyenMaiService(jpa, auth),
                new server.service.ThongKeService(jpa, auth), new server.service.LichSuKhoService(jpa, auth));
    }
}
