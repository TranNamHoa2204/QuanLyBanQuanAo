package client.ui;

/** Cửa sổ tùy chọn; màn hình chính dùng tab DanhMucPanel cùng chức năng. */
public final class LoaiSanPhamForm extends javax.swing.JDialog {
    private static final long serialVersionUID = 1L;

    public LoaiSanPhamForm(java.awt.Window owner, client.controller.QuanLySanPhamController controller) {
        super(owner, "Loại sản phẩm", ModalityType.APPLICATION_MODAL);
        DanhMucPanel panel = new DanhMucPanel(controller, false);
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
