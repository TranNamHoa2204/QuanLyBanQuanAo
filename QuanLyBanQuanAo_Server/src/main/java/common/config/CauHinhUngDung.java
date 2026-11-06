package common.config;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import common.utils.DocThuocTinh;

/** Cấu hình UTF-8 ngoài source; dùng chung cho server, client và công cụ setup. */
public final class CauHinhUngDung {
    public static final String FILE_NAME = "application.local.properties";
    public static final String PATH_PROPERTY = "quanao.config";

    private CauHinhUngDung() {
    }

    public static Properties load() throws IOException {
        Properties properties = new Properties();
        Path file = findFile();
        if (file != null) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
        return properties;
    }

    public static Properties withDefaults(String resource) throws IOException {
        Properties properties = DocThuocTinh.load(resource);
        properties.putAll(load());
        return properties;
    }

    public static String required(Properties properties, String key, String environmentName) {
        // File cục bộ là nguồn chính; biến môi trường chỉ bù khóa chưa cấu hình.
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(environmentName);
        }
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu cấu hình " + key + " trong " + FILE_NAME + ".");
        }
        return value;
    }

    private static Path findFile() throws IOException {
        String configuredPath = System.getProperty(PATH_PROPERTY);
        if (configuredPath != null && !configuredPath.trim().isEmpty()) {
            Path file = Paths.get(configuredPath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(file)) {
                throw new IOException("Không tìm thấy file cấu hình: " + file);
            }
            return file;
        }
        Path workingFile = Paths.get(FILE_NAME).toAbsolutePath();
        if (Files.isRegularFile(workingFile)) {
            return workingFile;
        }
        // Eclipse/Maven: target/classes -> target -> project. JAR: thư mục chứa JAR.
        try {
            Path directory = Paths
                    .get(CauHinhUngDung.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isDirectory(directory)) {
                directory = directory.getParent();
            }
            for (int i = 0; directory != null && i < 4; i++, directory = directory.getParent()) {
                Path candidate = directory.resolve(FILE_NAME);
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        } catch (URISyntaxException | IllegalArgumentException failure) {
            throw new IOException("Không xác định được vị trí " + FILE_NAME + ".", failure);
        }
        return null;
    }
}
