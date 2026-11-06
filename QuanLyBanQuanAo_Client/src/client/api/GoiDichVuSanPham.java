package client.api;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;

import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.danhmuc.DuLieuLoaiSanPham;
import common.dto.danhmuc.DuLieuThuongHieu;
import common.dto.danhmuc.YeuCauLuuDanhMuc;
import common.dto.sanpham.DuLieuBienThe;
import common.dto.sanpham.DuLieuDayDuSanPham;
import common.dto.sanpham.KetQuaTraCuuSanPham;
import common.dto.sanpham.YeuCauChiTietSanPham;
import common.dto.sanpham.YeuCauLuuSanPham;
import common.dto.sanpham.YeuCauTraCuuSanPham;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;

public final class GoiDichVuSanPham {
    private final KetNoiMayChu client;
    private final PhienDangNhap session;

    public GoiDichVuSanPham(KetNoiMayChu client, PhienDangNhap session) {
        this.client = client;
        this.session = session;
    }

    public String saveProduct(YeuCauLuuSanPham r, int mode) throws IOException {
        return call(
                mode == 0 ? HanhDong.THEM_SAN_PHAM
                        : mode == 1 ? HanhDong.CAP_NHAT_SAN_PHAM : HanhDong.NGUNG_KINH_DOANH_SAN_PHAM,
                r, BoMaHoaJson.mapper().constructType(String.class));
    }

    public String saveCategory(YeuCauLuuDanhMuc r, boolean create) throws IOException {
        return call(create ? HanhDong.THEM_LOAI_SAN_PHAM : HanhDong.CAP_NHAT_LOAI_SAN_PHAM, r,
                BoMaHoaJson.mapper().constructType(String.class));
    }

    public String saveBrand(YeuCauLuuDanhMuc r, boolean create) throws IOException {
        return call(create ? HanhDong.THEM_THUONG_HIEU : HanhDong.CAP_NHAT_THUONG_HIEU, r,
                BoMaHoaJson.mapper().constructType(String.class));
    }

    public DuLieuBienThe saveVariant(DuLieuBienThe r, boolean create) throws IOException {
        return call(create ? HanhDong.THEM_BIEN_THE : HanhDong.CAP_NHAT_BIEN_THE, r,
                BoMaHoaJson.mapper().constructType(DuLieuBienThe.class));
    }

    public KetQuaTraCuuSanPham search(YeuCauTraCuuSanPham request) throws IOException {
        return call(HanhDong.TRA_CUU_SAN_PHAM, request,
                BoMaHoaJson.mapper().constructType(KetQuaTraCuuSanPham.class));
    }

    public DuLieuDayDuSanPham detail(String maSP) throws IOException {
        YeuCauChiTietSanPham request = new YeuCauChiTietSanPham();
        request.setMaSP(maSP);
        return call(HanhDong.XEM_CHI_TIET_SAN_PHAM, request,
                BoMaHoaJson.mapper().constructType(DuLieuDayDuSanPham.class));
    }

    public List<DuLieuLoaiSanPham> categories() throws IOException {
        return call(HanhDong.TRA_CUU_LOAI_SAN_PHAM, null, BoMaHoaJson.mapper().getTypeFactory()
                .constructCollectionType(List.class, DuLieuLoaiSanPham.class));
    }

    public List<DuLieuThuongHieu> brands() throws IOException {
        return call(HanhDong.TRA_CUU_THUONG_HIEU, null, BoMaHoaJson.mapper().getTypeFactory()
                .constructCollectionType(List.class, DuLieuThuongHieu.class));
    }

    private <T> T call(HanhDong action, Object payload, JavaType type) throws IOException {
        PhanHoi<JsonNode> response = client.send(action, session.getToken(), payload);
        if (!response.isSuccess()) {
            if (response.getErrorCode() == MaLoi.UNAUTHENTICATED) {
                session.clear();
            }
            throw new NgoaiLeGoiDichVu(response.getErrorCode(), response.getMessage());
        }
        return BoMaHoaJson.mapper().convertValue(response.getPayload(), type);
    }
}
