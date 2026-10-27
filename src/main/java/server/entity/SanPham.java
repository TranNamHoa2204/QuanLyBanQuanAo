package server.entity;

import javax.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

/** Entity JPA theo SQL hiện tại; không truyền entity qua Socket. */
@Entity
@Table(name = "SanPham", schema = "dbo")
public class SanPham {
    @Id
    @Column(length = 20)
    private String maSP;
    @Nationalized
    @Column(nullable = false, length = 200)
    private String tenSP;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maLoai", nullable = false)
    private LoaiSanPham loaiSanPham;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maThuongHieu", nullable = false)
    private ThuongHieu thuongHieu;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal giaBan;
    @Column(nullable = false)
    private boolean trangThai = true;

    public SanPham() {
    }

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

    public LoaiSanPham getLoaiSanPham() {
        return loaiSanPham;
    }

    public void setLoaiSanPham(LoaiSanPham loaiSanPham) {
        this.loaiSanPham = loaiSanPham;
    }

    public ThuongHieu getThuongHieu() {
        return thuongHieu;
    }

    public void setThuongHieu(ThuongHieu thuongHieu) {
        this.thuongHieu = thuongHieu;
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
