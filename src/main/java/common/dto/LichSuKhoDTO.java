package common.dto;

import java.time.LocalDateTime;
import common.enums.NguonBienDongKho;

/** Bản chiếu DTO của một bút toán tồn; không truyền entity qua Socket. */
public class LichSuKhoDTO {
    private Long maLichSuKho;
    private String maBienThe;
    private String maSP;
    private String maNV;
    private String tenNguoiThaoTac;
    private LocalDateTime thoiGian;
    private NguonBienDongKho nguon;
    private String thamChieu;
    private String operationId;
    private int soLuongTruoc;
    private int soLuongThayDoi;
    private int soLuongSau;
    private Long phienBanTruoc;
    private String lyDo;

    public LichSuKhoDTO() { }
    public Long getMaLichSuKho() { return maLichSuKho; }
    public void setMaLichSuKho(Long value) { this.maLichSuKho = value; }
    public String getMaBienThe() { return maBienThe; }
    public void setMaBienThe(String value) { this.maBienThe = value; }
    public String getMaSP() { return maSP; }
    public void setMaSP(String value) { this.maSP = value; }
    public String getMaNV() { return maNV; }
    public void setMaNV(String value) { this.maNV = value; }
    public String getTenNguoiThaoTac() { return tenNguoiThaoTac; }
    public void setTenNguoiThaoTac(String value) { this.tenNguoiThaoTac = value; }
    public LocalDateTime getThoiGian() { return thoiGian; }
    public void setThoiGian(LocalDateTime value) { this.thoiGian = value; }
    public NguonBienDongKho getNguon() { return nguon; }
    public void setNguon(NguonBienDongKho value) { this.nguon = value; }
    public String getThamChieu() { return thamChieu; }
    public void setThamChieu(String value) { this.thamChieu = value; }
    public String getOperationId() { return operationId; }
    public void setOperationId(String value) { this.operationId = value; }
    public int getSoLuongTruoc() { return soLuongTruoc; }
    public void setSoLuongTruoc(int value) { this.soLuongTruoc = value; }
    public int getSoLuongThayDoi() { return soLuongThayDoi; }
    public void setSoLuongThayDoi(int value) { this.soLuongThayDoi = value; }
    public int getSoLuongSau() { return soLuongSau; }
    public void setSoLuongSau(int value) { this.soLuongSau = value; }
    public Long getPhienBanTruoc() { return phienBanTruoc; }
    public void setPhienBanTruoc(Long value) { this.phienBanTruoc = value; }
    public String getLyDo() { return lyDo; }
    public void setLyDo(String value) { this.lyDo = value; }
}
