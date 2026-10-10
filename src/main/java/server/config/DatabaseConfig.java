package server.config;

import java.util.HashMap;
import java.util.Map;

/** Đặt biến môi trường trong Eclipse Run Configuration → Environment. */
public final class DatabaseConfig {
    private DatabaseConfig() { }
    public static Map<String, Object> fromEnvironment() {
        Map<String, Object> result = new HashMap<>();
        result.put("javax.persistence.jdbc.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        result.put("javax.persistence.jdbc.url", required("QUANAO_DB_URL"));
        result.put("javax.persistence.jdbc.user", required("QUANAO_DB_USER"));
        result.put("javax.persistence.jdbc.password", required("QUANAO_DB_PASSWORD"));
        return result;
    }
    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Chưa cấu hình biến môi trường " + name);
        }
        return value;
    }
}
