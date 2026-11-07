package client.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

/** Các biểu tượng 18×18 vẽ bằng Java2D theo thanh công cụ Windows cổ điển. */
public enum BieuTuongCoDien implements Icon {
    TRANG_CHU, SAN_PHAM, TIM_KIEM, THU_MUC, NHAN_VIEN, KHACH_HANG, KHO, KHUYEN_MAI,
    BAN_HANG, HOA_DON, BAO_CAO, IN, KHOA, THOAT, XAC_NHAN, LAM_MOI, THEM;

    private static final Color BLUE = new Color(0x245FAD), SKY = new Color(0xDDE9F7),
            GOLD = new Color(0xFFE191), AMBER = new Color(0xA96E16), GREEN = new Color(0x398C36),
            RED = new Color(0xC9382E), SKIN = new Color(0xE8BA83);

    @Override public int getIconWidth() { return 18; }
    @Override public int getIconHeight() { return 18; }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(1.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(BLUE);
            switch (this) {
            case TRANG_CHU:
                box(g, 3, 7, 12, 9, SKY);
                polygon(g, new int[] { 1, 9, 17 }, new int[] { 8, 1, 8 }, BLUE, BLUE);
                g.fillRect(8, 11, 3, 5);
                break;
            case SAN_PHAM:
                polygon(g, new int[] { 1, 9, 17, 17, 9, 1 }, new int[] { 5, 1, 5, 13, 17, 13 }, GOLD, AMBER);
                g.drawPolyline(new int[] { 1, 9, 17 }, new int[] { 5, 9, 5 }, 3);
                g.drawLine(9, 9, 9, 17);
                g.drawLine(5, 3, 13, 7);
                break;
            case TIM_KIEM:
                g.setColor(SKY);
                g.fillOval(2, 1, 10, 10);
                g.setColor(BLUE);
                g.drawOval(2, 1, 10, 10);
                g.setStroke(new BasicStroke(3));
                g.drawLine(11, 11, 16, 16);
                break;
            case THU_MUC:
                g.setColor(AMBER);
                g.fillRect(2, 2, 7, 4);
                g.fillRoundRect(1, 4, 16, 12, 2, 2);
                g.setColor(GOLD);
                g.fillRoundRect(2, 6, 14, 9, 2, 2);
                break;
            case NHAN_VIEN:
                g.setColor(SKIN);
                g.fillOval(6, 1, 7, 7);
                g.setColor(BLUE);
                g.fillRoundRect(3, 9, 13, 8, 5, 5);
                break;
            case KHACH_HANG:
                box(g, 1, 2, 16, 14, SKY);
                g.setColor(SKIN);
                g.fillOval(4, 4, 5, 5);
                g.setColor(GREEN);
                g.fillRoundRect(3, 10, 7, 4, 3, 3);
                g.setColor(BLUE);
                g.drawLine(12, 6, 15, 6);
                g.drawLine(12, 10, 15, 10);
                break;
            case KHO:
                box(g, 2, 6, 14, 10, SKY);
                polygon(g, new int[] { 1, 9, 17 }, new int[] { 6, 1, 6 }, SKY, BLUE);
                g.drawLine(2, 11, 16, 11);
                g.setColor(AMBER);
                g.fillRect(4, 8, 4, 3);
                g.fillRect(10, 8, 4, 3);
                g.fillRect(5, 13, 7, 3);
                break;
            case KHUYEN_MAI:
                polygon(g, new int[] { 1, 13, 17, 13, 1 }, new int[] { 3, 3, 9, 15, 15 }, GOLD, AMBER);
                g.setColor(RED);
                g.drawOval(4, 5, 3, 3);
                g.drawOval(9, 10, 3, 3);
                g.drawLine(5, 13, 11, 5);
                break;
            case BAN_HANG:
                box(g, 2, 8, 14, 9, SKY);
                box(g, 4, 1, 10, 6, SKY);
                g.drawLine(9, 7, 9, 8);
                g.drawLine(5, 11, 12, 11);
                g.drawLine(5, 14, 12, 14);
                g.setColor(GREEN);
                g.fillRect(5, 2, 8, 3);
                break;
            case HOA_DON:
            case BAO_CAO:
                box(g, 3, 1, 12, 16, Color.WHITE);
                if (this == HOA_DON) {
                    g.drawLine(5, 5, 12, 5);
                    g.drawLine(5, 8, 12, 8);
                    g.drawLine(5, 11, 9, 11);
                    g.setColor(GREEN);
                    g.fillOval(10, 11, 7, 7);
                } else {
                    g.fillRect(5, 10, 2, 5);
                    g.setColor(AMBER);
                    g.fillRect(8, 7, 2, 8);
                    g.setColor(GREEN);
                    g.fillRect(11, 4, 2, 11);
                }
                break;
            case IN:
                box(g, 1, 6, 16, 8, new Color(0xD4DCE5));
                box(g, 4, 1, 10, 5, Color.WHITE);
                box(g, 4, 11, 10, 6, Color.WHITE);
                break;
            case KHOA:
                g.drawRoundRect(5, 1, 8, 11, 6, 6);
                g.setColor(GOLD);
                g.fillRoundRect(2, 7, 14, 10, 2, 2);
                g.setColor(AMBER);
                g.drawRoundRect(2, 7, 14, 10, 2, 2);
                g.drawLine(9, 11, 9, 14);
                break;
            case THOAT:
                g.setColor(RED);
                g.fillRoundRect(1, 1, 16, 16, 3, 3);
                g.setColor(Color.WHITE);
                g.drawLine(5, 5, 13, 13);
                g.drawLine(5, 13, 13, 5);
                break;
            case XAC_NHAN:
                g.setColor(GREEN);
                g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolyline(new int[] { 2, 7, 16 }, new int[] { 9, 14, 3 }, 3);
                break;
            case LAM_MOI:
                g.setColor(GREEN);
                g.drawArc(2, 2, 13, 13, 40, 280);
                g.fillPolygon(new int[] { 11, 17, 15 }, new int[] { 2, 2, 8 }, 3);
                break;
            case THEM:
                g.setColor(GREEN);
                g.fillOval(1, 1, 16, 16);
                g.setColor(Color.WHITE);
                g.drawLine(9, 5, 9, 13);
                g.drawLine(5, 9, 13, 9);
                break;
            }
        } finally {
            g.dispose();
        }
    }

    private static void box(Graphics2D g, int x, int y, int width, int height, Color fill) {
        g.setColor(fill);
        g.fillRect(x, y, width, height);
        g.setColor(BLUE);
        g.drawRect(x, y, width, height);
    }

    private static void polygon(Graphics2D g, int[] x, int[] y, Color fill, Color outline) {
        g.setColor(fill);
        g.fillPolygon(x, y, x.length);
        g.setColor(outline);
        g.drawPolygon(x, y, x.length);
    }
}
