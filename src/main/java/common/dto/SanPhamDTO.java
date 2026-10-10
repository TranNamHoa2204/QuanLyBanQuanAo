package common.dto;

import java.math.BigDecimal;

/** Dữ liệu trao đổi; không phụ thuộc entity/JPA. Không ghi mật khẩu hoặc token vào log. */
public class SanPhamDTO {
    private String maSP;
    private String tenSP;
    private String maLoai;
    private String tenLoai;
    private String maThuongHieu;
    private String tenThuongHieu;
    private BigDecimal giaBan;
    private boolean trangThai = true;
    private long tongTon;

    public SanPhamDTO() {
    }
    public long getTongTon() { return tongTon; }
    public void setTongTon(long tongTon) { this.tongTon = tongTon; }

    public String getMaSP() {
        return maSP;
    }

    public void setMaSP(String maSP) {
        this.maSP = maSP;
    }

    public String getTenSP() {
        return tenSP;
    }

    public void setTenSP(String tenSP) {
        this.tenSP = tenSP;
    }

    public String getMaLoai() {
        return maLoai;
    }

    public void setMaLoai(String maLoai) {
        this.maLoai = maLoai;
    }

    public String getTenLoai() {
        return tenLoai;
    }

    public void setTenLoai(String tenLoai) {
        this.tenLoai = tenLoai;
    }

    public String getMaThuongHieu() {
        return maThuongHieu;
    }

    public void setMaThuongHieu(String maThuongHieu) {
        this.maThuongHieu = maThuongHieu;
    }

    public String getTenThuongHieu() {
        return tenThuongHieu;
    }

    public void setTenThuongHieu(String tenThuongHieu) {
        this.tenThuongHieu = tenThuongHieu;
    }

    public BigDecimal getGiaBan() {
        return giaBan;
    }

    public void setGiaBan(BigDecimal giaBan) {
        this.giaBan = giaBan;
    }

    public boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }
}
