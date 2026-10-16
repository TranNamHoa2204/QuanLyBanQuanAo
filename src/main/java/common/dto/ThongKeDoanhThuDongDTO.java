package common.dto;

import java.math.BigDecimal;

/** Một kỳ báo cáo hoặc một sản phẩm; các số tiền đều là tổng của hóa đơn đã trả. */
public class ThongKeDoanhThuDongDTO {
    private String khoa;
    private String nhan;
    private String maSP;
    private String tenSP;
    private boolean tenSPHienHanhFallback;
    private long soHoaDon;
    private BigDecimal hangBanTruocGiam;
    private BigDecimal tienGiam;
    private BigDecimal tienThue;
    private BigDecimal doanhThuSauGiam;
    private BigDecimal tongKhachTra;

    public ThongKeDoanhThuDongDTO() { }
    public String getKhoa() { return khoa; }
    public void setKhoa(String khoa) { this.khoa = khoa; }
    public String getNhan() { return nhan; }
    public void setNhan(String nhan) { this.nhan = nhan; }
    public String getMaSP() { return maSP; }
    public void setMaSP(String maSP) { this.maSP = maSP; }
    public String getTenSP() { return tenSP; }
    public void setTenSP(String tenSP) { this.tenSP = tenSP; }
    public boolean isTenSPHienHanhFallback() { return tenSPHienHanhFallback; }
    public void setTenSPHienHanhFallback(boolean tenSPHienHanhFallback) { this.tenSPHienHanhFallback = tenSPHienHanhFallback; }
    public long getSoHoaDon() { return soHoaDon; }
    public void setSoHoaDon(long soHoaDon) { this.soHoaDon = soHoaDon; }
    public BigDecimal getHangBanTruocGiam() { return hangBanTruocGiam; }
    public void setHangBanTruocGiam(BigDecimal hangBanTruocGiam) { this.hangBanTruocGiam = hangBanTruocGiam; }
    public BigDecimal getTienGiam() { return tienGiam; }
    public void setTienGiam(BigDecimal tienGiam) { this.tienGiam = tienGiam; }
    public BigDecimal getTienThue() { return tienThue; }
    public void setTienThue(BigDecimal tienThue) { this.tienThue = tienThue; }
    public BigDecimal getDoanhThuSauGiam() { return doanhThuSauGiam; }
    public void setDoanhThuSauGiam(BigDecimal doanhThuSauGiam) { this.doanhThuSauGiam = doanhThuSauGiam; }
    public BigDecimal getTongKhachTra() { return tongKhachTra; }
    public void setTongKhachTra(BigDecimal tongKhachTra) { this.tongKhachTra = tongKhachTra; }
}
