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
import common.dto.NhanVienSearchRequest;
import server.entity.NhanVien;

/** JPA implementation dùng trong transaction do service sở hữu. */
public final class JpaNhanVienDao extends JpaGenericDao<NhanVien, String> implements NhanVienDao {
    public JpaNhanVienDao(EntityManager em) { super(em, NhanVien.class); }

    @Override public Optional<NhanVien> findForUpdate(String maNV) {
        return Optional.ofNullable(em.find(NhanVien.class, maNV, LockModeType.PESSIMISTIC_WRITE));
    }

    @Override public List<NhanVien> search(NhanVienSearchRequest request) {
        Filter filter = filter(request);
        TypedQuery<NhanVien> query = em.createQuery("select n from NhanVien n" + filter.where
                + " order by n.maNV", NhanVien.class);
        bind(query, filter);
        return query.setFirstResult(request.getPage() * request.getPageSize())
                .setMaxResults(request.getPageSize()).getResultList();
    }

    @Override public long count(NhanVienSearchRequest request) {
        Filter filter = filter(request);
        TypedQuery<Long> query = em.createQuery("select count(n) from NhanVien n" + filter.where, Long.class);
        bind(query, filter);
        return query.getSingleResult();
    }

    @Override public boolean phoneInUse(String sdt, String exceptMaNV) {
        TypedQuery<String> query = em.createQuery("select n.sdt from NhanVien n"
                + (exceptMaNV == null ? "" : " where n.maNV <> :except"), String.class);
        if (exceptMaNV != null) { query.setParameter("except", exceptMaNV); }
        String wanted = phoneKey(sdt);
        for (String existing : query.getResultList()) {
            if (wanted.equals(phoneKey(existing))) { return true; }
        }
        return false;
    }

    @Override public boolean codeInUseIgnoreCase(String maNV) {
        return em.createQuery("select count(n) from NhanVien n where lower(n.maNV) = :code", Long.class)
                .setParameter("code", maNV.toLowerCase(Locale.ROOT)).getSingleResult() > 0;
    }

    private Filter filter(NhanVienSearchRequest request) {
        Filter filter = new Filter();
        String keyword = request.getTuKhoa();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
            filter.where += " and (lower(n.maNV) like :keyword escape '!'"
                    + " or lower(n.hoTen) like :keyword escape '!'"
                    + " or lower(n.sdt) like :keyword escape '!'"
                    + " or lower(n.email) like :keyword escape '!'"
                    + " or exists (select t.username from TaiKhoan t where t.nhanVien = n"
                    + " and lower(t.username) like :keyword escape '!'))";
            filter.params.put("keyword", pattern);
        }
        if (request.getTrangThai() != null) {
            filter.where += " and n.trangThai = :employeeActive";
            filter.params.put("employeeActive", request.getTrangThai());
        }
        if (request.getCoTaiKhoan() != null) {
            filter.where += request.getCoTaiKhoan()
                    ? " and exists (select t.username from TaiKhoan t where t.nhanVien = n)"
                    : " and not exists (select t.username from TaiKhoan t where t.nhanVien = n)";
        }
        if (request.getTaiKhoanHoatDong() != null) {
            filter.where += " and exists (select t.username from TaiKhoan t where t.nhanVien = n and t.trangThai = :accountActive)";
            filter.params.put("accountActive", request.getTaiKhoanHoatDong());
        }
        if (request.getVaiTro() != null) {
            filter.where += " and exists (select t.username from TaiKhoan t where t.nhanVien = n and t.vaiTro = :role)";
            filter.params.put("role", request.getVaiTro());
        }
        return filter;
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_")
                .replace("[", "![").replace("]", "!]");
    }
    /** So sánh số theo chữ số để dấu cách, ngoặc và gạch nối không tạo bản ghi trùng. */
    private static String phoneKey(String value) { return value.replaceAll("[^0-9]", ""); }
    private static void bind(Query query, Filter filter) { filter.params.forEach(query::setParameter); }
    private static final class Filter {
        String where = " where 1=1";
        final Map<String, Object> params = new HashMap<>();
    }
}
