package server.dispatcher;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import common.dto.danhmuc.YeuCauLuuDanhMuc;
import common.dto.danhmuc.YeuCauCapMa;
import common.dto.hoadon.YeuCauDongHoaDon;
import common.dto.hoadon.YeuCauGanKhachHangHoaDon;
import common.dto.hoadon.YeuCauPhienBanHoaDon;
import common.dto.hoadon.YeuCauTaoHoaDon;
import common.dto.hoadon.YeuCauThanhToanHoaDon;
import common.dto.hoadon.YeuCauTraCuuHoaDon;
import common.dto.khachhang.DuLieuKhachHang;
import common.dto.khachhang.YeuCauChiTietKhachHang;
import common.dto.khachhang.YeuCauLuuKhachHang;
import common.dto.khachhang.YeuCauTraCuuKhachHang;
import common.dto.khachhang.YeuCauTrangThaiKhachHang;
import common.dto.kho.YeuCauDieuChinhTonKho;
import common.dto.kho.YeuCauTraCuuLichSuKho;
import common.dto.khuyenmai.DuLieuKhuyenMai;
import common.dto.khuyenmai.YeuCauChiTietKhuyenMai;
import common.dto.khuyenmai.YeuCauLuuKhuyenMai;
import common.dto.khuyenmai.YeuCauTraCuuKhuyenMai;
import common.dto.khuyenmai.YeuCauTrangThaiKhuyenMai;
import common.dto.nhacungcap.DuLieuNhaCungCap;
import common.dto.nhacungcap.YeuCauLuuNhaCungCap;
import common.dto.nhacungcap.YeuCauTraCuuNhaCungCap;
import common.dto.nhacungcap.YeuCauTrangThaiNhaCungCap;
import common.dto.nhanvien.YeuCauChiTietNhanVien;
import common.dto.nhanvien.YeuCauLuuNhanVien;
import common.dto.nhanvien.YeuCauTaoNhanVien;
import common.dto.nhanvien.YeuCauTraCuuNhanVien;
import common.dto.nhanvien.YeuCauTrangThaiNhanVien;
import common.dto.phieunhap.YeuCauDongPhieuNhap;
import common.dto.phieunhap.YeuCauPhienBanPhieuNhap;
import common.dto.phieunhap.YeuCauTaoPhieuNhap;
import common.dto.phieunhap.YeuCauTraCuuPhieuNhap;
import common.dto.phieunhap.YeuCauXacNhanPhieuNhap;
import common.dto.sanpham.DuLieuBienThe;
import common.dto.sanpham.YeuCauChiTietSanPham;
import common.dto.sanpham.YeuCauLuuSanPham;
import common.dto.sanpham.YeuCauTraCuuSanPham;
import common.dto.taikhoan.YeuCauDangNhap;
import common.dto.taikhoan.YeuCauDoiMatKhau;
import common.dto.taikhoan.YeuCauTaoTaiKhoan;
import common.dto.taikhoan.YeuCauTrangThaiTaiKhoan;
import common.dto.taikhoan.YeuCauVaiTroTaiKhoan;
import common.dto.thongke.YeuCauThongKeDoanhThu;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;
import common.protocol.YeuCau;
import server.services.DichVuCapMa;
import server.services.DichVuHoaDon;
import server.services.DichVuKhachHang;
import server.services.DichVuKhuyenMai;
import server.services.DichVuLichSuKho;
import server.services.DichVuNhaCungCap;
import server.services.DichVuNhanVien;
import server.services.DichVuPhieuNhap;
import server.services.DichVuSanPham;
import server.services.DichVuThongKe;
import server.services.DichVuXacThuc;
import server.services.NgoaiLeNghiepVu;

public final class DieuPhoiYeuCau {
    private static final Logger LOG = LoggerFactory.getLogger(DieuPhoiYeuCau.class);
    private final DichVuXacThuc auth;
    private final DichVuSanPham sanPham;
    private final DichVuHoaDon hoaDon;
    private final DichVuNhanVien nhanVien;
    private final DichVuKhachHang khachHang;
    private final DichVuKhuyenMai khuyenMai;
    private final DichVuThongKe thongKe;
    private final DichVuLichSuKho lichSuKho;
    private final DichVuNhaCungCap nhaCungCap;
    private final DichVuPhieuNhap phieuNhap;
    private final DichVuCapMa capMa;

    public DieuPhoiYeuCau(DichVuXacThuc auth) {
        this(auth, null);
    }

    public DieuPhoiYeuCau(DichVuXacThuc auth, DichVuSanPham sanPham) {
        this(auth, sanPham, null);
    }

    public DieuPhoiYeuCau(DichVuXacThuc auth, DichVuSanPham sanPham, DichVuHoaDon hoaDon) {
        this(auth, sanPham, hoaDon, null);
    }

    public DieuPhoiYeuCau(DichVuXacThuc auth, DichVuSanPham sanPham, DichVuHoaDon hoaDon,
            DichVuNhanVien nhanVien) {
        this(auth, sanPham, hoaDon, nhanVien, null);
    }

    public DieuPhoiYeuCau(DichVuXacThuc auth, DichVuSanPham sanPham, DichVuHoaDon hoaDon,
            DichVuNhanVien nhanVien, DichVuKhachHang khachHang) {
        this(auth, sanPham, hoaDon, nhanVien, khachHang, null);
    }

    public DieuPhoiYeuCau(DichVuXacThuc auth, DichVuSanPham sanPham, DichVuHoaDon hoaDon,
            DichVuNhanVien nhanVien, DichVuKhachHang khachHang, DichVuKhuyenMai khuyenMai) {
        this(auth, sanPham, hoaDon, nhanVien, khachHang, khuyenMai, null);
    }

    public DieuPhoiYeuCau(DichVuXacThuc auth, DichVuSanPham sanPham, DichVuHoaDon hoaDon,
            DichVuNhanVien nhanVien, DichVuKhachHang khachHang, DichVuKhuyenMai khuyenMai,
            DichVuThongKe thongKe) {
        this(auth, sanPham, hoaDon, nhanVien, khachHang, khuyenMai, thongKe, null);
    }

    public DieuPhoiYeuCau(DichVuXacThuc auth, DichVuSanPham sanPham, DichVuHoaDon hoaDon,
            DichVuNhanVien nhanVien, DichVuKhachHang khachHang, DichVuKhuyenMai khuyenMai,
            DichVuThongKe thongKe, DichVuLichSuKho lichSuKho) {
        this(auth, sanPham, hoaDon, nhanVien, khachHang, khuyenMai, thongKe, lichSuKho, null, null);
    }

    public DieuPhoiYeuCau(DichVuXacThuc auth, DichVuSanPham sanPham, DichVuHoaDon hoaDon,
            DichVuNhanVien nhanVien, DichVuKhachHang khachHang, DichVuKhuyenMai khuyenMai,
            DichVuThongKe thongKe, DichVuLichSuKho lichSuKho, DichVuNhaCungCap nhaCungCap,
            DichVuPhieuNhap phieuNhap) {
        this(auth, sanPham, hoaDon, nhanVien, khachHang, khuyenMai, thongKe, lichSuKho, nhaCungCap, phieuNhap, null);
    }

    public DieuPhoiYeuCau(DichVuXacThuc auth, DichVuSanPham sanPham, DichVuHoaDon hoaDon,
            DichVuNhanVien nhanVien, DichVuKhachHang khachHang, DichVuKhuyenMai khuyenMai,
            DichVuThongKe thongKe, DichVuLichSuKho lichSuKho, DichVuNhaCungCap nhaCungCap,
            DichVuPhieuNhap phieuNhap, DichVuCapMa capMa) {
        this.auth = auth;
        this.sanPham = sanPham;
        this.hoaDon = hoaDon;
        this.nhanVien = nhanVien;
        this.khachHang = khachHang;
        this.khuyenMai = khuyenMai;
        this.thongKe = thongKe;
        this.lichSuKho = lichSuKho;
        this.nhaCungCap = nhaCungCap;
        this.phieuNhap = phieuNhap;
        this.capMa = capMa;
    }

    public PhanHoi<?> dispatch(YeuCau<JsonNode> request) {
        String id = request == null ? null : request.getRequestId();
        if (request == null || id == null || id.trim().isEmpty() || id.length() > 100
                || request.getAction() == null
                || (request.getSessionToken() != null && request.getSessionToken().length() > 128)) {
            return PhanHoi.failure(id, MaLoi.INVALID_REQUEST, "Yêu cầu không hợp lệ.");
        }
        try {
            switch (request.getAction()) {
            case CAP_MA_TU_DONG:
                if (capMa == null) {
                    throw new NgoaiLeNghiepVu(MaLoi.SERVER_ERROR, "Chưa khởi tạo dịch vụ cấp mã.");
                }
                return PhanHoi.success(id, capMa.next(request.getSessionToken(), payload(request, YeuCauCapMa.class).getLoai()));
            case THEM_LOAI_SAN_PHAM:
            case CAP_NHAT_LOAI_SAN_PHAM:
                return PhanHoi.success(id,
                        products().management().category(request.getSessionToken(),
                                payload(request, YeuCauLuuDanhMuc.class),
                                request.getAction() == HanhDong.THEM_LOAI_SAN_PHAM));
            case THEM_THUONG_HIEU:
            case CAP_NHAT_THUONG_HIEU:
                return PhanHoi.success(id,
                        products().management().brand(request.getSessionToken(),
                                payload(request, YeuCauLuuDanhMuc.class),
                                request.getAction() == HanhDong.THEM_THUONG_HIEU));
            case THEM_SAN_PHAM:
            case CAP_NHAT_SAN_PHAM:
            case NGUNG_KINH_DOANH_SAN_PHAM:
                return PhanHoi.success(id,
                        products().management().product(request.getSessionToken(),
                                payload(request, YeuCauLuuSanPham.class),
                                request.getAction() == HanhDong.THEM_SAN_PHAM ? 0
                                        : request.getAction() == HanhDong.CAP_NHAT_SAN_PHAM ? 1 : 2));
            case THEM_BIEN_THE:
            case CAP_NHAT_BIEN_THE:
                return PhanHoi.success(id, products().management().variant(request.getSessionToken(),
                        variantStockPayload(request), request.getAction() == HanhDong.THEM_BIEN_THE));
            case LAP_HOA_DON:
                return PhanHoi.success(id, invoices().create(request.getSessionToken(),
                        payload(request, YeuCauTaoHoaDon.class)));
            case THEM_CHI_TIET_HOA_DON:
            case CAP_NHAT_SO_LUONG:
            case BO_MAT_HANG:
                return PhanHoi.success(id,
                        invoices().editLine(request.getSessionToken(), invoiceLinePayload(request),
                                request.getAction() == HanhDong.THEM_CHI_TIET_HOA_DON ? 0
                                        : request.getAction() == HanhDong.CAP_NHAT_SO_LUONG ? 1 : 2));
            case THANH_TOAN_HOA_DON:
                return PhanHoi.success(id, invoices().pay(request.getSessionToken(),
                        payload(request, YeuCauThanhToanHoaDon.class)));
            case HUY_HOA_DON_NHAP:
                return PhanHoi.success(id, invoices().cancel(request.getSessionToken(),
                        payload(request, YeuCauPhienBanHoaDon.class)));
            case CAP_NHAT_KHACH_HANG_HOA_DON_NHAP:
                return PhanHoi.success(id,
                        invoices().setCustomer(request.getSessionToken(), invoiceCustomerPayload(request)));
            case XEM_CHI_TIET_HOA_DON:
                return PhanHoi.success(id, invoices().detail(request.getSessionToken(),
                        payload(request, YeuCauPhienBanHoaDon.class).getMaHD()));
            case TRA_CUU_HOA_DON:
                return PhanHoi.success(id, invoices().search(request.getSessionToken(),
                        payload(request, YeuCauTraCuuHoaDon.class)));
            case TRA_CUU_THUE:
                return PhanHoi.success(id, invoices().taxes(request.getSessionToken()));
            case DANG_NHAP:
                return PhanHoi.success(id, auth.login(payload(request, YeuCauDangNhap.class)));
            case DANG_XUAT:
                auth.logout(request.getSessionToken());
                return PhanHoi.success(id, null);
            case XEM_THONG_TIN_CA_NHAN:
                return PhanHoi.success(id, auth.profile(request.getSessionToken()));
            case DOI_MAT_KHAU:
                auth.changePassword(request.getSessionToken(), payload(request, YeuCauDoiMatKhau.class));
                return PhanHoi.success(id, null);
            case TRA_CUU_SAN_PHAM:
                return PhanHoi.success(id, products().search(request.getSessionToken(),
                        payload(request, YeuCauTraCuuSanPham.class)));
            case TRA_CUU_NHAN_VIEN:
                return PhanHoi.success(id, employees().search(request.getSessionToken(),
                        payload(request, YeuCauTraCuuNhanVien.class)));
            case TRA_CUU_KHACH_HANG:
                return PhanHoi.success(id, customers().search(request.getSessionToken(),
                        payload(request, YeuCauTraCuuKhachHang.class)));
            case TRA_CUU_KHUYEN_MAI:
                return PhanHoi.success(id, promotions().search(request.getSessionToken(),
                        payload(request, YeuCauTraCuuKhuyenMai.class)));
            case XEM_CHI_TIET_KHUYEN_MAI:
                return PhanHoi.success(id, promotions().detail(request.getSessionToken(),
                        payload(request, YeuCauChiTietKhuyenMai.class)));
            case THEM_KHUYEN_MAI:
                return PhanHoi.success(id, promotions().create(request.getSessionToken(),
                        payload(request, DuLieuKhuyenMai.class)));
            case CAP_NHAT_KHUYEN_MAI:
                return PhanHoi.success(id, promotions().update(request.getSessionToken(),
                        payload(request, YeuCauLuuKhuyenMai.class)));
            case NGUNG_HOAT_DONG_KHUYEN_MAI:
                return PhanHoi.success(id, promotions().setActive(request.getSessionToken(),
                        payload(request, YeuCauTrangThaiKhuyenMai.class), false));
            case KICH_HOAT_KHUYEN_MAI:
                return PhanHoi.success(id, promotions().setActive(request.getSessionToken(),
                        payload(request, YeuCauTrangThaiKhuyenMai.class), true));
            case THONG_KE_DOANH_THU_THEO_THOI_GIAN:
                return PhanHoi.success(id, statistics().byTime(request.getSessionToken(),
                        payload(request, YeuCauThongKeDoanhThu.class)));
            case THONG_KE_DOANH_THU_THEO_SAN_PHAM:
                return PhanHoi.success(id, statistics().byProduct(request.getSessionToken(),
                        payload(request, YeuCauThongKeDoanhThu.class)));
            case TRA_CUU_LICH_SU_KHO:
                return PhanHoi.success(id, stockHistory().search(request.getSessionToken(),
                        payload(request, YeuCauTraCuuLichSuKho.class)));
            case DIEU_CHINH_TON_KHO:
                return PhanHoi.success(id,
                        stockHistory().adjust(request.getSessionToken(), stockAdjustmentPayload(request)));
            case TRA_CUU_NHA_CUNG_CAP:
                return PhanHoi.success(id, suppliers().search(request.getSessionToken(),
                        payload(request, YeuCauTraCuuNhaCungCap.class)));
            case THEM_NHA_CUNG_CAP:
                return PhanHoi.success(id, suppliers().create(request.getSessionToken(),
                        payload(request, DuLieuNhaCungCap.class)));
            case CAP_NHAT_NHA_CUNG_CAP:
                return PhanHoi.success(id, suppliers().update(request.getSessionToken(),
                        payload(request, YeuCauLuuNhaCungCap.class)));
            case NGUNG_HOAT_DONG_NHA_CUNG_CAP:
            case KICH_HOAT_NHA_CUNG_CAP:
                return PhanHoi.success(id,
                        suppliers().setActive(request.getSessionToken(),
                                payload(request, YeuCauTrangThaiNhaCungCap.class),
                                request.getAction() == HanhDong.KICH_HOAT_NHA_CUNG_CAP));
            case TRA_CUU_PHIEU_NHAP:
                return PhanHoi.success(id, receipts().search(request.getSessionToken(),
                        payload(request, YeuCauTraCuuPhieuNhap.class)));
            case XEM_CHI_TIET_PHIEU_NHAP:
                return PhanHoi.success(id, receipts().detail(request.getSessionToken(),
                        payload(request, YeuCauPhienBanPhieuNhap.class).getMaPhieuNhap()));
            case TAO_PHIEU_NHAP:
                return PhanHoi.success(id, receipts().create(request.getSessionToken(),
                        payload(request, YeuCauTaoPhieuNhap.class)));
            case THEM_DONG_PHIEU_NHAP:
            case CAP_NHAT_DONG_PHIEU_NHAP:
            case BO_DONG_PHIEU_NHAP:
                return PhanHoi.success(id,
                        receipts().editLine(request.getSessionToken(), receiptLinePayload(request),
                                request.getAction() == HanhDong.THEM_DONG_PHIEU_NHAP ? 0
                                        : request.getAction() == HanhDong.CAP_NHAT_DONG_PHIEU_NHAP ? 1 : 2));
            case XAC_NHAN_PHIEU_NHAP:
                return PhanHoi.success(id,
                        receipts().confirm(request.getSessionToken(), receiptConfirmPayload(request)));
            case HUY_PHIEU_NHAP:
                return PhanHoi.success(id,
                        receipts().cancel(request.getSessionToken(), receiptVersionPayload(request)));
            case XEM_CHI_TIET_KHACH_HANG:
                return PhanHoi.success(id, customers().detail(request.getSessionToken(),
                        payload(request, YeuCauChiTietKhachHang.class)));
            case THEM_KHACH_HANG:
                return PhanHoi.success(id, customers().create(request.getSessionToken(),
                        payload(request, DuLieuKhachHang.class)));
            case CAP_NHAT_KHACH_HANG:
                return PhanHoi.success(id, customers().update(request.getSessionToken(),
                        payload(request, YeuCauLuuKhachHang.class)));
            case NGUNG_HOAT_DONG_KHACH_HANG:
                return PhanHoi.success(id, customers().setActive(request.getSessionToken(),
                        payload(request, YeuCauTrangThaiKhachHang.class), false));
            case KICH_HOAT_KHACH_HANG:
                return PhanHoi.success(id, customers().setActive(request.getSessionToken(),
                        payload(request, YeuCauTrangThaiKhachHang.class), true));
            case XEM_CHI_TIET_NHAN_VIEN:
                return PhanHoi.success(id, employees().detail(request.getSessionToken(),
                        payload(request, YeuCauChiTietNhanVien.class)));
            case THEM_NHAN_VIEN:
                return PhanHoi.success(id, employees().create(request.getSessionToken(),
                        payload(request, YeuCauTaoNhanVien.class)));
            case CAP_NHAT_NHAN_VIEN:
                return PhanHoi.success(id, employees().update(request.getSessionToken(),
                        payload(request, YeuCauLuuNhanVien.class)));
            case NGUNG_HOAT_DONG_NHAN_VIEN:
                return PhanHoi.success(id, employees().setEmployeeActive(request.getSessionToken(),
                        payload(request, YeuCauTrangThaiNhanVien.class), false));
            case KICH_HOAT_NHAN_VIEN:
                return PhanHoi.success(id, employees().setEmployeeActive(request.getSessionToken(),
                        payload(request, YeuCauTrangThaiNhanVien.class), true));
            case TAO_TAI_KHOAN:
                return PhanHoi.success(id, employees().createAccount(request.getSessionToken(),
                        payload(request, YeuCauTaoTaiKhoan.class)));
            case KHOA_TAI_KHOAN:
                return PhanHoi.success(id, employees().setAccountActive(request.getSessionToken(),
                        payload(request, YeuCauTrangThaiTaiKhoan.class), false));
            case MO_KHOA_TAI_KHOAN:
                return PhanHoi.success(id, employees().setAccountActive(request.getSessionToken(),
                        payload(request, YeuCauTrangThaiTaiKhoan.class), true));
            case CAP_NHAT_VAI_TRO_TAI_KHOAN:
                return PhanHoi.success(id, employees().changeRole(request.getSessionToken(),
                        payload(request, YeuCauVaiTroTaiKhoan.class)));
            case XEM_CHI_TIET_SAN_PHAM:
                return PhanHoi.success(id, products().detail(request.getSessionToken(),
                        payload(request, YeuCauChiTietSanPham.class).getMaSP()));
            case TRA_CUU_LOAI_SAN_PHAM:
                return PhanHoi.success(id, products().categories(request.getSessionToken()));
            case TRA_CUU_THUONG_HIEU:
                return PhanHoi.success(id, products().brands(request.getSessionToken()));
            default:
                return PhanHoi.failure(id, MaLoi.INVALID_REQUEST, "Chức năng này chưa được triển khai.");
            }
        } catch (NgoaiLeNghiepVu failure) {
            return PhanHoi.failure(id, failure.getCode(), failure.getMessage());
        } catch (IllegalArgumentException | com.fasterxml.jackson.core.JsonProcessingException invalid) {
            return PhanHoi.failure(id, MaLoi.INVALID_REQUEST, "Dữ liệu yêu cầu không đúng cấu trúc.");
        } catch (RuntimeException internal) {
            for (Throwable cause = internal; cause != null; cause = cause.getCause()) {
                if (cause instanceof javax.persistence.OptimisticLockException
                        || cause instanceof javax.persistence.PessimisticLockException
                        || cause instanceof javax.persistence.LockTimeoutException
                        || cause instanceof org.hibernate.StaleStateException) {
                    return PhanHoi.failure(id, MaLoi.VERSION_CONFLICT,
                            "Dữ liệu đang được thay đổi. Hãy tải lại trước khi thao tác.");
                }
                if (cause instanceof org.hibernate.exception.ConstraintViolationException) {
                    String constraint = ((org.hibernate.exception.ConstraintViolationException) cause)
                            .getConstraintName();
                    if ("UQ_LichSuKho_OperationId".equalsIgnoreCase(constraint)) {
                        return PhanHoi.failure(id, MaLoi.VERSION_CONFLICT,
                                "operationId đã được dùng bởi một thao tác kho khác.");
                    }
                    if ("PK_PhieuNhapThaoTac".equalsIgnoreCase(constraint)
                            || "UQ_PhieuNhap_OperationTao".equalsIgnoreCase(constraint)
                            || "UQ_PhieuNhap_OperationXacNhan".equalsIgnoreCase(constraint)) {
                        return PhanHoi.failure(id, MaLoi.VERSION_CONFLICT,
                                "operationId đã được dùng cho thao tác phiếu nhập khác.");
                    }
                    return PhanHoi.failure(id, MaLoi.VALIDATION_ERROR,
                            "Trùng mã/tên/màu-size hoặc dữ liệu vi phạm ràng buộc CSDL. Hãy tải lại để kiểm tra.");
                }
            }
            // Không log request, exception message có thể chứa dữ liệu DB/mật khẩu.
            LOG.error("YeuCau failed: {}", internal.getClass().getSimpleName());
            return PhanHoi.failure(id, MaLoi.SERVER_ERROR, "Không xử lý được yêu cầu. Vui lòng thử lại.");
        }
    }

    private static <T> T payload(YeuCau<JsonNode> request, Class<T> type)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode node = request.getPayload();
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("Missing payload");
        }
        return BoMaHoaJson.mapper().treeToValue(node, type);
    }

    private static YeuCauDieuChinhTonKho stockAdjustmentPayload(YeuCau<JsonNode> request)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode node = request.getPayload();
        if (node == null || !node.isObject() || !node.path("soLuongMoi").isIntegralNumber()
                || !node.path("expectedVersion").isIntegralNumber()) {
            throw new IllegalArgumentException("Stock quantity and version must be JSON integers.");
        }
        return BoMaHoaJson.mapper().treeToValue(node, YeuCauDieuChinhTonKho.class);
    }

    private static DuLieuBienThe variantStockPayload(YeuCau<JsonNode> request)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode node = request.getPayload();
        if (node == null || !node.isObject() || !node.path("soLuongTon").isIntegralNumber()) {
            throw new IllegalArgumentException("Variant stock must be a JSON integer.");
        }
        return BoMaHoaJson.mapper().treeToValue(node, DuLieuBienThe.class);
    }

    private static YeuCauDongHoaDon invoiceLinePayload(YeuCau<JsonNode> request)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode node = objectPayload(request);
        if (!integerLong(node, "phienBan") || !integerInt(node, "soLuong")) {
            throw new IllegalArgumentException("Invoice version and quantity must be JSON integers.");
        }
        return BoMaHoaJson.mapper().treeToValue(node, YeuCauDongHoaDon.class);
    }

    private static YeuCauGanKhachHangHoaDon invoiceCustomerPayload(YeuCau<JsonNode> request)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode node = objectPayload(request);
        if (!integerLong(node, "phienBan")) {
            throw new IllegalArgumentException("Invoice version must be a JSON integer.");
        }
        return BoMaHoaJson.mapper().treeToValue(node, YeuCauGanKhachHangHoaDon.class);
    }

    private static YeuCauDongPhieuNhap receiptLinePayload(YeuCau<JsonNode> request)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode node = objectPayload(request);
        boolean remove = request.getAction() == HanhDong.BO_DONG_PHIEU_NHAP;
        if (!integerLong(node, "phienBan") || (!remove && !integerInt(node, "soLuong"))) {
            throw new IllegalArgumentException("Receipt version and quantity must be JSON integers.");
        }
        JsonNode price = node.path("donGiaNhap");
        if (remove ? (!price.isMissingNode() && !price.isNull()) : !price.isNumber()) {
            throw new IllegalArgumentException("Purchase price must be a JSON number.");
        }
        return BoMaHoaJson.mapper().treeToValue(node, YeuCauDongPhieuNhap.class);
    }

    private static YeuCauXacNhanPhieuNhap receiptConfirmPayload(YeuCau<JsonNode> request)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode node = objectPayload(request);
        if (!integerLong(node, "phienBan")) {
            throw new IllegalArgumentException("Receipt version must be a JSON integer.");
        }
        return BoMaHoaJson.mapper().treeToValue(node, YeuCauXacNhanPhieuNhap.class);
    }

    private static YeuCauPhienBanPhieuNhap receiptVersionPayload(YeuCau<JsonNode> request)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode node = objectPayload(request);
        if (!integerLong(node, "phienBan")) {
            throw new IllegalArgumentException("Receipt version must be a JSON integer.");
        }
        return BoMaHoaJson.mapper().treeToValue(node, YeuCauPhienBanPhieuNhap.class);
    }

    private static JsonNode objectPayload(YeuCau<JsonNode> request) {
        JsonNode node = request == null ? null : request.getPayload();
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("Missing object payload");
        }
        return node;
    }

    private static boolean integerLong(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isIntegralNumber() && value.canConvertToLong();
    }

    private static boolean integerInt(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isIntegralNumber() && value.canConvertToInt();
    }

    private DichVuSanPham products() {
        if (sanPham == null) {
            throw new NgoaiLeNghiepVu(MaLoi.INVALID_REQUEST, "Dịch vụ sản phẩm chưa được cấu hình.");
        }
        return sanPham;
    }

    private DichVuHoaDon invoices() {
        if (hoaDon == null) {
            throw new NgoaiLeNghiepVu(MaLoi.INVALID_REQUEST, "Dịch vụ hóa đơn chưa được cấu hình.");
        }
        return hoaDon;
    }

    private DichVuNhanVien employees() {
        if (nhanVien == null) {
            throw new NgoaiLeNghiepVu(MaLoi.INVALID_REQUEST, "Dịch vụ nhân viên chưa được cấu hình.");
        }
        return nhanVien;
    }

    private DichVuKhachHang customers() {
        if (khachHang == null) {
            throw new NgoaiLeNghiepVu(MaLoi.INVALID_REQUEST, "Dịch vụ khách hàng chưa được cấu hình.");
        }
        return khachHang;
    }

    private DichVuKhuyenMai promotions() {
        if (khuyenMai == null) {
            throw new NgoaiLeNghiepVu(MaLoi.INVALID_REQUEST, "Dịch vụ khuyến mãi chưa được cấu hình.");
        }
        return khuyenMai;
    }

    private DichVuThongKe statistics() {
        if (thongKe == null) {
            throw new NgoaiLeNghiepVu(MaLoi.INVALID_REQUEST, "Dịch vụ thống kê chưa được cấu hình.");
        }
        return thongKe;
    }

    private DichVuLichSuKho stockHistory() {
        if (lichSuKho == null) {
            throw new NgoaiLeNghiepVu(MaLoi.INVALID_REQUEST, "Dịch vụ lịch sử kho chưa được cấu hình.");
        }
        return lichSuKho;
    }

    private DichVuNhaCungCap suppliers() {
        if (nhaCungCap == null) {
            throw new NgoaiLeNghiepVu(MaLoi.INVALID_REQUEST, "Dịch vụ nhà cung cấp chưa được cấu hình.");
        }
        return nhaCungCap;
    }

    private DichVuPhieuNhap receipts() {
        if (phieuNhap == null) {
            throw new NgoaiLeNghiepVu(MaLoi.INVALID_REQUEST, "Dịch vụ phiếu nhập chưa được cấu hình.");
        }
        return phieuNhap;
    }
}
