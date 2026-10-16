package common.dto;

/** Cập nhật hồ sơ có optimistic snapshot để không ghi đè chỉnh sửa mới hơn. */
public class NhanVienSaveRequest {
    private NhanVienDTO value;
    private NhanVienDTO expected;
    public NhanVienSaveRequest() { }
    public NhanVienDTO getValue() { return value; }
    public void setValue(NhanVienDTO value) { this.value = value; }
    public NhanVienDTO getExpected() { return expected; }
    public void setExpected(NhanVienDTO expected) { this.expected = expected; }
}
