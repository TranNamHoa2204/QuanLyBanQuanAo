import static org.junit.Assert.*;
import java.io.InputStream;
import java.util.*;
import org.junit.Test;
import com.fasterxml.jackson.databind.JsonNode;
import common.dto.*;
import common.enums.VaiTro;
import common.protocol.JsonCodec;
import client.api.*;
import client.network.SocketClient;
import client.session.ClientSession;
import client.util.ImageLoader;
import server.persistence.JpaManager;
import server.session.SessionManager;
import server.security.Pbkdf2PasswordHasher;
import server.service.*;
import server.dispatcher.RequestDispatcher;
import server.network.SocketServer;

public class NormalizedCatalogTest {
    @Test public void entireUserCatalogSurvivesJpaSocketAndImageThumbnail() throws Exception {
        JsonNode source;
        try (InputStream input = getClass().getResourceAsStream("/data/catalog.json")) {
            assertNotNull(input); source = JsonCodec.mapper().readTree(input);
        }
        try (JpaManager jpa = new JpaManager(CatalogTestSupport.memoryProperties("normalized_catalog_test"))) {
            Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
            String hash = hasher.hash("CatalogPass123!".toCharArray());
            jpa.transaction(em -> {
                CatalogTestSupport.catalog(em, source);
                CatalogTestSupport.user(em, "catalog", "NVTEST", VaiTro.NHAN_VIEN_BAN_HANG, hash); return null;
            });
            SessionManager sessions = new SessionManager(); AuthService auth = new AuthService(jpa, sessions, hasher);
            try (SocketServer server = new SocketServer("127.0.0.1", 0,
                    new RequestDispatcher(auth, new SanPhamService(jpa, auth)))) {
                server.start();
                try (SocketClient socket = new SocketClient("127.0.0.1", server.getPort())) {
                    ClientSession session = new ClientSession();
                    new AuthApi(socket, session).login("catalog", "CatalogPass123!");
                    SanPhamApi api = new SanPhamApi(socket, session);
                    SanPhamSearchRequest query = new SanPhamSearchRequest(); query.setPageSize(100);
                    SanPhamSearchResult all = api.search(query);
                    assertEquals(40, all.getTotal()); assertEquals(40, all.getItems().size());
                    assertEquals(6, api.categories().size()); assertEquals(3, api.brands().size());
                    int variants = 0, images = 0;
                    for (SanPhamDTO row : all.getItems()) {
                        SanPhamDetailDTO detail = api.detail(row.getMaSP());
                        assertEquals(row.getGiaBan(), detail.getSanPham().getGiaBan());
                        assertEquals(row.getTongTon(), detail.getSanPham().getTongTon());
                        variants += detail.getBienThe().size(); images += detail.getHinhAnh().size();
                        Set<String> colors = new HashSet<>();
                        for (BienTheDTO b : detail.getBienThe()) { colors.add(b.getMauSac()); }
                        for (AnhSanPhamDTO a : detail.getHinhAnh()) {
                            assertTrue(colors.contains(a.getMauSac()));
                            assertNotNull(getClass().getClassLoader().getResource(a.getDuongDan()));
                        }
                    }
                    assertEquals(152, variants); assertEquals(255, images);
                    java.awt.image.BufferedImage thumbnail = ImageLoader.thumbnail(
                            api.detail("SP1001").getHinhAnh().get(0).getDuongDan(), 350, 400);
                    assertTrue(thumbnail.getWidth() <= 350); assertTrue(thumbnail.getHeight() <= 400);
                    try { ImageLoader.load("images/products/missing.jpg"); fail("Missing image"); }
                    catch (java.io.IOException expected) { }
                    try { ImageLoader.load("../secrets.png"); fail("Unsafe path"); }
                    catch (IllegalArgumentException expected) { }
                }
            }
        }
    }
}
