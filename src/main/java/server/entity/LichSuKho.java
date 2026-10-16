package server.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.Nationalized;
import common.enums.NguonBienDongKho;

/** Ledger append-only. JPA chỉ ghi dòng mới; không sửa hoặc xóa lịch sử. */
@Entity
@Table(name = "LichSuKho", schema = "dbo",
        uniqueConstraints = @UniqueConstraint(name = "UQ_LichSuKho_OperationId", columnNames = "operationId"),
        indexes = {
            @Index(name = "IX_LichSuKho_BienThe_ThoiGian", columnList = "maBienThe, thoiGian, maLichSuKho"),
            @Index(name = "IX_LichSuKho_Nguon_ThoiGian", columnList = "nguon, thoiGian, maLichSuKho"),
            @Index(name = "IX_LichSuKho_NhanVien_ThoiGian", columnList = "maNV, thoiGian, maLichSuKho")
        })
public class LichSuKho {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maLichSuKho")
    private Long maLichSuKho;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maBienThe", nullable = false)
    private BienThe bienThe;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maNV")
    private NhanVien nguoiThaoTac;
    @Column(nullable = false)
    private LocalDateTime thoiGian;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private NguonBienDongKho nguon;
    @Column(nullable = false, length = 100)
    private String thamChieu;
    @Column(nullable = false, length = 100)
    private String operationId;
    @Column(nullable = false)
    private int soLuongTruoc;
    @Column(nullable = false)
    private int soLuongThayDoi;
    @Column(nullable = false)
    private int soLuongSau;
    @Column
    private Long phienBanTruoc;
    @Nationalized @Column(length = 100)
    private String tenNguoiThaoTacSnapshot;
    @Nationalized @Column(nullable = false, length = 500)
    private String lyDo;

    public LichSuKho() { }
    public Long getMaLichSuKho() { return maLichSuKho; }
    public void setMaLichSuKho(Long maLichSuKho) { this.maLichSuKho = maLichSuKho; }
    public BienThe getBienThe() { return bienThe; }
    public void setBienThe(BienThe bienThe) { this.bienThe = bienThe; }
    public NhanVien getNguoiThaoTac() { return nguoiThaoTac; }
    public void setNguoiThaoTac(NhanVien nguoiThaoTac) { this.nguoiThaoTac = nguoiThaoTac; }
    public LocalDateTime getThoiGian() { return thoiGian; }
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }
    public NguonBienDongKho getNguon() { return nguon; }
    public void setNguon(NguonBienDongKho nguon) { this.nguon = nguon; }
    public String getThamChieu() { return thamChieu; }
    public void setThamChieu(String thamChieu) { this.thamChieu = thamChieu; }
    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }
    public int getSoLuongTruoc() { return soLuongTruoc; }
    public void setSoLuongTruoc(int soLuongTruoc) { this.soLuongTruoc = soLuongTruoc; }
    public int getSoLuongThayDoi() { return soLuongThayDoi; }
    public void setSoLuongThayDoi(int soLuongThayDoi) { this.soLuongThayDoi = soLuongThayDoi; }
    public int getSoLuongSau() { return soLuongSau; }
    public void setSoLuongSau(int soLuongSau) { this.soLuongSau = soLuongSau; }
    public Long getPhienBanTruoc() { return phienBanTruoc; }
    public void setPhienBanTruoc(Long phienBanTruoc) { this.phienBanTruoc = phienBanTruoc; }
    public String getTenNguoiThaoTacSnapshot() { return tenNguoiThaoTacSnapshot; }
    public void setTenNguoiThaoTacSnapshot(String value) { this.tenNguoiThaoTacSnapshot = value; }
    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
}
