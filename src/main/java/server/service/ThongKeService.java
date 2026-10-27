package server.service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.persistence.*;
import common.dto.*;
import common.enums.TrangThaiHoaDon;
import server.persistence.JpaManager;

/** Aggregate tại database; không tải toàn bộ hóa đơn về client. */
public final class ThongKeService {
    private final JpaManager jpa;
    private final AuthService auth;

    public ThongKeService(JpaManager jpa, AuthService auth) {
        this.jpa = jpa;
        this.auth = auth;
    }

    public RevenueResult report(String token, RevenueRequest r, boolean products) {
        return jpa.transaction(em -> {
            ManagementSupport.manager(auth, em, token);
            if (r == null || r.getTuNgay() == null || r.getDenNgay() == null || r.getGroup() == null
                    || r.getTuNgay().getYear() < 1 || r.getDenNgay().getYear() > 9998
                    || r.getTuNgay().isAfter(r.getDenNgay())
                    || ChronoUnit.DAYS.between(r.getTuNgay(), r.getDenNgay()) > 3660 || r.getPage() < 0
                    || r.getPage() > 1000000 || r.getPageSize() < 1 || r.getPageSize() > 100) {
                throw ManagementSupport
                        .invalid("Chọn khoảng ngày hợp lệ (tối đa 3.660 ngày) và phân trang 1–100 dòng.");
            }
            String where = " where h.trangThai = :paid and h.ngayLap >= :from and h.ngayLap < :to";
            Object[] summary = (Object[]) bind(em.createQuery(
                    "select count(h), sum(h.tamTinh), sum(h.tienGiam), sum(h.tienThue), sum(h.tongTien) from HoaDon h"
                            + where),
                    r).getSingleResult();
            RevenueResult out = new RevenueResult();
            out.setSoHoaDon(((Number) summary[0]).longValue());
            out.setTamTinh(amount(summary[1]));
            out.setTienGiam(amount(summary[2]));
            out.setTienThue(amount(summary[3]));
            out.setTongTien(amount(summary[4]));
            String select, from, group, order;
            if (products) {
                select = "p.maSP, p.tenSP, sum(cast(c.soLuong as long)), sum(c.donGia * c.soLuong)";
                from = " from ChiTietHoaDon c join c.hoaDon h join c.bienThe b join b.sanPham p";
                group = "p.maSP, p.tenSP";
                order = "sum(c.donGia * c.soLuong) desc, p.maSP";
            } else {
                group = "year(h.ngayLap)";
                if (r.getGroup() != RevenueRequest.Group.YEAR) {
                    group += ", month(h.ngayLap)";
                }
                if (r.getGroup() == RevenueRequest.Group.DAY) {
                    group += ", day(h.ngayLap)";
                }
                select = group + ", count(h), sum(h.tongTien)";
                from = " from HoaDon h";
                order = group;
            }
            @SuppressWarnings("unchecked")
            List<Object[]> rows = bind(
                    em.createQuery("select " + select + from + where + " group by " + group + " order by " + order), r)
                    .setFirstResult(r.getPage() * r.getPageSize()).setMaxResults(r.getPageSize() + 1).getResultList();
            out.setHasNext(rows.size() > r.getPageSize());
            for (int i = 0; i < Math.min(rows.size(), r.getPageSize()); i++) {
                Object[] row = rows.get(i);
                RevenueResult.Row d = new RevenueResult.Row();
                if (products) {
                    d.setCode((String) row[0]);
                    d.setLabel((String) row[1]);
                } else {
                    String label = String.format(java.util.Locale.ROOT, "%04d", ((Number) row[0]).intValue());
                    if (r.getGroup() != RevenueRequest.Group.YEAR) {
                        label += String.format(java.util.Locale.ROOT, "-%02d", ((Number) row[1]).intValue());
                    }
                    if (r.getGroup() == RevenueRequest.Group.DAY) {
                        label += String.format(java.util.Locale.ROOT, "-%02d", ((Number) row[2]).intValue());
                    }
                    d.setCode(label);
                    d.setLabel(label);
                }
                d.setQuantity(((Number) row[row.length - 2]).longValue());
                d.setAmount(amount(row[row.length - 1]));
                out.getItems().add(d);
            }
            return out;
        });
    }

    private static Query bind(Query query, RevenueRequest r) {
        return query.setParameter("paid", TrangThaiHoaDon.DA_THANH_TOAN)
                .setParameter("from", r.getTuNgay().atStartOfDay())
                .setParameter("to", r.getDenNgay().plusDays(1).atStartOfDay());
    }

    private static BigDecimal amount(Object value) {
        return value == null ? new BigDecimal("0.00") : ((BigDecimal) value).setScale(2);
    }
}
