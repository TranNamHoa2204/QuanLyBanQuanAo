package common.dto;

import java.time.LocalDateTime;

import common.enums.TrangThaiHoaDon;
import common.enums.NguonDuLieuHoaDon;
import common.enums.PhuongThucThanhToan;

import java.math.BigDecimal;

/** Dữ liệu trao đổi; không phụ thuộc entity/JPA. Không ghi mật khẩu hoặc token vào log. */
public class HoaDonDTO {
    private String maHD;
    private String maNV;
    private String maKH;
    private String maKM;
    private String maThue;
    private LocalDateTime ngayLap;
    private LocalDateTime ngayThanhToan;
    private PhuongThucThanhToan phuongThucThanhToan;
    private BigDecimal tienKhachDua;
    private BigDecimal tienThua;
    private Boolean nhanVienXacNhanChuyenKhoan;
    private String tenNhanVien;
    private String tenKhachHang;
    private NguonDuLieuHoaDon nguonDuLieuHienThi;
    private BigDecimal tamTinh;
    private BigDecimal phanTramGiam;
    private BigDecimal tienGiam;
    private BigDecimal phanTramThue;
    private BigDecimal tienThue;
    private BigDecimal tongTien;
    private TrangThaiHoaDon trangThai = TrangThaiHoaDon.NHAP;
    private String ghiChu;
    private Long phienBan = 0L;

    public HoaDonDTO() {
    }

    public String getMaHD() {
        return maHD;
    }

    public void setMaHD(String maHD) {
        this.maHD = maHD;
    }

    public String getMaNV() {
        return maNV;
    }

    public void setMaNV(String maNV) {
        this.maNV = maNV;
    }

    public String getMaKH() {
        return maKH;
    }

    public void setMaKH(String maKH) {
        this.maKH = maKH;
    }

    public String getMaKM() {
        return maKM;
    }

    public void setMaKM(String maKM) {
        this.maKM = maKM;
    }

    public String getMaThue() {
        return maThue;
    }

    public void setMaThue(String maThue) {
        this.maThue = maThue;
    }

    public LocalDateTime getNgayLap() {
        return ngayLap;
    }

    public void setNgayLap(LocalDateTime ngayLap) {
        this.ngayLap = ngayLap;
    }

    public LocalDateTime getNgayThanhToan() {
        return ngayThanhToan;
    }

    public void setNgayThanhToan(LocalDateTime ngayThanhToan) {
        this.ngayThanhToan = ngayThanhToan;
    }

    public PhuongThucThanhToan getPhuongThucThanhToan() { return phuongThucThanhToan; }
    public void setPhuongThucThanhToan(PhuongThucThanhToan value) { this.phuongThucThanhToan = value; }
    public BigDecimal getTienKhachDua() { return tienKhachDua; }
    public void setTienKhachDua(BigDecimal value) { this.tienKhachDua = value; }
    public BigDecimal getTienThua() { return tienThua; }
    public void setTienThua(BigDecimal value) { this.tienThua = value; }
    public Boolean getNhanVienXacNhanChuyenKhoan() { return nhanVienXacNhanChuyenKhoan; }
    public void setNhanVienXacNhanChuyenKhoan(Boolean value) { this.nhanVienXacNhanChuyenKhoan = value; }

    public String getTenNhanVien() {
        return tenNhanVien;
    }

    public void setTenNhanVien(String tenNhanVien) {
        this.tenNhanVien = tenNhanVien;
    }

    public String getTenKhachHang() {
        return tenKhachHang;
    }

    public void setTenKhachHang(String tenKhachHang) {
        this.tenKhachHang = tenKhachHang;
    }

    public NguonDuLieuHoaDon getNguonDuLieuHienThi() {
        return nguonDuLieuHienThi;
    }

    public void setNguonDuLieuHienThi(NguonDuLieuHoaDon nguonDuLieuHienThi) {
        this.nguonDuLieuHienThi = nguonDuLieuHienThi;
    }

    public BigDecimal getTamTinh() {
        return tamTinh;
    }

    public void setTamTinh(BigDecimal tamTinh) {
        this.tamTinh = tamTinh;
    }

    public BigDecimal getPhanTramGiam() {
        return phanTramGiam;
    }

    public void setPhanTramGiam(BigDecimal phanTramGiam) {
        this.phanTramGiam = phanTramGiam;
    }

    public BigDecimal getTienGiam() {
        return tienGiam;
    }

    public void setTienGiam(BigDecimal tienGiam) {
        this.tienGiam = tienGiam;
    }

    public BigDecimal getPhanTramThue() {
        return phanTramThue;
    }

    public void setPhanTramThue(BigDecimal phanTramThue) {
        this.phanTramThue = phanTramThue;
    }

    public BigDecimal getTienThue() {
        return tienThue;
    }

    public void setTienThue(BigDecimal tienThue) {
        this.tienThue = tienThue;
    }

    public BigDecimal getTongTien() {
        return tongTien;
    }

    public void setTongTien(BigDecimal tongTien) {
        this.tongTien = tongTien;
    }

    public TrangThaiHoaDon getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(TrangThaiHoaDon trangThai) {
        this.trangThai = trangThai;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public Long getPhienBan() {
        return phienBan;
    }

    public void setPhienBan(Long phienBan) {
        this.phienBan = phienBan;
    }
}
