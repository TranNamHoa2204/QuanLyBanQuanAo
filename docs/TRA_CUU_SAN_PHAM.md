# Tra cứu và chi tiết sản phẩm

## Chuẩn bị trước khi chạy bản mới

Server dùng validate, không tự tạo bảng hoặc nạp dữ liệu. Nếu đã có 12 bảng và
dữ liệu sản phẩm trong SQL Server, không cần chạy lại script.

Nếu chưa có dbo.AnhSanPham:

1. Chạy sql/01_ThemBangAnhSanPham.sql trong SSMS.
2. Nếu muốn dùng bộ dữ liệu demo đã chuẩn hóa, chạy sql/02_NhapDuLieuQuanAo.sql.
   Script thêm bản ghi thiếu và không ghi đè giá/tồn kho hiện có. Đọc thông báo xung đột
   nếu database đã có dữ liệu với mã/tên khác; không xóa database để xử lý.
3. F5 → Maven Update Project → Project Clean trong Eclipse.
4. Dừng và chạy lại ServerMain, rồi chạy lại ClientMain.

Không chạy lại script tạo database gốc trên database đang có dữ liệu.
Lượt code này không thực thi script hoặc cập nhật dữ liệu trong SQL Server của bạn.

## Cách dùng

Đăng nhập → Tra cứu sản phẩm.

- Tên: tìm chuỗi chứa, không phân biệt hoa/thường qua LOWER trong query.
  Có dấu/không dấu còn phụ thuộc collation của SQL Server.
- Loại, thương hiệu: chọn từ danh mục database, không hard-code tên.
- Màu và size: nhập đúng tên (bỏ khoảng trắng đầu/cuối, không phân biệt hoa/thường).
  Hai điều kiện phải thuộc cùng một biến thể.
- Tra cứu hoặc Enter: bắt đầu lại từ trang 1.
- Xóa bộ lọc: bỏ mọi điều kiện và tra cứu lại.
- Chọn 10/20/50/100 dòng rồi Tra cứu để áp dụng.
- Trang trước/sau dùng đúng bộ lọc của lần tra cứu thành công gần nhất.
- Click tiêu đề cột để sắp xếp trong trang đang xem. Giá và tồn được sort theo số.
- Chọn một dòng → Xem chi tiết, hoặc double-click dòng sản phẩm.

Giá dùng BigDecimal từ database qua Socket; JSON được cấu hình tránh chuyển tiền qua double.
Tồn đang bán là tổng số lượng của các biến thể hoạt động trong sản phẩm,
không chỉ tổng của màu/size đang lọc. Dùng BIGINT khi SUM để không tràn số INT.

## Chi tiết và ảnh

Màn hình gồm thông tin sản phẩm, giá, tổng tồn, bảng biến thể và ảnh.

- Chọn dòng biến thể sẽ chọn màu tương ứng cho ảnh.
- Có thể chọn màu trực tiếp ở combobox ảnh.
- Ảnh chính được mở trước; Ảnh trước/sau di chuyển trong nhóm ảnh cùng màu.
- Ảnh được thu nhỏ giữ tỷ lệ, không làm thay đổi file gốc.
- Ảnh tải ngoài Event Dispatch Thread, có cache thumbnail tối đa 16 ảnh/màn hình.
- Chưa có ảnh hoặc file resource không tồn tại: hiển thị thông báo, vẫn xem được dữ liệu.
- Tồn 0 nghĩa là hết hàng; thao tác tra cứu không làm tăng/giảm tồn kho.

Server trả đường dẫn dạng images/products/1001/red-1.jpg, không truyền ảnh nhị phân
và không tải từ website. Client đọc từ classpath, nên đóng gói resources cùng client JAR.
Không dùng đường dẫn ổ Q: trong ứng dụng.

## Quyền truy cập

| Chức năng | Nhân viên bán hàng | Nhân viên quản lý |
| --- | --- | --- |
| Tra cứu và danh mục | Có | Có |
| Xem chi tiết hàng đang bán | Có | Có |
| Gồm hàng/biến thể ngừng bán trong tra cứu | Không | Có, bật checkbox |
| Xem chi tiết hàng ngừng bán bằng mã | Không | Có |
| Biến thể/ảnh trong chi tiết | Chỉ màu có biến thể đang hoạt động | Cả biến thể ngừng bán |

Cả hai phải có phiên hợp lệ và tài khoản/nhân viên đang hoạt động.
Server kiểm tra trong cùng transaction truy vấn dữ liệu; gửi token giả hoặc yêu cầu
includeInactive=true từ client bán hàng không vượt qua được kiểm tra.

Quản lý xem đủ biến thể trong chi tiết kể cả khi checkbox tra cứu chưa bật,
để nhận biết tình trạng từng biến thể. Trạng thái được ghi rõ trong bảng.
Tổng tồn trong cả hai vai trò chỉ tính biến thể đang hoạt động.

## Giao thức và giới hạn

Dùng các action có sẵn:

- TRA_CUU_SAN_PHAM: SanPhamSearchRequest → SanPhamSearchResult.
- XEM_CHI_TIET_SAN_PHAM: SanPhamDetailRequest → SanPhamDetailDTO.
- TRA_CUU_LOAI_SAN_PHAM → danh sách LoaiSanPhamDTO.
- TRA_CUU_THUONG_HIEU → danh sách ThuongHieuDTO.

page trong DTO bắt đầu từ 0; UI hiển thị bắt đầu từ 1.
pageSize từ 1 đến 100; page tối đa 1.000.000; sort server theo maSP để phân trang ổn định.
Màu/size dùng EXISTS để không nhân đôi sản phẩm khi có nhiều biến thể khớp.
Dữ liệu tìm kiếm dùng query parameter; %, _, [, ], ! trong tên được tìm như ký tự literal.

Giới hạn chi tiết 500 biến thể và 500 ảnh mỗi sản phẩm; danh mục tối đa 1.000 bản ghi
mỗi loại. Nếu vượt, server báo cần phân trang, không lặng lẽ cắt bỏ dữ liệu.
Nếu giá/tồn thay đổi trong khi người dùng tra cứu, dữ liệu lần đọc tiếp theo có thể khác;
đây chưa phải khóa tồn kho để thanh toán.

## Kiểm thử

ProductIntegrationTest chạy Socket và JPA thật với H2 trong bộ nhớ:
phân trang, lọc kết hợp, màu/size cùng biến thể, quyền quản lý, token/tài khoản bị khóa,
hàng không có biến thể/ảnh, mã không tồn tại, kiểm tra pagination, tiền chính xác và tổng tồn lớn.

NormalizedCatalogTest nhập catalog JSON chỉ vào H2, kiểm tra 40 sản phẩm, 152 biến thể,
255 ảnh qua API, khớp màu và đường dẫn ảnh classpath. Đây không phải trình import SQL Server
tự động trong ứng dụng.

ProductUiTest render hai panel Swing bằng dữ liệu thật vào PNG trong .tmp/product-ui
để kiểm tra bố cục headless, bảng chỉ đọc và tải ảnh ngoài EDT.
Các test tài khoản trước đó vẫn được chạy lại.

## Chưa triển khai

Màn hình tra cứu này chỉ đọc. Thêm/sửa/ngừng bán và sửa tồn nằm ở menu
Danh mục & sản phẩm dành cho quản lý; xem QUAN_LY_DANH_MUC_SAN_PHAM.md.
Lập hóa đơn/thanh toán nằm ở Bán hàng / Hóa đơn; xem BAN_HANG.md.
Chưa có phiếu nhập hàng, đổi trả hoặc thống kê.
