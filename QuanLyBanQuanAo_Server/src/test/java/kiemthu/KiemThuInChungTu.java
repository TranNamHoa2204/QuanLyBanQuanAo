package kiemthu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.math.BigDecimal;

import org.junit.Test;

import client.gui.inchungtu.BanInChungTu;
import common.dto.hoadon.DuLieuChiTietHoaDon;
import common.dto.hoadon.DuLieuDayDuHoaDon;
import common.dto.hoadon.DuLieuHoaDon;
import common.dto.phieunhap.DuLieuChiTietPhieuNhap;
import common.dto.phieunhap.DuLieuDayDuPhieuNhap;
import common.dto.phieunhap.DuLieuPhieuNhap;
import common.enums.TrangThaiHoaDon;
import common.enums.TrangThaiPhieuNhap;

public class KiemThuInChungTu {
    @Test
    public void paidInvoiceSpansPagesAndRetainsSnapshot() throws Exception {
        DuLieuDayDuHoaDon detail = new DuLieuDayDuHoaDon();
        DuLieuHoaDon invoice = new DuLieuHoaDon();
        invoice.setMaHD("HD_IN_THU");
        invoice.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN);
        invoice.setTongTien(new BigDecimal("12500000"));
        detail.setHoaDon(invoice);
        for (int i = 0; i < 75; i++) {
            DuLieuChiTietHoaDon row = new DuLieuChiTietHoaDon();
            row.setTenSP("Áo khoác vải dệt chống gió có lớp lót màu xanh navy – mẫu dành cho mùa đông");
            row.setMaBienThe("BT" + i);
            row.setMauSac("Xanh navy");
            row.setKichCo("XL");
            row.setSoLuong(2);
            row.setDonGia(new BigDecimal("250000"));
            row.setThanhTien(new BigDecimal("500000"));
            detail.getChiTiet().add(row);
        }
        BanInChungTu document = BanInChungTu.hoaDon(detail);
        detail.getChiTiet().clear();
        Graphics2D g = new BufferedImage(596, 842, BufferedImage.TYPE_INT_RGB).createGraphics();
        try {
            int page = 0;
            PageFormat a4 = pageFormat();
            while (page < 20 && document.print(g, a4, page) == Printable.PAGE_EXISTS) {
                page++;
            }
            assertTrue(page > 1 && page < 20);
            assertEquals(Printable.NO_SUCH_PAGE, document.print(g, a4, page + 1));
        } finally {
            g.dispose();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void draftReceiptCannotBePrinted() {
        DuLieuDayDuPhieuNhap detail = new DuLieuDayDuPhieuNhap();
        DuLieuPhieuNhap receipt = new DuLieuPhieuNhap();
        receipt.setTrangThai(TrangThaiPhieuNhap.NHAP);
        detail.setPhieuNhap(receipt);
        BanInChungTu.phieuNhap(detail);
    }

    @Test
    public void confirmedReceiptFitsOnePage() throws Exception {
        DuLieuDayDuPhieuNhap detail = new DuLieuDayDuPhieuNhap();
        DuLieuPhieuNhap receipt = new DuLieuPhieuNhap();
        receipt.setMaPhieuNhap("PN_IN_THU");
        receipt.setTrangThai(TrangThaiPhieuNhap.DA_XAC_NHAN);
        receipt.setTongTien(new BigDecimal("360000"));
        detail.setPhieuNhap(receipt);
        DuLieuChiTietPhieuNhap row = new DuLieuChiTietPhieuNhap();
        row.setTenSP("Áo sơ mi trắng");
        row.setKichCo("L");
        row.setSoLuong(3);
        row.setDonGiaNhap(new BigDecimal("120000"));
        row.setThanhTien(receipt.getTongTien());
        detail.getChiTiet().add(row);
        BanInChungTu document = BanInChungTu.phieuNhap(detail);
        Graphics2D g = new BufferedImage(596, 842, BufferedImage.TYPE_INT_RGB).createGraphics();
        try {
            assertEquals(Printable.PAGE_EXISTS, document.print(g, pageFormat(), 0));
            assertEquals(Printable.NO_SUCH_PAGE, document.print(g, pageFormat(), 1));
        } finally {
            g.dispose();
        }
    }

    private static PageFormat pageFormat() {
        Paper paper = new Paper();
        paper.setSize(595.28, 841.89);
        paper.setImageableArea(36, 36, 523.28, 769.89);
        PageFormat format = new PageFormat();
        format.setPaper(paper);
        return format;
    }
}
