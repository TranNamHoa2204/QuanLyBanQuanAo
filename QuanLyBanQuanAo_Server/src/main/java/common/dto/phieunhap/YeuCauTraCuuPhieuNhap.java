package common.dto.phieunhap;

import java.time.LocalDate;

import common.enums.TrangThaiPhieuNhap;

public class YeuCauTraCuuPhieuNhap {
    private String tuKhoa, maNCC;
    private LocalDate tuNgay, denNgay;
    private TrangThaiPhieuNhap trangThai;
    private int page, pageSize = 20;

    public YeuCauTraCuuPhieuNhap() {
    }

    public String getTuKhoa() {
        return tuKhoa;
    }

    public void setTuKhoa(String value) {
        this.tuKhoa = value;
    }

    public String getMaNCC() {
        return maNCC;
    }

    public void setMaNCC(String value) {
        this.maNCC = value;
    }

    public LocalDate getTuNgay() {
        return tuNgay;
    }

    public void setTuNgay(LocalDate value) {
        this.tuNgay = value;
    }

    public LocalDate getDenNgay() {
        return denNgay;
    }

    public void setDenNgay(LocalDate value) {
        this.denNgay = value;
    }

    public TrangThaiPhieuNhap getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(TrangThaiPhieuNhap value) {
        this.trangThai = value;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int value) {
        this.page = value;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int value) {
        this.pageSize = value;
    }
}
