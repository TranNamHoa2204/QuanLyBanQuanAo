package server.repositories;

import server.entities.TaiKhoan;

/**
 * Hợp đồng truy cập dữ liệu; triển khai trong repositories.impl. Không xóa cứng dữ liệu đã có lịch sử giao
 * dịch.
 */
public interface LuuTruTaiKhoan extends LuuTruCoBan<TaiKhoan, String> {
}
