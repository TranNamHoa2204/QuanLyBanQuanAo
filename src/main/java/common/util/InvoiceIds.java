package common.util;

import java.util.Locale;
import java.util.UUID;

public final class InvoiceIds {
    private InvoiceIds() { }
    /** Client giữ mã này để tải lại hóa đơn nếu mất phản hồi tạo hóa đơn. */
    public static String newId() { return "HD" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase(Locale.ROOT); }
}

