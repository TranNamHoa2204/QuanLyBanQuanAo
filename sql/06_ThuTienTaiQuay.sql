-- S18 / FR12: ghi nhận phương thức thu tại quầy; không tích hợp/xác minh ngân hàng.
-- Chạy sau baseline, 04_HoaDonSnapshotVaThoiDiemThanhToan.sql và 05_LichSuBienDongKho.sql.
-- Các cột nullable để giữ nguyên hóa đơn lịch sử; không backfill phương thức thu.
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF DB_NAME() <> N'QuanLyCuaHangQuanAo'
        THROW 51060, N'S18 yêu cầu database demo QuanLyCuaHangQuanAo; đã dừng trước khi thay đổi schema.', 1;
    IF OBJECT_ID(N'dbo.HoaDon', N'U') IS NULL OR OBJECT_ID(N'dbo.LichSuKho', N'U') IS NULL
        THROW 51061, N'Cần có baseline HoaDon và migration S15 LichSuKho trước S18.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.HoaDon') AND name = N'ngayThanhToan')
        THROW 51062, N'Cần áp dụng migration S11 trước S18.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.HoaDon') AND name = N'phuongThucThanhToan')
        ALTER TABLE dbo.HoaDon ADD phuongThucThanhToan VARCHAR(20) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.HoaDon') AND name = N'tienKhachDua')
        ALTER TABLE dbo.HoaDon ADD tienKhachDua DECIMAL(18,2) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.HoaDon') AND name = N'tienThua')
        ALTER TABLE dbo.HoaDon ADD tienThua DECIMAL(18,2) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.HoaDon') AND name = N'nhanVienXacNhanChuyenKhoan')
        ALTER TABLE dbo.HoaDon ADD nhanVienXacNhanChuyenKhoan BIT NULL;

    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.HoaDon') AND c.name = N'phuongThucThanhToan'
          AND t.name = N'varchar' AND c.max_length = 20 AND c.is_nullable = 1)
        THROW 51063, N'dbo.HoaDon.phuongThucThanhToan phải VARCHAR(20) NULL.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.HoaDon') AND c.name = N'tienKhachDua'
          AND t.name = N'decimal' AND c.precision = 18 AND c.scale = 2 AND c.is_nullable = 1)
        THROW 51064, N'dbo.HoaDon.tienKhachDua phải DECIMAL(18,2) NULL.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.HoaDon') AND c.name = N'tienThua'
          AND t.name = N'decimal' AND c.precision = 18 AND c.scale = 2 AND c.is_nullable = 1)
        THROW 51065, N'dbo.HoaDon.tienThua phải DECIMAL(18,2) NULL.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.HoaDon') AND c.name = N'nhanVienXacNhanChuyenKhoan'
          AND t.name = N'bit' AND c.is_nullable = 1)
        THROW 51066, N'dbo.HoaDon.nhanVienXacNhanChuyenKhoan phải BIT NULL.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.HoaDon')
        AND name = N'CK_HoaDon_ThuTien')
        ALTER TABLE dbo.HoaDon ADD CONSTRAINT CK_HoaDon_ThuTien CHECK (
            (phuongThucThanhToan IS NULL AND tienKhachDua IS NULL AND tienThua IS NULL
                AND nhanVienXacNhanChuyenKhoan IS NULL)
            OR (trangThai = 'DA_THANH_TOAN' AND phuongThucThanhToan = 'TIEN_MAT'
                AND tienKhachDua IS NOT NULL AND tienThua IS NOT NULL
                AND tienKhachDua >= 0 AND tienThua >= 0
                AND tienKhachDua >= tongTien AND tienThua = tienKhachDua - tongTien
                AND nhanVienXacNhanChuyenKhoan IS NULL)
            OR (trangThai = 'DA_THANH_TOAN' AND phuongThucThanhToan = 'CHUYEN_KHOAN'
                AND tienKhachDua IS NULL AND tienThua IS NULL AND nhanVienXacNhanChuyenKhoan = 1)
        );

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.HoaDon')
        AND name = N'CK_HoaDon_ThuTien' AND is_disabled = 0 AND is_not_trusted = 0
        AND definition LIKE N'%phuongThucThanhToan%'
        AND definition LIKE N'%TIEN_MAT%' AND definition LIKE N'%CHUYEN_KHOAN%'
        AND definition LIKE N'%tienKhachDua%' AND definition LIKE N'%tienThua%'
        AND definition LIKE N'%nhanVienXacNhanChuyenKhoan%' AND definition LIKE N'%tongTien%')
        THROW 51067, N'Thiếu hoặc chưa kiểm chứng CK_HoaDon_ThuTien.', 1;

    COMMIT TRANSACTION;
    PRINT N'S18 migration hoàn tất hoặc schema đã đúng; hóa đơn lịch sử không được gán phương thức thu.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
