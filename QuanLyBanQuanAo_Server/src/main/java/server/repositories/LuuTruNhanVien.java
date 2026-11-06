package server.repositories;

import java.util.List;
import java.util.Optional;

import common.dto.nhanvien.YeuCauTraCuuNhanVien;
import server.entities.NhanVien;

/** DAO truy cập nhân viên; không xóa cứng dữ liệu có thể được hóa đơn tham chiếu. */
public interface LuuTruNhanVien extends LuuTruCoBan<NhanVien, String> {
    Optional<NhanVien> findForUpdate(String maNV);

    List<NhanVien> search(YeuCauTraCuuNhanVien request);

    long count(YeuCauTraCuuNhanVien request);

    boolean codeInUseIgnoreCase(String maNV);

    boolean phoneInUse(String sdt, String exceptMaNV);
}
