package common.dto;

/** Phân trang dùng chung cho khách hàng, nhân viên và khuyến mãi. */
public class ManagementSearchRequest {
    private String query;
    private int page, pageSize = 25;
    private boolean includeInactive;
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int size) { pageSize = size; }
    public boolean getIncludeInactive() { return includeInactive; }
    public void setIncludeInactive(boolean value) { includeInactive = value; }
}
