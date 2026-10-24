# Chạy ứng dụng trong Eclipse

## 1. Phạm vi đã làm

Đăng nhập → xem thông tin cá nhân → đổi mật khẩu → đăng xuất.
Có hai vai trò NHAN_VIEN_BAN_HANG và NHAN_VIEN_QUAN_LY, lấy từ database.
Cả hai được dùng các chức năng tài khoản và tra cứu sản phẩm (xem TRA_CUU_SAN_PHAM.md).
Đã có bán hàng/thanh toán (BAN_HANG.md) và quản lý danh mục/sản phẩm
(QUAN_LY_DANH_MUC_SAN_PHAM.md). Các màn hình lấy dữ liệu từ SQL Server qua máy chủ,
không dùng dữ liệu demo thay cho dữ liệu thật.

Luồng thực tế:

Client Swing → controller → AuthApi → SocketClient → RequestDispatcher
→ AuthService → JpaTaiKhoanDao → JPA/Hibernate → SQL Server.

EntityManager riêng cho từng transaction; chỉ DTO được trả về client.
Server kiểm tra tài khoản và nhân viên đang hoạt động mỗi lần đọc hồ sơ/đổi mật khẩu.
Không lấy mã nhân viên hoặc vai trò do client tự gửi làm nguồn phân quyền.

## 2. Chuẩn bị database

Database phải có dbo.NhanVien và dbo.TaiKhoan theo script hiện tại.
Nếu đã tạo database bằng script trước đây, không chạy lại script tạo database.
Không đổi passwordHash thành mật khẩu rõ để thử đăng nhập.

Trong SSMS kiểm tra server/instance, tên database và SQL login được cấp quyền phù hợp.
URL dùng cổng TCP: SQL Server phải bật TCP/IP và lắng nghe tại cổng đó.
Không mặc định mọi instance đều dùng cổng 1433.

## 3. Refresh Maven và cấu hình server

Trong Eclipse:

1. F5 → Maven → Update Project (Alt+F5).
2. Kiểm tra JDK trong Java Build Path; source/target vẫn Java 8.
3. Run → Run Configurations → Java Application → server.ServerMain.
4. Tab Environment, thêm:

| Biến | Giá trị |
| --- | --- |
| QUANAO_DB_URL | jdbc:sqlserver://localhost:1433;databaseName=QuanLyCuaHangQuanAo;encrypt=true;trustServerCertificate=true;loginTimeout=10 |
| QUANAO_DB_USER | SQL login của bạn |
| QUANAO_DB_PASSWORD | Mật khẩu SQL login |

URL trên chỉ là ví dụ cho SQL Server local. trustServerCertificate=true chỉ phù hợp
demo local với chứng chỉ tự ký; khi triển khai thật phải kiểm chứng chứng chỉ.

Không lưu mật khẩu vào source, persistence.xml hoặc commit file .launch chứa bí mật.
Client không cần ba biến DB này. Chỉ server được biết thông tin truy cập database.

persistence.xml dùng hbm2ddl.auto=validate: kiểm tra schema, không tự tạo/xóa/sửa bảng.
Đã ánh xạ NhanVien, TaiKhoan, SanPham, BienThe, LoaiSanPham, ThuongHieu, AnhSanPham.
Cần bổ sung bảng ảnh bằng script 01 nếu chưa có; các bảng khác vẫn được giữ nguyên.

## 4. Tạo tài khoản quản lý đầu tiên nếu database chưa có tài khoản

Tạo Run Configuration riêng cho server.AccountSetupMain, với ba biến DB ở trên
và năm biến:

| Biến | Giá trị tự chọn |
| --- | --- |
| QUANAO_DB_URL | jdbc:sqlserver://localhost:1433;databaseName=QuanLyCuaHangQuanAo;encrypt=true;trustServerCertificate=true;loginTimeout=10 |
| QUANAO_DB_USER | SQL login của bạn |
| QUANAO_DB_PASSWORD | Mật khẩu SQL login |
| QUANAO_INITIAL_USERNAME | Tên đăng nhập ASCII, ví dụ quanly |
| QUANAO_INITIAL_PASSWORD | Mật khẩu riêng của bạn, 8–128 ký tự |
| QUANAO_INITIAL_EMPLOYEE_ID | Mã nhân viên mới chưa tồn tại, ví dụ NVQL001 |
| QUANAO_INITIAL_EMPLOYEE_NAME | Họ tên của bạn |
| QUANAO_INITIAL_EMPLOYEE_PHONE | Số điện thoại chưa trùng trong NhanVien |

Chạy một lần. Công cụ tạo NhanVien và TaiKhoan trong cùng transaction, vai trò quản lý,
mật khẩu băm PBKDF2, isolation SERIALIZABLE cho bước setup. Nếu đã có bất kỳ tài khoản nào hoặc mã nhân viên đã tồn tại,
công cụ từ chối, không ghi đè. Nếu vi phạm unique số điện thoại, toàn bộ transaction rollback.

Xóa QUANAO_INITIAL_PASSWORD khỏi Run Configuration sau khi tạo.
Đây là công cụ setup cục bộ, không được gọi từ client và không phải chức năng quản lý nhân viên.
Nếu đã có tài khoản hợp lệ, bỏ qua bước này và dùng mật khẩu hiện có.
Hash cũ không đúng định dạng PBKDF2 của project sẽ không xác thực được; không có fallback plaintext.

## 5. Chạy và thử

1. Run server.ServerMain. Console phải báo server đang chạy tại 127.0.0.1:8888.
2. Run client.ClientMain. Xuất hiện cửa sổ Swing đăng nhập.
3. Đăng nhập bằng tài khoản vừa tạo.
4. Bấm Thông tin cá nhân → Tải thông tin.
5. Thử mật khẩu cũ sai khi đổi mật khẩu: phải báo lỗi và giữ nguyên dữ liệu.
6. Đổi bằng mật khẩu cũ đúng: trở về đăng nhập; mật khẩu mới dùng được, mật khẩu cũ không.
7. Đăng xuất: thu hồi token ở server và trở về đăng nhập.

MainForm có Tra cứu sản phẩm và Bán hàng / Hóa đơn; xem BAN_HANG.md để thử
tạo nháp, thêm/đổi/bỏ hàng, hủy nháp và thanh toán/trừ tồn. Quản lý khách hàng,
nhân viên/tài khoản, khuyến mãi, thuế và thống kê đã có; xem
docs/QUAN_LY_CUA_HANG_THONG_KE.md để chạy dữ liệu hóa đơn/khuyến mãi/thuế minh họa.
Giao diện đã dùng Swing + FlatLaf: xem GIAO_DIEN_FLATLAF.md.
Sau khi Refresh phải Maven → Update Project để có dependency FlatLaf trước khi chạy ClientMain.
Hồ sơ chỉ đọc; chưa có sửa hồ sơ cá nhân.
I/O và phép chờ server chạy bằng SwingWorker, không chặn Event Dispatch Thread.

## 6. Kiểm thử tự động

Eclipse: Run As → Maven build → Goals: test, hoặc chạy các lớp bằng Run As → JUnit Test.

- DependencySmokeTest: classpath và JSON.
- SecurityProtocolTest: băm mật khẩu, salt, phiên hết hạn và frame.
- AuthIntegrationTest: JPA + Socket thật, đăng nhập hai vai trò, hồ sơ, đổi mật khẩu,
  thu hồi phiên, tài khoản/nhân viên bị khóa, chống chọn hồ sơ bằng mã giả,
  rollback, hai client đồng thời và frame/JSON không hợp lệ.

Chỉ test cấu hình H2 trong bộ nhớ với create-drop; cấu hình SQL Server thực tế vẫn validate.
Test không cần mật khẩu DB và không đọc QUANAO_DB_*.

Nếu không có mvn trên PATH nhưng các dependency đã được Eclipse tải vào .m2,
có thể dùng PowerShell với JDK 9+ để biên dịch --release 8:

    .\scripts\Test-Local.ps1 -JdkDirectory 'C:\Program Files\Java\jdk-23'

Đường dẫn JDK là ví dụ; thay bằng đường dẫn đã cài. Có thể thêm -JavaDirectory
để chạy test bằng JRE khác, chẳng hạn Java 8. Script dùng cache dependency và không tải thêm.

## 7. Quy tắc mạng và bảo mật của bản demo

- Mặc định server chỉ bind 127.0.0.1.
- Giao thức: int32 big-endian độ dài, tiếp theo là JSON UTF-8; giới hạn 1 MiB/frame.
- Connect timeout client 5 giây; read timeout client 15 giây; server 60 giây.
- Server có 16 worker và queue 64; kết nối vượt giới hạn sẽ bị đóng.
- Sau 5 lần đăng nhập lỗi trên một kết nối, server đóng kết nối đó.
  Đây chưa phải rate limit chống brute-force toàn hệ thống.
- Token 32 byte ngẫu nhiên; hết hạn tuyệt đối sau 30 phút, không tự gia hạn.
- Đổi mật khẩu làm mất hiệu lực tất cả token cũ. Khóa tài khoản/nhân viên
  được kiểm tra ở server khi dùng chức năng tài khoản.
- PBKDF2-HMAC-SHA256 600.000 vòng, salt 16 byte; không log mật khẩu/token.
- TCP hiện chưa có TLS, nên mật khẩu/token không được mã hóa trên đường truyền.
  Không đưa lên Internet hoặc dùng tài khoản thật trên mạng không tin cậy.
- Khi demo hai máy trong LAN tin cậy: chủ động đổi server.bind.host trong
  config/server.properties, server.host trong config/client.properties sang IP máy chủ
  và cấu hình firewall. Chưa có mã tự mở firewall.
- Không tự gửi lại request sau lỗi mạng, để tránh thực hiện thao tác hai lần.
  Nếu đổi mật khẩu bị mất phản hồi, kiểm tra đăng nhập lại trước khi thử tiếp.
- Phiên lưu trong RAM: restart server sẽ khiến mọi người dùng phải đăng nhập lại.

## 8. Dữ liệu minh họa và phần tiếp theo

Tra cứu sản phẩm/biến thể/ảnh đã triển khai; xem TRA_CUU_SAN_PHAM.md.
Hóa đơn, thanh toán và trừ tồn kho đã triển khai; xem BAN_HANG.md.
Quản lý danh mục/sản phẩm đã có; xem QUAN_LY_DANH_MUC_SAN_PHAM.md.
Để có doanh thu mẫu, chọn database đúng trong SSMS rồi chạy thủ công `sql/03_CauHinhThueBanLe.sql`, sau đó
`sql/04_DuLieuMauHoaDon_KhuyenMai_DoanhThu.sql` trong SSMS sau khi đã nạp danh mục/biến thể
và tạo tài khoản hoạt động. Đây là giao dịch minh họa, không chạy trên database thật;
script 04 trừ tồn một lần cho hóa đơn demo đã thanh toán. Chi tiết và thứ tự đầy đủ ở
`docs/QUAN_LY_CUA_HANG_THONG_KE.md`. In hóa đơn/đổi trả/phiếu nhập kho vẫn chưa thuộc bản này.
