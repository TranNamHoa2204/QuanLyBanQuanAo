package server.repository;

import java.util.List;
import javax.persistence.EntityManager;
import server.entity.ChiTietHoaDon;

public final class JpaChiTietHoaDonDao extends JpaGenericDao<ChiTietHoaDon, Long> implements ChiTietHoaDonDao {
    public JpaChiTietHoaDonDao(EntityManager em) {
        super(em, ChiTietHoaDon.class);
    }

    public List<ChiTietHoaDon> byInvoice(String id) {
        return em.createQuery(
                "select c from ChiTietHoaDon c where c.hoaDon.maHD = :id" + " order by c.bienThe.maBienThe",
                ChiTietHoaDon.class).setParameter("id", id).setMaxResults(201).getResultList();
    }
}
