package common.dto;

/** Cập nhật hồ sơ kèm snapshot để phát hiện dữ liệu đã đổi trên server. */
public class KhachHangSaveRequest {
    private KhachHangDTO value;
    private KhachHangDTO expected;
    public KhachHangSaveRequest() { }
    public KhachHangDTO getValue() { return value; }
    public void setValue(KhachHangDTO value) { this.value = value; }
    public KhachHangDTO getExpected() { return expected; }
    public void setExpected(KhachHangDTO expected) { this.expected = expected; }
}
