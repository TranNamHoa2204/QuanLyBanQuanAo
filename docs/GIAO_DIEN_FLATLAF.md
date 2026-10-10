# Giao diện Java Swing + FlatLaf

## Chạy trên Eclipse

1. Chọn project → F5 (Refresh).
2. Chuột phải → Maven → Update Project… (Alt+F5).
   Maven tải com.formdev:flatlaf:3.7 từ dependency trong pom.xml.
   Không thêm jar bằng Java Build Path thủ công.
3. Chạy server.ServerMain như trước (cấu hình QUANAO_DB_* không đổi).
4. Dừng ClientMain cũ và chạy lại client.ClientMain.
   UiTheme.install() khởi tạo FlatLaf trước khi tạo bất kỳ view Swing nào.
5. Nếu báo thiếu com.formdev.flatlaf: kiểm tra Maven Dependencies,
   kết nối tải Maven Central, rồi Update Project và Project → Clean.

Java 8 được giữ nguyên. Không cần WindowBuilder, JavaFX, Spring Boot hoặc
thêm dependency Socket. Không chạy SQL mới cho thay đổi giao diện này.

## Bố cục đã có

- Đăng nhập: hai cột, phần nhận diện cửa hàng và form tài khoản; Enter để đăng nhập.
  Mật khẩu được che mặc định; có nút hiện/ẩn khi nhập.
- Màn hình chính: sidebar Tổng quan / Sản phẩm / Bán hàng & hóa đơn;
  khu vực tài khoản, vai trò và các lối tắt công việc.
  THREAD là tên nhận diện minh họa cho giao diện, có thể đổi trong MainShellPanel
  và DangNhapPanel; không phải tên thương hiệu từ dữ liệu sản phẩm.
- Sản phẩm: thẻ bộ lọc, bảng phân trang, trạng thái dữ liệu; giữ tra cứu,
  sắp xếp trong trang, xem chi tiết, chọn màu để xem ảnh.
- Bán hàng: phần thông tin hóa đơn, chọn biến thể, bảng hàng và tóm tắt thanh toán.
  Chuyển về Tổng quan/Sản phẩm không hủy nháp hay mất trạng thái panel bán hàng.
  Cửa sổ nhỏ có thanh cuộn để không che các thao tác ở cuối màn hình.
- Hồ sơ: các trường chỉ đọc, nút tải thông tin theo tài khoản đang đăng nhập.
- Đổi mật khẩu: form riêng, nhập lại mật khẩu mới, giữ việc thu hồi phiên sau khi đổi.

Danh mục & sản phẩm đã mở cho quản lý, gồm bốn tab: sản phẩm, loại, thương hiệu,
biến thể & tồn. Xem QUAN_LY_DANH_MUC_SAN_PHAM.md.
Nhân viên / Khuyến mãi / Thống kê vẫn ghi “Sắp có” và bị vô hiệu hóa.
Không có số liệu thống kê giả trên Tổng quan.

## Tổ chức mã giao diện

- UiTheme: FlatLightLaf, màu xanh đậm, nền sáng, font, thẻ, bảng và định dạng tiền.
- BrandIcon: biểu tượng móc áo vẽ vector bằng Graphics2D, không cần tải ảnh ngoài.
- MainShellPanel: khung điều hướng, không tự gọi mạng.
- MainForm: nối điều hướng với các panel và API/controller hiện có.
- DangNhapPanel / DoiMatKhauPanel / NhanVienProfilePanel: view tái sử dụng,
  tách khỏi JFrame/JDialog để render và kiểm thử headless.
- SanPhamPanel / SanPhamDetailPanel / BanHangPanel: các view nghiệp vụ thật,
  không thay bằng mock HTML.

Client vẫn gọi Socket qua API; entity/JPA/repository chỉ nằm ở server.
I/O vẫn chạy bằng SwingWorker, không chặn EDT. Quy tắc giá, phân quyền,
trạng thái hóa đơn, phiên bản và transaction tồn kho không chuyển sang client.

## Kiểm thử

FlatLafUiTest render view thật vào .tmp/flatlaf-ui, kiểm tra look-and-feel,
điều hướng, quyền hiển thị menu và thanh cuộn ở cửa sổ nhỏ.
ProductUiTest và InvoiceUiTest cũng dùng UiTheme/FlatLaf để kiểm tra bố cục hiện tại.
Ảnh render dùng dữ liệu fixture cho QA, không phải dữ liệu từ SQL Server của bạn.

Trong Eclipse dùng Run As → Maven build → Goals: test.
scripts/Test-Local.ps1 hỗ trợ dependency trong .m2 và bản jar tạm .tmp/dependencies
để kiểm thử cục bộ; Eclipse vẫn lấy dependency chính thức từ pom.xml.

Tham khảo chính thức: https://www.formdev.com/flatlaf/
