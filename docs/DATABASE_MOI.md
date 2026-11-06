# Database mới đầy đủ — 05/11/2026

Database dùng cho ứng dụng: **QuanLyCuaHangQuanAo**, SQL Server `localhost:1433`.

Đã tạo và kiểm tra trực tiếp trên SQL Server. File
[`sql/08_TaoDatabaseMoiDayDu.sql`](../sql/08_TaoDatabaseMoiDayDu.sql) chứa toàn bộ schema,
khóa, index, CHECK và dữ liệu; không phụ thuộc database cũ và không cần chạy thêm
baseline hoặc migration 01–07. Chạy toàn bộ file UTF-8 bằng SSMS (F5).

File dừng ngay nếu database cùng tên đã tồn tại. Không cần chạy lại trên máy hiện tại:
database mới đã được tạo sẵn. File tự chọn tên vật lý MDF/LDF riêng để tránh trùng file
của một database cũ đã đổi tên.

## Tài khoản đăng nhập

| Tài khoản | Mật khẩu demo | Vai trò |
| --- | --- | --- |
| `demo.quanly` | `123456` | Nhân viên quản lý |
| `demo.banhang` | `123456` | Nhân viên bán hàng |
| `demo.banhang2` | `123456` | Nhân viên bán hàng |
| `QUAN_LY` | `123456` | Nhân viên quản lý (tài khoản dùng lại) |

Tài khoản và nhân viên cũ được sao chép. Theo yêu cầu, mật khẩu cả 4 tài khoản trên
database mới đã được đặt thành `123456`, băm riêng từng tài khoản và kiểm tra đăng nhập
thành công. File SQL cũng đã đồng bộ hash mới. Bản database cũ không đổi mật khẩu.
Database chỉ lưu **TaiKhoan.matKhauHash VARCHAR(255) NOT NULL**, không lưu mật khẩu rõ.
Chuỗi hash có dạng `pbkdf2-sha256$600000$salt$hash`, salt ngẫu nhiên riêng mỗi tài khoản.
App dùng `BoBamMatKhauPbkdf2` để kiểm tra mật khẩu. Entity `TaiKhoan` ánh xạ trường
Java `passwordHash` vào cột SQL `matKhauHash`; API không trả hash về client.

## 17 bảng và dữ liệu ban đầu

| Bảng | Số bản ghi | Nội dung |
| --- | ---: | --- |
| LoaiSanPham | 6 | Dùng lại dữ liệu |
| ThuongHieu | 3 | Dùng lại dữ liệu |
| SanPham | 40 | Dùng lại mã, tên, giá và trạng thái |
| BienThe | 152 | Dùng lại màu, size, số lượng tồn |
| AnhSanPham | 255 | Dùng lại đường dẫn ảnh |
| NhanVien | 4 | 1 nhân viên cũ và 3 nhân viên demo |
| TaiKhoan | 4 | 1 tài khoản cũ và 3 tài khoản demo, đều có hash |
| KhachHang | 12 | Dữ liệu demo bổ sung |
| KhuyenMai | 3 | Hai chương trình hiệu lực 2026–2027, một đã kết thúc |
| Thue | 1 | Cấu hình demo 0% có sẵn |
| HoaDon | 15 | 12 đã thanh toán, 2 nháp, 1 đã hủy |
| ChiTietHoaDon | 30 | Hai dòng hàng mỗi hóa đơn |
| LichSuKho | 188 | 152 dòng cũ, 12 dòng nhập, 24 dòng bán |
| NhaCungCap | 3 | Dữ liệu nhà cung cấp demo |
| PhieuNhap | 5 | 3 đã xác nhận, 1 nháp, 1 đã hủy |
| ChiTietPhieuNhap | 16 | Dòng hàng của phiếu nhập |
| PhieuNhapThaoTac | 25 | Dấu vết chống lặp thao tác do service tạo |

Các bản ghi bổ sung dùng mã `DEMO` và thông tin liên hệ `example.com` để dễ nhận biết.
Hóa đơn mẫu phát sinh ngày 02–05/11/2026, có cả tiền mặt và chuyển khoản. Phiếu nhập mẫu
ngày 01/11/2026. Tổng số lượng nhập đã xác nhận bằng số lượng bán mẫu, nên số lượng tồn
cuối cùng của cả 152 biến thể bằng dữ liệu gốc.

Hóa đơn, phiếu nhập, ledger và operation fingerprint được tạo bằng service thật của app,
bao gồm chốt giá, thu tiền, kiểm tra phiên bản và ghi lịch sử trong transaction.
Các mốc thời gian của dữ liệu demo được sắp theo đúng thứ tự phát sinh.

## Ảnh dùng lại

Ảnh nằm tại `QuanLyBanQuanAo_Client/src/images/`. SQL lưu đường dẫn trong `AnhSanPham.duongDan`.
Đã kiểm tra 255/255 đường dẫn ảnh sản phẩm tồn tại. Bộ resource tổng cộng 267 ảnh
(255 ảnh sản phẩm và 12 ảnh banner/logo/thanh toán) được giữ nguyên.

## Database cũ

Bản cũ được giữ dưới tên **QuanLyCuaHangQuanAo_Cu_20261105**. Đã đối chiếu SHA-256 dữ liệu
của từng bảng trước và sau khi tạo bản mới, không thay đổi dữ liệu của bản cũ.

## Kiểm tra

- Hibernate `validate` với SQL Server thật: đủ 17 entity/bảng và đúng kiểu cột.
- File SQL được chạy thực tế; dữ liệu cả 17 bảng khớp bản đã kiểm tra.
- Khóa ngoại và CHECK đều được bật và trusted.
- Tổng tiền hóa đơn/phiếu nhập khớp chi tiết; ledger liên tục và khớp tồn hiện tại.
- Đăng nhập bằng PBKDF2 và tính thống kê bằng service trên SQL Server thật đạt.
- Kiểm tra TCP cổng 8888: đăng nhập cả 3 tài khoản, từ chối mật khẩu sai, phân quyền,
  tra cứu các module và chi tiết hóa đơn/phiếu nhập đều đạt; server kiểm tra đã đóng.
- Biên dịch mới toàn bộ mã nguồn: **98/98 JUnit test đạt**, scaffold smoke đạt.
- Chạy lại file SQL khi database tồn tại bị từ chối, dữ liệu không thay đổi.
- Tổng 12 hóa đơn đã thanh toán: **3.397.200 VNĐ**.

Cấu hình mặc định của app vẫn dùng database `QuanLyCuaHangQuanAo`. Chạy
`scripts/ChayUngDung.cmd` để mở app và đăng nhập bằng một tài khoản ở trên.
Kết nối SQL Server và cổng server/client được cấu hình trong `application.local.properties`
tại thư mục gốc; xem [CAU_HINH_LOCAL.md](CAU_HINH_LOCAL.md).

Các script 01–07 và baseline cũ được giữ làm lịch sử cấu trúc trước đó. Với database
mới dùng file 08; baseline cũ còn tên cột `passwordHash` nên không thay thế file 08.
