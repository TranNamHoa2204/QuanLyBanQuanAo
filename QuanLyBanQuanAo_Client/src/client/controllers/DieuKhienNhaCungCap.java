package client.controllers;

import java.io.IOException;

import client.api.GoiDichVuNhaCungCap;
import common.dto.nhacungcap.DuLieuNhaCungCap;
import common.dto.nhacungcap.KetQuaTraCuuNhaCungCap;
import common.dto.nhacungcap.YeuCauLuuNhaCungCap;
import common.dto.nhacungcap.YeuCauTraCuuNhaCungCap;
import common.dto.nhacungcap.YeuCauTrangThaiNhaCungCap;

public final class DieuKhienNhaCungCap {
    private final GoiDichVuNhaCungCap api;

    public DieuKhienNhaCungCap(GoiDichVuNhaCungCap api) {
        this.api = api;
    }

    public KetQuaTraCuuNhaCungCap search(YeuCauTraCuuNhaCungCap r) throws IOException {
        return api.search(r);
    }

    public DuLieuNhaCungCap create(DuLieuNhaCungCap r) throws IOException {
        return api.create(r);
    }

    public DuLieuNhaCungCap update(YeuCauLuuNhaCungCap r) throws IOException {
        return api.update(r);
    }

    public DuLieuNhaCungCap setActive(YeuCauTrangThaiNhaCungCap r, boolean active) throws IOException {
        return api.setActive(r, active);
    }
}
