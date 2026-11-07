# Quản lý cửa hàng quần áo

Ứng dụng desktop Java Swing + FlatLaf, trao đổi dữ liệu qua TCP JSON.
Server dùng JPA/Hibernate và SQL Server. Client làm việc qua DTO và API.

Các form tạo sản phẩm, biến thể, loại hàng, thương hiệu, khách hàng, nhân viên,
nhà cung cấp và khuyến mãi tự sinh mã có tiền tố tương ứng. Mã được giữ cố định trong lần lưu.

## Mở workspace Eclipse

1. Chọn `C:\Users\KHOA\Desktop\QuanLyBanQuanAo` làm workspace.
2. Chọn **File → Import → General → Existing Projects into Workspace**. Dùng thư mục trên làm **Select root directory**, bật **Search for nested projects** nếu Eclipse cần, rồi chọn cả hai project. Bỏ chọn **Copy projects into workspace** vì chúng đã nằm trong workspace.
3. Chọn JDK 17. Trên project Server, dùng **Maven → Update Project** để nạp dependency.
4. Bật SQL Server. Mở `ServerMain.java` và chọn **Run As → Java Application**. Khi Console báo server đang chạy, mở `ClientMain.java` và chạy tương tự.
5. Đăng nhập bằng **`QUAN_LY` / `123456`**. File `application.local.properties` nằm cùng cấp với hai project.

Khi clone sang máy khác, sao chép `application.local.properties.example` thành
`application.local.properties` và điền thông tin SQL Server của máy đó. Nếu chưa có
database, làm theo [hướng dẫn tạo database](docs/DATABASE_MOI.md) trước khi chạy Server.

| Project | Loại | Lớp khởi chạy |
| --- | --- | --- |
| `QuanLyBanQuanAo_Server` | Maven Project | `server.app.ServerMain` |
| `QuanLyBanQuanAo_Client` | Java Project | `client.app.ClientMain` |

Chạy cả hai bằng launcher trên Windows.

```powershell
.\scripts\ChayUngDung.cmd
```

Launcher biên dịch Server, xuất JAR, biên dịch Client rồi chạy hai tiến trình.

## Hai project và thư viện Client

Server sở hữu source `server` và `common`. Client nhận DTO và giao thức từ `lib/server.jar` qua Java Build Path. Ảnh và dữ liệu hiển thị nằm trong `src` của Client.

```text
QuanLyBanQuanAo/
├── QuanLyBanQuanAo_Server/
│   ├── pom.xml
│   ├── src/main/java/{server,common}/
│   ├── src/main/resources/
│   ├── src/test/java/
│   └── target/server.jar
├── QuanLyBanQuanAo_Client/
│   ├── src/{client,config,data,images}/
│   ├── lib/server.jar
│   └── bin/
└── application.local.properties
```

`lib` đã có `server.jar`, FlatLaf và Jackson. Khi sửa source Server hoặc `common`, chạy **Run As → Maven build… → Goals `package`** trên Server. Maven tạo `target/server.jar` rồi sao chép bản mới vào `Client/lib`. Nhấn **F5** trên Client để Eclipse nhận bản JAR mới. Nếu dùng cache Maven và JDK trên máy, có thể chạy `scripts/XuatThuVienClient.cmd`.

## Chức năng hiện có

- Đăng nhập, phân quyền quản lý/bán hàng, thông tin cá nhân và đổi mật khẩu.
- Tra cứu sản phẩm, ảnh theo màu, danh mục, thương hiệu, biến thể và tồn kho.
- Hóa đơn nháp, chọn khách hàng, tính tiền, thu tiền mặt/chuyển khoản tại quầy.
- Quản lý nhân viên/tài khoản, khách hàng, khuyến mãi, nhà cung cấp và phiếu nhập.
- Lịch sử biến động kho và thống kê doanh thu.
- Menu ngang: Hệ thống, Danh mục, Cập nhật, Xử lý, Tìm kiếm, Báo biểu.

In/PDF/CSV, dashboard/cảnh báo tồn và audit/đặt lại mật khẩu chưa được triển khai.
Ảnh vẫn dùng bộ resource có sẵn: 255 ảnh sản phẩm và 12 ảnh banner/logo/thanh toán.

## Kiểm thử và biên dịch

Hai project dùng JDK 17 và giữ bytecode Java 8.

```powershell
mvn -f .\QuanLyBanQuanAo_Server\pom.xml package
```

Nếu chưa có Maven trên PATH nhưng các thư viện đã được tải trong `.m2`:

```powershell
.\scripts\KiemThuCucBo.ps1 -JdkDirectory 'C:\Program Files\Java\jdk-17'
```

Surefire chạy các lớp `KiemThu*.java`. `kiemthu.KiemTraNenTang` và
`kiemthu.KiemTraTaiNguyenAnh` là chương trình kiểm tra thủ công.
Test tích hợp tự động dùng H2 trong RAM; SQL Server được kiểm tra riêng.

Maven đã chạy **107/107 test** và xuất JAR vào `lib` của Client.

## Tài liệu

- [Cấu trúc và quy tắc mã nguồn](docs/CAU_TRUC_DU_AN.md)
- [Bảng đổi tên lớp/package](docs/BANG_DOI_TEN_LOP.md)
- [Cấu hình và chạy local](docs/CAU_HINH_LOCAL.md)
- [Hướng dẫn chạy chi tiết](docs/HUONG_DAN_CHAY.md)
- [Database mới và tài khoản](docs/DATABASE_MOI.md)
- [Giao diện và menu chức năng](docs/GIAO_DIEN_FLATLAF.md)
- [Bán hàng](docs/BAN_HANG.md), [lịch sử kho](docs/LICH_SU_KHO.md)

File `sql/08_TaoDatabaseMoiDayDu.sql` tạo database mới đầy đủ khi cần chuyển máy.
Database đang có sẵn trên máy nên không cần chạy lại SQL. Các script baseline/migration
và báo cáo trong `ltpt` được giữ để đối chiếu các mốc trước khi sắp xếp.
