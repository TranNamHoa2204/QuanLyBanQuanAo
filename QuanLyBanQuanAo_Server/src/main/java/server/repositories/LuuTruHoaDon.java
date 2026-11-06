package server.repositories;

import server.entities.HoaDon;

/**
 * Hợp đồng truy cập dữ liệu; triển khai trong repositories.impl. Không xóa cứng dữ liệu đã có lịch sử giao
 * dịch.
 */
public interface LuuTruHoaDon extends LuuTruCoBan<HoaDon, String> {
}
