package server.service;
import java.text.Normalizer;
import java.util.*;
import javax.persistence.*;
import com.fasterxml.jackson.databind.JsonNode;
import common.dto.*;
import common.enums.VaiTro;
import common.protocol.*;
import server.entity.TaiKhoan;

/** Chính sách/validation chung; tên bảng/thuộc tính chỉ do service cung cấp. */
final class ManagementSupport {
    static TaiKhoan manager(AuthService auth, EntityManager em, String token) {
        TaiKhoan account = auth.requireAuthenticated(em, token);
        if (account.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chức năng chỉ dành cho nhân viên quản lý.");
        }
        return account;
    }
    static String text(String value, int max, boolean required) {
        String s = value == null ? null : Normalizer.normalize(value, Normalizer.Form.NFC).trim();
        if (s == null || s.isEmpty()) {
            if (required) { throw invalid("Thiếu trường bắt buộc."); } return null;
        }
        if (s.length() > max || s.chars().anyMatch(Character::isISOControl)) {
            throw invalid("Thông tin quá dài hoặc có ký tự điều khiển.");
        } return s;
    }
    static String code(String s) {
        String value = text(s, 20, true);
        if (!value.matches("[A-Za-z0-9_-]{1,20}")) { throw invalid("Mã tối đa 20 ký tự: chữ không dấu, số, - hoặc _."); }
        return value;
    }
    static String phone(String s, boolean required) {
        String value = text(s, 20, required);
        if (value != null) {
            value = value.replaceAll("[ .()-]", "");
            if (!value.matches("\\+?[0-9]{7,19}")) { throw invalid("Số điện thoại không hợp lệ (7–19 chữ số, có thể có +)."); }
        }
        return value;
    }
    static String email(String s) {
        String value = text(s, 254, false);
        if (value != null && !value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) { throw invalid("Email không hợp lệ."); }
        return value;
    }
    static void page(ManagementSearchRequest r) {
        if (r == null || r.getPage() < 0 || r.getPage() > 1000000 || r.getPageSize() < 1 || r.getPageSize() > 100) {
            throw invalid("Phân trang không hợp lệ.");
        }
        r.setQuery(text(r.getQuery(), 100, false));
    }
    static <T> T value(ManagementSaveRequest r, Class<T> type) {
        if (r == null || r.getValue() == null || !r.getValue().isObject()) { throw invalid("Thiếu dữ liệu."); }
        return JsonCodec.mapper().convertValue(r.getValue(), type);
    }
    static void expected(ManagementSaveRequest r, Object dto) {
        if (r.getExpected() == null || !same(r.getExpected(), JsonCodec.mapper().valueToTree(dto))) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT, "Thông tin đã đổi. Tải lại trước khi lưu.");
        }
    }
    private static boolean same(JsonNode a, JsonNode b) {
        if (a == null || b == null) { return a == b; }
        if (a.isNumber() && b.isNumber()) { return a.decimalValue().compareTo(b.decimalValue()) == 0; }
        if (a.isObject() && b.isObject()) {
            if (a.size() != b.size()) { return false; }
            Iterator<String> names = a.fieldNames();
            while (names.hasNext()) { String name = names.next(); if (!same(a.get(name), b.get(name))) { return false; } }
            return true;
        }
        return a.equals(b);
    }
    static BusinessException invalid(String s) { return new BusinessException(ErrorCode.VALIDATION_ERROR, s); }
    static void missing(Object entity) {
        if (entity == null) { throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bản ghi."); }
    }
}
