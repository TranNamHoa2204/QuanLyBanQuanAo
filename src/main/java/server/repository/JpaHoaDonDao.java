package server.repository;

import java.util.*;
import javax.persistence.*;
import common.dto.HoaDonSearchRequest;
import server.entity.HoaDon;

public final class JpaHoaDonDao extends JpaGenericDao<HoaDon, String> implements HoaDonDao {
    public JpaHoaDonDao(EntityManager em) {
        super(em, HoaDon.class);
    }

    public Optional<HoaDon> locked(String id) {
        return Optional.ofNullable(em.find(HoaDon.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    public List<HoaDon> search(HoaDonSearchRequest request, String employeeId) {
        Filter filter = filter(request, employeeId);
        TypedQuery<HoaDon> query = em.createQuery(
                "select h from HoaDon h" + filter.where + " order by h.ngayLap desc, h.maHD desc", HoaDon.class);
        filter.params.forEach(query::setParameter);
        return query.setFirstResult(request.getPage() * request.getPageSize()).setMaxResults(request.getPageSize())
                .getResultList();
    }

    public long count(HoaDonSearchRequest request, String employeeId) {
        Filter filter = filter(request, employeeId);
        TypedQuery<Long> query = em.createQuery("select count(h) from HoaDon h" + filter.where, Long.class);
        filter.params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    private static Filter filter(HoaDonSearchRequest request, String employeeId) {
        Filter f = new Filter();
        if (employeeId != null) {
            f.where += " and h.nhanVien.maNV = :employee";
            f.params.put("employee", employeeId);
        }
        if (request.getMaHD() != null && !request.getMaHD().trim().isEmpty()) {
            f.where += " and h.maHD = :id";
            f.params.put("id", request.getMaHD().trim());
        }
        if (request.getTrangThai() != null) {
            f.where += " and h.trangThai = :state";
            f.params.put("state", request.getTrangThai());
        }
        return f;
    }

    private static final class Filter {
        String where = " where 1=1";
        final Map<String, Object> params = new HashMap<>();
    }
}
