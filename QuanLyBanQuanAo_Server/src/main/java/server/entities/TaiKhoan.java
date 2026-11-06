package server.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import common.enums.VaiTro;

/** Entity JPA theo SQL hiện tại; không truyền entity hoặc passwordHash qua Socket. */
@Entity
@Table(name = "TaiKhoan", schema = "dbo")
public class TaiKhoan {
    @Id
    @Column(length = 50)
    private String username;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNV", nullable = false, unique = true)
    private NhanVien nhanVien;
    @Column(name = "matKhauHash", nullable = false, length = 255)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VaiTro vaiTro;
    @Column(nullable = false)
    private boolean trangThai = true;

    public TaiKhoan() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    public void setNhanVien(NhanVien nhanVien) {
        this.nhanVien = nhanVien;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public VaiTro getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(VaiTro vaiTro) {
        this.vaiTro = vaiTro;
    }

    public boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }
}
