package server.repository;

import java.util.List;
import java.util.Optional;

/** DAO chỉ truy cập dữ liệu. Service sở hữu transaction; không commit riêng từng DAO. */
public interface GenericDao<T, ID> {
    Optional<T> findById(ID id);

    List<T> findAll();

    T save(T entity);
}
