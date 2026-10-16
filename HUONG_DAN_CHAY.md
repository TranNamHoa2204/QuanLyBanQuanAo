# Chạy ứng dụng trong Eclipse

## Chuẩn bị JDK, Maven và kiểm thử

- Dự án giữ baseline bytecode Java 8 (`maven.compiler.source/target=1.8` và Maven Compiler Plugin `release=8`). JDK 17 đã biên dịch được toàn bộ source; file `.class` có major version 52. Dùng JDK 9 trở lên để hỗ trợ `--release 8`; không cần đổi dependency hoặc đổi `javax.persistence`.
- Cài Maven 3.6.3 trở lên và kiểm tra `mvn -version` để chắc Maven đang chạy bằng JDK dự định. Nếu `JAVA_HOME`/`MAVEN_HOME` chưa cấu hình trong PowerShell, đặt theo thư mục cài trên máy rồi thêm `bin` vào `PATH`:

  ```powershell
  $env:JAVA_HOME = '<thư-mục-JDK-9-trở-lên>'
  $env:MAVEN_HOME = '<thư-mục-Apache-Maven-3.6.3-trở-lên>'
  $env:Path = "$env:MAVEN_HOME\bin;$env:JAVA_HOME\bin;$env:Path"
  java -version
  mvn -version
  ```

  Trong Eclipse, cấu hình JDK ở Preferences → Java → Installed JREs, sau đó dùng Maven → Update Project. Maven có thể là bản cài riêng hoặc bản tích hợp của Eclipse; lệnh `mvn` trong PowerShell cần Maven nằm trên `PATH`.
- Lệnh kiểm thử chuẩn tại thư mục dự án: `mvn -Djava.awt.headless=true test`. Lệnh này để Maven biên dịch theo Java 8, chạy toàn bộ JUnit hiện có và dùng H2 trong bộ nhớ. Không cần SQL Server hoặc biến `QUANAO_DB_*` cho bộ test này.
- Maven tự giải quyết dependency theo `pom.xml`; mặc định lưu cache ở `%USERPROFILE%\.m2\repository` (có thể đổi bằng Maven `settings.xml`) và tải dependency còn thiếu nếu repository truy cập được. Nếu Maven không có trên `PATH` nhưng cache đã đủ, có thể dùng script dự phòng:

  ```powershell
  # Đặt JAVA_HOME thành thư mục JDK 9+ đã cài trên máy trước khi chạy.
  .\scripts\Test-Local.ps1 -JdkDirectory $env:JAVA_HOME
  ```

  Nếu Maven dùng local repository riêng, thêm `-RepositoryDirectory '<thư-mục-repository>'`. Script dự phòng không tải hoặc tìm dependency thay Maven: nó chỉ đọc các JAR phiên bản cố định đã có trong cache, biên dịch vào thư mục tạm mới, tự tìm các lớp JUnit có `@Test`, rồi chạy thêm `ScaffoldSmokeTest`. Thiếu JAR sẽ báo rõ tên dependency cần bổ sung bằng Maven/Eclipse Update Project.
- `target/` hiện được Git theo dõi trong checkout này. Tránh `mvn clean` và tránh build trực tiếp khi cần giữ các thay đổi đang có trong `target`; chạy build/test trong bản sao làm việc riêng. Kết quả S01 bên dưới được chạy trong bản sao tạm ngoài repo.

## 1. Phạm vi đã làm

Đăng nhập → xem thông tin cá nhân → đổi mật khẩu → đăng xuất. Có hai vai trò
`NHAN_VIEN_BAN_HANG` và `NHAN_VIEN_QUAN_LY`, lấy từ database. Các luồng Swing đã nối
gồm tra cứu sản phẩm, quản lý danh mục/biến thể, bán hàng/hóa đơn và lịch sử kho; xem
`TRA_CUU_SAN_PHAM.md`, `QUAN_LY_DANH_MUC_SAN_PHAM.md`, `BAN_HANG.md` và
`LICH_SU_KHO.md`.

Backend khách hàng, nhân viên/tài khoản, khuyến mãi và thống kê có action/service/test,
nhưng client API/form tương ứng còn rỗng hoặc chưa được nối vào sidebar. Không chạy được
các chức năng đó từ giao diện hiện tại. FR11 phiếu nhập, FR13 in/xuất, FR14 dashboard/
cảnh báo tồn và FR15 audit/reset chưa được triển khai. Ma trận đầy đủ nằm trong
`ltpt/BaoCao_S27_HoanThien.md`.

Luồng thực tế:

Client Swing → controller → AuthApi → SocketClient → RequestDispatcher
→ AuthService → JpaTaiKhoanDao → JPA/Hibernate → SQL Server.

EntityManager riêng cho từng transaction; chỉ DTO được trả về client.
Server kiểm tra tài khoản và nhân viên đang hoạt động mỗi lần đọc hồ sơ/đổi mật khẩu.
Không lấy mã nhân viên hoặc vai trò do client tự gửi làm nguồn phân quyền.

## 2. Chuẩn bị database

Server cần database demo riêng có schema khớp các entity trong `META-INF/persistence.xml`.
Baseline trước ledger có 12 bảng; migration 05 thêm `LichSuKho`, nên schema sau mở rộng
có 13 bảng logic. Migrations 04 và 06 chỉ thêm cột vào `HoaDon`.
Nếu khởi tạo mới, script `sql/TaoCSDL_QuanLyCuaHangQuanAo_SQLServer.sql` chỉ dành cho
database trống và tự từ chối nếu đã có bảng; sau đó `sql/01_ThemBangAnhSanPham.sql`
bổ sung bảng ảnh, `sql/02_NhapDuLieuQuanAo.sql` thêm dữ liệu sản phẩm mẫu. Trên đúng
database demo chạy thêm `sql/04_HoaDonSnapshotVaThoiDiemThanhToan.sql` trước khi mở
server; migration 04 thêm cột nullable, kiểm tra schema khi chạy lại và không backfill.
Script `sql/03_ThueDemo_0PhanTram.sql` chỉ là cấu hình thuế 0% cho demo học tập, không dùng
để kết luận nghĩa vụ thuế thực tế. Chỉ xem xét các script này cho database demo được
xác định rõ; không chạy script khởi tạo lên database có dữ liệu cần giữ.

Nếu database đã tồn tại, không chạy lại script khởi tạo. `persistence.xml` dùng
`hbm2ddl.auto=validate`: Hibernate kiểm tra mapping khi server mở kết nối, nhưng không
tạo, sửa hoặc xóa bảng. Sai schema hoặc thiếu bảng/cột sẽ làm server dừng trước khi
Socket sẵn sàng. Không đổi `validate` để né lỗi schema.
Không đổi passwordHash thành mật khẩu rõ để thử đăng nhập.

Đối chiếu baseline, schema sau FR10, thứ tự tạo database demo và giới hạn migration:
[SCHEMA_BASELINE_AND_MIGRATIONS.md](docs/SCHEMA_BASELINE_AND_MIGRATIONS.md).

Trong SSMS kiểm tra server/instance, tên database và SQL login được cấp quyền phù hợp.
URL dùng cổng TCP: SQL Server phải bật TCP/IP và lắng nghe tại cổng đó.
Không mặc định mọi instance đều dùng cổng 1433.

## 3. Refresh Maven và cấu hình server

Trong Eclipse:

1. F5 → Maven → Update Project (Alt+F5).
2. Chọn JDK 17 (hoặc JDK 9+) trong Java Build Path; Maven vẫn sinh bytecode Java 8.
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
Persistence unit ánh xạ NhanVien, TaiKhoan, SanPham, BienThe, AnhSanPham, LoaiSanPham,
ThuongHieu, HoaDon, ChiTietHoaDon, KhachHang, KhuyenMai và Thue.

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

Thứ tự chạy là database demo đã sẵn sàng → `server.ServerMain` → `client.ClientMain`.
Trong Eclipse tạo hai Run Configuration kiểu Java Application:

1. Chạy `server.ServerMain` sau khi cấu hình biến DB ở mục 3. Server phải báo đang
   nghe tại `127.0.0.1:8888`.
2. Chạy `client.ClientMain`. Client đọc `config/client.properties`, kết nối tới
   `localhost:8888` mặc định và mở cửa sổ Swing đăng nhập; client không cần biến DB.
3. Đăng nhập bằng tài khoản vừa tạo.
4. Bấm Thông tin cá nhân → Tải thông tin.
5. Thử mật khẩu cũ sai khi đổi mật khẩu: phải báo lỗi và giữ nguyên dữ liệu.
6. Đổi bằng mật khẩu cũ đúng: trở về đăng nhập; mật khẩu mới dùng được, mật khẩu cũ không.
7. Đăng xuất: thu hồi token ở server và trở về đăng nhập.

MainForm có Tra cứu sản phẩm, Bán hàng / Hóa đơn, Danh mục & sản phẩm và Lịch sử kho.
Xem `BAN_HANG.md` để thử nháp, thêm/đổi/bỏ hàng, hủy nháp và thanh toán/trừ tồn;
`LICH_SU_KHO.md` để thử truy vấn ledger và điều chỉnh tồn.
Giao diện đã dùng Swing + FlatLaf: xem GIAO_DIEN_FLATLAF.md.
Sau khi Refresh phải Maven → Update Project để có dependency FlatLaf trước khi chạy ClientMain.
I/O và phép chờ server chạy bằng SwingWorker, không chặn Event Dispatch Thread.

Sidebar hiện vẫn có mục “Sắp có” cho Nhân viên, Khuyến mãi và Thống kê. Các lớp
`NhanVienForm`, `KhuyenMaiForm`, `ThongKeForm`, `KhachHangForm` cùng API/controller liên
quan chưa phải UI hoạt động; không dùng các mục đó trong demo giao diện.

Nếu báo thiếu `QUANAO_DB_URL`, `QUANAO_DB_USER` hoặc `QUANAO_DB_PASSWORD`, thêm đúng
biến được nêu trong thông báo vào Run Configuration của server. Nếu báo thiếu
`config/server.properties` hoặc `config/client.properties`, kiểm tra `src/main/resources`
đang nằm trên Maven/Eclipse build path rồi chạy Maven Update Project. Lỗi địa chỉ client
thì kiểm tra `server.host`/`server.port`; lỗi kết nối bị từ chối thường có nghĩa server
chưa chạy, đang nghe host/port khác hoặc bị firewall chặn. Nếu server dừng ở bước JPA
validate, kiểm tra URL trỏ đúng database demo và schema thay vì nới validation.

## 6. Kiểm thử tự động

Eclipse: Run As → Maven build → Goals: `-Djava.awt.headless=true test`.
Maven tự tìm mọi lớp JUnit. Trong lần cập nhật S27, script fallback biên dịch source
hiện tại và chạy 88 test về dependency/giao thức, đăng nhập, danh mục, sản phẩm, nhân viên,
khách hàng, khuyến mãi, hóa đơn, thống kê, Socket và render UI headless.
`ScaffoldSmokeTest` là smoke test thủ công nên Surefire loại khỏi JUnit.

Các test tích hợp dùng JPA/Hibernate, Socket TCP và H2 trong bộ nhớ (`create-drop`).
Chúng không kết nối SQL Server, không cần biến `QUANAO_DB_*` và không xác minh schema
trên một SQL Server thật. Cấu hình ứng dụng vẫn giữ `hbm2ddl.auto=validate`.

Khi cần chạy không qua Maven, dùng lệnh PowerShell ở phần chuẩn bị toolchain; script
`Test-Local.ps1` dùng cache JAR có sẵn và không thay thế Maven trong giải dependency,
đóng gói hoặc các lifecycle goal khác.

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

## 8. Trạng thái chức năng theo code

Tra cứu sản phẩm/biến thể/ảnh đã triển khai; xem TRA_CUU_SAN_PHAM.md.
Hóa đơn, thanh toán và trừ tồn kho đã triển khai; xem BAN_HANG.md.
Quản lý danh mục/sản phẩm đã có; xem QUAN_LY_DANH_MUC_SAN_PHAM.md.
Backend FR02/FR07/FR08/FR09 đã có nhưng thiếu API/form Swing hoàn chỉnh. FR11, FR13,
FR14 và FR15 chưa được triển khai; báo cáo S27 có ma trận FR/UC và kịch bản demo giới hạn.
SQL Server demo và UI tương tác chưa được nghiệm thu. Không xem kết quả H2 là xác minh SQL Server.
