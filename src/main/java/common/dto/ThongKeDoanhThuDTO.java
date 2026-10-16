package common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import common.enums.NhomThongKe;

/** Kết quả thống kê từ server; không chứa entity hoặc phép tính do client tự tổng hợp. */
public class ThongKeDoanhThuDTO {
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private NhomThongKe nhomThoiGian;
    private long soHoaDon;
    private BigDecimal hangBanTruocGiam;
    private BigDecimal tienGiam;
    private BigDecimal tienThue;
    private BigDecimal doanhThuSauGiam;
    private BigDecimal tongKhachTra;
    private long soHoaDonThieuThoiDiemThanhToan;
    private String canhBaoDuLieuCu;
    private int page;
    private int pageSize;
    private long totalItems;
    private List<ThongKeDoanhThuDongDTO> items = new ArrayList<ThongKeDoanhThuDongDTO>();

    public ThongKeDoanhThuDTO() { }
    public LocalDate getTuNgay() { return tuNgay; }
    public void setTuNgay(LocalDate tuNgay) { this.tuNgay = tuNgay; }
    public LocalDate getDenNgay() { return denNgay; }
    public void setDenNgay(LocalDate denNgay) { this.denNgay = denNgay; }
    public NhomThongKe getNhomThoiGian() { return nhomThoiGian; }
    public void setNhomThoiGian(NhomThongKe nhomThoiGian) { this.nhomThoiGian = nhomThoiGian; }
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
    public long getSoHoaDonThieuThoiDiemThanhToan() { return soHoaDonThieuThoiDiemThanhToan; }
    public void setSoHoaDonThieuThoiDiemThanhToan(long value) { this.soHoaDonThieuThoiDiemThanhToan = value; }
    public String getCanhBaoDuLieuCu() { return canhBaoDuLieuCu; }
    public void setCanhBaoDuLieuCu(String canhBaoDuLieuCu) { this.canhBaoDuLieuCu = canhBaoDuLieuCu; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public long getTotalItems() { return totalItems; }
    public void setTotalItems(long totalItems) { this.totalItems = totalItems; }
    public List<ThongKeDoanhThuDongDTO> getItems() { return items; }
    public void setItems(List<ThongKeDoanhThuDongDTO> items) { this.items = items; }
}
