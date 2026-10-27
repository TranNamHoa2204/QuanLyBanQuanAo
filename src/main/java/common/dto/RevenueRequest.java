package common.dto;

import java.time.LocalDate;

public class RevenueRequest {
    public enum Group {
        DAY, MONTH, YEAR
    }

    private LocalDate tuNgay, denNgay;
    private Group group = Group.DAY;
    private int page, pageSize = 50;

    public LocalDate getTuNgay() {
        return tuNgay;
    }

    public void setTuNgay(LocalDate value) {
        tuNgay = value;
    }

    public LocalDate getDenNgay() {
        return denNgay;
    }

    public void setDenNgay(LocalDate value) {
        denNgay = value;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group value) {
        group = value;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int value) {
        page = value;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int value) {
        pageSize = value;
    }
}
