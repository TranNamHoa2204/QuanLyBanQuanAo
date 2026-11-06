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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

/** Màu, khoảng cách và kiểu chữ dùng chung; không chứa nghiệp vụ hoặc I/O. */
public final class ChuDeGiaoDien {
    public static final Color BACKGROUND = new Color(0xECE9D8), SURFACE = Color.WHITE,
            INK = new Color(0x192638), MUTED = new Color(0x475A70), PRIMARY = new Color(0x245FAD),
            NAV = new Color(0x174799), LINE = new Color(0x9C9A88), SOFT = new Color(0xDCE6F7),
            SECONDARY = new Color(0x067B86), TABLE_HEADER = new Color(0xECE9D8);
    private static final Color TABLE_GRID = new Color(0xC1CEDB);
    static final Color TABLE_BORDER = new Color(0x7F9DB9);

    private ChuDeGiaoDien() {
    }

    public static void install() {
        try {
            MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException failure) {
            throw new IllegalStateException("Không khởi tạo được giao diện Swing cổ điển.", failure);
        }
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        UIManager.put("activeCaption", NAV);
        UIManager.put("activeCaptionText", Color.WHITE);
        UIManager.put("activeCaptionBorder", NAV.darker());
        UIManager.put("inactiveCaption", new Color(0x526D97));
        UIManager.put("inactiveCaptionText", Color.WHITE);
        UIManager.put("inactiveCaptionBorder", LINE);
        FontUIResource regular = new FontUIResource("Tahoma", Font.PLAIN, 12);
        FontUIResource bold = new FontUIResource(regular.deriveFont(Font.BOLD));
        for (String component : new String[] { "Label", "Menu", "MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem",
                "TextField", "PasswordField", "FormattedTextField", "TextArea", "ComboBox", "CheckBox", "RadioButton",
                "List", "Table", "TabbedPane", "ToolTip", "OptionPane", "TitledBorder" }) {
            UIManager.put(component + ".font", regular);
        }
        UIManager.put("Button.font", bold);
        UIManager.put("TableHeader.font", bold);
        UIManager.put("InternalFrame.titleFont", bold);
        UIManager.put("MenuItem.acceleratorFont", regular);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Viewport.background", SURFACE);
        UIManager.put("Label.foreground", INK);
        UIManager.put("ButtonUI", GiaoDienNutCoDien.class.getName());
        UIManager.put("Button.background", BACKGROUND);
        UIManager.put("Button.border", new BorderUIResource.EmptyBorderUIResource(4, 10, 4, 10));
        UIManager.put("Button.foreground", INK);
        UIManager.put("Button.textShiftOffset", 1);
        UIManager.put("TextFieldUI", GiaoDienONhapCoDien.class.getName());
        UIManager.put("PasswordFieldUI", GiaoDienONhapCoDien.MatKhau.class.getName());
        BorderUIResource.CompoundBorderUIResource input = new BorderUIResource.CompoundBorderUIResource(
                BorderFactory.createLineBorder(TABLE_BORDER), BorderFactory.createEmptyBorder(4, 6, 4, 6));
        for (String field : new String[] { "TextField", "PasswordField", "FormattedTextField" }) {
            UIManager.put(field + ".border", input);
            UIManager.put(field + ".background", new ColorUIResource(SURFACE));
            UIManager.put(field + ".foreground", INK);
        }
        UIManager.put("ComboBoxUI", GiaoDienHopChonCoDien.class.getName());
        UIManager.put("ComboBox.border", new BorderUIResource.LineBorderUIResource(TABLE_BORDER));
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.foreground", INK);
        UIManager.put("ComboBox.selectionBackground", PRIMARY);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("PopupMenu.background", BACKGROUND);
        UIManager.put("OptionPane.background", BACKGROUND);
        UIManager.put("OptionPane.messageForeground", INK);
        UIManager.put("MenuBar.background", BACKGROUND);
        UIManager.put("Menu.background", BACKGROUND);
        UIManager.put("MenuItem.background", BACKGROUND);
        UIManager.put("Menu.foreground", INK);
        UIManager.put("MenuItem.foreground", INK);
        UIManager.put("Menu.margin", new Insets(2, 7, 2, 7));
        UIManager.put("MenuItem.margin", new Insets(3, 5, 3, 8));
        UIManager.put("Menu.selectionBackground", PRIMARY);
        UIManager.put("Menu.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.selectionBackground", PRIMARY);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("TabbedPane.background", BACKGROUND);
        UIManager.put("TabbedPane.foreground", INK);
        UIManager.put("TabbedPane.selected", SOFT);
        UIManager.put("TabbedPane.tabInsets", new Insets(4, 9, 4, 9));
        UIManager.put("Table.background", new ColorUIResource(SURFACE));
        UIManager.put("Table.foreground", INK);
        UIManager.put("Table.alternateRowColor", new Color(0xEFF5FB));
        UIManager.put("Table.selectionBackground", new Color(0x3067B9));
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.gridColor", TABLE_GRID);
        UIManager.put("TableHeader.background", TABLE_HEADER);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("ScrollBarUI", GiaoDienThanhCuonCoDien.class.getName());
        UIManager.put("ScrollBar.width", 16);
        UIManager.put("ScrollBar.track", new Color(0xF5F4EF));
    }

    static JButton arrow(int direction, Dimension size) {
        JButton button = new BasicArrowButton(direction, SOFT, TABLE_BORDER, NAV, Color.WHITE);
        button.setBorder(BorderFactory.createLineBorder(TABLE_BORDER));
        button.setPreferredSize(size);
        return button;
    }

    public static void hint(JTextComponent field, String text) {
        field.putClientProperty(GiaoDienONhapCoDien.HINT, text);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) { field.repaint(); }
            @Override public void focusLost(FocusEvent event) { field.repaint(); }
        });
    }

    public static JCheckBox showPasswords(JPasswordField... fields) {
        char[] original = new char[fields.length];
        for (int i = 0; i < fields.length; i++) {
            original[i] = fields[i].getEchoChar();
        }
        JCheckBox show = new JCheckBox("Hiện mật khẩu");
        show.setOpaque(false);
        show.addActionListener(event -> {
            for (int i = 0; i < fields.length; i++) {
                fields[i].setEchoChar(show.isSelected() ? (char) 0 : original[i]);
            }
        });
        return show;
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
        pane.setBorder(BorderFactory.createLineBorder(TABLE_BORDER, 2));
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
