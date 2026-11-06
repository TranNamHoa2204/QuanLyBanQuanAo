package client.app;

import javax.swing.SwingUtilities;

import client.api.GoiDichVuXacThuc;
import client.config.ClientConfig;
import client.gui.components.ChuDeGiaoDien;
import client.gui.hethong.ManHinhDangNhap;
import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;

public final class ClientMain {
    private ClientMain() {
    }

    public static void main(String[] args) throws Exception {
        ClientConfig config = ClientConfig.load();
        KetNoiMayChu socket = new KetNoiMayChu(config.getHost(), config.getPort());
        PhienDangNhap session = new PhienDangNhap();
        GoiDichVuXacThuc api = new GoiDichVuXacThuc(socket, session);
        SwingUtilities.invokeLater(() -> {
            ChuDeGiaoDien.install();
            new ManHinhDangNhap(api, session).setVisible(true);
        });
    }
}
