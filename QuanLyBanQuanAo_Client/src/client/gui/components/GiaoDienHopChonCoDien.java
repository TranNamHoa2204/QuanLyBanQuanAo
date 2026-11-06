package client.gui.components;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;

public final class GiaoDienHopChonCoDien extends BasicComboBoxUI {
    public static ComponentUI createUI(JComponent component) {
        return new GiaoDienHopChonCoDien();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        squareButton = false;
    }

    @Override
    protected JButton createArrowButton() {
        JButton button = ChuDeGiaoDien.arrow(SwingConstants.SOUTH, new Dimension(19, 23));
        button.setName("ComboBox.arrowButton");
        button.getAccessibleContext().setAccessibleName("Mở danh sách");
        return button;
    }
}
