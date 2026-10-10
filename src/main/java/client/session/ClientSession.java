package client.session;

import common.dto.LoginResponse;

/** Chỉ lưu trong RAM; server vẫn là nơi quyết định quyền. */
public final class ClientSession {
    private LoginResponse login;
    public synchronized void setLogin(LoginResponse login) { this.login = login; }
    public synchronized LoginResponse getLogin() { return login; }
    public synchronized String getToken() { return login == null ? null : login.getSessionToken(); }
    public synchronized void clear() { login = null; }
}
