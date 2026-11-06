package common.dto.nhanvien;

/** Cập nhật hồ sơ có optimistic snapshot để không ghi đè chỉnh sửa mới hơn. */
public class YeuCauLuuNhanVien {
    private DuLieuNhanVien value;
    private DuLieuNhanVien expected;

    public YeuCauLuuNhanVien() {
    }

    public DuLieuNhanVien getValue() {
        return value;
    }

    public void setValue(DuLieuNhanVien value) {
        this.value = value;
    }

    public DuLieuNhanVien getExpected() {
        return expected;
    }

    public void setExpected(DuLieuNhanVien expected) {
        this.expected = expected;
    }
}
