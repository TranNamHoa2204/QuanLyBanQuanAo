# Baseline SQL Server và thiết kế migration

## Phạm vi và kết luận

Đây là đối chiếu source/DDL giữa `DatabaseConfig`, `META-INF/persistence.xml`, các entity,
`sql/TaoCSDL_QuanLyCuaHangQuanAo_SQLServer.sql` và các migration đang có. Baseline trước
ledger có 12 bảng; migration 05 thêm `LichSuKho`, nên schema sau S15 có 13 bảng logic.
Migrations 04/06 thêm cột vào `HoaDon`, không tạo bảng. Không kết nối SQL Server và không
chạy SQL trong S27; một database SQL Server cụ thể vẫn chưa được nghiệm thu.

Persistence unit khai báo tường minh 13 entity hiện có và giữ
`hibernate.hbm2ddl.auto=validate`. `DatabaseConfig` chỉ lấy URL/user/password từ
`QUANAO_DB_URL`, `QUANAO_DB_USER`, `QUANAO_DB_PASSWORD`. Khi khởi động, server tạo
EntityManagerFactory trước khi mở Socket; schema sai hoặc thiếu sẽ làm server không sẵn sàng.

Baseline schema B1 gồm 11 bảng từ script khởi tạo cộng `dbo.AnhSanPham` từ
`sql/01_ThemBangAnhSanPham.sql`, tổng cộng 12 bảng baseline và 11 FK. Migration 04 thêm
các cột nullable cho thời điểm thanh toán/snapshot; migration 05 thêm bảng `LichSuKho`;
migration 06 thêm các cột thu tiền vào `HoaDon`. Vì vậy schema sau các mở rộng hiện có
có 13 bảng logic, không phải 12. Cả ba migration phải được áp dụng đúng thứ tự trên
database demo trước khi server validate schema hiện tại. `02_NhapDuLieuQuanAo.sql` nạp
dữ liệu demo, không phải migration schema; `03_ThueDemo_0PhanTram.sql` là dữ liệu thuế
0% chỉ dành cho demo.

## Đối chiếu entity với baseline

| Entity / bảng | PK và FK | Unique, check và invariant trong SQL | Kiểu/mapping đáng chú ý |
| --- | --- | --- | --- |
| `NhanVien` | PK `maNV` | Unique `sdt`; tên và số điện thoại không trắng | `hoTen`, `diaChi`, `chucVu` là NVARCHAR; `gioiTinh` là BIT nullable; `ngaySinh` là DATE |
| `TaiKhoan` | PK `username`; FK `maNV` → `NhanVien` | Unique `maNV`; vai trò chỉ nhận hai giá trị hiện tại; username/hash không trắng | Mã/hash/vai trò là VARCHAR; hash lưu chuỗi băm, không lưu mật khẩu rõ |
| `SanPham` | PK `maSP`; FK tới `LoaiSanPham`, `ThuongHieu` | Giá không âm; tên không trắng | Tên NVARCHAR(200), giá DECIMAL(18,2), trạng thái BIT |
| `BienThe` | PK `maBienThe`; FK `maSP` → `SanPham` | Unique `(maSP,mauSac,kichCo)`; tồn/version không âm; màu và cỡ không trắng | Màu/cỡ NVARCHAR; tồn INT; `phienBan BIGINT` khớp `@Version` số |
| `AnhSanPham` | PK `maAnh`; FK `maSP` → `SanPham` | Unique `duongDan` và `(maSP,mauSac,thuTu)`; filtered unique chỉ một ảnh chính theo `(maSP,mauSac)`; thứ tự dương, màu/path hợp lệ | Màu NVARCHAR(50), đường dẫn VARCHAR(500); JPA khai báo unique cho đường dẫn nhưng không mô tả composite/filtered index |
| `LoaiSanPham` | PK `maLoai` | Unique tên loại; tên không trắng | Tên NVARCHAR(100) |
| `ThuongHieu` | PK `maThuongHieu` | Unique tên thương hiệu; tên không trắng | Tên NVARCHAR(100) |
| `HoaDon` | PK `maHD`; FK bắt buộc tới `NhanVien`, `Thue`; FK nullable tới `KhachHang`, `KhuyenMai` | Trạng thái hợp lệ; tiền/tỷ lệ trong miền; tổng bằng tạm tính − giảm + thuế; version không âm | Tiền DECIMAL(18,2), tỷ lệ DECIMAL(5,2); `ngayLap` và `ngayThanhToan` DATETIME2(0); S11 thêm nullable tên nhân viên/khách NVARCHAR(100); `phienBan BIGINT` ↔ `@Version` |
| `ChiTietHoaDon` | PK `maCTHD`; FK tới `HoaDon`, `BienThe` | Unique `(maHD,maBienThe)`; số lượng dương, đơn giá không âm | `maCTHD BIGINT IDENTITY(1,1)` ↔ `Long`/IDENTITY; S11 thêm tên SP/màu/size snapshot NVARCHAR(200/50/20) nullable; tiền DECIMAL(18,2); `thanhTien` computed persisted và JPA không ghi cột này |
| `KhachHang` | PK `maKH` | Filtered unique số điện thoại chỉ khi `sdt IS NOT NULL`; tên không trắng | Tên/địa chỉ NVARCHAR; số điện thoại/email VARCHAR; nhiều khách chưa có số điện thoại được phép |
| `KhuyenMai` | PK `maKM` | Tỷ lệ 0–100; ngày kết thúc không trước ngày bắt đầu; tên không trắng | Tên NVARCHAR; tỷ lệ DECIMAL(5,2); ngày DATE ↔ `LocalDate` |
| `Thue` | PK `maThue` | Tỷ lệ 0–100 | Tên/mô tả NVARCHAR; tỷ lệ DECIMAL(5,2) |

Script gốc tạo 10 FK; migration ảnh thêm FK thứ 11. Các mã nghiệp vụ là VARCHAR do ứng
dụng cấp, chỉ `ChiTietHoaDon.maCTHD` dùng IDENTITY. Các trường tiếng Việt được đánh dấu
`@Nationalized` tương ứng NVARCHAR; các chuỗi kỹ thuật như mã, email, username, đường
dẫn và password hash là VARCHAR. Kiểu Boolean khớp BIT, các trường tiền dùng BigDecimal
với precision/scale tương ứng DECIMAL trong DDL.

Không thấy sai lệch cột, kiểu, độ dài hoặc nullable nào được chứng minh trong source và
DDL đang có, nên S02 không sửa entity hay SQL baseline. Hibernate `validate` không thay
thế việc kiểm tra hành vi các CHECK, unique index lọc, biểu thức computed hoặc dữ liệu.
Đặc biệt, JPA metadata của `AnhSanPham` không khai báo composite/filtered index và các
entity không khai báo toàn bộ CHECK SQL; database vẫn áp dụng các ràng buộc này. Không
đổi unique số điện thoại khách hàng thành unique không lọc vì điều đó sẽ cấm nhiều NULL.

## Ngày, giờ và version

`DATE` map sang `LocalDate`. `HoaDon.ngayLap` và `ngayThanhToan` map sang `DATETIME2(0)`;
service đóng dấu cả hai bằng đồng hồ `Asia/Ho_Chi_Minh`; đây là giờ địa phương không kèm offset trong
SQL. `hibernate.jdbc.time_zone=Asia/Ho_Chi_Minh` không biến `DATETIME2` thành kiểu có
timezone. DDL còn có default `SYSDATETIME()` theo giờ máy SQL Server cho insert không
truyền `ngayLap`; không suy diễn rằng giá trị default đó luôn cùng múi giờ với server
ứng dụng. Thao tác nghiệp vụ mới nên lấy timestamp từ server ứng dụng theo một chính
sách đã chốt, thay vì dựa vào múi giờ máy SQL.

`BienThe.phienBan` và `HoaDon.phienBan` là version số BIGINT, không phải SQL Server
`ROWVERSION`; Hibernate `@Version` chịu trách nhiệm tăng giá trị khi cập nhật. Với
`ChiTietHoaDon`, SQL tính `thanhTien` bằng `soLuong * donGia`, ép DECIMAL(18,2) và lưu
PERSISTED; JPA đánh dấu không insert/update. SQL Server cần xác nhận runtime việc đọc lại
giá trị computed sau khi persist.

## Tạo database demo và thứ tự script hiện tại

Chỉ dùng một SQL Server instance/database được dành riêng cho demo. Script khởi tạo và
migration 04 hiện kiểm tra tên `QuanLyCuaHangQuanAo`; dùng đúng tên đó cho database demo.
Nếu cần tên khác, tạo bản script demo riêng đã review và đồng bộ điều kiện tên DB trong
migration. Không chạy hướng dẫn này trên database thật hoặc database có dữ liệu cần giữ.

1. Trong SSMS mở toàn bộ `sql/TaoCSDL_QuanLyCuaHangQuanAo_SQLServer.sql` và chạy trên
   instance demo. Script tạo database nếu chưa có và từ chối nếu database đã có user
   table; không xóa bảng. Nếu bị từ chối, dừng và xác định schema hiện có, không xóa dữ
   liệu để chạy lại.
2. Chạy toàn bộ `sql/01_ThemBangAnhSanPham.sql` trong SSMS. Đây là bước schema bắt buộc
   để đủ 12 bảng baseline trước khi có ledger.
3. Sau khi xác nhận đây đúng database demo, chạy `sql/04_HoaDonSnapshotVaThoiDiemThanhToan.sql`.
   Script thêm các cột nullable, kiểm tra định nghĩa cột kể cả khi chạy lại, không backfill
   dữ liệu và rollback nếu schema có cột cùng tên nhưng sai kiểu/độ dài/nullability.
4. Nếu cần dữ liệu mẫu, chạy `sql/02_NhapDuLieuQuanAo.sql`. Script này thêm dữ liệu thiếu,
   kiểm tra xung đột mã/tên và không thay thế/xóa dữ liệu đang có.
5. Chỉ khi cần kiểm thử thuế demo 0%, chạy `sql/03_ThueDemo_0PhanTram.sql`. Không dùng
   thuế demo để kết luận mức thuế thực tế.
6. Sau khi dữ liệu demo cuối cùng đã nạp và trước khi khởi động bản S15, chạy
   `sql/05_LichSuBienDongKho.sql`. Migration chỉ thêm bảng ledger và ghi một mốc mở đầu
   cho tồn hiện hành từng biến thể; không tái dựng giao dịch cũ. Chạy lại sẽ xác minh
   cấu trúc và từ chối nếu có biến thể không có ledger, không tự bù lịch sử.
7. Sau migration 05, chạy `sql/06_ThuTienTaiQuay.sql` cho S18. Script thêm phương thức,
   tiền khách đưa, tiền thừa và cờ xác nhận thủ công chuyển khoản vào `HoaDon`;
   trường mới nullable, không backfill lịch sử và CHECK ràng buộc hai dạng thanh toán.
   Chạy lại chỉ thành công nếu định nghĩa cột/ràng buộc tương thích.
8. Cấp cho server tài khoản database demo phù hợp, đặt `QUANAO_DB_URL`,
   `QUANAO_DB_USER`, `QUANAO_DB_PASSWORD` trong Run Configuration/môi trường cục bộ rồi
   chạy `server.ServerMain`. Hibernate phải validate thành công trước khi server mở
   Socket. Không ghi thông tin kết nối thật vào source hoặc báo cáo.

Không chạy lại script khởi tạo trên database đã có bảng. Nếu `AnhSanPham` đã tồn tại,
`01_ThemBangAnhSanPham.sql` chỉ in thông báo và bỏ qua kiểm tra cột, kiểu, FK, check,
unique và filtered index. Vì vậy thông báo “đã tồn tại” không chứng minh schema đó khớp
baseline; cần kiểm tra metadata trong database demo trước khi cho server sử dụng.

## Trạng thái migration và phần chưa có schema

Repo chưa có runner kiểm tra version/checksum; các file SQL chạy thủ công theo thứ tự.
S15 đã bổ sung bảng ledger và migration tăng dần `sql/05_LichSuBienDongKho.sql`:
script giới hạn database demo, kiểm tra baseline cần thiết, xác minh kiểu/cột/ràng buộc/
index nếu bảng đã tồn tại và chỉ seed tồn mở đầu khi chính migration lần đầu tạo bảng.
Không tự áp dụng SQL hoặc tạo bảng cho FR chưa được giao.

| Version/phạm vi | Trạng thái trong source/SQL hiện tại | Phụ thuộc và ghi chú |
| --- | --- | --- |
| Baseline B1 | Script khởi tạo 11 bảng; `01_ThemBangAnhSanPham.sql` bổ sung bảng ảnh thành 12 bảng. | Chỉ dùng database demo trống; không có migration runner/checksum trong repo. |
| FR10 / migration 05 | Entity `LichSuKho`, repository/service/API/UI ledger đã có; migration thêm bảng ledger và mốc tồn mở đầu. Schema dự kiến sau bước này có 13 bảng. | Bán/khởi tạo/điều chỉnh tồn ghi ledger trong transaction. Chưa chạy migration/validate trên SQL Server demo. |
| Snapshot thời gian / migration 04 | Timestamp thanh toán và snapshot tên nhân viên/khách/sản phẩm/màu/size đã map; dữ liệu lịch sử giữ NULL. | Migration tăng dần, không backfill. SQL Server chưa xác minh. Đây là nền dữ liệu chứng từ, chưa có chức năng FR13 in/xuất. |
| FR12 / migration 06 | Thu tiền mặt hoặc xác nhận chuyển khoản thủ công có cột, enum, DTO và UI checkout. | Thêm cột và CHECK trên `HoaDon`; không backfill giao dịch cũ, không tích hợp/xác minh ngân hàng. SQL Server chưa xác minh. |
| FR11 | Không có entity, service, action, migration NCC/phiếu nhập. | Chưa có DDL; không mô tả trạng thái phiếu nhập như phần đã triển khai. |
| FR14 | Không có ngưỡng tồn hoặc dashboard trong source. | Chưa có schema, API hoặc UI; chưa chốt cấu hình ngưỡng. |
| FR15 | Không có entity/bảng audit, action reset hay cờ buộc đổi mật khẩu. | Chưa có schema/API/UI. Không có migration cho FR15. |
| FR13 | Chưa có action, API hoặc UI in/PDF/CSV. | Migration 04 chỉ cung cấp snapshot/time, không tự tạo chức năng xuất chứng từ. |

Các script tăng dần đang có là `04_HoaDonSnapshotVaThoiDiemThanhToan.sql`,
`05_LichSuBienDongKho.sql` và `06_ThuTienTaiQuay.sql`. Chưa có script cho FR11, FR13, FR14,
FR15. Các script hiện chạy thủ công; không tự áp dụng trên database bất kỳ.

Quy ước migration đề xuất:

- Các script đánh số trong `sql/` phải chỉ thêm/điều chỉnh dữ liệu có chủ đích;
  khi mở rộng, chuyển sang `sql/migrations/` có version rõ ràng;
  không sửa file migration đã phát hành và không có lệnh reset/xóa database.
- Dùng ledger phiên bản/checksum. Ghi baseline V000 chỉ sau bước xác minh B1; mỗi lần
  chạy kiểm tra version, checksum và tiền điều kiện. Nếu đối tượng đã tồn tại, chỉ coi
  là đã áp dụng khi cấu trúc/định nghĩa khớp chính xác; schema không tương thích phải
  dừng với lỗi rõ ràng thay vì bỏ qua `IF OBJECT_ID`.
- Mỗi migration nên transaction hóa DDL/DML khi SQL Server hỗ trợ, bật `XACT_ABORT`,
  kiểm tra FK/check/unique/index sau khi chạy, và không ghi ledger nếu migration thất bại.
  Dữ liệu cũ phải giữ nguyên; backfill chỉ được làm khi có nguồn dữ liệu đúng và luật
  nghiệp vụ rõ ràng.
- Chạy trên database demo clone, lưu snapshot metadata và số dòng liên quan trước/sau.
  Apply lần đầu rồi chạy lại cùng version/checksum: lần hai chỉ thành công dạng no-op
  sau khi xác nhận postcondition; phải phát hiện checksum khác hoặc schema bị lệch.
  Không dùng lệnh xóa/reset để làm cho lần rerun “sạch”.

## Nghiệm thu cần chạy trên SQL Server demo

Các kiểm tra sau chưa được chạy trong S02 vì không có môi trường SQL Server demo được
xác định và không có quyền dùng database thật:

- Fresh install: chạy đúng thứ tự trên, kiểm tra 12 bảng baseline, 13 bảng logic sau
  migration 05, 11 FK baseline cộng FK ledger và metadata PK/unique/filtered index/check.
- Khởi động server bằng cấu hình DB demo để chứng minh Hibernate `validate` thành công.
- Ghi/đọc nội dung tiếng Việt qua ứng dụng và xác nhận không lỗi dấu; kiểm tra kiểu
  NVARCHAR của các cột tương ứng.
- Tạo dòng hóa đơn trong demo để xác nhận SQL tự tính và lưu `thanhTien`, JPA không gửi
  giá trị computed. Kiểm tra optimistic locking bằng hai transaction cập nhật cùng một
  `HoaDon`/`BienThe`: transaction dùng version cũ phải bị từ chối.
- Trước migration lưu metadata schema và số dòng; chạy migration; xác nhận không mất/sửa
  dữ liệu cũ; chạy lại đúng checksum; xác nhận schema vẫn đúng và không tạo trùng object.
  Với V000, cố ý thử database thiếu/sai object để xác nhận bước baseline từ chối.

Tình trạng S02: đối chiếu tĩnh hoàn tất; không sửa source/entity/SQL và không chạy test
hay SQL. SQL Server fresh install, JPA validate, Unicode, computed column, optimistic
locking và migration rerun vẫn **chưa được kiểm thử**.

Tình trạng S11: entity/DTO/detail mapping và migration cộng thêm đã cập nhật. Bộ
`scripts/Test-Local.ps1` ở mốc S11 chạy 73 JUnit tests; lần S27 chạy lại source hiện tại
đạt 88 JUnit tests cùng scaffold smoke test trên H2. Hai con số thuộc hai mốc chạy khác
nhau. SQL Server demo chưa có nên migration, Hibernate validate với SQL Server và rerun
trên database có dữ liệu vẫn **chưa được kiểm thử**.
