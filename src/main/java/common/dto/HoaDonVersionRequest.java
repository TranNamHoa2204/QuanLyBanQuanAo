package common.dto;

public class HoaDonVersionRequest {
    private String maHD;
    private Long phienBan;

    public HoaDonVersionRequest() {
    }

    public String getMaHD() {
        return maHD;
    }

    public void setMaHD(String maHD) {
        this.maHD = maHD;
    }

    public Long getPhienBan() {
        return phienBan;
    }

    public void setPhienBan(Long phienBan) {
        this.phienBan = phienBan;
    }
}
