package common.dto.sanpham;

/** Expected là ảnh chụp các trường đã đọc; không dùng tongTon để kiểm tra xung đột. */
public class YeuCauLuuSanPham {
    private DuLieuSanPham value, expected;

    public DuLieuSanPham getValue() {
        return value;
    }

    public void setValue(DuLieuSanPham value) {
        this.value = value;
    }

    public DuLieuSanPham getExpected() {
        return expected;
    }

    public void setExpected(DuLieuSanPham expected) {
        this.expected = expected;
    }
}
