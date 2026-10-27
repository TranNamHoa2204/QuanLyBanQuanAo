package client.ui;

public final class ThuongHieuForm extends javax.swing.JDialog {
    private static final long serialVersionUID = 1L;

    public ThuongHieuForm(java.awt.Window owner, client.controller.QuanLySanPhamController controller) {
        super(owner, "Thương hiệu", ModalityType.APPLICATION_MODAL);
        DanhMucPanel panel = new DanhMucPanel(controller, true);
        add(panel);
        setSize(850, 600);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                panel.initialize();
            }
        });
    }
}
