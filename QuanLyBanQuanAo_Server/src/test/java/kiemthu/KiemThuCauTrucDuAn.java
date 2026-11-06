package kiemthu;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Entity;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.NodeList;

/** Kiểm tra ranh giới tầng, package, ánh xạ JPA và các điểm khởi chạy sau khi sắp xếp. */
public class KiemThuCauTrucDuAn {
    @Test
    public void packageAndPublicTypeMatchTheSourceLocation() throws Exception {
        Pattern packagePattern = Pattern.compile("(?m)^package\\s+([\\w.]+);");
        Pattern typePattern = Pattern
                .compile("public\\s+(?:(?:final|abstract)\\s+)?(?:class|interface|enum)\\s+(\\w+)");
        Pattern oldSuffix = Pattern.compile(
                ".*(?:Controller|Form|Panel|Service|DTO|Dao|Api|Exception|Request|Response|Manager|Utils|Loader|Test)$");
        for (String folder : new String[] { "QuanLyBanQuanAo_Server/src/main/java",
                "QuanLyBanQuanAo_Server/src/test/java", "QuanLyBanQuanAo_Client/src" }) {
            Path root = workspace().resolve(folder);
            for (Path path : javaFiles(root)) {
                String text = read(path);
                Matcher packageName = packagePattern.matcher(text);
                Matcher typeName = typePattern.matcher(text);
                assertTrue(path.toString(), packageName.find());
                assertTrue(path.toString(), typeName.find());
                assertEquals(path.toString(), typeName.group(1) + ".java", path.getFileName().toString());
                assertEquals(path.toString(), packageName.group(1).replace('.', '/'),
                        root.relativize(path.getParent()).toString().replace('\\', '/'));
                assertTrue(path.toString(), typeName.group(1).matches("[A-Za-z][A-Za-z0-9]*"));
                assertFalse(path.toString(), oldSuffix.matcher(typeName.group(1)).matches());
            }
        }
    }

    @Test
    public void clientAndSharedDataDoNotDependOnServerOrJpa() throws Exception {
        Pattern forbidden = Pattern
                .compile("(?m)^import\\s+(?:server\\.|javax\\.persistence\\.|org\\.hibernate\\.)");
        for (String folder : new String[] { "QuanLyBanQuanAo_Client/src/client",
                "QuanLyBanQuanAo_Server/src/main/java/common" }) {
            for (Path path : javaFiles(workspace().resolve(folder))) {
                assertFalse("Sai ranh giới tầng: " + path, forbidden.matcher(read(path)).find());
            }
        }
    }

    @Test
    public void persistenceRegistersAllSeventeenEntitiesInTheirNewPackage() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (InputStream input = getClass().getResourceAsStream("/META-INF/persistence.xml")) {
            assertNotNull(input);
            NodeList entities = factory.newDocumentBuilder().parse(input).getElementsByTagName("class");
            assertEquals(17, entities.getLength());
            for (int i = 0; i < entities.getLength(); i++) {
                String name = entities.item(i).getTextContent().trim();
                assertTrue(name, name.startsWith("server.entities."));
                assertTrue(name, Class.forName(name).isAnnotationPresent(Entity.class));
            }
        }
    }

    @Test
    public void serverAndClientExposeRunnableEntryPoints() throws Exception {
        String launcher = read(Paths.get("scripts/ChayUngDung.ps1"));
        for (String entry : new String[] { "server.app.ServerMain", "client.app.ClientMain" }) {
            java.lang.reflect.Method main = Class.forName(entry).getMethod("main", String[].class);
            assertTrue(Modifier.isStatic(main.getModifiers()));
            assertEquals(void.class, main.getReturnType());
            assertTrue(launcher.contains(entry));
        }
        assertTrue(read(Paths.get("scripts/KiemThuCucBo.ps1")).contains("kiemthu.KiemTraNenTang"));
        assertTrue(read(Paths.get("QuanLyBanQuanAo_Server/pom.xml")).contains("**/KiemThu*.java"));
    }

    @Test
    public void twoProjectsMatchTheMavenServerAndPlainJavaClientLayout() throws Exception {
        assertTrue(read(Paths.get("QuanLyBanQuanAo_Server/.project"))
                .contains("org.eclipse.m2e.core.maven2Nature"));
        assertFalse(read(Paths.get("QuanLyBanQuanAo_Client/.project")).contains("org.eclipse.m2e"));
        String clientClasspath = read(Paths.get("QuanLyBanQuanAo_Client/.classpath"));
        assertTrue(clientClasspath.contains("lib/server.jar"));
        assertFalse(clientClasspath.contains("MAVEN2_CLASSPATH_CONTAINER"));
        assertFalse(clientClasspath.contains("QuanLyBanQuanAo_Server/src"));
        assertTrue(Files.isRegularFile(workspace().resolve("QuanLyBanQuanAo_Server/pom.xml")));
        assertFalse(Files.exists(workspace().resolve("QuanLyBanQuanAo_Client/pom.xml")));
        assertFalse(Files.exists(workspace().resolve("pom.xml")));
        assertTrue(Files.isDirectory(workspace().resolve("QuanLyBanQuanAo_Server/src/main/java/common")));
        assertFalse(Files.exists(workspace().resolve("QuanLyBanQuanAo_Client/src/common")));
        assertFalse(Files.exists(workspace().resolve("QuanLyBanQuanAo_Client/src/server")));
    }

    private static List<Path> javaFiles(Path root) throws Exception {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path.isAbsolute() ? path : workspace().resolve(path)),
                StandardCharsets.UTF_8);
    }

    private static Path workspace() {
        Path directory = Paths.get("").toAbsolutePath();
        if (Files.isDirectory(directory.resolve("QuanLyBanQuanAo_Server"))) {
            return directory;
        }
        return directory.getParent();
    }
}
