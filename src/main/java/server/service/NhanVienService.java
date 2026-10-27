package server.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import javax.persistence.*;
import common.dto.*;
import common.enums.VaiTro;
import common.protocol.JsonCodec;
import server.entity.*;
import server.persistence.JpaManager;
import server.repository.ManagementRepository;

/** Nhân viên/tài khoản được lưu cùng transaction, không bao giờ trả hash. */
public final class NhanVienService {
    // Một JVM server hiện tại. Serialize thay quyền để tránh hai quản lý đồng thời hạ quyền nhau.
    // Nếu triển khai nhiều JVM, thay guard bằng khóa liên tiến trình/database.
    private static final Object ACCOUNT_ADMIN_GUARD = new Object();
    private final JpaManager jpa;
    private final AuthService auth;

    public NhanVienService(JpaManager jpa, AuthService auth) {
        this.jpa = jpa;
        this.auth = auth;
    }

    public ManagementSearchResult search(String token, ManagementSearchRequest r) {
        return jpa.transaction(em -> {
            ManagementSupport.manager(auth, em, token);
            ManagementSupport.page(r);
            ManagementRepository<NhanVien> repo = new ManagementRepository<>(em, NhanVien.class, "maNV", "hoTen",
                    "sdt");
            ManagementSearchResult out = new ManagementSearchResult();
            out.setTotal(repo.count(r));
            for (NhanVien e : repo.search(r)) {
                out.getItems().add(JsonCodec.mapper().valueToTree(dto(e, account(em, e.getMaNV(), false))));
            }
            return out;
        });
    }

    public NhanVienAccountDTO save(String token, ManagementSaveRequest r, int mode) {
        synchronized (ACCOUNT_ADMIN_GUARD) {
            Change change = jpa.transaction(em -> {
                TaiKhoan caller = ManagementSupport.manager(auth, em, token);
                NhanVienAccountDTO v = ManagementSupport.value(r, NhanVienAccountDTO.class);
                String id = ManagementSupport.code(v.getMaNV());
                TaiKhoan a = account(em, id, mode != 0);
                NhanVien e = em.find(NhanVien.class, id,
                        mode == 0 ? LockModeType.NONE : LockModeType.PESSIMISTIC_WRITE);
                if (mode == 0) {
                    if (e != null) {
                        throw ManagementSupport.invalid("Mã nhân viên đã tồn tại.");
                    }
                    e = new NhanVien();
                    e.setMaNV(id);
                } else {
                    ManagementSupport.missing(e);
                    ManagementSupport.expected(r, dto(e, a));
                }
                boolean self = id.equals(caller.getNhanVien().getMaNV());
                if (self && (mode == 2 || !v.getTrangThai() || !v.getTaiKhoanHoatDong()
                        || v.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY)) {
                    throw ManagementSupport.invalid("Không thể tự khóa hoặc hạ quyền tài khoản đang sử dụng.");
                }
                boolean securityChanged = mode == 2 || (a != null && (a.getVaiTro() != v.getVaiTro()
                        || a.getTrangThai() != v.getTaiKhoanHoatDong() || e.getTrangThai() != v.getTrangThai()));
                if (mode == 2) {
                    e.setTrangThai(false);
                    if (a != null) {
                        a.setTrangThai(false);
                    }
                } else {
                    e.setHoTen(ManagementSupport.text(v.getHoTen(), 100, true));
                    String phone = ManagementSupport.phone(v.getSdt(), true);
                    if (em.createQuery("select count(n) from NhanVien n where n.sdt = :s and n.maNV <> :id", Long.class)
                            .setParameter("s", phone).setParameter("id", id).getSingleResult() > 0) {
                        throw ManagementSupport.invalid("Số điện thoại đã thuộc nhân viên khác.");
                    }
                    if (v.getNgaySinh() != null && (v.getNgaySinh().getYear() < 1
                            || v.getNgaySinh().isAfter(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))))) {
                        throw ManagementSupport.invalid("Ngày sinh không hợp lệ.");
                    }
                    e.setSdt(phone);
                    e.setEmail(ManagementSupport.email(v.getEmail()));
                    e.setDiaChi(ManagementSupport.text(v.getDiaChi(), 255, false));
                    e.setChucVu(ManagementSupport.text(v.getChucVu(), 100, false));
                    e.setGioiTinh(v.getGioiTinh());
                    e.setNgaySinh(v.getNgaySinh());
                    e.setTrangThai(v.getTrangThai());
                }
                if (mode == 0) {
                    em.persist(e);
                }
                if (mode != 2) {
                    String user = ManagementSupport.text(v.getUsername(), 50, false);
                    if (a != null) {
                        if (!Objects.equals(user, a.getUsername())) {
                            throw ManagementSupport.invalid("Không đổi tên đăng nhập đã cấp.");
                        }
                        if (r.getInitialPassword() != null && !r.getInitialPassword().isEmpty()) {
                            throw ManagementSupport.invalid(
                                    "Mật khẩu này chỉ dùng khi tạo tài khoản mới; tài khoản hiện có dùng Đổi mật khẩu.");
                        }
                    } else if (user != null) {
                        if (!user.matches("[A-Za-z0-9_.-]{1,50}")) {
                            throw ManagementSupport.invalid("Tên đăng nhập chỉ gồm chữ không dấu, số, _, . hoặc -.");
                        }
                        if (em.find(TaiKhoan.class, user) != null) {
                            throw ManagementSupport.invalid("Tên đăng nhập đã tồn tại.");
                        }
                        if (v.getVaiTro() == null) {
                            throw ManagementSupport.invalid("Chọn vai trò tài khoản.");
                        }
                        a = new TaiKhoan();
                        a.setUsername(user);
                        a.setNhanVien(e);
                        a.setVaiTro(v.getVaiTro());
                        a.setTrangThai(v.getTaiKhoanHoatDong());
                        a.setPasswordHash(auth.hashInitialPassword(r.getInitialPassword()));
                        em.persist(a);
                    } else if (r.getInitialPassword() != null && !r.getInitialPassword().isEmpty()) {
                        throw ManagementSupport.invalid("Nhập tên đăng nhập để cấp tài khoản.");
                    }
                    if (a != null) {
                        if (v.getVaiTro() == null) {
                            throw ManagementSupport.invalid("Chọn vai trò tài khoản.");
                        }
                        a.setVaiTro(v.getVaiTro());
                        a.setTrangThai(v.getTaiKhoanHoatDong());
                    }
                }
                em.flush();
                if (em.createQuery(
                        "select count(t) from TaiKhoan t where t.trangThai = true and t.nhanVien.trangThai = true and t.vaiTro = :role",
                        Long.class).setParameter("role", VaiTro.NHAN_VIEN_QUAN_LY).getSingleResult() == 0) {
                    throw ManagementSupport.invalid("Phải còn ít nhất một tài khoản quản lý hoạt động.");
                }
                return new Change(dto(e, a), securityChanged && a != null ? a.getUsername() : null);
            });
            if (change.revoke != null) {
                auth.revokeUser(change.revoke);
            }
            return change.dto;
        }
    }

    private static TaiKhoan account(EntityManager em, String id, boolean lock) {
        TypedQuery<TaiKhoan> q = em.createQuery("select t from TaiKhoan t where t.nhanVien.maNV = :id", TaiKhoan.class)
                .setParameter("id", id);
        if (lock) {
            q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }
        List<TaiKhoan> rows = q.getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static NhanVienAccountDTO dto(NhanVien e, TaiKhoan a) {
        NhanVienAccountDTO d = new NhanVienAccountDTO();
        d.setMaNV(e.getMaNV());
        d.setHoTen(e.getHoTen());
        d.setGioiTinh(e.getGioiTinh());
        d.setNgaySinh(e.getNgaySinh());
        d.setSdt(e.getSdt());
        d.setEmail(e.getEmail());
        d.setDiaChi(e.getDiaChi());
        d.setChucVu(e.getChucVu());
        d.setTrangThai(e.getTrangThai());
        d.setUsername(a == null ? null : a.getUsername());
        d.setVaiTro(a == null ? null : a.getVaiTro());
        d.setTaiKhoanHoatDong(a != null && a.getTrangThai());
        return d;
    }

    private static final class Change {
        final NhanVienAccountDTO dto;
        final String revoke;

        Change(NhanVienAccountDTO dto, String revoke) {
            this.dto = dto;
            this.revoke = revoke;
        }
    }
}
