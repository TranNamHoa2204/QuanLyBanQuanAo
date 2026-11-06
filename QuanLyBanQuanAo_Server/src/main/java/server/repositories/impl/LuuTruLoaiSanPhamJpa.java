package server.repositories.impl;

import java.util.List;

import javax.persistence.EntityManager;

import server.entities.LoaiSanPham;
import server.repositories.LuuTruLoaiSanPham;

public final class LuuTruLoaiSanPhamJpa extends LuuTruCoBanJpa<LoaiSanPham, String>
        implements LuuTruLoaiSanPham {
    public LuuTruLoaiSanPhamJpa(EntityManager em) {
        super(em, LoaiSanPham.class);
    }

    public List<LoaiSanPham> ordered() {
        return em.createQuery("select d from LoaiSanPham d order by d.tenLoai, d.maLoai", LoaiSanPham.class)
                .setMaxResults(1001).getResultList();
    }
}
