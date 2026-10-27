package server.entity;

import javax.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

/** Entity JPA phía server; không truyền entity qua Socket. */
@Entity
@Table(name = "Thue", schema = "dbo")
public class Thue {
    @Id
    @Column(length = 20)
    private String maThue;
    @Nationalized
    @Column(nullable = false, length = 100)
    private String tenThue;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal phanTram;
    @Nationalized
    @Column(length = 255)
    private String moTa;
    @Column(nullable = false)
    private boolean trangThai = true;

    public Thue() {
    }

    public String getMaThue() {
        return maThue;
    }

    public void setMaThue(String maThue) {
        this.maThue = maThue;
    }

    public String getTenThue() {
        return tenThue;
    }

    public void setTenThue(String tenThue) {
        this.tenThue = tenThue;
    }

    public BigDecimal getPhanTram() {
        return phanTram;
    }

    public void setPhanTram(BigDecimal phanTram) {
        this.phanTram = phanTram;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }
}
