package client.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Đọc JPEG/PNG từ classpath, hoạt động cả khi đóng gói JAR. */
public final class ImageLoader {
    private ImageLoader() {
    }

    public static BufferedImage thumbnail(String path, int maxWidth, int maxHeight) throws IOException {
        if (maxWidth < 1 || maxHeight < 1 || maxWidth > 2048 || maxHeight > 2048) {
            throw new IllegalArgumentException("Invalid thumbnail dimensions.");
        }
        BufferedImage original = load(path);
        double ratio = Math.min(1.0,
                Math.min((double) maxWidth / original.getWidth(), (double) maxHeight / original.getHeight()));
        int width = Math.max(1, (int) Math.round(original.getWidth() * ratio));
        int height = Math.max(1, (int) Math.round(original.getHeight() * ratio));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = result.createGraphics();
        try {
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(original, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    public static BufferedImage load(String resourcePath) throws IOException {
        if (resourcePath == null) {
            throw new IllegalArgumentException("Image path cannot be null.");
        }
        String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        if (!path.startsWith("images/") || path.contains("..") || path.contains("\\")
                || !(path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png"))) {
            throw new IllegalArgumentException("Invalid image resource path.");
        }
        try (InputStream stream = ImageLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Image resource not found: " + path);
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                throw new IOException("Unsupported image resource: " + path);
            }
            return image;
        }
    }
}
