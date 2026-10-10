package client.ui;

import java.awt.*;
import javax.swing.Icon;

/** Biểu tượng móc áo vector tự vẽ, không cần file ảnh hoặc thư viện icon ngoài. */
public final class BrandIcon implements Icon {
    private final int size;
    private final Color color;
    public BrandIcon(int size, Color color) { this.size = size; this.color = color; }
    public int getIconWidth() { return size; }
    public int getIconHeight() { return size; }
    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D)graphics.create();
        try {
            g.translate(x, y); g.scale(size / 64.0, size / 64.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color); g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(25, 9, 14, 16, 0, 190); g.drawLine(32, 25, 32, 30);
            g.drawLine(32, 30, 8, 45); g.drawLine(32, 30, 56, 45);
            g.drawLine(8, 45, 8, 50); g.drawLine(56, 45, 56, 50); g.drawLine(8, 50, 56, 50);
        } finally { g.dispose(); }
    }
}
