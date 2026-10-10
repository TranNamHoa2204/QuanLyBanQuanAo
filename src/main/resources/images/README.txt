ẢNH ĐÃ CHUẨN HÓA
- File JPEG/PNG, giữ nguyên kích thước và nội dung hình ảnh; bản WebP gốc ở data/backups/.
- Đường dẫn classpath bắt đầu images/, dùng dấu / và tên file viết thường.
- ProductImage.xlsx đã chứa đường dẫn đầy đủ như images/products/1001/red-1.jpg.
- Các Excel đã thêm mã dùng trong SQL; SKU và mã nguồn cũ không bị cắt.
- Không dùng đường dẫn ổ Q: hoặc new File(...) để đọc resource trong JAR.
- Đọc bằng client.util.ImageLoader.load(path).
- Dữ liệu demo từ người dùng; giá/tồn kho/thương hiệu chưa xác minh với nhà bán hàng.
