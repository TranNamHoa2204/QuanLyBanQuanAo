package common.dto;

/** DTO độc lập với JPA, truyền bằng JSON qua Socket. */
public class SanPhamSearchRequest {
    private String maSP;
    private String tenSP;
    private String maLoai;
    private String maThuongHieu;
    private String mauSac;
    private String kichCo;
    private int page = 0;
    private int pageSize = 20;
    private boolean includeInactive;
    public SanPhamSearchRequest() { }
    public String getMaSP() { return maSP; }
    public void setMaSP(String maSP) { this.maSP = maSP; }
    public String getTenSP() { return tenSP; }
    public void setTenSP(String tenSP) { this.tenSP = tenSP; }
    public String getMaLoai() { return maLoai; }
    public void setMaLoai(String maLoai) { this.maLoai = maLoai; }
    public String getMaThuongHieu() { return maThuongHieu; }
    public void setMaThuongHieu(String maThuongHieu) { this.maThuongHieu = maThuongHieu; }
    public String getMauSac() { return mauSac; }
    public void setMauSac(String mauSac) { this.mauSac = mauSac; }
    public String getKichCo() { return kichCo; }
    public void setKichCo(String kichCo) { this.kichCo = kichCo; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public boolean getIncludeInactive() { return includeInactive; }
    public void setIncludeInactive(boolean includeInactive) { this.includeInactive = includeInactive; }
}
