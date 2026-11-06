package client.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.TableModel;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.ui.FlatLineBorder;

/** Màu, khoảng cách và kiểu chữ dùng chung; không chứa nghiệp vụ hoặc I/O. */
public final class ChuDeGiaoDien {
    public static final Color BACKGROUND = new Color(0xEFF4FA), SURFACE = Color.WHITE,
            INK = new Color(0x142542), MUTED = new Color(0x53647B), PRIMARY = new Color(0x145FC4),
            NAV = new Color(0x132C52), LINE = new Color(0xDCE5F1), SOFT = new Color(0xE8F1FF),
            SECONDARY = new Color(0x067B86), TABLE_HEADER = new Color(0xEFF4FC);
    private static final int CARD_ARC = 16;
    private static final Color TABLE_GRID = new Color(0xBCCBDB), TABLE_BORDER = new Color(0x859CB6);

    private ChuDeGiaoDien() {
    }

    public static void install() {
        if (!FlatLightLaf.setup()) {
            throw new IllegalStateException("Không khởi tạo được FlatLaf.");
        }
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Component.borderColor", LINE);
        UIManager.put("Component.focusColor", PRIMARY);
        UIManager.put("Component.accentColor", PRIMARY);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Component.minimumHeight", 36);
        UIManager.put("Button.minimumHeight", 34);
        UIManager.put("Button.background", new Color(0xF5F8FD));
        UIManager.put("Button.foreground", INK);
        UIManager.put("Button.borderColor", LINE);
        UIManager.put("Button.hoverBorderColor", PRIMARY);
        UIManager.put("TextComponent.margin", new Insets(6, 10, 6, 10));
        UIManager.put("Button.margin", new Insets(5, 12, 5, 12));
        UIManager.put("TextField.foreground", INK);
        UIManager.put("PasswordField.foreground", INK);
        UIManager.put("TextArea.foreground", INK);
        UIManager.put("ComboBox.foreground", INK);
        UIManager.put("PopupMenu.background", SURFACE);
        UIManager.put("OptionPane.background", BACKGROUND);
        UIManager.put("OptionPane.messageForeground", INK);
        UIManager.put("MenuBar.background", SURFACE);
        UIManager.put("Menu.foreground", INK);
        UIManager.put("MenuItem.foreground", INK);
        UIManager.put("MenuItem.selectionBackground", SOFT);
        UIManager.put("MenuItem.selectionForeground", NAV);
        UIManager.put("MenuItem.selectionArc", 8);
        UIManager.put("TabbedPane.background", BACKGROUND);
        UIManager.put("TabbedPane.foreground", INK);
        UIManager.put("TabbedPane.underlineColor", PRIMARY);
        UIManager.put("TabbedPane.tabHeight", 36);
        UIManager.put("Table.rowHeight", 38);
        UIManager.put("Table.background", new ColorUIResource(SURFACE));
        UIManager.put("Table.foreground", INK);
        UIManager.put("Table.alternateRowColor", new Color(0xEFF5FB));
        UIManager.put("Table.selectionBackground", new Color(0x3067B9));
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.selectionInactiveBackground", new Color(0x456C9E));
        UIManager.put("Table.selectionInactiveForeground", Color.WHITE);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", true);
        UIManager.put("Table.gridColor", TABLE_GRID);
        UIManager.put("TableHeader.background", TABLE_HEADER);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.bottomSeparatorColor", TABLE_GRID);
        UIManager.put("TableHeader.height", 42);
        UIManager.put("ScrollBar.width", 10);
    }

    public static JLabel title(String text, float size) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, size));
        return label;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        return l;
    }

    public static JPanel card(LayoutManager layout) {
        return card(layout, 20);
    }

    public static JPanel card(LayoutManager layout, int padding) {
        JPanel p = new JPanel(layout) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(getBackground());
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), CARD_ARC, CARD_ARC);
                } finally {
                    g.dispose();
                }
            }
        };
        p.setOpaque(false);
        p.setBackground(SURFACE);
        p.setBorder(new FlatLineBorder(new Insets(padding, padding, padding, padding), LINE, 1, CARD_ARC));
        return p;
    }

    public static JTable tableWithHorizontalScroll(TableModel model, int... widths) {
        JTable table = new JTable(model) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getParent() instanceof JViewport
                        ? getPreferredSize().width <= getParent().getWidth()
                        : super.getScrollableTracksViewportWidth();
            }

            @Override
            public String getToolTipText(MouseEvent event) {
                int row = rowAtPoint(event.getPoint());
                int column = columnAtPoint(event.getPoint());
                if (row >= 0 && column >= 0) {
                    Component cell = prepareRenderer(getCellRenderer(row, column), row, column);
                    if (cell instanceof JLabel && cell.getPreferredSize().width > getCellRect(row, column, false).width) {
                        return ((JLabel) cell).getText();
                    }
                }
                return super.getToolTipText(event);
            }
        };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        return table;
    }

    public static JScrollPane tableScroll(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        pane.setColumnHeaderView(table.getTableHeader());
        pane.getViewport().setBackground(SURFACE);
        pane.setBorder(new FlatLineBorder(new Insets(2, 2, 2, 2), TABLE_BORDER, 2, 0));
        return pane;
    }

    public static JPanel field(String label, JComponent input) {
        JPanel p = new JPanel(new BorderLayout(0, 7));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setLabelFor(input);
        l.setForeground(MUTED);
        p.add(l, BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    public static JPanel heading(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(title(title, 24), BorderLayout.NORTH);
        p.add(muted(subtitle), BorderLayout.CENTER);
        return p;
    }

    public static void primary(JButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
    }

    public static void danger(JButton button) {
        button.setForeground(new Color(0xB54141));
        button.setBackground(new Color(0xFFF2F0));
    }

    public static void table(JTable table) {
        table.setRowHeight(38);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setGridColor(TABLE_GRID);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD, 12f));
        table.getTableHeader().setPreferredSize(new Dimension(0, 42));
        table.setFillsViewportHeight(true);
    }

    public static String money(Number value) {
        return value == null ? "—"
                : NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(value) + " đ";
    }
}
