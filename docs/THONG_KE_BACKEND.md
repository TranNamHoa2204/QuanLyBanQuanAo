# Thống kê backend (S13)

## Hợp đồng

Server nhận hai action `THONG_KE_DOANH_THU_THEO_THOI_GIAN` và
`THONG_KE_DOANH_THU_THEO_SAN_PHAM` với `ThongKeDoanhThuRequest`, trả
`ThongKeDoanhThuDTO`. Cả hai action yêu cầu phiên hợp lệ của quản lý; quyền được
kiểm tra tại server trong transaction. Trang đánh số từ 0, mặc định 50 dòng, tối đa
100 dòng/trang.

Ngày bắt đầu và kết thúc đều bao gồm trọn ngày theo giờ địa phương Việt Nam đã dùng
khi ghi `HoaDon.ngayThanhToan`. Truy vấn tương ứng khoảng nửa mở từ đầu ngày bắt đầu
đến đầu ngày kế tiếp sau ngày kết thúc. Báo cáo nhóm thời gian hỗ trợ ngày (tối đa
366 ngày), tháng (tối đa 120 tháng) và năm (tối đa 20 năm). Báo cáo sản phẩm tối đa
366 ngày. Mỗi truy vấn dừng ở 100.001 dòng nguồn và từ chối kết quả vượt 100.000 dòng;
người dùng phải thu hẹp khoảng thời gian.

## Định nghĩa tiền

- `hangBanTruocGiam`: tổng `HoaDon.tamTinh` đã lưu của hóa đơn đã thanh toán.
- `tienGiam`: tổng `HoaDon.tienGiam` đã lưu.
- `tienThue`: tổng `HoaDon.tienThue` đã lưu.
- `doanhThuSauGiam`: hàng bán trước giảm trừ tiền giảm; chưa cộng thuế.
- `tongKhachTra`: tổng tiền khách trả, tương ứng `HoaDon.tongTien` đã lưu.

Các cột tổng hóa đơn được truy vấn riêng, không join với dòng hàng nên không bị cộng
lặp. Server xác minh `tạm tính - giảm + thuế = tổng khách trả` trước khi đưa số liệu
vào báo cáo. Đây là doanh thu và tiền thu theo chứng từ, không phải lợi nhuận.

Ở báo cáo sản phẩm, giá dòng là `ChiTietHoaDon.donGia` đã chốt nhân số lượng. Tiền
giảm được phân bổ theo tỷ trọng giá trị hàng của từng sản phẩm trong từng hóa đơn;
thuế phân bổ theo tỷ trọng giá trị sau giảm. Phép chia làm việc bằng số xu nguyên:
phần xu lẻ phân theo phần dư lớn nhất, hòa thì mã sản phẩm tăng dần. Vì vậy tổng các
sản phẩm khớp từng hóa đơn đến xu. Không đọc giá hiện hành, thuế hiện hành hay tỷ lệ
khuyến mãi hiện hành để tính lại lịch sử. Tên sản phẩm dùng snapshot của lần bán gần
nhất trong khoảng; nếu snapshot của lần bán đó thiếu, dùng tên hiện hành và đặt
`tenSPHienHanhFallback=true`.

## Hóa đơn cũ thiếu thời điểm thanh toán

Hóa đơn `DA_THANH_TOAN` có `ngayThanhToan IS NULL` bị loại khỏi mọi khoảng ngày; không
dùng `ngayLap` để phỏng đoán thời điểm bán/thu tiền. Mỗi phản hồi trả
`soHoaDonThieuThoiDiemThanhToan` và `canhBaoDuLieuCu`; số đếm này là số hóa đơn thiếu
timestamp trên toàn hệ thống, không phải số trong khoảng lọc. Snapshot cũ thiếu tên
sản phẩm chỉ ảnh hưởng nhãn hiển thị; không thay đổi số tiền.

## Kiểm thử và giới hạn xác minh

`ThongKeIntegrationTest` chạy JPA/H2 và Socket TCP với fixture tính tay: nhiều dòng và
biến thể cùng sản phẩm, giảm/thuế, nháp/hủy, biên ngày/tháng/năm, cập nhật danh mục,
phân bổ phần xu, dữ liệu rỗng, trang, giới hạn khoảng, fallback tên và quyền quản lý.
SQL Server chưa được kiểm thử trực tiếp; không chạy SQL hoặc kết nối database thật.
`ThongKeApi`, `ThongKeController` và `ThongKeForm` hiện vẫn là lớp TODO/rỗng; sidebar
hiển thị “Sắp có”. Vì vậy FR09 có backend/test nhưng chưa phải chức năng Swing đầu cuối.
Trong lần S27, bộ test hiện tại chạy 88 JUnit test qua script fallback; test thống kê
vẫn dùng H2/Socket và không xác minh schema hoặc hiệu năng SQL Server.
