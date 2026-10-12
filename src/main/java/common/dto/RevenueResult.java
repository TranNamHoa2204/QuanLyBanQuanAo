package common.dto;
import java.math.BigDecimal;
import java.util.*;
public class RevenueResult {
    private long soHoaDon;
    private BigDecimal tamTinh = BigDecimal.ZERO, tienGiam = BigDecimal.ZERO,
            tienThue = BigDecimal.ZERO, tongTien = BigDecimal.ZERO;
    private List<Row> items = new ArrayList<>();
    private boolean hasNext;
    public long getSoHoaDon() { return soHoaDon; }
    public void setSoHoaDon(long value) { soHoaDon = value; }
    public BigDecimal getTamTinh() { return tamTinh; }
    public void setTamTinh(BigDecimal value) { tamTinh = value; }
    public BigDecimal getTienGiam() { return tienGiam; }
    public void setTienGiam(BigDecimal value) { tienGiam = value; }
    public BigDecimal getTienThue() { return tienThue; }
    public void setTienThue(BigDecimal value) { tienThue = value; }
    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal value) { tongTien = value; }
    public List<Row> getItems() { return items; }
    public void setItems(List<Row> value) { items = value; }
    public boolean getHasNext() { return hasNext; }
    public void setHasNext(boolean value) { hasNext = value; }
    public static class Row {
        private String code, label;
        private long quantity;
        private BigDecimal amount;
        public String getCode() { return code; }
        public void setCode(String value) { code = value; }
        public String getLabel() { return label; }
        public void setLabel(String value) { label = value; }
        public long getQuantity() { return quantity; }
        public void setQuantity(long value) { quantity = value; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal value) { amount = value; }
    }
}
