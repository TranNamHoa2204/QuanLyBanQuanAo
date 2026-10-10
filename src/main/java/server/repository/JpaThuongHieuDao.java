package server.repository;

import java.util.List;
import javax.persistence.EntityManager;
import server.entity.ThuongHieu;

public final class JpaThuongHieuDao extends JpaGenericDao<ThuongHieu, String> implements ThuongHieuDao {
    public JpaThuongHieuDao(EntityManager em) { super(em, ThuongHieu.class); }
    public List<ThuongHieu> ordered() {
        return em.createQuery("select d from ThuongHieu d order by d.tenThuongHieu, d.maThuongHieu", ThuongHieu.class)
                .setMaxResults(1001).getResultList();
    }
}
