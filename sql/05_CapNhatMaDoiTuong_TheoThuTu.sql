/*
    CHUẨN HÓA MÃ CÁC BẢNG NGHIỆP VỤ — SQL SERVER / SSMS

    Mã đích:
      KhachHang KH001...       NhanVien NV001...
      SanPham SP001...         LoaiSanPham LSP001...
      ThuongHieu TH001...      BienThe BT0001...
      KhuyenMai KM001...

    Quy trình an toàn:
      1. Dừng ứng dụng Java và sao lưu database.
      2. Chạy nguyên script với @ApplyChanges = 0 để xem bảng ánh xạ.
      3. Nếu đúng, đổi @ApplyChanges thành 1 và chạy lại toàn bộ script.

    Script tạo mã mới theo thứ tự mã cũ tăng dần, cập nhật các khóa ngoại
    tìm được trong metadata SQL Server, và dùng mã trung gian để tránh va
    chạm khóa chính. Không đổi mã hóa đơn, thuế, username hay maCTHD.
*/
USE [QuanLyCuaHangQuanAo];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @ApplyChanges BIT = 0; -- Chỉ đổi thành 1 sau khi đã xem trước và sao lưu.

IF OBJECT_ID(N'tempdb..#FkPlan', N'U') IS NOT NULL DROP TABLE #FkPlan;
IF OBJECT_ID(N'tempdb..#CodeMap', N'U') IS NOT NULL DROP TABLE #CodeMap;

IF OBJECT_ID(N'dbo.KhachHang', N'U') IS NULL
   OR OBJECT_ID(N'dbo.NhanVien', N'U') IS NULL
   OR OBJECT_ID(N'dbo.SanPham', N'U') IS NULL
   OR OBJECT_ID(N'dbo.LoaiSanPham', N'U') IS NULL
   OR OBJECT_ID(N'dbo.ThuongHieu', N'U') IS NULL
   OR OBJECT_ID(N'dbo.BienThe', N'U') IS NULL
   OR OBJECT_ID(N'dbo.KhuyenMai', N'U') IS NULL
    THROW 52001, N'Thiếu một hoặc nhiều bảng cần chuẩn hóa mã.', 1;

CREATE TABLE #CodeMap (
    kind VARCHAR(4) NOT NULL,
    parentSchema SYSNAME NOT NULL,
    parentTable SYSNAME NOT NULL,
    parentColumn SYSNAME NOT NULL,
    oldCode VARCHAR(20) NOT NULL,
    tempCode VARCHAR(20) NOT NULL,
    newCode VARCHAR(20) NOT NULL,
    CONSTRAINT PK_CodeMap PRIMARY KEY (kind, oldCode),
    CONSTRAINT UQ_CodeMap_Temp UNIQUE (kind, tempCode),
    CONSTRAINT UQ_CodeMap_New UNIQUE (kind, newCode)
);

;WITH numbered AS (
    SELECT maKH, ROW_NUMBER() OVER (ORDER BY maKH) AS seq
    FROM dbo.KhachHang
)
INSERT #CodeMap
SELECT 'KH', 'dbo', 'KhachHang', 'maKH', maKH,
       'X' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 19),
       'KH' + REPLICATE('0', CASE WHEN LEN(CONVERT(VARCHAR(20), seq)) < 3
                                 THEN 3 - LEN(CONVERT(VARCHAR(20), seq)) ELSE 0 END)
            + CONVERT(VARCHAR(20), seq)
FROM numbered;

;WITH numbered AS (
    SELECT maNV, ROW_NUMBER() OVER (ORDER BY maNV) AS seq
    FROM dbo.NhanVien
)
INSERT #CodeMap
SELECT 'NV', 'dbo', 'NhanVien', 'maNV', maNV,
       'X' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 19),
       'NV' + REPLICATE('0', CASE WHEN LEN(CONVERT(VARCHAR(20), seq)) < 3
                                 THEN 3 - LEN(CONVERT(VARCHAR(20), seq)) ELSE 0 END)
            + CONVERT(VARCHAR(20), seq)
FROM numbered;

;WITH numbered AS (
    SELECT maSP, ROW_NUMBER() OVER (ORDER BY maSP) AS seq
    FROM dbo.SanPham
)
INSERT #CodeMap
SELECT 'SP', 'dbo', 'SanPham', 'maSP', maSP,
       'X' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 19),
       'SP' + REPLICATE('0', CASE WHEN LEN(CONVERT(VARCHAR(20), seq)) < 3
                                 THEN 3 - LEN(CONVERT(VARCHAR(20), seq)) ELSE 0 END)
            + CONVERT(VARCHAR(20), seq)
FROM numbered;

;WITH numbered AS (
    SELECT maLoai, ROW_NUMBER() OVER (ORDER BY maLoai) AS seq
    FROM dbo.LoaiSanPham
)
INSERT #CodeMap
SELECT 'LSP', 'dbo', 'LoaiSanPham', 'maLoai', maLoai,
       'X' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 19),
       'LSP' + REPLICATE('0', CASE WHEN LEN(CONVERT(VARCHAR(20), seq)) < 3
                                  THEN 3 - LEN(CONVERT(VARCHAR(20), seq)) ELSE 0 END)
             + CONVERT(VARCHAR(20), seq)
FROM numbered;

;WITH numbered AS (
    SELECT maThuongHieu, ROW_NUMBER() OVER (ORDER BY maThuongHieu) AS seq
    FROM dbo.ThuongHieu
)
INSERT #CodeMap
SELECT 'TH', 'dbo', 'ThuongHieu', 'maThuongHieu', maThuongHieu,
       'X' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 19),
       'TH' + REPLICATE('0', CASE WHEN LEN(CONVERT(VARCHAR(20), seq)) < 3
                                 THEN 3 - LEN(CONVERT(VARCHAR(20), seq)) ELSE 0 END)
            + CONVERT(VARCHAR(20), seq)
FROM numbered;

;WITH numbered AS (
    SELECT maBienThe, ROW_NUMBER() OVER (ORDER BY maBienThe) AS seq
    FROM dbo.BienThe
)
INSERT #CodeMap
SELECT 'BT', 'dbo', 'BienThe', 'maBienThe', maBienThe,
       'X' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 19),
       'BT' + REPLICATE('0', CASE WHEN LEN(CONVERT(VARCHAR(20), seq)) < 4
                                 THEN 4 - LEN(CONVERT(VARCHAR(20), seq)) ELSE 0 END)
            + CONVERT(VARCHAR(20), seq)
FROM numbered;

;WITH numbered AS (
    SELECT maKM, ROW_NUMBER() OVER (ORDER BY maKM) AS seq
    FROM dbo.KhuyenMai
)
INSERT #CodeMap
SELECT 'KM', 'dbo', 'KhuyenMai', 'maKM', maKM,
       'X' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 19),
       'KM' + REPLICATE('0', CASE WHEN LEN(CONVERT(VARCHAR(20), seq)) < 3
                                 THEN 3 - LEN(CONVERT(VARCHAR(20), seq)) ELSE 0 END)
            + CONVERT(VARCHAR(20), seq)
FROM numbered;

IF EXISTS (SELECT 1 FROM #CodeMap WHERE LEN(newCode) > 20 OR LEN(tempCode) > 20)
    THROW 52002, N'Mã tạo ra vượt quá giới hạn 20 ký tự.', 1;

SELECT kind AS NhomMa, oldCode AS MaHienTai, newCode AS MaMoi
FROM #CodeMap
ORDER BY kind, newCode;

SELECT kind AS NhomMa, COUNT_BIG(*) AS SoBanGhi
FROM #CodeMap
GROUP BY kind
ORDER BY kind;

IF @ApplyChanges = 0
BEGIN
    PRINT N'Chế độ xem trước: chưa có dữ liệu nào bị thay đổi. Nếu ánh xạ đúng, đổi @ApplyChanges thành 1 rồi chạy lại.';
    DROP TABLE #CodeMap;
    RETURN;
END;

/* Tìm mọi FK đang trỏ đến các khóa được đổi, gồm cả bảng AnhSanPham nếu có. */
CREATE TABLE #FkPlan (
    fkObjectId INT NOT NULL,
    fkName SYSNAME NOT NULL,
    childSchema SYSNAME NOT NULL,
    childTable SYSNAME NOT NULL,
    childColumn SYSNAME NOT NULL,
    kind VARCHAR(4) NOT NULL,
    parentSchema SYSNAME NOT NULL,
    parentTable SYSNAME NOT NULL,
    parentColumn SYSNAME NOT NULL
);

INSERT #FkPlan
SELECT fk.object_id, fk.name, SCHEMA_NAME(ct.schema_id), ct.name, cc.name,
       m.kind, SCHEMA_NAME(pt.schema_id), pt.name, pc.name
FROM sys.foreign_keys fk
JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
JOIN sys.tables ct ON ct.object_id = fkc.parent_object_id
JOIN sys.columns cc ON cc.object_id = fkc.parent_object_id AND cc.column_id = fkc.parent_column_id
JOIN sys.tables pt ON pt.object_id = fkc.referenced_object_id
JOIN sys.columns pc ON pc.object_id = fkc.referenced_object_id AND pc.column_id = fkc.referenced_column_id
JOIN #CodeMap m ON m.parentSchema = SCHEMA_NAME(pt.schema_id)
               AND m.parentTable = pt.name AND m.parentColumn = pc.name;

IF EXISTS (
    SELECT 1
    FROM #FkPlan p
    JOIN sys.foreign_keys fk ON fk.object_id = p.fkObjectId
    WHERE fk.is_disabled = 1
)
    THROW 52003, N'Có khóa ngoại liên quan đang bị disable. Hãy kiểm tra schema trước khi chạy migration.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @sql NVARCHAR(MAX);
    DECLARE @childSchema SYSNAME, @childTable SYSNAME, @childColumn SYSNAME;
    DECLARE @parentSchema SYSNAME, @parentTable SYSNAME, @parentColumn SYSNAME;
    DECLARE @kind VARCHAR(4), @fkName SYSNAME;

    /* Tạm ngừng kiểm tra FK trong lúc hoán đổi khóa; sẽ kiểm tra lại trước COMMIT. */
    DECLARE disable_fk CURSOR LOCAL FAST_FORWARD FOR
        SELECT DISTINCT fkName, childSchema, childTable
        FROM #FkPlan;
    OPEN disable_fk;
    FETCH NEXT FROM disable_fk INTO @fkName, @childSchema, @childTable;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @sql = N'ALTER TABLE ' + QUOTENAME(@childSchema) + N'.' + QUOTENAME(@childTable)
                 + N' NOCHECK CONSTRAINT ' + QUOTENAME(@fkName) + N';';
        EXEC sys.sp_executesql @sql;
        FETCH NEXT FROM disable_fk INTO @fkName, @childSchema, @childTable;
    END;
    CLOSE disable_fk;
    DEALLOCATE disable_fk;

    /* Giai đoạn 1: chuyển các FK con sang mã trung gian. */
    DECLARE update_child_to_temp CURSOR LOCAL FAST_FORWARD FOR
        SELECT DISTINCT childSchema, childTable, childColumn, kind
        FROM #FkPlan;
    OPEN update_child_to_temp;
    FETCH NEXT FROM update_child_to_temp INTO @childSchema, @childTable, @childColumn, @kind;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @sql = N'UPDATE c SET ' + QUOTENAME(@childColumn) + N' = m.tempCode'
                 + N' FROM ' + QUOTENAME(@childSchema) + N'.' + QUOTENAME(@childTable) + N' AS c'
                 + N' JOIN #CodeMap AS m ON m.kind = @pKind AND c.' + QUOTENAME(@childColumn) + N' = m.oldCode;';
        EXEC sys.sp_executesql @sql, N'@pKind VARCHAR(4)', @pKind = @kind;
        FETCH NEXT FROM update_child_to_temp INTO @childSchema, @childTable, @childColumn, @kind;
    END;
    CLOSE update_child_to_temp;
    DEALLOCATE update_child_to_temp;

    /* Giai đoạn 2: đổi toàn bộ PK sang mã trung gian để giải phóng mã đích. */
    DECLARE update_parent_to_temp CURSOR LOCAL FAST_FORWARD FOR
        SELECT DISTINCT parentSchema, parentTable, parentColumn, kind
        FROM #CodeMap;
    OPEN update_parent_to_temp;
    FETCH NEXT FROM update_parent_to_temp INTO @parentSchema, @parentTable, @parentColumn, @kind;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @sql = N'UPDATE p SET ' + QUOTENAME(@parentColumn) + N' = m.tempCode'
                 + N' FROM ' + QUOTENAME(@parentSchema) + N'.' + QUOTENAME(@parentTable) + N' AS p'
                 + N' JOIN #CodeMap AS m ON m.kind = @pKind AND p.' + QUOTENAME(@parentColumn) + N' = m.oldCode;';
        EXEC sys.sp_executesql @sql, N'@pKind VARCHAR(4)', @pKind = @kind;
        FETCH NEXT FROM update_parent_to_temp INTO @parentSchema, @parentTable, @parentColumn, @kind;
    END;
    CLOSE update_parent_to_temp;
    DEALLOCATE update_parent_to_temp;

    /* Giai đoạn 3: gán mã chuẩn cho PK, sau đó đổi FK con sang mã chuẩn. */
    DECLARE update_parent_to_new CURSOR LOCAL FAST_FORWARD FOR
        SELECT DISTINCT parentSchema, parentTable, parentColumn, kind
        FROM #CodeMap;
    OPEN update_parent_to_new;
    FETCH NEXT FROM update_parent_to_new INTO @parentSchema, @parentTable, @parentColumn, @kind;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @sql = N'UPDATE p SET ' + QUOTENAME(@parentColumn) + N' = m.newCode'
                 + N' FROM ' + QUOTENAME(@parentSchema) + N'.' + QUOTENAME(@parentTable) + N' AS p'
                 + N' JOIN #CodeMap AS m ON m.kind = @pKind AND p.' + QUOTENAME(@parentColumn) + N' = m.tempCode;';
        EXEC sys.sp_executesql @sql, N'@pKind VARCHAR(4)', @pKind = @kind;
        FETCH NEXT FROM update_parent_to_new INTO @parentSchema, @parentTable, @parentColumn, @kind;
    END;
    CLOSE update_parent_to_new;
    DEALLOCATE update_parent_to_new;

    DECLARE update_child_to_new CURSOR LOCAL FAST_FORWARD FOR
        SELECT DISTINCT childSchema, childTable, childColumn, kind
        FROM #FkPlan;
    OPEN update_child_to_new;
    FETCH NEXT FROM update_child_to_new INTO @childSchema, @childTable, @childColumn, @kind;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @sql = N'UPDATE c SET ' + QUOTENAME(@childColumn) + N' = m.newCode'
                 + N' FROM ' + QUOTENAME(@childSchema) + N'.' + QUOTENAME(@childTable) + N' AS c'
                 + N' JOIN #CodeMap AS m ON m.kind = @pKind AND c.' + QUOTENAME(@childColumn) + N' = m.tempCode;';
        EXEC sys.sp_executesql @sql, N'@pKind VARCHAR(4)', @pKind = @kind;
        FETCH NEXT FROM update_child_to_new INTO @childSchema, @childTable, @childColumn, @kind;
    END;
    CLOSE update_child_to_new;
    DEALLOCATE update_child_to_new;

    /* Kiểm tra lại toàn bộ FK đã tạm ngừng và khôi phục trạng thái trusted. */
    DECLARE enable_fk CURSOR LOCAL FAST_FORWARD FOR
        SELECT DISTINCT fkName, childSchema, childTable
        FROM #FkPlan;
    OPEN enable_fk;
    FETCH NEXT FROM enable_fk INTO @fkName, @childSchema, @childTable;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @sql = N'ALTER TABLE ' + QUOTENAME(@childSchema) + N'.' + QUOTENAME(@childTable)
                 + N' WITH CHECK CHECK CONSTRAINT ' + QUOTENAME(@fkName) + N';';
        EXEC sys.sp_executesql @sql;
        FETCH NEXT FROM enable_fk INTO @fkName, @childSchema, @childTable;
    END;
    CLOSE enable_fk;
    DEALLOCATE enable_fk;

    COMMIT TRANSACTION;
    PRINT N'Đã chuẩn hóa mã và cập nhật các khóa ngoại liên quan.';
    SELECT kind AS NhomMa, oldCode AS MaCu, newCode AS MaMoi
    FROM #CodeMap
    ORDER BY kind, newCode;
    DROP TABLE #FkPlan;
    DROP TABLE #CodeMap;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    IF OBJECT_ID(N'tempdb..#FkPlan', N'U') IS NOT NULL DROP TABLE #FkPlan;
    IF OBJECT_ID(N'tempdb..#CodeMap', N'U') IS NOT NULL DROP TABLE #CodeMap;
    THROW;
END CATCH;
GO
