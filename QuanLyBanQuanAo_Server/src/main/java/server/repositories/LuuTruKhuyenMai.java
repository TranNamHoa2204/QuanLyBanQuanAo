package server.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import common.dto.khuyenmai.YeuCauTraCuuKhuyenMai;
import server.entities.KhuyenMai;

/** Truy cập khuyến mãi; lịch sử hóa đơn tham chiếu được giữ nguyên. */
public interface LuuTruKhuyenMai extends LuuTruCoBan<KhuyenMai, String> {
    Optional<KhuyenMai> findForUpdate(String maKM);

    Optional<KhuyenMai> bestEffective(LocalDate today);

    List<KhuyenMai> search(YeuCauTraCuuKhuyenMai request, Boolean savedActive, boolean effectiveOnly,
            LocalDate today);

    long count(YeuCauTraCuuKhuyenMai request, Boolean savedActive, boolean effectiveOnly, LocalDate today);

    boolean codeInUseIgnoreCase(String maKM);
}
