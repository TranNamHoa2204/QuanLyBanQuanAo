package server.repositories.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import common.dto.nhacungcap.YeuCauTraCuuNhaCungCap;
import server.entities.NhaCungCap;
import server.repositories.LuuTruNhaCungCap;

public final class LuuTruNhaCungCapJpa implements LuuTruNhaCungCap {
    private final EntityManager em;

    public LuuTruNhaCungCapJpa(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<NhaCungCap> findById(String id) {
        return Optional.ofNullable(em.find(NhaCungCap.class, id));
    }

    @Override
    public Optional<NhaCungCap> findForUpdate(String id) {
        return Optional.ofNullable(em.find(NhaCungCap.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    @Override
    public boolean codeExists(String id) {
        return em.createQuery("select count(n) from NhaCungCap n where lower(n.maNCC) = :id", Long.class)
                .setParameter("id", id.toLowerCase(Locale.ROOT)).getSingleResult() > 0;
    }

    @Override
    public boolean phoneExists(String phone, String exceptId) {
        if (phone == null) {
            return false;
        }
        TypedQuery<Long> query = em
                .createQuery("select count(n) from NhaCungCap n where n.sdt = :phone"
                        + (exceptId == null ? "" : " and n.maNCC <> :except"), Long.class)
                .setParameter("phone", phone);
        if (exceptId != null) {
            query.setParameter("except", exceptId);
        }
        return query.getSingleResult() > 0;
    }

    @Override
    public List<NhaCungCap> search(YeuCauTraCuuNhaCungCap request) {
        Filter f = filter(request);
        TypedQuery<NhaCungCap> q = em.createQuery(
                "select n from NhaCungCap n" + f.where + " order by n.tenNCC, n.maNCC", NhaCungCap.class);
        bind(q, f);
        return q.setFirstResult(request.getPage() * request.getPageSize())
                .setMaxResults(request.getPageSize()).getResultList();
    }

    @Override
    public long count(YeuCauTraCuuNhaCungCap request) {
        Filter f = filter(request);
        TypedQuery<Long> q = em.createQuery("select count(n) from NhaCungCap n" + f.where, Long.class);
        bind(q, f);
        return q.getSingleResult();
    }

    private static Filter filter(YeuCauTraCuuNhaCungCap r) {
        Filter f = new Filter();
        if (r.getTuKhoa() != null && !r.getTuKhoa().trim().isEmpty()) {
            f.where += " and (lower(n.maNCC) like :q escape '!' or lower(n.tenNCC) like :q escape '!'"
                    + " or lower(n.sdt) like :q escape '!' or lower(n.email) like :q escape '!')";
            f.params.put("q", "%" + r.getTuKhoa().trim().toLowerCase(Locale.ROOT).replace("!", "!!")
                    .replace("%", "!%").replace("_", "!_") + "%");
        }
        if (r.getTrangThai() != null) {
            f.where += " and n.trangThai = :active";
            f.params.put("active", r.getTrangThai());
        }
        return f;
    }

    private static void bind(Query q, Filter f) {
        f.params.forEach(q::setParameter);
    }

    private static final class Filter {
        String where = " where 1=1";
        final Map<String, Object> params = new HashMap<String, Object>();
    }
}
