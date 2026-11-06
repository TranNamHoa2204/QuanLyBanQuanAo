package client.gui.components;

import java.math.BigDecimal;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public final class TienIchBangSanPham {
    private TienIchBangSanPham() {
    }

    public static DefaultTableModel model(String[] columns, Class<?>[] types) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return types[column];
            }
        };
    }

    public static void configure(JTable table) {
        ChuDeGiaoDien.table(table);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(BigDecimal.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object value) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                setText(value == null ? "" : money((Number) value));
            }
        });
    }

    public static void onOpenRow(JTable table, Runnable open) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 2
                        && table.isEnabled() && table.rowAtPoint(event.getPoint()) == table.getSelectedRow()
                        && table.getSelectedRow() >= 0) {
                    open.run();
                }
            }
        });
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open-row");
        table.getActionMap().put("open-row", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent event) {
                if (table.isEnabled() && table.getSelectedRow() >= 0) {
                    open.run();
                }
            }
        });
    }

    public static String money(Number value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        BigDecimal amount = value instanceof BigDecimal ? (BigDecimal) value
                : new BigDecimal(value.toString());
        int digits = amount.stripTrailingZeros().scale() > 0 ? 2 : 0;
        format.setMinimumFractionDigits(digits);
        format.setMaximumFractionDigits(digits);
        return format.format(amount);
    }
}
