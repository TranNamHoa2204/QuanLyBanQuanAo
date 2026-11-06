package client.gui.sanpham;

import java.awt.Dialog.ModalityType;
import java.awt.Window;

import javax.swing.JDialog;

import common.dto.sanpham.DuLieuDayDuSanPham;

public final class HopThoaiChiTietSanPham extends JDialog {
    private static final long serialVersionUID = 1L;

    public HopThoaiChiTietSanPham(Window owner, DuLieuDayDuSanPham detail) {
        super(owner, "Chi tiết sản phẩm", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        add(new KhungChiTietSanPham(detail));
        pack();
        setLocationRelativeTo(owner);
    }
}
