# Ứng dụng quản lý cửa hàng quần áo

Bộ khung Maven cho nhóm 2 người, JPA + Socket Client/Server, SQL Server.
Các package gốc: `client`, `common`, `server`; groupId: `vn.edu.iuh`.

## Trạng thái hiện tại

- Có 12 mô hình dữ liệu (gồm AnhSanPham) và DTO riêng.
- Có enum vai trò/trạng thái, danh mục Action, Request/Response generic.
- Có đọc cấu hình UTF-8, kiểm tra vai trò cơ bản, tiện ích tính tiền và smoke test.
- Đã triển khai JPA cho NhanVien/TaiKhoan, JpaManager và JpaTaiKhoanDao.
- Đã có TCP Socket: JSON UTF-8 với frame độ dài, timeout và giới hạn kích thước.
- Đã có đăng nhập, hồ sơ cá nhân, đổi mật khẩu và đăng xuất xuyên suốt client/server.
- Giao diện Swing + FlatLaf 3.7: đăng nhập, sidebar, sản phẩm, bán hàng,
  hồ sơ, đổi mật khẩu và các màn quản lý/thống kê. Xem [docs/GIAO_DIEN_FLATLAF.md](docs/GIAO_DIEN_FLATLAF.md).
- PBKDF2 có salt; token ngẫu nhiên hết hạn sau 30 phút; đổi mật khẩu thu hồi phiên.
- Đã ánh xạ JPA đủ 12 entity; có bán hàng, hóa đơn nháp, chỉnh dòng, hủy nháp,
  danh sách/chi tiết hóa đơn và thanh toán/trừ tồn trong transaction chung.
- Đã có thêm/sửa loại, thương hiệu, sản phẩm; ngừng bán/kinh doanh lại,
  thêm biến thể và sửa tồn/trạng thái có kiểm tra xung đột.
- Đã có khách hàng; quản lý nhân viên/cấp tài khoản, khuyến mãi và thống kê theo
  ngày/tháng/năm/sản phẩm. Xem [docs/QUAN_LY_CUA_HANG_THONG_KE.md](docs/QUAN_LY_CUA_HANG_THONG_KE.md).
- Đã có màn quản trị Thuế và hai script SSMS để thêm thuế, khuyến mãi, hóa đơn và doanh thu minh họa.
- Chưa kết nối/ghi dữ liệu lên SQL Server thực tế trong quá trình viết code.
- Công cụ AccountSetupMain chỉ tạo quản lý đầu tiên khi chạy chủ động, không có mật khẩu mặc định.

Phần tra cứu sản phẩm đã có: lọc tên/loại/thương hiệu/màu/size, phân trang,
chi tiết biến thể, tồn kho và ảnh theo màu. Cả hai vai trò dùng được;
quản lý được xem cả hàng ngừng bán. Màn hình tra cứu này chỉ đọc, chưa có CRUD.
Quản lý ghi dữ liệu ở menu riêng: xem [docs/QUAN_LY_DANH_MUC_SAN_PHAM.md](docs/QUAN_LY_DANH_MUC_SAN_PHAM.md).
Luồng bán hàng nằm ở màn hình riêng; xem [docs/BAN_HANG.md](docs/BAN_HANG.md).
Xem [docs/TRA_CUU_SAN_PHAM.md](docs/TRA_CUU_SAN_PHAM.md) trước khi chạy bản cập nhật.

Hướng dẫn chạy chi tiết: [docs/HUONG_DAN_CHAY.md](docs/HUONG_DAN_CHAY.md).

## Các package

```text
src/main/java
├── common
│   ├── dto        Dữ liệu trao đổi; không chứa entity hoặc passwordHash
│   ├── enums      VaiTro, TrangThaiHoaDon
│   ├── protocol   Action, ErrorCode, Request<T>, Response<T>
│   └── util       PropertiesLoader, MoneyUtils
├── server
│   ├── config     ServerConfig, DatabaseConfig
│   ├── repository GenericDao và 11 DAO theo bảng (giữ tên class bạn đang dùng)
│   ├── dispatcher RequestDispatcher
│   ├── entity     12 entity đã ánh xạ JPA
│   ├── network    SocketServer, ClientHandler
│   ├── persistence JpaManager
│   ├── security   PasswordHasher, PermissionChecker
│   ├── service    Các lớp nghiệp vụ
│   ├── session    SessionManager
│   └── ServerMain
└── client
    ├── api        Gọi chức năng qua Socket
    ├── config     ClientConfig
    ├── controller Xử lý sự kiện của các màn hình
    ├── network    SocketClient
    ├── session    ClientSession
    ├── ui         Swing/FlatLaf cho tài khoản, bán hàng, quản lý và thống kê
    └── ClientMain
```

`server.repository` là tầng truy cập dữ liệu, nằm ngang cấp với `server.service`.
Không tạo thêm một tầng dao trùng vai trò. JPA là công nghệ truy cập dữ liệu, không thay thế
việc chia tầng entity/dao/service. Client chỉ phụ thuộc common và gửi yêu cầu;
không import server.entity/repository/service và không truy cập SQL Server trực tiếp.

## Dùng trong Eclipse

1. Chọn project, nhấn F5 để Refresh.
2. Chuột phải project → Maven → Update Project… (Alt+F5).
3. Kiểm tra Project → Properties → Java Compiler và Java Build Path.
   Bộ khung giữ Java 8 theo cấu hình project ban đầu; JDK chạy terminal có thể khác.
4. Chạy ServerMain và ClientMain bằng Run As → Java Application.
   Cấu hình biến QUANAO_DB_* cho ServerMain trước; xem docs/HUONG_DAN_CHAY.md.
5. Chạy ScaffoldSmokeTest trong src/test/java bằng Java Application.
   Đây là smoke test thủ công, chưa phải test JUnit do Maven tự chạy.

Không cần cấu hình một main class duy nhất: Eclipse có thể tạo hai Run Configuration.
Các cấu hình dùng classpath resources nên không phụ thuộc đường dẫn ổ Q:.
`target/` do Maven/Eclipse sinh, không sửa mã nguồn tại đó.

## Dependency đã cấu hình

- javax.persistence-api 2.2 + Hibernate 5.6.15.Final: bộ tương thích Java 8, import javax.persistence.*.
  Hibernate 5.6 đã hết hỗ trợ; nên nâng JDK 17/21 và provider trước khi dùng lâu dài.
- Microsoft SQL Server JDBC 13.2.1.jre8: driver runtime; không cần thêm jar thủ công.
- Jackson BOM 2.21.4, databind và jsr310: JSON và LocalDate/LocalDateTime.
  Khi dùng ObjectMapper cần đăng ký JavaTimeModule; không bật default typing cho dữ liệu mạng.
- SLF4J API + simple 2.0.20: log; không log mật khẩu/token.
- JUnit 4.13.2 và H2 2.1.214 (test scope). Test H2 không tác động SQL Server.
- Compiler plugin 3.14.1 và Surefire 3.2.5; cần Maven 3.6.3 trở lên.
- java.net.Socket/ServerSocket có sẵn trong JDK, không cần Spring Boot/RMI hay thư viện Socket.
- FlatLaf 3.7: giao diện Swing, không ảnh hưởng JPA hoặc giao thức Socket.

Trong Eclipse: F5 → Maven → Update Project (Alt+F5), sau đó Run As → Maven build,
Goals: test. DependencySmokeTest chỉ kiểm tra classpath và JSON, không kết nối database.
Kiểm thử thủ công ScaffoldSmokeTest chưa chuyển sang JUnit: Surefire bỏ qua lớp đó.
AuthIntegrationTest chạy Socket thật và JPA thật với H2 trong RAM.
Việc kết nối SQL Server vẫn cần cấu hình URL, tài khoản DB và schema đúng.

Nguồn đối chiếu: https://hibernate.org/orm/releases/5.6/,
https://github.com/microsoft/mssql-jdbc/releases/tag/v13.2.1,
https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.21.

## Quy tắc khi triển khai tiếp

- Hiện dùng Swing và javax.persistence; không trộn javax.persistence
  với jakarta.persistence. Khi nâng JDK, cập nhật pom.xml và cấu hình Eclipse đồng bộ.
- META-INF/persistence.xml đang dùng, liệt kê đầy đủ 12 entity.
  Production dùng validate, không tự sửa schema. File .example cũ không được nạp.
- SQL hiện tại là nguồn đối chiếu cho mapping. maCTHD là IDENTITY; phienBan dùng
  @Version; ChiTietHoaDon.thanhTien là cột tính toán, mapping chỉ đọc.
- Giữ BigDecimal cho tiền/tỷ lệ, LocalDate cho ngày sinh/khuyến mãi và LocalDateTime
  cho ngày lập hóa đơn. HoaDon dùng giá/tỷ lệ chốt tại thời điểm bán.
- Service sở hữu transaction: lưu hóa đơn, chi tiết và trừ tồn phải cùng transaction.
  EntityManager không được chia sẻ giữa các luồng xử lý client.
- Server kiểm tra phiên/quyền thật; ẩn nút ở client không phải biện pháp bảo mật.
- Không xóa cứng dữ liệu đã được tham chiếu. Thao tác bỏ mặt hàng/hủy chỉ áp dụng
  hóa đơn nháp trong phạm vi hiện tại, không sửa tùy ý hóa đơn đã thanh toán.
- Socket dùng frame 4 byte độ dài + JSON UTF-8, tối đa 1 MiB và có timeout.
  Không bật giải tuần tự Java ObjectInputStream cho dữ liệu mạng tùy ý.
- Thực hiện I/O ngoài luồng giao diện; không ghi mật khẩu hoặc session token vào log.
- Pbkdf2PasswordHasher dùng PBKDF2-HMAC-SHA256 qua JCE, 600.000 vòng và salt ngẫu nhiên.
  Thông tin database lấy từ môi trường hoặc cấu hình cục bộ không đưa vào Git.
- Utility tiền chỉ xử lý phép tính cơ bản, chưa thay thế chính sách hóa đơn và kiểm tra
  hiệu lực khuyến mãi/tồn kho. Các POJO setter chưa kiểm tra nghiệp vụ.

## Thứ tự triển khai gợi ý

Dữ liệu ảnh/Excel đã chuẩn hóa: xem data/README_DuLieu.md.
Có sql/01_ThemBangAnhSanPham.sql và sql/02_NhapDuLieuQuanAo.sql để chạy thủ công trong SSMS.
Hai script này không được tự chạy trong code; AnhSanPham đã ánh xạ JPA.

Các module cốt lõi đã có. Tiếp theo kiểm thử demo SQL Server và hoàn thiện tài liệu;
in hóa đơn/đổi trả/phiếu nhập kho chưa thuộc bản hiện tại.
Thống nhất hợp đồng common trước khi chia một bạn làm server và một bạn làm client.
