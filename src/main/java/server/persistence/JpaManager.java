package server.persistence;

import java.util.Map;
import java.util.function.Function;
import javax.persistence.*;

/** Factory dùng chung; mỗi tác vụ có EntityManager và transaction riêng. */
public final class JpaManager implements AutoCloseable {
    private final EntityManagerFactory factory;

    public JpaManager(Map<String, Object> properties) {
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
