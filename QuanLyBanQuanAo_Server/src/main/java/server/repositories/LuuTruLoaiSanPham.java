package server.repositories;

import server.entities.LoaiSanPham;

/**
 * Hợp đồng truy cập dữ liệu; triển khai trong repositories.impl. Không xóa cứng dữ liệu đã có lịch sử giao
 * dịch.
 */
public interface LuuTruLoaiSanPham extends LuuTruCoBan<LoaiSanPham, String> {
}
