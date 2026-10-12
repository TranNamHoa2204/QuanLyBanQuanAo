package server.service;

import java.math.BigDecimal;
import javax.persistence.*;
import common.dto.*;
import common.protocol.JsonCodec;
import server.entity.KhuyenMai;
import server.persistence.JpaManager;
import server.repository.ManagementRepository;

public final class KhuyenMaiService {
    private final JpaManager jpa;
    private final AuthService auth;
    public KhuyenMaiService(JpaManager jpa, AuthService auth) { this.jpa = jpa; this.auth = auth; }
    public ManagementSearchResult search(String token, ManagementSearchRequest r) {
        return jpa.transaction(em -> {
            server.entity.TaiKhoan a = auth.requireAuthenticated(em, token); ManagementSupport.page(r);
            if (r.getIncludeInactive() && a.getVaiTro() != common.enums.VaiTro.NHAN_VIEN_QUAN_LY) {
                throw new BusinessException(common.protocol.ErrorCode.FORBIDDEN, "Chỉ quản lý được xem khuyến mãi ngừng áp dụng.");
            }
            ManagementRepository<KhuyenMai> repo = new ManagementRepository<>(em, KhuyenMai.class, "maKM", "tenKM", null);
            ManagementSearchResult out = new ManagementSearchResult(); out.setTotal(repo.count(r));
            for (KhuyenMai e : repo.search(r)) { out.getItems().add(JsonCodec.mapper().valueToTree(dto(e))); } return out;
        });
    }
    public KhuyenMaiDTO save(String token, ManagementSaveRequest r, int mode) {
        return jpa.transaction(em -> {
            ManagementSupport.manager(auth, em, token);
            KhuyenMaiDTO v = ManagementSupport.value(r, KhuyenMaiDTO.class);
            String id = ManagementSupport.code(v.getMaKM());
            KhuyenMai e = em.find(KhuyenMai.class, id, mode == 0 ? LockModeType.NONE : LockModeType.PESSIMISTIC_WRITE);
            if (mode == 0) {
                if (e != null) { throw ManagementSupport.invalid("Mã khuyến mãi đã tồn tại."); }
                e = new KhuyenMai(); e.setMaKM(id);
            } else { ManagementSupport.missing(e); ManagementSupport.expected(r, dto(e)); }
            if (mode == 2) { e.setTrangThai(false); }
            else {
                BigDecimal rate = v.getPhanTramGiam();
                if (rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0 || rate.stripTrailingZeros().scale() > 2) {
                    throw ManagementSupport.invalid("Tỷ lệ giảm từ 0–100%, tối đa 2 chữ số thập phân.");
                }
                if (v.getNgayBatDau() == null || v.getNgayKetThuc() == null || v.getNgayBatDau().isAfter(v.getNgayKetThuc())
                        || v.getNgayBatDau().getYear() < 1 || v.getNgayKetThuc().getYear() > 9999) {
                    throw ManagementSupport.invalid("Ngày bắt đầu/kết thúc hợp lệ, bắt đầu không sau kết thúc.");
                }
                e.setTenKM(ManagementSupport.text(v.getTenKM(), 200, true)); e.setPhanTramGiam(rate.setScale(2));
                e.setNgayBatDau(v.getNgayBatDau()); e.setNgayKetThuc(v.getNgayKetThuc()); e.setTrangThai(v.getTrangThai());
            }
            if (mode == 0) { em.persist(e); } em.flush(); return dto(e);
        });
    }
    private static KhuyenMaiDTO dto(KhuyenMai e) {
        KhuyenMaiDTO d = new KhuyenMaiDTO(); d.setMaKM(e.getMaKM()); d.setTenKM(e.getTenKM());
        d.setPhanTramGiam(e.getPhanTramGiam()); d.setNgayBatDau(e.getNgayBatDau()); d.setNgayKetThuc(e.getNgayKetThuc());
        d.setTrangThai(e.getTrangThai()); return d;
    }
}
