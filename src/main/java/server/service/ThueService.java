package server.service;

import java.math.BigDecimal;
import java.util.*;
import javax.persistence.*;
import com.fasterxml.jackson.databind.JsonNode;
import common.dto.*;
import common.enums.VaiTro;
import common.protocol.*;
import server.entity.Thue;
import server.persistence.JpaManager;
import server.repository.ManagementRepository;

/** Cấu hình thuế chỉ quản lý; không xóa cấu hình đã được hóa đơn tham chiếu. */
public final class ThueService {
    private final JpaManager jpa;
    private final AuthService auth;
    public ThueService(JpaManager jpa, AuthService auth) { this.jpa = jpa; this.auth = auth; }
    public ManagementSearchResult search(String token, ManagementSearchRequest r) {
        return jpa.transaction(em -> {
            TaiKhoanRole.requireManager(auth, em, token);
            ManagementSupport.page(r);
            ManagementRepository<Thue> repo = new ManagementRepository<>(em, Thue.class, "maThue", "tenThue", "moTa");
            ManagementSearchResult result = new ManagementSearchResult(); result.setTotal(repo.count(r));
            for (Thue row : repo.search(r)) { result.getItems().add(JsonCodec.mapper().valueToTree(dto(row))); }
            return result;
        });
    }
    public ThueDTO save(String token, ManagementSaveRequest r, int mode) {
        return jpa.transaction(em -> {
            TaiKhoanRole.requireManager(auth, em, token);
            if (r == null || r.getValue() == null || !r.getValue().isObject()) { throw ManagementSupport.invalid("Thiếu dữ liệu thuế."); }
            ThueDTO v = JsonCodec.mapper().convertValue(r.getValue(), ThueDTO.class);
            String id = ManagementSupport.code(v.getMaThue());
            Thue entity = em.find(Thue.class, id, mode == 0 ? LockModeType.NONE : LockModeType.PESSIMISTIC_WRITE);
            if (mode == 0) {
                if (entity != null) { throw ManagementSupport.invalid("Mã thuế đã tồn tại."); }
                entity = new Thue(); entity.setMaThue(id);
            } else {
                ManagementSupport.missing(entity);
                if (r.getExpected() == null || !same(r.getExpected(), JsonCodec.mapper().valueToTree(dto(entity)))) {
                    throw new BusinessException(ErrorCode.VERSION_CONFLICT, "Cấu hình thuế đã đổi. Tải lại trước khi lưu.");
                }
            }
            if (mode == 2) { entity.setTrangThai(false); }
            else {
                String name = ManagementSupport.text(v.getTenThue(), 100, true);
                BigDecimal rate = v.getPhanTram();
                if (rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0
                        || rate.stripTrailingZeros().scale() > 2) {
                    throw ManagementSupport.invalid("Tỷ lệ thuế từ 0–100%, tối đa 2 chữ số thập phân.");
                }
                Long duplicate = em.createQuery("select count(t) from Thue t where lower(t.tenThue) = :name and t.maThue <> :id", Long.class)
                        .setParameter("name", name.toLowerCase(Locale.ROOT)).setParameter("id", id).getSingleResult();
                if (duplicate > 0) { throw ManagementSupport.invalid("Tên cấu hình thuế đã tồn tại."); }
                entity.setTenThue(name); entity.setPhanTram(rate.setScale(2));
                entity.setMoTa(ManagementSupport.text(v.getMoTa(), 255, false)); entity.setTrangThai(v.getTrangThai());
            }
            if (mode == 0) { em.persist(entity); }
            em.flush();
            return dto(entity);
        });
    }
    private static ThueDTO dto(Thue row) {
        ThueDTO d = new ThueDTO(); d.setMaThue(row.getMaThue()); d.setTenThue(row.getTenThue());
        d.setPhanTram(row.getPhanTram()); d.setMoTa(row.getMoTa()); d.setTrangThai(row.getTrangThai()); return d;
    }
    private static boolean same(JsonNode a, JsonNode b) {
        if (a == null || b == null) { return a == b; }
        if (a.isNumber() && b.isNumber()) { return a.decimalValue().compareTo(b.decimalValue()) == 0; }
        if (a.isObject() && b.isObject()) {
            if (a.size() != b.size()) { return false; }
            Iterator<String> it = a.fieldNames();
            while (it.hasNext()) { String key = it.next(); if (!same(a.get(key), b.get(key))) { return false; } }
            return true;
        }
        return a.equals(b);
    }
    private static final class TaiKhoanRole {
        static void requireManager(AuthService auth, EntityManager em, String token) {
            if (auth.requireAuthenticated(em, token).getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ nhân viên quản lý được cấu hình thuế.");
            }
        }
    }
}
