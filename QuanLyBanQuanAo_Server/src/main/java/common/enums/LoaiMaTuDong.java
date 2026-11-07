package common.enums;

public enum LoaiMaTuDong {
    NHAN_VIEN("NV"), NHA_CUNG_CAP("NCC"), PHIEU_NHAP("PHIEUNHAP"),
    KHACH_HANG("KH"), SAN_PHAM("SP"), BIEN_THE("BT"), LOAI_SAN_PHAM("LSP"),
    THUONG_HIEU("TH"), KHUYEN_MAI("KM"), HOA_DON("HD"), THUE("THUE");

    private final String prefix;

    LoaiMaTuDong(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
