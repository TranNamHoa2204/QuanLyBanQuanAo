# Bán hàng và hóa đơn

## Chạy trong Eclipse

1. F5 để Refresh project, Maven → Update Project nếu cần.
2. Dừng rồi chạy lại server.ServerMain với cấu hình QUANAO_DB_* hiện tại.
3. CSDL cần đầy đủ bảng của script gốc và bảng AnhSanPham của bước trước.
   JPA chỉ validate schema, không tự tạo/sửa bảng hay nhập dữ liệu.
4. Cần ít nhất một bản ghi dbo.Thue đang hoạt động. Nếu chỉ demo đồ án,
   bạn có thể chủ động chạy sql/03_ThueDemo_0PhanTram.sql trong SSMS,
   trên đúng database. Script không sửa bản ghi thuế đã có.
   0% ở đây chỉ là dữ liệu thử, không phải kết luận nghĩa vụ thuế thực tế.
5. Chạy client.ClientMain, đăng nhập, chọn Bán hàng / Hóa đơn.
6. Chọn thuế, để mã KH/KM trống khi bán cho khách vãng lai, không khuyến mãi.
   Nếu điền mã thì bản ghi phải tồn tại và đang hoạt động; khuyến mãi phải còn hạn.
7. Tạo nháp. Nhập mã SP lấy từ Tra cứu sản phẩm (ví dụ SP1001),
   bấm Tải biến thể, chọn màu/size, số lượng và Thêm hàng.
8. Chọn dòng trong bảng để Đổi SL (số lượng mới tuyệt đối) hoặc Bỏ hàng.
9. Kiểm tra tổng tiền rồi Thanh toán. Trạng thái chuyển DA_THANH_TOAN,
   tồn giảm và không thể sửa/hủy hóa đơn đó nữa.
10. Danh sách cho phép xem các trang 20 hóa đơn và mở lại chi tiết.
    Có thể nhập trực tiếp mã hóa đơn và Tải lại.

## Quy tắc đã cài đặt

- NHAP: thêm, đổi số lượng, bỏ mặt hàng, thanh toán, hủy nháp.
- DA_THANH_TOAN hoặc DA_HUY: chỉ xem. Chưa có trả hàng/hoàn tiền.
- Nhân viên bán hàng chỉ xem/thao tác hóa đơn do mình lập.
  Nhân viên quản lý xem/thao tác mọi hóa đơn trong giới hạn trạng thái trên.
- Giá lấy từ CSDL khi thêm dòng và giữ nguyên nếu giá sản phẩm đổi sau đó.
  Client không gửi giá hay tổng tiền. Mỗi biến thể chỉ có một dòng/hóa đơn.
- Mỗi hóa đơn tối đa 200 dòng; số lượng mỗi dòng 1–1.000.000 và không vượt
  tồn lúc sửa. Hóa đơn nháp không giữ chỗ hay trừ tồn.
- Tạm tính = tổng số lượng × đơn giá. Giảm = tạm tính × tỷ lệ giảm.
  Thuế = (tạm tính − giảm) × tỷ lệ thuế. Tổng = tạm tính − giảm + thuế.
  Sử dụng BigDecimal, làm tròn từng khoản đến 2 chữ số HALF_UP.
  Đây là quy tắc tính tiền của đồ án; chưa có hóa đơn điện tử.
- Tỷ lệ được chốt trong nháp. Nếu thuế/khuyến mãi đổi tỷ lệ, ngừng hoạt động
  hoặc khuyến mãi hết hạn, không thanh toán nháp với cấu hình cũ:
  hủy nháp và tạo hóa đơn mới sau khi kiểm tra thông tin.
- Ghi hóa đơn và trừ tồn trong cùng một transaction JPA.
  Server khóa hóa đơn, các sản phẩm rồi từng biến thể theo thứ tự mã cố định; kiểm tra tất cả
  tồn trước khi trừ. Lỗi ở một mặt hàng rollback cả giao dịch.
- phienBan + @Version kiểm tra màn hình cũ; nếu VERSION_CONFLICT, Tải lại
  trước khi chỉnh tiếp. Phiên bản tăng nhưng không cần tăng đúng một đơn vị.
- Thanh toán lại mã hóa đơn đã thanh toán trả lại kết quả đã lưu,
  không trừ tồn lần hai. Hủy lại hóa đơn đã hủy cũng không làm thay đổi tồn.
- Mã hóa đơn tạo trên client được giữ lại để truy hồi khi mất phản hồi tạo nháp.
  Không tự gửi lại yêu cầu mạng. Khi mất phản hồi, dùng mã đã hiển thị để Tải lại
  và kiểm tra trạng thái trước khi thao tác tiếp. Không tạo mã mới để thử thanh toán.
- Bấm Hóa đơn mới không xóa nháp cũ. Nháp lưu trong database, mở lại từ Danh sách.
- Thanh toán trong đồ án là ghi nhận giao dịch bán hàng, chưa kết nối ngân hàng,
  cổng thanh toán, máy in, in PDF hay quản lý tiền khách đưa/tiền thừa.

## Kiểm thử và giới hạn xác minh

InvoiceIntegrationTest kiểm tra qua TCP Socket thật, JPA và H2 trong bộ nhớ:
tính tiền, phiên bản cũ, thêm/đổi/bỏ dòng, quyền sở hữu, phân trang cơ bản,
giá chốt, cấu hình thuế đổi, hủy nháp, vượt miền tiền, hàng ngừng bán,
khuyến mãi hết hạn, rollback khi thiếu tồn, thanh toán lặp lại và hai client
cùng mua một đơn vị tồn cuối.

InvoiceUiTest render màn hình Swing headless và kiểm tra bảng chỉ đọc,
vô hiệu hóa sửa/thanh toán khi đã thanh toán.

Chưa kiểm thử trực tiếp SQL Server của bạn; không có script nào được chạy tự động.
Cột thanhTien trong SQL Server là computed persisted, JPA không ghi vào cột này.
H2 fixture không mô phỏng computed column đó; DTO tính lại từ đơn giá chốt và số lượng.
Nên thử quy trình trên CSDL demo SQL Server trước khi dùng dữ liệu thật.

Quản lý danh mục/sản phẩm và điều chỉnh tồn cơ bản đã có; xem QUAN_LY_DANH_MUC_SAN_PHAM.md.
Quản lý nhân viên/khách hàng/khuyến mãi, phiếu nhập hàng, thống kê và in hóa đơn
vẫn là các bước tiếp theo.
