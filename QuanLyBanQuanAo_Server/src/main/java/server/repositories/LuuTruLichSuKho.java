package server.repositories;

import java.util.List;
import java.util.Optional;

import common.dto.kho.YeuCauTraCuuLichSuKho;
import server.entities.LichSuKho;

public interface LuuTruLichSuKho {
    Optional<LichSuKho> findByOperationId(String operationId);

    List<LichSuKho> search(YeuCauTraCuuLichSuKho request);

    long count(YeuCauTraCuuLichSuKho request);
}
