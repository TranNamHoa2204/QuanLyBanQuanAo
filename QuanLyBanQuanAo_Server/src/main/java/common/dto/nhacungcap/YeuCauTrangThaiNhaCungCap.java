package common.dto.nhacungcap;

public class YeuCauTrangThaiNhaCungCap {
    private String maNCC;
    private Long expectedVersion;

    public YeuCauTrangThaiNhaCungCap() {
    }

    public String getMaNCC() {
        return maNCC;
    }

    public void setMaNCC(String value) {
        this.maNCC = value;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long value) {
        this.expectedVersion = value;
    }
}
