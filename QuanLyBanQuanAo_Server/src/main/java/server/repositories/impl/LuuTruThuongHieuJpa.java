package server.repositories.impl;

import java.util.List;

import javax.persistence.EntityManager;

import server.entities.ThuongHieu;
import server.repositories.LuuTruThuongHieu;

public final class LuuTruThuongHieuJpa extends LuuTruCoBanJpa<ThuongHieu, String>
        implements LuuTruThuongHieu {
    public LuuTruThuongHieuJpa(EntityManager em) {
        super(em, ThuongHieu.class);
    }

    public List<ThuongHieu> ordered() {
        return em.createQuery("select d from ThuongHieu d order by d.tenThuongHieu, d.maThuongHieu",
                ThuongHieu.class).setMaxResults(1001).getResultList();
    }
}
