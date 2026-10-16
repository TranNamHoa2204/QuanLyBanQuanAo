# Quản lý danh mục và sản phẩm

## Chạy và thử trên Eclipse

1. F5 để Refresh project. Maven → Update Project nếu cần.
2. Dừng và chạy lại cả server.ServerMain và client.ClientMain.
   Cấu hình QUANAO_DB_* giữ nguyên. Không tạo lại database, không chạy lại script nhập dữ liệu.
3. Đăng nhập bằng tài khoản NHAN_VIEN_QUAN_LY.
4. Chọn Danh mục & sản phẩm ở sidebar. Nhân viên bán hàng không có mục này.
5. Tab Loại sản phẩm / Thương hiệu: Tải lại, Thêm mới hoặc chọn dòng rồi Sửa tên.
   Mã cố định sau khi tạo. Tên không trùng; không có nút xóa các danh mục này.
6. Tab Sản phẩm: Thêm sản phẩm, hoặc chọn dòng rồi Sửa sản phẩm / Ngừng bán.
   Chọn loại, thương hiệu và nhập giá bằng số không có dấu phân cách hàng nghìn,
   ví dụ 149000 hoặc 149000.50. Tên và giá được kiểm tra lại tại server.
7. Chọn sản phẩm → Biến thể / tồn, hoặc vào tab Biến thể & tồn và nhập mã sản phẩm.
   Thêm mã biến thể, màu, size, tồn ban đầu. Tồn 0 là hợp lệ.
8. Chọn biến thể → Sửa tồn / trạng thái. Tồn mới là số lượng tuyệt đối,
   không phải số lượng cộng thêm. Khi thay đổi tồn có hộp xác nhận.
9. Để kinh doanh lại: tìm sản phẩm với bộ lọc Gồm hàng/biến thể ngừng bán,
   chọn Sửa sản phẩm, bật Đang kinh doanh. Trạng thái các biến thể giữ nguyên,
   cần bật lại biến thể riêng nếu trước đó đã ngừng bán.
10. Khi sửa danh mục, quay lại Sản phẩm → Tải lại để cập nhật tên và bộ lọc.

Nên thử trên CSDL demo hoặc sao lưu dữ liệu trước khi thêm/sửa.
Các test tự động chỉ dùng H2 trong bộ nhớ; chưa chạy hoặc ghi lên SQL Server của bạn.

## Quy tắc dữ liệu và phân quyền

- Mọi thao tác ghi kiểm tra token, tài khoản/nhân viên hoạt động và vai trò quản lý
  ở server. Ẩn menu ở client không phải biện pháp phân quyền duy nhất.
- Ngừng bán sản phẩm chỉ đặt trangThai=false, không xóa sản phẩm, biến thể, ảnh
  hay chi tiết hóa đơn. Ngừng bán biến thể được thực hiện qua Sửa tồn / trạng thái.
- Loại và thương hiệu hiện không có cột trangThai: chỉ thêm/sửa, không giả lập xóa mềm.
- Mã mới tối đa 20 ký tự, dùng A-Z/a-z/0-9/gạch ngang/gạch dưới.
  Tên loại/thương hiệu tối đa 100; tên sản phẩm tối đa 200; màu tối đa 50; size tối đa 20.
- Giá không âm, thuộc DECIMAL(18,2), tối đa 2 chữ số thập phân.
  Không tự làm tròn giá đầu vào sai độ chính xác.
- Loại/thương hiệu phải tồn tại. Một sản phẩm tối đa 500 biến thể;
  không thêm lại cùng màu + size, kể cả biến thể đã ngừng bán.
- Mã, sản phẩm, màu và size của biến thể cố định sau khi tạo.
  Muốn đổi nhận diện, thêm biến thể mới và ngừng bán biến thể cũ.
- Điều chỉnh tồn kiểm tra phienBan và khóa dòng biến thể trong transaction.
  Nếu có client vừa bán hoặc điều chỉnh tồn, yêu cầu cũ bị VERSION_CONFLICT.
  Tải lại trước khi lưu; không gửi lại dữ liệu cũ để ghi đè.
- Sản phẩm/loại/thương hiệu chưa có cột phiên bản trong schema hiện tại.
  Khi sửa, server khóa dòng và so sánh các trường gốc mà client đã đọc.
  Đây là kiểm tra ảnh chụp giá trị, không phải bộ đếm phiên bản: không phát hiện
  được trường hợp giá trị đổi rồi trở lại y hệt ban đầu.
- Tên danh mục và tên sản phẩm trên chi tiết hóa đơn có thể phản ánh tên hiện tại.
  Đơn giá, tỷ lệ và tiền đã chốt của hóa đơn không bị sửa khi đổi giá sản phẩm.
- Thanh toán khóa hóa đơn → các sản phẩm theo mã → các biến thể theo mã,
  rồi đọc lại tồn mới trước khi trừ. Thêm biến thể cũng khóa sản phẩm trước.
  Cách này ưu tiên tính nhất quán cho đồ án, có thể làm các giao dịch cùng sản phẩm
  chờ nhau dù mua các size khác nhau.
- Mất phản hồi lúc lưu: Tải lại danh sách/chi tiết để kiểm tra xem đã lưu chưa.
  Không tự retry thao tác ghi. Lỗi trùng mã/tên hoặc ràng buộc được trả về có kiểm soát.

## Phạm vi còn chưa làm

- Đã có sổ `LichSuKho` cho mốc tồn mở đầu, khởi tạo biến thể, bán hàng và điều chỉnh tồn.
  Chưa có nhà cung cấp hoặc phiếu nhập kho (FR11); không mô phỏng nhập hàng bằng thao tác
  điều chỉnh tồn.
- Chưa có tải ảnh mới qua Socket hoặc quản lý thêm/sửa metadata ảnh.
  Ảnh đã chuẩn hóa trong resources vẫn được dùng; sản phẩm mới chưa có ảnh sẽ hiện thông báo thiếu ảnh.
- Backend nhân viên/tài khoản, khách hàng, khuyến mãi và thống kê có service/action/test,
  nhưng UI/API client tương ứng còn rỗng hoặc chưa nối vào sidebar.
- Baseline trước ledger có 12 bảng; migration 05 thêm `LichSuKho`, nên schema sau mở rộng
  có 13 bảng logic. Migrations 04/05/06 vẫn phải được kiểm tra trên SQL Server demo;
  `hbm2ddl.auto=validate` không tạo hoặc sửa schema tự động.

## Kiểm thử

CatalogManagementIntegrationTest: 12 test qua TCP/JPA/H2, bao gồm thêm/sửa,
trùng mã/tên/màu-size, validation, khóa tài khoản, kiểm tra mọi action ghi bằng vai trò bán hàng,
xung đột khi hai client cùng sửa, điều chỉnh tồn đồng thời thanh toán và giữ giá chốt hóa đơn.

CatalogManagementUiTest render các tab và editor với FlatLaf ở .tmp/catalog-management-ui,
kiểm tra bảng chỉ đọc và editor tạo DTO mới, không làm thay đổi ảnh chụp expected.
Các test đăng nhập, tra cứu và hóa đơn trước đó cũng được chạy lại.
