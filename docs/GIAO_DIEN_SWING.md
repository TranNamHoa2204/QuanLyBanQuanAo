# Giao diện Java Swing cổ điển

## Chạy trên Eclipse

1. Import hai project theo hướng dẫn ở README.
2. Kiểm tra **Java Build Path → Libraries** của Client có `lib/server.jar` và Jackson.
3. Chạy `server.app.ServerMain` trong Server, sau đó `client.app.ClientMain` trong Client.
4. Khi xuất lại thư viện bằng Maven `package` của Server, nhấn F5 trên Client.

Java 8 được giữ nguyên. Không cần WindowBuilder, JavaFX, Spring Boot hoặc
thêm dependency Socket. Không chạy SQL mới cho thay đổi giao diện này.

## Bố cục đã có

- Đăng nhập: hai cột, phần nhận diện cửa hàng và form tài khoản; Enter để đăng nhập.
  Mật khẩu được che mặc định; có nút hiện/ẩn khi nhập.
- Màn hình chính: thanh menu ngang theo nhóm chức năng, menu con, khu vực tài khoản,
  vai trò, ngày hiện tại và trang chủ có các lối tắt công việc.
  THREAD là tên nhận diện minh họa cho giao diện, có thể đổi trong KhungUngDung
  và KhungDangNhap; không phải tên thương hiệu từ dữ liệu sản phẩm.
- Sản phẩm: thẻ bộ lọc, bảng phân trang, trạng thái dữ liệu; giữ tra cứu,
  sắp xếp trong trang, xem chi tiết, chọn màu để xem ảnh.
- Bán hàng: phần thông tin hóa đơn, chọn biến thể, bảng hàng và tóm tắt thanh toán.
  Chuyển về Trang chủ/Sản phẩm không hủy nháp hay mất trạng thái panel bán hàng.
  Cửa sổ nhỏ có thanh cuộn để không che các thao tác ở cuối màn hình.
- Hồ sơ: các trường chỉ đọc, nút tải thông tin theo tài khoản đang đăng nhập.
- Đổi mật khẩu: form riêng, nhập lại mật khẩu mới, giữ việc thu hồi phiên sau khi đổi.

Danh mục & sản phẩm đã mở cho quản lý, gồm bốn tab: sản phẩm, loại, thương hiệu,
biến thể & tồn. Xem QUAN_LY_DANH_MUC_SAN_PHAM.md.
Nhân viên: tìm kiếm/phân trang, hồ sơ, tạo nhân viên cùng tài khoản tùy chọn,
cấp tài khoản, khóa/mở khóa, ngừng/khôi phục nhân viên và đổi vai trò.
Khách hàng: tìm kiếm/phân trang, hồ sơ, thêm/sửa và ngừng/khôi phục.
Khuyến mãi: tìm kiếm/phân trang, thêm/sửa/ngừng/khôi phục và phân biệt cờ lưu
với hiệu lực tính theo ngày server. Thống kê tải kết quả server theo ngày/tháng/năm
hoặc sản phẩm, hiển thị chỉ tiêu tiền và biểu đồ trang hiện tại.
Trang chủ hiển thị lối tắt công việc; FR13 export chưa có trong các màn hình này.

## Điều hướng theo chức năng — 06/11/2026

| Nhóm menu | Chức năng |
| --- | --- |
| Hệ thống | Trang chủ, thông tin cá nhân, đổi mật khẩu, đăng xuất |
| Danh mục | Sản phẩm → danh sách sản phẩm, loại sản phẩm, thương hiệu, biến thể & tồn |
| Cập nhật | Nhân viên & tài khoản, khách hàng, khuyến mãi, nhà cung cấp |
| Xử lý | Bán hàng & thanh toán, phiếu nhập kho |
| Tìm kiếm | Tra cứu sản phẩm, tra cứu hóa đơn, lịch sử kho |
| Báo biểu | Thống kê doanh thu |

Danh mục, Cập nhật và Báo biểu dành cho quản lý. Phiếu nhập và lịch sử kho cũng chỉ
hiển thị cho quản lý; server tiếp tục kiểm tra quyền trên mỗi yêu cầu. Nhân viên bán
hàng có Hệ thống, Xử lý và Tìm kiếm; tra cứu hóa đơn giữ phạm vi quyền hiện tại.

Menu Danh mục / Sản phẩm mở trực tiếp tab tương ứng của màn hình quản lý hiện có.
Tra cứu hóa đơn mở danh sách hóa đơn của quầy bán hàng để chọn và xem lại chứng từ.
Các màn hình được tái sử dụng khi chuyển menu để giữ trạng thái công việc.

Phím tắt: Ctrl+Home về trang chủ; F2 bán hàng; F3 tra cứu sản phẩm; F4 tra cứu hóa đơn;
F6 phiếu nhập kho (quản lý). Alt+H/D/C/X/T/B chọn các nhóm menu tương ứng.

## Tổ chức mã giao diện

- ChuDeGiaoDien: Metal Look and Feel của JDK, phông Tahoma, bảng và định dạng tiền.
- BieuTuongCoDien: icon 18×18 cho menu, tab và nút, vẽ trực tiếp bằng Java2D.
- BieuTuongCuaHang: biểu tượng móc áo vẽ vector bằng Graphics2D, không cần tải ảnh ngoài.
- KhungUngDung: khung điều hướng, không tự gọi mạng.
- ManHinhChinh: nối điều hướng với các panel và API/controller hiện có.
- KhungDangNhap / KhungDoiMatKhau / KhungHoSoNhanVien: view tái sử dụng,
  tách khỏi JFrame/JDialog để render và kiểm thử headless.
- KhungSanPham / KhungChiTietSanPham / KhungBanHang: các view nghiệp vụ thật,
  không thay bằng mock HTML.

Client vẫn gọi Socket qua API; entity/JPA/repository chỉ nằm ở server.
I/O vẫn chạy bằng SwingWorker, không chặn EDT. Quy tắc giá, phân quyền,
trạng thái hóa đơn, phiên bản và transaction tồn kho không chuyển sang client.

## Kiểm thử

kiemthu.KiemThuGiaoDienChung render view thật vào .tmp/swing-classic-ui, kiểm tra look-and-feel,
điều hướng, quyền hiển thị menu và thanh cuộn ở cửa sổ nhỏ.
kiemthu.KiemThuGiaoDienSanPham và kiemthu.KiemThuGiaoDienHoaDon cũng dùng ChuDeGiaoDien để kiểm tra bố cục hiện tại.
Ảnh render dùng dữ liệu fixture cho QA, không phải dữ liệu từ SQL Server của bạn.

Kiểm tra ngày 06/11/2026: 98/98 JUnit test và scaffold smoke đạt. Đã kiểm tra riêng
cửa sổ ManHinhChinh thật qua Socket/SQL Server: menu nằm trên JFrame, loại sản phẩm tải
6 dòng, thương hiệu tải 3 dòng, menu con mở đúng tab, màn hình bán hàng được giữ khi
chuyển trang và nhân viên bán hàng chỉ thấy 3 nhóm menu. Dữ liệu SQL Server không đổi.
Ảnh của lần kiểm tra thật lưu tại `.tmp/functional-menu-live/`.

Trong Eclipse dùng Run As → Maven build → Goals: test.
scripts/KiemThuCucBo.ps1 hỗ trợ dependency trong .m2 và bản jar tạm .tmp/dependencies
để kiểm thử cục bộ; Eclipse vẫn lấy dependency chính thức từ pom.xml.
