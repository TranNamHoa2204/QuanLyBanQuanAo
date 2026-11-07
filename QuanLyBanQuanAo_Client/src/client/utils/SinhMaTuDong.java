package client.utils;

import java.util.Locale;
import java.util.UUID;

/** Tạo mã ngay khi mở form để giữ cùng một mã trong suốt lần lưu. */
public final class SinhMaTuDong {
    private SinhMaTuDong() {
    }

    public static String tao(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 20 - prefix.length()).toUpperCase(Locale.ROOT);
    }
}
