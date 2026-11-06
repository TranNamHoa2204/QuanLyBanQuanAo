package common.dto.khuyenmai;

/** Thay đổi cờ hoạt động bằng snapshot để phát hiện thao tác lỗi thời. */
public class YeuCauTrangThaiKhuyenMai {
    private DuLieuKhuyenMai expected;

    public YeuCauTrangThaiKhuyenMai() {
    }

    public DuLieuKhuyenMai getExpected() {
        return expected;
    }

    public void setExpected(DuLieuKhuyenMai expected) {
        this.expected = expected;
    }
}
