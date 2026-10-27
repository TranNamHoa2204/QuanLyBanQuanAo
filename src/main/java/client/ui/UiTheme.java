package client.ui;

import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.*;
import com.formdev.flatlaf.FlatLightLaf;

/** Màu, khoảng cách và kiểu chữ dùng chung; không chứa nghiệp vụ hoặc I/O. */
public final class UiTheme {
    public static final Color BACKGROUND = new Color(0xF4F6F5), SURFACE = Color.WHITE, INK = new Color(0x192B27),
            MUTED = new Color(0x64756F), PRIMARY = new Color(0x176B50), NAV = new Color(0x142F26),
            LINE = new Color(0xDFE7E2), SOFT = new Color(0xE9F4EE);

    private UiTheme() {
    }

    public static void install() {
        if (!FlatLightLaf.setup()) {
            throw new IllegalStateException("Không khởi tạo được FlatLaf.");
        }
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Component.focusColor", PRIMARY);
        UIManager.put("Component.accentColor", PRIMARY);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Component.minimumHeight", 36);
        UIManager.put("Button.minimumHeight", 38);
        UIManager.put("TextComponent.margin", new Insets(6, 10, 6, 10));
        UIManager.put("Button.margin", new Insets(8, 16, 8, 16));
        UIManager.put("Table.rowHeight", 38);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.selectionBackground", SOFT);
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("Table.alternateRowColor", new Color(0xF8FAF9));
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor", LINE);
        UIManager.put("TableHeader.background", new Color(0xEEF3F0));
        UIManager.put("TableHeader.foreground", MUTED);
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
        JPanel p = new JPanel(layout);
        p.setBackground(SURFACE);
        p.setBorder(new CompoundBorder(new LineBorder(LINE, 1, true), new EmptyBorder(20, 20, 20, 20)));
        return p;
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
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.setOpaque(false);
        p.add(title(title, 24));
        p.add(muted(subtitle));
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
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(LINE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD, 12f));
        table.getTableHeader().setPreferredSize(new Dimension(0, 42));
        table.setFillsViewportHeight(true);
    }

    public static String money(Number value) {
        return value == null ? "—" : NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(value) + " đ";
    }
}
