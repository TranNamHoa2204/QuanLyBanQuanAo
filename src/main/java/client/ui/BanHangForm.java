package client.ui;

import java.awt.Frame;
import javax.swing.JDialog;
import client.api.AuthApi;

public final class BanHangForm extends JDialog {
    private static final long serialVersionUID = 1L;
    public BanHangForm(Frame owner, AuthApi api) {
        super(owner, "Bán hàng / Hóa đơn", true);
        BanHangPanel panel = new BanHangPanel(api.invoices(), api.products(), api.management());
        add(new javax.swing.JScrollPane(panel)); setSize(1100, 850);
        setMinimumSize(new java.awt.Dimension(950, 550)); setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); panel.loadReferenceData();
    }
}
