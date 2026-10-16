package common.dto;

import java.time.LocalDate;
import common.enums.NhomThongKe;

/** Khoảng ngày theo giờ địa phương Việt Nam; trang được đánh số từ 0. */
public class ThongKeDoanhThuRequest {
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private NhomThongKe nhomThoiGian;
    private int page;
    private int pageSize = 50;

    public ThongKeDoanhThuRequest() { }
    public LocalDate getTuNgay() { return tuNgay; }
    public void setTuNgay(LocalDate tuNgay) { this.tuNgay = tuNgay; }
    public LocalDate getDenNgay() { return denNgay; }
    public void setDenNgay(LocalDate denNgay) { this.denNgay = denNgay; }
    public NhomThongKe getNhomThoiGian() { return nhomThoiGian; }
    public void setNhomThoiGian(NhomThongKe nhomThoiGian) { this.nhomThoiGian = nhomThoiGian; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
