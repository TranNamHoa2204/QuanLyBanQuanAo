package common.dto;

/** Expected là ảnh chụp các trường đã đọc; không dùng tongTon để kiểm tra xung đột. */
public class SanPhamSaveRequest {
    private SanPhamDTO value, expected;

    public SanPhamDTO getValue() {
        return value;
    }

    public void setValue(SanPhamDTO value) {
        this.value = value;
    }

    public SanPhamDTO getExpected() {
        return expected;
    }

    public void setExpected(SanPhamDTO expected) {
        this.expected = expected;
    }
}
