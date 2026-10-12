package client.ui;
import client.api.ManagementApi;
/** View KhachHang Swing/FlatLaf, được nhúng trong MainForm. */
public final class KhachHangForm extends ManagementPanel {
    private static final long serialVersionUID = 1L;
    public KhachHangForm(ManagementApi api) { super(api, ManagementApi.Kind.CUSTOMER); }
}
