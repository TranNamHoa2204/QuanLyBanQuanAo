# Chạy ứng dụng trong Eclipse

Chọn `C:\Users\KHOA\Desktop\QuanLyBanQuanAo` làm workspace và import cả hai
project theo [README](../README.md#mở-workspace-eclipse). Server dùng Maven. Client
dùng Java Build Path với các JAR trong `QuanLyBanQuanAo_Client/lib`.

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
- Chạy `mvn -f QuanLyBanQuanAo_Server/pom.xml test` từ workspace để kiểm thử bằng H2.
  Goal `package` xuất thêm `server.jar` và sao chép vào `QuanLyBanQuanAo_Client/lib`.
- Maven tự giải quyết dependency theo `pom.xml`; mặc định lưu cache ở `%USERPROFILE%\.m2\repository` (có thể đổi bằng Maven `settings.xml`) và tải dependency còn thiếu nếu repository truy cập được. Nếu Maven không có trên `PATH` nhưng cache đã đủ, có thể dùng script dự phòng:

  ```powershell
  # Đặt JAVA_HOME thành thư mục JDK 9+ đã cài trên máy trước khi chạy.
  .\scripts\KiemThuCucBo.ps1 -JdkDirectory $env:JAVA_HOME
  ```

  Nếu Maven dùng local repository riêng, thêm `-RepositoryDirectory '<thư-mục-repository>'`. Script dự phòng không tải hoặc tìm dependency thay Maven: nó chỉ đọc các JAR phiên bản cố định đã có trong cache, biên dịch vào thư mục tạm mới, tự tìm các lớp JUnit có `@Test`, rồi chạy thêm `KiemTraNenTang`. Thiếu JAR sẽ báo rõ tên dependency cần bổ sung bằng Maven/Eclipse Update Project.
- Kết quả biên dịch nằm trong `QuanLyBanQuanAo_Server/target` và `QuanLyBanQuanAo_Client/bin`.

## 1. Phạm vi đã làm

Đăng nhập → xem thông tin cá nhân → đổi mật khẩu → đăng xuất. Có hai vai trò
`NHAN_VIEN_BAN_HANG` và `NHAN_VIEN_QUAN_LY`, lấy từ database. Các luồng Swing đã nối
gồm tra cứu sản phẩm, quản lý danh mục/biến thể, bán hàng/hóa đơn và lịch sử kho; xem
`TRA_CUU_SAN_PHAM.md`, `QUAN_LY_DANH_MUC_SAN_PHAM.md`, `BAN_HANG.md` và
`LICH_SU_KHO.md`.

Nhân viên/tài khoản, khách hàng, khuyến mãi, thống kê, nhà cung cấp và phiếu nhập đã có
API/màn hình Swing thật dưới menu quản lý. Phiếu nhập tăng tồn khi xác nhận và ghi lịch sử
kho trong cùng transaction. FR13 in/PDF/CSV, FR14 cảnh báo tồn/dashboard và FR15 audit/
đặt lại mật khẩu vẫn còn thiếu; báo cáo S27 cần được đồng bộ trước khi bàn giao.

Luồng thực tế:

Client Swing → controller → GoiDichVuXacThuc → KetNoiMayChu → DieuPhoiYeuCau
→ DichVuXacThuc → LuuTruTaiKhoanJpa → JPA/Hibernate → SQL Server.

EntityManager riêng cho từng transaction; chỉ DTO được trả về client.
Server kiểm tra tài khoản và nhân viên đang hoạt động mỗi lần đọc hồ sơ/đổi mật khẩu.
Không lấy mã nhân viên hoặc vai trò do client tự gửi làm nguồn phân quyền.

## 2. Chuẩn bị database

Server cần database demo riêng có schema khớp các entity trong `META-INF/persistence.xml`.
Baseline trước ledger có 12 bảng; sau migration 07 cho FR11 có 17 bảng logic.
Migrations 04 và 06 thêm cột vào `HoaDon`; migration 07 thêm nhà cung cấp, phiếu nhập,
chi tiết phiếu và ledger idempotency.
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
Không đổi matKhauHash thành mật khẩu rõ để thử đăng nhập.

Đối chiếu baseline, schema sau FR10, thứ tự tạo database demo và giới hạn migration:
[SCHEMA_BASELINE_AND_MIGRATIONS.md](SCHEMA_BASELINE_AND_MIGRATIONS.md).

Trong SSMS kiểm tra server/instance, tên database và SQL login được cấp quyền phù hợp.
URL dùng cổng TCP: SQL Server phải bật TCP/IP và lắng nghe tại cổng đó.
Không mặc định mọi instance đều dùng cổng 1433.

## 3. Cấu hình SQL Server local demo

Cấu hình kết nối nằm trong `application.local.properties` tại thư mục gốc dự án.
File trên máy đã được điền SQL Server/database hiện tại và cổng app 8888.
File được ưu tiên; `QUANAO_DB_*` chỉ bù khóa chưa khai báo. Client chỉ dùng địa chỉ/cổng server.
Xem `docs/CAU_HINH_LOCAL.md` (hoặc `CAU_HINH_LOCAL.md` trong thư mục docs).

Trong Eclipse, chọn JDK 17 (hoặc JDK 9+) rồi chạy Maven → Update Project.
Có thể chạy `server.app.ServerMain` trực tiếp; không cần nhập biến môi trường cho demo.
`persistence.xml` giữ `hbm2ddl.auto=validate`: Hibernate kiểm tra schema,
không tự tạo, xóa hoặc sửa bảng. Database và migrations phải được chuẩn bị trước.

## 4. Tạo tài khoản quản lý đầu tiên nếu database chưa có tài khoản

Database hiện tại đã có tài khoản: **QUAN_LY / 123456**, không cần tạo lại.
Các khóa `setup.*` trong `application.local.properties` chỉ dành cho công cụ setup
trên database chưa có tài khoản; server/client không tự chạy công cụ này.

Chỉ với database demo rỗng, chạy:

    .\scripts\ChayUngDung.cmd -CreateInitialAccount

Xác nhận bằng `CREATE-MANAGER` khi được hỏi. Công cụ tạo `NhanVien` và `TaiKhoan`
trong cùng transaction, vai trò quản lý, mật khẩu băm PBKDF2 và isolation
SERIALIZABLE. Nếu DB đã có tài khoản, mã nhân viên đã tồn tại hoặc số điện thoại
vi phạm unique, thao tác thất bại và không ghi đè hồ sơ. Không chạy lặp công cụ
để thử lại nếu chưa kiểm tra trạng thái DB.

`TaoTaiKhoanQuanLy` chỉ là công cụ setup local, không được gọi qua Socket.
Nếu DB đã có tài khoản hợp lệ, bỏ qua bước tạo ban đầu và đăng nhập bằng tài khoản đó.

## 5. Chạy và thử

Database demo và migrations phải sẵn sàng trước. Dùng:

    .\scripts\ChayUngDung.cmd

Launcher biên dịch source vào thư mục tạm, mở server rồi client. Nhập tài khoản
ứng dụng ở cửa sổ Swing. Trước lần chạy thường đầu tiên, nếu DB rỗng, thực hiện
bước tạo quản lý ở mục 4. `.cmd` gọi PowerShell với policy chỉ cho tiến trình đó,
nên không cần đổi execution policy toàn hệ thống.

Trong Eclipse vẫn có thể chạy `server.app.ServerMain` rồi `client.app.ClientMain` riêng.
Client đọc `server.host`/`server.port` trong `application.local.properties`,
kết nối `localhost:8888` theo cấu hình hiện tại.

Sau khi đăng nhập, chọn **Hệ thống → Thông tin cá nhân** để mở hồ sơ. Thử đổi mật khẩu cũ sai
và đúng, rồi đăng xuất để kiểm tra thu hồi token.

ManHinhChinh có Tra cứu sản phẩm, Bán hàng / Hóa đơn, Danh mục & sản phẩm và Lịch sử kho.
Xem `BAN_HANG.md` để thử nháp, thêm/đổi/bỏ hàng, hủy nháp và thanh toán/trừ tồn;
`LICH_SU_KHO.md` để thử truy vấn ledger và điều chỉnh tồn.
Giao diện đã dùng Swing + FlatLaf: xem GIAO_DIEN_FLATLAF.md.
FlatLaf được nạp từ `lib` của Client. Sau khi xuất lại JAR, nhấn F5 trên Client.
I/O và phép chờ server chạy bằng SwingWorker, không chặn Event Dispatch Thread.

Sidebar quản lý hiện có màn hình Nhân viên, Khuyến mãi và Thống kê với dữ liệu qua
Socket; thao tác quản trị được kiểm tra lại ở server. Màn hình khách hàng chưa được
nối trực tiếp với bộ chọn khách khi bán hàng; quản lý hồ sơ khách hàng có màn hình riêng.

Nếu cần đổi kết nối DB, sửa `db.url`, `db.username`, `db.password` trong
`application.local.properties` rồi chạy lại server. Nếu báo thiếu
`config/server.properties`, kiểm tra `src/main/resources` của Server và chạy Maven Update Project.
Nếu thiếu `config/client.properties`, kiểm tra `src/config` trong Client. Lỗi địa chỉ client
thì kiểm tra `server.host`/`server.port`; lỗi kết nối bị từ chối thường có nghĩa server
chưa chạy, đang nghe host/port khác hoặc bị firewall chặn. Nếu server dừng ở bước JPA
validate, kiểm tra URL trỏ đúng database demo và schema thay vì nới validation.

## 6. Kiểm thử tự động

Eclipse: Run As → Maven build → Goals: `-Djava.awt.headless=true test`.
Maven tự tìm mọi lớp JUnit. Trong lần cập nhật S27, script fallback biên dịch source
hiện tại và chạy 88 test về dependency/giao thức, đăng nhập, danh mục, sản phẩm, nhân viên,
khách hàng, khuyến mãi, hóa đơn, thống kê, Socket và render UI headless.
`KiemTraNenTang` là smoke test thủ công nên Surefire loại khỏi JUnit.

Các test tích hợp dùng JPA/Hibernate, Socket TCP và H2 trong bộ nhớ (`create-drop`).
Chúng không kết nối SQL Server, không cần biến `QUANAO_DB_*` và không xác minh schema
trên một SQL Server thật. Cấu hình ứng dụng vẫn giữ `hbm2ddl.auto=validate`.

Khi cần chạy không qua Maven, dùng lệnh PowerShell ở phần chuẩn bị toolchain; script
`KiemThuCucBo.ps1` dùng cache JAR có sẵn và không thay thế Maven trong giải dependency,
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
FR07–FR11 và thống kê hiện có API/màn hình, gồm nhà cung cấp và phiếu nhập. FR13 in/PDF/CSV,
FR14 dashboard/cảnh báo và FR15 audit/reset chưa hoàn tất. Xem `docs/tien-do.md` để biết
phần đã sửa, test và gate migration.
SQL Server demo và UI tương tác chưa được nghiệm thu. Không xem kết quả H2 là xác minh SQL Server.
