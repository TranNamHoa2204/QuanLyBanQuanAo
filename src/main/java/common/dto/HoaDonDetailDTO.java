package common.dto;

public class HoaDonDetailDTO {
    private HoaDonDTO hoaDon;
    private java.util.List<ChiTietHoaDonDTO> chiTiet = new java.util.ArrayList<>();
    public HoaDonDetailDTO() { }
    public HoaDonDTO getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDonDTO hoaDon) { this.hoaDon = hoaDon; }
    public java.util.List<ChiTietHoaDonDTO> getChiTiet() { return chiTiet; }
    public void setChiTiet(java.util.List<ChiTietHoaDonDTO> chiTiet) { this.chiTiet = chiTiet; }
}
