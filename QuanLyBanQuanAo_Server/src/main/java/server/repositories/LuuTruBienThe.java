package server.repositories;

import server.entities.BienThe;

/**
 * Hợp đồng truy cập dữ liệu; triển khai trong repositories.impl. Không xóa cứng dữ liệu đã có lịch sử giao
 * dịch.
 */
public interface LuuTruBienThe extends LuuTruCoBan<BienThe, String> {
}
