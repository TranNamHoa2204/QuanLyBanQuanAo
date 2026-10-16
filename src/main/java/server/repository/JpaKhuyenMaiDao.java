package server.repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import common.dto.KhuyenMaiSearchRequest;
import server.entity.KhuyenMai;

/** JPA implementation; transaction do service sở hữu. */
public final class JpaKhuyenMaiDao extends JpaGenericDao<KhuyenMai, String> implements KhuyenMaiDao {
    public JpaKhuyenMaiDao(EntityManager em) { super(em, KhuyenMai.class); }

    @Override public Optional<KhuyenMai> findForUpdate(String maKM) {
        return Optional.ofNullable(em.find(KhuyenMai.class, maKM, LockModeType.PESSIMISTIC_WRITE));
    }

    @Override public List<KhuyenMai> search(KhuyenMaiSearchRequest request, Boolean savedActive,
            boolean effectiveOnly, LocalDate today) {
        Filter filter = filter(request, savedActive, effectiveOnly, today);
        TypedQuery<KhuyenMai> query = em.createQuery("select k from KhuyenMai k" + filter.where
                + " order by k.maKM", KhuyenMai.class);
        bind(query, filter);
        return query.setFirstResult(request.getPage() * request.getPageSize())
                .setMaxResults(request.getPageSize()).getResultList();
    }

    @Override public long count(KhuyenMaiSearchRequest request, Boolean savedActive,
            boolean effectiveOnly, LocalDate today) {
        Filter filter = filter(request, savedActive, effectiveOnly, today);
        TypedQuery<Long> query = em.createQuery("select count(k) from KhuyenMai k" + filter.where, Long.class);
        bind(query, filter); return query.getSingleResult();
    }

    @Override public boolean codeInUseIgnoreCase(String maKM) {
        return em.createQuery("select count(k) from KhuyenMai k where lower(k.maKM) = :code", Long.class)
                .setParameter("code", maKM.toLowerCase(Locale.ROOT)).getSingleResult() > 0;
    }

    private static Filter filter(KhuyenMaiSearchRequest request, Boolean savedActive,
            boolean effectiveOnly, LocalDate today) {
        Filter filter = new Filter();
        String keyword = request.getTuKhoa();
        if (keyword != null && !keyword.isEmpty()) {
            filter.where += " and (lower(k.maKM) like :keyword escape '!'"
                    + " or lower(k.tenKM) like :keyword escape '!')";
            filter.params.put("keyword", "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%");
        }
        if (savedActive != null) {
            filter.where += " and k.trangThai = :savedActive";
            filter.params.put("savedActive", savedActive);
        }
        if (effectiveOnly) {
            filter.where += " and k.trangThai = true and k.ngayBatDau <= :today and k.ngayKetThuc >= :today";
            filter.params.put("today", today);
        }
        return filter;
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_")
                .replace("[", "![").replace("]", "!]");
    }
    private static void bind(Query query, Filter filter) { filter.params.forEach(query::setParameter); }

    private static final class Filter {
        String where = " where 1=1";
        final Map<String, Object> params = new HashMap<>();
    }
}
