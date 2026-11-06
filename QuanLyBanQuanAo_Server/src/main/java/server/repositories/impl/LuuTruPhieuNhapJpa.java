package server.repositories.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import common.dto.phieunhap.YeuCauTraCuuPhieuNhap;
import server.entities.ChiTietPhieuNhap;
import server.entities.PhieuNhap;
import server.repositories.LuuTruPhieuNhap;

public final class LuuTruPhieuNhapJpa implements LuuTruPhieuNhap {
    private final EntityManager em;

    public LuuTruPhieuNhapJpa(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<PhieuNhap> findById(String id) {
        return Optional.ofNullable(em.find(PhieuNhap.class, id));
    }

    @Override
    public Optional<PhieuNhap> findForUpdate(String id) {
        return Optional.ofNullable(em.find(PhieuNhap.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    @Override
    public Optional<PhieuNhap> findByCreateOperation(String id) {
        return em.createQuery("select p from PhieuNhap p where p.operationIdTao = :id", PhieuNhap.class)
                .setParameter("id", id).getResultStream().findFirst();
    }

    @Override
    public Optional<PhieuNhap> findByConfirmOperation(String id) {
        return em.createQuery("select p from PhieuNhap p where p.operationIdXacNhan = :id", PhieuNhap.class)
                .setParameter("id", id).getResultStream().findFirst();
    }

    @Override
    public List<ChiTietPhieuNhap> lines(String id) {
        return em
                .createQuery(
                        "select c from ChiTietPhieuNhap c join fetch c.bienThe b join fetch b.sanPham "
                                + "where c.phieuNhap.maPhieuNhap = :id order by b.maBienThe",
                        ChiTietPhieuNhap.class)
                .setParameter("id", id).getResultList();
    }

    @Override
    public List<PhieuNhap> search(YeuCauTraCuuPhieuNhap request) {
        Filter f = filter(request);
        TypedQuery<PhieuNhap> q = em.createQuery("select p from PhieuNhap p join fetch p.nhaCungCap"
                + " join fetch p.nhanVienTao" + f.where + " order by p.ngayLap desc, p.maPhieuNhap desc",
                PhieuNhap.class);
        bind(q, f);
        return q.setFirstResult(request.getPage() * request.getPageSize())
                .setMaxResults(request.getPageSize()).getResultList();
    }

    @Override
    public long count(YeuCauTraCuuPhieuNhap request) {
        Filter f = filter(request);
        TypedQuery<Long> q = em.createQuery("select count(p) from PhieuNhap p" + f.where, Long.class);
        bind(q, f);
        return q.getSingleResult();
    }

    private static Filter filter(YeuCauTraCuuPhieuNhap r) {
        Filter f = new Filter();
        if (r.getMaNCC() != null && !r.getMaNCC().trim().isEmpty()) {
            f.where += " and p.nhaCungCap.maNCC = :supplier";
            f.params.put("supplier", r.getMaNCC().trim());
        }
        if (r.getTrangThai() != null) {
            f.where += " and p.trangThai = :status";
            f.params.put("status", r.getTrangThai());
        }
        if (r.getTuNgay() != null) {
            f.where += " and p.ngayLap >= :from";
            f.params.put("from", r.getTuNgay().atStartOfDay());
        }
        if (r.getDenNgay() != null) {
            f.where += " and p.ngayLap < :to";
            f.params.put("to", r.getDenNgay().plusDays(1).atStartOfDay());
        }
        if (r.getTuKhoa() != null && !r.getTuKhoa().trim().isEmpty()) {
            f.where += " and (lower(p.maPhieuNhap) like :q or lower(p.nhaCungCap.maNCC) like :q or lower(p.nhaCungCap.tenNCC) like :q)";
            f.params.put("q", "%" + r.getTuKhoa().trim().toLowerCase(java.util.Locale.ROOT) + "%");
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
