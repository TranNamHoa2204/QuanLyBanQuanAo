package server.repositories;

import java.util.List;
import java.util.Optional;

import common.dto.nhacungcap.YeuCauTraCuuNhaCungCap;
import server.entities.NhaCungCap;

public interface LuuTruNhaCungCap {
    Optional<NhaCungCap> findById(String id);

    Optional<NhaCungCap> findForUpdate(String id);

    boolean codeExists(String id);

    boolean phoneExists(String phone, String exceptId);

    List<NhaCungCap> search(YeuCauTraCuuNhaCungCap request);

    long count(YeuCauTraCuuNhaCungCap request);
}
