package server.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import common.enums.TrangThaiPhieuNhap;

@Entity
@Table(name = "PhieuNhap", schema = "dbo")
public class PhieuNhap {
    @Id
    @Column(length = 20)
    private String maPhieuNhap;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNCC", nullable = false)
    private NhaCungCap nhaCungCap;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNVTao", nullable = false)
    private NhanVien nhanVienTao;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maNVXacNhan")
    private NhanVien nhanVienXacNhan;
    @Column(nullable = false)
    private LocalDateTime ngayLap;
    private LocalDateTime ngayXacNhan;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrangThaiPhieuNhap trangThai = TrangThaiPhieuNhap.NHAP;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal tongTien = BigDecimal.ZERO;
    @Column(nullable = false, length = 36, unique = true)
    private String operationIdTao;
    @Column(length = 36)
    private String operationIdXacNhan;
    private Long phienBanTruocXacNhan;
    @Version
    @Column(nullable = false)
    private Long phienBan = 0L;

    public PhieuNhap() {
    }

    public String getMaPhieuNhap() {
        return maPhieuNhap;
    }

    public void setMaPhieuNhap(String value) {
        this.maPhieuNhap = value;
    }

    public NhaCungCap getNhaCungCap() {
        return nhaCungCap;
    }

    public void setNhaCungCap(NhaCungCap value) {
        this.nhaCungCap = value;
    }

    public NhanVien getNhanVienTao() {
        return nhanVienTao;
    }

    public void setNhanVienTao(NhanVien value) {
        this.nhanVienTao = value;
    }

    public NhanVien getNhanVienXacNhan() {
        return nhanVienXacNhan;
    }

    public void setNhanVienXacNhan(NhanVien value) {
        this.nhanVienXacNhan = value;
    }

    public LocalDateTime getNgayLap() {
        return ngayLap;
    }

    public void setNgayLap(LocalDateTime value) {
        this.ngayLap = value;
    }

    public LocalDateTime getNgayXacNhan() {
        return ngayXacNhan;
    }

    public void setNgayXacNhan(LocalDateTime value) {
        this.ngayXacNhan = value;
    }

    public TrangThaiPhieuNhap getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(TrangThaiPhieuNhap value) {
        this.trangThai = value;
    }

    public BigDecimal getTongTien() {
        return tongTien;
    }

    public void setTongTien(BigDecimal value) {
        this.tongTien = value;
    }

    public String getOperationIdTao() {
        return operationIdTao;
    }

    public void setOperationIdTao(String value) {
        this.operationIdTao = value;
    }

    public String getOperationIdXacNhan() {
        return operationIdXacNhan;
    }

    public void setOperationIdXacNhan(String value) {
        this.operationIdXacNhan = value;
    }

    public Long getPhienBanTruocXacNhan() {
        return phienBanTruocXacNhan;
    }

    public void setPhienBanTruocXacNhan(Long value) {
        this.phienBanTruocXacNhan = value;
    }

    public Long getPhienBan() {
        return phienBan;
    }

    public void setPhienBan(Long value) {
        this.phienBan = value;
    }
}
