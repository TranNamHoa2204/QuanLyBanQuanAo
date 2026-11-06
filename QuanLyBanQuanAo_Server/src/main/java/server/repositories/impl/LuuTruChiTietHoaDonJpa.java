package server.repositories.impl;

import java.util.List;

import javax.persistence.EntityManager;

import server.entities.ChiTietHoaDon;
import server.repositories.LuuTruChiTietHoaDon;

public final class LuuTruChiTietHoaDonJpa extends LuuTruCoBanJpa<ChiTietHoaDon, Long>
        implements LuuTruChiTietHoaDon {
    public LuuTruChiTietHoaDonJpa(EntityManager em) {
        super(em, ChiTietHoaDon.class);
    }

    public List<ChiTietHoaDon> byInvoice(String id) {
        return em
                .createQuery("select c from ChiTietHoaDon c join fetch c.bienThe b join fetch b.sanPham"
                        + " where c.hoaDon.maHD = :id order by b.maBienThe", ChiTietHoaDon.class)
                .setParameter("id", id).setMaxResults(201).getResultList();
    }
}
