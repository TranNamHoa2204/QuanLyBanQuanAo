package client.gui.components;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.JTextComponent;

/** Gợi ý trong ô rỗng chỉ được vẽ lên màn hình, không đi vào dữ liệu nhập. */
public final class GiaoDienONhapCoDien extends BasicTextFieldUI {
    static final String HINT = "quanao.fieldHint";

    public static ComponentUI createUI(JComponent component) {
        return new GiaoDienONhapCoDien();
    }

    @Override
    protected void paintSafely(Graphics graphics) {
        super.paintSafely(graphics);
        paintHint(graphics, getComponent());
    }

    private static void paintHint(Graphics graphics, JTextComponent field) {
        Object hint = field.getClientProperty(HINT);
        if (hint == null || field.getDocument().getLength() != 0 || field.isFocusOwner()) {
            return;
        }
        Insets insets = field.getInsets();
        Graphics g = graphics.create();
        try {
            g.clipRect(insets.left, insets.top, field.getWidth() - insets.left - insets.right,
                    field.getHeight() - insets.top - insets.bottom);
            g.setFont(field.getFont());
            g.setColor(ChuDeGiaoDien.MUTED);
            FontMetrics metrics = g.getFontMetrics();
            int y = (field.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(hint.toString(), insets.left + 1, y);
        } finally {
            g.dispose();
        }
    }

    public static final class MatKhau extends BasicPasswordFieldUI {
        public static ComponentUI createUI(JComponent component) {
            return new MatKhau();
        }

        @Override
        protected void paintSafely(Graphics graphics) {
            super.paintSafely(graphics);
            paintHint(graphics, getComponent());
        }
    }
}
