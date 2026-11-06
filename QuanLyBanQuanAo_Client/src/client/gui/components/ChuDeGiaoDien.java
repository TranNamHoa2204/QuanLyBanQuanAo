package client.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.TableModel;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.ui.FlatEmptyBorder;
import com.formdev.flatlaf.ui.FlatLineBorder;

/** Màu, khoảng cách và kiểu chữ dùng chung; không chứa nghiệp vụ hoặc I/O. */
public final class ChuDeGiaoDien {
    public static final Color BACKGROUND = new Color(0xECE9D8), SURFACE = Color.WHITE,
            INK = new Color(0x192638), MUTED = new Color(0x475A70), PRIMARY = new Color(0x245FAD),
            NAV = new Color(0x174799), LINE = new Color(0x9C9A88), SOFT = new Color(0xDCE6F7),
            SECONDARY = new Color(0x067B86), TABLE_HEADER = new Color(0xECE9D8);
    private static final Color TABLE_GRID = new Color(0xC1CEDB), TABLE_BORDER = new Color(0x7F9DB9);

    private ChuDeGiaoDien() {
    }

    public static void install() {
        if (!FlatLightLaf.setup()) {
            throw new IllegalStateException("Không khởi tạo được FlatLaf.");
        }
        FlatLaf.setUseNativeWindowDecorations(false);
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        UIManager.put("TitlePane.background", NAV);
        UIManager.put("TitlePane.foreground", Color.WHITE);
        UIManager.put("TitlePane.inactiveBackground", new Color(0x526D97));
        UIManager.put("TitlePane.inactiveForeground", Color.WHITE);
        UIManager.put("TitlePane.buttonHoverBackground", PRIMARY);
        UIManager.put("TitlePane.menuBarEmbedded", false);
        FontUIResource regular = new FontUIResource("Tahoma", Font.PLAIN, 12);
        FontUIResource bold = new FontUIResource(regular.deriveFont(Font.BOLD));
        UIManager.put("defaultFont", regular);
        UIManager.put("Button.font", bold);
        UIManager.put("Menu.font", regular);
        UIManager.put("MenuItem.font", regular);
        UIManager.put("Table.font", regular);
        UIManager.put("TitlePane.font", bold);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Component.borderColor", LINE);
        UIManager.put("Component.focusColor", PRIMARY);
        UIManager.put("Component.accentColor", PRIMARY);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.arc", 0);
        UIManager.put("ButtonUI", GiaoDienNutCoDien.class.getName());
        UIManager.put("TextComponent.arc", 0);
        UIManager.put("Component.minimumHeight", 31);
        UIManager.put("Button.background", BACKGROUND);
        UIManager.put("Button.border", new FlatEmptyBorder(4, 10, 4, 10));
        UIManager.put("Button.foreground", INK);
        UIManager.put("Button.textShiftOffset", 1);
        UIManager.put("TextComponent.margin", new Insets(6, 10, 6, 10));
        UIManager.put("TextField.foreground", INK);
        UIManager.put("PasswordField.foreground", INK);
        UIManager.put("TextArea.foreground", INK);
        UIManager.put("ComboBox.foreground", INK);
        UIManager.put("Component.arrowType", "triangle");
        UIManager.put("ComboBox.buttonStyle", "button");
        UIManager.put("ComboBox.buttonBackground", new Color(0xD8E7FB));
        UIManager.put("ComboBox.buttonEditableBackground", new Color(0xD8E7FB));
        UIManager.put("ComboBox.buttonArrowColor", NAV);
        UIManager.put("ComboBox.buttonSeparatorColor", TABLE_BORDER);
        UIManager.put("PopupMenu.background", BACKGROUND);
        UIManager.put("OptionPane.background", BACKGROUND);
        UIManager.put("OptionPane.messageForeground", INK);
        UIManager.put("MenuBar.background", BACKGROUND);
        UIManager.put("MenuBar.itemMargins", new Insets(2, 6, 2, 6));
        UIManager.put("Menu.background", BACKGROUND);
        UIManager.put("MenuItem.background", BACKGROUND);
        UIManager.put("Menu.foreground", INK);
        UIManager.put("MenuItem.foreground", INK);
        UIManager.put("Menu.selectionBackground", PRIMARY);
        UIManager.put("Menu.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.selectionBackground", PRIMARY);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.selectionArc", 0);
        UIManager.put("TabbedPane.background", BACKGROUND);
        UIManager.put("TabbedPane.foreground", INK);
        UIManager.put("TabbedPane.underlineColor", PRIMARY);
        UIManager.put("TabbedPane.tabType", "card");
        UIManager.put("TabbedPane.cardTabArc", 4);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TabbedPane.tabHeight", 29);
        UIManager.put("Table.rowHeight", 27);
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
        UIManager.put("TableHeader.height", 31);
        UIManager.put("ScrollBar.width", 16);
        UIManager.put("ScrollBar.showButtons", true);
        UIManager.put("ScrollBar.squareButtons", true);
        UIManager.put("ScrollBar.buttonArrowColor", NAV);
        UIManager.put("ScrollBar.thumb", new Color(0xAFCAE9));
        UIManager.put("ScrollBar.thumbInsets", new Insets(1, 1, 1, 1));
        UIManager.put("ScrollBar.thumbArc", 0);
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

    public static JLabel sectionBar(String text) {
        JLabel label = new JLabel(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setPaint(new GradientPaint(0, 0, new Color(0x548DD7), 0, getHeight(), NAV));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } finally {
                    g.dispose();
                }
                super.paintComponent(graphics);
            }
        };
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        label.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
        return label;
    }

    public static JPanel group(String title, LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(TABLE_BORDER), title);
        border.setTitleColor(NAV);
        panel.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(6, 8, 8, 8)));
        return panel;
    }

    public static JPanel card(LayoutManager layout) {
        return card(layout, 20);
    }

    public static JPanel card(LayoutManager layout, int padding) {
        JPanel p = new JPanel(layout);
        p.setBackground(SURFACE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED,
                        Color.WHITE, new Color(0xF5F3E8), LINE, TABLE_BORDER),
                BorderFactory.createEmptyBorder(padding, padding, padding, padding)));
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
        p.add(title(title, 16), BorderLayout.NORTH);
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
        button.setBackground(BACKGROUND);
    }

    public static void table(JTable table) {
        table.setRowHeight(27);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setGridColor(TABLE_GRID);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD, 12f));
        table.getTableHeader().setPreferredSize(new Dimension(0, 31));
        table.setFillsViewportHeight(true);
    }

    public static String money(Number value) {
        return value == null ? "—"
                : NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(value) + " đ";
    }
}
