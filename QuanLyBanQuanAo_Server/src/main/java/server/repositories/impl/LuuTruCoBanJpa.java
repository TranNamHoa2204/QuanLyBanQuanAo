package server.repositories.impl;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import server.repositories.LuuTruCoBan;

/** Service cấp EntityManager và sở hữu transaction; DAO không tự commit. */
public class LuuTruCoBanJpa<T, ID> implements LuuTruCoBan<T, ID> {
    protected final EntityManager em;
    private final Class<T> type;

    public LuuTruCoBanJpa(EntityManager em, Class<T> type) {
        this.em = em;
        this.type = type;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(em.find(type, id));
    }

    @Override
    public List<T> findAll() {
        return em.createQuery("select e from " + type.getSimpleName() + " e", type).getResultList();
    }

    @Override
    public T save(T entity) {
        return em.merge(entity);
    }
}
