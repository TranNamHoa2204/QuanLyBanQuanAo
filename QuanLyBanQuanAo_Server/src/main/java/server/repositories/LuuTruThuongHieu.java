package server.repositories;

import server.entities.ThuongHieu;

/**
 * Hợp đồng truy cập dữ liệu; triển khai trong repositories.impl. Không xóa cứng dữ liệu đã có lịch sử giao
 * dịch.
 */
public interface LuuTruThuongHieu extends LuuTruCoBan<ThuongHieu, String> {
}
