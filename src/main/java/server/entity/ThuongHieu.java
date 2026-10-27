package server.entity;

import javax.persistence.*;
import org.hibernate.annotations.Nationalized;

/** Entity JPA theo SQL hiện tại; không truyền entity qua Socket. */
@Entity
@Table(name = "ThuongHieu", schema = "dbo")
public class ThuongHieu {
    @Id
    @Column(length = 20)
    private String maThuongHieu;
    @Nationalized
    @Column(nullable = false, length = 100, unique = true)
    private String tenThuongHieu;

    public ThuongHieu() {
    }

    public String getMaThuongHieu() {
        return maThuongHieu;
    }

    public void setMaThuongHieu(String maThuongHieu) {
        this.maThuongHieu = maThuongHieu;
    }

    public String getTenThuongHieu() {
        return tenThuongHieu;
    }

    public void setTenThuongHieu(String tenThuongHieu) {
        this.tenThuongHieu = tenThuongHieu;
    }
}
