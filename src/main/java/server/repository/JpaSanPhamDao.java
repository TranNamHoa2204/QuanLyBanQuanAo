package server.repository;

import java.util.*;
import javax.persistence.*;
import common.dto.SanPhamSearchRequest;
import server.entity.SanPham;

public final class JpaSanPhamDao extends JpaGenericDao<SanPham, String> implements SanPhamDao {
    public JpaSanPhamDao(EntityManager em) {
        super(em, SanPham.class);
    }

    public List<SanPham> search(SanPhamSearchRequest request) {
        Filter filter = filter(request);
        TypedQuery<SanPham> query = em.createQuery("select p from SanPham p join fetch p.loaiSanPham"
                + " join fetch p.thuongHieu" + filter.where + " order by p.maSP", SanPham.class);
        bind(query, filter);
        return query.setFirstResult(request.getPage() * request.getPageSize()).setMaxResults(request.getPageSize())
                .getResultList();
    }

    public long count(SanPhamSearchRequest request) {
        Filter filter = filter(request);
        TypedQuery<Long> query = em.createQuery("select count(p) from SanPham p" + filter.where, Long.class);
        bind(query, filter);
        return query.getSingleResult();
    }

    public Map<String, Long> activeStock(List<String> ids) {
        Map<String, Long> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        // Ép BIGINT trước SUM để không tràn SUM(INT) trên SQL Server.
        List<Object[]> rows = em.createQuery("select b.sanPham.maSP, sum(cast(b.soLuongTon as long))"
                + " from BienThe b where b.trangThai = true and b.sanPham.maSP in :ids" + " group by b.sanPham.maSP",
                Object[].class).setParameter("ids", ids).getResultList();
        for (Object[] row : rows) {
            result.put((String) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    private static Filter filter(SanPhamSearchRequest request) {
        Filter f = new Filter();
        if (!request.getIncludeInactive()) {
            f.where += " and p.trangThai = true";
        }
        if (request.getMaSP() != null && request.getTenSP() != null) {
            f.where += " and (lower(p.maSP) like :code escape '!' or lower(p.tenSP) like :name escape '!')";
            f.params.put("code", "%" + escapeLike(request.getMaSP().toLowerCase(Locale.ROOT)) + "%");
            f.params.put("name", "%" + escapeLike(request.getTenSP().toLowerCase(Locale.ROOT)) + "%");
        } else if (request.getMaSP() != null) {
            f.where += " and lower(p.maSP) like :code escape '!'";
            f.params.put("code", "%" + escapeLike(request.getMaSP().toLowerCase(Locale.ROOT)) + "%");
        } else if (request.getTenSP() != null) {
            f.where += " and lower(p.tenSP) like :name escape '!'";
            f.params.put("name", "%" + escapeLike(request.getTenSP().toLowerCase(Locale.ROOT)) + "%");
        }
        if (request.getMaLoai() != null) {
            f.where += " and p.loaiSanPham.maLoai = :category";
            f.params.put("category", request.getMaLoai());
        }
        if (request.getMaThuongHieu() != null) {
            f.where += " and p.thuongHieu.maThuongHieu = :brand";
            f.params.put("brand", request.getMaThuongHieu());
        }
        if (request.getMauSac() != null || request.getKichCo() != null) {
            // Màu + size phải thuộc CÙNG biến thể, không ghép hai biến thể khác nhau.
            f.where += " and exists (select b.maBienThe from BienThe b where b.sanPham = p";
            if (!request.getIncludeInactive()) {
                f.where += " and b.trangThai = true";
            }
            if (request.getMauSac() != null) {
                f.where += " and lower(b.mauSac) = :color";
                f.params.put("color", request.getMauSac().toLowerCase(Locale.ROOT));
            }
            if (request.getKichCo() != null) {
                f.where += " and lower(b.kichCo) = :size";
                f.params.put("size", request.getKichCo().toLowerCase(Locale.ROOT));
            }
            f.where += ")";
        }
        return f;
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_").replace("[", "![").replace("]", "!]");
    }

    private static void bind(Query query, Filter filter) {
        filter.params.forEach(query::setParameter);
    }

    private static final class Filter {
        String where = " where 1=1";
        final Map<String, Object> params = new HashMap<>();
    }
}
