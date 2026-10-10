package common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class PropertiesLoader {
    private PropertiesLoader() { }
    public static Properties load(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = PropertiesLoader.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) { throw new IOException("Missing resource: " + resource); }
            // UTF-8, không dùng cách load InputStream với charset ngầm định.
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        return properties;
    }
}
