package common.dto.khuyenmai;

/** Yêu cầu xem chi tiết theo mã khuyến mãi. */
public class YeuCauChiTietKhuyenMai {
    private String maKM;

    public YeuCauChiTietKhuyenMai() {
    }

    public String getMaKM() {
        return maKM;
    }

    public void setMaKM(String maKM) {
        this.maKM = maKM;
    }
}
