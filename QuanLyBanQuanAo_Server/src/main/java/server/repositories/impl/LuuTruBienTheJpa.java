package server.repositories.impl;

import java.util.List;

import javax.persistence.EntityManager;

import server.entities.BienThe;
import server.repositories.LuuTruBienThe;

public final class LuuTruBienTheJpa extends LuuTruCoBanJpa<BienThe, String> implements LuuTruBienThe {
    public LuuTruBienTheJpa(EntityManager em) {
        super(em, BienThe.class);
    }

    public List<BienThe> byProduct(String id, boolean includeInactive) {
        return em
                .createQuery("select b from BienThe b where b.sanPham.maSP = :id"
                        + (includeInactive ? "" : " and b.trangThai = true")
                        + " order by b.mauSac, b.kichCo, b.maBienThe", BienThe.class)
                .setParameter("id", id).setMaxResults(501).getResultList();
    }
}
