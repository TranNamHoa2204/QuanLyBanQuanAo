package server.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import common.dto.KhachHangSearchRequest;
import server.entity.KhachHang;

/** JPA implementation; transaction do service sở hữu. */
public final class JpaKhachHangDao extends JpaGenericDao<KhachHang, String> implements KhachHangDao {
    public JpaKhachHangDao(EntityManager em) { super(em, KhachHang.class); }

    @Override public Optional<KhachHang> findForUpdate(String maKH) {
        return Optional.ofNullable(em.find(KhachHang.class, maKH, LockModeType.PESSIMISTIC_WRITE));
    }

    @Override public List<KhachHang> search(KhachHangSearchRequest request) {
        Filter filter = filter(request);
        TypedQuery<KhachHang> query = em.createQuery("select k from KhachHang k" + filter.where
                + " order by k.maKH", KhachHang.class);
        bind(query, filter);
        return query.setFirstResult(request.getPage() * request.getPageSize())
                .setMaxResults(request.getPageSize()).getResultList();
    }

    @Override public long count(KhachHangSearchRequest request) {
        Filter filter = filter(request);
        TypedQuery<Long> query = em.createQuery("select count(k) from KhachHang k" + filter.where, Long.class);
        bind(query, filter);
        return query.getSingleResult();
    }

    @Override public boolean codeInUseIgnoreCase(String maKH) {
        return em.createQuery("select count(k) from KhachHang k where lower(k.maKH) = :code", Long.class)
                .setParameter("code", maKH.toLowerCase(Locale.ROOT)).getSingleResult() > 0;
    }

    @Override public boolean phoneInUse(String sdt, String exceptMaKH) {
        if (sdt == null) { return false; }
        TypedQuery<String> query = em.createQuery("select k.sdt from KhachHang k where k.sdt is not null"
                + (exceptMaKH == null ? "" : " and k.maKH <> :except"), String.class);
        if (exceptMaKH != null) { query.setParameter("except", exceptMaKH); }
        String wanted = phoneKey(sdt);
        for (String existing : query.getResultList()) {
            if (wanted.equals(phoneKey(existing))) { return true; }
        }
        return false;
    }

    private static Filter filter(KhachHangSearchRequest request) {
        Filter filter = new Filter();
        String keyword = request.getTuKhoa();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
            filter.where += " and (lower(k.maKH) like :keyword escape '!'"
                    + " or lower(k.hoTen) like :keyword escape '!'"
                    + " or lower(k.sdt) like :keyword escape '!'"
                    + " or lower(k.email) like :keyword escape '!'";
            filter.params.put("keyword", pattern);
            String phoneDigits = phoneSearchDigits(keyword);
            if (phoneDigits.length() >= 4) {
                filter.where += " or k.sdt like :phoneKeyword";
                filter.params.put("phoneKeyword", "%" + phoneDigits + "%");
            }
            filter.where += ")";
        }
        if (request.getTrangThai() != null) {
            filter.where += " and k.trangThai = :active";
            filter.params.put("active", request.getTrangThai());
        }
        return filter;
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_")
                .replace("[", "![").replace("]", "!]");
    }
    private static String phoneSearchDigits(String value) { return value.replaceAll("[^0-9]", ""); }
    /** SĐT dạng cũ có thể chứa dấu cách, ngoặc hoặc gạch; so trùng theo chữ số. */
    private static String phoneKey(String value) { return value.replaceAll("[^0-9]", ""); }
    private static void bind(Query query, Filter filter) { filter.params.forEach(query::setParameter); }

    private static final class Filter {
        String where = " where 1=1";
        final Map<String, Object> params = new HashMap<>();
    }
}
