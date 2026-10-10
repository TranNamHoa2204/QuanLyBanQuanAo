package server.entity;
import javax.persistence.*;
import org.hibernate.annotations.Nationalized;

/** Entity JPA phía server; không truyền entity qua Socket. */
@Entity
@Table(name = "KhachHang", schema = "dbo")
public class KhachHang {
    @Id @Column(length = 20)
    private String maKH;
    @Nationalized @Column(nullable = false, length = 100)
    private String hoTen;
    @Column(length = 20)
    private String sdt;
    @Column(length = 254)
    private String email;
    @Nationalized @Column(length = 255)
    private String diaChi;
    @Column(nullable = false)
    private boolean trangThai = true;

    public KhachHang() {
    }

    public String getMaKH() {
        return maKH;
    }

    public void setMaKH(String maKH) {
        this.maKH = maKH;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getSdt() {
        return sdt;
    }

    public void setSdt(String sdt) {
        this.sdt = sdt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }
}
