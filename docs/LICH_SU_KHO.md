# FR10 — Lịch sử biến động kho (S15)

## Hợp đồng nghiệp vụ

`LichSuKho` là ledger append-only. Mỗi dòng giữ biến thể, thời điểm do server đóng,
nguồn, chứng từ/tham chiếu, operationId, tồn trước, delta, tồn sau, version trước,
người thao tác (FK và tên snapshot), và lý do. Bất biến số lượng là
`soLuongTruoc + soLuongThayDoi = soLuongSau`; số trước/sau không âm.

Các nguồn S15 hiện có:

- `TON_MO_DAU`: mốc số tồn đang có khi triển khai migration; actor để trống và lý do
  ghi rõ đây không phải lịch sử giao dịch.
- `KHOI_TAO_BIEN_THE`: tồn khởi tạo (kể cả 0) khi tạo biến thể qua service quản lý.
- `BAN_HANG`: số âm theo lượng bán, cùng transaction thanh toán hóa đơn.
- `DIEU_CHINH_TON`: đặt tồn mục tiêu theo thao tác quản lý riêng, kèm lý do, UUID
  operationId và expectedVersion. Client không gửi tồn trước/delta/tồn sau.

Nháp và hủy nháp không ghi lịch sử tồn. Thanh toán gọi lại hóa đơn đã trả chỉ đọc kết quả
cũ. Các thay đổi tồn của những giao dịch mới ghi ledger và cập nhật `BienThe` trong cùng
transaction; lỗi/rollback không để lại một nửa giao dịch. Không xóa cứng ledger hoặc
biến thể đã được tham chiếu.

## Socket và phân quyền

`TRA_CUU_LICH_SU_KHO` nhận bộ lọc biến thể, ngày bắt đầu/kết thúc (bao gồm trọn ngày kết
thúc), nguồn, nhân viên, page/pageSize. Trả DTO và tổng số dòng, tối đa 100 dòng/trang
và khoảng thời gian 10 năm. `DIEU_CHINH_TON_KHO` nhận mã biến thể, số nguyên mục tiêu,
expectedVersion, lý do và UUID operationId. Chỉ `NHAN_VIEN_QUAN_LY` được tra cứu và điều
chỉnh; quyền được kiểm tra trên server.

Gửi lại cùng operationId và cùng nội dung điều chỉnh trả dòng ledger đã ghi. Dùng lại ID
cho nội dung/biến thể/người thao tác khác bị từ chối. UI giữ operationId trong suốt một
lần xác nhận để có thể gửi lại đúng request nếu response mạng bị mất; không tự tạo request
mới hoặc tự retry ghi.

Thanh toán giữ thứ tự khóa hiện có: hóa đơn, các sản phẩm theo mã tăng dần, rồi các biến
thể theo mã tăng dần. Tạo biến thể khóa sản phẩm trước biến thể. Điều chỉnh tồn khóa một
biến thể; không khóa sản phẩm ngược thứ tự. Version và khóa pessimistic ngăn một lần bán
và kiểm kê cùng ghi đè tồn của nhau.

## Triển khai database

Sau baseline 12 bảng, migration 04 và dữ liệu demo cần nạp theo thứ tự trong hướng dẫn,
chạy `sql/05_LichSuBienDongKho.sql` một lần trên database demo `QuanLyCuaHangQuanAo`,
trước khi khởi động phiên bản có ledger. Script tạo ledger, FK, CHECK, unique operationId, index tìm kiếm,
rồi ghi mỗi biến thể hiện có một dòng `TON_MO_DAU`. Migration lần đầu và seed nằm trong
cùng transaction. Rerun xác minh định nghĩa bảng/ràng buộc/index và không seed lại; nếu
phát hiện một biến thể chưa có bất kỳ dòng ledger nào, script dừng để điều tra thay vì
tự ghi bù.

Không chạy migration này trên database thật. Không có SQL Server demo được xác minh trong
lần cập nhật S27, nên migration SQL Server, kiểm tra Hibernate `validate` trên schema SQL Server, và
rerun giữ dữ liệu vẫn chưa được xác minh. Các test H2 kiểm tra entity, service, quyền,
Socket và transaction nghiệp vụ. Một lần thử mở lại schema H2 ở chế độ Hibernate
`validate` dừng tại mapping baseline `AnhSanPham.mauSac` (H2 báo `VARCHAR`, Hibernate
mong `NVARCHAR`); đây không phải lỗi ledger, và chưa dùng H2 validate làm bằng chứng thay
cho SQL Server.

## Giới hạn lịch sử

Migration chỉ biết số tồn hiện hành, nên tạo mốc tại thời điểm triển khai. Báo cáo không
thể suy ra thời điểm/actor/lý do của các lần nhập, bán hoặc chỉnh tồn trước đó. Mọi đường
ghi tồn trong ứng dụng cần tiếp tục đi qua `HoaDonService`, `QuanLySanPhamService` hoặc
service ledger; cập nhật trực tiếp SQL ngoài ứng dụng không được ledger ghi nhận.
