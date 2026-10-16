package server.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import common.dto.KhuyenMaiSearchRequest;
import server.entity.KhuyenMai;

/** Truy cập khuyến mãi; lịch sử hóa đơn tham chiếu được giữ nguyên. */
public interface KhuyenMaiDao extends GenericDao<KhuyenMai, String> {
    Optional<KhuyenMai> findForUpdate(String maKM);
    List<KhuyenMai> search(KhuyenMaiSearchRequest request, Boolean savedActive, boolean effectiveOnly, LocalDate today);
    long count(KhuyenMaiSearchRequest request, Boolean savedActive, boolean effectiveOnly, LocalDate today);
    boolean codeInUseIgnoreCase(String maKM);
}
