package common.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** Snapshot expected chống ghi đè. initialPassword chỉ dùng khi cấp tài khoản mới. */
public class ManagementSaveRequest {
    private JsonNode value, expected;
    private String initialPassword;

    public JsonNode getValue() {
        return value;
    }

    public void setValue(JsonNode value) {
        this.value = value;
    }

    public JsonNode getExpected() {
        return expected;
    }

    public void setExpected(JsonNode value) {
        expected = value;
    }

    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String value) {
        initialPassword = value;
    }
}
