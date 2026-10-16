-- S11: bổ sung thời điểm thanh toán và tên hiển thị snapshot cho chứng từ.
-- Chạy một lần hoặc chạy lại an toàn sau baseline 12 bảng; không backfill lịch sử.
-- Chỉ áp dụng trên database demo đã xác định, không chạy trên database thật.
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF DB_NAME() <> N'QuanLyCuaHangQuanAo'
        THROW 51010, N'S11 yêu cầu database demo QuanLyCuaHangQuanAo; đã dừng trước khi thay đổi schema.', 1;
    IF OBJECT_ID(N'dbo.HoaDon', N'U') IS NULL OR OBJECT_ID(N'dbo.ChiTietHoaDon', N'U') IS NULL
        THROW 51011, N'Không tìm thấy đủ bảng HoaDon và ChiTietHoaDon của baseline.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.HoaDon') AND name = N'ngayThanhToan')
        ALTER TABLE dbo.HoaDon ADD ngayThanhToan DATETIME2(0) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.HoaDon') AND name = N'tenNhanVienSnapshot')
        ALTER TABLE dbo.HoaDon ADD tenNhanVienSnapshot NVARCHAR(100) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.HoaDon') AND name = N'tenKhachHangSnapshot')
        ALTER TABLE dbo.HoaDon ADD tenKhachHangSnapshot NVARCHAR(100) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.ChiTietHoaDon') AND name = N'tenSPSnapshot')
        ALTER TABLE dbo.ChiTietHoaDon ADD tenSPSnapshot NVARCHAR(200) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.ChiTietHoaDon') AND name = N'mauSacSnapshot')
        ALTER TABLE dbo.ChiTietHoaDon ADD mauSacSnapshot NVARCHAR(50) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.ChiTietHoaDon') AND name = N'kichCoSnapshot')
        ALTER TABLE dbo.ChiTietHoaDon ADD kichCoSnapshot NVARCHAR(20) NULL;

    -- Existence alone is not enough: fail on a pre-existing incompatible definition.
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.HoaDon') AND c.name = N'ngayThanhToan'
          AND t.name = N'datetime2' AND c.scale = 0 AND c.is_nullable = 1)
        THROW 51012, N'dbo.HoaDon.ngayThanhToan phải DATETIME2(0) NULL.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.HoaDon') AND c.name = N'tenNhanVienSnapshot'
          AND t.name = N'nvarchar' AND c.max_length = 200 AND c.is_nullable = 1)
        THROW 51013, N'dbo.HoaDon.tenNhanVienSnapshot phải NVARCHAR(100) NULL.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.HoaDon') AND c.name = N'tenKhachHangSnapshot'
          AND t.name = N'nvarchar' AND c.max_length = 200 AND c.is_nullable = 1)
        THROW 51014, N'dbo.HoaDon.tenKhachHangSnapshot phải NVARCHAR(100) NULL.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.ChiTietHoaDon') AND c.name = N'tenSPSnapshot'
          AND t.name = N'nvarchar' AND c.max_length = 400 AND c.is_nullable = 1)
        THROW 51015, N'dbo.ChiTietHoaDon.tenSPSnapshot phải NVARCHAR(200) NULL.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.ChiTietHoaDon') AND c.name = N'mauSacSnapshot'
          AND t.name = N'nvarchar' AND c.max_length = 100 AND c.is_nullable = 1)
        THROW 51016, N'dbo.ChiTietHoaDon.mauSacSnapshot phải NVARCHAR(50) NULL.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.ChiTietHoaDon') AND c.name = N'kichCoSnapshot'
          AND t.name = N'nvarchar' AND c.max_length = 40 AND c.is_nullable = 1)
        THROW 51017, N'dbo.ChiTietHoaDon.kichCoSnapshot phải NVARCHAR(20) NULL.', 1;

    COMMIT TRANSACTION;
    PRINT N'S11 migration hoàn tất hoặc schema đã đúng; dữ liệu hóa đơn cũ được giữ nguyên.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
