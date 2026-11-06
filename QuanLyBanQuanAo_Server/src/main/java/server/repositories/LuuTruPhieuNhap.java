package server.repositories;

import java.util.List;
import java.util.Optional;

import common.dto.phieunhap.YeuCauTraCuuPhieuNhap;
import server.entities.ChiTietPhieuNhap;
import server.entities.PhieuNhap;

public interface LuuTruPhieuNhap {
    Optional<PhieuNhap> findById(String id);

    Optional<PhieuNhap> findForUpdate(String id);

    Optional<PhieuNhap> findByCreateOperation(String operationId);

    Optional<PhieuNhap> findByConfirmOperation(String operationId);

    List<ChiTietPhieuNhap> lines(String id);

    List<PhieuNhap> search(YeuCauTraCuuPhieuNhap request);

    long count(YeuCauTraCuuPhieuNhap request);
}
