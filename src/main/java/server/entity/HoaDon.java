package server.entity;
import javax.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

import common.enums.TrangThaiHoaDon;
import common.enums.PhuongThucThanhToan;

import java.math.BigDecimal;

/** Entity JPA phía server; không truyền entity qua Socket. */
@Entity
@Table(name = "HoaDon", schema = "dbo")
public class HoaDon {
    @Id @Column(length = 20)
    private String maHD;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNV", nullable = false)
    private NhanVien nhanVien;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maKH")
    private KhachHang khachHang;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maKM")
    private KhuyenMai khuyenMai;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maThue", nullable = false)
    private Thue thue;
    @Column(nullable = false)
    private LocalDateTime ngayLap;
    @Column(name = "ngayThanhToan")
    private LocalDateTime ngayThanhToan;
    @Enumerated(EnumType.STRING) @Column(name = "phuongThucThanhToan", length = 20)
    private PhuongThucThanhToan phuongThucThanhToan;
    @Column(name = "tienKhachDua", precision = 18, scale = 2)
    private BigDecimal tienKhachDua;
    @Column(name = "tienThua", precision = 18, scale = 2)
    private BigDecimal tienThua;
    @Column(name = "nhanVienXacNhanChuyenKhoan")
    private Boolean nhanVienXacNhanChuyenKhoan;
    @Nationalized @Column(name = "tenNhanVienSnapshot", length = 100)
    private String tenNhanVienSnapshot;
    @Nationalized @Column(name = "tenKhachHangSnapshot", length = 100)
    private String tenKhachHangSnapshot;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal tamTinh;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal phanTramGiam;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal tienGiam;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal phanTramThue;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal tienThue;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal tongTien;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TrangThaiHoaDon trangThai = TrangThaiHoaDon.NHAP;
    @Nationalized @Column(length = 500)
    private String ghiChu;
    @Version @Column(nullable = false)
    private Long phienBan = 0L;

    public HoaDon() {
    }

    public String getMaHD() {
        return maHD;
    }

    public void setMaHD(String maHD) {
        this.maHD = maHD;
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    public void setNhanVien(NhanVien nhanVien) {
        this.nhanVien = nhanVien;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    public KhuyenMai getKhuyenMai() {
        return khuyenMai;
    }

    public void setKhuyenMai(KhuyenMai khuyenMai) {
        this.khuyenMai = khuyenMai;
    }

    public Thue getThue() {
        return thue;
    }

    public void setThue(Thue thue) {
        this.thue = thue;
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
    public void setPhuongThucThanhToan(PhuongThucThanhToan phuongThucThanhToan) { this.phuongThucThanhToan = phuongThucThanhToan; }
    public BigDecimal getTienKhachDua() { return tienKhachDua; }
    public void setTienKhachDua(BigDecimal tienKhachDua) { this.tienKhachDua = tienKhachDua; }
    public BigDecimal getTienThua() { return tienThua; }
    public void setTienThua(BigDecimal tienThua) { this.tienThua = tienThua; }
    public Boolean getNhanVienXacNhanChuyenKhoan() { return nhanVienXacNhanChuyenKhoan; }
    public void setNhanVienXacNhanChuyenKhoan(Boolean value) { this.nhanVienXacNhanChuyenKhoan = value; }

    public String getTenNhanVienSnapshot() {
        return tenNhanVienSnapshot;
    }

    public void setTenNhanVienSnapshot(String tenNhanVienSnapshot) {
        this.tenNhanVienSnapshot = tenNhanVienSnapshot;
    }

    public String getTenKhachHangSnapshot() {
        return tenKhachHangSnapshot;
    }

    public void setTenKhachHangSnapshot(String tenKhachHangSnapshot) {
        this.tenKhachHangSnapshot = tenKhachHangSnapshot;
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
