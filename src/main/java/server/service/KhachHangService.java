package server.service;

import java.net.IDN;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import common.dto.KhachHangDTO;
import common.dto.KhachHangDetailRequest;
import common.dto.KhachHangSaveRequest;
import common.dto.KhachHangSearchRequest;
import common.dto.KhachHangSearchResult;
import common.dto.KhachHangStatusRequest;
import common.protocol.ErrorCode;
import server.entity.KhachHang;
import server.persistence.JpaManager;
import server.repository.JpaKhachHangDao;

/** Nghiệp vụ khách hàng phía server; mọi thao tác JPA chạy trong transaction. */
public final class KhachHangService {
    private final JpaManager jpa;
    private final AuthService auth;

    public KhachHangService(JpaManager jpa, AuthService auth) {
        this.jpa = Objects.requireNonNull(jpa, "jpa");
        this.auth = Objects.requireNonNull(auth, "auth");
    }

    public KhachHangSearchResult search(String token, KhachHangSearchRequest request) {
        validateSearch(request);
        return jpa.transaction(em -> {
            auth.requireAuthenticated(em, token);
            JpaKhachHangDao customers = new JpaKhachHangDao(em);
            List<KhachHangDTO> items = new ArrayList<>();
            for (KhachHang row : customers.search(request)) { items.add(toDto(row)); }
            KhachHangSearchResult result = new KhachHangSearchResult();
            result.setItems(items); result.setTotal(customers.count(request));
            result.setPage(request.getPage()); result.setPageSize(request.getPageSize());
            return result;
        });
    }

    /** Chi tiết không lọc trạng thái để khách ngừng vẫn xem được lịch sử/liên kết cũ. */
    public KhachHangDTO detail(String token, KhachHangDetailRequest request) {
        if (request == null) { throw invalid("Thiếu mã khách hàng."); }
        String id = code(request.getMaKH());
        return jpa.transaction(em -> {
            auth.requireAuthenticated(em, token);
            KhachHang row = new JpaKhachHangDao(em).findById(id)
                    .orElseThrow(() -> missing("Không tìm thấy khách hàng."));
            return toDto(row);
        });
    }

    public KhachHangDTO create(String token, KhachHangDTO request) {
        CustomerData data = createData(request);
        return jpa.transaction(em -> {
            auth.requireAuthenticated(em, token);
            JpaKhachHangDao customers = new JpaKhachHangDao(em);
            if (customers.codeInUseIgnoreCase(data.maKH)) { throw invalid("Mã khách hàng đã tồn tại."); }
            if (customers.phoneInUse(data.sdt, null)) { throw invalid("Số điện thoại đã được sử dụng."); }
            KhachHang row = new KhachHang(); apply(row, data); em.persist(row); em.flush();
            return toDto(row);
        });
    }

    /** So sánh snapshot dưới khóa ghi; không cần thay đổi baseline/schema để có optimistic semantics. */
    public KhachHangDTO update(String token, KhachHangSaveRequest request) {
        if (request == null || request.getExpected() == null || request.getValue() == null) {
            throw invalid("Thiếu snapshot hiện tại hoặc hồ sơ cần lưu.");
        }
        KhachHangDTO expected = request.getExpected();
        KhachHangDTO value = request.getValue();
        String id = code(expected.getMaKH());
        if (!id.equals(code(value.getMaKH()))) { throw invalid("Không thể đổi mã khách hàng."); }

        return jpa.transaction(em -> {
            auth.requireAuthenticated(em, token);
            JpaKhachHangDao customers = new JpaKhachHangDao(em);
            KhachHang row = customers.findForUpdate(id)
                    .orElseThrow(() -> missing("Không tìm thấy khách hàng."));
            em.refresh(row, LockModeType.PESSIMISTIC_WRITE);
            if (!matches(expected, row)) { throw conflict(); }
            if (value.getTrangThai() != row.getTrangThai()) {
                throw invalid("Trạng thái phải đổi bằng thao tác ngừng/khôi phục khách hàng.");
            }

            // Giữ nguyên giá trị cũ không hợp lệ nếu người dùng không sửa trường đó;
            // các giá trị mới/thay đổi vẫn phải qua chuẩn hóa và validation.
            CustomerData data = updateData(value, row);
            if (!Objects.equals(data.sdt, row.getSdt()) && customers.phoneInUse(data.sdt, row.getMaKH())) {
                throw invalid("Số điện thoại đã được sử dụng.");
            }
            apply(row, data); em.flush();
            return toDto(row);
        });
    }

    public KhachHangDTO setActive(String token, KhachHangStatusRequest request, boolean active) {
        if (request == null || request.getExpectedTrangThai() == null) {
            throw invalid("Thiếu trạng thái khách hàng hiện tại.");
        }
        String id = code(request.getMaKH());
        return jpa.transaction(em -> {
            auth.requireAuthenticated(em, token);
            KhachHang row = new JpaKhachHangDao(em).findForUpdate(id)
                    .orElseThrow(() -> missing("Không tìm thấy khách hàng."));
            em.refresh(row, LockModeType.PESSIMISTIC_WRITE);
            if (row.getTrangThai() != request.getExpectedTrangThai()) { throw conflict(); }
            row.setTrangThai(active); em.flush();
            return toDto(row);
        });
    }

    private static CustomerData createData(KhachHangDTO request) {
        if (request == null) { throw invalid("Thiếu hồ sơ khách hàng."); }
        if (!request.getTrangThai()) { throw invalid("Khách hàng mới phải ở trạng thái hoạt động."); }
        return new CustomerData(code(request.getMaKH()), requiredText(request.getHoTen(), 100, "Họ tên"),
                phone(request.getSdt()), email(request.getEmail()), optionalText(request.getDiaChi(), 255, "Địa chỉ"), true);
    }

    private static CustomerData updateData(KhachHangDTO value, KhachHang current) {
        String name = Objects.equals(value.getHoTen(), current.getHoTen())
                ? current.getHoTen() : requiredText(value.getHoTen(), 100, "Họ tên");
        String phone = Objects.equals(value.getSdt(), current.getSdt())
                ? current.getSdt() : phone(value.getSdt());
        String email = Objects.equals(value.getEmail(), current.getEmail())
                ? current.getEmail() : email(value.getEmail());
        String address = Objects.equals(value.getDiaChi(), current.getDiaChi())
                ? current.getDiaChi() : optionalText(value.getDiaChi(), 255, "Địa chỉ");
        return new CustomerData(current.getMaKH(), name, phone, email, address, current.getTrangThai());
    }

    private static void apply(KhachHang row, CustomerData data) {
        row.setMaKH(data.maKH); row.setHoTen(data.hoTen); row.setSdt(data.sdt);
        row.setEmail(data.email); row.setDiaChi(data.diaChi); row.setTrangThai(data.trangThai);
    }

    private static boolean matches(KhachHangDTO expected, KhachHang row) {
        return Objects.equals(expected.getMaKH(), row.getMaKH())
                && Objects.equals(expected.getHoTen(), row.getHoTen())
                && Objects.equals(expected.getSdt(), row.getSdt())
                && Objects.equals(expected.getEmail(), row.getEmail())
                && Objects.equals(expected.getDiaChi(), row.getDiaChi())
                && expected.getTrangThai() == row.getTrangThai();
    }

    private static KhachHangDTO toDto(KhachHang row) {
        KhachHangDTO dto = new KhachHangDTO(); dto.setMaKH(row.getMaKH()); dto.setHoTen(row.getHoTen());
        dto.setSdt(row.getSdt()); dto.setEmail(row.getEmail()); dto.setDiaChi(row.getDiaChi());
        dto.setTrangThai(row.getTrangThai()); return dto;
    }

    private static void validateSearch(KhachHangSearchRequest request) {
        if (request == null || request.getPage() < 0 || request.getPage() > 1000000
                || request.getPageSize() < 1 || request.getPageSize() > 100) {
            throw invalid("Bộ lọc hoặc phân trang không hợp lệ.");
        }
        if (request.getTuKhoa() != null) {
            String keyword = Normalizer.normalize(request.getTuKhoa(), Normalizer.Form.NFC);
            keyword = stripUnicodeSpace(keyword);
            if (keyword.length() > 100 || hasControl(keyword)) {
                throw invalid("Từ khóa tối đa 100 ký tự và không chứa ký tự điều khiển.");
            }
            request.setTuKhoa(keyword);
        }
    }

    private static String code(String value) {
        String result = requiredText(value, 20, "Mã khách hàng");
        if (!result.matches("[A-Za-z0-9_-]{1,20}")) {
            throw invalid("Mã khách hàng chỉ gồm chữ Latin, số, _ hoặc -.");
        }
        return result.toUpperCase(Locale.ROOT);
    }

    private static String phone(String value) {
        String source = optionalText(value, 100, "Số điện thoại");
        if (source == null) { return null; }
        String text = stripUnicodeSpace(Normalizer.normalize(source, Normalizer.Form.NFC));
        boolean international = text.startsWith("+");
        StringBuilder digits = new StringBuilder();
        for (int i = international ? 1 : 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int digit = Character.digit(c, 10);
            if (digit >= 0) { digits.append((char) ('0' + digit)); }
            else if (Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '-' || c == '(' || c == ')' || c == '.') { }
            else { throw invalid("Số điện thoại chứa ký tự không hợp lệ."); }
        }
        if (digits.length() < 6 || digits.length() > (international ? 19 : 20)) {
            throw invalid("Số điện thoại phải có từ 6 đến 20 chữ số.");
        }
        return (international ? "+" : "") + digits;
    }

    private static String email(String value) {
        String result = optionalText(value, 254, "Email");
        if (result == null) { return null; }
        int at = result.indexOf('@');
        if (at < 1 || at != result.lastIndexOf('@') || at > 64 || at == result.length() - 1) {
            throw invalid("Email không đúng định dạng.");
        }
        String local = result.substring(0, at), domain = result.substring(at + 1);
        if (local.startsWith(".") || local.endsWith(".") || local.contains("..")) { throw invalid("Email không đúng định dạng."); }
        for (int i = 0; i < local.length(); i++) {
            char c = local.charAt(i);
            if (!(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9'
                    || ".!#$%&'*+/=?^_`{|}~-".indexOf(c) >= 0)) {
                throw invalid("Email không đúng định dạng.");
            }
        }
        final String asciiDomain;
        try { asciiDomain = IDN.toASCII(domain, IDN.USE_STD3_ASCII_RULES); }
        catch (IllegalArgumentException badDomain) { throw invalid("Email không đúng định dạng."); }
        if (asciiDomain.length() > 253 || asciiDomain.indexOf('.') < 1 || asciiDomain.endsWith(".")) {
            throw invalid("Email không đúng định dạng.");
        }
        String[] labels = asciiDomain.split("\\.", -1);
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63 || label.startsWith("-") || label.endsWith("-")) {
                throw invalid("Email không đúng định dạng.");
            }
        }
        return result;
    }

    private static String requiredText(String value, int max, String field) {
        String result = optionalText(value, max, field);
        if (result == null) { throw invalid(field + " không được để trống."); }
        return result;
    }

    private static String optionalText(String value, int max, String field) {
        if (value == null) { return null; }
        String result = stripUnicodeSpace(Normalizer.normalize(value, Normalizer.Form.NFC));
        if (result.isEmpty()) { return null; }
        if (result.length() > max || hasControl(result)) {
            throw invalid(field + " quá dài hoặc chứa ký tự điều khiển.");
        }
        return result;
    }

    private static String stripUnicodeSpace(String value) {
        int start = 0, end = value.length();
        while (start < end) {
            int cp = value.codePointAt(start);
            if (!Character.isWhitespace(cp) && !Character.isSpaceChar(cp)) { break; }
            start += Character.charCount(cp);
        }
        while (start < end) {
            int cp = value.codePointBefore(end);
            if (!Character.isWhitespace(cp) && !Character.isSpaceChar(cp)) { break; }
            end -= Character.charCount(cp);
        }
        return value.substring(start, end);
    }

    private static boolean hasControl(String value) { return value.codePoints().anyMatch(Character::isISOControl); }
    private static BusinessException invalid(String message) { return new BusinessException(ErrorCode.VALIDATION_ERROR, message); }
    private static BusinessException missing(String message) { return new BusinessException(ErrorCode.NOT_FOUND, message); }
    private static BusinessException conflict() { return new BusinessException(ErrorCode.VERSION_CONFLICT, "Dữ liệu khách hàng đã đổi. Tải lại trước khi lưu."); }

    private static final class CustomerData {
        final String maKH, hoTen, sdt, email, diaChi;
        final boolean trangThai;
        CustomerData(String maKH, String hoTen, String sdt, String email, String diaChi, boolean trangThai) {
            this.maKH = maKH; this.hoTen = hoTen; this.sdt = sdt; this.email = email; this.diaChi = diaChi; this.trangThai = trangThai;
        }
    }
}
