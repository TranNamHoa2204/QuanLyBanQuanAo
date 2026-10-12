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
    private server.service.NhanVienService employees;
    private server.service.KhachHangService customers;
    private server.service.KhuyenMaiService promotions;
    private server.service.ThongKeService reports;
    public RequestDispatcher(AuthService auth) { this(auth, null); }
    public RequestDispatcher(AuthService auth, SanPhamService sanPham) {
        this(auth, sanPham, null);
    }
    public RequestDispatcher(AuthService auth, SanPhamService sanPham, server.service.HoaDonService hoaDon) {
        this.auth = auth; this.sanPham = sanPham; this.hoaDon = hoaDon;
    }
    public RequestDispatcher(AuthService auth, SanPhamService products, server.service.HoaDonService invoices,
            server.persistence.JpaManager jpa) {
        this(auth, products, invoices);
        employees = new server.service.NhanVienService(jpa, auth);
        customers = new server.service.KhachHangService(jpa, auth);
        promotions = new server.service.KhuyenMaiService(jpa, auth);
        reports = new server.service.ThongKeService(jpa, auth);
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
            case TRA_CUU_KHACH_HANG:
                configured(customers); return Response.success(id, customers.search(request.getSessionToken(), payload(request, ManagementSearchRequest.class)));
            case THEM_KHACH_HANG:
            case CAP_NHAT_KHACH_HANG:
            case NGUNG_HOAT_DONG_KHACH_HANG:
                configured(customers); return Response.success(id, customers.save(request.getSessionToken(), payload(request, ManagementSaveRequest.class),
                        request.getAction() == Action.THEM_KHACH_HANG ? 0 : request.getAction() == Action.CAP_NHAT_KHACH_HANG ? 1 : 2));
            case TRA_CUU_NHAN_VIEN:
                configured(employees); return Response.success(id, employees.search(request.getSessionToken(), payload(request, ManagementSearchRequest.class)));
            case THEM_NHAN_VIEN:
            case CAP_NHAT_NHAN_VIEN:
            case NGUNG_HOAT_DONG_NHAN_VIEN:
                configured(employees); return Response.success(id, employees.save(request.getSessionToken(), payload(request, ManagementSaveRequest.class),
                        request.getAction() == Action.THEM_NHAN_VIEN ? 0 : request.getAction() == Action.CAP_NHAT_NHAN_VIEN ? 1 : 2));
            case TRA_CUU_KHUYEN_MAI:
                configured(promotions); return Response.success(id, promotions.search(request.getSessionToken(), payload(request, ManagementSearchRequest.class)));
            case THEM_KHUYEN_MAI:
            case CAP_NHAT_KHUYEN_MAI:
            case NGUNG_HOAT_DONG_KHUYEN_MAI:
                configured(promotions); return Response.success(id, promotions.save(request.getSessionToken(), payload(request, ManagementSaveRequest.class),
                        request.getAction() == Action.THEM_KHUYEN_MAI ? 0 : request.getAction() == Action.CAP_NHAT_KHUYEN_MAI ? 1 : 2));
            case THONG_KE_DOANH_THU_THEO_THOI_GIAN:
            case THONG_KE_DOANH_THU_THEO_SAN_PHAM:
                configured(reports); return Response.success(id, reports.report(request.getSessionToken(), payload(request, RevenueRequest.class),
                        request.getAction() == Action.THONG_KE_DOANH_THU_THEO_SAN_PHAM));
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
                        payload(request, BienTheDTO.class), request.getAction() == Action.THEM_BIEN_THE));
            case LAP_HOA_DON:
                return Response.success(id, invoices().create(request.getSessionToken(), payload(request, HoaDonCreateRequest.class)));
            case THEM_CHI_TIET_HOA_DON:
            case CAP_NHAT_SO_LUONG:
            case BO_MAT_HANG:
                return Response.success(id, invoices().editLine(request.getSessionToken(), payload(request, HoaDonLineRequest.class),
                        request.getAction() == Action.THEM_CHI_TIET_HOA_DON ? 0 : request.getAction() == Action.CAP_NHAT_SO_LUONG ? 1 : 2));
            case THANH_TOAN_HOA_DON:
                return Response.success(id, invoices().pay(request.getSessionToken(), payload(request, HoaDonVersionRequest.class)));
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
    private static void configured(Object service) {
        if (service == null) { throw new BusinessException(ErrorCode.INVALID_REQUEST, "Dịch vụ chưa được cấu hình."); }
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
}
