package client;

import client.config.ClientConfig;
import client.network.SocketClient;
import client.session.ClientSession;
import client.api.AuthApi;
import client.ui.DangNhapForm;
import javax.swing.SwingUtilities;

public final class ClientMain {
    private ClientMain() { }
    public static void main(String[] args) throws Exception {
        ClientConfig config = ClientConfig.load();
        SocketClient socket = new SocketClient(config.getHost(), config.getPort());
        ClientSession session = new ClientSession();
        AuthApi api = new AuthApi(socket, session);
        SwingUtilities.invokeLater(() -> {
            client.ui.UiTheme.install();
            new DangNhapForm(api, session).setVisible(true);
        });
    }
}
