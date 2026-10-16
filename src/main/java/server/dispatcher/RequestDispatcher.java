package server.dispatcher;

import com.fasterxml.jackson.databind.JsonNode;
import common.dto.*;
import common.protocol.*;
import server.service.AuthService;
import server.service.SanPhamService;
import server.service.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(RequestDispatcher.class);
    private final AuthService auth;
    private final SanPhamService sanPham;
    private final server.service.HoaDonService hoaDon;
    private final server.service.NhanVienService nhanVien;
    private final server.service.KhachHangService khachHang;
    private final server.service.KhuyenMaiService khuyenMai;
    private final server.service.ThongKeService thongKe;
    private final server.service.LichSuKhoService lichSuKho;
    public RequestDispatcher(AuthService auth) { this(auth, null); }
    public RequestDispatcher(AuthService auth, SanPhamService sanPham) {
        this(auth, sanPham, null);
    }
    public RequestDispatcher(AuthService auth, SanPhamService sanPham, server.service.HoaDonService hoaDon) {
        this(auth, sanPham, hoaDon, null);
    }
    public RequestDispatcher(AuthService auth, SanPhamService sanPham, server.service.HoaDonService hoaDon,
            server.service.NhanVienService nhanVien) {
        this(auth, sanPham, hoaDon, nhanVien, null);
    }
    public RequestDispatcher(AuthService auth, SanPhamService sanPham, server.service.HoaDonService hoaDon,
            server.service.NhanVienService nhanVien, server.service.KhachHangService khachHang) {
        this(auth, sanPham, hoaDon, nhanVien, khachHang, null);
    }
    public RequestDispatcher(AuthService auth, SanPhamService sanPham, server.service.HoaDonService hoaDon,
            server.service.NhanVienService nhanVien, server.service.KhachHangService khachHang,
            server.service.KhuyenMaiService khuyenMai) {
        this(auth, sanPham, hoaDon, nhanVien, khachHang, khuyenMai, null);
    }
    public RequestDispatcher(AuthService auth, SanPhamService sanPham, server.service.HoaDonService hoaDon,
            server.service.NhanVienService nhanVien, server.service.KhachHangService khachHang,
            server.service.KhuyenMaiService khuyenMai, server.service.ThongKeService thongKe) {
        this(auth, sanPham, hoaDon, nhanVien, khachHang, khuyenMai, thongKe, null);
    }
    public RequestDispatcher(AuthService auth, SanPhamService sanPham, server.service.HoaDonService hoaDon,
            server.service.NhanVienService nhanVien, server.service.KhachHangService khachHang,
            server.service.KhuyenMaiService khuyenMai, server.service.ThongKeService thongKe,
            server.service.LichSuKhoService lichSuKho) {
        this.auth = auth; this.sanPham = sanPham; this.hoaDon = hoaDon; this.nhanVien = nhanVien;
        this.khachHang = khachHang; this.khuyenMai = khuyenMai; this.thongKe = thongKe; this.lichSuKho = lichSuKho;
    }
    public Response<?> dispatch(Request<JsonNode> request) {
        String id = request == null ? null : request.getRequestId();
        if (request == null || id == null || id.trim().isEmpty() || id.length() > 100
                || request.getAction() == null
                || (request.getSessionToken() != null && request.getSessionToken().length() > 128)) {
            return Response.failure(id, ErrorCode.INVALID_REQUEST, "Yêu cầu không hợp lệ.");
        }
        try {
            switch (request.getAction()) {
            case THEM_LOAI_SAN_PHAM:
            case CAP_NHAT_LOAI_SAN_PHAM:
                return Response.success(id, products().management().category(request.getSessionToken(),
                        payload(request, DanhMucSaveRequest.class), request.getAction() == Action.THEM_LOAI_SAN_PHAM));
            case THEM_THUONG_HIEU:
            case CAP_NHAT_THUONG_HIEU:
                return Response.success(id, products().management().brand(request.getSessionToken(),
                        payload(request, DanhMucSaveRequest.class), request.getAction() == Action.THEM_THUONG_HIEU));
            case THEM_SAN_PHAM:
            case CAP_NHAT_SAN_PHAM:
            case NGUNG_KINH_DOANH_SAN_PHAM:
                return Response.success(id, products().management().product(request.getSessionToken(),
                        payload(request, SanPhamSaveRequest.class), request.getAction() == Action.THEM_SAN_PHAM ? 0
                        : request.getAction() == Action.CAP_NHAT_SAN_PHAM ? 1 : 2));
            case THEM_BIEN_THE:
            case CAP_NHAT_BIEN_THE:
                return Response.success(id, products().management().variant(request.getSessionToken(),
                        variantStockPayload(request), request.getAction() == Action.THEM_BIEN_THE));
            case LAP_HOA_DON:
                return Response.success(id, invoices().create(request.getSessionToken(), payload(request, HoaDonCreateRequest.class)));
            case THEM_CHI_TIET_HOA_DON:
            case CAP_NHAT_SO_LUONG:
            case BO_MAT_HANG:
                return Response.success(id, invoices().editLine(request.getSessionToken(), payload(request, HoaDonLineRequest.class),
                        request.getAction() == Action.THEM_CHI_TIET_HOA_DON ? 0 : request.getAction() == Action.CAP_NHAT_SO_LUONG ? 1 : 2));
            case THANH_TOAN_HOA_DON:
                return Response.success(id, invoices().pay(request.getSessionToken(), payload(request, HoaDonThanhToanRequest.class)));
            case HUY_HOA_DON_NHAP:
                return Response.success(id, invoices().cancel(request.getSessionToken(), payload(request, HoaDonVersionRequest.class)));
            case XEM_CHI_TIET_HOA_DON:
                return Response.success(id, invoices().detail(request.getSessionToken(), payload(request, HoaDonVersionRequest.class).getMaHD()));
            case TRA_CUU_HOA_DON:
                return Response.success(id, invoices().search(request.getSessionToken(), payload(request, HoaDonSearchRequest.class)));
            case TRA_CUU_THUE:
                return Response.success(id, invoices().taxes(request.getSessionToken()));
            case DANG_NHAP:
                return Response.success(id, auth.login(payload(request, LoginRequest.class)));
            case DANG_XUAT:
                auth.logout(request.getSessionToken()); return Response.success(id, null);
            case XEM_THONG_TIN_CA_NHAN:
                return Response.success(id, auth.profile(request.getSessionToken()));
            case DOI_MAT_KHAU:
                auth.changePassword(request.getSessionToken(), payload(request, DoiMatKhauRequest.class));
                return Response.success(id, null);
            case TRA_CUU_SAN_PHAM:
                return Response.success(id, products().search(request.getSessionToken(),
                        payload(request, SanPhamSearchRequest.class)));
            case TRA_CUU_NHAN_VIEN:
                return Response.success(id, employees().search(request.getSessionToken(),
                        payload(request, NhanVienSearchRequest.class)));
            case TRA_CUU_KHACH_HANG:
                return Response.success(id, customers().search(request.getSessionToken(),
                        payload(request, KhachHangSearchRequest.class)));
            case TRA_CUU_KHUYEN_MAI:
                return Response.success(id, promotions().search(request.getSessionToken(),
                        payload(request, KhuyenMaiSearchRequest.class)));
            case XEM_CHI_TIET_KHUYEN_MAI:
                return Response.success(id, promotions().detail(request.getSessionToken(),
                        payload(request, KhuyenMaiDetailRequest.class)));
            case THEM_KHUYEN_MAI:
                return Response.success(id, promotions().create(request.getSessionToken(),
                        payload(request, KhuyenMaiDTO.class)));
            case CAP_NHAT_KHUYEN_MAI:
                return Response.success(id, promotions().update(request.getSessionToken(),
                        payload(request, KhuyenMaiSaveRequest.class)));
            case NGUNG_HOAT_DONG_KHUYEN_MAI:
                return Response.success(id, promotions().setActive(request.getSessionToken(),
                        payload(request, KhuyenMaiStatusRequest.class), false));
            case KICH_HOAT_KHUYEN_MAI:
                return Response.success(id, promotions().setActive(request.getSessionToken(),
                        payload(request, KhuyenMaiStatusRequest.class), true));
            case THONG_KE_DOANH_THU_THEO_THOI_GIAN:
                return Response.success(id, statistics().byTime(request.getSessionToken(),
                        payload(request, ThongKeDoanhThuRequest.class)));
            case THONG_KE_DOANH_THU_THEO_SAN_PHAM:
                return Response.success(id, statistics().byProduct(request.getSessionToken(),
                        payload(request, ThongKeDoanhThuRequest.class)));
            case TRA_CUU_LICH_SU_KHO:
                return Response.success(id, stockHistory().search(request.getSessionToken(),
                        payload(request, LichSuKhoSearchRequest.class)));
            case DIEU_CHINH_TON_KHO:
                return Response.success(id, stockHistory().adjust(request.getSessionToken(),
                        stockAdjustmentPayload(request)));
            case XEM_CHI_TIET_KHACH_HANG:
                return Response.success(id, customers().detail(request.getSessionToken(),
                        payload(request, KhachHangDetailRequest.class)));
            case THEM_KHACH_HANG:
                return Response.success(id, customers().create(request.getSessionToken(),
                        payload(request, KhachHangDTO.class)));
            case CAP_NHAT_KHACH_HANG:
                return Response.success(id, customers().update(request.getSessionToken(),
                        payload(request, KhachHangSaveRequest.class)));
            case NGUNG_HOAT_DONG_KHACH_HANG:
                return Response.success(id, customers().setActive(request.getSessionToken(),
                        payload(request, KhachHangStatusRequest.class), false));
            case KICH_HOAT_KHACH_HANG:
                return Response.success(id, customers().setActive(request.getSessionToken(),
                        payload(request, KhachHangStatusRequest.class), true));
            case XEM_CHI_TIET_NHAN_VIEN:
                return Response.success(id, employees().detail(request.getSessionToken(),
                        payload(request, NhanVienDetailRequest.class)));
            case THEM_NHAN_VIEN:
                return Response.success(id, employees().create(request.getSessionToken(),
                        payload(request, NhanVienCreateRequest.class)));
            case CAP_NHAT_NHAN_VIEN:
                return Response.success(id, employees().update(request.getSessionToken(),
                        payload(request, NhanVienSaveRequest.class)));
            case NGUNG_HOAT_DONG_NHAN_VIEN:
                return Response.success(id, employees().setEmployeeActive(request.getSessionToken(),
                        payload(request, NhanVienStatusRequest.class), false));
            case KICH_HOAT_NHAN_VIEN:
                return Response.success(id, employees().setEmployeeActive(request.getSessionToken(),
                        payload(request, NhanVienStatusRequest.class), true));
            case TAO_TAI_KHOAN:
                return Response.success(id, employees().createAccount(request.getSessionToken(),
                        payload(request, TaiKhoanCreateRequest.class)));
            case KHOA_TAI_KHOAN:
                return Response.success(id, employees().setAccountActive(request.getSessionToken(),
                        payload(request, TaiKhoanStatusRequest.class), false));
            case MO_KHOA_TAI_KHOAN:
                return Response.success(id, employees().setAccountActive(request.getSessionToken(),
                        payload(request, TaiKhoanStatusRequest.class), true));
            case CAP_NHAT_VAI_TRO_TAI_KHOAN:
                return Response.success(id, employees().changeRole(request.getSessionToken(),
                        payload(request, TaiKhoanRoleRequest.class)));
            case XEM_CHI_TIET_SAN_PHAM:
                return Response.success(id, products().detail(request.getSessionToken(),
                        payload(request, SanPhamDetailRequest.class).getMaSP()));
            case TRA_CUU_LOAI_SAN_PHAM:
                return Response.success(id, products().categories(request.getSessionToken()));
            case TRA_CUU_THUONG_HIEU:
                return Response.success(id, products().brands(request.getSessionToken()));
            default:
                return Response.failure(id, ErrorCode.INVALID_REQUEST, "Chức năng này chưa được triển khai.");
            }
        } catch (BusinessException failure) {
            return Response.failure(id, failure.getCode(), failure.getMessage());
        } catch (IllegalArgumentException | com.fasterxml.jackson.core.JsonProcessingException invalid) {
            return Response.failure(id, ErrorCode.INVALID_REQUEST, "Dữ liệu yêu cầu không đúng cấu trúc.");
        } catch (RuntimeException internal) {
            for (Throwable cause = internal; cause != null; cause = cause.getCause()) {
                if (cause instanceof javax.persistence.OptimisticLockException
                        || cause instanceof javax.persistence.PessimisticLockException
                        || cause instanceof javax.persistence.LockTimeoutException
                        || cause instanceof org.hibernate.StaleStateException) {
                    return Response.failure(id, ErrorCode.VERSION_CONFLICT, "Dữ liệu đang được thay đổi. Hãy tải lại trước khi thao tác.");
                }
                if (cause instanceof org.hibernate.exception.ConstraintViolationException) {
                    String constraint = ((org.hibernate.exception.ConstraintViolationException) cause).getConstraintName();
                    if ("UQ_LichSuKho_OperationId".equalsIgnoreCase(constraint)) {
                        return Response.failure(id, ErrorCode.VERSION_CONFLICT,
                                "operationId đã được dùng bởi một thao tác kho khác.");
                    }
                    return Response.failure(id, ErrorCode.VALIDATION_ERROR, "Trùng mã/tên/màu-size hoặc dữ liệu vi phạm ràng buộc CSDL. Hãy tải lại để kiểm tra.");
                }
            }
            // Không log request, exception message có thể chứa dữ liệu DB/mật khẩu.
            LOG.error("Request failed: {}", internal.getClass().getSimpleName());
            return Response.failure(id, ErrorCode.SERVER_ERROR, "Không xử lý được yêu cầu. Vui lòng thử lại.");
        }
    }
    private static <T> T payload(Request<JsonNode> request, Class<T> type)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode node = request.getPayload();
        if (node == null || !node.isObject()) { throw new IllegalArgumentException("Missing payload"); }
        return JsonCodec.mapper().treeToValue(node, type);
    }
    private static DieuChinhTonKhoRequest stockAdjustmentPayload(Request<JsonNode> request)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode node = request.getPayload();
        if (node == null || !node.isObject() || !node.path("soLuongMoi").isIntegralNumber()
                || !node.path("expectedVersion").isIntegralNumber()) {
            throw new IllegalArgumentException("Stock quantity and version must be JSON integers.");
        }
        return JsonCodec.mapper().treeToValue(node, DieuChinhTonKhoRequest.class);
    }
    private static BienTheDTO variantStockPayload(Request<JsonNode> request)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode node = request.getPayload();
        if (node == null || !node.isObject() || !node.path("soLuongTon").isIntegralNumber()) {
            throw new IllegalArgumentException("Variant stock must be a JSON integer.");
        }
        return JsonCodec.mapper().treeToValue(node, BienTheDTO.class);
    }
    private SanPhamService products() {
        if (sanPham == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Dịch vụ sản phẩm chưa được cấu hình.");
        }
        return sanPham;
    }
    private server.service.HoaDonService invoices() {
        if (hoaDon == null) { throw new BusinessException(ErrorCode.INVALID_REQUEST, "Dịch vụ hóa đơn chưa được cấu hình."); }
        return hoaDon;
    }
    private server.service.NhanVienService employees() {
        if (nhanVien == null) { throw new BusinessException(ErrorCode.INVALID_REQUEST, "Dịch vụ nhân viên chưa được cấu hình."); }
        return nhanVien;
    }
    private server.service.KhachHangService customers() {
        if (khachHang == null) { throw new BusinessException(ErrorCode.INVALID_REQUEST, "Dịch vụ khách hàng chưa được cấu hình."); }
        return khachHang;
    }
    private server.service.KhuyenMaiService promotions() {
        if (khuyenMai == null) { throw new BusinessException(ErrorCode.INVALID_REQUEST, "Dịch vụ khuyến mãi chưa được cấu hình."); }
        return khuyenMai;
    }
    private server.service.ThongKeService statistics() {
        if (thongKe == null) { throw new BusinessException(ErrorCode.INVALID_REQUEST, "Dịch vụ thống kê chưa được cấu hình."); }
        return thongKe;
    }
    private server.service.LichSuKhoService stockHistory() {
        if (lichSuKho == null) { throw new BusinessException(ErrorCode.INVALID_REQUEST, "Dịch vụ lịch sử kho chưa được cấu hình."); }
        return lichSuKho;
    }
}
