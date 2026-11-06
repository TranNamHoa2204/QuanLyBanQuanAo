package client.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;

/** Vẽ nút nổi kiểu Windows XP. Swing xử lý bàn phím và thao tác bấm. */
public final class GiaoDienNutCoDien extends BasicButtonUI {
    public static ComponentUI createUI(JComponent component) {
        return new GiaoDienNutCoDien();
    }

    @Override
    protected void installDefaults(AbstractButton button) {
        super.installDefaults(button);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
    }

    @Override
    public Dimension getPreferredSize(JComponent component) {
        Dimension size = super.getPreferredSize(component);
        if (size != null) {
            size.height = Math.max(size.height, 29);
        }
        return size;
    }

    @Override
    public void paint(Graphics graphics, JComponent component) {
        AbstractButton button = (AbstractButton) component;
        if (button.isContentAreaFilled() && button.getWidth() > 4 && button.getHeight() > 4) {
            paintFace(graphics, button);
        }
        super.paint(graphics, component);
    }

    private void paintFace(Graphics graphics, AbstractButton button) {
        ButtonModel model = button.getModel();
        Color base = button.getBackground();
        boolean neutral = ChuDeGiaoDien.BACKGROUND.equals(base);
        Color light = neutral ? Color.WHITE : base;
        Color dark = neutral ? new Color(0xD9D6C7) : base.darker();
        boolean pressed = model.isArmed() && model.isPressed();
        if (!button.isEnabled()) {
            light = new Color(0xF5F4EF);
            dark = new Color(0xE5E3DB);
        }
        int width = button.getWidth() - 1;
        int height = button.getHeight() - 1;
        int arc = 5;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, pressed ? dark : light, 0, height, pressed ? light : dark));
            g.fillRoundRect(0, 0, width, height, arc, arc);
            if (button.isBorderPainted()) {
                g.setColor(button.isEnabled() ? new Color(0x003C74) : new Color(0xC2C0B5));
                g.drawRoundRect(0, 0, width, height, arc, arc);
                g.setColor(pressed ? new Color(0, 0, 0, 70) : new Color(255, 255, 255, 150));
                if (button.isEnabled() && model.isRollover() && !pressed) {
                    g.setColor(new Color(0xF2AE32));
                    g.drawRoundRect(2, 2, width - 4, height - 4, arc, arc);
                } else if (button.isEnabled() && button instanceof JButton && ((JButton) button).isDefaultButton()) {
                    g.setColor(new Color(0x8BB8EC));
                }
                g.drawRoundRect(1, 1, width - 2, height - 2, arc, arc);
            }
        } finally {
            g.dispose();
        }
    }

    @Override
    protected void paintButtonPressed(Graphics graphics, AbstractButton button) {
        setTextShiftOffset();
    }

    @Override
    protected void paintText(Graphics graphics, AbstractButton button, Rectangle bounds, String text) {
        if (button.isEnabled()) {
            super.paintText(graphics, button, bounds, text);
        } else {
            graphics.setColor(new Color(0x77776D));
            BasicGraphicsUtils.drawStringUnderlineCharAt(graphics, text, button.getDisplayedMnemonicIndex(),
                    bounds.x, bounds.y + graphics.getFontMetrics().getAscent());
        }
    }

    @Override
    protected void paintFocus(Graphics graphics, AbstractButton button, Rectangle view, Rectangle text, Rectangle icon) {
        int inset = 4;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(button.getForeground());
            g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[] { 1, 1 }, 0));
            g.drawRect(inset, inset, button.getWidth() - 2 * inset - 1, button.getHeight() - 2 * inset - 1);
        } finally {
            g.dispose();
        }
    }
}
