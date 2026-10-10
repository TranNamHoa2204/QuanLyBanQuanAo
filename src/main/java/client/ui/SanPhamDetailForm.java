package client.ui;

import java.awt.Window;
import javax.swing.JDialog;
import common.dto.SanPhamDetailDTO;

public final class SanPhamDetailForm extends JDialog {
    private static final long serialVersionUID = 1L;
    public SanPhamDetailForm(Window owner, SanPhamDetailDTO detail) {
        super(owner, "Chi tiết sản phẩm", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        add(new SanPhamDetailPanel(detail)); pack(); setLocationRelativeTo(owner);
    }
}
