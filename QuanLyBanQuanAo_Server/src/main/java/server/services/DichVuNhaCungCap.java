package server.services;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import common.dto.nhacungcap.DuLieuNhaCungCap;
import common.dto.nhacungcap.KetQuaTraCuuNhaCungCap;
import common.dto.nhacungcap.YeuCauLuuNhaCungCap;
import common.dto.nhacungcap.YeuCauTraCuuNhaCungCap;
import common.dto.nhacungcap.YeuCauTrangThaiNhaCungCap;
import common.enums.VaiTro;
import common.protocol.MaLoi;
import server.entities.NhaCungCap;
import server.entities.TaiKhoan;
import server.repositories.impl.LuuTruNhaCungCapJpa;
import server.utils.QuanLyJpa;

/** Supplier directory. Referenced suppliers are deactivated, never physically deleted. */
public final class DichVuNhaCungCap {
    private static final int MAX_PAGE = 1000000;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final QuanLyJpa jpa;
    private final DichVuXacThuc auth;

    public DichVuNhaCungCap(QuanLyJpa jpa, DichVuXacThuc auth) {
        this.jpa = Objects.requireNonNull(jpa, "jpa");
        this.auth = Objects.requireNonNull(auth, "auth");
    }

    public KetQuaTraCuuNhaCungCap search(String token, YeuCauTraCuuNhaCungCap input) {
        YeuCauTraCuuNhaCungCap request = input == null ? new YeuCauTraCuuNhaCungCap() : input;
        if (request.getPage() < 0 || request.getPage() > MAX_PAGE || request.getPageSize() < 1
                || request.getPageSize() > 100) {
            throw invalid("Phân trang nhà cung cấp không hợp lệ.");
        }
        String keyword = optional(request.getTuKhoa(), 200, "Từ khóa");
        request.setTuKhoa(keyword);
        return jpa.transaction(em -> {
            manager(em, token);
            LuuTruNhaCungCapJpa dao = new LuuTruNhaCungCapJpa(em);
            KetQuaTraCuuNhaCungCap result = new KetQuaTraCuuNhaCungCap();
            ArrayList<DuLieuNhaCungCap> items = new ArrayList<DuLieuNhaCungCap>();
            for (NhaCungCap row : dao.search(request)) {
                items.add(dto(row));
            }
            result.setItems(items);
            result.setTotal(dao.count(request));
            result.setPage(request.getPage());
            result.setPageSize(request.getPageSize());
            return result;
        });
    }

    public DuLieuNhaCungCap create(String token, DuLieuNhaCungCap request) {
        Data data = data(request, true);
        return jpa.transaction(em -> {
            manager(em, token);
            LuuTruNhaCungCapJpa dao = new LuuTruNhaCungCapJpa(em);
            if (dao.codeExists(data.id)) {
                throw invalid("Mã nhà cung cấp đã tồn tại.");
            }
            NhaCungCap row = new NhaCungCap();
            apply(row, data);
            em.persist(row);
            em.flush();
            return dto(row);
        });
    }

    public DuLieuNhaCungCap update(String token, YeuCauLuuNhaCungCap request) {
        if (request == null || request.getExpected() == null || request.getValue() == null) {
            throw invalid("Thiếu dữ liệu đối chiếu nhà cung cấp.");
        }
        Data value = data(request.getValue(), false);
        String id = code(request.getExpected().getMaNCC());
        if (!id.equals(value.id)) {
            throw invalid("Không thể đổi mã nhà cung cấp.");
        }
        return jpa.transaction(em -> {
            manager(em, token);
            NhaCungCap row = new LuuTruNhaCungCapJpa(em).findForUpdate(id)
                    .orElseThrow(() -> new NgoaiLeNghiepVu(MaLoi.NOT_FOUND, "Không tìm thấy nhà cung cấp."));
            em.refresh(row, LockModeType.PESSIMISTIC_WRITE);
            if (!matches(request.getExpected(), row)) {
                throw conflict();
            }
            if (request.getValue().getTrangThai() != row.getTrangThai()) {
                throw invalid("Trạng thái đổi bằng nút ngừng/khôi phục.");
            }
            apply(row, value);
            em.flush();
            return dto(row);
        });
    }

    public DuLieuNhaCungCap setActive(String token, YeuCauTrangThaiNhaCungCap request, boolean active) {
        if (request == null || request.getExpectedVersion() == null) {
            throw invalid("Thiếu phiên bản nhà cung cấp.");
        }
        String id = code(request.getMaNCC());
        return jpa.transaction(em -> {
            manager(em, token);
            NhaCungCap row = new LuuTruNhaCungCapJpa(em).findForUpdate(id)
                    .orElseThrow(() -> new NgoaiLeNghiepVu(MaLoi.NOT_FOUND, "Không tìm thấy nhà cung cấp."));
            em.refresh(row, LockModeType.PESSIMISTIC_WRITE);
            if (!request.getExpectedVersion().equals(row.getPhienBan())) {
                throw conflict();
            }
            if (row.getTrangThai() != active) {
                row.setTrangThai(active);
                em.flush();
            }
            return dto(row);
        });
    }

    private TaiKhoan manager(EntityManager em, String token) {
        TaiKhoan account = auth.requireAuthenticated(em, token);
        if (account.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY) {
            throw new NgoaiLeNghiepVu(MaLoi.FORBIDDEN, "Chỉ quản lý được quản lý nhà cung cấp.");
        }
        return account;
    }

    private static Data data(DuLieuNhaCungCap dto, boolean create) {
        if (dto == null) {
            throw invalid("Thiếu dữ liệu nhà cung cấp.");
        }
        if (create && !dto.getTrangThai()) {
            throw invalid("Nhà cung cấp mới phải ở trạng thái hoạt động.");
        }
        String phone = optional(dto.getSdt(), 20, "Số điện thoại");
        if (phone != null) {
            phone = phone.replaceAll("[ .()\\-]", "");
            if (!phone.matches("\\+?[0-9]{8,15}")) {
                throw invalid("Số điện thoại nhà cung cấp không hợp lệ.");
            }
        }
        String email = optional(dto.getEmail(), 254, "Email");
        if (email != null && !EMAIL.matcher(email).matches()) {
            throw invalid("Email nhà cung cấp không hợp lệ.");
        }
        return new Data(code(dto.getMaNCC()), required(dto.getTenNCC(), 150, "Tên nhà cung cấp"), phone,
                email, optional(dto.getDiaChi(), 255, "Địa chỉ"));
    }

    private static boolean matches(DuLieuNhaCungCap e, NhaCungCap r) {
        return e.getMaNCC() != null && e.getMaNCC().equals(r.getMaNCC())
                && Objects.equals(e.getTenNCC(), r.getTenNCC()) && Objects.equals(e.getSdt(), r.getSdt())
                && Objects.equals(e.getEmail(), r.getEmail()) && Objects.equals(e.getDiaChi(), r.getDiaChi())
                && e.getTrangThai() == r.getTrangThai() && Objects.equals(e.getPhienBan(), r.getPhienBan());
    }

    private static void apply(NhaCungCap row, Data d) {
        row.setMaNCC(d.id);
        row.setTenNCC(d.name);
        row.setSdt(d.phone);
        row.setEmail(d.email);
        row.setDiaChi(d.address);
    }

    private static DuLieuNhaCungCap dto(NhaCungCap r) {
        DuLieuNhaCungCap d = new DuLieuNhaCungCap();
        d.setMaNCC(r.getMaNCC());
        d.setTenNCC(r.getTenNCC());
        d.setSdt(r.getSdt());
        d.setEmail(r.getEmail());
        d.setDiaChi(r.getDiaChi());
        d.setTrangThai(r.getTrangThai());
        d.setPhienBan(r.getPhienBan());
        return d;
    }

    private static String code(String value) {
        String v = optional(value, 20, "Mã nhà cung cấp");
        if (v == null || !v.matches("[A-Za-z0-9_-]{1,20}")) {
            throw invalid("Mã nhà cung cấp chỉ gồm chữ, số, _ hoặc -.");
        }
        return v;
    }

    private static String required(String value, int max, String label) {
        String result = optional(value, max, label);
        if (result == null) {
            throw invalid(label + " không được để trống.");
        }
        return result;
    }

    private static String optional(String value, int max, String label) {
        if (value == null) {
            return null;
        }
        String v = Normalizer.normalize(value, Normalizer.Form.NFC).trim();
        if (v.isEmpty()) {
            return null;
        }
        if (v.length() > max) {
            throw invalid(label + " tối đa " + max + " ký tự.");
        }
        return v;
    }

    private static NgoaiLeNghiepVu invalid(String m) {
        return new NgoaiLeNghiepVu(MaLoi.VALIDATION_ERROR, m);
    }

    private static NgoaiLeNghiepVu conflict() {
        return new NgoaiLeNghiepVu(MaLoi.VERSION_CONFLICT, "Nhà cung cấp đã đổi. Tải lại trước khi lưu.");
    }

    private static final class Data {
        final String id, name, phone, email, address;

        Data(String id, String name, String phone, String email, String address) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.address = address;
        }
    }
}
