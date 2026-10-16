package common.dto;

import java.math.BigDecimal;

/** Client chỉ gửi tồn mục tiêu và version; server tự lấy tồn trước và tính delta/sau. */
public class DieuChinhTonKhoRequest {
    private String maBienThe;
    // BigDecimal preserves decimal precision so the server can reject 1.5
    // instead of allowing a coercing mapper to turn it into 1.
    private BigDecimal soLuongMoi;
    private BigDecimal expectedVersion;
    private String lyDo;
    private String operationId;

    public DieuChinhTonKhoRequest() { }
    public String getMaBienThe() { return maBienThe; }
    public void setMaBienThe(String maBienThe) { this.maBienThe = maBienThe; }
    public BigDecimal getSoLuongMoi() { return soLuongMoi; }
    public void setSoLuongMoi(BigDecimal soLuongMoi) { this.soLuongMoi = soLuongMoi; }
    public BigDecimal getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(BigDecimal expectedVersion) { this.expectedVersion = expectedVersion; }
    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }
}
