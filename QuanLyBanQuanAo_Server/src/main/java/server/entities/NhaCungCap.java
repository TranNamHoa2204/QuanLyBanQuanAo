package server.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "NhaCungCap", schema = "dbo")
public class NhaCungCap {
    @Id
    @Column(length = 20)
    private String maNCC;
    @Nationalized
    @Column(nullable = false, length = 150)
    private String tenNCC;
    @Column(length = 20)
    private String sdt;
    @Column(length = 254)
    private String email;
    @Nationalized
    @Column(length = 255)
    private String diaChi;
    @Column(nullable = false)
    private boolean trangThai = true;
    @Version
    @Column(nullable = false)
    private Long phienBan = 0L;

    public NhaCungCap() {
    }

    public String getMaNCC() {
        return maNCC;
    }

    public void setMaNCC(String value) {
        this.maNCC = value;
    }

    public String getTenNCC() {
        return tenNCC;
    }

    public void setTenNCC(String value) {
        this.tenNCC = value;
    }

    public String getSdt() {
        return sdt;
    }

    public void setSdt(String value) {
        this.sdt = value;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        this.email = value;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String value) {
        this.diaChi = value;
    }

    public boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean value) {
        this.trangThai = value;
    }

    public Long getPhienBan() {
        return phienBan;
    }

    public void setPhienBan(Long value) {
        this.phienBan = value;
    }
}
