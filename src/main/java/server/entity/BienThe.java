package server.entity;
import javax.persistence.*;
import org.hibernate.annotations.Nationalized;

/** Entity JPA theo SQL hiện tại; không truyền entity qua Socket. */
@Entity
@Table(name = "BienThe", schema = "dbo", uniqueConstraints = @UniqueConstraint(columnNames = {"maSP", "mauSac", "kichCo"}))
public class BienThe {
    @Id @Column(length = 20)
    private String maBienThe;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maSP", nullable = false)
    private SanPham sanPham;
    @Nationalized @Column(nullable = false, length = 50)
    private String mauSac;
    @Nationalized @Column(nullable = false, length = 20)
    private String kichCo;
    @Column(nullable = false)
    private int soLuongTon;
    @Column(nullable = false)
    private boolean trangThai = true;
    @Version @Column(nullable = false)
    private Long phienBan = 0L;

    public BienThe() {
    }

    public String getMaBienThe() {
        return maBienThe;
    }

    public void setMaBienThe(String maBienThe) {
        this.maBienThe = maBienThe;
    }

    public SanPham getSanPham() {
        return sanPham;
    }

    public void setSanPham(SanPham sanPham) {
        this.sanPham = sanPham;
    }

    public String getMauSac() {
        return mauSac;
    }

    public void setMauSac(String mauSac) {
        this.mauSac = mauSac;
    }

    public String getKichCo() {
        return kichCo;
    }

    public void setKichCo(String kichCo) {
        this.kichCo = kichCo;
    }

    public int getSoLuongTon() {
        return soLuongTon;
    }

    public void setSoLuongTon(int soLuongTon) {
        this.soLuongTon = soLuongTon;
    }

    public boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }

    public Long getPhienBan() {
        return phienBan;
    }

    public void setPhienBan(Long phienBan) {
        this.phienBan = phienBan;
    }
}
