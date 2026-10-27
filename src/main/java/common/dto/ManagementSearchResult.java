package common.dto;

import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;

/** items chỉ chứa DTO của action đã chọn, không chứa entity/mật khẩu. */
public class ManagementSearchResult {
    private List<JsonNode> items = new ArrayList<>();
    private long total;

    public List<JsonNode> getItems() {
        return items;
    }

    public void setItems(List<JsonNode> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
