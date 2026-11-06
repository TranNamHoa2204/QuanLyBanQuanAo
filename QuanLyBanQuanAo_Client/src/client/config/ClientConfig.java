package client.config;

import java.io.IOException;
import java.util.Properties;

import common.config.CauHinhUngDung;

public final class ClientConfig {
    private final String host;
    private final int port;

    private ClientConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static ClientConfig load() throws IOException {
        Properties properties = CauHinhUngDung.withDefaults("config/client.properties");
        String host = properties.getProperty("server.host", "").trim();
        int port = Integer.parseInt(properties.getProperty("server.port").trim());
        if (host.isEmpty() || port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid server address.");
        }
        return new ClientConfig(host, port);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
