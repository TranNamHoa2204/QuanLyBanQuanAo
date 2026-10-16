package server.repository;

import java.util.List;
import javax.persistence.EntityManager;
import server.entity.ChiTietHoaDon;

public final class JpaChiTietHoaDonDao extends JpaGenericDao<ChiTietHoaDon, Long> implements ChiTietHoaDonDao {
    public JpaChiTietHoaDonDao(EntityManager em) { super(em, ChiTietHoaDon.class); }
    public List<ChiTietHoaDon> byInvoice(String id) {
        return em.createQuery("select c from ChiTietHoaDon c join fetch c.bienThe b join fetch b.sanPham"
                + " where c.hoaDon.maHD = :id order by b.maBienThe", ChiTietHoaDon.class)
                .setParameter("id", id).setMaxResults(201).getResultList();
    }
}

