package client.ui;

import client.api.ManagementApi;

/** View KhuyenMai Swing/FlatLaf, được nhúng trong MainForm. */
public final class KhuyenMaiForm extends ManagementPanel {
    private static final long serialVersionUID = 1L;

    public KhuyenMaiForm(ManagementApi api) {
        super(api, ManagementApi.Kind.PROMOTION);
    }
}
