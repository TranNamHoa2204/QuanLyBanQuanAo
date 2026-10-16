-- S15 / FR10: ledger append-only cho mọi biến động tồn kho.
-- Chạy sau baseline 12 bảng và 04_HoaDonSnapshotVaThoiDiemThanhToan.sql.
-- Chỉ áp dụng cho database demo có tên cố định; không xóa hay reset dữ liệu.
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF DB_NAME() <> N'QuanLyCuaHangQuanAo'
        THROW 51020, N'S15 yêu cầu database demo QuanLyCuaHangQuanAo; đã dừng trước khi thay đổi schema.', 1;
    IF OBJECT_ID(N'dbo.BienThe', N'U') IS NULL OR OBJECT_ID(N'dbo.NhanVien', N'U') IS NULL
        THROW 51021, N'Không tìm thấy đủ bảng BienThe và NhanVien của baseline.', 1;

    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.BienThe') AND c.name = N'maBienThe'
          AND t.name = N'varchar' AND c.max_length = 20 AND c.is_nullable = 0)
        THROW 51022, N'dbo.BienThe.maBienThe phải VARCHAR(20) NOT NULL.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.BienThe') AND c.name = N'soLuongTon'
          AND t.name = N'int' AND c.is_nullable = 0)
        THROW 51023, N'dbo.BienThe.soLuongTon phải INT NOT NULL.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.BienThe') AND c.name = N'phienBan'
          AND t.name = N'bigint' AND c.is_nullable = 0)
        THROW 51024, N'dbo.BienThe.phienBan phải BIGINT NOT NULL.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.NhanVien') AND c.name = N'maNV'
          AND t.name = N'varchar' AND c.max_length = 20 AND c.is_nullable = 0)
        THROW 51025, N'dbo.NhanVien.maNV phải VARCHAR(20) NOT NULL.', 1;

    DECLARE @created bit = 0;
    IF OBJECT_ID(N'dbo.LichSuKho', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.LichSuKho (
            maLichSuKho BIGINT IDENTITY(1,1) NOT NULL,
            maBienThe VARCHAR(20) NOT NULL,
            maNV VARCHAR(20) NULL,
            thoiGian DATETIME2(0) NOT NULL,
            nguon VARCHAR(30) NOT NULL,
            thamChieu VARCHAR(100) NOT NULL,
            operationId VARCHAR(100) NOT NULL,
            soLuongTruoc INT NOT NULL,
            soLuongThayDoi INT NOT NULL,
            soLuongSau INT NOT NULL,
            phienBanTruoc BIGINT NULL,
            tenNguoiThaoTacSnapshot NVARCHAR(100) NULL,
            lyDo NVARCHAR(500) NOT NULL,
            CONSTRAINT PK_LichSuKho PRIMARY KEY (maLichSuKho),
            CONSTRAINT FK_LichSuKho_BienThe FOREIGN KEY (maBienThe) REFERENCES dbo.BienThe(maBienThe),
            CONSTRAINT FK_LichSuKho_NhanVien FOREIGN KEY (maNV) REFERENCES dbo.NhanVien(maNV),
            CONSTRAINT UQ_LichSuKho_OperationId UNIQUE (operationId),
            CONSTRAINT CK_LichSuKho_Nguon CHECK (nguon IN ('TON_MO_DAU','KHOI_TAO_BIEN_THE','BAN_HANG','DIEU_CHINH_TON')),
            CONSTRAINT CK_LichSuKho_SoLuong CHECK (soLuongTruoc >= 0 AND soLuongSau >= 0
                AND CONVERT(BIGINT, soLuongTruoc) + CONVERT(BIGINT, soLuongThayDoi) = CONVERT(BIGINT, soLuongSau)),
            CONSTRAINT CK_LichSuKho_OperationId CHECK (LEN(LTRIM(RTRIM(operationId))) > 0),
            CONSTRAINT CK_LichSuKho_ThamChieu CHECK (LEN(LTRIM(RTRIM(thamChieu))) > 0),
            CONSTRAINT CK_LichSuKho_LyDo CHECK (LEN(LTRIM(RTRIM(lyDo))) > 0)
        );
        CREATE INDEX IX_LichSuKho_BienThe_ThoiGian ON dbo.LichSuKho(maBienThe, thoiGian DESC, maLichSuKho DESC);
        CREATE INDEX IX_LichSuKho_Nguon_ThoiGian ON dbo.LichSuKho(nguon, thoiGian DESC, maLichSuKho DESC);
        CREATE INDEX IX_LichSuKho_NhanVien_ThoiGian ON dbo.LichSuKho(maNV, thoiGian DESC, maLichSuKho DESC);
        SET @created = 1;
    END;

    -- If an object with this name predates S15, do not accept it by name alone.
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.LichSuKho') AND c.name = N'maLichSuKho'
          AND t.name = N'bigint' AND c.is_nullable = 0 AND c.is_identity = 1)
        THROW 51026, N'LichSuKho.maLichSuKho phải BIGINT IDENTITY NOT NULL.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.LichSuKho') AND c.name = N'maBienThe'
          AND t.name = N'varchar' AND c.max_length = 20 AND c.is_nullable = 0)
        THROW 51027, N'LichSuKho.maBienThe phải VARCHAR(20) NOT NULL.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.LichSuKho') AND c.name = N'maNV'
          AND t.name = N'varchar' AND c.max_length = 20 AND c.is_nullable = 1)
        THROW 51028, N'LichSuKho.maNV phải VARCHAR(20) NULL.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.LichSuKho') AND c.name = N'thoiGian'
          AND t.name = N'datetime2' AND c.scale = 0 AND c.is_nullable = 0)
        THROW 51029, N'LichSuKho.thoiGian phải DATETIME2(0) NOT NULL.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.LichSuKho') AND c.name = N'nguon'
          AND t.name = N'varchar' AND c.max_length = 30 AND c.is_nullable = 0)
        THROW 51030, N'LichSuKho.nguon phải VARCHAR(30) NOT NULL.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.LichSuKho') AND c.name = N'thamChieu'
          AND t.name = N'varchar' AND c.max_length = 100 AND c.is_nullable = 0)
        THROW 51031, N'LichSuKho.thamChieu phải VARCHAR(100) NOT NULL.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.LichSuKho') AND c.name = N'operationId'
          AND t.name = N'varchar' AND c.max_length = 100 AND c.is_nullable = 0)
        THROW 51032, N'LichSuKho.operationId phải VARCHAR(100) NOT NULL.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.LichSuKho') AND c.name = N'soLuongTruoc'
          AND t.name = N'int' AND c.is_nullable = 0)
        THROW 51033, N'LichSuKho.soLuongTruoc phải INT NOT NULL.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.LichSuKho') AND c.name = N'soLuongThayDoi'
          AND t.name = N'int' AND c.is_nullable = 0)
        THROW 51034, N'LichSuKho.soLuongThayDoi phải INT NOT NULL.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.LichSuKho') AND c.name = N'soLuongSau'
          AND t.name = N'int' AND c.is_nullable = 0)
        THROW 51035, N'LichSuKho.soLuongSau phải INT NOT NULL.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.LichSuKho') AND c.name = N'phienBanTruoc'
          AND t.name = N'bigint' AND c.is_nullable = 1)
        THROW 51036, N'LichSuKho.phienBanTruoc phải BIGINT NULL.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.LichSuKho') AND c.name = N'tenNguoiThaoTacSnapshot'
          AND t.name = N'nvarchar' AND c.max_length = 200 AND c.is_nullable = 1)
        THROW 51037, N'LichSuKho.tenNguoiThaoTacSnapshot phải NVARCHAR(100) NULL.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.LichSuKho') AND c.name = N'lyDo'
          AND t.name = N'nvarchar' AND c.max_length = 1000 AND c.is_nullable = 0)
        THROW 51038, N'LichSuKho.lyDo phải NVARCHAR(500) NOT NULL.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints kc JOIN sys.index_columns ic
        ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
        JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
        WHERE kc.parent_object_id = OBJECT_ID(N'dbo.LichSuKho')
          AND kc.name = N'PK_LichSuKho' AND kc.type = N'PK' AND ic.key_ordinal = 1
          AND c.name = N'maLichSuKho'
          AND (SELECT COUNT(*) FROM sys.index_columns allKeys WHERE allKeys.object_id = kc.parent_object_id
               AND allKeys.index_id = kc.unique_index_id AND allKeys.key_ordinal > 0) = 1)
        THROW 51039, N'Thiếu khóa chính PK_LichSuKho.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints kc JOIN sys.index_columns ic
        ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
        JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
        WHERE kc.parent_object_id = OBJECT_ID(N'dbo.LichSuKho')
          AND kc.name = N'UQ_LichSuKho_OperationId' AND kc.type = N'UQ' AND ic.key_ordinal = 1
          AND c.name = N'operationId'
          AND (SELECT COUNT(*) FROM sys.index_columns allKeys WHERE allKeys.object_id = kc.parent_object_id
               AND allKeys.index_id = kc.unique_index_id AND allKeys.key_ordinal > 0) = 1)
        THROW 51040, N'Thiếu unique constraint UQ_LichSuKho_OperationId.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
        JOIN sys.columns pc ON pc.object_id = fkc.parent_object_id AND pc.column_id = fkc.parent_column_id
        JOIN sys.columns rc ON rc.object_id = fkc.referenced_object_id AND rc.column_id = fkc.referenced_column_id
        WHERE fk.parent_object_id = OBJECT_ID(N'dbo.LichSuKho') AND fk.name = N'FK_LichSuKho_BienThe'
          AND fk.referenced_object_id = OBJECT_ID(N'dbo.BienThe') AND pc.name = N'maBienThe' AND rc.name = N'maBienThe'
          AND fk.is_disabled = 0 AND fk.is_not_trusted = 0 AND fkc.constraint_column_id = 1
          AND (SELECT COUNT(*) FROM sys.foreign_key_columns allKeys WHERE allKeys.constraint_object_id = fk.object_id) = 1)
        THROW 51041, N'Thiếu hoặc chưa kiểm chứng FK_LichSuKho_BienThe.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
        JOIN sys.columns pc ON pc.object_id = fkc.parent_object_id AND pc.column_id = fkc.parent_column_id
        JOIN sys.columns rc ON rc.object_id = fkc.referenced_object_id AND rc.column_id = fkc.referenced_column_id
        WHERE fk.parent_object_id = OBJECT_ID(N'dbo.LichSuKho') AND fk.name = N'FK_LichSuKho_NhanVien'
          AND fk.referenced_object_id = OBJECT_ID(N'dbo.NhanVien') AND pc.name = N'maNV' AND rc.name = N'maNV'
          AND fk.is_disabled = 0 AND fk.is_not_trusted = 0 AND fkc.constraint_column_id = 1
          AND (SELECT COUNT(*) FROM sys.foreign_key_columns allKeys WHERE allKeys.constraint_object_id = fk.object_id) = 1)
        THROW 51042, N'Thiếu hoặc chưa kiểm chứng FK_LichSuKho_NhanVien.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.LichSuKho')
        AND name = N'CK_LichSuKho_Nguon' AND is_disabled = 0 AND is_not_trusted = 0
        AND definition LIKE N'%TON_MO_DAU%' AND definition LIKE N'%KHOI_TAO_BIEN_THE%'
        AND definition LIKE N'%BAN_HANG%' AND definition LIKE N'%DIEU_CHINH_TON%')
       OR NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.LichSuKho')
        AND name = N'CK_LichSuKho_SoLuong' AND is_disabled = 0 AND is_not_trusted = 0
        AND definition LIKE N'%soLuongTruoc%' AND definition LIKE N'%soLuongThayDoi%' AND definition LIKE N'%soLuongSau%')
       OR NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.LichSuKho')
        AND name = N'CK_LichSuKho_OperationId' AND is_disabled = 0 AND is_not_trusted = 0
        AND definition LIKE N'%operationId%')
       OR NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.LichSuKho')
        AND name = N'CK_LichSuKho_ThamChieu' AND is_disabled = 0 AND is_not_trusted = 0
        AND definition LIKE N'%thamChieu%')
       OR NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.LichSuKho')
        AND name = N'CK_LichSuKho_LyDo' AND is_disabled = 0 AND is_not_trusted = 0
        AND definition LIKE N'%lyDo%')
        THROW 51043, N'Thiếu CHECK constraint hợp lệ cho bảng LichSuKho.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id = OBJECT_ID(N'dbo.LichSuKho')
        AND i.name = N'IX_LichSuKho_BienThe_ThoiGian' AND i.is_disabled = 0
        AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 3
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=1 AND c.name=N'maBienThe')
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=2 AND c.name=N'thoiGian')
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=3 AND c.name=N'maLichSuKho'))
       OR NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id = OBJECT_ID(N'dbo.LichSuKho')
        AND i.name = N'IX_LichSuKho_Nguon_ThoiGian' AND i.is_disabled = 0
        AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 3
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=1 AND c.name=N'nguon')
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=2 AND c.name=N'thoiGian')
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=3 AND c.name=N'maLichSuKho'))
       OR NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id = OBJECT_ID(N'dbo.LichSuKho')
        AND i.name = N'IX_LichSuKho_NhanVien_ThoiGian' AND i.is_disabled = 0
        AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 3
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=1 AND c.name=N'maNV')
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=2 AND c.name=N'thoiGian')
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=3 AND c.name=N'maLichSuKho'))
        THROW 51044, N'Thiếu index tra cứu của bảng LichSuKho.', 1;

    IF @created = 1
    BEGIN
        -- Only current quantities are recorded as a conversion-time opening balance.
        -- No attempt is made to reconstruct past sales, purchases, or adjustments.
        INSERT dbo.LichSuKho (maBienThe, maNV, thoiGian, nguon, thamChieu, operationId,
                              soLuongTruoc, soLuongThayDoi, soLuongSau, phienBanTruoc,
                              tenNguoiThaoTacSnapshot, lyDo)
        SELECT b.maBienThe, NULL, CONVERT(DATETIME2(0), SYSDATETIME()), 'TON_MO_DAU',
               CONCAT('BASELINE:', b.maBienThe), CONCAT('S15:BASELINE:', b.maBienThe),
               0, b.soLuongTon, b.soLuongTon, b.phienBan, NULL,
               N'Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'
        FROM dbo.BienThe b;
    END
    ELSE IF EXISTS (
        SELECT 1 FROM dbo.BienThe b
        WHERE NOT EXISTS (SELECT 1 FROM dbo.LichSuKho l WHERE l.maBienThe = b.maBienThe))
        THROW 51045, N'Có biến thể không có ledger. Migration không tự bù dữ liệu khi chạy lại; cần kiểm tra nguyên nhân.', 1;

    COMMIT TRANSACTION;
    PRINT N'S15 migration hoàn tất hoặc schema ledger đã được xác minh; dữ liệu cũ được giữ nguyên.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
