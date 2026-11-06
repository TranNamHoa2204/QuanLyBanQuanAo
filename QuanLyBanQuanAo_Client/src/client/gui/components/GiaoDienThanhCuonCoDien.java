package client.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

public final class GiaoDienThanhCuonCoDien extends BasicScrollBarUI {
    public static ComponentUI createUI(JComponent component) {
        return new GiaoDienThanhCuonCoDien();
    }

    @Override protected JButton createDecreaseButton(int direction) {
        return ChuDeGiaoDien.arrow(direction, new Dimension(16, 16));
    }

    @Override protected JButton createIncreaseButton(int direction) {
        return ChuDeGiaoDien.arrow(direction, new Dimension(16, 16));
    }

    @Override
    protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
        if (bounds.isEmpty() || !component.isEnabled()) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            boolean vertical = scrollbar.getOrientation() == JScrollBar.VERTICAL;
            g.translate(bounds.x, bounds.y);
            Color dark = isDragging || isThumbRollover() ? new Color(0x8DB6ED) : new Color(0xAFCAE9);
            g.setPaint(new GradientPaint(0, 0, new Color(0xEBF4FF), vertical ? bounds.width : 0,
                    vertical ? 0 : bounds.height, dark));
            g.fillRect(0, 0, bounds.width, bounds.height);
            g.setColor(ChuDeGiaoDien.TABLE_BORDER);
            g.drawRect(0, 0, bounds.width - 1, bounds.height - 1);
            if (bounds.width > 9 && bounds.height > 9) {
                int x = bounds.width / 2, y = bounds.height / 2;
                for (int offset = -3; offset <= 3; offset += 3) {
                    if (vertical) {
                        g.drawLine(x - 3, y + offset, x + 3, y + offset);
                    } else {
                        g.drawLine(x + offset, y - 3, x + offset, y + 3);
                    }
                }
            }
        } finally {
            g.dispose();
        }
    }
}
