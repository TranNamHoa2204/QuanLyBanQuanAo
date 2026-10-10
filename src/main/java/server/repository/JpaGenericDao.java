package server.repository;

import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;

/** Service cấp EntityManager và sở hữu transaction; DAO không tự commit. */
public class JpaGenericDao<T, ID> implements GenericDao<T, ID> {
    protected final EntityManager em;
    private final Class<T> type;
    public JpaGenericDao(EntityManager em, Class<T> type) { this.em = em; this.type = type; }
    @Override public Optional<T> findById(ID id) { return Optional.ofNullable(em.find(type, id)); }
    @Override public List<T> findAll() {
        return em.createQuery("select e from " + type.getSimpleName() + " e", type).getResultList();
    }
    @Override public T save(T entity) { return em.merge(entity); }
}
