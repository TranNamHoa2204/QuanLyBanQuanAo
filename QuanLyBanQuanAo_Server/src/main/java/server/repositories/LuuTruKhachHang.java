package server.repositories;

import java.util.List;
import java.util.Optional;

import common.dto.khachhang.YeuCauTraCuuKhachHang;
import server.entities.KhachHang;

/** Truy cập khách hàng; không xóa cứng bản ghi có thể được hóa đơn tham chiếu. */
public interface LuuTruKhachHang extends LuuTruCoBan<KhachHang, String> {
    Optional<KhachHang> findForUpdate(String maKH);

    List<KhachHang> search(YeuCauTraCuuKhachHang request);

    long count(YeuCauTraCuuKhachHang request);

    boolean codeInUseIgnoreCase(String maKH);

    boolean phoneInUse(String sdt, String exceptMaKH);
}
