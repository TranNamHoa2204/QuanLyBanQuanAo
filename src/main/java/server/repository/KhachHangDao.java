package server.repository;

import java.util.List;
import java.util.Optional;
import common.dto.KhachHangSearchRequest;
import server.entity.KhachHang;

/** Truy cập khách hàng; không xóa cứng bản ghi có thể được hóa đơn tham chiếu. */
public interface KhachHangDao extends GenericDao<KhachHang, String> {
    Optional<KhachHang> findForUpdate(String maKH);
    List<KhachHang> search(KhachHangSearchRequest request);
    long count(KhachHangSearchRequest request);
    boolean codeInUseIgnoreCase(String maKH);
    boolean phoneInUse(String sdt, String exceptMaKH);
}
