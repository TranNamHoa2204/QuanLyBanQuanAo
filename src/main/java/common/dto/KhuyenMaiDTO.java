package common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import common.enums.TrangThaiHieuLucKhuyenMai;

/** Dữ liệu trao đổi; không phụ thuộc entity/JPA. Không ghi mật khẩu hoặc token vào log. */
public class KhuyenMaiDTO {
    private String maKM;
    private String tenKM;
    private BigDecimal phanTramGiam;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private boolean trangThai = true;
    /** Giá trị tính trên server khi trả kết quả; không phải cột persistence. */
    private TrangThaiHieuLucKhuyenMai trangThaiHieuLuc;

    public KhuyenMaiDTO() {
    }

    public String getMaKM() {
        return maKM;
    }

    public void setMaKM(String maKM) {
        this.maKM = maKM;
    }

    public String getTenKM() {
        return tenKM;
    }

    public void setTenKM(String tenKM) {
        this.tenKM = tenKM;
    }

    public BigDecimal getPhanTramGiam() {
        return phanTramGiam;
    }

    public void setPhanTramGiam(BigDecimal phanTramGiam) {
        this.phanTramGiam = phanTramGiam;
    }

    public LocalDate getNgayBatDau() {
        return ngayBatDau;
    }

    public void setNgayBatDau(LocalDate ngayBatDau) {
        this.ngayBatDau = ngayBatDau;
    }

    public LocalDate getNgayKetThuc() {
        return ngayKetThuc;
    }

    public void setNgayKetThuc(LocalDate ngayKetThuc) {
        this.ngayKetThuc = ngayKetThuc;
    }

    public boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }

    public TrangThaiHieuLucKhuyenMai getTrangThaiHieuLuc() { return trangThaiHieuLuc; }
    public void setTrangThaiHieuLuc(TrangThaiHieuLucKhuyenMai trangThaiHieuLuc) { this.trangThaiHieuLuc = trangThaiHieuLuc; }
}
