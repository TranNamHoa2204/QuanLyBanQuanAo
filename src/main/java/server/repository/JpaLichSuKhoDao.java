package server.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import common.dto.LichSuKhoSearchRequest;
import server.entity.LichSuKho;

public final class JpaLichSuKhoDao implements LichSuKhoDao {
    private final EntityManager em;

    public JpaLichSuKhoDao(EntityManager em) { this.em = em; }

    @Override public Optional<LichSuKho> findByOperationId(String operationId) {
        List<LichSuKho> rows = em.createQuery("select l from LichSuKho l where l.operationId = :operationId", LichSuKho.class)
                .setParameter("operationId", operationId).setMaxResults(1).getResultList();
        return rows.isEmpty() ? Optional.<LichSuKho>empty() : Optional.of(rows.get(0));
    }

    @Override public List<LichSuKho> search(LichSuKhoSearchRequest request) {
        Filter filter = filter(request);
        TypedQuery<LichSuKho> query = em.createQuery("select l from LichSuKho l "
                + "join fetch l.bienThe b join fetch b.sanPham p left join fetch l.nguoiThaoTac n"
                + filter.where + " order by l.thoiGian desc, l.maLichSuKho desc", LichSuKho.class);
        filter.params.forEach(query::setParameter);
        int offset = request.getPage() * request.getPageSize();
        return query.setFirstResult(offset).setMaxResults(request.getPageSize()).getResultList();
    }

    @Override public long count(LichSuKhoSearchRequest request) {
        Filter filter = filter(request);
        TypedQuery<Long> query = em.createQuery("select count(l) from LichSuKho l" + filter.where, Long.class);
        filter.params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    private static Filter filter(LichSuKhoSearchRequest request) {
        Filter filter = new Filter();
        if (request.getMaBienThe() != null) {
            filter.where += " and l.bienThe.maBienThe = :variant";
            filter.params.put("variant", request.getMaBienThe());
        }
        if (request.getTuNgay() != null) {
            filter.where += " and l.thoiGian >= :from";
            filter.params.put("from", request.getTuNgay().atStartOfDay());
        }
        if (request.getDenNgay() != null) {
            filter.where += " and l.thoiGian < :until";
            filter.params.put("until", request.getDenNgay().plusDays(1).atStartOfDay());
        }
        if (request.getNguon() != null) {
            filter.where += " and l.nguon = :source";
            filter.params.put("source", request.getNguon());
        }
        if (request.getMaNV() != null) {
            filter.where += " and l.nguoiThaoTac.maNV = :employee";
            filter.params.put("employee", request.getMaNV());
        }
        return filter;
    }

    private static final class Filter {
        String where = " where 1=1";
        final Map<String, Object> params = new HashMap<String, Object>();
    }
}
