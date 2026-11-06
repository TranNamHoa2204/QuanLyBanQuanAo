package server.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import common.config.CauHinhUngDung;

/** Kết nối SQL Server lấy từ application.local.properties. */
public final class CauHinhCoSoDuLieu {
    private CauHinhCoSoDuLieu() {
    }

    public static Map<String, Object> load() {
        Properties properties;
        try {
            properties = CauHinhUngDung.load();
        } catch (IOException failure) {
            throw new IllegalStateException("Không đọc được cấu hình database.", failure);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("javax.persistence.jdbc.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        result.put("javax.persistence.jdbc.url",
                CauHinhUngDung.required(properties, "db.url", "QUANAO_DB_URL").trim());
        result.put("javax.persistence.jdbc.user",
                CauHinhUngDung.required(properties, "db.username", "QUANAO_DB_USER").trim());
        result.put("javax.persistence.jdbc.password",
                CauHinhUngDung.required(properties, "db.password", "QUANAO_DB_PASSWORD"));
        return result;
    }

}
