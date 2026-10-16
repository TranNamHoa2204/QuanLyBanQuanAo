package server.repository;

import java.util.List;
import java.util.Optional;
import common.dto.NhanVienSearchRequest;
import server.entity.NhanVien;

/** DAO truy cập nhân viên; không xóa cứng dữ liệu có thể được hóa đơn tham chiếu. */
public interface NhanVienDao extends GenericDao<NhanVien, String> {
    Optional<NhanVien> findForUpdate(String maNV);
    List<NhanVien> search(NhanVienSearchRequest request);
    long count(NhanVienSearchRequest request);
    boolean codeInUseIgnoreCase(String maNV);
    boolean phoneInUse(String sdt, String exceptMaNV);
}
