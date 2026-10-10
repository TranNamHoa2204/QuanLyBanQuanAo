package server.entity;
import javax.persistence.*;
import org.hibernate.annotations.Nationalized;

/** Entity JPA theo SQL hiện tại; không truyền entity qua Socket. */
@Entity
@Table(name = "LoaiSanPham", schema = "dbo")
public class LoaiSanPham {
    @Id @Column(length = 20)
    private String maLoai;
    @Nationalized @Column(nullable = false, length = 100, unique = true)
    private String tenLoai;

    public LoaiSanPham() {
    }

    public String getMaLoai() {
        return maLoai;
    }

    public void setMaLoai(String maLoai) {
        this.maLoai = maLoai;
    }

    public String getTenLoai() {
        return tenLoai;
    }

    public void setTenLoai(String tenLoai) {
        this.tenLoai = tenLoai;
    }
}
