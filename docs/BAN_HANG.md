# Bán hàng và hóa đơn

## Chạy trong Eclipse

1. F5 để Refresh project, Maven → Update Project nếu cần.
2. Dừng server nếu đang chạy.
3. Chạy `sql/04_HoaDonSnapshotVaThoiDiemThanhToan.sql` sau baseline và migration
   ảnh. Chỉ áp dụng trên database demo `QuanLyCuaHangQuanAo`; script không backfill
   và sẽ dừng nếu cột đã có nhưng sai kiểu/độ dài/nullability.
4. Nạp dữ liệu demo cần thiết bằng `sql/02_NhapDuLieuQuanAo.sql`. Cần ít nhất một bản ghi `dbo.Thue` đang hoạt động. Nếu chỉ demo đồ án,
   bạn có thể chủ động chạy sql/03_ThueDemo_0PhanTram.sql trong SSMS,
   trên đúng database. Script không sửa bản ghi thuế đã có.
   0% ở đây chỉ là dữ liệu thử, không phải kết luận nghĩa vụ thuế thực tế.
5. Sau khi dữ liệu mẫu tồn kho đã nạp xong, chạy `sql/05_LichSuBienDongKho.sql`;
   lần đầu thiết lập mốc tồn mở đầu, không tái dựng giao dịch cũ.
6. Chạy `sql/06_ThuTienTaiQuay.sql` sau S15. Nó thêm các cột thu tiền nullable,
   không gán phương thức cho hóa đơn cũ. Cả migration 05 và 06 chỉ áp dụng trên
   database demo đã xác định.
7. Khởi động `server.ServerMain` với cấu hình QUANAO_DB_* của database demo.
   JPA giữ `hbm2ddl.auto=validate`, không tự tạo/sửa schema hay nhập dữ liệu.
8. Chạy `client.ClientMain`, đăng nhập, chọn Bán hàng / Hóa đơn.
9. Chọn thuế. Có thể để mã KH/KM trống để bán cho khách vãng lai/không khuyến mãi.
   Form hiện chỉ nhận mã KH/KM gõ trực tiếp; chưa có tìm kiếm, chọn hoặc tạo hồ sơ từ
   màn hình bán hàng. Nếu nhập mã, khách phải đang hoạt động và khuyến mãi phải còn hạn.
10. Tạo nháp. Nhập mã SP lấy từ Tra cứu sản phẩm (ví dụ SP1001),
   bấm Tải biến thể, chọn màu/size, số lượng và Thêm hàng.
11. Chọn dòng trong bảng để Đổi SL (số lượng mới tuyệt đối) hoặc Bỏ hàng.
12. Kiểm tra tổng rồi chọn phương thức thu. Tiền mặt yêu cầu nhập số tiền khách
   đưa; server xác nhận không thấp hơn tổng và tự tính tiền thừa. Chuyển khoản là
   xác nhận thủ công của nhân viên rằng đã nhận thông báo tiền; ứng dụng không
   kết nối hoặc xác minh với ngân hàng. Chỉ sau khi server lưu thành công, trạng thái
   chuyển DA_THANH_TOAN, tồn giảm và thông tin thu tiền được lưu cùng ledger.
13. Danh sách cho phép xem các trang 20 hóa đơn và mở lại chi tiết.
    Có thể nhập trực tiếp mã hóa đơn và Tải lại.

## Quy tắc đã cài đặt

- NHAP: thêm, đổi số lượng, bỏ mặt hàng, thanh toán, hủy nháp.
- DA_THANH_TOAN hoặc DA_HUY: chỉ xem. Chưa có trả hàng/hoàn tiền.
- Nhân viên bán hàng chỉ xem/thao tác hóa đơn do mình lập.
  Nhân viên quản lý xem/thao tác mọi hóa đơn trong giới hạn trạng thái trên.
- Giá lấy từ CSDL khi thêm dòng và giữ nguyên nếu giá sản phẩm đổi sau đó.
  Client không quyết định giá hay tổng tiền; khi xác nhận thu tiền, client gửi lại
  tổng đã hiển thị để server tính lại và đối chiếu. Mỗi biến thể chỉ có một dòng/hóa đơn.
- Mỗi hóa đơn tối đa 200 dòng; số lượng mỗi dòng 1–1.000.000 và không vượt
  tồn lúc sửa. Hóa đơn nháp không giữ chỗ hay trừ tồn.
- Tạm tính = tổng số lượng × đơn giá. Giảm = tạm tính × tỷ lệ giảm.
  Thuế = (tạm tính − giảm) × tỷ lệ thuế. Tổng = tạm tính − giảm + thuế.
  Sử dụng BigDecimal, làm tròn từng khoản đến 2 chữ số HALF_UP.
  Đây là quy tắc tính tiền của đồ án; chưa có hóa đơn điện tử.
- Tỷ lệ được chốt trong nháp. Nếu thuế/khuyến mãi đổi tỷ lệ, ngừng hoạt động
  hoặc khuyến mãi hết hạn, server không cho thanh toán theo cấu hình cũ.
  **Giới hạn hiện tại:** `HoaDonService.pay` yêu cầu hủy nháp và tạo hóa đơn mới khi tỷ lệ
  đã đổi; chưa có luồng cập nhật nháp, preview và xác nhận tổng mới mà không hủy nháp theo
  yêu cầu S10.
- Ghi hóa đơn và trừ tồn trong cùng một transaction JPA.
  Server khóa hóa đơn, các sản phẩm rồi từng biến thể theo thứ tự mã cố định; kiểm tra tất cả
  tồn trước khi trừ. Lỗi ở một mặt hàng rollback cả giao dịch.
- phienBan + @Version kiểm tra màn hình cũ; nếu VERSION_CONFLICT, Tải lại
  trước khi chỉnh tiếp. Nếu tổng đã xem cũ, server từ chối trước khi đổi trạng thái
  hoặc tồn; tải lại, rà tổng mới, nhập/kiểm tra lại tiền đưa rồi xác nhận lần nữa.
  Phiên bản tăng nhưng không cần tăng đúng một đơn vị.
- Thanh toán lại mã hóa đơn đã thanh toán trả lại kết quả đã lưu,
  bao gồm phương thức, tiền khách đưa, tiền thừa hoặc xác nhận chuyển khoản;
  không thay thông tin cũ và không trừ tồn lần hai. Hủy lại hóa đơn đã hủy cũng không làm thay đổi tồn.
- Mã hóa đơn tạo trên client được giữ lại để truy hồi khi mất phản hồi tạo nháp.
  Không tự gửi lại yêu cầu mạng. Khi mất phản hồi, dùng mã đã hiển thị để Tải lại
  và kiểm tra trạng thái trước khi thao tác tiếp. Không tạo mã mới để thử thanh toán.
- Bấm Hóa đơn mới không xóa nháp cũ. Nháp lưu trong database, mở lại từ Danh sách.
- Khi thanh toán, server lưu `ngayThanhToan` theo giờ địa phương `Asia/Ho_Chi_Minh`
  và chụp tên nhân viên/khách cùng tên sản phẩm/màu/size trong cùng transaction
  với việc trừ kho. Đổi danh mục sau đó không đổi thông tin chứng từ đã thanh toán.
- Nháp và hóa đơn đã hủy vẫn hiện tên hiện hành, được đánh dấu nguồn
  `DU_LIEU_HIEN_TAI`; hóa đơn thanh toán cũ thiếu dữ liệu mới giữ ngày NULL và được
  đánh dấu `FALLBACK_DU_LIEU_HIEN_TAI`. Không suy đoán thời điểm hoặc tên lịch sử.
- Snapshot chứng từ chỉ gồm tên hiển thị cần thiết; không sao chép số điện thoại,
  email hay địa chỉ khách. Đơn giá, tỷ lệ giảm/thuế và các khoản tiền vẫn lấy từ dữ liệu
  đã lưu trên hóa đơn/dòng hóa đơn.
- Chuyển khoản chỉ ghi nhận xác nhận thủ công của nhân viên, không xác minh
  giao dịch ngân hàng. Hóa đơn cũ có các trường thu tiền NULL sẽ hiển thị
  “chưa có dữ liệu”; không suy đoán phương thức hoặc tiền thừa.
- Đây là chứng từ bán hàng nội bộ của đồ án, không phải hóa đơn điện tử pháp lý.
  In/PDF hóa đơn và CSV thống kê thuộc FR13 nhưng chưa được triển khai.

## Kiểm thử và giới hạn xác minh

InvoiceIntegrationTest kiểm tra qua TCP Socket thật, JPA và H2 trong bộ nhớ:
tính tiền, phiên bản cũ, thêm/đổi/bỏ dòng, quyền sở hữu, phân trang cơ bản,
giá chốt, cấu hình thuế đổi, hủy nháp, vượt miền tiền, hàng ngừng bán,
khuyến mãi hết hạn, rollback khi thiếu tồn, thanh toán lặp lại và hai client
cùng mua một đơn vị tồn cuối. S18 kiểm tra tiền mặt thiếu/đủ/thừa, scale/overflow,
phương thức chuyển khoản cần xác nhận nhân viên, tổng xem cũ, retry không đổi dữ liệu
thu, rollback tiền/kho/ledger và hóa đơn cũ chưa có thông tin thu.
Kiểm tra S11 còn xác nhận snapshot tên trước/sau khi danh mục đổi, retry thanh toán,
fallback hóa đơn cũ thiếu timestamp/snapshot và chuyển timestamp UTC sang giờ nghiệp vụ.

InvoiceUiTest render màn hình Swing headless và kiểm tra bảng chỉ đọc,
vô hiệu hóa sửa/thanh toán khi đã thanh toán.

Chưa kiểm thử trực tiếp SQL Server demo; các script `sql/04_HoaDonSnapshotVaThoiDiemThanhToan.sql`,
`sql/05_LichSuBienDongKho.sql` và `sql/06_ThuTienTaiQuay.sql` chưa được chạy trên instance demo.
Cột thanhTien trong SQL Server là computed persisted, JPA không ghi vào cột này.
H2 fixture không mô phỏng computed column đó; DTO tính lại từ đơn giá chốt và số lượng.
Nên thử chuỗi migration 04 → nạp dữ liệu demo → 05 → 06 và quy trình checkout
trên CSDL demo SQL Server trước khi dùng dữ liệu thật.

Quản lý danh mục/sản phẩm và điều chỉnh tồn có ledger đã có; xem
`QUAN_LY_DANH_MUC_SAN_PHAM.md` và `LICH_SU_KHO.md`. Backend thống kê có nhưng API/controller/
form Swing còn rỗng; xem `THONG_KE_BACKEND.md`. Khách hàng, nhân viên/tài khoản và khuyến mãi
có backend nhưng thiếu UI hoàn chỉnh. FR11 phiếu nhập, FR13 in/xuất, FR14 cảnh báo tồn/
dashboard và FR15 audit/reset chưa được triển khai. S27 ghi ma trận trạng thái đầy đủ.
