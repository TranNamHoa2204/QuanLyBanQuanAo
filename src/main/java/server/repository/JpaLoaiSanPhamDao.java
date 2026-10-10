package server.repository;

import java.util.List;
import javax.persistence.EntityManager;
import server.entity.LoaiSanPham;

public final class JpaLoaiSanPhamDao extends JpaGenericDao<LoaiSanPham, String> implements LoaiSanPhamDao {
    public JpaLoaiSanPhamDao(EntityManager em) { super(em, LoaiSanPham.class); }
    public List<LoaiSanPham> ordered() {
        return em.createQuery("select d from LoaiSanPham d order by d.tenLoai, d.maLoai", LoaiSanPham.class)
                .setMaxResults(1001).getResultList();
    }
}
