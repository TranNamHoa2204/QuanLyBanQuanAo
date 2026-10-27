package client.ui;

import client.api.ManagementApi;

/** View NhanVien Swing/FlatLaf, được nhúng trong MainForm. */
public final class NhanVienForm extends ManagementPanel {
    private static final long serialVersionUID = 1L;

    public NhanVienForm(ManagementApi api) {
        super(api, ManagementApi.Kind.EMPLOYEE);
    }
}
