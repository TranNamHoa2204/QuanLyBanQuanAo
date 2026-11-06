package server.entities;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/** Idempotency receipt for mutating operations on a purchase receipt. */
@Entity
@Table(name = "PhieuNhapThaoTac", schema = "dbo")
public class PhieuNhapThaoTac {
    @Id
    @Column(length = 36)
    private String operationId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maPhieuNhap", nullable = false)
    private PhieuNhap phieuNhap;
    @Column(nullable = false, length = 20)
    private String loaiThaoTac;
    @Column(nullable = false, length = 64)
    private String fingerprint;
    private Long phienBanTruoc;
    @Column(nullable = false)
    private Long phienBanSau;
    @Column(nullable = false)
    private LocalDateTime thoiGian;

    public PhieuNhapThaoTac() {
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String value) {
        this.operationId = value;
    }

    public PhieuNhap getPhieuNhap() {
        return phieuNhap;
    }

    public void setPhieuNhap(PhieuNhap value) {
        this.phieuNhap = value;
    }

    public String getLoaiThaoTac() {
        return loaiThaoTac;
    }

    public void setLoaiThaoTac(String value) {
        this.loaiThaoTac = value;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String value) {
        this.fingerprint = value;
    }

    public Long getPhienBanTruoc() {
        return phienBanTruoc;
    }

    public void setPhienBanTruoc(Long value) {
        this.phienBanTruoc = value;
    }

    public Long getPhienBanSau() {
        return phienBanSau;
    }

    public void setPhienBanSau(Long value) {
        this.phienBanSau = value;
    }

    public LocalDateTime getThoiGian() {
        return thoiGian;
    }

    public void setThoiGian(LocalDateTime value) {
        this.thoiGian = value;
    }
}
