package server.repository;

import java.util.List;
import javax.persistence.EntityManager;
import server.entity.BienThe;

public final class JpaBienTheDao extends JpaGenericDao<BienThe, String> implements BienTheDao {
    public JpaBienTheDao(EntityManager em) { super(em, BienThe.class); }
    public List<BienThe> byProduct(String id, boolean includeInactive) {
        return em.createQuery("select b from BienThe b where b.sanPham.maSP = :id"
                + (includeInactive ? "" : " and b.trangThai = true")
                + " order by b.mauSac, b.kichCo, b.maBienThe", BienThe.class)
                .setParameter("id", id).setMaxResults(501).getResultList();
    }
}
