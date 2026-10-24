/*
  Cấu hình thuế GTGT cho đồ án bán lẻ.
  Mức 8% được cấu hình theo chính sách hiện hành đến hết 31/12/2026;
  người quản lý cần ngừng áp dụng/cập nhật nếu chính sách thay đổi.
  Mức 0% chỉ dùng cho giao dịch đủ điều kiện; việc chọn mức thuế không
  tự xác nhận điều kiện pháp lý. Chọn đúng database trong SSMS trước khi chạy.
  Chỉ thêm mã còn thiếu. Không đổi/xóa cấu hình đã có.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.Thue', N'U') IS NULL
        THROW 50001, N'Chọn database đã có bảng dbo.Thue.', 1;

    DECLARE @Thue TABLE(maThue VARCHAR(20), tenThue NVARCHAR(100), phanTram DECIMAL(5,2), moTa NVARCHAR(255));
    INSERT INTO @Thue VALUES
      ('VAT10', N'Thuế GTGT 10%', 10.00, N'Mức thuế suất GTGT tiêu chuẩn; áp dụng tùy mã hàng và phương pháp tính thuế.'),
      ('VAT8',  N'Thuế GTGT 8%',   8.00, N'Mức giảm theo chính sách hiện hành đến hết 31/12/2026; cần rà soát khi chính sách thay đổi.'),
      ('VAT0',  N'Thuế GTGT 0%',   0.00, N'Chỉ áp dụng giao dịch đủ điều kiện theo quy định, ví dụ xuất khẩu có đủ hồ sơ.');

    IF EXISTS (
        SELECT 1 FROM @Thue d JOIN dbo.Thue t WITH (UPDLOCK, HOLDLOCK) ON t.maThue = d.maThue
        WHERE t.tenThue <> d.tenThue OR t.phanTram <> d.phanTram
    )
        THROW 50002, N'Một mã thuế demo đã được dùng cho cấu hình khác; script không ghi đè dữ liệu.', 1;

    INSERT INTO dbo.Thue(maThue, tenThue, phanTram, moTa, trangThai)
        SELECT d.maThue, d.tenThue, d.phanTram, d.moTa, 1
        FROM @Thue d
        WHERE NOT EXISTS (SELECT 1 FROM dbo.Thue t WHERE t.maThue = d.maThue);

    COMMIT TRANSACTION;
    SELECT maThue, tenThue, phanTram, trangThai
    FROM dbo.Thue WHERE maThue IN ('VAT10','VAT8','VAT0')
    ORDER BY maThue;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
