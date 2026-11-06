package common.dto.nhacungcap;

/** Supplier data over TCP; server owns validation and version checks. */
public class DuLieuNhaCungCap {
    private String maNCC, tenNCC, sdt, email, diaChi;
    private boolean trangThai = true;
    private Long phienBan;

    public DuLieuNhaCungCap() {
    }

    public String getMaNCC() {
        return maNCC;
    }

    public void setMaNCC(String value) {
        this.maNCC = value;
    }

    public String getTenNCC() {
        return tenNCC;
    }

    public void setTenNCC(String value) {
        this.tenNCC = value;
    }

    public String getSdt() {
        return sdt;
    }

    public void setSdt(String value) {
        this.sdt = value;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        this.email = value;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String value) {
        this.diaChi = value;
    }

    public boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean value) {
        this.trangThai = value;
    }

    public Long getPhienBan() {
        return phienBan;
    }

    public void setPhienBan(Long value) {
        this.phienBan = value;
    }
}
