package common.dto;

import java.math.BigDecimal;
import common.enums.PhuongThucThanhToan;

/** Client submits the amount it reviewed; the server recalculates and validates payment. */
public class HoaDonThanhToanRequest {
    private String maHD;
    private Long phienBan;
    private BigDecimal tongTienDaXem;
    private PhuongThucThanhToan phuongThucThanhToan;
    private BigDecimal tienKhachDua;
    private Boolean nhanVienXacNhanChuyenKhoan;

    public HoaDonThanhToanRequest() { }
    public String getMaHD() { return maHD; }
    public void setMaHD(String maHD) { this.maHD = maHD; }
    public Long getPhienBan() { return phienBan; }
    public void setPhienBan(Long phienBan) { this.phienBan = phienBan; }
    public BigDecimal getTongTienDaXem() { return tongTienDaXem; }
    public void setTongTienDaXem(BigDecimal tongTienDaXem) { this.tongTienDaXem = tongTienDaXem; }
    public PhuongThucThanhToan getPhuongThucThanhToan() { return phuongThucThanhToan; }
    public void setPhuongThucThanhToan(PhuongThucThanhToan phuongThucThanhToan) { this.phuongThucThanhToan = phuongThucThanhToan; }
    public BigDecimal getTienKhachDua() { return tienKhachDua; }
    public void setTienKhachDua(BigDecimal tienKhachDua) { this.tienKhachDua = tienKhachDua; }
    public Boolean getNhanVienXacNhanChuyenKhoan() { return nhanVienXacNhanChuyenKhoan; }
    public void setNhanVienXacNhanChuyenKhoan(Boolean nhanVienXacNhanChuyenKhoan) { this.nhanVienXacNhanChuyenKhoan = nhanVienXacNhanChuyenKhoan; }
}
