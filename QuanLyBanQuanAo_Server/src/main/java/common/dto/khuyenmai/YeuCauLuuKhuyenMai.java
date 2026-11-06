package common.dto.khuyenmai;

/** Cập nhật có điều kiện theo snapshot đầy đủ đã đọc từ server. */
public class YeuCauLuuKhuyenMai {
    private DuLieuKhuyenMai expected;
    private DuLieuKhuyenMai value;

    public YeuCauLuuKhuyenMai() {
    }

    public DuLieuKhuyenMai getExpected() {
        return expected;
    }

    public void setExpected(DuLieuKhuyenMai expected) {
        this.expected = expected;
    }

    public DuLieuKhuyenMai getValue() {
        return value;
    }

    public void setValue(DuLieuKhuyenMai value) {
        this.value = value;
    }
}
