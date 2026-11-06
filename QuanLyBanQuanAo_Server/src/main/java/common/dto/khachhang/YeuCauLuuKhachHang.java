package common.dto.khachhang;

/** Cập nhật hồ sơ kèm snapshot để phát hiện dữ liệu đã đổi trên server. */
public class YeuCauLuuKhachHang {
    private DuLieuKhachHang value;
    private DuLieuKhachHang expected;

    public YeuCauLuuKhachHang() {
    }

    public DuLieuKhachHang getValue() {
        return value;
    }

    public void setValue(DuLieuKhachHang value) {
        this.value = value;
    }

    public DuLieuKhachHang getExpected() {
        return expected;
    }

    public void setExpected(DuLieuKhachHang expected) {
        this.expected = expected;
    }
}
