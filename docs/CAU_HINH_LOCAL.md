# Chạy app bằng application.local.properties

File `application.local.properties` nằm tại thư mục workspace, cùng cấp với
`QuanLyBanQuanAo_Server` và `QuanLyBanQuanAo_Client`. File trên máy đã có thông tin kết nối SQL Server.

## Chạy bằng Eclipse

1. Bật dịch vụ SQL Server `SQLEXPRESS`; database `QuanLyCuaHangQuanAo` đã được tạo sẵn.
2. Import cả hai project theo [hướng dẫn mở workspace](../README.md#mở-workspace-eclipse).
3. Run As → Java Application với **server.app.ServerMain**. Chờ thông báo server chạy cổng 8888.
4. Run As → Java Application với **client.app.ClientMain**; giao diện đăng nhập hiện ra.
5. Đăng nhập bằng **QUAN_LY / 123456** hoặc **demo.quanly / 123456**.
   Nhân viên bán hàng dùng **demo.banhang / 123456**.

Không cần nhập biến môi trường, sửa Java hoặc chạy TaoTaiKhoanQuanLy.
Ứng dụng không tự đăng nhập; bạn nhập tài khoản/mật khẩu trên giao diện.

## Các khóa cấu hình

| Khóa | Mục đích |
| --- | --- |
| `db.url` | JDBC SQL Server, database `QuanLyCuaHangQuanAo`, cổng SQL 1433 |
| `db.username` | SQL login, hiện tại `sa` |
| `db.password` | Mật khẩu SQL login đang dùng trên máy, đã điền sẵn |
| `server.bind.host` | Địa chỉ server lắng nghe, `127.0.0.1` |
| `server.host` | Địa chỉ client kết nối, `localhost` |
| `server.port` | Cổng app dùng chung cho server và client, `8888` |
| `setup.*` | Chỉ dành cho công cụ tạo quản lý đầu tiên trên database chưa có tài khoản |

`db.password` là mật khẩu SQL Server. Mật khẩu đăng nhập giao diện `123456` được kiểm
tra bằng hash trong bảng `TaiKhoan`, độc lập với tài khoản kết nối SQL Server.

Server và Client tự tìm file cấu hình ở thư mục workspace phía trên hai project.
Khi cần đặt ở vị trí khác, thêm VM argument
`-Dquanao.config="C:/duong-dan/application.local.properties"`. Nếu đường dẫn chỉ định
không tồn tại, app báo lỗi thay vì tự dùng cấu hình khác.

File UTF-8 được ưu tiên trước biến môi trường. `config/server.properties` nằm trong
resources của Server và `config/client.properties` nằm trong `src` của Client.
Hai file này cung cấp giá trị mạng dự phòng. Thay đổi cấu hình có hiệu lực khi chạy lại ứng dụng.

## Chạy bằng launcher

Chạy `scripts/ChayUngDung.cmd` để launcher biên dịch source mới, bật server ở nền rồi
mở client. Launcher dùng cùng file cấu hình và đóng server của nó khi client kết thúc.
Nếu đã chạy ServerMain riêng trong Eclipse, chạy ClientMain riêng để kết nối tới server đó.

File local được bỏ qua trong Git; `application.local.properties.example` là bản mẫu
khi chuyển dự án sang máy khác. Ảnh vẫn dùng resource sẵn có của client.

Kiểm tra sau khi sắp xếp ngày 06/11/2026: **106/106 test đạt**. Đã chạy ServerMain trực tiếp bằng file
local, kiểm tra khởi tạo form đăng nhập/ManHinhChinh và đăng nhập cả 4 tài khoản bằng
`123456` qua TCP/SQL Server thật; dữ liệu database không thay đổi. Các tiến trình kiểm
tra đã được đóng, sẵn sàng để bạn chạy server/client riêng trong Eclipse.
