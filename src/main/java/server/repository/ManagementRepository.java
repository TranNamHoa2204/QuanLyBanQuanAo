package server.repository;

import java.util.*;
import javax.persistence.*;
import common.dto.ManagementSearchRequest;

/** Tra cứu phân trang cho ba danh mục. Không nhận tên bảng/cột từ client. */
public final class ManagementRepository<T> {
    private final EntityManager em;
    private final Class<T> type;
    private final String id, name, extra;

    public ManagementRepository(EntityManager em, Class<T> type, String id, String name, String extra) {
        this.em = em;
        this.type = type;
        this.id = id;
        this.name = name;
        this.extra = extra;
    }

    private Query query(ManagementSearchRequest r, boolean count) {
        String q = r.getQuery() == null ? "" : r.getQuery().trim().toLowerCase(Locale.ROOT);
        String where = " where (:all = true or e.trangThai = true) and (:q = '' or locate(:q, lower(e." + id
                + "), 1) > 0 or locate(:q, lower(e." + name + "), 1) > 0"
                + (extra == null ? "" : " or locate(:q, lower(e." + extra + "), 1) > 0") + ")";
        String jpql = (count ? "select count(e)" : "select e") + " from " + type.getSimpleName() + " e" + where
                + (count ? "" : " order by e." + id);
        return em.createQuery(jpql).setParameter("all", r.getIncludeInactive()).setParameter("q", q);
    }

    public long count(ManagementSearchRequest r) {
        return ((Number) query(r, true).getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<T> search(ManagementSearchRequest r) {
        return query(r, false).setFirstResult(r.getPage() * r.getPageSize()).setMaxResults(r.getPageSize())
                .getResultList();
    }
}
