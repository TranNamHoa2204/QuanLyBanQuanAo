package common.dto;

import java.time.LocalDate;
import java.math.BigDecimal;

/** Dữ liệu trao đổi; không phụ thuộc entity/JPA. Không ghi mật khẩu hoặc token vào log. */
public class ThongKeDoanhThuDTO {
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private long soHoaDon;
    private BigDecimal doanhThu;

    public ThongKeDoanhThuDTO() {
    }

    public LocalDate getTuNgay() {
        return tuNgay;
    }

    public void setTuNgay(LocalDate tuNgay) {
        this.tuNgay = tuNgay;
    }

    public LocalDate getDenNgay() {
        return denNgay;
    }

    public void setDenNgay(LocalDate denNgay) {
        this.denNgay = denNgay;
    }

    public long getSoHoaDon() {
        return soHoaDon;
    }

    public void setSoHoaDon(long soHoaDon) {
        this.soHoaDon = soHoaDon;
    }

    public BigDecimal getDoanhThu() {
        return doanhThu;
    }

    public void setDoanhThu(BigDecimal doanhThu) {
        this.doanhThu = doanhThu;
    }
}
