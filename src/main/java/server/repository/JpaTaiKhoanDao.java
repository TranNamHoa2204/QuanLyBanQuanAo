package server.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import common.enums.VaiTro;
import server.entity.NhanVien;
import server.entity.TaiKhoan;

/** Ngắn hạn, chỉ tồn tại bên trong transaction của service. */
public final class JpaTaiKhoanDao implements TaiKhoanDao {
    private final EntityManager em;
    public JpaTaiKhoanDao(EntityManager em) { this.em = em; }
    @Override public Optional<TaiKhoan> findById(String id) {
        return Optional.ofNullable(em.find(TaiKhoan.class, id));
    }
    public Optional<TaiKhoan> findForUpdate(String id) {
        return Optional.ofNullable(em.find(TaiKhoan.class, id, LockModeType.PESSIMISTIC_WRITE));
    }
    public Optional<TaiKhoan> findByEmployee(String maNV) {
        return em.createQuery("select t from TaiKhoan t join fetch t.nhanVien where t.nhanVien.maNV = :id",
                TaiKhoan.class).setParameter("id", maNV).getResultStream().findFirst();
    }
    public List<TaiKhoan> findByEmployeeIds(List<String> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) { return java.util.Collections.emptyList(); }
        return em.createQuery("select t from TaiKhoan t join fetch t.nhanVien where t.nhanVien.maNV in :ids",
                TaiKhoan.class).setParameter("ids", employeeIds).getResultList();
    }
    public boolean usernameExistsIgnoreCase(String username) {
        return em.createQuery("select count(t) from TaiKhoan t where lower(t.username) = :username", Long.class)
                .setParameter("username", username.toLowerCase(java.util.Locale.ROOT)).getSingleResult() > 0;
    }
    /** Khóa tập quản lý đang hoạt động theo thứ tự username để tuần tự hóa thao tác có thể giảm số quản lý. */
    public List<TaiKhoan> lockActiveManagers() {
        List<TaiKhoan> rows = em.createQuery("select t from TaiKhoan t where t.trangThai = true"
                + " and t.vaiTro = :role and t.nhanVien.trangThai = true order by t.username", TaiKhoan.class)
                .setParameter("role", VaiTro.NHAN_VIEN_QUAN_LY)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        for (TaiKhoan account : rows) {
            em.refresh(account, LockModeType.PESSIMISTIC_WRITE);
            NhanVien employee = account.getNhanVien();
            if (employee != null) { em.refresh(employee, LockModeType.PESSIMISTIC_WRITE); }
        }
        // Một truy vấn đang chờ khóa có thể đã thấy trạng thái trước commit của request đứng trước.
        // Chỉ đếm lại những dòng còn là quản lý hoạt động sau refresh, nếu không hai request có thể
        // đồng thời hạ quyền hai quản lý cuối dựa trên cùng số đếm cũ.
        List<TaiKhoan> current = new ArrayList<>();
        for (TaiKhoan account : rows) {
            if (account.getTrangThai() && account.getVaiTro() == VaiTro.NHAN_VIEN_QUAN_LY
                    && account.getNhanVien() != null && account.getNhanVien().getTrangThai()) {
                current.add(account);
            }
        }
        return current;
    }
    @Override public List<TaiKhoan> findAll() {
        return em.createQuery("select t from TaiKhoan t", TaiKhoan.class).getResultList();
    }
    @Override public TaiKhoan save(TaiKhoan entity) { return em.merge(entity); }
}
