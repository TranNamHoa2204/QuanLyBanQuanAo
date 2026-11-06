package client.gui.thongke;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JPanel;

import client.gui.components.ChuDeGiaoDien;
import common.dto.thongke.DuLieuThongKeDoanhThu;
import common.dto.thongke.DuLieuThongKeDoanhThuDong;

/** Biểu đồ đọc số liệu báo cáo đã tính trên máy chủ. */
public final class BieuDoThongKe {
    private static final Color TAX_COLOR = new Color(0x5CB7C2);
    private static final Locale VIETNAMESE = new Locale("vi", "VN");

    private BieuDoThongKe() {
    }

    static final class TheoNhom extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int LEFT = 46, RIGHT = 10, TOP = 10, BOTTOM = 23;
        private final boolean revenue;
        private List<DuLieuThongKeDoanhThuDong> rows = Collections.emptyList();
        private boolean product;
        private double maximum;

        TheoNhom(boolean revenue) {
            this.revenue = revenue;
            setOpaque(false);
            setToolTipText("");
            getAccessibleContext().setAccessibleName(revenue ? "Biểu đồ doanh thu" : "Biểu đồ số hóa đơn");
        }

        void setData(List<DuLieuThongKeDoanhThuDong> items, boolean byProduct) {
            rows = new ArrayList<>(items);
            product = byProduct;
            maximum = 0;
            for (DuLieuThongKeDoanhThuDong row : rows) {
                maximum = Math.max(maximum, amount(row).doubleValue());
            }
            repaint();
        }

        private Number amount(DuLieuThongKeDoanhThuDong row) {
            if (!revenue) {
                return row.getSoHoaDon();
            }
            return row.getDoanhThuSauGiam() == null ? 0 : row.getDoanhThuSauGiam();
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            int width = getWidth() - LEFT - RIGHT;
            if (rows.isEmpty() || width <= 0 || event.getX() < LEFT || event.getX() >= LEFT + width
                    || event.getY() < TOP || event.getY() > getHeight() - BOTTOM) {
                return null;
            }
            int index = (event.getX() - LEFT) * rows.size() / width;
            DuLieuThongKeDoanhThuDong row = rows.get(index);
            return label(row) + " · " + (revenue ? ChuDeGiaoDien.money(amount(row))
                    : NumberFormat.getIntegerInstance(VIETNAMESE).format(amount(row)) + " hóa đơn");
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = prepare(graphics);
            try {
                int width = getWidth() - LEFT - RIGHT;
                int bottom = getHeight() - BOTTOM;
                if (width < 32 || bottom <= TOP) {
                    return;
                }
                if (rows.isEmpty()) {
                    empty(g, getWidth(), getHeight(), "Chưa có số liệu");
                    return;
                }
                // Số hóa đơn là số nguyên nên các vạch trục luôn dùng số nguyên.
                double ceiling = revenue ? Math.max(1, maximum) : Math.max(3, Math.ceil(maximum / 3) * 3);
                for (int tick = 0; tick <= 3; tick++) {
                    int y = bottom - (bottom - TOP) * tick / 3;
                    g.setColor(ChuDeGiaoDien.LINE);
                    g.drawLine(LEFT, y, LEFT + width, y);
                    g.setColor(ChuDeGiaoDien.MUTED);
                    String text = compact(ceiling * tick / 3);
                    g.drawString(text, LEFT - g.getFontMetrics().stringWidth(text) - 6, y + 4);
                }
                double slot = (double) width / rows.size();
                boolean line = revenue && !product;
                Color seriesColor = revenue ? ChuDeGiaoDien.PRIMARY : ChuDeGiaoDien.SECONDARY;
                Path2D.Double path = new Path2D.Double();
                for (int i = 0; i < rows.size(); i++) {
                    double amount = amount(rows.get(i)).doubleValue();
                    int x = LEFT + (int) ((i + 0.5) * slot);
                    int y = bottom - (int) Math.round(amount / ceiling * (bottom - TOP));
                    g.setColor(seriesColor);
                    if (line) {
                        if (i == 0) {
                            path.moveTo(x, y);
                        } else {
                            path.lineTo(x, y);
                        }
                        int radius = slot >= 16 ? 3 : 2;
                        g.fillOval(x - radius, y - radius, 2 * radius, 2 * radius);
                    } else if (amount > 0) {
                        int bar = Math.max(1, Math.min(38, (int) (slot * 0.65)));
                        g.fillRoundRect(x - bar / 2, Math.min(bottom - 1, y), bar,
                                Math.max(1, bottom - y), 5, 5);
                    }
                }
                if (line) {
                    g.setColor(seriesColor);
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(path);
                }
                drawLabels(g, slot, bottom);
            } finally {
                g.dispose();
            }
        }

        private void drawLabels(Graphics2D g, double slot, int bottom) {
            g.setColor(ChuDeGiaoDien.MUTED);
            int step = Math.max(1, (int) Math.ceil(78 / slot));
            for (int i = 0; i < rows.size(); i += step) {
                String label = label(rows.get(i));
                int center = LEFT + (int) ((i + 0.5) * slot);
                int width = g.getFontMetrics().stringWidth(label);
                int x = Math.max(LEFT, Math.min(center - width / 2, getWidth() - RIGHT - width));
                g.drawString(label, x, bottom + 16);
            }
        }

        private String label(DuLieuThongKeDoanhThuDong row) {
            String value = product ? row.getMaSP() : row.getNhan();
            return value == null ? "—" : value;
        }
    }

    static final class CoCau extends JPanel {
        private static final long serialVersionUID = 1L;
        private DuLieuThongKeDoanhThu data;

        CoCau() {
            setOpaque(false);
            getAccessibleContext().setAccessibleName("Cơ cấu tổng tiền khách trả");
        }

        void setData(DuLieuThongKeDoanhThu value) {
            data = value;
            setToolTipText("Doanh thu sau giảm: " + ChuDeGiaoDien.money(value.getDoanhThuSauGiam())
                    + " · Thuế: " + ChuDeGiaoDien.money(value.getTienThue()));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = prepare(graphics);
            try {
                if (data == null || data.getTongKhachTra() == null || data.getTongKhachTra().signum() == 0) {
                    empty(g, getWidth(), getHeight(), data == null ? "Chưa có số liệu" : "Chưa phát sinh tiền thu");
                    return;
                }
                int size = Math.min(getWidth() - 14, getHeight() - 44);
                if (size < 50) {
                    return;
                }
                int x = (getWidth() - size) / 2, y = 2;
                double paid = data.getTongKhachTra().doubleValue();
                double net = data.getDoanhThuSauGiam().doubleValue();
                double tax = data.getTienThue().doubleValue();
                double sweep = 360 * net / paid;
                g.setColor(TAX_COLOR);
                g.fillOval(x, y, size, size);
                g.setColor(ChuDeGiaoDien.PRIMARY);
                g.fill(new Arc2D.Double(x, y, size, size, 90, -sweep, Arc2D.PIE));
                int ring = Math.max(9, size / 8);
                g.setColor(ChuDeGiaoDien.SURFACE);
                g.fillOval(x + ring, y + ring, size - 2 * ring, size - 2 * ring);
                g.setColor(ChuDeGiaoDien.MUTED);
                center(g, "Tổng thu", getWidth(), y + size / 2 - 3);
                g.setColor(ChuDeGiaoDien.INK);
                g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
                center(g, compact(paid) + " đ", getWidth(), y + size / 2 + 15);
                g.setFont(getFont().deriveFont(11f));
                legend(g, ChuDeGiaoDien.PRIMARY, "Doanh thu sau giảm", net, size + 17);
                legend(g, TAX_COLOR, "Thuế", tax, size + 35);
            } finally {
                g.dispose();
            }
        }

        private void legend(Graphics2D g, Color color, String label, double amount, int y) {
            g.setColor(color);
            g.fillRoundRect(4, y - 9, 9, 9, 3, 3);
            g.setColor(ChuDeGiaoDien.MUTED);
            g.drawString(label, 19, y);
            String text = compact(amount) + " đ";
            g.setColor(ChuDeGiaoDien.INK);
            g.drawString(text, getWidth() - 4 - g.getFontMetrics().stringWidth(text), y);
        }
    }

    private static Graphics2D prepare(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(g.getFont().deriveFont(11f));
        return g;
    }

    private static void empty(Graphics2D g, int width, int height, String text) {
        g.setColor(ChuDeGiaoDien.MUTED);
        center(g, text, width, height / 2);
    }

    private static void center(Graphics2D g, String text, int width, int baseline) {
        g.drawString(text, (width - g.getFontMetrics().stringWidth(text)) / 2, baseline);
    }

    private static String compact(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(VIETNAMESE);
        format.setMaximumFractionDigits(1);
        if (value >= 1_000_000_000) {
            return format.format(value / 1_000_000_000) + " tỷ";
        }
        if (value >= 1_000_000) {
            return format.format(value / 1_000_000) + " tr";
        }
        if (value >= 1_000) {
            return format.format(value / 1_000) + "k";
        }
        return format.format(value);
    }
}
