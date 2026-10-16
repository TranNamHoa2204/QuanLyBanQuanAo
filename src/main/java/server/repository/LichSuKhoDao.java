package server.repository;

import java.util.List;
import java.util.Optional;
import common.dto.LichSuKhoSearchRequest;
import server.entity.LichSuKho;

public interface LichSuKhoDao {
    Optional<LichSuKho> findByOperationId(String operationId);
    List<LichSuKho> search(LichSuKhoSearchRequest request);
    long count(LichSuKhoSearchRequest request);
}
