package client.ui;

import client.api.ManagementApi;

/** Bảng cấu hình thuế chỉ dành cho quản lý. */
public final class ThueForm extends ManagementPanel {
    private static final long serialVersionUID = 1L;

    public ThueForm(ManagementApi api) {
        super(api, ManagementApi.Kind.TAX);
    }
}
