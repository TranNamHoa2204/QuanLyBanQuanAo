package client.controllers;

import java.io.IOException;

import client.api.GoiDichVuNhanVien;
import common.dto.nhanvien.DuLieuHoSoNhanVien;
import common.dto.nhanvien.KetQuaTraCuuNhanVien;
import common.dto.nhanvien.YeuCauLuuNhanVien;
import common.dto.nhanvien.YeuCauTaoNhanVien;
import common.dto.nhanvien.YeuCauTraCuuNhanVien;
import common.dto.nhanvien.YeuCauTrangThaiNhanVien;
import common.dto.taikhoan.DuLieuTaiKhoan;
import common.dto.taikhoan.YeuCauTaoTaiKhoan;
import common.dto.taikhoan.YeuCauTrangThaiTaiKhoan;
import common.dto.taikhoan.YeuCauVaiTroTaiKhoan;

/** Thin Swing controller; authorization and validation remain on the server. */
public final class DieuKhienNhanVien {
    private final GoiDichVuNhanVien api;

    public DieuKhienNhanVien(GoiDichVuNhanVien api) {
        this.api = api;
    }

    public KetQuaTraCuuNhanVien search(YeuCauTraCuuNhanVien request) throws IOException {
        return api.search(request);
    }

    public DuLieuHoSoNhanVien detail(String id) throws IOException {
        return api.detail(id);
    }

    public DuLieuHoSoNhanVien create(YeuCauTaoNhanVien request) throws IOException {
        return api.create(request);
    }

    public DuLieuHoSoNhanVien update(YeuCauLuuNhanVien request) throws IOException {
        return api.update(request);
    }

    public DuLieuHoSoNhanVien employeeActive(YeuCauTrangThaiNhanVien request, boolean active)
            throws IOException {
        return api.setEmployeeActive(request, active);
    }

    public DuLieuTaiKhoan createAccount(YeuCauTaoTaiKhoan request) throws IOException {
        return api.createAccount(request);
    }

    public DuLieuTaiKhoan accountActive(YeuCauTrangThaiTaiKhoan request, boolean active) throws IOException {
        return api.setAccountActive(request, active);
    }

    public DuLieuTaiKhoan role(YeuCauVaiTroTaiKhoan request) throws IOException {
        return api.changeRole(request);
    }
}
