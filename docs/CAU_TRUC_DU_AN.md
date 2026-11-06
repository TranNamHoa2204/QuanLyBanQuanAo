# Cấu trúc dự án sau khi sắp xếp — 06/11/2026

## Đối chiếu bài mẫu của cô

Bài `SM36_TCPSocket_11111` tách Server Maven Project và Client Java Project.
Workspace này áp dụng cùng bố cục. Server chứa `server` và `common` trong `src/main/java`.
Client chứa source Swing và tài nguyên trong `src`, kết quả biên dịch vào `bin`.
Java Build Path của Client nạp `lib/server.jar`, FlatLaf và Jackson. Maven `package`
trên Server tạo `target/server.jar` và cập nhật JAR trong `lib` của Client.

Maven dùng nguồn Client trong bước kiểm thử tích hợp của Server. JAR xuất từ Server
được đóng gói từ `target/classes`. [Tài liệu Build Helper Maven Plugin](https://www.mojohaus.org/build-helper-maven-plugin/add-test-source-mojo.html)

| Vai trò | Package |
| --- | --- |
| Điểm khởi chạy | `server.app`, `client.app` |
| Entity JPA | `server.entities` |
| Hợp đồng truy cập dữ liệu | `server.repositories` |
| Triển khai JPA | `server.repositories.impl` |
| Xử lý nghiệp vụ và transaction | `server.services` |
| Khởi tạo EntityManagerFactory, phạm vi EntityManager | `server.utils.QuanLyJpa` |
| Nhận kết nối/xử lý TCP | `server.network`, `client.network` |
| Phân phối action | `server.dispatcher` |
| Băm mật khẩu, phiên đăng nhập, quyền | `server.security`, `server.session` |
| Gọi API và xử lý sự kiện Swing | `client.api`, `client.controllers` |
| Giao diện theo nghiệp vụ | `client.gui.*` |
| DTO theo nghiệp vụ | `common.dto.*` |
| Giao thức, enum và cấu hình dùng chung | `common.protocol`, `common.enums`, `common.config` |

Cấu trúc cây đầy đủ có trong README. Các lớp/file Java dùng tiếng Việt không dấu.
Các tên chuẩn của Java/Maven, `persistence.xml` và `application.local.properties`
được giữ để tương thích công cụ và cấu hình local đã có.

## Các lớp tiêu biểu

| Trước đây | Hiện tại |
| --- | --- |
| `server.ServerMain` | `server.app.ServerMain` |
| `client.ClientMain` | `client.app.ClientMain` |
| `server.persistence.JpaManager` | `server.utils.QuanLyJpa` |
| `server.repository.SanPhamDao` | `server.repositories.LuuTruSanPham` |
| `server.repository.JpaSanPhamDao` | `server.repositories.impl.LuuTruSanPhamJpa` |
| `client.ui.MainForm` | `client.gui.hethong.ManHinhChinh` |
| `client.ui.MainShellPanel` | `client.gui.hethong.KhungUngDung` |
| `common.dto.LoginRequest` | `common.dto.taikhoan.YeuCauDangNhap` |

[Bảng đầy đủ](BANG_DOI_TEN_LOP.md) ghi lại từng lớp trước và sau khi đổi tên.

## Quy tắc phụ thuộc

Luồng xử lý: GUI → controller → API → TCP → dispatcher → service → repository/JPA.
DTO và giao thức nằm trong common; client không truy cập database trực tiếp.
Service giữ transaction cho toàn bộ nghiệp vụ, gồm hóa đơn/chi tiết/tồn kho/ledger
hoặc phiếu nhập/chi tiết/tồn kho/operation id. Mỗi tác vụ có EntityManager riêng.

Giao thức JSON và tên trường dữ liệu được giữ nguyên. Entity vẫn ánh xạ các bảng/cột
SQL Server hiện tại; `TaiKhoan.matKhauHash` tiếp tục chứa PBKDF2-SHA256.
Ứng dụng dùng bộ JPA `javax.persistence`/Hibernate đã kiểm tra tương thích trong pom.

## Những phần đã dọn

- Bỏ 17 file Java không có tham chiếu từ mã chạy hoặc kiểm thử: các service/controller/API
  khung rỗng, form khung và cửa sổ cũ đã được thay bằng tab/panel đang dùng.
- Bỏ `META-INF/persistence.xml.example` cũ; file JPA hiện hành khai báo đủ 17 entity.
- Bỏ hướng dẫn chạy trùng tại thư mục gốc; bản chính nằm trong `docs/HUONG_DAN_CHAY.md`.
- Tách interface repository và triển khai JPA; bỏ comment TODO đã lỗi thời.
- Chia DTO và GUI theo nghiệp vụ, đưa các test mặc định vào package `kiemthu`.
- Loại import thừa, thay import wildcard bằng import cụ thể và định dạng mã bằng Eclipse JDT:
  thụt 4 khoảng trắng, độ dài dòng mục tiêu 110 ký tự.
- Cập nhật script, Maven Surefire và cấu hình launch của Eclipse theo tên lớp mới.
- Eclipse dùng JDK 17, target bytecode Java 8; bỏ đường dẫn generated-sources không dùng.
- Metadata workspace Eclipse và output build được bỏ qua trong Git.

Ảnh, catalog, SQL, `application.local.properties`, database và báo cáo gốc được giữ.
Bản sao trước khi chỉnh sửa được lưu bằng ZIP tại thư mục Temp trên máy.

## Chạy lại sau khi đổi tên

1. Import cả hai project vào workspace `QuanLyBanQuanAo` theo hướng dẫn trong README.
2. Chạy `ServerMain.java` trong Server, sau đó `ClientMain.java` trong Client bằng **Run As → Java Application**.
3. Hoặc chạy `scripts/ChayUngDung.cmd`.
4. Đăng nhập `QUAN_LY / 123456`; cấu hình vẫn lấy từ `application.local.properties`.

Các lớp `KiemThuCauTrucDuAn` kiểm tra package/đường dẫn, tên lớp/file, ranh giới
client/common với server/JPA, đủ 17 entity và điểm khởi chạy trong script/Eclipse.

## Kết quả kiểm tra hai project

- Maven `package` đạt **107/107 JUnit test** và xuất `server.jar` vào Client.
- Script PowerShell đạt **107/107 test** và đọc được **267/267 ảnh** từ bản build Client.
- Client biên dịch bằng đúng các JAR trong `.classpath`. Kiểm thử hai JVM với H2 đã đăng nhập qua TCP, đọc 40 sản phẩm và khởi tạo Swing.
- Lần kiểm tra SQL Server thật gặp dịch vụ `SQLEXPRESS` đang dừng.
