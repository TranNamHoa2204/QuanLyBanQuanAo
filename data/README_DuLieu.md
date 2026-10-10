# Dữ liệu quần áo đã chuẩn hóa

Nguồn: thư mục ảnh và ba Excel người dùng đưa vào project. Bộ này dùng cho demo;
giá, tồn kho và thương hiệu được giữ theo Excel, chưa đối chiếu nhà bán hàng.
Đơn vị tiền quy ước cho bộ demo là VNĐ; không có chuyển đổi ngoại tệ.

## Những gì đã thay đổi

- Giữ 40 sản phẩm, 152 biến thể, 255 ảnh sản phẩm và 12 ảnh banner/logo/thanh toán.
- Đổi WebP sang JPEG chất lượng 95 hoặc PNG khi có alpha; giữ nguyên kích thước,
  không cắt ảnh hay đổi nội dung. Ảnh gốc vẫn ở bản sao lưu.
- Tên file viết thường. Đường dẫn dùng dấu /, bắt đầu images/, đọc được từ JAR.
- Sửa pink-34.webp của sản phẩm 1026 thành pink-3.jpg trong bộ chuẩn hóa.
- Chuẩn hóa Unicode NFC và khoảng trắng trong chuỗi; không cắt mã nguồn/SKU.
- Mã dùng trong database: MaSP = SP + ProductID; MaBienThe = BT + VariantID đệm
  đủ 6 chữ số; mã loại LSP001…; mã thương hiệu TH001…; mã ảnh ANH000001…
- Thêm các cột mapping vào Excel; giữ các cột gốc và Notes, mở rộng bảng Excel hiện có.
- Mỗi nhóm sản phẩm/màu có một ảnh chính và thứ tự ảnh liên tục.

## Đối chiếu màu đã thực hiện

| ProductID | Chuẩn hóa | Cơ sở |
| --- | --- | --- |
| 1005 | Xanh lơ / Xanh dương nhạt → Xanh | Nhãn nguồn không khớp rõ với ảnh xanh đậm; dùng tên trung tính cho demo |
| 1006 | Đen → Đen họa tiết trong metadata ảnh | Đối chiếu ảnh có họa tiết và tên màu của biến thể |
| 1007 | Đen họa tiết → Đen trong metadata ảnh | Đối chiếu áo đen và tên màu của biến thể |
| 1036 | Đen → Xám trong biến thể | Đối chiếu áo xám thực tế và metadata ảnh |
| 1020 | Xanh Indigo với khoảng trắng đặc biệt → khoảng trắng thường | Chuẩn hóa chuỗi, không đổi màu |

Đây là quyết định cho bộ demo, không xác nhận mã màu/SKU chính thức của nhà bán hàng.
Danh sách thay đổi chi tiết nằm trong normalization-report.json; dữ liệu gốc ở backups/.
Thư mục data resource cũ (catalog.json theo cấu trúc trước) được giữ riêng tại
backups/resource-data-original-20261008; catalog mới thống nhất với Excel và SQL mới.

## Dữ liệu dùng trong ứng dụng

- src/main/resources/images/: ảnh và ba Excel chuẩn hóa.
- src/main/resources/data/catalog.json: dữ liệu sản phẩm/biến thể/ảnh và mapping nguồn.
- src/main/resources/data/image-paths.txt: danh sách 267 resource dùng để kiểm tra.
- client.util.ImageLoader.load("images/products/1001/red-1.jpg"): đọc ảnh từ classpath.
- server.entity.AnhSanPham, common.dto.AnhSanPhamDTO và repository.AnhSanPhamDao:
  khung dữ liệu cho bảng ảnh bổ sung. Chưa có ánh xạ JPA hoặc repository implementation.

## Nhập vào SQL Server bằng SSMS

1. Nếu chưa có database, chạy sql/TaoCSDL_QuanLyCuaHangQuanAo_SQLServer.sql một lần.
   Không chạy lại script gốc trên database đã có dữ liệu; không cần xóa database.
2. Chạy sql/01_ThemBangAnhSanPham.sql để bổ sung bảng ảnh. Database thành 12 bảng.
3. Chạy sql/02_NhapDuLieuQuanAo.sql. Kết quả bộ demo phải là 40 / 152 / 255.

Các script mới chưa được thực thi trên SQL Server. Script nhập chỉ thêm bản ghi thiếu,
không ghi đè giá/tồn kho hiện có hoặc xóa lịch sử. Nếu mã/tên bị dữ liệu khác sử dụng,
script dừng và rollback; không tự động sửa xung đột. Không tạo tài khoản/mật khẩu mẫu.
SQL chỉ lưu đường dẫn tương đối; bản thân ảnh vẫn là resource của client, không nằm trong SQL Server.
Khi chạy client ở máy khác, đóng gói bộ ảnh cùng JAR client.

Nếu vẽ lại database diagram, thêm AnhSanPham và quan hệ SanPham (1) — AnhSanPham (0..*).
Các DOCX/sơ đồ hiện có chưa được sửa trong lượt chuẩn hóa dữ liệu này.

## Kiểm tra

- Đã đối chiếu dữ liệu nguồn với bản chuẩn hóa; giữ giá/tồn/SKU và các thông tin sản phẩm,
  chỉ chuẩn hóa Unicode/khoảng trắng cùng những mapping/màu đã nêu.
- Kiểm tra đường dẫn theo đúng chữ hoa/thường, màu/size và ràng buộc mã <= 20 ký tự.
- Đã render và xem các sheet Excel bị ảnh hưởng; giữ tên sheet và native table.
- AssetSmokeCheck là kiểm tra thủ công qua Java ImageIO, chạy được với resource đóng gói JAR.
- Trong Eclipse nhấn F5, sau đó Project → Clean để làm mới resource cache cũ.
- Chuẩn hóa asset không có nghĩa ứng dụng đã có UI, Socket, JPA mapping hay dữ liệu trong database.

Sau lượt chuẩn hóa, phần tài khoản đã được triển khai bằng Swing + Socket + JPA;
xem ../docs/HUONG_DAN_CHAY.md. Sản phẩm/ảnh cũng đã có mapping JPA và màn hình tra cứu;
xem ../docs/TRA_CUU_SAN_PHAM.md. Chưa có lập hóa đơn/thanh toán.
