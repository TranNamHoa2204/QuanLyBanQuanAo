package server.config;

import java.io.IOException;
import java.util.Properties;

import common.util.PropertiesLoader;

public final class ServerConfig {
    private final int port;
    private final String host;

    private ServerConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static ServerConfig load() throws IOException {
        Properties properties = PropertiesLoader.load("config/server.properties");
        int port = Integer.parseInt(properties.getProperty("server.port"));
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid server port.");
        }
        String host = properties.getProperty("server.bind.host", "127.0.0.1").trim();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Invalid bind host.");
        }
        return new ServerConfig(host, port);
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }
}
