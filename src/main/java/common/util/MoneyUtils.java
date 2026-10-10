package common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Hàm tiền cơ bản; chính sách làm tròn hóa đơn sẽ được service thống nhất riêng. */
public final class MoneyUtils {
    private MoneyUtils() { }
    public static BigDecimal round(BigDecimal value) {
        if (value == null) { throw new IllegalArgumentException("Money cannot be null."); }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
    public static BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        if (unitPrice == null || unitPrice.signum() < 0 || quantity <= 0) {
            throw new IllegalArgumentException("Invalid price or quantity.");
        }
        return round(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }
    public static BigDecimal percentage(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null || amount.signum() < 0
                || rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Invalid amount or percentage.");
        }
        return amount.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
