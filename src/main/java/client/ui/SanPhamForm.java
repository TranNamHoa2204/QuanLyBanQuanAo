package client.ui;

import java.awt.event.*;
import javax.swing.*;
import client.controller.SanPhamController;
import common.enums.VaiTro;

public final class SanPhamForm extends JDialog {
    private static final long serialVersionUID = 1L;
    public SanPhamForm(JFrame owner, SanPhamController controller, VaiTro role) {
        super(owner, "Tra cứu sản phẩm", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        SanPhamPanel panel = new SanPhamPanel(controller, role);
        add(panel); pack(); setLocationRelativeTo(owner);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { panel.initialize(); }
        });
    }
}
