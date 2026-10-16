package common.dto;

import common.enums.VaiTro;

/** Bộ lọc danh sách nhân viên; không chứa thông tin xác thực. */
public class NhanVienSearchRequest {
    private String tuKhoa;
    private Boolean trangThai;
    private Boolean coTaiKhoan;
    private Boolean taiKhoanHoatDong;
    private VaiTro vaiTro;
    private int page;
    private int pageSize = 20;

    public NhanVienSearchRequest() { }
    public String getTuKhoa() { return tuKhoa; }
    public void setTuKhoa(String tuKhoa) { this.tuKhoa = tuKhoa; }
    public Boolean getTrangThai() { return trangThai; }
    public void setTrangThai(Boolean trangThai) { this.trangThai = trangThai; }
    public Boolean getCoTaiKhoan() { return coTaiKhoan; }
    public void setCoTaiKhoan(Boolean coTaiKhoan) { this.coTaiKhoan = coTaiKhoan; }
    public Boolean getTaiKhoanHoatDong() { return taiKhoanHoatDong; }
    public void setTaiKhoanHoatDong(Boolean taiKhoanHoatDong) { this.taiKhoanHoatDong = taiKhoanHoatDong; }
    public VaiTro getVaiTro() { return vaiTro; }
    public void setVaiTro(VaiTro vaiTro) { this.vaiTro = vaiTro; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
