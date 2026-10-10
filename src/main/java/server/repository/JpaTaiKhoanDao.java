package server.repository;

import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import server.entity.TaiKhoan;

/** Ngắn hạn, chỉ tồn tại bên trong transaction của service. */
public final class JpaTaiKhoanDao implements TaiKhoanDao {
    private final EntityManager em;
    public JpaTaiKhoanDao(EntityManager em) { this.em = em; }
    @Override public Optional<TaiKhoan> findById(String id) {
        return Optional.ofNullable(em.find(TaiKhoan.class, id));
    }
    public Optional<TaiKhoan> findForUpdate(String id) {
        return Optional.ofNullable(em.find(TaiKhoan.class, id, LockModeType.PESSIMISTIC_WRITE));
    }
    @Override public List<TaiKhoan> findAll() {
        return em.createQuery("select t from TaiKhoan t", TaiKhoan.class).getResultList();
    }
    @Override public TaiKhoan save(TaiKhoan entity) { return em.merge(entity); }
}
