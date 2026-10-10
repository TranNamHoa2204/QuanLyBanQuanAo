package server.entity;
import javax.persistence.*;
import org.hibernate.annotations.Nationalized;


/** Entity JPA của bảng ảnh; chỉ truyền DTO/đường dẫn tương đối qua Socket. */
@Entity
@Table(name = "AnhSanPham", schema = "dbo")
public class AnhSanPham {
    @Id @Column(length = 20)
    private String maAnh;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maSP", nullable = false)
    private SanPham sanPham;
    @Nationalized @Column(nullable = false, length = 50)
    private String mauSac;
    @Column(nullable = false, length = 500, unique = true)
    private String duongDan;
    @Column(nullable = false)
    private int thuTu;
    @Column(nullable = false)
    private boolean anhChinh;

    public AnhSanPham() { }

    public String getMaAnh() { return maAnh; }
    public void setMaAnh(String maAnh) { this.maAnh = maAnh; }

    public SanPham getSanPham() { return sanPham; }
    public void setSanPham(SanPham sanPham) { this.sanPham = sanPham; }

    public String getMauSac() { return mauSac; }
    public void setMauSac(String mauSac) { this.mauSac = mauSac; }

    public String getDuongDan() { return duongDan; }
    public void setDuongDan(String duongDan) { this.duongDan = duongDan; }

    public int getThuTu() { return thuTu; }
    public void setThuTu(int thuTu) { this.thuTu = thuTu; }

    public boolean getAnhChinh() { return anhChinh; }
    public void setAnhChinh(boolean anhChinh) { this.anhChinh = anhChinh; }
}
