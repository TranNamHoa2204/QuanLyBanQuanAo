package common.dto;

import java.time.LocalDate;
import common.enums.NguonBienDongKho;

/** Bộ lọc lịch sử kho; các trường lọc có thể để trống. */
public class LichSuKhoSearchRequest {
    private String maBienThe;
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private NguonBienDongKho nguon;
    private String maNV;
    private int page;
    private int pageSize = 50;

    public LichSuKhoSearchRequest() { }
    public String getMaBienThe() { return maBienThe; }
    public void setMaBienThe(String value) { this.maBienThe = value; }
    public LocalDate getTuNgay() { return tuNgay; }
    public void setTuNgay(LocalDate value) { this.tuNgay = value; }
    public LocalDate getDenNgay() { return denNgay; }
    public void setDenNgay(LocalDate value) { this.denNgay = value; }
    public NguonBienDongKho getNguon() { return nguon; }
    public void setNguon(NguonBienDongKho value) { this.nguon = value; }
    public String getMaNV() { return maNV; }
    public void setMaNV(String value) { this.maNV = value; }
    public int getPage() { return page; }
    public void setPage(int value) { this.page = value; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int value) { this.pageSize = value; }
}
