/*
  CHỈ DÙNG CHO DEMO HỌC TẬP, không xác định nghĩa vụ thuế thực tế.
  Chọn đúng database QuanLyCuaHangQuanAo trong SSMS trước khi chạy.
  Thêm nếu thiếu, không sửa bất kỳ loại thuế đã có.
*/
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.Thue', N'U') IS NULL
BEGIN
    ROLLBACK TRANSACTION;
    THROW 50001, N'Chọn database đã có bảng dbo.Thue.', 1;
END;
IF NOT EXISTS (SELECT 1 FROM dbo.Thue WITH (UPDLOCK, HOLDLOCK) WHERE maThue = 'T_DEMO_0')
BEGIN
    INSERT INTO dbo.Thue (maThue, tenThue, phanTram, moTa, trangThai)
    VALUES ('T_DEMO_0', N'Thuế demo học tập 0%', 0,
            N'Cấu hình kiểm thử đồ án; không dùng để kết luận thuế bán hàng thực tế.', 1);
END;
COMMIT TRANSACTION;
SELECT maThue, tenThue, phanTram, trangThai FROM dbo.Thue ORDER BY maThue;
