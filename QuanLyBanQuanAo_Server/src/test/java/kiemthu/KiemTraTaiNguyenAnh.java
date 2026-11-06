package kiemthu;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import client.utils.TaiAnh;

/** Kiểm tra thủ công, không mở UI/Socket/database; không cần dependency ngoài JDK. */
public final class KiemTraTaiNguyenAnh {
    private KiemTraTaiNguyenAnh() {
    }

    public static void main(String[] args) throws Exception {
        int count = 0;
        InputStream stream = KiemTraTaiNguyenAnh.class.getResourceAsStream("/data/image-paths.txt");
        if (stream == null) {
            throw new AssertionError("Missing asset manifest");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String path;
            while ((path = reader.readLine()) != null) {
                if (path.trim().isEmpty()) {
                    continue;
                }
                BufferedImage image = TaiAnh.load(path);
                if (image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw new AssertionError(path);
                }
                count++;
            }
        }
        if (count != 267) {
            throw new AssertionError("Unexpected asset count: " + count);
        }
        System.out.println("PASS: all " + count + " image resources decode through Java ImageIO.");
    }
}
