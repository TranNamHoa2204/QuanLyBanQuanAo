package server.repository;

import java.util.List;
import javax.persistence.EntityManager;
import server.entity.AnhSanPham;

public final class JpaAnhSanPhamDao extends JpaGenericDao<AnhSanPham, String> implements AnhSanPhamDao {
    public JpaAnhSanPhamDao(EntityManager em) { super(em, AnhSanPham.class); }
    public List<AnhSanPham> byProduct(String id, boolean includeInactive) {
        return em.createQuery("select a from AnhSanPham a where a.sanPham.maSP = :id"
                + (includeInactive ? "" : " and exists (select b.maBienThe from BienThe b"
                    + " where b.sanPham = a.sanPham and b.mauSac = a.mauSac and b.trangThai = true)")
                + " order by a.mauSac, a.anhChinh desc, a.thuTu, a.maAnh", AnhSanPham.class)
                .setParameter("id", id).setMaxResults(501).getResultList();
    }
}
