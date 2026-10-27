package server.service;

import javax.persistence.*;
import common.dto.*;
import common.protocol.JsonCodec;
import server.entity.KhachHang;
import server.persistence.JpaManager;
import server.repository.ManagementRepository;

public final class KhachHangService {
    private final JpaManager jpa;
    private final AuthService auth;

    public KhachHangService(JpaManager jpa, AuthService auth) {
        this.jpa = jpa;
        this.auth = auth;
    }

    public ManagementSearchResult search(String token, ManagementSearchRequest r) {
        return jpa.transaction(em -> {
            auth.requireAuthenticated(em, token);
            ManagementSupport.page(r);
            ManagementRepository<KhachHang> repo = new ManagementRepository<>(em, KhachHang.class, "maKH", "hoTen",
                    "sdt");
            ManagementSearchResult out = new ManagementSearchResult();
            out.setTotal(repo.count(r));
            for (KhachHang e : repo.search(r)) {
                out.getItems().add(JsonCodec.mapper().valueToTree(dto(e)));
            }
            return out;
        });
    }

    /** mode 0: thêm, 1: sửa/kích hoạt lại, 2: ngừng hoạt động. */
    public KhachHangDTO save(String token, ManagementSaveRequest r, int mode) {
        return jpa.transaction(em -> {
            auth.requireAuthenticated(em, token);
            KhachHangDTO v = ManagementSupport.value(r, KhachHangDTO.class);
            String id = ManagementSupport.code(v.getMaKH());
            KhachHang e = em.find(KhachHang.class, id, mode == 0 ? LockModeType.NONE : LockModeType.PESSIMISTIC_WRITE);
            if (mode == 0) {
                if (e != null) {
                    throw ManagementSupport.invalid("Mã khách hàng đã tồn tại.");
                }
                e = new KhachHang();
                e.setMaKH(id);
            } else {
                ManagementSupport.missing(e);
                ManagementSupport.expected(r, dto(e));
            }
            if (mode == 2) {
                e.setTrangThai(false);
            } else {
                String phone = ManagementSupport.phone(v.getSdt(), false);
                if (phone != null && em
                        .createQuery("select count(k) from KhachHang k where k.sdt = :s and k.maKH <> :id", Long.class)
                        .setParameter("s", phone).setParameter("id", id).getSingleResult() > 0) {
                    throw ManagementSupport.invalid("Số điện thoại đã thuộc khách hàng khác.");
                }
                e.setHoTen(ManagementSupport.text(v.getHoTen(), 100, true));
                e.setSdt(phone);
                e.setEmail(ManagementSupport.email(v.getEmail()));
                e.setDiaChi(ManagementSupport.text(v.getDiaChi(), 255, false));
                e.setTrangThai(v.getTrangThai());
            }
            if (mode == 0) {
                em.persist(e);
            }
            em.flush();
            return dto(e);
        });
    }

    private static KhachHangDTO dto(KhachHang e) {
        KhachHangDTO d = new KhachHangDTO();
        d.setMaKH(e.getMaKH());
        d.setHoTen(e.getHoTen());
        d.setSdt(e.getSdt());
        d.setEmail(e.getEmail());
        d.setDiaChi(e.getDiaChi());
        d.setTrangThai(e.getTrangThai());
        return d;
    }
}
