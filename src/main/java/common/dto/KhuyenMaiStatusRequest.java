package common.dto;

/** Thay đổi cờ hoạt động bằng snapshot để phát hiện thao tác lỗi thời. */
public class KhuyenMaiStatusRequest {
    private KhuyenMaiDTO expected;
    public KhuyenMaiStatusRequest() { }
    public KhuyenMaiDTO getExpected() { return expected; }
    public void setExpected(KhuyenMaiDTO expected) { this.expected = expected; }
}
