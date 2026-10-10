package client.config;

import java.io.IOException;
import java.util.Properties;

import common.util.PropertiesLoader;

public final class ClientConfig {
    private final String host;
    private final int port;
    private ClientConfig(String host, int port) { this.host = host; this.port = port; }
    public static ClientConfig load() throws IOException {
        Properties properties = PropertiesLoader.load("config/client.properties");
        String host = properties.getProperty("server.host", "").trim();
        int port = Integer.parseInt(properties.getProperty("server.port"));
        if (host.isEmpty() || port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid server address.");
        }
        return new ClientConfig(host, port);
    }
    public String getHost() { return host; }
    public int getPort() { return port; }
}
