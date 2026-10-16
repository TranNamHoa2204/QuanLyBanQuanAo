# Ứng dụng quản lý cửa hàng quần áo

Ứng dụng desktop phân tầng cho quản lý cửa hàng quần áo, dùng Java Swing + FlatLaf,
JPA/Hibernate, SQL Server và TCP Socket. Các package gốc: `client`, `common`, `server`;
groupId: `vn.edu.iuh`.

## Trạng thái hiện tại

- Giao thức TCP JSON dùng DTO `common`; JPA/Hibernate chỉ chạy ở server.
- Có đăng nhập, hồ sơ, đổi mật khẩu, đăng xuất, sản phẩm/biến thể, bán hàng,
  tra cứu hóa đơn, thanh toán tại quầy và lịch sử biến động kho.
- Backend cho khách hàng, nhân viên/tài khoản, khuyến mãi và thống kê có action,
  service, repository và test; API/màn hình Swing của các nhóm này còn rỗng hoặc
  chưa nối vào sidebar. Không xem backend đơn lẻ là chức năng hoàn chỉnh.
- Nếu tỷ lệ thuế/khuyến mãi đổi sau khi lập nháp, checkout hiện yêu cầu hủy nháp và
  lập lại; chưa có luồng S10 cập nhật nháp/preview/xác nhận tổng mới không cần hủy.
- FR11 nhà cung cấp/phiếu nhập, FR13 in/PDF/CSV, FR14 cảnh báo tồn/dashboard và
  FR15 audit/đặt lại mật khẩu chưa được triển khai. FR16 nằm ngoài phạm vi S27.
- Schema baseline có 12 bảng; ledger FR10 thêm bảng `LichSuKho`, nên schema hiện
  được source ánh xạ có 13 bảng logic sau migrations. SQL Server demo chưa được
  nghiệm thu; không dùng test H2 để tuyên bố SQL Server đã chạy.
- `AccountSetupMain` chỉ tạo tài khoản quản lý đầu tiên khi được chạy chủ động;
  không có tài khoản hoặc mật khẩu mặc định.
- Báo cáo S27: [Word](ltpt/BaoCao_S27_HoanThien.docx),
  [PDF](ltpt/BaoCao_S27_HoanThien.pdf), [Markdown](ltpt/BaoCao_S27_HoanThien.md).
  Báo cáo ghi ma trận FR/UC, sơ đồ use case/lớp/ERD, bằng chứng test và kịch bản demo.

Phần tra cứu sản phẩm đã có: lọc tên/loại/thương hiệu/màu/size, phân trang,
chi tiết biến thể, tồn kho và ảnh theo màu. Cả hai vai trò dùng được;
quản lý được xem cả hàng ngừng bán. Màn hình tra cứu này chỉ đọc, chưa có CRUD.
Quản lý ghi dữ liệu ở menu riêng: xem [docs/QUAN_LY_DANH_MUC_SAN_PHAM.md](docs/QUAN_LY_DANH_MUC_SAN_PHAM.md).
Luồng bán hàng nằm ở màn hình riêng; xem [docs/BAN_HANG.md](docs/BAN_HANG.md).
Xem [docs/TRA_CUU_SAN_PHAM.md](docs/TRA_CUU_SAN_PHAM.md) trước khi chạy bản cập nhật.

Hướng dẫn chạy chi tiết: [docs/HUONG_DAN_CHAY.md](docs/HUONG_DAN_CHAY.md).
Trạng thái FR/API/UI/test và giới hạn S27: [ltpt/BaoCao_S27_HoanThien.md](ltpt/BaoCao_S27_HoanThien.md).
Schema baseline và migration: [docs/SCHEMA_BASELINE_AND_MIGRATIONS.md](docs/SCHEMA_BASELINE_AND_MIGRATIONS.md).

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
│   ├── repository DAO/JPA repository theo nghiệp vụ
│   ├── dispatcher RequestDispatcher
│   ├── entity     entity nghiệp vụ được khai báo trong persistence-unit JPA
│   ├── network    SocketServer, ClientHandler
│   ├── persistence JpaManager
│   ├── security   PasswordHasher, PermissionChecker
│   ├── service    Các lớp nghiệp vụ
│   ├── session    SessionManager
│   └── ServerMain
└── client
    ├── api        DTO/action client; một số module quản trị còn là stub
    ├── config     ClientConfig
    ├── controller Xử lý sự kiện của các màn hình
    ├── network    SocketClient
    ├── session    ClientSession
    ├── ui         Swing cho tài khoản, sản phẩm, bán hàng, danh mục và lịch sử kho
    └── ClientMain
```

`server.repository` là tầng truy cập dữ liệu, nằm ngang cấp với `server.service`.
Không tạo thêm một tầng dao trùng vai trò. JPA là công nghệ truy cập dữ liệu, không thay thế
việc chia tầng entity/dao/service. Client chỉ phụ thuộc common và gửi yêu cầu;
không import server.entity/repository/service và không truy cập SQL Server trực tiếp.

## Dùng trong Eclipse

1. Chọn project, nhấn F5 để Refresh.
2. Chuột phải project → Maven → Update Project… (Alt+F5).
3. Chọn JDK 17 (hoặc JDK 9 trở lên) trong Project → Properties → Java Build Path.
   `pom.xml` giữ bytecode Java 8 bằng Maven Compiler Plugin `release=8`.
4. Chạy ServerMain và ClientMain bằng Run As → Java Application.
   Cấu hình biến QUANAO_DB_* cho ServerMain trước; xem docs/HUONG_DAN_CHAY.md.
5. Test hiện có: `mvn -Djava.awt.headless=true test`; ScaffoldSmokeTest được gọi
   riêng bởi script fallback, không phải test JUnit Surefire.

Kiểm thử JUnit chuẩn: `mvn -Djava.awt.headless=true test` (Maven 3.6.3+).
Nếu không có Maven trên PATH nhưng dependency đã có trong cache, dùng
`scripts/Test-Local.ps1`; hướng dẫn và giới hạn cache được ghi tại
docs/HUONG_DAN_CHAY.md. `target/` đang được Git theo dõi, vì vậy tránh `mvn clean`
trong checkout có thay đổi build output cần giữ.

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
Goals: `-Djava.awt.headless=true test`. DependencySmokeTest chỉ kiểm tra classpath và JSON, không kết nối database.
Kiểm thử thủ công ScaffoldSmokeTest chưa chuyển sang JUnit: Surefire bỏ qua lớp đó.
Trong lần cập nhật S27, script fallback chạy 88 JUnit test và ScaffoldSmokeTest đạt.
Test tích hợp chạy Socket/JPA với H2 trong RAM; chưa có xác minh SQL Server demo.
Việc kết nối SQL Server vẫn cần URL, tài khoản DB cục bộ và schema đúng.

Nguồn đối chiếu: https://hibernate.org/orm/releases/5.6/,
https://github.com/microsoft/mssql-jdbc/releases/tag/v13.2.1,
https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.21.

## Quy tắc khi triển khai tiếp

- Hiện dùng Swing và javax.persistence; không trộn javax.persistence
  với jakarta.persistence. Khi nâng JDK, cập nhật pom.xml và cấu hình Eclipse đồng bộ.
- `META-INF/persistence.xml` hiện ánh xạ 13 entity logic, gồm ledger `LichSuKho`.
  Baseline SQL trước ledger là 12 bảng; không dùng số baseline để mô tả schema sau mở rộng.
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
- Quy tắc hóa đơn và tồn nằm ở server service; DTO/UI không phải nguồn quyết định tiền.

## Tình trạng và giới hạn cần biết

Dữ liệu ảnh/Excel đã chuẩn hóa: xem data/README_DuLieu.md.
Có sql/01_ThemBangAnhSanPham.sql và sql/02_NhapDuLieuQuanAo.sql để chạy thủ công trong SSMS.
`sql/04_HoaDonSnapshotVaThoiDiemThanhToan.sql`, `sql/05_LichSuBienDongKho.sql` và
`sql/06_ThuTienTaiQuay.sql` là migration tăng dần cho snapshot/thời điểm, ledger và thu
tiền tại quầy. Chỉ chạy theo thứ tự trong `docs/HUONG_DAN_CHAY.md` trên database demo đã
xác định; ứng dụng không tự chạy SQL và JPA vẫn giữ `validate`.

FR02, FR07, FR08 và FR09 hiện thiếu giao diện/API client hoàn chỉnh; FR11, FR13, FR14 và
FR15 chưa có triển khai tương ứng. Chi tiết theo FR/UC và kịch bản demo giới hạn nằm trong
báo cáo S27. Tài liệu gốc Chương 1–3 trong `ltpt` được giữ nguyên.
