package server.security;

import common.enums.VaiTro;

/** Tiện ích cơ bản; dispatcher vẫn phải kiểm tra phiên thật ở server. */
public final class PermissionChecker {
    private PermissionChecker() { }
    public static void requireAuthenticated(VaiTro vaiTro) {
        if (vaiTro == null) { throw new SecurityException("Chua dang nhap."); }
    }
    public static void requireManager(VaiTro vaiTro) {
        requireAuthenticated(vaiTro);
        if (vaiTro != VaiTro.NHAN_VIEN_QUAN_LY) {
            throw new SecurityException("Khong co quyen quan ly.");
        }
    }
}
