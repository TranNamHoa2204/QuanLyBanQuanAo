USE [QuanLyCuaHangQuanAo];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET LOCK_TIMEOUT 15000;

IF DB_NAME() <> N'QuanLyCuaHangQuanAo'
    THROW 51090, N'Sai database khi chuẩn hóa mã.', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @sql nvarchar(max), @table sysname, @column sysname, @prefix varchar(12),
            @entityNo int, @phase int, @from sysname, @to sysname, @next bigint, @locked bigint;

    CREATE TABLE #Types (entityNo int PRIMARY KEY, tableName sysname, columnName sysname, prefix varchar(12));
    INSERT #Types VALUES
        (1,'NhanVien','maNV','NV'), (2,'NhaCungCap','maNCC','NCC'),
        (3,'PhieuNhap','maPhieuNhap','PHIEUNHAP'), (4,'KhachHang','maKH','KH'),
        (5,'SanPham','maSP','SP'), (6,'BienThe','maBienThe','BT'),
        (7,'LoaiSanPham','maLoai','LSP'), (8,'ThuongHieu','maThuongHieu','TH'),
        (9,'KhuyenMai','maKM','KM'), (10,'HoaDon','maHD','HD'), (11,'Thue','maThue','THUE');

    -- Giữ khóa bảng đến khi đổi xong khóa chính và toàn bộ khóa ngoại.
    DECLARE locks CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM sys.tables WHERE schema_id = SCHEMA_ID('dbo') ORDER BY name;
    OPEN locks;
    FETCH NEXT FROM locks INTO @table;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @sql = N'SELECT @n=COUNT_BIG(*) FROM dbo.' + QUOTENAME(@table) + N' WITH (TABLOCKX,HOLDLOCK);';
        EXEC sys.sp_executesql @sql, N'@n bigint OUTPUT', @locked OUTPUT;
        FETCH NEXT FROM locks INTO @table;
    END;
    CLOSE locks;
    DEALLOCATE locks;

    IF OBJECT_ID(N'dbo.Seq_NV', N'SO') IS NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE is_disabled = 1 OR is_not_trusted = 1)
            THROW 51091, N'Cần kiểm tra các khóa ngoại trước khi đổi mã.', 1;

        CREATE TABLE #Map (tableName sysname, oldCode varchar(20), newCode varchar(20), tempCode varchar(20),
            PRIMARY KEY (tableName,oldCode), UNIQUE (tableName,newCode), UNIQUE (tableName,tempCode));
        DECLARE types CURSOR LOCAL FAST_FORWARD FOR SELECT entityNo,tableName,columnName,prefix FROM #Types ORDER BY entityNo;
        OPEN types;
        FETCH NEXT FROM types INTO @entityNo,@table,@column,@prefix;
        WHILE @@FETCH_STATUS = 0
        BEGIN
            SET @sql = N'INSERT #Map SELECT @t, oldCode, @p + CASE WHEN n<1000 THEN RIGHT(''000''+CONVERT(varchar(17),n),3) '
                + N'ELSE CONVERT(varchar(17),n) END, ''ZZ''+RIGHT(''00''+CONVERT(varchar(2),@i),2)'
                + N'+RIGHT(REPLICATE(''0'',16)+CONVERT(varchar(16),n),16) FROM '
                + N'(SELECT ' + QUOTENAME(@column) + N' oldCode, ROW_NUMBER() OVER(ORDER BY '
                + QUOTENAME(@column) + N') n FROM dbo.' + QUOTENAME(@table) + N') numbered;';
            EXEC sys.sp_executesql @sql, N'@t sysname,@p varchar(12),@i int',@table,@prefix,@entityNo;
            FETCH NEXT FROM types INTO @entityNo,@table,@column,@prefix;
        END;
        CLOSE types;
        DEALLOCATE types;
        IF EXISTS (SELECT 1 FROM #Map m JOIN #Map original ON original.tableName=m.tableName AND original.oldCode=m.tempCode)
            THROW 51092, N'Mã tạm bị trùng. Đã hủy toàn bộ chuyển đổi.', 1;

        SELECT OBJECT_NAME(fk.parent_object_id) tableName, fk.name constraintName INTO #ForeignKeys
        FROM sys.foreign_keys fk;
        CREATE TABLE #Columns (tableName sysname,columnName sysname,mappingTable sysname);
        INSERT #Columns SELECT tableName,columnName,tableName FROM #Types;
        INSERT #Columns
        SELECT DISTINCT OBJECT_NAME(fkc.parent_object_id),child.name,parent.tableName
        FROM sys.foreign_key_columns fkc
        JOIN sys.columns child ON child.object_id=fkc.parent_object_id AND child.column_id=fkc.parent_column_id
        JOIN sys.columns ref ON ref.object_id=fkc.referenced_object_id AND ref.column_id=fkc.referenced_column_id
        JOIN #Types parent ON parent.tableName=OBJECT_NAME(fkc.referenced_object_id) AND parent.columnName=ref.name;

        DECLARE constraints CURSOR LOCAL FAST_FORWARD FOR SELECT tableName,constraintName FROM #ForeignKeys;
        OPEN constraints;
        FETCH NEXT FROM constraints INTO @table,@column;
        WHILE @@FETCH_STATUS=0
        BEGIN
            SET @sql=N'ALTER TABLE dbo.'+QUOTENAME(@table)+N' NOCHECK CONSTRAINT '+QUOTENAME(@column)+N';';
            EXEC sys.sp_executesql @sql;
            FETCH NEXT FROM constraints INTO @table,@column;
        END;
        CLOSE constraints;
        DEALLOCATE constraints;

        SET @phase=0;
        WHILE @phase<2
        BEGIN
            SET @from=CASE WHEN @phase=0 THEN 'oldCode' ELSE 'tempCode' END;
            SET @to=CASE WHEN @phase=0 THEN 'tempCode' ELSE 'newCode' END;
            DECLARE columnsToUpdate CURSOR LOCAL FAST_FORWARD FOR SELECT DISTINCT tableName,columnName,mappingTable FROM #Columns;
            OPEN columnsToUpdate;
            FETCH NEXT FROM columnsToUpdate INTO @table,@column,@prefix;
            WHILE @@FETCH_STATUS=0
            BEGIN
                SET @sql=N'UPDATE target SET '+QUOTENAME(@column)+N'=m.'+QUOTENAME(@to)
                    +N' FROM dbo.'+QUOTENAME(@table)+N' target JOIN #Map m ON m.tableName=@t AND target.'
                    +QUOTENAME(@column)+N'=m.'+QUOTENAME(@from)+N';';
                EXEC sys.sp_executesql @sql,N'@t sysname',@prefix;
                FETCH NEXT FROM columnsToUpdate INTO @table,@column,@prefix;
            END;
            CLOSE columnsToUpdate;
            DEALLOCATE columnsToUpdate;

            SET @sql=N'UPDATE l SET thamChieu=m.'+QUOTENAME(@to)+N' FROM dbo.LichSuKho l JOIN #Map m '
                +N'ON l.thamChieu=m.'+QUOTENAME(@from)+N' AND ((m.tableName=''HoaDon'' AND l.nguon=''BAN_HANG'') '
                +N'OR (m.tableName=''PhieuNhap'' AND l.nguon=''NHAP_HANG''));';
            EXEC sys.sp_executesql @sql;
            DECLARE @before varchar(20),@after varchar(20);
            DECLARE ledgerCodes CURSOR LOCAL FAST_FORWARD FOR
                SELECT CASE WHEN @phase=0 THEN oldCode ELSE tempCode END,
                       CASE WHEN @phase=0 THEN tempCode ELSE newCode END
                FROM #Map WHERE tableName IN ('HoaDon','PhieuNhap','BienThe');
            OPEN ledgerCodes;
            FETCH NEXT FROM ledgerCodes INTO @before,@after;
            WHILE @@FETCH_STATUS=0
            BEGIN
                UPDATE dbo.LichSuKho SET operationId=SUBSTRING(
                    REPLACE(':'+operationId+':', ':'+@before+':', ':'+@after+':'),2,
                    LEN(REPLACE(':'+operationId+':', ':'+@before+':', ':'+@after+':'))-2)
                WHERE CHARINDEX(':'+@before+':', ':'+operationId+':')>0;
                UPDATE dbo.LichSuKho SET lyDo=N'Nhập hàng theo phiếu '+@after
                WHERE lyDo=N'Nhập hàng theo phiếu '+@before;
                FETCH NEXT FROM ledgerCodes INTO @before,@after;
            END;
            CLOSE ledgerCodes;
            DEALLOCATE ledgerCodes;
            SET @phase+=1;
        END;

        DECLARE restoreConstraints CURSOR LOCAL FAST_FORWARD FOR SELECT tableName,constraintName FROM #ForeignKeys;
        OPEN restoreConstraints;
        FETCH NEXT FROM restoreConstraints INTO @table,@column;
        WHILE @@FETCH_STATUS=0
        BEGIN
            SET @sql=N'ALTER TABLE dbo.'+QUOTENAME(@table)+N' WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@column)+N';';
            EXEC sys.sp_executesql @sql;
            FETCH NEXT FROM restoreConstraints INTO @table,@column;
        END;
        CLOSE restoreConstraints;
        DEALLOCATE restoreConstraints;
        SELECT tableName,oldCode,newCode FROM #Map ORDER BY tableName,newCode;

        DECLARE sequences CURSOR LOCAL FAST_FORWARD FOR SELECT tableName,columnName,prefix FROM #Types;
        OPEN sequences;
        FETCH NEXT FROM sequences INTO @table,@column,@prefix;
        WHILE @@FETCH_STATUS=0
        BEGIN
            SET @sql=N'SELECT @n=COUNT_BIG(*)+1 FROM dbo.'+QUOTENAME(@table)+N';';
            EXEC sys.sp_executesql @sql,N'@n bigint OUTPUT',@next OUTPUT;
            SET @sql=N'CREATE SEQUENCE dbo.'+QUOTENAME('Seq_'+@prefix)+N' AS bigint START WITH '
                +CONVERT(varchar(20),@next)+N' INCREMENT BY 1 NO CYCLE;';
            EXEC sys.sp_executesql @sql;
            FETCH NEXT FROM sequences INTO @table,@column,@prefix;
        END;
        CLOSE sequences;
        DEALLOCATE sequences;
    END;

    -- Dọn chú thích thử nghiệm trong các trường tên hiển thị.
    DECLARE names CURSOR LOCAL FAST_FORWARD FOR
        SELECT 'NhaCungCap','tenNCC' UNION ALL SELECT 'KhuyenMai','tenKM'
        UNION ALL SELECT 'SanPham','tenSP' UNION ALL SELECT 'LoaiSanPham','tenLoai'
        UNION ALL SELECT 'ThuongHieu','tenThuongHieu' UNION ALL SELECT 'Thue','tenThue';
    OPEN names;
    FETCH NEXT FROM names INTO @table,@column;
    WHILE @@FETCH_STATUS=0
    BEGIN
        SET @sql=N'UPDATE dbo.'+QUOTENAME(@table)+N' SET '+QUOTENAME(@column)+N'=LTRIM(RTRIM(REPLACE(REPLACE(REPLACE('
            +QUOTENAME(@column)+N',N'' (dữ liệu demo)'',N''''),N'' (demo)'',N''''),N'' (tự sinh)'',N'''')));';
        EXEC sys.sp_executesql @sql;
        FETCH NEXT FROM names INTO @table,@column;
    END;
    CLOSE names;
    DEALLOCATE names;
    UPDATE dbo.Thue SET tenThue=N'Thuế suất '+CONVERT(nvarchar(20),phanTram)+N'%' WHERE tenThue LIKE N'%demo%';
    UPDATE dbo.HoaDon SET ghiChu=NULL WHERE ghiChu LIKE N'Dữ liệu demo bổ sung%';

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
