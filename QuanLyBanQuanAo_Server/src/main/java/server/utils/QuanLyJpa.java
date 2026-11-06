package server.utils;

import java.util.Map;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

/** Factory dùng chung; mỗi tác vụ có EntityManager và transaction riêng. */
public final class QuanLyJpa implements AutoCloseable {
    private final EntityManagerFactory factory;

    public QuanLyJpa(Map<String, Object> properties) {
        factory = Persistence.createEntityManagerFactory("QuanAoPU", properties);
    }

    public <T> T transaction(Function<EntityManager, T> work) {
        EntityManager em = factory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T result = work.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException failure) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw failure;
        } finally {
            em.close();
        }
    }

    @Override
    public void close() {
        if (factory.isOpen()) {
            factory.close();
        }
    }
}
