package server.repositories.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

import common.dto.hoadon.YeuCauTraCuuHoaDon;
import server.entities.HoaDon;
import server.repositories.LuuTruHoaDon;

public final class LuuTruHoaDonJpa extends LuuTruCoBanJpa<HoaDon, String> implements LuuTruHoaDon {
    public LuuTruHoaDonJpa(EntityManager em) {
        super(em, HoaDon.class);
    }

    public Optional<HoaDon> locked(String id) {
        return Optional.ofNullable(em.find(HoaDon.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    public List<HoaDon> search(YeuCauTraCuuHoaDon request, String employeeId) {
        Filter filter = filter(request, employeeId);
        // headerDto reads employee/customer names; both joins are to-one, so paging stays row-stable.
        TypedQuery<HoaDon> query = em.createQuery("select h from HoaDon h join fetch h.nhanVien"
                + " left join fetch h.khachHang" + filter.where + " order by h.ngayLap desc, h.maHD desc",
                HoaDon.class);
        filter.params.forEach(query::setParameter);
        return query.setFirstResult(request.getPage() * request.getPageSize())
                .setMaxResults(request.getPageSize()).getResultList();
    }

    public long count(YeuCauTraCuuHoaDon request, String employeeId) {
        Filter filter = filter(request, employeeId);
        TypedQuery<Long> query = em.createQuery("select count(h) from HoaDon h" + filter.where, Long.class);
        filter.params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    private static Filter filter(YeuCauTraCuuHoaDon request, String employeeId) {
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
