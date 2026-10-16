package server.repository;

import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import common.enums.TrangThaiHoaDon;

/** Các tổng tiền hóa đơn được đọc riêng để không bị nhân lên khi đọc dòng hàng. */
public final class JpaThongKeDao implements ThongKeDao {
    private final EntityManager em;

    public JpaThongKeDao(EntityManager em) { this.em = em; }

    @Override
    public List<Object[]> paidInvoiceTotals(LocalDateTime tuLuc, LocalDateTime truocLuc, int gioiHan) {
        return em.createQuery("select h.maHD, h.ngayThanhToan, h.tamTinh, h.tienGiam, h.tienThue, h.tongTien "
                + "from HoaDon h where h.trangThai = :paid and h.ngayThanhToan >= :from and h.ngayThanhToan < :until "
                + "order by h.ngayThanhToan asc, h.maHD asc", Object[].class)
                .setParameter("paid", TrangThaiHoaDon.DA_THANH_TOAN)
                .setParameter("from", tuLuc).setParameter("until", truocLuc)
                .setMaxResults(gioiHan).getResultList();
    }

    @Override
    public List<Object[]> paidInvoiceLines(LocalDateTime tuLuc, LocalDateTime truocLuc, int gioiHan) {
        return em.createQuery("select h.maHD, p.maSP, c.tenSPSnapshot, p.tenSP, c.donGia, c.soLuong "
                + "from ChiTietHoaDon c join c.hoaDon h join c.bienThe b join b.sanPham p "
                + "where h.trangThai = :paid and h.ngayThanhToan >= :from and h.ngayThanhToan < :until "
                + "order by h.ngayThanhToan asc, h.maHD asc, p.maSP asc, c.maCTHD asc", Object[].class)
                .setParameter("paid", TrangThaiHoaDon.DA_THANH_TOAN)
                .setParameter("from", tuLuc).setParameter("until", truocLuc)
                .setMaxResults(gioiHan).getResultList();
    }

    @Override
    public long countPaidWithoutPaymentTime() {
        return em.createQuery("select count(h) from HoaDon h where h.trangThai = :paid and h.ngayThanhToan is null", Long.class)
                .setParameter("paid", TrangThaiHoaDon.DA_THANH_TOAN).getSingleResult();
    }
}
