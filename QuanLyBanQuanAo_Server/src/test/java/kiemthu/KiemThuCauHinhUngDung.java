package kiemthu;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.Test;

import client.config.ClientConfig;
import common.config.CauHinhUngDung;
import server.config.CauHinhCoSoDuLieu;
import server.config.ServerConfig;

public class KiemThuCauHinhUngDung {
    @Test
    public void oneUtf8FileConfiguresDatabaseServerAndClient() throws Exception {
        withFile("db.url=jdbc:sqlserver://example.test:1444;databaseName=Demo\n"
                + "db.username=demo_user\ndb.password=p\\\\a\\tss=word!\n"
                + "server.bind.host=127.0.0.2\nserver.host=example.test\nserver.port=9123\n"
                + "setup.employee.name=Nguyễn Minh Anh\n", () -> {
                    assertEquals("example.test", ClientConfig.load().getHost());
                    assertEquals("127.0.0.2", ServerConfig.load().getHost());
                    assertEquals(9123, ClientConfig.load().getPort());
                    assertEquals(ServerConfig.load().getPort(), ClientConfig.load().getPort());
                    assertEquals("jdbc:sqlserver://example.test:1444;databaseName=Demo",
                            CauHinhCoSoDuLieu.load().get("javax.persistence.jdbc.url"));
                    assertEquals("demo_user", CauHinhCoSoDuLieu.load().get("javax.persistence.jdbc.user"));
                    assertEquals("p\\a\tss=word!",
                            CauHinhCoSoDuLieu.load().get("javax.persistence.jdbc.password"));
                    assertEquals("Nguyễn Minh Anh", CauHinhUngDung.load().getProperty("setup.employee.name"));
                });
    }

    @Test
    public void explicitMissingFileDoesNotSilentlyUseAnotherConfiguration() throws Exception {
        String previous = System.getProperty(CauHinhUngDung.PATH_PROPERTY);
        Path directory = Files.createTempDirectory("quanao-config-missing-");
        Path missing = directory.resolve("missing.properties");
        try {
            System.setProperty(CauHinhUngDung.PATH_PROPERTY, missing.toString());
            try {
                ClientConfig.load();
                fail("Missing explicit file accepted");
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("missing.properties"));
            }
        } finally {
            restore(previous);
            Files.delete(directory);
        }
    }

    @Test
    public void localPortIsValidatedByBothApplications() throws Exception {
        withFile("server.port=70000\n", () -> {
            try {
                ServerConfig.load();
                fail("Invalid server port accepted");
            } catch (IllegalArgumentException expected) {
            }
            try {
                ClientConfig.load();
                fail("Invalid client port accepted");
            } catch (IllegalArgumentException expected) {
            }
        });
    }

    @Test
    public void localValuesTakePriorityAndNetworkDefaultsStillWork() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("value", "from-local-file");
        assertEquals("from-local-file", CauHinhUngDung.required(properties, "value", "PATH"));
        withFile("# Network defaults\n", () -> {
            assertEquals(8888, ServerConfig.load().getPort());
            assertEquals("localhost", ClientConfig.load().getHost());
            assertEquals(8888, ClientConfig.load().getPort());
        });
    }

    private interface Checked {
        void run() throws Exception;
    }

    private static void withFile(String content, Checked action) throws Exception {
        String previous = System.getProperty(CauHinhUngDung.PATH_PROPERTY);
        Path file = Files.createTempFile("quanao-config-", ".properties");
        try {
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
            System.setProperty(CauHinhUngDung.PATH_PROPERTY, file.toString());
            action.run();
        } finally {
            restore(previous);
            Files.delete(file);
        }
    }

    private static void restore(String previous) {
        if (previous == null) {
            System.clearProperty(CauHinhUngDung.PATH_PROPERTY);
        } else {
            System.setProperty(CauHinhUngDung.PATH_PROPERTY, previous);
        }
    }
}
