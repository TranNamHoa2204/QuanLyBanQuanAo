package client.session;

import common.dto.taikhoan.KetQuaDangNhap;

/** Chỉ lưu trong RAM; server vẫn là nơi quyết định quyền. */
public final class PhienDangNhap {
    private KetQuaDangNhap login;

    public synchronized void setLogin(KetQuaDangNhap login) {
        this.login = login;
    }

    public synchronized KetQuaDangNhap getLogin() {
        return login;
    }

    public synchronized String getToken() {
        return login == null ? null : login.getSessionToken();
    }

    public synchronized void clear() {
        login = null;
    }
}
