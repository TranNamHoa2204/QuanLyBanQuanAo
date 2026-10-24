# Khách hàng, nhân viên/tài khoản, khuyến mãi và thống kê

## Chạy bản cập nhật

1. Trong Eclipse: chọn project → F5; Project → Clean nếu Eclipse còn chạy class cũ.
2. Dừng cả ServerMain và ClientMain cũ, chạy lại server.ServerMain rồi client.ClientMain.
3. Đăng nhập bằng tài khoản quản lý để thấy đủ menu.
4. Mã nguồn đã có mục Thuế trong Quản trị; để có dữ liệu demo, chạy thủ công hai script SQL bên dưới.
   Các thao tác trên giao diện chỉ ghi dữ liệu khi bạn bấm lưu/ngừng hoạt động/thanh toán.
   Không chạy lại script tạo CSDL hoặc import để cập nhật mã.

## Khách hàng — cả hai vai trò

- Tra cứu mã/tên/điện thoại, 25 bản ghi/trang; checkbox xem cả ngừng hoạt động.
- Thêm/sửa họ tên, điện thoại, email, địa chỉ và trạng thái.
- Điện thoại/email/địa chỉ có thể trống; chuỗi trắng chuyển thành NULL.
- Điện thoại bỏ dấu cách/dấu chấm/ngoặc/gạch ngang, kiểm tra 7–19 chữ số và dấu + tùy chọn.
  Một số điện thoại không dùng cho hai khách hàng. SQL gốc có unique index lọc NULL.
- Mã bất biến, tối đa 20 ký tự chữ không dấu/số/-/_.
- Ngừng hoạt động không xóa khách hàng hoặc hóa đơn cũ; sửa và bật trạng thái để kích hoạt lại.
- Màn Bán hàng hiện vẫn nhập mã KH; lấy mã từ danh sách này, để trống khi khách lẻ.
  Server kiểm tra khách còn hoạt động khi tạo nháp và thanh toán.

## Nhân viên & tài khoản — chỉ quản lý

- Thêm/sửa thông tin cá nhân, chức vụ, trạng thái nhân viên.
- Có thể tạo nhân viên trước và để trống tên đăng nhập; cấp tài khoản sau bằng Sửa.
- Khi cấp tài khoản mới: tên đăng nhập không dấu (chữ/số/_/./-), tối đa 50 ký tự,
  mật khẩu ban đầu 8–128 ký tự, chọn vai trò bán hàng hoặc quản lý.
  Nếu cấp tài khoản sau, nhớ bật “Cho phép đăng nhập”.
- Một nhân viên có tối đa một tài khoản; lưu nhân viên/tài khoản trong cùng transaction.
- Không đổi mã nhân viên/tên đăng nhập đã cấp. Không đọc, hiển thị hoặc trả passwordHash.
- Mật khẩu ban đầu chỉ dùng lúc cấp tài khoản. Không có chức năng quản lý đặt lại
  mật khẩu tài khoản hiện có: người dùng tự dùng Đổi mật khẩu với mật khẩu cũ.
- Ngừng nhân viên đồng thời khóa tài khoản; các phiên đăng nhập của người đó bị thu hồi.
  Đổi vai trò/trạng thái tài khoản hoặc trạng thái nhân viên cũng thu hồi phiên sau commit.
- Không cho tự khóa/hạ quyền tài khoản đang sử dụng; luôn phải còn quản lý hoạt động.
- Hai thao tác quản trị tài khoản được serialize trong JVM server hiện tại để tránh hai
  quản lý đồng thời hạ quyền nhau. Không hỗ trợ nhiều JVM server chia sẻ cùng CSDL;
  cần khóa liên tiến trình/database nếu mở rộng triển khai đó.
- Quyền được kiểm tra từ CSDL ở mỗi yêu cầu, không tin vai trò gửi bởi client.

## Khuyến mãi — chỉ quản lý ghi, cả hai vai trò có API tra cứu

- Thêm/sửa tên, tỷ lệ giảm 0–100%, tối đa hai chữ số thập phân, ngày bắt đầu/kết thúc.
- Ngày có dạng yyyy-MM-dd; ngày bắt đầu không được sau ngày kết thúc.
- Ngừng áp dụng giữ bản ghi và hóa đơn cũ; không xóa cứng.
- “Đang áp dụng” là cờ bật/tắt, chưa đồng nghĩa còn hiệu lực: còn phải kiểm tra ngày.
  API tra cứu mặc định trả các chương trình bật, kể cả chưa đến ngày/hết hạn;
  server bán hàng quyết định hiệu lực khi tạo/thanh toán. Chỉ quản lý xem được cờ tắt.
- Màn Bán hàng vẫn nhập mã KM (tùy chọn). Quản lý lấy mã ở danh sách này.
  Nhân viên bán hàng có thể nhập mã được cửa hàng thông báo; chưa có picker trên form.
- Hóa đơn giữ phần trăm và tiền giảm đã chốt. Đổi cấu hình không sửa tiền hóa đơn cũ.
- Nếu tỷ lệ đổi sau khi lập nháp hoặc chương trình tắt/hết hạn, thanh toán bị từ chối;
  hủy nháp rồi tạo lại theo cấu hình hiện hành.
- Thanh toán khóa/đọc lại khuyến mãi và khách hàng trước khi kiểm tra,
  tránh dùng trạng thái cũ trong khi một người khác đang sửa.

## Thuế — chỉ quản lý

- Mục Thuế nằm trong nhóm Quản trị; quản lý có thể tra cứu, thêm, sửa và ngừng áp dụng cấu hình.
- Tỷ lệ nhận từ 0–100%, tối đa hai chữ số thập phân. Cấu hình được tham chiếu bởi hóa đơn thì
  chỉ được ngừng áp dụng, không xóa; hóa đơn đã lập giữ nguyên tỷ lệ/tiền thuế đã chốt.
- Màn Bán hàng chỉ tải cấu hình đang áp dụng để chọn khi lập nháp. Máy chủ kiểm tra lại cấu hình
  khi thanh toán; không thể thanh toán với cấu hình đã ngừng.
- `VAT10` là mức tiêu chuẩn; `VAT8` có thể áp dụng cho hàng hóa thuộc diện giảm theo chính sách
  đến hết 31/12/2026; `VAT0` chỉ dành cho giao dịch đủ điều kiện (ví dụ xuất khẩu đủ hồ sơ).
  Không mặc định mọi sản phẩm quần áo đều đủ điều kiện hưởng 8%—cần đối chiếu mã hàng với danh mục
  loại trừ hiện hành.
- CSDL hiện không lưu thời hạn hiệu lực của cấu hình thuế, nên quản lý phải ngừng/cập nhật VAT8
  khi hết hạn hoặc khi có thay đổi chính sách. Chọn 0% không tự xác minh điều kiện xuất khẩu.
- Mức 1,5% của hộ kinh doanh bán lẻ (1% GTGT + 0,5% TNCN) không phải một thuế suất VAT để cộng
  vào hóa đơn khách hàng. Ứng dụng hiện chưa hỗ trợ phương pháp tính theo tỷ lệ trên doanh thu;
  cần mô hình thuế riêng và xác định đúng chế độ/ngưỡng áp dụng trước khi triển khai.

## Tạo dữ liệu minh họa cho hóa đơn, khuyến mãi và doanh thu

Trong SSMS, trên bản sao/demo database, chạy theo thứ tự:

1. `sql/02_NhapDuLieuQuanAo.sql` nếu chưa có danh mục và biến thể mẫu.
2. Chọn đúng database trong SSMS rồi chạy `sql/03_CauHinhThueBanLe.sql` để thêm các cấu hình VAT.
3. `sql/04_DuLieuMauHoaDon_KhuyenMai_DoanhThu.sql` để thêm hai khách hàng,
   hai khuyến mãi và sáu hóa đơn mẫu (bốn đã thanh toán, một nháp, một đã hủy).

Script 04 cần ít nhất một nhân viên có tài khoản đang hoạt động và tồn kho đủ cho các
hóa đơn đã thanh toán. Hóa đơn demo dùng các mã biến thể thuộc bộ dữ liệu trong script 02.
Chạy 04 lần đầu sẽ trừ tồn kho cho hóa đơn đã thanh toán; chạy lại không tạo trùng hoặc trừ
tồn lần hai. Các script chỉ thêm mã demo còn thiếu, không tự sửa/xóa dữ liệu cửa hàng.
Không chạy lên database thật nếu không muốn có giao dịch minh họa và thay đổi tồn kho.

Sau khi chạy SQL, khởi động lại server/client. Mục Khuyến mãi và Thuế sẽ hiển thị dữ liệu;
màn Thống kê lọc theo ngày lập hóa đơn và chỉ tính bốn hóa đơn `DA_THANH_TOAN`.
Màn Bán hàng có thể chọn các cấu hình thuế demo đang áp dụng.

## Thống kê — chỉ quản lý

- Chọn từ/đến (bao gồm cả hai ngày), rồi Xem thống kê.
- Nhóm theo ngày/tháng/năm hoặc sản phẩm, tối đa 50 dòng/trang trên giao diện.
- Chỉ lấy hóa đơn DA_THANH_TOAN, không tính NHAP và DA_HUY.
- Lọc và nhóm theo ngayLap, **không phải ngày thanh toán**:
  schema hiện tại chưa có ngayThanhToan. Nháp lập ngày trước nhưng thanh toán hôm nay
  vẫn được ghi vào ngày lập trong báo cáo này.
- Tổng thanh toán = SUM(HoaDon.tongTien); tạm tính/giảm/thuế được cộng từ header
  một lần mỗi hóa đơn, không nhân lên theo số dòng hàng.
- Theo sản phẩm: số lượng bán và SUM(ChiTietHoaDon.donGia × soLuong), dùng giá chốt.
  Đây là tiền hàng trước giảm/thuế, **không phải doanh thu thuần phân bổ theo sản phẩm**.
- Báo cáo không có giao dịch trả 0 và danh sách trống; không tạo số liệu giả.
- Aggregate ở database, phân trang 1–100 dòng; giới hạn khoảng ngày 3.660 ngày.
- Tên sản phẩm hiển thị theo tên hiện tại. Không có chi phí vốn/lợi nhuận hay xuất Excel/PDF.
- Báo cáo dùng các truy vấn đọc trong transaction mặc định của DB, không phải snapshot
  kế toán bất biến: khi có giao dịch thanh toán đồng thời, tải lại để đối chiếu số mới nhất.

## Xung đột và lỗi mạng

Khách hàng, nhân viên/tài khoản và khuyến mãi chưa có cột @Version trong schema.
Server khóa dòng và so sánh snapshot expected của các trường được sửa; nếu khác,
trả VERSION_CONFLICT thay vì ghi đè. Đây không phải bộ đếm phiên bản, không phát hiện
trường hợp giá trị đổi rồi quay lại y hệt.

Không tự retry yêu cầu ghi. Nếu mất phản hồi lúc lưu/cấp tài khoản, tải lại danh sách
để kiểm tra đã lưu chưa trước khi tạo lại cùng mã. Mật khẩu đã cấp không thể xem lại.

TCP chưa có TLS; chỉ dùng tài khoản demo/localhost hoặc LAN tin cậy theo HUONG_DAN_CHAY.md.

## Kiểm thử

StoreManagementIntegrationTest có 17 test qua TCP + Hibernate/H2: quyền mọi action
quản trị, CRUD/soft stop, phone/email, phân trang, stale snapshot, cấp tài khoản,
rollback mật khẩu sai, không lộ hash, thu hồi phiên, bảo vệ quản lý cuối,
thay khuyến mãi/khách khi thanh toán và báo cáo không cộng trùng.

StoreManagementUiTest render bảng, editor và báo cáo Swing/FlatLaf,
kiểm tra menu theo vai trò, bảng chỉ đọc và vòng chuyển đổi dữ liệu trên form.
Ảnh QA dùng fixture nằm trong .tmp/store-management-ui, không phải dữ liệu cửa hàng thật.

Test chạy H2 trong RAM, không kết nối SQL Server của bạn; H2 không mô phỏng đầy đủ
filtered unique index, computed column hoặc cơ chế khóa SQL Server.
Cần thử trên CSDL demo SQL Server trước khi dùng dữ liệu thật.

## Ngoài phạm vi bản này

Chưa có in hóa đơn, đổi/trả/hoàn tiền hóa đơn đã thanh toán, phiếu nhập kho,
sổ điều chỉnh tồn, tải ảnh mới hoặc tích hợp thanh toán ngân hàng.
