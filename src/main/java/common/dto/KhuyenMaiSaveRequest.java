package common.dto;

/** Cập nhật có điều kiện theo snapshot đầy đủ đã đọc từ server. */
public class KhuyenMaiSaveRequest {
    private KhuyenMaiDTO expected;
    private KhuyenMaiDTO value;
    public KhuyenMaiSaveRequest() { }
    public KhuyenMaiDTO getExpected() { return expected; }
    public void setExpected(KhuyenMaiDTO expected) { this.expected = expected; }
    public KhuyenMaiDTO getValue() { return value; }
    public void setValue(KhuyenMaiDTO value) { this.value = value; }
}
