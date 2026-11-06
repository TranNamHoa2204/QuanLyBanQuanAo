-- FR11: nhà cung cấp và phiếu nhập; tăng dần, không reset dữ liệu.
-- Chạy sau baseline và các migration 04, 05, 06 trên database demo.
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;
    IF DB_NAME() <> N'QuanLyCuaHangQuanAo'
        THROW 51070, N'Migration 07 chỉ dành cho database demo QuanLyCuaHangQuanAo.', 1;
    IF OBJECT_ID(N'dbo.NhanVien', N'U') IS NULL OR OBJECT_ID(N'dbo.BienThe', N'U') IS NULL
        THROW 51071, N'Cần baseline NhanVien và BienThe trước migration 07.', 1;
    IF OBJECT_ID(N'dbo.LichSuKho', N'U') IS NULL
        THROW 51072, N'Cần chạy migration 05 trước migration 07.', 1;

    IF OBJECT_ID(N'dbo.NhaCungCap', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.NhaCungCap (
            maNCC VARCHAR(20) NOT NULL CONSTRAINT PK_NhaCungCap PRIMARY KEY,
            tenNCC NVARCHAR(150) NOT NULL,
            sdt VARCHAR(20) NULL,
            email VARCHAR(254) NULL,
            diaChi NVARCHAR(255) NULL,
            trangThai BIT NOT NULL CONSTRAINT DF_NhaCungCap_TrangThai DEFAULT (1),
            phienBan BIGINT NOT NULL CONSTRAINT DF_NhaCungCap_PhienBan DEFAULT (0),
            CONSTRAINT CK_NhaCungCap_PhienBan CHECK (phienBan >= 0),
            CONSTRAINT CK_NhaCungCap_Ten CHECK (LEN(LTRIM(RTRIM(tenNCC))) > 0)
        );
        CREATE INDEX IX_NhaCungCap_TrangThai_Ten ON dbo.NhaCungCap(trangThai, tenNCC, maNCC);
    END;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N'dbo.NhaCungCap') AND c.name=N'maNCC' AND t.name=N'varchar' AND c.max_length=20 AND c.is_nullable=0)
       OR NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N'dbo.NhaCungCap') AND c.name=N'tenNCC' AND t.name=N'nvarchar' AND c.max_length=300 AND c.is_nullable=0)
       OR NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N'dbo.NhaCungCap') AND c.name=N'phienBan' AND t.name=N'bigint' AND c.is_nullable=0)
        THROW 51073, N'dbo.NhaCungCap hiện hữu không khớp cấu trúc migration 07.', 1;

    IF OBJECT_ID(N'dbo.PhieuNhap', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.PhieuNhap (
            maPhieuNhap VARCHAR(20) NOT NULL CONSTRAINT PK_PhieuNhap PRIMARY KEY,
            maNCC VARCHAR(20) NOT NULL,
            maNVTao VARCHAR(20) NOT NULL,
            maNVXacNhan VARCHAR(20) NULL,
            ngayLap DATETIME2(0) NOT NULL,
            ngayXacNhan DATETIME2(0) NULL,
            trangThai VARCHAR(20) NOT NULL CONSTRAINT DF_PhieuNhap_TrangThai DEFAULT ('NHAP'),
            tongTien DECIMAL(18,2) NOT NULL CONSTRAINT DF_PhieuNhap_TongTien DEFAULT (0),
            operationIdTao VARCHAR(36) NOT NULL,
            operationIdXacNhan VARCHAR(36) NULL,
            phienBanTruocXacNhan BIGINT NULL,
            phienBan BIGINT NOT NULL CONSTRAINT DF_PhieuNhap_PhienBan DEFAULT (0),
            CONSTRAINT FK_PhieuNhap_NhaCungCap FOREIGN KEY (maNCC) REFERENCES dbo.NhaCungCap(maNCC),
            CONSTRAINT FK_PhieuNhap_NhanVienTao FOREIGN KEY (maNVTao) REFERENCES dbo.NhanVien(maNV),
            CONSTRAINT FK_PhieuNhap_NhanVienXacNhan FOREIGN KEY (maNVXacNhan) REFERENCES dbo.NhanVien(maNV),
            CONSTRAINT UQ_PhieuNhap_OperationTao UNIQUE (operationIdTao),
            CONSTRAINT CK_PhieuNhap_TrangThai CHECK (trangThai IN ('NHAP','DA_XAC_NHAN','DA_HUY')),
            CONSTRAINT CK_PhieuNhap_Tien CHECK (tongTien >= 0),
            CONSTRAINT CK_PhieuNhap_PhienBan CHECK (phienBan >= 0),
            CONSTRAINT CK_PhieuNhap_XacNhan CHECK (
                (trangThai = 'DA_XAC_NHAN' AND ngayXacNhan IS NOT NULL AND maNVXacNhan IS NOT NULL
                    AND operationIdXacNhan IS NOT NULL AND phienBanTruocXacNhan IS NOT NULL)
                OR (trangThai IN ('NHAP','DA_HUY') AND ngayXacNhan IS NULL AND maNVXacNhan IS NULL
                    AND operationIdXacNhan IS NULL AND phienBanTruocXacNhan IS NULL))
        );
        CREATE INDEX IX_PhieuNhap_NgayTrangThai ON dbo.PhieuNhap(ngayLap, trangThai, maPhieuNhap);
        CREATE INDEX IX_PhieuNhap_NhaCungCap_Ngay ON dbo.PhieuNhap(maNCC, ngayLap, maPhieuNhap);
    END;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.PhieuNhap') AND name=N'UQ_PhieuNhap_OperationXacNhan')
        CREATE UNIQUE INDEX UQ_PhieuNhap_OperationXacNhan ON dbo.PhieuNhap(operationIdXacNhan)
            WHERE operationIdXacNhan IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.PhieuNhap')
        AND name=N'UQ_PhieuNhap_OperationXacNhan' AND is_unique=1 AND has_filter=1
        AND filter_definition LIKE N'%operationIdXacNhan%IS NOT NULL%')
        THROW 51079, N'UQ_PhieuNhap_OperationXacNhan phải là unique filtered index, cho phép nhiều phiếu nháp NULL.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N'dbo.PhieuNhap') AND c.name=N'maPhieuNhap' AND t.name=N'varchar' AND c.max_length=20 AND c.is_nullable=0)
       OR NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N'dbo.PhieuNhap') AND c.name=N'tongTien' AND t.name=N'decimal' AND c.precision=18 AND c.scale=2 AND c.is_nullable=0)
       OR NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N'dbo.PhieuNhap') AND c.name=N'phienBan' AND t.name=N'bigint' AND c.is_nullable=0)
        THROW 51074, N'dbo.PhieuNhap hiện hữu không khớp cấu trúc migration 07.', 1;

    IF OBJECT_ID(N'dbo.ChiTietPhieuNhap', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.ChiTietPhieuNhap (
            maCTPN BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ChiTietPhieuNhap PRIMARY KEY,
            maPhieuNhap VARCHAR(20) NOT NULL,
            maBienThe VARCHAR(20) NOT NULL,
            soLuong INT NOT NULL,
            donGiaNhap DECIMAL(18,2) NOT NULL,
            thanhTien AS CONVERT(DECIMAL(18,2), soLuong * donGiaNhap) PERSISTED,
            CONSTRAINT FK_ChiTietPhieuNhap_PhieuNhap FOREIGN KEY (maPhieuNhap) REFERENCES dbo.PhieuNhap(maPhieuNhap),
            CONSTRAINT FK_ChiTietPhieuNhap_BienThe FOREIGN KEY (maBienThe) REFERENCES dbo.BienThe(maBienThe),
            CONSTRAINT UQ_ChiTietPhieuNhap_Phieu_BienThe UNIQUE (maPhieuNhap, maBienThe),
            CONSTRAINT CK_ChiTietPhieuNhap_SoLuong CHECK (soLuong > 0),
            CONSTRAINT CK_ChiTietPhieuNhap_DonGia CHECK (donGiaNhap >= 0)
        );
    END;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N'dbo.ChiTietPhieuNhap') AND c.name=N'thanhTien' AND t.name=N'decimal'
          AND c.precision=18 AND c.scale=2 AND c.is_computed=1)
        THROW 51075, N'dbo.ChiTietPhieuNhap.thanhTien phải là cột computed DECIMAL(18,2).', 1;

    IF OBJECT_ID(N'dbo.PhieuNhapThaoTac', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.PhieuNhapThaoTac (
            operationId VARCHAR(36) NOT NULL CONSTRAINT PK_PhieuNhapThaoTac PRIMARY KEY,
            maPhieuNhap VARCHAR(20) NOT NULL,
            loaiThaoTac VARCHAR(20) NOT NULL,
            fingerprint VARCHAR(64) NOT NULL,
            phienBanTruoc BIGINT NULL,
            phienBanSau BIGINT NOT NULL,
            thoiGian DATETIME2(0) NOT NULL,
            CONSTRAINT FK_PhieuNhapThaoTac_PhieuNhap FOREIGN KEY (maPhieuNhap) REFERENCES dbo.PhieuNhap(maPhieuNhap),
            CONSTRAINT CK_PhieuNhapThaoTac_Loai CHECK (loaiThaoTac IN ('CREATE','ADD_LINE','UPDATE_LINE','REMOVE_LINE','CONFIRM','CANCEL'))
        );
        CREATE INDEX IX_PhieuNhapThaoTac_Phieu ON dbo.PhieuNhapThaoTac(maPhieuNhap, thoiGian, operationId);
    END;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N'dbo.PhieuNhapThaoTac') AND c.name=N'operationId' AND t.name=N'varchar' AND c.max_length=36 AND c.is_nullable=0)
       OR NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N'dbo.PhieuNhapThaoTac') AND c.name=N'fingerprint' AND t.name=N'varchar' AND c.max_length=64 AND c.is_nullable=0)
        THROW 51076, N'dbo.PhieuNhapThaoTac hiện hữu không khớp cấu trúc migration 07.', 1;

    -- Migration 05 chỉ cho phép bốn nguồn; bổ sung nguồn nhập bằng migration mới.
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.LichSuKho')
        AND name=N'CK_LichSuKho_Nguon' AND is_disabled=0 AND is_not_trusted=0
        AND definition LIKE N'%TON_MO_DAU%' AND definition LIKE N'%KHOI_TAO_BIEN_THE%'
        AND definition LIKE N'%BAN_HANG%' AND definition LIKE N'%DIEU_CHINH_TON%')
       AND NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.LichSuKho')
        AND name=N'CK_LichSuKho_Nguon' AND is_disabled=0 AND is_not_trusted=0
        AND definition LIKE N'%NHAP_HANG%' AND definition LIKE N'%BAN_HANG%')
        THROW 51077, N'CK_LichSuKho_Nguon không có baseline nhận diện được; cần rà soát thủ công.', 1;

    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.LichSuKho')
        AND name=N'CK_LichSuKho_Nguon' AND definition NOT LIKE N'%NHAP_HANG%')
    BEGIN
        ALTER TABLE dbo.LichSuKho DROP CONSTRAINT CK_LichSuKho_Nguon;
        EXEC sys.sp_executesql N'ALTER TABLE dbo.LichSuKho WITH CHECK ADD CONSTRAINT CK_LichSuKho_Nguon
            CHECK (nguon IN (''TON_MO_DAU'',''KHOI_TAO_BIEN_THE'',''BAN_HANG'',''DIEU_CHINH_TON'',''NHAP_HANG''));';
        ALTER TABLE dbo.LichSuKho WITH CHECK CHECK CONSTRAINT CK_LichSuKho_Nguon;
    END;
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.LichSuKho')
        AND name=N'CK_LichSuKho_Nguon' AND is_disabled=0 AND is_not_trusted=0 AND definition LIKE N'%NHAP_HANG%')
        THROW 51078, N'Không xác minh được CHECK constraint cho nguồn NHAP_HANG.', 1;

    COMMIT TRANSACTION;
    PRINT N'Migration 07 hoàn tất; dữ liệu cũ được giữ nguyên.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
