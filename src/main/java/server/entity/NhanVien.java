package server.entity;

import java.time.LocalDate;
import javax.persistence.*;
import org.hibernate.annotations.Nationalized;

/** Entity JPA theo SQL hiện tại; không truyền entity qua Socket. */
@Entity
@Table(name = "NhanVien", schema = "dbo")
public class NhanVien {
    @Id
    @Column(length = 20)
    private String maNV;
    @Nationalized
    @Column(nullable = false, length = 100)
    private String hoTen;
    private Boolean gioiTinh;
    private LocalDate ngaySinh;
    @Column(nullable = false, length = 20, unique = true)
    private String sdt;
    @Column(length = 254)
    private String email;
    @Nationalized
    @Column(length = 255)
    private String diaChi;
    @Nationalized
    @Column(length = 100)
    private String chucVu;
    @Column(nullable = false)
    private boolean trangThai = true;

    public NhanVien() {
    }

    public String getMaNV() {
        return maNV;
    }

    public void setMaNV(String maNV) {
        this.maNV = maNV;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public Boolean getGioiTinh() {
        return gioiTinh;
    }

    public void setGioiTinh(Boolean gioiTinh) {
        this.gioiTinh = gioiTinh;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(LocalDate ngaySinh) {
        this.ngaySinh = ngaySinh;
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

    public String getChucVu() {
        return chucVu;
    }

    public void setChucVu(String chucVu) {
        this.chucVu = chucVu;
    }

    public boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }
}
