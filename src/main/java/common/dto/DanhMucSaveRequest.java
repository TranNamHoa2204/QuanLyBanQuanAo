package common.dto;

public class DanhMucSaveRequest {
    private String code, name, expectedName;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpectedName() {
        return expectedName;
    }

    public void setExpectedName(String name) {
        this.expectedName = name;
    }
}
