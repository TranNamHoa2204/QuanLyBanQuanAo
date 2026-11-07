-- DATABASE MỚI ĐẦY ĐỦ: QuanLyCuaHangQuanAo. Snapshot ngày 05/11/2026.
-- File UTF-8; chạy TOÀN BỘ bằng SSMS F5 hoặc sqlcmd -b. Không cần database nguồn.
-- Tự tạo 17 bảng, khóa/index/CHECK, dữ liệu cũ và dữ liệu demo bổ sung.
-- TaiKhoan.matKhauHash: PBKDF2-HMAC-SHA256, 600000 vòng, salt riêng; không lưu mật khẩu rõ.
-- Ảnh dùng lại resource images/ của ứng dụng, SQL chỉ lưu đường dẫn.
-- Script một batch: nếu tên database đã tồn tại thì DỪNG, không sửa/xóa database đó.
USE [master];
SET NOCOUNT ON;
IF DB_ID(N'QuanLyCuaHangQuanAo') IS NOT NULL
    THROW 51100,N'Database QuanLyCuaHangQuanAo đã tồn tại. Đổi tên/di chuyển bản cũ trước khi tạo mới.',1;
DECLARE @DataPath nvarchar(4000)=CONVERT(nvarchar(4000),SERVERPROPERTY('InstanceDefaultDataPath'));
DECLARE @LogPath nvarchar(4000)=CONVERT(nvarchar(4000),SERVERPROPERTY('InstanceDefaultLogPath'));
IF @DataPath IS NULL OR @LogPath IS NULL THROW 51101,N'Không xác định được thư mục dữ liệu SQL Server.',1;
DECLARE @Suffix nvarchar(32)=REPLACE(CONVERT(nvarchar(36),NEWID()),'-','');
DECLARE @DataFile nvarchar(4000)=@DataPath+N'QuanLyCuaHangQuanAo_'+@Suffix+N'.mdf';
DECLARE @LogFile nvarchar(4000)=@LogPath+N'QuanLyCuaHangQuanAo_'+@Suffix+N'_log.ldf';
DECLARE @CreateSql nvarchar(max)=N'CREATE DATABASE [QuanLyCuaHangQuanAo] ON PRIMARY (NAME=N''QuanLyCuaHangQuanAo_Data'',FILENAME=N'''+REPLACE(@DataFile,N'''',N'''''')+N''') LOG ON (NAME=N''QuanLyCuaHangQuanAo_Log'',FILENAME=N'''+REPLACE(@LogFile,N'''',N'''''')+N''')';
EXEC sys.sp_executesql @CreateSql;

EXEC(N'USE [QuanLyCuaHangQuanAo];
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;
    IF EXISTS (SELECT 1 FROM sys.tables WHERE is_ms_shipped = 0)
        THROW 50001, N''Database đã có bảng. Script không thay đổi cấu trúc hiện có. Hãy dùng database trống hoặc đổi tên database trong script.'', 1;

    CREATE TABLE dbo.LoaiSanPham (
        maLoai VARCHAR(20) NOT NULL,
        tenLoai NVARCHAR(100) NOT NULL,
        CONSTRAINT PK_LoaiSanPham PRIMARY KEY (maLoai),
        CONSTRAINT UQ_LoaiSanPham_Ten UNIQUE (tenLoai),
        CONSTRAINT CK_LoaiSanPham_Ten CHECK (LEN(LTRIM(RTRIM(tenLoai))) > 0)
    );

    CREATE TABLE dbo.ThuongHieu (
        maThuongHieu VARCHAR(20) NOT NULL,
        tenThuongHieu NVARCHAR(100) NOT NULL,
        CONSTRAINT PK_ThuongHieu PRIMARY KEY (maThuongHieu),
        CONSTRAINT UQ_ThuongHieu_Ten UNIQUE (tenThuongHieu),
        CONSTRAINT CK_ThuongHieu_Ten CHECK (LEN(LTRIM(RTRIM(tenThuongHieu))) > 0)
    );

    CREATE TABLE dbo.SanPham (
        maSP VARCHAR(20) NOT NULL,
        tenSP NVARCHAR(200) NOT NULL,
        maLoai VARCHAR(20) NOT NULL,
        maThuongHieu VARCHAR(20) NOT NULL,
        giaBan DECIMAL(18,2) NOT NULL,
        trangThai BIT NOT NULL CONSTRAINT DF_SanPham_TrangThai DEFAULT (1),
        CONSTRAINT PK_SanPham PRIMARY KEY (maSP),
        CONSTRAINT FK_SanPham_LoaiSanPham FOREIGN KEY (maLoai) REFERENCES dbo.LoaiSanPham(maLoai),
        CONSTRAINT FK_SanPham_ThuongHieu FOREIGN KEY (maThuongHieu) REFERENCES dbo.ThuongHieu(maThuongHieu),
        CONSTRAINT CK_SanPham_GiaBan CHECK (giaBan >= 0),
        CONSTRAINT CK_SanPham_Ten CHECK (LEN(LTRIM(RTRIM(tenSP))) > 0)
    );

    CREATE TABLE dbo.BienThe (
        maBienThe VARCHAR(20) NOT NULL,
        maSP VARCHAR(20) NOT NULL,
        mauSac NVARCHAR(50) NOT NULL,
        kichCo NVARCHAR(20) NOT NULL,
        soLuongTon INT NOT NULL CONSTRAINT DF_BienThe_Ton DEFAULT (0),
        trangThai BIT NOT NULL CONSTRAINT DF_BienThe_TrangThai DEFAULT (1),
        phienBan BIGINT NOT NULL CONSTRAINT DF_BienThe_PhienBan DEFAULT (0),
        CONSTRAINT PK_BienThe PRIMARY KEY (maBienThe),
        CONSTRAINT FK_BienThe_SanPham FOREIGN KEY (maSP) REFERENCES dbo.SanPham(maSP),
        CONSTRAINT UQ_BienThe_SanPham_Mau_Size UNIQUE (maSP, mauSac, kichCo),
        CONSTRAINT CK_BienThe_Ton CHECK (soLuongTon >= 0),
        CONSTRAINT CK_BienThe_PhienBan CHECK (phienBan >= 0),
        CONSTRAINT CK_BienThe_Mau_Size CHECK (LEN(LTRIM(RTRIM(mauSac))) > 0 AND LEN(LTRIM(RTRIM(kichCo))) > 0)
    );

    CREATE TABLE dbo.KhachHang (
        maKH VARCHAR(20) NOT NULL,
        hoTen NVARCHAR(100) NOT NULL,
        sdt VARCHAR(20) NULL,
        email VARCHAR(254) NULL,
        diaChi NVARCHAR(255) NULL,
        trangThai BIT NOT NULL CONSTRAINT DF_KhachHang_TrangThai DEFAULT (1),
        CONSTRAINT PK_KhachHang PRIMARY KEY (maKH),
        CONSTRAINT CK_KhachHang_Ten CHECK (LEN(LTRIM(RTRIM(hoTen))) > 0)
    );

    CREATE TABLE dbo.NhanVien (
        maNV VARCHAR(20) NOT NULL,
        hoTen NVARCHAR(100) NOT NULL,
        gioiTinh BIT NULL, -- 1: nam; 0: nữ; NULL: chưa khai báo
        ngaySinh DATE NULL,
        sdt VARCHAR(20) NOT NULL,
        email VARCHAR(254) NULL,
        diaChi NVARCHAR(255) NULL,
        chucVu NVARCHAR(100) NULL, -- thông tin nghiệp vụ; quyền lấy từ TaiKhoan.vaiTro
        trangThai BIT NOT NULL CONSTRAINT DF_NhanVien_TrangThai DEFAULT (1),
        CONSTRAINT PK_NhanVien PRIMARY KEY (maNV),
        CONSTRAINT UQ_NhanVien_Sdt UNIQUE (sdt),
        CONSTRAINT CK_NhanVien_Ten CHECK (LEN(LTRIM(RTRIM(hoTen))) > 0),
        CONSTRAINT CK_NhanVien_Sdt CHECK (LEN(LTRIM(RTRIM(sdt))) > 0)
    );

    CREATE TABLE dbo.TaiKhoan (
        username VARCHAR(50) NOT NULL,
        maNV VARCHAR(20) NOT NULL,
        matKhauHash VARCHAR(255) NOT NULL, -- lưu chuỗi hash gồm tham số/salt, KHÔNG lưu mật khẩu rõ
        vaiTro VARCHAR(30) NOT NULL,
        trangThai BIT NOT NULL CONSTRAINT DF_TaiKhoan_TrangThai DEFAULT (1),
        CONSTRAINT PK_TaiKhoan PRIMARY KEY (username),
        CONSTRAINT UQ_TaiKhoan_NhanVien UNIQUE (maNV),
        CONSTRAINT FK_TaiKhoan_NhanVien FOREIGN KEY (maNV) REFERENCES dbo.NhanVien(maNV),
        CONSTRAINT CK_TaiKhoan_VaiTro CHECK (vaiTro IN (''NHAN_VIEN_BAN_HANG'', ''NHAN_VIEN_QUAN_LY'')),
        CONSTRAINT CK_TaiKhoan_Username CHECK (LEN(LTRIM(RTRIM(username))) > 0),
        CONSTRAINT CK_TaiKhoan_Hash CHECK (LEN(LTRIM(RTRIM(matKhauHash))) > 0)
    );

    CREATE TABLE dbo.KhuyenMai (
        maKM VARCHAR(20) NOT NULL,
        tenKM NVARCHAR(200) NOT NULL,
        phanTramGiam DECIMAL(5,2) NOT NULL,
        ngayBatDau DATE NOT NULL,
        ngayKetThuc DATE NOT NULL,
        trangThai BIT NOT NULL CONSTRAINT DF_KhuyenMai_TrangThai DEFAULT (1),
        CONSTRAINT PK_KhuyenMai PRIMARY KEY (maKM),
        CONSTRAINT CK_KhuyenMai_PhanTram CHECK (phanTramGiam BETWEEN 0 AND 100),
        CONSTRAINT CK_KhuyenMai_ThoiGian CHECK (ngayKetThuc >= ngayBatDau),
        CONSTRAINT CK_KhuyenMai_Ten CHECK (LEN(LTRIM(RTRIM(tenKM))) > 0)
    );

    CREATE TABLE dbo.Thue (
        maThue VARCHAR(20) NOT NULL,
        tenThue NVARCHAR(100) NOT NULL,
        phanTram DECIMAL(5,2) NOT NULL,
        moTa NVARCHAR(255) NULL,
        trangThai BIT NOT NULL CONSTRAINT DF_Thue_TrangThai DEFAULT (1),
        CONSTRAINT PK_Thue PRIMARY KEY (maThue),
        CONSTRAINT CK_Thue_PhanTram CHECK (phanTram BETWEEN 0 AND 100)
    );

    CREATE TABLE dbo.HoaDon (
        maHD VARCHAR(20) NOT NULL,
        maNV VARCHAR(20) NOT NULL,
        maKH VARCHAR(20) NULL, -- NULL: khách vãng lai
        maKM VARCHAR(20) NULL, -- NULL: không áp dụng khuyến mãi
        maThue VARCHAR(20) NOT NULL, -- có thể dùng cấu hình 0% khi không áp dụng thuế
        ngayLap DATETIME2(0) NOT NULL CONSTRAINT DF_HoaDon_NgayLap DEFAULT (SYSDATETIME()),
        tamTinh DECIMAL(18,2) NOT NULL CONSTRAINT DF_HoaDon_TamTinh DEFAULT (0),
        phanTramGiam DECIMAL(5,2) NOT NULL CONSTRAINT DF_HoaDon_PhanTramGiam DEFAULT (0),
        tienGiam DECIMAL(18,2) NOT NULL CONSTRAINT DF_HoaDon_TienGiam DEFAULT (0),
        phanTramThue DECIMAL(5,2) NOT NULL CONSTRAINT DF_HoaDon_PhanTramThue DEFAULT (0),
        tienThue DECIMAL(18,2) NOT NULL CONSTRAINT DF_HoaDon_TienThue DEFAULT (0),
        tongTien DECIMAL(18,2) NOT NULL CONSTRAINT DF_HoaDon_TongTien DEFAULT (0),
        trangThai VARCHAR(20) NOT NULL CONSTRAINT DF_HoaDon_TrangThai DEFAULT (''NHAP''),
        ghiChu NVARCHAR(500) NULL,
        phienBan BIGINT NOT NULL CONSTRAINT DF_HoaDon_PhienBan DEFAULT (0),
        CONSTRAINT PK_HoaDon PRIMARY KEY (maHD),
        CONSTRAINT FK_HoaDon_NhanVien FOREIGN KEY (maNV) REFERENCES dbo.NhanVien(maNV),
        CONSTRAINT FK_HoaDon_KhachHang FOREIGN KEY (maKH) REFERENCES dbo.KhachHang(maKH),
        CONSTRAINT FK_HoaDon_KhuyenMai FOREIGN KEY (maKM) REFERENCES dbo.KhuyenMai(maKM),
        CONSTRAINT FK_HoaDon_Thue FOREIGN KEY (maThue) REFERENCES dbo.Thue(maThue),
        CONSTRAINT CK_HoaDon_TrangThai CHECK (trangThai IN (''NHAP'',''DA_THANH_TOAN'',''DA_HUY'')),
        CONSTRAINT CK_HoaDon_Tien CHECK (tamTinh >= 0 AND tienGiam >= 0 AND tienGiam <= tamTinh AND tienThue >= 0 AND tongTien >= 0),
        CONSTRAINT CK_HoaDon_TongTien CHECK (tongTien = tamTinh - tienGiam + tienThue),
        CONSTRAINT CK_HoaDon_TyLe CHECK (phanTramGiam BETWEEN 0 AND 100 AND phanTramThue BETWEEN 0 AND 100),
        CONSTRAINT CK_HoaDon_KhongKhuyenMai CHECK (maKM IS NOT NULL OR (phanTramGiam = 0 AND tienGiam = 0)),
        CONSTRAINT CK_HoaDon_PhienBan CHECK (phienBan >= 0)
    );

    CREATE TABLE dbo.ChiTietHoaDon (
        maCTHD BIGINT IDENTITY(1,1) NOT NULL,
        maHD VARCHAR(20) NOT NULL,
        maBienThe VARCHAR(20) NOT NULL,
        soLuong INT NOT NULL,
        donGia DECIMAL(18,2) NOT NULL, -- chốt giá tại lúc bán, không đọc lại giá hiện hành
        thanhTien AS CONVERT(DECIMAL(18,2), soLuong * donGia) PERSISTED,
        CONSTRAINT PK_ChiTietHoaDon PRIMARY KEY (maCTHD),
        CONSTRAINT FK_ChiTietHoaDon_HoaDon FOREIGN KEY (maHD) REFERENCES dbo.HoaDon(maHD),
        CONSTRAINT FK_ChiTietHoaDon_BienThe FOREIGN KEY (maBienThe) REFERENCES dbo.BienThe(maBienThe),
        CONSTRAINT UQ_ChiTietHoaDon_HD_BienThe UNIQUE (maHD, maBienThe),
        CONSTRAINT CK_ChiTietHoaDon_SoLuong CHECK (soLuong > 0),
        CONSTRAINT CK_ChiTietHoaDon_DonGia CHECK (donGia >= 0)
    );

    -- NULL không phải số điện thoại: dùng index lọc để nhiều khách có thể chưa khai báo.
    CREATE UNIQUE INDEX UX_KhachHang_Sdt ON dbo.KhachHang(sdt) WHERE sdt IS NOT NULL;
    CREATE INDEX IX_SanPham_MaLoai ON dbo.SanPham(maLoai);
    CREATE INDEX IX_SanPham_MaThuongHieu ON dbo.SanPham(maThuongHieu);
    CREATE INDEX IX_HoaDon_NhanVien ON dbo.HoaDon(maNV);
    CREATE INDEX IX_HoaDon_KhachHang ON dbo.HoaDon(maKH);
    CREATE INDEX IX_HoaDon_KhuyenMai ON dbo.HoaDon(maKM);
    CREATE INDEX IX_HoaDon_Thue ON dbo.HoaDon(maThue);
    CREATE INDEX IX_HoaDon_TrangThai_NgayLap ON dbo.HoaDon(trangThai, ngayLap);
    CREATE INDEX IX_ChiTietHoaDon_BienThe ON dbo.ChiTietHoaDon(maBienThe);

    COMMIT TRANSACTION;
    PRINT N''Đã tạo 11 bảng và 10 khóa ngoại. Có thể tạo Database Diagram trong SSMS.'';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
');

EXEC(N'USE [QuanLyCuaHangQuanAo];

-- Migration bổ sung bảng ảnh, không xóa hoặc tạo lại 11 bảng gốc.
-- Chạy trong SSMS trước 02_NhapDuLieuQuanAo.sql. Không thay đổi database tự động từ Java.
');

EXEC(N'USE [QuanLyCuaHangQuanAo];

SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N''dbo.SanPham'', N''U'') IS NULL
        THROW 51001, N''Chưa có bảng SanPham. Hãy tạo database bằng script gốc trước.'', 1;
    IF OBJECT_ID(N''dbo.AnhSanPham'', N''U'') IS NULL
    BEGIN
        CREATE TABLE dbo.AnhSanPham (
            maAnh VARCHAR(20) NOT NULL,
            maSP VARCHAR(20) NOT NULL,
            mauSac NVARCHAR(50) NOT NULL,
            duongDan VARCHAR(500) NOT NULL,
            thuTu INT NOT NULL,
            anhChinh BIT NOT NULL CONSTRAINT DF_AnhSanPham_AnhChinh DEFAULT (0),
            CONSTRAINT PK_AnhSanPham PRIMARY KEY (maAnh),
            CONSTRAINT FK_AnhSanPham_SanPham FOREIGN KEY (maSP) REFERENCES dbo.SanPham(maSP),
            CONSTRAINT UQ_AnhSanPham_DuongDan UNIQUE (duongDan),
            CONSTRAINT UQ_AnhSanPham_ThuTu UNIQUE (maSP, mauSac, thuTu),
            CONSTRAINT CK_AnhSanPham_ThuTu CHECK (thuTu > 0),
            CONSTRAINT CK_AnhSanPham_MauSac CHECK (LEN(LTRIM(RTRIM(mauSac))) > 0),
            CONSTRAINT CK_AnhSanPham_DuongDan CHECK (
                duongDan LIKE ''images/%'' AND duongDan NOT LIKE ''%..%''
                AND CHARINDEX(CHAR(92),duongDan) = 0)
        );
        CREATE UNIQUE INDEX UX_AnhSanPham_Chinh
            ON dbo.AnhSanPham(maSP, mauSac) WHERE anhChinh = 1;
        PRINT N''Đã bổ sung bảng AnhSanPham. Database hiện có 12 bảng.'';
    END
    ELSE
        PRINT N''AnhSanPham đã tồn tại: không thay đổi cấu trúc hiện có.'';
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
');

EXEC(N'USE [QuanLyCuaHangQuanAo];

-- S11: bổ sung thời điểm thanh toán và tên hiển thị snapshot cho chứng từ.
-- Chạy một lần hoặc chạy lại an toàn sau baseline 12 bảng; không backfill lịch sử.
-- Chỉ áp dụng trên database demo đã xác định, không chạy trên database thật.
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF DB_NAME() <> N''QuanLyCuaHangQuanAo''
        THROW 51010, N''S11 yêu cầu database demo QuanLyCuaHangQuanAo; đã dừng trước khi thay đổi schema.'', 1;
    IF OBJECT_ID(N''dbo.HoaDon'', N''U'') IS NULL OR OBJECT_ID(N''dbo.ChiTietHoaDon'', N''U'') IS NULL
        THROW 51011, N''Không tìm thấy đủ bảng HoaDon và ChiTietHoaDon của baseline.'', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N''dbo.HoaDon'') AND name = N''ngayThanhToan'')
        ALTER TABLE dbo.HoaDon ADD ngayThanhToan DATETIME2(0) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N''dbo.HoaDon'') AND name = N''tenNhanVienSnapshot'')
        ALTER TABLE dbo.HoaDon ADD tenNhanVienSnapshot NVARCHAR(100) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N''dbo.HoaDon'') AND name = N''tenKhachHangSnapshot'')
        ALTER TABLE dbo.HoaDon ADD tenKhachHangSnapshot NVARCHAR(100) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N''dbo.ChiTietHoaDon'') AND name = N''tenSPSnapshot'')
        ALTER TABLE dbo.ChiTietHoaDon ADD tenSPSnapshot NVARCHAR(200) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N''dbo.ChiTietHoaDon'') AND name = N''mauSacSnapshot'')
        ALTER TABLE dbo.ChiTietHoaDon ADD mauSacSnapshot NVARCHAR(50) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N''dbo.ChiTietHoaDon'') AND name = N''kichCoSnapshot'')
        ALTER TABLE dbo.ChiTietHoaDon ADD kichCoSnapshot NVARCHAR(20) NULL;

    -- Existence alone is not enough: fail on a pre-existing incompatible definition.
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.HoaDon'') AND c.name = N''ngayThanhToan''
          AND t.name = N''datetime2'' AND c.scale = 0 AND c.is_nullable = 1)
        THROW 51012, N''dbo.HoaDon.ngayThanhToan phải DATETIME2(0) NULL.'', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.HoaDon'') AND c.name = N''tenNhanVienSnapshot''
          AND t.name = N''nvarchar'' AND c.max_length = 200 AND c.is_nullable = 1)
        THROW 51013, N''dbo.HoaDon.tenNhanVienSnapshot phải NVARCHAR(100) NULL.'', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.HoaDon'') AND c.name = N''tenKhachHangSnapshot''
          AND t.name = N''nvarchar'' AND c.max_length = 200 AND c.is_nullable = 1)
        THROW 51014, N''dbo.HoaDon.tenKhachHangSnapshot phải NVARCHAR(100) NULL.'', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.ChiTietHoaDon'') AND c.name = N''tenSPSnapshot''
          AND t.name = N''nvarchar'' AND c.max_length = 400 AND c.is_nullable = 1)
        THROW 51015, N''dbo.ChiTietHoaDon.tenSPSnapshot phải NVARCHAR(200) NULL.'', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.ChiTietHoaDon'') AND c.name = N''mauSacSnapshot''
          AND t.name = N''nvarchar'' AND c.max_length = 100 AND c.is_nullable = 1)
        THROW 51016, N''dbo.ChiTietHoaDon.mauSacSnapshot phải NVARCHAR(50) NULL.'', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.ChiTietHoaDon'') AND c.name = N''kichCoSnapshot''
          AND t.name = N''nvarchar'' AND c.max_length = 40 AND c.is_nullable = 1)
        THROW 51017, N''dbo.ChiTietHoaDon.kichCoSnapshot phải NVARCHAR(20) NULL.'', 1;

    COMMIT TRANSACTION;
    PRINT N''S11 migration hoàn tất hoặc schema đã đúng; dữ liệu hóa đơn cũ được giữ nguyên.'';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
');

EXEC(N'USE [QuanLyCuaHangQuanAo];

-- S15 / FR10: ledger append-only cho mọi biến động tồn kho.
-- Chạy sau baseline 12 bảng và 04_HoaDonSnapshotVaThoiDiemThanhToan.sql.
-- Chỉ áp dụng cho database demo có tên cố định; không xóa hay reset dữ liệu.
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF DB_NAME() <> N''QuanLyCuaHangQuanAo''
        THROW 51020, N''S15 yêu cầu database demo QuanLyCuaHangQuanAo; đã dừng trước khi thay đổi schema.'', 1;
    IF OBJECT_ID(N''dbo.BienThe'', N''U'') IS NULL OR OBJECT_ID(N''dbo.NhanVien'', N''U'') IS NULL
        THROW 51021, N''Không tìm thấy đủ bảng BienThe và NhanVien của baseline.'', 1;

    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.BienThe'') AND c.name = N''maBienThe''
          AND t.name = N''varchar'' AND c.max_length = 20 AND c.is_nullable = 0)
        THROW 51022, N''dbo.BienThe.maBienThe phải VARCHAR(20) NOT NULL.'', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.BienThe'') AND c.name = N''soLuongTon''
          AND t.name = N''int'' AND c.is_nullable = 0)
        THROW 51023, N''dbo.BienThe.soLuongTon phải INT NOT NULL.'', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.BienThe'') AND c.name = N''phienBan''
          AND t.name = N''bigint'' AND c.is_nullable = 0)
        THROW 51024, N''dbo.BienThe.phienBan phải BIGINT NOT NULL.'', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.NhanVien'') AND c.name = N''maNV''
          AND t.name = N''varchar'' AND c.max_length = 20 AND c.is_nullable = 0)
        THROW 51025, N''dbo.NhanVien.maNV phải VARCHAR(20) NOT NULL.'', 1;

    DECLARE @created bit = 0;
    IF OBJECT_ID(N''dbo.LichSuKho'', N''U'') IS NULL
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
            CONSTRAINT CK_LichSuKho_Nguon CHECK (nguon IN (''TON_MO_DAU'',''KHOI_TAO_BIEN_THE'',''BAN_HANG'',''DIEU_CHINH_TON'')),
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
        WHERE c.object_id = OBJECT_ID(N''dbo.LichSuKho'') AND c.name = N''maLichSuKho''
          AND t.name = N''bigint'' AND c.is_nullable = 0 AND c.is_identity = 1)
        THROW 51026, N''LichSuKho.maLichSuKho phải BIGINT IDENTITY NOT NULL.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.LichSuKho'') AND c.name = N''maBienThe''
          AND t.name = N''varchar'' AND c.max_length = 20 AND c.is_nullable = 0)
        THROW 51027, N''LichSuKho.maBienThe phải VARCHAR(20) NOT NULL.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.LichSuKho'') AND c.name = N''maNV''
          AND t.name = N''varchar'' AND c.max_length = 20 AND c.is_nullable = 1)
        THROW 51028, N''LichSuKho.maNV phải VARCHAR(20) NULL.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.LichSuKho'') AND c.name = N''thoiGian''
          AND t.name = N''datetime2'' AND c.scale = 0 AND c.is_nullable = 0)
        THROW 51029, N''LichSuKho.thoiGian phải DATETIME2(0) NOT NULL.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.LichSuKho'') AND c.name = N''nguon''
          AND t.name = N''varchar'' AND c.max_length = 30 AND c.is_nullable = 0)
        THROW 51030, N''LichSuKho.nguon phải VARCHAR(30) NOT NULL.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.LichSuKho'') AND c.name = N''thamChieu''
          AND t.name = N''varchar'' AND c.max_length = 100 AND c.is_nullable = 0)
        THROW 51031, N''LichSuKho.thamChieu phải VARCHAR(100) NOT NULL.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.LichSuKho'') AND c.name = N''operationId''
          AND t.name = N''varchar'' AND c.max_length = 100 AND c.is_nullable = 0)
        THROW 51032, N''LichSuKho.operationId phải VARCHAR(100) NOT NULL.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.LichSuKho'') AND c.name = N''soLuongTruoc''
          AND t.name = N''int'' AND c.is_nullable = 0)
        THROW 51033, N''LichSuKho.soLuongTruoc phải INT NOT NULL.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.LichSuKho'') AND c.name = N''soLuongThayDoi''
          AND t.name = N''int'' AND c.is_nullable = 0)
        THROW 51034, N''LichSuKho.soLuongThayDoi phải INT NOT NULL.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.LichSuKho'') AND c.name = N''soLuongSau''
          AND t.name = N''int'' AND c.is_nullable = 0)
        THROW 51035, N''LichSuKho.soLuongSau phải INT NOT NULL.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.LichSuKho'') AND c.name = N''phienBanTruoc''
          AND t.name = N''bigint'' AND c.is_nullable = 1)
        THROW 51036, N''LichSuKho.phienBanTruoc phải BIGINT NULL.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.LichSuKho'') AND c.name = N''tenNguoiThaoTacSnapshot''
          AND t.name = N''nvarchar'' AND c.max_length = 200 AND c.is_nullable = 1)
        THROW 51037, N''LichSuKho.tenNguoiThaoTacSnapshot phải NVARCHAR(100) NULL.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.LichSuKho'') AND c.name = N''lyDo''
          AND t.name = N''nvarchar'' AND c.max_length = 1000 AND c.is_nullable = 0)
        THROW 51038, N''LichSuKho.lyDo phải NVARCHAR(500) NOT NULL.'', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints kc JOIN sys.index_columns ic
        ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
        JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
        WHERE kc.parent_object_id = OBJECT_ID(N''dbo.LichSuKho'')
          AND kc.name = N''PK_LichSuKho'' AND kc.type = N''PK'' AND ic.key_ordinal = 1
          AND c.name = N''maLichSuKho''
          AND (SELECT COUNT(*) FROM sys.index_columns allKeys WHERE allKeys.object_id = kc.parent_object_id
               AND allKeys.index_id = kc.unique_index_id AND allKeys.key_ordinal > 0) = 1)
        THROW 51039, N''Thiếu khóa chính PK_LichSuKho.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints kc JOIN sys.index_columns ic
        ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
        JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
        WHERE kc.parent_object_id = OBJECT_ID(N''dbo.LichSuKho'')
          AND kc.name = N''UQ_LichSuKho_OperationId'' AND kc.type = N''UQ'' AND ic.key_ordinal = 1
          AND c.name = N''operationId''
          AND (SELECT COUNT(*) FROM sys.index_columns allKeys WHERE allKeys.object_id = kc.parent_object_id
               AND allKeys.index_id = kc.unique_index_id AND allKeys.key_ordinal > 0) = 1)
        THROW 51040, N''Thiếu unique constraint UQ_LichSuKho_OperationId.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
        JOIN sys.columns pc ON pc.object_id = fkc.parent_object_id AND pc.column_id = fkc.parent_column_id
        JOIN sys.columns rc ON rc.object_id = fkc.referenced_object_id AND rc.column_id = fkc.referenced_column_id
        WHERE fk.parent_object_id = OBJECT_ID(N''dbo.LichSuKho'') AND fk.name = N''FK_LichSuKho_BienThe''
          AND fk.referenced_object_id = OBJECT_ID(N''dbo.BienThe'') AND pc.name = N''maBienThe'' AND rc.name = N''maBienThe''
          AND fk.is_disabled = 0 AND fk.is_not_trusted = 0 AND fkc.constraint_column_id = 1
          AND (SELECT COUNT(*) FROM sys.foreign_key_columns allKeys WHERE allKeys.constraint_object_id = fk.object_id) = 1)
        THROW 51041, N''Thiếu hoặc chưa kiểm chứng FK_LichSuKho_BienThe.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
        JOIN sys.columns pc ON pc.object_id = fkc.parent_object_id AND pc.column_id = fkc.parent_column_id
        JOIN sys.columns rc ON rc.object_id = fkc.referenced_object_id AND rc.column_id = fkc.referenced_column_id
        WHERE fk.parent_object_id = OBJECT_ID(N''dbo.LichSuKho'') AND fk.name = N''FK_LichSuKho_NhanVien''
          AND fk.referenced_object_id = OBJECT_ID(N''dbo.NhanVien'') AND pc.name = N''maNV'' AND rc.name = N''maNV''
          AND fk.is_disabled = 0 AND fk.is_not_trusted = 0 AND fkc.constraint_column_id = 1
          AND (SELECT COUNT(*) FROM sys.foreign_key_columns allKeys WHERE allKeys.constraint_object_id = fk.object_id) = 1)
        THROW 51042, N''Thiếu hoặc chưa kiểm chứng FK_LichSuKho_NhanVien.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N''dbo.LichSuKho'')
        AND name = N''CK_LichSuKho_Nguon'' AND is_disabled = 0 AND is_not_trusted = 0
        AND definition LIKE N''%TON_MO_DAU%'' AND definition LIKE N''%KHOI_TAO_BIEN_THE%''
        AND definition LIKE N''%BAN_HANG%'' AND definition LIKE N''%DIEU_CHINH_TON%'')
       OR NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N''dbo.LichSuKho'')
        AND name = N''CK_LichSuKho_SoLuong'' AND is_disabled = 0 AND is_not_trusted = 0
        AND definition LIKE N''%soLuongTruoc%'' AND definition LIKE N''%soLuongThayDoi%'' AND definition LIKE N''%soLuongSau%'')
       OR NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N''dbo.LichSuKho'')
        AND name = N''CK_LichSuKho_OperationId'' AND is_disabled = 0 AND is_not_trusted = 0
        AND definition LIKE N''%operationId%'')
       OR NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N''dbo.LichSuKho'')
        AND name = N''CK_LichSuKho_ThamChieu'' AND is_disabled = 0 AND is_not_trusted = 0
        AND definition LIKE N''%thamChieu%'')
       OR NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N''dbo.LichSuKho'')
        AND name = N''CK_LichSuKho_LyDo'' AND is_disabled = 0 AND is_not_trusted = 0
        AND definition LIKE N''%lyDo%'')
        THROW 51043, N''Thiếu CHECK constraint hợp lệ cho bảng LichSuKho.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id = OBJECT_ID(N''dbo.LichSuKho'')
        AND i.name = N''IX_LichSuKho_BienThe_ThoiGian'' AND i.is_disabled = 0
        AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 3
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=1 AND c.name=N''maBienThe'')
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=2 AND c.name=N''thoiGian'')
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=3 AND c.name=N''maLichSuKho''))
       OR NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id = OBJECT_ID(N''dbo.LichSuKho'')
        AND i.name = N''IX_LichSuKho_Nguon_ThoiGian'' AND i.is_disabled = 0
        AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 3
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=1 AND c.name=N''nguon'')
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=2 AND c.name=N''thoiGian'')
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=3 AND c.name=N''maLichSuKho''))
       OR NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id = OBJECT_ID(N''dbo.LichSuKho'')
        AND i.name = N''IX_LichSuKho_NhanVien_ThoiGian'' AND i.is_disabled = 0
        AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 3
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=1 AND c.name=N''maNV'')
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=2 AND c.name=N''thoiGian'')
        AND EXISTS (SELECT 1 FROM sys.index_columns x JOIN sys.columns c ON c.object_id=x.object_id AND c.column_id=x.column_id
                    WHERE x.object_id=i.object_id AND x.index_id=i.index_id AND x.key_ordinal=3 AND c.name=N''maLichSuKho''))
        THROW 51044, N''Thiếu index tra cứu của bảng LichSuKho.'', 1;

    IF @created = 1
    BEGIN
        -- Only current quantities are recorded as a conversion-time opening balance.
        -- No attempt is made to reconstruct past sales, purchases, or adjustments.
        INSERT dbo.LichSuKho (maBienThe, maNV, thoiGian, nguon, thamChieu, operationId,
                              soLuongTruoc, soLuongThayDoi, soLuongSau, phienBanTruoc,
                              tenNguoiThaoTacSnapshot, lyDo)
        SELECT b.maBienThe, NULL, CONVERT(DATETIME2(0), SYSDATETIME()), ''TON_MO_DAU'',
               CONCAT(''BASELINE:'', b.maBienThe), CONCAT(''S15:BASELINE:'', b.maBienThe),
               0, b.soLuongTon, b.soLuongTon, b.phienBan, NULL,
               N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.''
        FROM dbo.BienThe b;
    END
    ELSE IF EXISTS (
        SELECT 1 FROM dbo.BienThe b
        WHERE NOT EXISTS (SELECT 1 FROM dbo.LichSuKho l WHERE l.maBienThe = b.maBienThe))
        THROW 51045, N''Có biến thể không có ledger. Migration không tự bù dữ liệu khi chạy lại; cần kiểm tra nguyên nhân.'', 1;

    COMMIT TRANSACTION;
    PRINT N''S15 migration hoàn tất hoặc schema ledger đã được xác minh; dữ liệu cũ được giữ nguyên.'';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
');

EXEC(N'USE [QuanLyCuaHangQuanAo];

-- S18 / FR12: ghi nhận phương thức thu tại quầy; không tích hợp/xác minh ngân hàng.
-- Chạy sau baseline, 04_HoaDonSnapshotVaThoiDiemThanhToan.sql và 05_LichSuBienDongKho.sql.
-- Các cột nullable để giữ nguyên hóa đơn lịch sử; không backfill phương thức thu.
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF DB_NAME() <> N''QuanLyCuaHangQuanAo''
        THROW 51060, N''S18 yêu cầu database demo QuanLyCuaHangQuanAo; đã dừng trước khi thay đổi schema.'', 1;
    IF OBJECT_ID(N''dbo.HoaDon'', N''U'') IS NULL OR OBJECT_ID(N''dbo.LichSuKho'', N''U'') IS NULL
        THROW 51061, N''Cần có baseline HoaDon và migration S15 LichSuKho trước S18.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N''dbo.HoaDon'') AND name = N''ngayThanhToan'')
        THROW 51062, N''Cần áp dụng migration S11 trước S18.'', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N''dbo.HoaDon'') AND name = N''phuongThucThanhToan'')
        ALTER TABLE dbo.HoaDon ADD phuongThucThanhToan VARCHAR(20) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N''dbo.HoaDon'') AND name = N''tienKhachDua'')
        ALTER TABLE dbo.HoaDon ADD tienKhachDua DECIMAL(18,2) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N''dbo.HoaDon'') AND name = N''tienThua'')
        ALTER TABLE dbo.HoaDon ADD tienThua DECIMAL(18,2) NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N''dbo.HoaDon'') AND name = N''nhanVienXacNhanChuyenKhoan'')
        ALTER TABLE dbo.HoaDon ADD nhanVienXacNhanChuyenKhoan BIT NULL;

    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.HoaDon'') AND c.name = N''phuongThucThanhToan''
          AND t.name = N''varchar'' AND c.max_length = 20 AND c.is_nullable = 1)
        THROW 51063, N''dbo.HoaDon.phuongThucThanhToan phải VARCHAR(20) NULL.'', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.HoaDon'') AND c.name = N''tienKhachDua''
          AND t.name = N''decimal'' AND c.precision = 18 AND c.scale = 2 AND c.is_nullable = 1)
        THROW 51064, N''dbo.HoaDon.tienKhachDua phải DECIMAL(18,2) NULL.'', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.HoaDon'') AND c.name = N''tienThua''
          AND t.name = N''decimal'' AND c.precision = 18 AND c.scale = 2 AND c.is_nullable = 1)
        THROW 51065, N''dbo.HoaDon.tienThua phải DECIMAL(18,2) NULL.'', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N''dbo.HoaDon'') AND c.name = N''nhanVienXacNhanChuyenKhoan''
          AND t.name = N''bit'' AND c.is_nullable = 1)
        THROW 51066, N''dbo.HoaDon.nhanVienXacNhanChuyenKhoan phải BIT NULL.'', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N''dbo.HoaDon'')
        AND name = N''CK_HoaDon_ThuTien'')
    BEGIN
        -- Compile this DDL after the conditional ALTER TABLE ADD statements above.
        -- SQL Server otherwise resolves the new column names before those ALTERs run.
        EXEC sys.sp_executesql N''
            ALTER TABLE dbo.HoaDon ADD CONSTRAINT CK_HoaDon_ThuTien CHECK (
                (phuongThucThanhToan IS NULL AND tienKhachDua IS NULL AND tienThua IS NULL
                    AND nhanVienXacNhanChuyenKhoan IS NULL)
                OR (trangThai = ''''DA_THANH_TOAN'''' AND phuongThucThanhToan = ''''TIEN_MAT''''
                    AND tienKhachDua IS NOT NULL AND tienThua IS NOT NULL
                    AND tienKhachDua >= 0 AND tienThua >= 0
                    AND tienKhachDua >= tongTien AND tienThua = tienKhachDua - tongTien
                    AND nhanVienXacNhanChuyenKhoan IS NULL)
                OR (trangThai = ''''DA_THANH_TOAN'''' AND phuongThucThanhToan = ''''CHUYEN_KHOAN''''
                    AND tienKhachDua IS NULL AND tienThua IS NULL AND nhanVienXacNhanChuyenKhoan = 1)
            );'';
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N''dbo.HoaDon'')
        AND name = N''CK_HoaDon_ThuTien'' AND is_disabled = 0 AND is_not_trusted = 0
        AND definition LIKE N''%phuongThucThanhToan%''
        AND definition LIKE N''%TIEN_MAT%'' AND definition LIKE N''%CHUYEN_KHOAN%''
        AND definition LIKE N''%tienKhachDua%'' AND definition LIKE N''%tienThua%''
        AND definition LIKE N''%nhanVienXacNhanChuyenKhoan%'' AND definition LIKE N''%tongTien%'')
        THROW 51067, N''Thiếu hoặc chưa kiểm chứng CK_HoaDon_ThuTien.'', 1;

    COMMIT TRANSACTION;
    PRINT N''S18 migration hoàn tất hoặc schema đã đúng; hóa đơn lịch sử không được gán phương thức thu.'';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
');

EXEC(N'USE [QuanLyCuaHangQuanAo];

-- FR11: nhà cung cấp và phiếu nhập; tăng dần, không reset dữ liệu.
-- Chạy sau baseline và các migration 04, 05, 06 trên database demo.
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;
    IF DB_NAME() <> N''QuanLyCuaHangQuanAo''
        THROW 51070, N''Migration 07 chỉ dành cho database demo QuanLyCuaHangQuanAo.'', 1;
    IF OBJECT_ID(N''dbo.NhanVien'', N''U'') IS NULL OR OBJECT_ID(N''dbo.BienThe'', N''U'') IS NULL
        THROW 51071, N''Cần baseline NhanVien và BienThe trước migration 07.'', 1;
    IF OBJECT_ID(N''dbo.LichSuKho'', N''U'') IS NULL
        THROW 51072, N''Cần chạy migration 05 trước migration 07.'', 1;

    IF OBJECT_ID(N''dbo.NhaCungCap'', N''U'') IS NULL
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
        WHERE c.object_id=OBJECT_ID(N''dbo.NhaCungCap'') AND c.name=N''maNCC'' AND t.name=N''varchar'' AND c.max_length=20 AND c.is_nullable=0)
       OR NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N''dbo.NhaCungCap'') AND c.name=N''tenNCC'' AND t.name=N''nvarchar'' AND c.max_length=300 AND c.is_nullable=0)
       OR NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N''dbo.NhaCungCap'') AND c.name=N''phienBan'' AND t.name=N''bigint'' AND c.is_nullable=0)
        THROW 51073, N''dbo.NhaCungCap hiện hữu không khớp cấu trúc migration 07.'', 1;

    IF OBJECT_ID(N''dbo.PhieuNhap'', N''U'') IS NULL
    BEGIN
        CREATE TABLE dbo.PhieuNhap (
            maPhieuNhap VARCHAR(20) NOT NULL CONSTRAINT PK_PhieuNhap PRIMARY KEY,
            maNCC VARCHAR(20) NOT NULL,
            maNVTao VARCHAR(20) NOT NULL,
            maNVXacNhan VARCHAR(20) NULL,
            ngayLap DATETIME2(0) NOT NULL,
            ngayXacNhan DATETIME2(0) NULL,
            trangThai VARCHAR(20) NOT NULL CONSTRAINT DF_PhieuNhap_TrangThai DEFAULT (''NHAP''),
            tongTien DECIMAL(18,2) NOT NULL CONSTRAINT DF_PhieuNhap_TongTien DEFAULT (0),
            operationIdTao VARCHAR(36) NOT NULL,
            operationIdXacNhan VARCHAR(36) NULL,
            phienBanTruocXacNhan BIGINT NULL,
            phienBan BIGINT NOT NULL CONSTRAINT DF_PhieuNhap_PhienBan DEFAULT (0),
            CONSTRAINT FK_PhieuNhap_NhaCungCap FOREIGN KEY (maNCC) REFERENCES dbo.NhaCungCap(maNCC),
            CONSTRAINT FK_PhieuNhap_NhanVienTao FOREIGN KEY (maNVTao) REFERENCES dbo.NhanVien(maNV),
            CONSTRAINT FK_PhieuNhap_NhanVienXacNhan FOREIGN KEY (maNVXacNhan) REFERENCES dbo.NhanVien(maNV),
            CONSTRAINT UQ_PhieuNhap_OperationTao UNIQUE (operationIdTao),
            CONSTRAINT CK_PhieuNhap_TrangThai CHECK (trangThai IN (''NHAP'',''DA_XAC_NHAN'',''DA_HUY'')),
            CONSTRAINT CK_PhieuNhap_Tien CHECK (tongTien >= 0),
            CONSTRAINT CK_PhieuNhap_PhienBan CHECK (phienBan >= 0),
            CONSTRAINT CK_PhieuNhap_XacNhan CHECK (
                (trangThai = ''DA_XAC_NHAN'' AND ngayXacNhan IS NOT NULL AND maNVXacNhan IS NOT NULL
                    AND operationIdXacNhan IS NOT NULL AND phienBanTruocXacNhan IS NOT NULL)
                OR (trangThai IN (''NHAP'',''DA_HUY'') AND ngayXacNhan IS NULL AND maNVXacNhan IS NULL
                    AND operationIdXacNhan IS NULL AND phienBanTruocXacNhan IS NULL))
        );
        CREATE INDEX IX_PhieuNhap_NgayTrangThai ON dbo.PhieuNhap(ngayLap, trangThai, maPhieuNhap);
        CREATE INDEX IX_PhieuNhap_NhaCungCap_Ngay ON dbo.PhieuNhap(maNCC, ngayLap, maPhieuNhap);
    END;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N''dbo.PhieuNhap'') AND name=N''UQ_PhieuNhap_OperationXacNhan'')
        CREATE UNIQUE INDEX UQ_PhieuNhap_OperationXacNhan ON dbo.PhieuNhap(operationIdXacNhan)
            WHERE operationIdXacNhan IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N''dbo.PhieuNhap'')
        AND name=N''UQ_PhieuNhap_OperationXacNhan'' AND is_unique=1 AND has_filter=1
        AND filter_definition LIKE N''%operationIdXacNhan%IS NOT NULL%'')
        THROW 51079, N''UQ_PhieuNhap_OperationXacNhan phải là unique filtered index, cho phép nhiều phiếu nháp NULL.'', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N''dbo.PhieuNhap'') AND c.name=N''maPhieuNhap'' AND t.name=N''varchar'' AND c.max_length=20 AND c.is_nullable=0)
       OR NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N''dbo.PhieuNhap'') AND c.name=N''tongTien'' AND t.name=N''decimal'' AND c.precision=18 AND c.scale=2 AND c.is_nullable=0)
       OR NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N''dbo.PhieuNhap'') AND c.name=N''phienBan'' AND t.name=N''bigint'' AND c.is_nullable=0)
        THROW 51074, N''dbo.PhieuNhap hiện hữu không khớp cấu trúc migration 07.'', 1;

    IF OBJECT_ID(N''dbo.ChiTietPhieuNhap'', N''U'') IS NULL
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
        WHERE c.object_id=OBJECT_ID(N''dbo.ChiTietPhieuNhap'') AND c.name=N''thanhTien'' AND t.name=N''decimal''
          AND c.precision=18 AND c.scale=2 AND c.is_computed=1)
        THROW 51075, N''dbo.ChiTietPhieuNhap.thanhTien phải là cột computed DECIMAL(18,2).'', 1;

    IF OBJECT_ID(N''dbo.PhieuNhapThaoTac'', N''U'') IS NULL
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
            CONSTRAINT CK_PhieuNhapThaoTac_Loai CHECK (loaiThaoTac IN (''CREATE'',''ADD_LINE'',''UPDATE_LINE'',''REMOVE_LINE'',''CONFIRM'',''CANCEL''))
        );
        CREATE INDEX IX_PhieuNhapThaoTac_Phieu ON dbo.PhieuNhapThaoTac(maPhieuNhap, thoiGian, operationId);
    END;
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N''dbo.PhieuNhapThaoTac'') AND c.name=N''operationId'' AND t.name=N''varchar'' AND c.max_length=36 AND c.is_nullable=0)
       OR NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N''dbo.PhieuNhapThaoTac'') AND c.name=N''fingerprint'' AND t.name=N''varchar'' AND c.max_length=64 AND c.is_nullable=0)
        THROW 51076, N''dbo.PhieuNhapThaoTac hiện hữu không khớp cấu trúc migration 07.'', 1;

    -- Migration 05 chỉ cho phép bốn nguồn; bổ sung nguồn nhập bằng migration mới.
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N''dbo.LichSuKho'')
        AND name=N''CK_LichSuKho_Nguon'' AND is_disabled=0 AND is_not_trusted=0
        AND definition LIKE N''%TON_MO_DAU%'' AND definition LIKE N''%KHOI_TAO_BIEN_THE%''
        AND definition LIKE N''%BAN_HANG%'' AND definition LIKE N''%DIEU_CHINH_TON%'')
       AND NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N''dbo.LichSuKho'')
        AND name=N''CK_LichSuKho_Nguon'' AND is_disabled=0 AND is_not_trusted=0
        AND definition LIKE N''%NHAP_HANG%'' AND definition LIKE N''%BAN_HANG%'')
        THROW 51077, N''CK_LichSuKho_Nguon không có baseline nhận diện được; cần rà soát thủ công.'', 1;

    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N''dbo.LichSuKho'')
        AND name=N''CK_LichSuKho_Nguon'' AND definition NOT LIKE N''%NHAP_HANG%'')
    BEGIN
        ALTER TABLE dbo.LichSuKho DROP CONSTRAINT CK_LichSuKho_Nguon;
        EXEC sys.sp_executesql N''ALTER TABLE dbo.LichSuKho WITH CHECK ADD CONSTRAINT CK_LichSuKho_Nguon
            CHECK (nguon IN (''''TON_MO_DAU'''',''''KHOI_TAO_BIEN_THE'''',''''BAN_HANG'''',''''DIEU_CHINH_TON'''',''''NHAP_HANG''''));'';
        ALTER TABLE dbo.LichSuKho WITH CHECK CHECK CONSTRAINT CK_LichSuKho_Nguon;
    END;
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N''dbo.LichSuKho'')
        AND name=N''CK_LichSuKho_Nguon'' AND is_disabled=0 AND is_not_trusted=0 AND definition LIKE N''%NHAP_HANG%'')
        THROW 51078, N''Không xác minh được CHECK constraint cho nguồn NHAP_HANG.'', 1;

    COMMIT TRANSACTION;
    PRINT N''Migration 07 hoàn tất; dữ liệu cũ được giữ nguyên.'';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
');

EXEC(N'USE [QuanLyCuaHangQuanAo];

SET XACT_ABORT ON;
BEGIN TRY
BEGIN TRANSACTION;

-- LoaiSanPham
INSERT dbo.[LoaiSanPham] ([maLoai],[tenLoai]) VALUES (N''LSP001'',N''Quần dài'');
INSERT dbo.[LoaiSanPham] ([maLoai],[tenLoai]) VALUES (N''LSP002'',N''Quần shorts'');
INSERT dbo.[LoaiSanPham] ([maLoai],[tenLoai]) VALUES (N''LSP003'',N''Áo Phông'');
INSERT dbo.[LoaiSanPham] ([maLoai],[tenLoai]) VALUES (N''LSP004'',N''Áo Polo'');
INSERT dbo.[LoaiSanPham] ([maLoai],[tenLoai]) VALUES (N''LSP005'',N''Áo khoác'');
INSERT dbo.[LoaiSanPham] ([maLoai],[tenLoai]) VALUES (N''LSP006'',N''Áo sơ mi'');

-- ThuongHieu
INSERT dbo.[ThuongHieu] ([maThuongHieu],[tenThuongHieu]) VALUES (N''TH001'',N''Canifa'');
INSERT dbo.[ThuongHieu] ([maThuongHieu],[tenThuongHieu]) VALUES (N''TH002'',N''Owen'');
INSERT dbo.[ThuongHieu] ([maThuongHieu],[tenThuongHieu]) VALUES (N''TH003'',N''Việt Tiến'');

-- SanPham
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1001'',N''Áo phông nữ basic cổ tim'',N''LSP003'',N''TH001'',149000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1002'',N''Áo phông nữ Cotton USA basic dáng suông'',N''LSP003'',N''TH003'',149000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1003'',N''Áo phông active unisex trẻ em oversize | Wicking'',N''LSP003'',N''TH001'',279000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1004'',N''Quần soóc bé trai túi hộp dáng rộng'',N''LSP002'',N''TH003'',349000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1005'',N''Áo phông unisex người lớn dáng rộng hình in Mickey & Friends'',N''LSP003'',N''TH001'',244300.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1006'',N''Áo phông active nam có hình in | Wicking'',N''LSP003'',N''TH001'',399000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1007'',N''Áo phông active nam | Wicking - Anti UV'',N''LSP003'',N''TH003'',399000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1008'',N''Áo polo active nam phối màu | Anti UV - Wicking'',N''LSP004'',N''TH002'',499000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1009'',N''Áo polo active nam | Anti UV - Wicking'',N''LSP004'',N''TH001'',499000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1010'',N''Áo polo nam cotton kẻ ngang'',N''LSP004'',N''TH001'',599000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1011'',N''Áo polo nam phối màu'',N''LSP004'',N''TH002'',599000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1012'',N''Áo polo unisex người lớn cotton dáng boxy có hình in'',N''LSP004'',N''TH001'',419300.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1013'',N''Quần soóc cargo nam dáng suông'',N''LSP002'',N''TH001'',549000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1014'',N''Quần soóc âu nam dáng suông'',N''LSP002'',N''TH003'',419000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1015'',N''Quần soóc nam cotton họa tiết'',N''LSP002'',N''TH001'',419300.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1016'',N''Quần soóc active nam hai lớp | Wicking'',N''LSP002'',N''TH001'',419300.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1017'',N''Quần soóc nam cotton lưng chun cài cúc'',N''LSP002'',N''TH001'',384000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1018'',N''Quần soóc nam cotton phối kẻ sọc'',N''LSP002'',N''TH003'',394000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1019'',N''Quần soóc nam cotton dáng rộng'',N''LSP002'',N''TH001'',499000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1020'',N''Quần soóc jeans unisex người lớn cotton dáng bermuda'',N''LSP002'',N''TH001'',599000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1021'',N''Áo sơ mi nam cotton ngắn tay'',N''LSP006'',N''TH001'',419300.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1022'',N''Áo sơ mi nam cotton họa tiết dáng rộng'',N''LSP006'',N''TH003'',599000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1023'',N''Áo sơ mi nam cotton kẻ sọc dáng rộng'',N''LSP006'',N''TH001'',419300.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1024'',N''Áo sơ mi unisex người lớn cotton in hình Mickey'',N''LSP006'',N''TH001'',419300.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1025'',N''Áo sơ mi nam cotton basic dáng ôm'',N''LSP006'',N''TH001'',549000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1026'',N''Áo sơ mi nam Cotton USA ngắn tay'',N''LSP006'',N''TH003'',279000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1027'',N''Áo sơ mi flannel dài tay nam họa tiết kẻ'',N''LSP006'',N''TH001'',419300.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1028'',N''Áo sơ mi nam cotton flannel họa tiết kẻ'',N''LSP006'',N''TH001'',179000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1029'',N''Áo sơ mi nam cotton dài tay dáng ôm'',N''LSP006'',N''TH003'',419300.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1030'',N''Quần jeans nữ cotton cạp cao dáng suông'',N''LSP001'',N''TH001'',799000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1031'',N''Quần leggings nữ dáng ôm | Wicking - Stretch'',N''LSP001'',N''TH002'',499000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1032'',N''Quần dài nữ rayon linen dáng rộng'',N''LSP001'',N''TH002'',419300.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1033'',N''Quần dài nữ dáng suông ống đứng hai lớp'',N''LSP001'',N''TH001'',599000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1034'',N''Quần dài nữ túi hộp dáng suông'',N''LSP001'',N''TH001'',699000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1035'',N''Quần khaki nữ dáng suông'',N''LSP001'',N''TH003'',419300.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1036'',N''Áo khoác chống nắng nam | Anti UV - Wicking'',N''LSP005'',N''TH001'',699000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1037'',N''Áo khoác gió active nam trượt nước | Anti UV - Light weight'',N''LSP005'',N''TH001'',524000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1038'',N''Áo khoác măng tô nam 2 lớp trượt nước'',N''LSP005'',N''TH002'',2999000.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1039'',N''Áo khoác lông vũ nam chống thấm nước'',N''LSP005'',N''TH003'',1199200.00,1);
INSERT dbo.[SanPham] ([maSP],[tenSP],[maLoai],[maThuongHieu],[giaBan],[trangThai]) VALUES (N''SP1040'',N''Áo khoác active nam interlock, wicking'',N''LSP005'',N''TH002'',899000.00,1);

-- BienThe
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000001'',N''SP1001'',N''Đỏ'',N''S'',10,1,3);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000002'',N''SP1001'',N''Đỏ'',N''M'',10,1,3);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000003'',N''SP1001'',N''Vàng'',N''S'',20,1,3);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000004'',N''SP1001'',N''Vàng'',N''M'',10,1,3);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000005'',N''SP1001'',N''Nâu'',N''S'',5,1,3);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000006'',N''SP1001'',N''Nâu'',N''M'',10,1,3);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000007'',N''SP1002'',N''Nâu'',N''S'',10,1,3);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000008'',N''SP1002'',N''Nâu'',N''M'',10,1,3);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000009'',N''SP1002'',N''Đen'',N''S'',10,1,3);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000010'',N''SP1002'',N''Đen'',N''M'',10,1,3);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000011'',N''SP1002'',N''Hồng'',N''S'',10,1,3);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000012'',N''SP1002'',N''Hồng'',N''M'',10,1,3);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000013'',N''SP1003'',N''Trắng'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000014'',N''SP1003'',N''Trắng'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000015'',N''SP1003'',N''Xanh dương'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000016'',N''SP1003'',N''Xanh dương'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000017'',N''SP1003'',N''Đen'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000018'',N''SP1003'',N''Đen'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000019'',N''SP1004'',N''Xanh dương'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000020'',N''SP1004'',N''Xanh dương'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000021'',N''SP1004'',N''Xám'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000022'',N''SP1004'',N''Xám'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000023'',N''SP1005'',N''Xanh'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000024'',N''SP1005'',N''Xanh'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000025'',N''SP1005'',N''Trắng'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000026'',N''SP1005'',N''Trắng'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000027'',N''SP1006'',N''Xanh họa tiết'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000028'',N''SP1006'',N''Xanh họa tiết'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000029'',N''SP1006'',N''Đen họa tiết'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000030'',N''SP1006'',N''Đen họa tiết'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000031'',N''SP1007'',N''Đen'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000032'',N''SP1007'',N''Đen'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000033'',N''SP1007'',N''Xanh dương'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000034'',N''SP1007'',N''Xanh dương'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000035'',N''SP1008'',N''Xanh dương'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000036'',N''SP1008'',N''Xanh dương'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000037'',N''SP1009'',N''Đen'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000038'',N''SP1009'',N''Đen'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000039'',N''SP1009'',N''Xanh dương'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000040'',N''SP1009'',N''Xanh dương'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000041'',N''SP1010'',N''Xanh dương kẻ sọc'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000042'',N''SP1010'',N''Xanh dương kẻ sọc'',N''M'',5,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000043'',N''SP1011'',N''Xanh lơ'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000044'',N''SP1011'',N''Xanh lơ'',N''L'',20,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000045'',N''SP1011'',N''Trắng'',N''M'',30,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000046'',N''SP1011'',N''Trắng'',N''L'',40,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000047'',N''SP1012'',N''Xanh dương'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000048'',N''SP1012'',N''Xanh dương'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000049'',N''SP1012'',N''Xanh lá'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000050'',N''SP1012'',N''Xanh lá'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000051'',N''SP1013'',N''Xanh lá'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000052'',N''SP1013'',N''Xanh lá'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000053'',N''SP1013'',N''Xám'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000054'',N''SP1013'',N''Xám'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000055'',N''SP1014'',N''Be'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000056'',N''SP1014'',N''Be'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000057'',N''SP1014'',N''Đen'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000058'',N''SP1014'',N''Đen'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000059'',N''SP1015'',N''Xanh lá họa tiết'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000060'',N''SP1015'',N''Xanh lá họa tiết'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000061'',N''SP1015'',N''Xanh họa tiết'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000062'',N''SP1015'',N''Xanh họa tiết'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000063'',N''SP1016'',N''Đen'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000064'',N''SP1016'',N''Đen'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000065'',N''SP1016'',N''Trắng'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000066'',N''SP1016'',N''Trắng'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000067'',N''SP1017'',N''Đen'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000068'',N''SP1017'',N''Đen'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000069'',N''SP1017'',N''Xanh dương'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000070'',N''SP1017'',N''Xanh dương'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000071'',N''SP1018'',N''Xám'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000072'',N''SP1018'',N''Xám'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000073'',N''SP1018'',N''Xanh dương'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000074'',N''SP1018'',N''Xanh dương'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000075'',N''SP1019'',N''Trắng'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000076'',N''SP1019'',N''Trắng'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000077'',N''SP1019'',N''Xám'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000078'',N''SP1019'',N''Xám'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000079'',N''SP1020'',N''Xanh Indigo'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000080'',N''SP1020'',N''Xanh Indigo'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000081'',N''SP1021'',N''Trắng'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000082'',N''SP1021'',N''Trắng'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000083'',N''SP1021'',N''Be'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000084'',N''SP1021'',N''Be'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000085'',N''SP1022'',N''Trắng họa tiết'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000086'',N''SP1022'',N''Trắng họa tiết'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000087'',N''SP1023'',N''Xám kẻ sọc'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000088'',N''SP1023'',N''Xám kẻ sọc'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000089'',N''SP1023'',N''Xanh dương kẻ sọc'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000090'',N''SP1023'',N''Xanh dương kẻ sọc'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000091'',N''SP1024'',N''Be kẻ sọc'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000092'',N''SP1024'',N''Be kẻ sọc'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000093'',N''SP1024'',N''Xanh dương kẻ sọc'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000094'',N''SP1024'',N''Xanh dương kẻ sọc'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000095'',N''SP1025'',N''Xanh lơ'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000096'',N''SP1025'',N''Xanh lơ'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000097'',N''SP1025'',N''Trắng'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000098'',N''SP1025'',N''Trắng'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000099'',N''SP1026'',N''Trắng'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000100'',N''SP1026'',N''Trắng'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000101'',N''SP1026'',N''Hồng'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000102'',N''SP1026'',N''Hồng'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000103'',N''SP1027'',N''Xanh lá caro'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000104'',N''SP1027'',N''Xanh lá caro'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000105'',N''SP1027'',N''Đỏ caro'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000106'',N''SP1027'',N''Đỏ caro'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000107'',N''SP1028'',N''Xám caro'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000108'',N''SP1028'',N''Xám caro'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000109'',N''SP1029'',N''Xám'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000110'',N''SP1029'',N''Xám'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000111'',N''SP1029'',N''Xanh lơ'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000112'',N''SP1029'',N''Xanh lơ'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000113'',N''SP1030'',N''Be'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000114'',N''SP1030'',N''Be'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000115'',N''SP1030'',N''Trắng'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000116'',N''SP1030'',N''Trắng'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000117'',N''SP1031'',N''Đen'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000118'',N''SP1031'',N''Đen'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000119'',N''SP1032'',N''Be'',N''XS'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000120'',N''SP1032'',N''Be'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000121'',N''SP1032'',N''Trắng'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000122'',N''SP1032'',N''Trắng'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000123'',N''SP1033'',N''Đen'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000124'',N''SP1033'',N''Đen'',N''XL'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000125'',N''SP1033'',N''Xám'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000126'',N''SP1033'',N''Xám'',N''XL'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000127'',N''SP1034'',N''Be'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000128'',N''SP1034'',N''Be'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000129'',N''SP1034'',N''Xanh lá'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000130'',N''SP1034'',N''Xanh lá'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000131'',N''SP1034'',N''Đen'',N''XL'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000132'',N''SP1034'',N''Đen'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000133'',N''SP1035'',N''Đen'',N''XL'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000134'',N''SP1035'',N''Đen'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000135'',N''SP1035'',N''Trắng'',N''XL'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000136'',N''SP1035'',N''Trắng'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000137'',N''SP1036'',N''Xanh dương'',N''XL'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000138'',N''SP1036'',N''Xanh dương'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000139'',N''SP1036'',N''Xám'',N''XL'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000140'',N''SP1036'',N''Xám'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000141'',N''SP1037'',N''Xanh dương'',N''XL'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000142'',N''SP1037'',N''Xanh dương'',N''L'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000143'',N''SP1037'',N''Đen'',N''XL'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000144'',N''SP1037'',N''Đen'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000145'',N''SP1038'',N''Xanh dương'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000146'',N''SP1038'',N''Xanh dương'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000147'',N''SP1039'',N''Trắng'',N''XL'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000148'',N''SP1039'',N''Trắng'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000149'',N''SP1039'',N''Xanh dương'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000150'',N''SP1039'',N''Xanh dương'',N''M'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000151'',N''SP1040'',N''Be'',N''S'',10,1,0);
INSERT dbo.[BienThe] ([maBienThe],[maSP],[mauSac],[kichCo],[soLuongTon],[trangThai],[phienBan]) VALUES (N''BT000152'',N''SP1040'',N''Be'',N''M'',10,1,0);

-- AnhSanPham
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000001'',N''SP1001'',N''Đỏ'',N''images/products/1001/red-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000002'',N''SP1001'',N''Đỏ'',N''images/products/1001/red-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000003'',N''SP1001'',N''Đỏ'',N''images/products/1001/red-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000004'',N''SP1001'',N''Đỏ'',N''images/products/1001/red-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000005'',N''SP1001'',N''Vàng'',N''images/products/1001/yellow-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000006'',N''SP1001'',N''Vàng'',N''images/products/1001/yellow-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000007'',N''SP1001'',N''Vàng'',N''images/products/1001/yellow-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000008'',N''SP1001'',N''Vàng'',N''images/products/1001/yellow-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000009'',N''SP1001'',N''Vàng'',N''images/products/1001/yellow-5.jpg'',5,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000010'',N''SP1001'',N''Nâu'',N''images/products/1001/brown-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000011'',N''SP1001'',N''Nâu'',N''images/products/1001/brown-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000012'',N''SP1001'',N''Nâu'',N''images/products/1001/brown-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000013'',N''SP1001'',N''Nâu'',N''images/products/1001/brown-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000014'',N''SP1001'',N''Nâu'',N''images/products/1001/brown-5.jpg'',5,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000015'',N''SP1002'',N''Nâu'',N''images/products/1002/brown-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000016'',N''SP1002'',N''Nâu'',N''images/products/1002/brown-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000017'',N''SP1002'',N''Nâu'',N''images/products/1002/brown-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000018'',N''SP1002'',N''Nâu'',N''images/products/1002/brown-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000019'',N''SP1002'',N''Nâu'',N''images/products/1002/brown-5.jpg'',5,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000020'',N''SP1002'',N''Đen'',N''images/products/1002/black-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000021'',N''SP1002'',N''Đen'',N''images/products/1002/black-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000022'',N''SP1002'',N''Đen'',N''images/products/1002/black-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000023'',N''SP1002'',N''Đen'',N''images/products/1002/black-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000024'',N''SP1002'',N''Đen'',N''images/products/1002/black-5.jpg'',5,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000025'',N''SP1002'',N''Hồng'',N''images/products/1002/pink-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000026'',N''SP1002'',N''Hồng'',N''images/products/1002/pink-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000027'',N''SP1002'',N''Hồng'',N''images/products/1002/pink-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000028'',N''SP1002'',N''Hồng'',N''images/products/1002/pink-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000029'',N''SP1002'',N''Hồng'',N''images/products/1002/pink-5.jpg'',5,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000030'',N''SP1003'',N''Đen'',N''images/products/1003/black-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000031'',N''SP1003'',N''Đen'',N''images/products/1003/black-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000032'',N''SP1003'',N''Đen'',N''images/products/1003/black-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000033'',N''SP1003'',N''Đen'',N''images/products/1003/black-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000034'',N''SP1003'',N''Xanh dương'',N''images/products/1003/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000035'',N''SP1003'',N''Xanh dương'',N''images/products/1003/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000036'',N''SP1003'',N''Xanh dương'',N''images/products/1003/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000037'',N''SP1003'',N''Xanh dương'',N''images/products/1003/blue-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000038'',N''SP1003'',N''Trắng'',N''images/products/1003/white-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000039'',N''SP1003'',N''Trắng'',N''images/products/1003/white-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000040'',N''SP1003'',N''Trắng'',N''images/products/1003/white-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000041'',N''SP1003'',N''Trắng'',N''images/products/1003/white-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000042'',N''SP1004'',N''Xanh dương'',N''images/products/1004/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000043'',N''SP1004'',N''Xanh dương'',N''images/products/1004/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000044'',N''SP1004'',N''Xanh dương'',N''images/products/1004/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000045'',N''SP1004'',N''Xanh dương'',N''images/products/1004/blue-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000046'',N''SP1004'',N''Xám'',N''images/products/1004/grey-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000047'',N''SP1004'',N''Xám'',N''images/products/1004/grey-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000048'',N''SP1004'',N''Xám'',N''images/products/1004/grey-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000049'',N''SP1004'',N''Xám'',N''images/products/1004/grey-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000050'',N''SP1005'',N''Xanh'',N''images/products/1005/lightblue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000051'',N''SP1005'',N''Xanh'',N''images/products/1005/lightblue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000052'',N''SP1005'',N''Xanh'',N''images/products/1005/lightblue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000053'',N''SP1005'',N''Xanh'',N''images/products/1005/lightblue-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000054'',N''SP1005'',N''Trắng'',N''images/products/1005/white-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000055'',N''SP1005'',N''Trắng'',N''images/products/1005/white-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000056'',N''SP1005'',N''Trắng'',N''images/products/1005/white-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000057'',N''SP1005'',N''Trắng'',N''images/products/1005/white-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000058'',N''SP1006'',N''Đen họa tiết'',N''images/products/1006/black-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000059'',N''SP1006'',N''Đen họa tiết'',N''images/products/1006/black-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000060'',N''SP1006'',N''Đen họa tiết'',N''images/products/1006/black-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000061'',N''SP1006'',N''Đen họa tiết'',N''images/products/1006/black-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000062'',N''SP1006'',N''Đen họa tiết'',N''images/products/1006/black-5.jpg'',5,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000063'',N''SP1006'',N''Xanh họa tiết'',N''images/products/1006/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000064'',N''SP1006'',N''Xanh họa tiết'',N''images/products/1006/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000065'',N''SP1006'',N''Xanh họa tiết'',N''images/products/1006/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000066'',N''SP1006'',N''Xanh họa tiết'',N''images/products/1006/blue-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000067'',N''SP1006'',N''Xanh họa tiết'',N''images/products/1006/blue-5.jpg'',5,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000068'',N''SP1007'',N''Đen'',N''images/products/1007/black-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000069'',N''SP1007'',N''Đen'',N''images/products/1007/black-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000070'',N''SP1007'',N''Đen'',N''images/products/1007/black-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000071'',N''SP1007'',N''Xanh dương'',N''images/products/1007/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000072'',N''SP1007'',N''Xanh dương'',N''images/products/1007/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000073'',N''SP1007'',N''Xanh dương'',N''images/products/1007/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000074'',N''SP1008'',N''Xanh dương'',N''images/products/1008/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000075'',N''SP1008'',N''Xanh dương'',N''images/products/1008/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000076'',N''SP1008'',N''Xanh dương'',N''images/products/1008/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000077'',N''SP1009'',N''Đen'',N''images/products/1009/black-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000078'',N''SP1009'',N''Đen'',N''images/products/1009/black-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000079'',N''SP1009'',N''Đen'',N''images/products/1009/black-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000080'',N''SP1009'',N''Xanh dương'',N''images/products/1009/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000081'',N''SP1009'',N''Xanh dương'',N''images/products/1009/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000082'',N''SP1009'',N''Xanh dương'',N''images/products/1009/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000083'',N''SP1010'',N''Xanh dương kẻ sọc'',N''images/products/1010/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000084'',N''SP1010'',N''Xanh dương kẻ sọc'',N''images/products/1010/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000085'',N''SP1010'',N''Xanh dương kẻ sọc'',N''images/products/1010/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000086'',N''SP1011'',N''Xanh lơ'',N''images/products/1011/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000087'',N''SP1011'',N''Xanh lơ'',N''images/products/1011/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000088'',N''SP1011'',N''Xanh lơ'',N''images/products/1011/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000089'',N''SP1011'',N''Trắng'',N''images/products/1011/white-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000090'',N''SP1011'',N''Trắng'',N''images/products/1011/white-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000091'',N''SP1011'',N''Trắng'',N''images/products/1011/white-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000092'',N''SP1012'',N''Xanh dương'',N''images/products/1012/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000093'',N''SP1012'',N''Xanh dương'',N''images/products/1012/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000094'',N''SP1012'',N''Xanh dương'',N''images/products/1012/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000095'',N''SP1012'',N''Xanh lá'',N''images/products/1012/green-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000096'',N''SP1012'',N''Xanh lá'',N''images/products/1012/green-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000097'',N''SP1012'',N''Xanh lá'',N''images/products/1012/green-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000098'',N''SP1013'',N''Xanh lá'',N''images/products/1013/green-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000099'',N''SP1013'',N''Xanh lá'',N''images/products/1013/green-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000100'',N''SP1013'',N''Xanh lá'',N''images/products/1013/green-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000101'',N''SP1013'',N''Xám'',N''images/products/1013/grey-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000102'',N''SP1013'',N''Xám'',N''images/products/1013/grey-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000103'',N''SP1013'',N''Xám'',N''images/products/1013/grey-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000104'',N''SP1014'',N''Be'',N''images/products/1014/beige-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000105'',N''SP1014'',N''Be'',N''images/products/1014/beige-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000106'',N''SP1014'',N''Be'',N''images/products/1014/beige-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000107'',N''SP1014'',N''Đen'',N''images/products/1014/black-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000108'',N''SP1014'',N''Đen'',N''images/products/1014/black-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000109'',N''SP1014'',N''Đen'',N''images/products/1014/black-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000110'',N''SP1015'',N''Xanh họa tiết'',N''images/products/1015/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000111'',N''SP1015'',N''Xanh họa tiết'',N''images/products/1015/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000112'',N''SP1015'',N''Xanh họa tiết'',N''images/products/1015/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000113'',N''SP1015'',N''Xanh lá họa tiết'',N''images/products/1015/green-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000114'',N''SP1015'',N''Xanh lá họa tiết'',N''images/products/1015/green-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000115'',N''SP1015'',N''Xanh lá họa tiết'',N''images/products/1015/green-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000116'',N''SP1016'',N''Đen'',N''images/products/1016/black-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000117'',N''SP1016'',N''Đen'',N''images/products/1016/black-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000118'',N''SP1016'',N''Đen'',N''images/products/1016/black-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000119'',N''SP1016'',N''Trắng'',N''images/products/1016/white-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000120'',N''SP1016'',N''Trắng'',N''images/products/1016/white-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000121'',N''SP1016'',N''Trắng'',N''images/products/1016/white-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000122'',N''SP1017'',N''Đen'',N''images/products/1017/black-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000123'',N''SP1017'',N''Đen'',N''images/products/1017/black-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000124'',N''SP1017'',N''Đen'',N''images/products/1017/black-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000125'',N''SP1017'',N''Xanh dương'',N''images/products/1017/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000126'',N''SP1017'',N''Xanh dương'',N''images/products/1017/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000127'',N''SP1017'',N''Xanh dương'',N''images/products/1017/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000128'',N''SP1018'',N''Xanh dương'',N''images/products/1018/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000129'',N''SP1018'',N''Xanh dương'',N''images/products/1018/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000130'',N''SP1018'',N''Xanh dương'',N''images/products/1018/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000131'',N''SP1018'',N''Xám'',N''images/products/1018/grey-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000132'',N''SP1018'',N''Xám'',N''images/products/1018/grey-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000133'',N''SP1018'',N''Xám'',N''images/products/1018/grey-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000134'',N''SP1019'',N''Xám'',N''images/products/1019/grey-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000135'',N''SP1019'',N''Xám'',N''images/products/1019/grey-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000136'',N''SP1019'',N''Xám'',N''images/products/1019/grey-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000137'',N''SP1019'',N''Trắng'',N''images/products/1019/white-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000138'',N''SP1019'',N''Trắng'',N''images/products/1019/white-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000139'',N''SP1019'',N''Trắng'',N''images/products/1019/white-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000140'',N''SP1020'',N''Xanh Indigo'',N''images/products/1020/indigo-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000141'',N''SP1020'',N''Xanh Indigo'',N''images/products/1020/indigo-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000142'',N''SP1020'',N''Xanh Indigo'',N''images/products/1020/indigo-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000143'',N''SP1021'',N''Be'',N''images/products/1021/beige-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000144'',N''SP1021'',N''Be'',N''images/products/1021/beige-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000145'',N''SP1021'',N''Be'',N''images/products/1021/beige-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000146'',N''SP1021'',N''Trắng'',N''images/products/1021/white-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000147'',N''SP1021'',N''Trắng'',N''images/products/1021/white-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000148'',N''SP1021'',N''Trắng'',N''images/products/1021/white-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000149'',N''SP1022'',N''Trắng họa tiết'',N''images/products/1022/white-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000150'',N''SP1022'',N''Trắng họa tiết'',N''images/products/1022/white-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000151'',N''SP1022'',N''Trắng họa tiết'',N''images/products/1022/white-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000152'',N''SP1023'',N''Xanh dương kẻ sọc'',N''images/products/1023/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000153'',N''SP1023'',N''Xanh dương kẻ sọc'',N''images/products/1023/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000154'',N''SP1023'',N''Xanh dương kẻ sọc'',N''images/products/1023/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000155'',N''SP1023'',N''Xám kẻ sọc'',N''images/products/1023/grey-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000156'',N''SP1023'',N''Xám kẻ sọc'',N''images/products/1023/grey-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000157'',N''SP1023'',N''Xám kẻ sọc'',N''images/products/1023/grey-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000158'',N''SP1024'',N''Be kẻ sọc'',N''images/products/1024/beige-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000159'',N''SP1024'',N''Be kẻ sọc'',N''images/products/1024/beige-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000160'',N''SP1024'',N''Be kẻ sọc'',N''images/products/1024/beige-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000161'',N''SP1024'',N''Xanh dương kẻ sọc'',N''images/products/1024/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000162'',N''SP1024'',N''Xanh dương kẻ sọc'',N''images/products/1024/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000163'',N''SP1024'',N''Xanh dương kẻ sọc'',N''images/products/1024/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000164'',N''SP1025'',N''Xanh lơ'',N''images/products/1025/cyan-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000165'',N''SP1025'',N''Xanh lơ'',N''images/products/1025/cyan-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000166'',N''SP1025'',N''Xanh lơ'',N''images/products/1025/cyan-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000167'',N''SP1025'',N''Trắng'',N''images/products/1025/white-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000168'',N''SP1025'',N''Trắng'',N''images/products/1025/white-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000169'',N''SP1025'',N''Trắng'',N''images/products/1025/white-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000170'',N''SP1026'',N''Hồng'',N''images/products/1026/pink-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000171'',N''SP1026'',N''Hồng'',N''images/products/1026/pink-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000172'',N''SP1026'',N''Hồng'',N''images/products/1026/pink-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000173'',N''SP1026'',N''Trắng'',N''images/products/1026/white-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000174'',N''SP1026'',N''Trắng'',N''images/products/1026/white-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000175'',N''SP1026'',N''Trắng'',N''images/products/1026/white-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000176'',N''SP1027'',N''Xanh lá caro'',N''images/products/1027/greencaro-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000177'',N''SP1027'',N''Xanh lá caro'',N''images/products/1027/greencaro-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000178'',N''SP1027'',N''Xanh lá caro'',N''images/products/1027/greencaro-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000179'',N''SP1027'',N''Đỏ caro'',N''images/products/1027/redcaro-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000180'',N''SP1027'',N''Đỏ caro'',N''images/products/1027/redcaro-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000181'',N''SP1027'',N''Đỏ caro'',N''images/products/1027/redcaro-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000182'',N''SP1028'',N''Xám caro'',N''images/products/1028/greycaro-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000183'',N''SP1028'',N''Xám caro'',N''images/products/1028/greycaro-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000184'',N''SP1028'',N''Xám caro'',N''images/products/1028/greycaro-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000185'',N''SP1029'',N''Xanh lơ'',N''images/products/1029/cyan-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000186'',N''SP1029'',N''Xanh lơ'',N''images/products/1029/cyan-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000187'',N''SP1029'',N''Xanh lơ'',N''images/products/1029/cyan-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000188'',N''SP1029'',N''Xám'',N''images/products/1029/grey-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000189'',N''SP1029'',N''Xám'',N''images/products/1029/grey-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000190'',N''SP1029'',N''Xám'',N''images/products/1029/grey-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000191'',N''SP1030'',N''Be'',N''images/products/1030/beige-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000192'',N''SP1030'',N''Be'',N''images/products/1030/beige-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000193'',N''SP1030'',N''Be'',N''images/products/1030/beige-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000194'',N''SP1030'',N''Be'',N''images/products/1030/beige-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000195'',N''SP1030'',N''Trắng'',N''images/products/1030/white-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000196'',N''SP1030'',N''Trắng'',N''images/products/1030/white-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000197'',N''SP1030'',N''Trắng'',N''images/products/1030/white-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000198'',N''SP1030'',N''Trắng'',N''images/products/1030/white-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000199'',N''SP1031'',N''Đen'',N''images/products/1031/black-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000200'',N''SP1031'',N''Đen'',N''images/products/1031/black-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000201'',N''SP1031'',N''Đen'',N''images/products/1031/black-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000202'',N''SP1031'',N''Đen'',N''images/products/1031/black-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000203'',N''SP1032'',N''Be'',N''images/products/1032/beige-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000204'',N''SP1032'',N''Be'',N''images/products/1032/beige-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000205'',N''SP1032'',N''Be'',N''images/products/1032/beige-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000206'',N''SP1032'',N''Be'',N''images/products/1032/beige-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000207'',N''SP1032'',N''Trắng'',N''images/products/1032/white-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000208'',N''SP1032'',N''Trắng'',N''images/products/1032/white-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000209'',N''SP1032'',N''Trắng'',N''images/products/1032/white-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000210'',N''SP1032'',N''Trắng'',N''images/products/1032/white-4.jpg'',4,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000211'',N''SP1033'',N''Đen'',N''images/products/1033/black-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000212'',N''SP1033'',N''Đen'',N''images/products/1033/black-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000213'',N''SP1033'',N''Đen'',N''images/products/1033/black-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000214'',N''SP1033'',N''Xám'',N''images/products/1033/grey-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000215'',N''SP1033'',N''Xám'',N''images/products/1033/grey-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000216'',N''SP1033'',N''Xám'',N''images/products/1033/grey-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000217'',N''SP1034'',N''Be'',N''images/products/1034/beige-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000218'',N''SP1034'',N''Be'',N''images/products/1034/beige-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000219'',N''SP1034'',N''Be'',N''images/products/1034/beige-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000220'',N''SP1034'',N''Đen'',N''images/products/1034/black-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000221'',N''SP1034'',N''Đen'',N''images/products/1034/black-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000222'',N''SP1034'',N''Đen'',N''images/products/1034/black-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000223'',N''SP1034'',N''Xanh lá'',N''images/products/1034/green-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000224'',N''SP1034'',N''Xanh lá'',N''images/products/1034/green-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000225'',N''SP1034'',N''Xanh lá'',N''images/products/1034/green-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000226'',N''SP1035'',N''Đen'',N''images/products/1035/black-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000227'',N''SP1035'',N''Đen'',N''images/products/1035/black-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000228'',N''SP1035'',N''Đen'',N''images/products/1035/black-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000229'',N''SP1035'',N''Trắng'',N''images/products/1035/white-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000230'',N''SP1035'',N''Trắng'',N''images/products/1035/white-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000231'',N''SP1035'',N''Trắng'',N''images/products/1035/white-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000232'',N''SP1036'',N''Xanh dương'',N''images/products/1036/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000233'',N''SP1036'',N''Xanh dương'',N''images/products/1036/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000234'',N''SP1036'',N''Xanh dương'',N''images/products/1036/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000235'',N''SP1036'',N''Xám'',N''images/products/1036/grey-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000236'',N''SP1036'',N''Xám'',N''images/products/1036/grey-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000237'',N''SP1036'',N''Xám'',N''images/products/1036/grey-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000238'',N''SP1037'',N''Đen'',N''images/products/1037/black-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000239'',N''SP1037'',N''Đen'',N''images/products/1037/black-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000240'',N''SP1037'',N''Đen'',N''images/products/1037/black-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000241'',N''SP1037'',N''Xanh dương'',N''images/products/1037/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000242'',N''SP1037'',N''Xanh dương'',N''images/products/1037/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000243'',N''SP1037'',N''Xanh dương'',N''images/products/1037/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000244'',N''SP1038'',N''Xanh dương'',N''images/products/1038/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000245'',N''SP1038'',N''Xanh dương'',N''images/products/1038/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000246'',N''SP1038'',N''Xanh dương'',N''images/products/1038/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000247'',N''SP1039'',N''Xanh dương'',N''images/products/1039/blue-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000248'',N''SP1039'',N''Xanh dương'',N''images/products/1039/blue-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000249'',N''SP1039'',N''Xanh dương'',N''images/products/1039/blue-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000250'',N''SP1039'',N''Trắng'',N''images/products/1039/white-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000251'',N''SP1039'',N''Trắng'',N''images/products/1039/white-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000252'',N''SP1039'',N''Trắng'',N''images/products/1039/white-3.jpg'',3,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000253'',N''SP1040'',N''Be'',N''images/products/1040/beige-1.jpg'',1,1);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000254'',N''SP1040'',N''Be'',N''images/products/1040/beige-2.jpg'',2,0);
INSERT dbo.[AnhSanPham] ([maAnh],[maSP],[mauSac],[duongDan],[thuTu],[anhChinh]) VALUES (N''ANH000255'',N''SP1040'',N''Be'',N''images/products/1040/beige-3.jpg'',3,0);

-- NhanVien
INSERT dbo.[NhanVien] ([maNV],[hoTen],[gioiTinh],[ngaySinh],[sdt],[email],[diaChi],[chucVu],[trangThai]) VALUES (N''NVDEMO01'',N''Nguyễn Minh Anh'',1,''1998-06-15'',N''0909000101'',N''demo.quanly@example.com'',N''TP. Hồ Chí Minh'',N''Quản lý cửa hàng'',1);
INSERT dbo.[NhanVien] ([maNV],[hoTen],[gioiTinh],[ngaySinh],[sdt],[email],[diaChi],[chucVu],[trangThai]) VALUES (N''NVDEMO02'',N''Trần Thu Hà'',0,''1998-06-15'',N''0909000102'',N''demo.banhang@example.com'',N''TP. Hồ Chí Minh'',N''Nhân viên bán hàng'',1);
INSERT dbo.[NhanVien] ([maNV],[hoTen],[gioiTinh],[ngaySinh],[sdt],[email],[diaChi],[chucVu],[trangThai]) VALUES (N''NVDEMO03'',N''Lê Hoàng Nam'',1,''1998-06-15'',N''0909000103'',N''demo.banhang2@example.com'',N''TP. Hồ Chí Minh'',N''Nhân viên bán hàng'',1);
INSERT dbo.[NhanVien] ([maNV],[hoTen],[gioiTinh],[ngaySinh],[sdt],[email],[diaChi],[chucVu],[trangThai]) VALUES (N''NVQL001'',N''Nguyen Van A'',NULL,NULL,N''0123456789'',NULL,NULL,N''Quản lý'',1);

-- TaiKhoan
INSERT dbo.[TaiKhoan] ([username],[maNV],[matKhauHash],[vaiTro],[trangThai]) VALUES (N''demo.banhang'',N''NVDEMO02'',N''pbkdf2-sha256$600000$VmZ3hU6XyZkxx21V7prqRg==$L/AgiYvqAN9fTcvUwUs7xluI+Ti1EALC851cwg2+7Fw='',N''NHAN_VIEN_BAN_HANG'',1);
INSERT dbo.[TaiKhoan] ([username],[maNV],[matKhauHash],[vaiTro],[trangThai]) VALUES (N''demo.banhang2'',N''NVDEMO03'',N''pbkdf2-sha256$600000$wo4fsq4OxzReFQlHsQTdNA==$knGU8msxGaT7rGGgQVCW9XFHL7ZY2g54NjSbx10eee8='',N''NHAN_VIEN_BAN_HANG'',1);
INSERT dbo.[TaiKhoan] ([username],[maNV],[matKhauHash],[vaiTro],[trangThai]) VALUES (N''demo.quanly'',N''NVDEMO01'',N''pbkdf2-sha256$600000$V199N58bNUwKmGf/nv2elA==$1apVxhPjtVZh5vmU8BSzy6jps3yitWJCf172Pin1lXE='',N''NHAN_VIEN_QUAN_LY'',1);
INSERT dbo.[TaiKhoan] ([username],[maNV],[matKhauHash],[vaiTro],[trangThai]) VALUES (N''QUAN_LY'',N''NVQL001'',N''pbkdf2-sha256$600000$R3raZ2VKF8e0JhL8eHzhzw==$NhL3WOKb1ODw4q9wsHFEMXthNNp8YbirdJNe0+rAQgo='',N''NHAN_VIEN_QUAN_LY'',1);

-- KhachHang
INSERT dbo.[KhachHang] ([maKH],[hoTen],[sdt],[email],[diaChi],[trangThai]) VALUES (N''KHDEMO01'',N''Nguyễn Ngọc Mai'',N''0909110001'',N''khach.demo1@example.com'',N''12 Nguyễn Trãi, TP. Hồ Chí Minh'',1);
INSERT dbo.[KhachHang] ([maKH],[hoTen],[sdt],[email],[diaChi],[trangThai]) VALUES (N''KHDEMO02'',N''Trần Quốc Bảo'',N''0909110002'',N''khach.demo2@example.com'',N''17 Nguyễn Trãi, TP. Hồ Chí Minh'',1);
INSERT dbo.[KhachHang] ([maKH],[hoTen],[sdt],[email],[diaChi],[trangThai]) VALUES (N''KHDEMO03'',N''Lê Phương Linh'',N''0909110003'',N''khach.demo3@example.com'',N''22 Nguyễn Trãi, TP. Hồ Chí Minh'',1);
INSERT dbo.[KhachHang] ([maKH],[hoTen],[sdt],[email],[diaChi],[trangThai]) VALUES (N''KHDEMO04'',N''Phạm Hoàng Long'',N''0909110004'',N''khach.demo4@example.com'',N''27 Nguyễn Trãi, TP. Hồ Chí Minh'',1);
INSERT dbo.[KhachHang] ([maKH],[hoTen],[sdt],[email],[diaChi],[trangThai]) VALUES (N''KHDEMO05'',N''Võ Thảo Vy'',N''0909110005'',N''khach.demo5@example.com'',N''32 Nguyễn Trãi, TP. Hồ Chí Minh'',1);
INSERT dbo.[KhachHang] ([maKH],[hoTen],[sdt],[email],[diaChi],[trangThai]) VALUES (N''KHDEMO06'',N''Đặng Minh Khang'',N''0909110006'',N''khach.demo6@example.com'',N''37 Nguyễn Trãi, TP. Hồ Chí Minh'',1);
INSERT dbo.[KhachHang] ([maKH],[hoTen],[sdt],[email],[diaChi],[trangThai]) VALUES (N''KHDEMO07'',N''Bùi Thanh Trúc'',N''0909110007'',N''khach.demo7@example.com'',N''42 Nguyễn Trãi, TP. Hồ Chí Minh'',1);
INSERT dbo.[KhachHang] ([maKH],[hoTen],[sdt],[email],[diaChi],[trangThai]) VALUES (N''KHDEMO08'',N''Đỗ Gia Huy'',N''0909110008'',N''khach.demo8@example.com'',N''47 Nguyễn Trãi, TP. Hồ Chí Minh'',1);
INSERT dbo.[KhachHang] ([maKH],[hoTen],[sdt],[email],[diaChi],[trangThai]) VALUES (N''KHDEMO09'',N''Hồ Bảo Ngọc'',N''0909110009'',N''khach.demo9@example.com'',N''52 Nguyễn Trãi, TP. Hồ Chí Minh'',1);
INSERT dbo.[KhachHang] ([maKH],[hoTen],[sdt],[email],[diaChi],[trangThai]) VALUES (N''KHDEMO10'',N''Ngô Tuấn Kiệt'',N''0909110010'',N''khach.demo10@example.com'',N''57 Nguyễn Trãi, TP. Hồ Chí Minh'',1);
INSERT dbo.[KhachHang] ([maKH],[hoTen],[sdt],[email],[diaChi],[trangThai]) VALUES (N''KHDEMO11'',N''Dương Khánh An'',N''0909110011'',N''khach.demo11@example.com'',N''62 Nguyễn Trãi, TP. Hồ Chí Minh'',1);
INSERT dbo.[KhachHang] ([maKH],[hoTen],[sdt],[email],[diaChi],[trangThai]) VALUES (N''KHDEMO12'',N''Lý Hải Yến'',N''0909110012'',N''khach.demo12@example.com'',N''67 Nguyễn Trãi, TP. Hồ Chí Minh'',1);

-- KhuyenMai
INSERT dbo.[KhuyenMai] ([maKM],[tenKM],[phanTramGiam],[ngayBatDau],[ngayKetThuc],[trangThai]) VALUES (N''KMDEMO05'',N''Ưu đãi thành viên 5% (demo)'',5.00,''2026-01-01'',''2027-12-31'',1);
INSERT dbo.[KhuyenMai] ([maKM],[tenKM],[phanTramGiam],[ngayBatDau],[ngayKetThuc],[trangThai]) VALUES (N''KMDEMO10'',N''Bộ sưu tập mới giảm 10% (demo)'',10.00,''2026-01-01'',''2027-12-31'',1);
INSERT dbo.[KhuyenMai] ([maKM],[tenKM],[phanTramGiam],[ngayBatDau],[ngayKetThuc],[trangThai]) VALUES (N''KMDEMOHET'',N''Ưu đãi mùa hè đã kết thúc (demo)'',15.00,''2026-06-01'',''2026-08-31'',0);

-- Thue
INSERT dbo.[Thue] ([maThue],[tenThue],[phanTram],[moTa],[trangThai]) VALUES (N''T_DEMO_0'',N''Thuế demo học tập 0%'',0.00,N''Cấu hình kiểm thử đồ án; không dùng để kết luận thuế bán hàng thực tế.'',1);

-- HoaDon
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO001'',N''NVDEMO02'',N''KHDEMO01'',N''KMDEMO05'',N''T_DEMO_0'',''2026-11-02T09:00:00'',298000.00,5.00,14900.00,0.00,0.00,283100.00,N''DA_THANH_TOAN'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',5,''2026-11-02T09:00:00'',N''Trần Thu Hà'',N''Nguyễn Ngọc Mai'',N''TIEN_MAT'',300000.00,16900.00,NULL);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO002'',N''NVDEMO03'',N''KHDEMO02'',N''KMDEMO10'',N''T_DEMO_0'',''2026-11-02T11:00:00'',298000.00,10.00,29800.00,0.00,0.00,268200.00,N''DA_THANH_TOAN'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',5,''2026-11-02T11:00:00'',N''Lê Hoàng Nam'',N''Trần Quốc Bảo'',N''CHUYEN_KHOAN'',NULL,NULL,1);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO003'',N''NVDEMO02'',N''KHDEMO03'',NULL,N''T_DEMO_0'',''2026-11-02T13:00:00'',298000.00,0.00,0.00,0.00,0.00,298000.00,N''DA_THANH_TOAN'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',5,''2026-11-02T13:00:00'',N''Trần Thu Hà'',N''Lê Phương Linh'',N''TIEN_MAT'',300000.00,2000.00,NULL);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO004'',N''NVDEMO03'',N''KHDEMO04'',N''KMDEMO05'',N''T_DEMO_0'',''2026-11-03T09:00:00'',298000.00,5.00,14900.00,0.00,0.00,283100.00,N''DA_THANH_TOAN'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',5,''2026-11-03T09:00:00'',N''Lê Hoàng Nam'',N''Phạm Hoàng Long'',N''CHUYEN_KHOAN'',NULL,NULL,1);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO005'',N''NVDEMO02'',N''KHDEMO05'',N''KMDEMO10'',N''T_DEMO_0'',''2026-11-03T11:00:00'',298000.00,10.00,29800.00,0.00,0.00,268200.00,N''DA_THANH_TOAN'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',5,''2026-11-03T11:00:00'',N''Trần Thu Hà'',N''Võ Thảo Vy'',N''TIEN_MAT'',300000.00,31800.00,NULL);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO006'',N''NVDEMO03'',N''KHDEMO06'',NULL,N''T_DEMO_0'',''2026-11-03T13:00:00'',298000.00,0.00,0.00,0.00,0.00,298000.00,N''DA_THANH_TOAN'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',5,''2026-11-03T13:00:00'',N''Lê Hoàng Nam'',N''Đặng Minh Khang'',N''CHUYEN_KHOAN'',NULL,NULL,1);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO007'',N''NVDEMO02'',N''KHDEMO07'',N''KMDEMO05'',N''T_DEMO_0'',''2026-11-04T09:00:00'',298000.00,5.00,14900.00,0.00,0.00,283100.00,N''DA_THANH_TOAN'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',5,''2026-11-04T09:00:00'',N''Trần Thu Hà'',N''Bùi Thanh Trúc'',N''TIEN_MAT'',300000.00,16900.00,NULL);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO008'',N''NVDEMO03'',N''KHDEMO08'',N''KMDEMO10'',N''T_DEMO_0'',''2026-11-04T11:00:00'',298000.00,10.00,29800.00,0.00,0.00,268200.00,N''DA_THANH_TOAN'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',5,''2026-11-04T11:00:00'',N''Lê Hoàng Nam'',N''Đỗ Gia Huy'',N''CHUYEN_KHOAN'',NULL,NULL,1);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO009'',N''NVDEMO02'',N''KHDEMO09'',NULL,N''T_DEMO_0'',''2026-11-04T13:00:00'',298000.00,0.00,0.00,0.00,0.00,298000.00,N''DA_THANH_TOAN'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',5,''2026-11-04T13:00:00'',N''Trần Thu Hà'',N''Hồ Bảo Ngọc'',N''TIEN_MAT'',300000.00,2000.00,NULL);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO010'',N''NVDEMO03'',N''KHDEMO10'',N''KMDEMO05'',N''T_DEMO_0'',''2026-11-05T09:00:00'',298000.00,5.00,14900.00,0.00,0.00,283100.00,N''DA_THANH_TOAN'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',5,''2026-11-05T09:00:00'',N''Lê Hoàng Nam'',N''Ngô Tuấn Kiệt'',N''CHUYEN_KHOAN'',NULL,NULL,1);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO011'',N''NVDEMO02'',N''KHDEMO11'',N''KMDEMO10'',N''T_DEMO_0'',''2026-11-05T11:00:00'',298000.00,10.00,29800.00,0.00,0.00,268200.00,N''DA_THANH_TOAN'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',5,''2026-11-05T11:00:00'',N''Trần Thu Hà'',N''Dương Khánh An'',N''TIEN_MAT'',300000.00,31800.00,NULL);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO012'',N''NVDEMO03'',N''KHDEMO12'',NULL,N''T_DEMO_0'',''2026-11-05T13:00:00'',298000.00,0.00,0.00,0.00,0.00,298000.00,N''DA_THANH_TOAN'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',5,''2026-11-05T13:00:00'',N''Lê Hoàng Nam'',N''Lý Hải Yến'',N''CHUYEN_KHOAN'',NULL,NULL,1);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO013'',N''NVDEMO02'',N''KHDEMO01'',N''KMDEMO05'',N''T_DEMO_0'',''2026-11-05T09:00:00'',298000.00,5.00,14900.00,0.00,0.00,283100.00,N''NHAP'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',4,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO014'',N''NVDEMO03'',N''KHDEMO02'',N''KMDEMO10'',N''T_DEMO_0'',''2026-11-05T11:00:00'',298000.00,10.00,29800.00,0.00,0.00,268200.00,N''NHAP'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',4,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT dbo.[HoaDon] ([maHD],[maNV],[maKH],[maKM],[maThue],[ngayLap],[tamTinh],[phanTramGiam],[tienGiam],[phanTramThue],[tienThue],[tongTien],[trangThai],[ghiChu],[phienBan],[ngayThanhToan],[tenNhanVienSnapshot],[tenKhachHangSnapshot],[phuongThucThanhToan],[tienKhachDua],[tienThua],[nhanVienXacNhanChuyenKhoan]) VALUES (N''HDDEMO015'',N''NVDEMO02'',N''KHDEMO03'',NULL,N''T_DEMO_0'',''2026-11-05T13:00:00'',298000.00,0.00,0.00,0.00,0.00,298000.00,N''DA_HUY'',N''Dữ liệu demo bổ sung ngày 05/11/2026'',5,NULL,NULL,NULL,NULL,NULL,NULL,NULL);

-- ChiTietHoaDon
SET IDENTITY_INSERT dbo.[ChiTietHoaDon] ON;
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (1,N''HDDEMO001'',N''BT000001'',1,149000.00,N''Áo phông nữ basic cổ tim'',N''Đỏ'',N''S'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (2,N''HDDEMO001'',N''BT000007'',1,149000.00,N''Áo phông nữ Cotton USA basic dáng suông'',N''Nâu'',N''S'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (3,N''HDDEMO002'',N''BT000002'',1,149000.00,N''Áo phông nữ basic cổ tim'',N''Đỏ'',N''M'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (4,N''HDDEMO002'',N''BT000008'',1,149000.00,N''Áo phông nữ Cotton USA basic dáng suông'',N''Nâu'',N''M'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (5,N''HDDEMO003'',N''BT000003'',1,149000.00,N''Áo phông nữ basic cổ tim'',N''Vàng'',N''S'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (6,N''HDDEMO003'',N''BT000009'',1,149000.00,N''Áo phông nữ Cotton USA basic dáng suông'',N''Đen'',N''S'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (7,N''HDDEMO004'',N''BT000004'',1,149000.00,N''Áo phông nữ basic cổ tim'',N''Vàng'',N''M'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (8,N''HDDEMO004'',N''BT000010'',1,149000.00,N''Áo phông nữ Cotton USA basic dáng suông'',N''Đen'',N''M'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (9,N''HDDEMO005'',N''BT000005'',1,149000.00,N''Áo phông nữ basic cổ tim'',N''Nâu'',N''S'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (10,N''HDDEMO005'',N''BT000011'',1,149000.00,N''Áo phông nữ Cotton USA basic dáng suông'',N''Hồng'',N''S'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (11,N''HDDEMO006'',N''BT000006'',1,149000.00,N''Áo phông nữ basic cổ tim'',N''Nâu'',N''M'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (12,N''HDDEMO006'',N''BT000012'',1,149000.00,N''Áo phông nữ Cotton USA basic dáng suông'',N''Hồng'',N''M'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (13,N''HDDEMO007'',N''BT000007'',1,149000.00,N''Áo phông nữ Cotton USA basic dáng suông'',N''Nâu'',N''S'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (14,N''HDDEMO007'',N''BT000001'',1,149000.00,N''Áo phông nữ basic cổ tim'',N''Đỏ'',N''S'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (15,N''HDDEMO008'',N''BT000008'',1,149000.00,N''Áo phông nữ Cotton USA basic dáng suông'',N''Nâu'',N''M'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (16,N''HDDEMO008'',N''BT000002'',1,149000.00,N''Áo phông nữ basic cổ tim'',N''Đỏ'',N''M'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (17,N''HDDEMO009'',N''BT000009'',1,149000.00,N''Áo phông nữ Cotton USA basic dáng suông'',N''Đen'',N''S'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (18,N''HDDEMO009'',N''BT000003'',1,149000.00,N''Áo phông nữ basic cổ tim'',N''Vàng'',N''S'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (19,N''HDDEMO010'',N''BT000010'',1,149000.00,N''Áo phông nữ Cotton USA basic dáng suông'',N''Đen'',N''M'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (20,N''HDDEMO010'',N''BT000004'',1,149000.00,N''Áo phông nữ basic cổ tim'',N''Vàng'',N''M'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (21,N''HDDEMO011'',N''BT000011'',1,149000.00,N''Áo phông nữ Cotton USA basic dáng suông'',N''Hồng'',N''S'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (22,N''HDDEMO011'',N''BT000005'',1,149000.00,N''Áo phông nữ basic cổ tim'',N''Nâu'',N''S'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (23,N''HDDEMO012'',N''BT000012'',1,149000.00,N''Áo phông nữ Cotton USA basic dáng suông'',N''Hồng'',N''M'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (24,N''HDDEMO012'',N''BT000006'',1,149000.00,N''Áo phông nữ basic cổ tim'',N''Nâu'',N''M'');
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (25,N''HDDEMO013'',N''BT000001'',1,149000.00,NULL,NULL,NULL);
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (26,N''HDDEMO013'',N''BT000007'',1,149000.00,NULL,NULL,NULL);
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (27,N''HDDEMO014'',N''BT000002'',1,149000.00,NULL,NULL,NULL);
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (28,N''HDDEMO014'',N''BT000008'',1,149000.00,NULL,NULL,NULL);
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (29,N''HDDEMO015'',N''BT000003'',1,149000.00,NULL,NULL,NULL);
INSERT dbo.[ChiTietHoaDon] ([maCTHD],[maHD],[maBienThe],[soLuong],[donGia],[tenSPSnapshot],[mauSacSnapshot],[kichCoSnapshot]) VALUES (30,N''HDDEMO015'',N''BT000009'',1,149000.00,NULL,NULL,NULL);
SET IDENTITY_INSERT dbo.[ChiTietHoaDon] OFF;

-- LichSuKho
SET IDENTITY_INSERT dbo.[LichSuKho] ON;
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (1,N''BT000001'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000001'',N''S15:BASELINE:BT000001'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (2,N''BT000002'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000002'',N''S15:BASELINE:BT000002'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (3,N''BT000003'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000003'',N''S15:BASELINE:BT000003'',0,20,20,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (4,N''BT000004'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000004'',N''S15:BASELINE:BT000004'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (5,N''BT000005'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000005'',N''S15:BASELINE:BT000005'',0,5,5,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (6,N''BT000006'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000006'',N''S15:BASELINE:BT000006'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (7,N''BT000007'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000007'',N''S15:BASELINE:BT000007'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (8,N''BT000008'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000008'',N''S15:BASELINE:BT000008'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (9,N''BT000009'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000009'',N''S15:BASELINE:BT000009'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (10,N''BT000010'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000010'',N''S15:BASELINE:BT000010'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (11,N''BT000011'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000011'',N''S15:BASELINE:BT000011'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (12,N''BT000012'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000012'',N''S15:BASELINE:BT000012'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (13,N''BT000013'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000013'',N''S15:BASELINE:BT000013'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (14,N''BT000014'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000014'',N''S15:BASELINE:BT000014'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (15,N''BT000015'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000015'',N''S15:BASELINE:BT000015'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (16,N''BT000016'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000016'',N''S15:BASELINE:BT000016'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (17,N''BT000017'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000017'',N''S15:BASELINE:BT000017'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (18,N''BT000018'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000018'',N''S15:BASELINE:BT000018'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (19,N''BT000019'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000019'',N''S15:BASELINE:BT000019'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (20,N''BT000020'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000020'',N''S15:BASELINE:BT000020'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (21,N''BT000021'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000021'',N''S15:BASELINE:BT000021'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (22,N''BT000022'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000022'',N''S15:BASELINE:BT000022'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (23,N''BT000023'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000023'',N''S15:BASELINE:BT000023'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (24,N''BT000024'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000024'',N''S15:BASELINE:BT000024'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (25,N''BT000025'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000025'',N''S15:BASELINE:BT000025'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (26,N''BT000026'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000026'',N''S15:BASELINE:BT000026'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (27,N''BT000027'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000027'',N''S15:BASELINE:BT000027'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (28,N''BT000028'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000028'',N''S15:BASELINE:BT000028'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (29,N''BT000029'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000029'',N''S15:BASELINE:BT000029'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (30,N''BT000030'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000030'',N''S15:BASELINE:BT000030'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (31,N''BT000031'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000031'',N''S15:BASELINE:BT000031'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (32,N''BT000032'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000032'',N''S15:BASELINE:BT000032'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (33,N''BT000033'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000033'',N''S15:BASELINE:BT000033'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (34,N''BT000034'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000034'',N''S15:BASELINE:BT000034'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (35,N''BT000035'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000035'',N''S15:BASELINE:BT000035'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (36,N''BT000036'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000036'',N''S15:BASELINE:BT000036'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (37,N''BT000037'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000037'',N''S15:BASELINE:BT000037'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (38,N''BT000038'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000038'',N''S15:BASELINE:BT000038'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (39,N''BT000039'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000039'',N''S15:BASELINE:BT000039'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (40,N''BT000040'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000040'',N''S15:BASELINE:BT000040'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (41,N''BT000041'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000041'',N''S15:BASELINE:BT000041'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (42,N''BT000042'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000042'',N''S15:BASELINE:BT000042'',0,5,5,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (43,N''BT000043'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000043'',N''S15:BASELINE:BT000043'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (44,N''BT000044'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000044'',N''S15:BASELINE:BT000044'',0,20,20,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (45,N''BT000045'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000045'',N''S15:BASELINE:BT000045'',0,30,30,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (46,N''BT000046'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000046'',N''S15:BASELINE:BT000046'',0,40,40,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (47,N''BT000047'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000047'',N''S15:BASELINE:BT000047'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (48,N''BT000048'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000048'',N''S15:BASELINE:BT000048'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (49,N''BT000049'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000049'',N''S15:BASELINE:BT000049'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (50,N''BT000050'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000050'',N''S15:BASELINE:BT000050'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (51,N''BT000051'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000051'',N''S15:BASELINE:BT000051'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (52,N''BT000052'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000052'',N''S15:BASELINE:BT000052'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (53,N''BT000053'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000053'',N''S15:BASELINE:BT000053'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (54,N''BT000054'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000054'',N''S15:BASELINE:BT000054'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (55,N''BT000055'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000055'',N''S15:BASELINE:BT000055'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (56,N''BT000056'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000056'',N''S15:BASELINE:BT000056'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (57,N''BT000057'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000057'',N''S15:BASELINE:BT000057'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (58,N''BT000058'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000058'',N''S15:BASELINE:BT000058'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (59,N''BT000059'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000059'',N''S15:BASELINE:BT000059'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (60,N''BT000060'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000060'',N''S15:BASELINE:BT000060'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (61,N''BT000061'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000061'',N''S15:BASELINE:BT000061'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (62,N''BT000062'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000062'',N''S15:BASELINE:BT000062'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (63,N''BT000063'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000063'',N''S15:BASELINE:BT000063'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (64,N''BT000064'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000064'',N''S15:BASELINE:BT000064'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (65,N''BT000065'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000065'',N''S15:BASELINE:BT000065'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (66,N''BT000066'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000066'',N''S15:BASELINE:BT000066'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (67,N''BT000067'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000067'',N''S15:BASELINE:BT000067'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (68,N''BT000068'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000068'',N''S15:BASELINE:BT000068'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (69,N''BT000069'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000069'',N''S15:BASELINE:BT000069'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (70,N''BT000070'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000070'',N''S15:BASELINE:BT000070'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (71,N''BT000071'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000071'',N''S15:BASELINE:BT000071'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (72,N''BT000072'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000072'',N''S15:BASELINE:BT000072'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (73,N''BT000073'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000073'',N''S15:BASELINE:BT000073'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (74,N''BT000074'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000074'',N''S15:BASELINE:BT000074'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (75,N''BT000075'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000075'',N''S15:BASELINE:BT000075'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (76,N''BT000076'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000076'',N''S15:BASELINE:BT000076'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (77,N''BT000077'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000077'',N''S15:BASELINE:BT000077'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (78,N''BT000078'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000078'',N''S15:BASELINE:BT000078'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (79,N''BT000079'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000079'',N''S15:BASELINE:BT000079'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (80,N''BT000080'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000080'',N''S15:BASELINE:BT000080'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (81,N''BT000081'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000081'',N''S15:BASELINE:BT000081'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (82,N''BT000082'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000082'',N''S15:BASELINE:BT000082'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (83,N''BT000083'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000083'',N''S15:BASELINE:BT000083'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (84,N''BT000084'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000084'',N''S15:BASELINE:BT000084'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (85,N''BT000085'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000085'',N''S15:BASELINE:BT000085'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (86,N''BT000086'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000086'',N''S15:BASELINE:BT000086'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (87,N''BT000087'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000087'',N''S15:BASELINE:BT000087'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (88,N''BT000088'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000088'',N''S15:BASELINE:BT000088'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (89,N''BT000089'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000089'',N''S15:BASELINE:BT000089'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (90,N''BT000090'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000090'',N''S15:BASELINE:BT000090'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (91,N''BT000091'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000091'',N''S15:BASELINE:BT000091'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (92,N''BT000092'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000092'',N''S15:BASELINE:BT000092'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (93,N''BT000093'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000093'',N''S15:BASELINE:BT000093'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (94,N''BT000094'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000094'',N''S15:BASELINE:BT000094'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (95,N''BT000095'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000095'',N''S15:BASELINE:BT000095'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (96,N''BT000096'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000096'',N''S15:BASELINE:BT000096'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (97,N''BT000097'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000097'',N''S15:BASELINE:BT000097'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (98,N''BT000098'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000098'',N''S15:BASELINE:BT000098'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (99,N''BT000099'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000099'',N''S15:BASELINE:BT000099'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (100,N''BT000100'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000100'',N''S15:BASELINE:BT000100'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (101,N''BT000101'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000101'',N''S15:BASELINE:BT000101'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (102,N''BT000102'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000102'',N''S15:BASELINE:BT000102'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (103,N''BT000103'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000103'',N''S15:BASELINE:BT000103'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (104,N''BT000104'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000104'',N''S15:BASELINE:BT000104'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (105,N''BT000105'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000105'',N''S15:BASELINE:BT000105'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (106,N''BT000106'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000106'',N''S15:BASELINE:BT000106'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (107,N''BT000107'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000107'',N''S15:BASELINE:BT000107'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (108,N''BT000108'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000108'',N''S15:BASELINE:BT000108'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (109,N''BT000109'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000109'',N''S15:BASELINE:BT000109'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (110,N''BT000110'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000110'',N''S15:BASELINE:BT000110'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (111,N''BT000111'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000111'',N''S15:BASELINE:BT000111'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (112,N''BT000112'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000112'',N''S15:BASELINE:BT000112'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (113,N''BT000113'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000113'',N''S15:BASELINE:BT000113'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (114,N''BT000114'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000114'',N''S15:BASELINE:BT000114'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (115,N''BT000115'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000115'',N''S15:BASELINE:BT000115'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (116,N''BT000116'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000116'',N''S15:BASELINE:BT000116'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (117,N''BT000117'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000117'',N''S15:BASELINE:BT000117'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (118,N''BT000118'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000118'',N''S15:BASELINE:BT000118'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (119,N''BT000119'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000119'',N''S15:BASELINE:BT000119'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (120,N''BT000120'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000120'',N''S15:BASELINE:BT000120'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (121,N''BT000121'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000121'',N''S15:BASELINE:BT000121'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (122,N''BT000122'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000122'',N''S15:BASELINE:BT000122'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (123,N''BT000123'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000123'',N''S15:BASELINE:BT000123'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (124,N''BT000124'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000124'',N''S15:BASELINE:BT000124'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (125,N''BT000125'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000125'',N''S15:BASELINE:BT000125'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (126,N''BT000126'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000126'',N''S15:BASELINE:BT000126'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (127,N''BT000127'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000127'',N''S15:BASELINE:BT000127'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (128,N''BT000128'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000128'',N''S15:BASELINE:BT000128'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (129,N''BT000129'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000129'',N''S15:BASELINE:BT000129'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (130,N''BT000130'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000130'',N''S15:BASELINE:BT000130'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (131,N''BT000131'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000131'',N''S15:BASELINE:BT000131'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (132,N''BT000132'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000132'',N''S15:BASELINE:BT000132'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (133,N''BT000133'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000133'',N''S15:BASELINE:BT000133'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (134,N''BT000134'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000134'',N''S15:BASELINE:BT000134'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (135,N''BT000135'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000135'',N''S15:BASELINE:BT000135'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (136,N''BT000136'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000136'',N''S15:BASELINE:BT000136'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (137,N''BT000137'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000137'',N''S15:BASELINE:BT000137'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (138,N''BT000138'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000138'',N''S15:BASELINE:BT000138'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (139,N''BT000139'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000139'',N''S15:BASELINE:BT000139'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (140,N''BT000140'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000140'',N''S15:BASELINE:BT000140'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (141,N''BT000141'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000141'',N''S15:BASELINE:BT000141'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (142,N''BT000142'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000142'',N''S15:BASELINE:BT000142'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (143,N''BT000143'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000143'',N''S15:BASELINE:BT000143'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (144,N''BT000144'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000144'',N''S15:BASELINE:BT000144'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (145,N''BT000145'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000145'',N''S15:BASELINE:BT000145'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (146,N''BT000146'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000146'',N''S15:BASELINE:BT000146'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (147,N''BT000147'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000147'',N''S15:BASELINE:BT000147'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (148,N''BT000148'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000148'',N''S15:BASELINE:BT000148'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (149,N''BT000149'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000149'',N''S15:BASELINE:BT000149'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (150,N''BT000150'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000150'',N''S15:BASELINE:BT000150'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (151,N''BT000151'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000151'',N''S15:BASELINE:BT000151'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (152,N''BT000152'',NULL,''2026-10-13T01:22:02'',N''TON_MO_DAU'',N''BASELINE:BT000152'',N''S15:BASELINE:BT000152'',0,10,10,0,NULL,N''Mốc tồn hiện có tại thời điểm triển khai S15; không phải lịch sử giao dịch.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (153,N''BT000001'',N''NVDEMO01'',''2026-11-01T08:00:00'',N''NHAP_HANG'',N''PNDEMO001'',N''PN:PNDEMO001:BT000001'',10,2,12,0,N''Nguyễn Minh Anh'',N''Nhập hàng theo phiếu PNDEMO001'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (154,N''BT000002'',N''NVDEMO01'',''2026-11-01T08:00:00'',N''NHAP_HANG'',N''PNDEMO001'',N''PN:PNDEMO001:BT000002'',10,2,12,0,N''Nguyễn Minh Anh'',N''Nhập hàng theo phiếu PNDEMO001'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (155,N''BT000003'',N''NVDEMO01'',''2026-11-01T08:00:00'',N''NHAP_HANG'',N''PNDEMO001'',N''PN:PNDEMO001:BT000003'',20,2,22,0,N''Nguyễn Minh Anh'',N''Nhập hàng theo phiếu PNDEMO001'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (156,N''BT000004'',N''NVDEMO01'',''2026-11-01T08:00:00'',N''NHAP_HANG'',N''PNDEMO001'',N''PN:PNDEMO001:BT000004'',10,2,12,0,N''Nguyễn Minh Anh'',N''Nhập hàng theo phiếu PNDEMO001'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (157,N''BT000005'',N''NVDEMO01'',''2026-11-01T09:00:00'',N''NHAP_HANG'',N''PNDEMO002'',N''PN:PNDEMO002:BT000005'',5,2,7,0,N''Nguyễn Minh Anh'',N''Nhập hàng theo phiếu PNDEMO002'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (158,N''BT000006'',N''NVDEMO01'',''2026-11-01T09:00:00'',N''NHAP_HANG'',N''PNDEMO002'',N''PN:PNDEMO002:BT000006'',10,2,12,0,N''Nguyễn Minh Anh'',N''Nhập hàng theo phiếu PNDEMO002'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (159,N''BT000007'',N''NVDEMO01'',''2026-11-01T09:00:00'',N''NHAP_HANG'',N''PNDEMO002'',N''PN:PNDEMO002:BT000007'',10,2,12,0,N''Nguyễn Minh Anh'',N''Nhập hàng theo phiếu PNDEMO002'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (160,N''BT000008'',N''NVDEMO01'',''2026-11-01T09:00:00'',N''NHAP_HANG'',N''PNDEMO002'',N''PN:PNDEMO002:BT000008'',10,2,12,0,N''Nguyễn Minh Anh'',N''Nhập hàng theo phiếu PNDEMO002'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (161,N''BT000009'',N''NVDEMO01'',''2026-11-01T10:00:00'',N''NHAP_HANG'',N''PNDEMO003'',N''PN:PNDEMO003:BT000009'',10,2,12,0,N''Nguyễn Minh Anh'',N''Nhập hàng theo phiếu PNDEMO003'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (162,N''BT000010'',N''NVDEMO01'',''2026-11-01T10:00:00'',N''NHAP_HANG'',N''PNDEMO003'',N''PN:PNDEMO003:BT000010'',10,2,12,0,N''Nguyễn Minh Anh'',N''Nhập hàng theo phiếu PNDEMO003'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (163,N''BT000011'',N''NVDEMO01'',''2026-11-01T10:00:00'',N''NHAP_HANG'',N''PNDEMO003'',N''PN:PNDEMO003:BT000011'',10,2,12,0,N''Nguyễn Minh Anh'',N''Nhập hàng theo phiếu PNDEMO003'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (164,N''BT000012'',N''NVDEMO01'',''2026-11-01T10:00:00'',N''NHAP_HANG'',N''PNDEMO003'',N''PN:PNDEMO003:BT000012'',10,2,12,0,N''Nguyễn Minh Anh'',N''Nhập hàng theo phiếu PNDEMO003'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (165,N''BT000001'',N''NVDEMO02'',''2026-11-02T09:00:00'',N''BAN_HANG'',N''HDDEMO001'',N''SALE:HDDEMO001:BT000001'',12,-1,11,1,N''Trần Thu Hà'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (166,N''BT000007'',N''NVDEMO02'',''2026-11-02T09:00:00'',N''BAN_HANG'',N''HDDEMO001'',N''SALE:HDDEMO001:BT000007'',12,-1,11,1,N''Trần Thu Hà'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (167,N''BT000002'',N''NVDEMO03'',''2026-11-02T11:00:00'',N''BAN_HANG'',N''HDDEMO002'',N''SALE:HDDEMO002:BT000002'',12,-1,11,1,N''Lê Hoàng Nam'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (168,N''BT000008'',N''NVDEMO03'',''2026-11-02T11:00:00'',N''BAN_HANG'',N''HDDEMO002'',N''SALE:HDDEMO002:BT000008'',12,-1,11,1,N''Lê Hoàng Nam'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (169,N''BT000003'',N''NVDEMO02'',''2026-11-02T13:00:00'',N''BAN_HANG'',N''HDDEMO003'',N''SALE:HDDEMO003:BT000003'',22,-1,21,1,N''Trần Thu Hà'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (170,N''BT000009'',N''NVDEMO02'',''2026-11-02T13:00:00'',N''BAN_HANG'',N''HDDEMO003'',N''SALE:HDDEMO003:BT000009'',12,-1,11,1,N''Trần Thu Hà'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (171,N''BT000004'',N''NVDEMO03'',''2026-11-03T09:00:00'',N''BAN_HANG'',N''HDDEMO004'',N''SALE:HDDEMO004:BT000004'',12,-1,11,1,N''Lê Hoàng Nam'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (172,N''BT000010'',N''NVDEMO03'',''2026-11-03T09:00:00'',N''BAN_HANG'',N''HDDEMO004'',N''SALE:HDDEMO004:BT000010'',12,-1,11,1,N''Lê Hoàng Nam'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (173,N''BT000005'',N''NVDEMO02'',''2026-11-03T11:00:00'',N''BAN_HANG'',N''HDDEMO005'',N''SALE:HDDEMO005:BT000005'',7,-1,6,1,N''Trần Thu Hà'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (174,N''BT000011'',N''NVDEMO02'',''2026-11-03T11:00:00'',N''BAN_HANG'',N''HDDEMO005'',N''SALE:HDDEMO005:BT000011'',12,-1,11,1,N''Trần Thu Hà'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (175,N''BT000006'',N''NVDEMO03'',''2026-11-03T13:00:00'',N''BAN_HANG'',N''HDDEMO006'',N''SALE:HDDEMO006:BT000006'',12,-1,11,1,N''Lê Hoàng Nam'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (176,N''BT000012'',N''NVDEMO03'',''2026-11-03T13:00:00'',N''BAN_HANG'',N''HDDEMO006'',N''SALE:HDDEMO006:BT000012'',12,-1,11,1,N''Lê Hoàng Nam'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (177,N''BT000001'',N''NVDEMO02'',''2026-11-04T09:00:00'',N''BAN_HANG'',N''HDDEMO007'',N''SALE:HDDEMO007:BT000001'',11,-1,10,2,N''Trần Thu Hà'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (178,N''BT000007'',N''NVDEMO02'',''2026-11-04T09:00:00'',N''BAN_HANG'',N''HDDEMO007'',N''SALE:HDDEMO007:BT000007'',11,-1,10,2,N''Trần Thu Hà'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (179,N''BT000002'',N''NVDEMO03'',''2026-11-04T11:00:00'',N''BAN_HANG'',N''HDDEMO008'',N''SALE:HDDEMO008:BT000002'',11,-1,10,2,N''Lê Hoàng Nam'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (180,N''BT000008'',N''NVDEMO03'',''2026-11-04T11:00:00'',N''BAN_HANG'',N''HDDEMO008'',N''SALE:HDDEMO008:BT000008'',11,-1,10,2,N''Lê Hoàng Nam'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (181,N''BT000003'',N''NVDEMO02'',''2026-11-04T13:00:00'',N''BAN_HANG'',N''HDDEMO009'',N''SALE:HDDEMO009:BT000003'',21,-1,20,2,N''Trần Thu Hà'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (182,N''BT000009'',N''NVDEMO02'',''2026-11-04T13:00:00'',N''BAN_HANG'',N''HDDEMO009'',N''SALE:HDDEMO009:BT000009'',11,-1,10,2,N''Trần Thu Hà'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (183,N''BT000004'',N''NVDEMO03'',''2026-11-05T09:00:00'',N''BAN_HANG'',N''HDDEMO010'',N''SALE:HDDEMO010:BT000004'',11,-1,10,2,N''Lê Hoàng Nam'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (184,N''BT000010'',N''NVDEMO03'',''2026-11-05T09:00:00'',N''BAN_HANG'',N''HDDEMO010'',N''SALE:HDDEMO010:BT000010'',11,-1,10,2,N''Lê Hoàng Nam'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (185,N''BT000005'',N''NVDEMO02'',''2026-11-05T11:00:00'',N''BAN_HANG'',N''HDDEMO011'',N''SALE:HDDEMO011:BT000005'',6,-1,5,2,N''Trần Thu Hà'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (186,N''BT000011'',N''NVDEMO02'',''2026-11-05T11:00:00'',N''BAN_HANG'',N''HDDEMO011'',N''SALE:HDDEMO011:BT000011'',11,-1,10,2,N''Trần Thu Hà'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (187,N''BT000006'',N''NVDEMO03'',''2026-11-05T13:00:00'',N''BAN_HANG'',N''HDDEMO012'',N''SALE:HDDEMO012:BT000006'',11,-1,10,2,N''Lê Hoàng Nam'',N''Trừ tồn khi thanh toán hóa đơn.'');
INSERT dbo.[LichSuKho] ([maLichSuKho],[maBienThe],[maNV],[thoiGian],[nguon],[thamChieu],[operationId],[soLuongTruoc],[soLuongThayDoi],[soLuongSau],[phienBanTruoc],[tenNguoiThaoTacSnapshot],[lyDo]) VALUES (188,N''BT000012'',N''NVDEMO03'',''2026-11-05T13:00:00'',N''BAN_HANG'',N''HDDEMO012'',N''SALE:HDDEMO012:BT000012'',11,-1,10,2,N''Lê Hoàng Nam'',N''Trừ tồn khi thanh toán hóa đơn.'');
SET IDENTITY_INSERT dbo.[LichSuKho] OFF;

-- NhaCungCap
INSERT dbo.[NhaCungCap] ([maNCC],[tenNCC],[sdt],[email],[diaChi],[trangThai],[phienBan]) VALUES (N''NCCDEMO01'',N''Dệt may An Phú (dữ liệu demo)'',N''0909220001'',N''ncc.demo1@example.com'',N''TP. Hồ Chí Minh'',1,0);
INSERT dbo.[NhaCungCap] ([maNCC],[tenNCC],[sdt],[email],[diaChi],[trangThai],[phienBan]) VALUES (N''NCCDEMO02'',N''Thời trang Bình Minh (dữ liệu demo)'',N''0909220002'',N''ncc.demo2@example.com'',N''TP. Hồ Chí Minh'',1,0);
INSERT dbo.[NhaCungCap] ([maNCC],[tenNCC],[sdt],[email],[diaChi],[trangThai],[phienBan]) VALUES (N''NCCDEMO03'',N''May mặc Sài Gòn (dữ liệu demo)'',N''0909220003'',N''ncc.demo3@example.com'',N''TP. Hồ Chí Minh'',1,0);

-- PhieuNhap
INSERT dbo.[PhieuNhap] ([maPhieuNhap],[maNCC],[maNVTao],[maNVXacNhan],[ngayLap],[ngayXacNhan],[trangThai],[tongTien],[operationIdTao],[operationIdXacNhan],[phienBanTruocXacNhan],[phienBan]) VALUES (N''PNDEMO001'',N''NCCDEMO01'',N''NVDEMO01'',N''NVDEMO01'',''2026-11-01T08:00:00'',''2026-11-01T08:00:00'',N''DA_XAC_NHAN'',715200.00,N''9651f9dc-7052-4139-98b8-b7d65771d1a6'',N''2d36a119-adc5-4dab-8aa8-166caf81c5a0'',8,9);
INSERT dbo.[PhieuNhap] ([maPhieuNhap],[maNCC],[maNVTao],[maNVXacNhan],[ngayLap],[ngayXacNhan],[trangThai],[tongTien],[operationIdTao],[operationIdXacNhan],[phienBanTruocXacNhan],[phienBan]) VALUES (N''PNDEMO002'',N''NCCDEMO02'',N''NVDEMO01'',N''NVDEMO01'',''2026-11-01T09:00:00'',''2026-11-01T09:00:00'',N''DA_XAC_NHAN'',715200.00,N''9736281a-038a-4337-9646-90697f30aa50'',N''6ec7d119-7652-4020-a0ca-3966f25b9727'',8,9);
INSERT dbo.[PhieuNhap] ([maPhieuNhap],[maNCC],[maNVTao],[maNVXacNhan],[ngayLap],[ngayXacNhan],[trangThai],[tongTien],[operationIdTao],[operationIdXacNhan],[phienBanTruocXacNhan],[phienBan]) VALUES (N''PNDEMO003'',N''NCCDEMO03'',N''NVDEMO01'',N''NVDEMO01'',''2026-11-01T10:00:00'',''2026-11-01T10:00:00'',N''DA_XAC_NHAN'',715200.00,N''53985e84-6ef8-4681-95e5-ef98fa405a96'',N''c86e21bf-1885-4afc-8508-a7aabec3b689'',8,9);
INSERT dbo.[PhieuNhap] ([maPhieuNhap],[maNCC],[maNVTao],[maNVXacNhan],[ngayLap],[ngayXacNhan],[trangThai],[tongTien],[operationIdTao],[operationIdXacNhan],[phienBanTruocXacNhan],[phienBan]) VALUES (N''PNDEMO004'',N''NCCDEMO01'',N''NVDEMO01'',NULL,''2026-11-01T11:00:00'',NULL,N''NHAP'',357600.00,N''4f5cf1e5-73de-4b90-8224-ad2caafc2d43'',NULL,NULL,4);
INSERT dbo.[PhieuNhap] ([maPhieuNhap],[maNCC],[maNVTao],[maNVXacNhan],[ngayLap],[ngayXacNhan],[trangThai],[tongTien],[operationIdTao],[operationIdXacNhan],[phienBanTruocXacNhan],[phienBan]) VALUES (N''PNDEMO005'',N''NCCDEMO02'',N''NVDEMO01'',NULL,''2026-11-01T12:00:00'',NULL,N''DA_HUY'',357600.00,N''c01b8cad-f7c3-44e9-b723-c462e97a7042'',NULL,NULL,6);

-- ChiTietPhieuNhap
SET IDENTITY_INSERT dbo.[ChiTietPhieuNhap] ON;
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (1,N''PNDEMO001'',N''BT000001'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (2,N''PNDEMO001'',N''BT000002'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (3,N''PNDEMO001'',N''BT000003'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (4,N''PNDEMO001'',N''BT000004'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (5,N''PNDEMO002'',N''BT000005'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (6,N''PNDEMO002'',N''BT000006'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (7,N''PNDEMO002'',N''BT000007'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (8,N''PNDEMO002'',N''BT000008'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (9,N''PNDEMO003'',N''BT000009'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (10,N''PNDEMO003'',N''BT000010'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (11,N''PNDEMO003'',N''BT000011'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (12,N''PNDEMO003'',N''BT000012'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (13,N''PNDEMO004'',N''BT000001'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (14,N''PNDEMO004'',N''BT000002'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (15,N''PNDEMO005'',N''BT000005'',2,89400.00);
INSERT dbo.[ChiTietPhieuNhap] ([maCTPN],[maPhieuNhap],[maBienThe],[soLuong],[donGiaNhap]) VALUES (16,N''PNDEMO005'',N''BT000006'',2,89400.00);
SET IDENTITY_INSERT dbo.[ChiTietPhieuNhap] OFF;

-- PhieuNhapThaoTac
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''1779cd5f-a087-4a09-9267-b2e1199d55ac'',N''PNDEMO002'',N''ADD_LINE'',N''ea57eca0e7840557ef671d41ad60e9f9d214bbf7d09521cb00030aea66c796af'',4,6,''2026-11-01T09:00:06'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''2d36a119-adc5-4dab-8aa8-166caf81c5a0'',N''PNDEMO001'',N''CONFIRM'',N''4ca8d43c9bfb56e7ce85835743a8ee0189984aca917a3ec1a8479d5028539759'',8,9,''2026-11-01T08:00:09'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''37a248bb-7435-4ee0-a389-97a9c1e85b7f'',N''PNDEMO003'',N''ADD_LINE'',N''d0aa7f61f1ce43c3c55d369273c80a0f457f84345e4059cec2d01f1d1b800935'',2,4,''2026-11-01T10:00:04'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''3be6bbc0-3d98-4dd3-ae03-53111355c99c'',N''PNDEMO003'',N''ADD_LINE'',N''9680ead2a7cc6bc56578c36c4ab458452f54093cbaab817680c5dde99689845d'',4,6,''2026-11-01T10:00:06'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''4bcb3275-053d-40df-b87c-62431179eeed'',N''PNDEMO001'',N''ADD_LINE'',N''d120083cd26c59828f5ee07564c2c9b14c6d2f679912c1a47faecd681ded0d0d'',2,4,''2026-11-01T08:00:04'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''4f5cf1e5-73de-4b90-8224-ad2caafc2d43'',N''PNDEMO004'',N''CREATE'',N''9975b7a252fd4b845a36999f7e5254d9c1e7860a10a90e64fce213ce9db3e723'',NULL,0,''2026-11-01T11:00:00'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''53985e84-6ef8-4681-95e5-ef98fa405a96'',N''PNDEMO003'',N''CREATE'',N''89b87719db09dac16e5522309c6497c766d53be1f72fcf95b5e9cef7273cbd34'',NULL,0,''2026-11-01T10:00:00'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''5e876a64-efcf-4377-97f7-49ac7d24b140'',N''PNDEMO004'',N''ADD_LINE'',N''a2a652f5b0fdeed19722b38a55d1b94870aa12e0bedeffba9435e8226a995fdc'',0,2,''2026-11-01T11:00:02'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''6681c264-0bef-4cf5-b43c-3641ce145d85'',N''PNDEMO004'',N''ADD_LINE'',N''d5ced236b57c657b47f3819d076e8ec489ef2cc8402810274ff376abbb8b235c'',2,4,''2026-11-01T11:00:04'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''6c529f0a-2c1f-43fb-80ea-c052d8788422'',N''PNDEMO002'',N''ADD_LINE'',N''ed1d6545c3a80103527a0393c769507bf11ef1399b9ee0bcd5eec0625182d8db'',2,4,''2026-11-01T09:00:04'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''6ec7d119-7652-4020-a0ca-3966f25b9727'',N''PNDEMO002'',N''CONFIRM'',N''5b1fa3f0f14bae2f453e355a84d0b883710b92e88067ff0650368a1e3e4e1c59'',8,9,''2026-11-01T09:00:09'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''7798fb89-dccb-4aba-9961-ce2455271496'',N''PNDEMO002'',N''ADD_LINE'',N''d0f0063cda6d652d02d420d66386a3c0cea7c507df58a07ab6b6649cf18c5c72'',6,8,''2026-11-01T09:00:08'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''780b164e-5366-4b41-81de-6439eadb6b5f'',N''PNDEMO005'',N''ADD_LINE'',N''1bf6a048bd14947ab6589c21c1477eefbdc54416d126dba554167a59dbe8e82d'',0,2,''2026-11-01T12:00:02'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''86092015-ec2d-446a-95b4-9fe65f8f124b'',N''PNDEMO002'',N''ADD_LINE'',N''952f483311493de11a712168c98135b13e72bee2e669ac304351e05657a02c8f'',0,2,''2026-11-01T09:00:02'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''8ebcfaa8-d496-43c3-8bfa-8cef0156dea8'',N''PNDEMO001'',N''ADD_LINE'',N''2aa9ae9d9ca827b0b50afb699085acb70d478027d280e3457f2812afcc076330'',0,2,''2026-11-01T08:00:02'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''9651f9dc-7052-4139-98b8-b7d65771d1a6'',N''PNDEMO001'',N''CREATE'',N''d7c033555f04984fd334b48c96fcc8645ab2ab204b2ac6b5673c0c461ff9060d'',NULL,0,''2026-11-01T08:00:00'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''9736281a-038a-4337-9646-90697f30aa50'',N''PNDEMO002'',N''CREATE'',N''372051d5d7b5b8474751a94cc33841e5017a79a7d80a6824181b83ebee59616e'',NULL,0,''2026-11-01T09:00:00'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''9750ac20-73ef-42b8-85b2-422c54f86bfe'',N''PNDEMO005'',N''ADD_LINE'',N''c6413241dff0a6afaa0f2f0d09c034b6988962d68000d2ddf72a048abcda53f3'',2,4,''2026-11-01T12:00:04'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''9f16d337-b0ec-4e59-9e0b-dada12d1e309'',N''PNDEMO001'',N''ADD_LINE'',N''af8c0d48bd5e275fb0ffc78ca2dc34231f11d7c5d6a66a287a9db6b09bdbed43'',4,6,''2026-11-01T08:00:06'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''c01b8cad-f7c3-44e9-b723-c462e97a7042'',N''PNDEMO005'',N''CREATE'',N''f3d3624ea3393930147ae6e37971bfefe9da523a9562deba7235c0ef61737704'',NULL,0,''2026-11-01T12:00:00'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''c86e21bf-1885-4afc-8508-a7aabec3b689'',N''PNDEMO003'',N''CONFIRM'',N''e53262884486afe8781c7c92c01f725f2fbc313fe7e898d02d9de958fd3cae18'',8,9,''2026-11-01T10:00:09'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''d4743725-940d-4d6d-baea-f19ac18539a5'',N''PNDEMO003'',N''ADD_LINE'',N''cbba7f4cde6070e8602f1559e57289c864455702091d6d29458f168b629124b5'',0,2,''2026-11-01T10:00:02'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''d789ca7f-7b1a-4240-a603-4b135ab3aa92'',N''PNDEMO001'',N''ADD_LINE'',N''eb44df24cfedecabaf0e41ecef49d4e1691becdfa91fdc9b1cbde01c39a3e755'',6,8,''2026-11-01T08:00:08'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''ebfc0ce9-79b1-4eb8-8e67-7f1f32ceba8d'',N''PNDEMO005'',N''CANCEL'',N''b93092740d73a960c19da622c880fa2032ca82734cdd7b4751c55c9bb030bf81'',4,6,''2026-11-01T12:00:06'');
INSERT dbo.[PhieuNhapThaoTac] ([operationId],[maPhieuNhap],[loaiThaoTac],[fingerprint],[phienBanTruoc],[phienBanSau],[thoiGian]) VALUES (N''ef77c942-99d5-42a9-8ee5-8ff17f86cefc'',N''PNDEMO003'',N''ADD_LINE'',N''9c91ef2c60e90c737e00ccd609b2cd4bd5504d9cc80acdff3920629496766e3b'',6,8,''2026-11-01T10:00:08'');

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
THROW;
END CATCH;
');

EXEC(N'USE [QuanLyCuaHangQuanAo];

SELECT t.name,SUM(p.rows) AS soBanGhi FROM sys.tables t JOIN sys.partitions p ON p.object_id=t.object_id AND p.index_id IN (0,1) GROUP BY t.name ORDER BY t.name;
');

-- 09: Chuan hoa ma va khoi tao sequence
EXEC(N'USE [QuanLyCuaHangQuanAo];

SET NOCOUNT ON;
SET XACT_ABORT ON;
SET LOCK_TIMEOUT 15000;

IF DB_NAME() <> N''QuanLyCuaHangQuanAo''
    THROW 51090, N''Sai database khi chuẩn hóa mã.'', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @sql nvarchar(max), @table sysname, @column sysname, @prefix varchar(12),
            @entityNo int, @phase int, @from sysname, @to sysname, @next bigint, @locked bigint;

    CREATE TABLE #Types (entityNo int PRIMARY KEY, tableName sysname, columnName sysname, prefix varchar(12));
    INSERT #Types VALUES
        (1,''NhanVien'',''maNV'',''NV''), (2,''NhaCungCap'',''maNCC'',''NCC''),
        (3,''PhieuNhap'',''maPhieuNhap'',''PHIEUNHAP''), (4,''KhachHang'',''maKH'',''KH''),
        (5,''SanPham'',''maSP'',''SP''), (6,''BienThe'',''maBienThe'',''BT''),
        (7,''LoaiSanPham'',''maLoai'',''LSP''), (8,''ThuongHieu'',''maThuongHieu'',''TH''),
        (9,''KhuyenMai'',''maKM'',''KM''), (10,''HoaDon'',''maHD'',''HD''), (11,''Thue'',''maThue'',''THUE'');

    -- Giữ khóa bảng đến khi đổi xong khóa chính và toàn bộ khóa ngoại.
    DECLARE locks CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM sys.tables WHERE schema_id = SCHEMA_ID(''dbo'') ORDER BY name;
    OPEN locks;
    FETCH NEXT FROM locks INTO @table;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @sql = N''SELECT @n=COUNT_BIG(*) FROM dbo.'' + QUOTENAME(@table) + N'' WITH (TABLOCKX,HOLDLOCK);'';
        EXEC sys.sp_executesql @sql, N''@n bigint OUTPUT'', @locked OUTPUT;
        FETCH NEXT FROM locks INTO @table;
    END;
    CLOSE locks;
    DEALLOCATE locks;

    IF OBJECT_ID(N''dbo.Seq_NV'', N''SO'') IS NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE is_disabled = 1 OR is_not_trusted = 1)
            THROW 51091, N''Cần kiểm tra các khóa ngoại trước khi đổi mã.'', 1;

        CREATE TABLE #Map (tableName sysname, oldCode varchar(20), newCode varchar(20), tempCode varchar(20),
            PRIMARY KEY (tableName,oldCode), UNIQUE (tableName,newCode), UNIQUE (tableName,tempCode));
        DECLARE types CURSOR LOCAL FAST_FORWARD FOR SELECT entityNo,tableName,columnName,prefix FROM #Types ORDER BY entityNo;
        OPEN types;
        FETCH NEXT FROM types INTO @entityNo,@table,@column,@prefix;
        WHILE @@FETCH_STATUS = 0
        BEGIN
            SET @sql = N''INSERT #Map SELECT @t, oldCode, @p + CASE WHEN n<1000 THEN RIGHT(''''000''''+CONVERT(varchar(17),n),3) ''
                + N''ELSE CONVERT(varchar(17),n) END, ''''ZZ''''+RIGHT(''''00''''+CONVERT(varchar(2),@i),2)''
                + N''+RIGHT(REPLICATE(''''0'''',16)+CONVERT(varchar(16),n),16) FROM ''
                + N''(SELECT '' + QUOTENAME(@column) + N'' oldCode, ROW_NUMBER() OVER(ORDER BY ''
                + QUOTENAME(@column) + N'') n FROM dbo.'' + QUOTENAME(@table) + N'') numbered;'';
            EXEC sys.sp_executesql @sql, N''@t sysname,@p varchar(12),@i int'',@table,@prefix,@entityNo;
            FETCH NEXT FROM types INTO @entityNo,@table,@column,@prefix;
        END;
        CLOSE types;
        DEALLOCATE types;
        IF EXISTS (SELECT 1 FROM #Map m JOIN #Map original ON original.tableName=m.tableName AND original.oldCode=m.tempCode)
            THROW 51092, N''Mã tạm bị trùng. Đã hủy toàn bộ chuyển đổi.'', 1;

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
            SET @sql=N''ALTER TABLE dbo.''+QUOTENAME(@table)+N'' NOCHECK CONSTRAINT ''+QUOTENAME(@column)+N'';'';
            EXEC sys.sp_executesql @sql;
            FETCH NEXT FROM constraints INTO @table,@column;
        END;
        CLOSE constraints;
        DEALLOCATE constraints;

        SET @phase=0;
        WHILE @phase<2
        BEGIN
            SET @from=CASE WHEN @phase=0 THEN ''oldCode'' ELSE ''tempCode'' END;
            SET @to=CASE WHEN @phase=0 THEN ''tempCode'' ELSE ''newCode'' END;
            DECLARE columnsToUpdate CURSOR LOCAL FAST_FORWARD FOR SELECT DISTINCT tableName,columnName,mappingTable FROM #Columns;
            OPEN columnsToUpdate;
            FETCH NEXT FROM columnsToUpdate INTO @table,@column,@prefix;
            WHILE @@FETCH_STATUS=0
            BEGIN
                SET @sql=N''UPDATE target SET ''+QUOTENAME(@column)+N''=m.''+QUOTENAME(@to)
                    +N'' FROM dbo.''+QUOTENAME(@table)+N'' target JOIN #Map m ON m.tableName=@t AND target.''
                    +QUOTENAME(@column)+N''=m.''+QUOTENAME(@from)+N'';'';
                EXEC sys.sp_executesql @sql,N''@t sysname'',@prefix;
                FETCH NEXT FROM columnsToUpdate INTO @table,@column,@prefix;
            END;
            CLOSE columnsToUpdate;
            DEALLOCATE columnsToUpdate;

            SET @sql=N''UPDATE l SET thamChieu=m.''+QUOTENAME(@to)+N'' FROM dbo.LichSuKho l JOIN #Map m ''
                +N''ON l.thamChieu=m.''+QUOTENAME(@from)+N'' AND ((m.tableName=''''HoaDon'''' AND l.nguon=''''BAN_HANG'''') ''
                +N''OR (m.tableName=''''PhieuNhap'''' AND l.nguon=''''NHAP_HANG''''));'';
            EXEC sys.sp_executesql @sql;
            DECLARE @before varchar(20),@after varchar(20);
            DECLARE ledgerCodes CURSOR LOCAL FAST_FORWARD FOR
                SELECT CASE WHEN @phase=0 THEN oldCode ELSE tempCode END,
                       CASE WHEN @phase=0 THEN tempCode ELSE newCode END
                FROM #Map WHERE tableName IN (''HoaDon'',''PhieuNhap'',''BienThe'');
            OPEN ledgerCodes;
            FETCH NEXT FROM ledgerCodes INTO @before,@after;
            WHILE @@FETCH_STATUS=0
            BEGIN
                UPDATE dbo.LichSuKho SET operationId=SUBSTRING(
                    REPLACE('':''+operationId+'':'', '':''+@before+'':'', '':''+@after+'':''),2,
                    LEN(REPLACE('':''+operationId+'':'', '':''+@before+'':'', '':''+@after+'':''))-2)
                WHERE CHARINDEX('':''+@before+'':'', '':''+operationId+'':'')>0;
                UPDATE dbo.LichSuKho SET lyDo=N''Nhập hàng theo phiếu ''+@after
                WHERE lyDo=N''Nhập hàng theo phiếu ''+@before;
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
            SET @sql=N''ALTER TABLE dbo.''+QUOTENAME(@table)+N'' WITH CHECK CHECK CONSTRAINT ''+QUOTENAME(@column)+N'';'';
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
            SET @sql=N''SELECT @n=COUNT_BIG(*)+1 FROM dbo.''+QUOTENAME(@table)+N'';'';
            EXEC sys.sp_executesql @sql,N''@n bigint OUTPUT'',@next OUTPUT;
            SET @sql=N''CREATE SEQUENCE dbo.''+QUOTENAME(''Seq_''+@prefix)+N'' AS bigint START WITH ''
                +CONVERT(varchar(20),@next)+N'' INCREMENT BY 1 NO CYCLE;'';
            EXEC sys.sp_executesql @sql;
            FETCH NEXT FROM sequences INTO @table,@column,@prefix;
        END;
        CLOSE sequences;
        DEALLOCATE sequences;
    END;

    -- Dọn chú thích thử nghiệm trong các trường tên hiển thị.
    DECLARE names CURSOR LOCAL FAST_FORWARD FOR
        SELECT ''NhaCungCap'',''tenNCC'' UNION ALL SELECT ''KhuyenMai'',''tenKM''
        UNION ALL SELECT ''SanPham'',''tenSP'' UNION ALL SELECT ''LoaiSanPham'',''tenLoai''
        UNION ALL SELECT ''ThuongHieu'',''tenThuongHieu'' UNION ALL SELECT ''Thue'',''tenThue'';
    OPEN names;
    FETCH NEXT FROM names INTO @table,@column;
    WHILE @@FETCH_STATUS=0
    BEGIN
        SET @sql=N''UPDATE dbo.''+QUOTENAME(@table)+N'' SET ''+QUOTENAME(@column)+N''=LTRIM(RTRIM(REPLACE(REPLACE(REPLACE(''
            +QUOTENAME(@column)+N'',N'''' (dữ liệu demo)'''',N''''''''),N'''' (demo)'''',N''''''''),N'''' (tự sinh)'''',N'''''''')));'';
        EXEC sys.sp_executesql @sql;
        FETCH NEXT FROM names INTO @table,@column;
    END;
    CLOSE names;
    DEALLOCATE names;
    UPDATE dbo.Thue SET tenThue=N''Thuế suất ''+CONVERT(nvarchar(20),phanTram)+N''%'' WHERE tenThue LIKE N''%demo%'';
    UPDATE dbo.HoaDon SET ghiChu=NULL WHERE ghiChu LIKE N''Dữ liệu demo bổ sung%'';

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
');

-- 10: Bo sung du lieu cua hang
EXEC(N'-- Bổ sung dữ liệu cửa hàng, giữ lại toàn bộ dữ liệu hiện có.
-- Chạy sau 09; toàn bộ cập nhật nằm trong một transaction, có đánh dấu để chạy lại an toàn.
-- Tài khoản bổ sung dùng mật khẩu 123456, mỗi tài khoản có PBKDF2 salt riêng.
-- Giao dịch phân bố trong 180 ngày, chỉ thay đổi tồn của các biến thể được thêm ở bước này.
USE [QuanLyCuaHangQuanAo];

SET NOCOUNT ON;
SET XACT_ABORT ON;
SET LOCK_TIMEOUT 15000;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID(N''dbo.Seq_NV'',N''SO'') IS NULL
    THROW 51110,N''Chạy 09_ChuanHoaMaVaTen.sql trước khi bổ sung dữ liệu.'',1;
 DECLARE @locked bigint,@table sysname,@sql nvarchar(max);
 DECLARE locks CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM sys.tables WHERE schema_id=SCHEMA_ID(''dbo'') ORDER BY name;
 OPEN locks; FETCH NEXT FROM locks INTO @table;
 WHILE @@FETCH_STATUS=0
 BEGIN
    SET @sql=N''SELECT @n=COUNT_BIG(*) FROM dbo.''+QUOTENAME(@table)+N'' WITH(TABLOCKX,HOLDLOCK);'';
    EXEC sys.sp_executesql @sql,N''@n bigint OUTPUT'',@locked OUTPUT;
    FETCH NEXT FROM locks INTO @table;
 END;
 CLOSE locks; DEALLOCATE locks;
 DECLARE @manager varchar(20),@oldName nvarchar(100),@today date=CONVERT(date,SYSDATETIME());
 SELECT @manager=n.maNV,@oldName=n.hoTen FROM dbo.NhanVien n JOIN dbo.TaiKhoan t ON t.maNV=n.maNV WHERE t.username=''QUAN_LY'';
 IF @manager IS NULL THROW 51111,N''Không tìm thấy nhân viên gắn với tài khoản QUAN_LY.'',1;
 UPDATE dbo.NhanVien SET hoTen=N''Võ Minh Khoa'',chucVu=N''Quản lý cửa hàng'' WHERE maNV=@manager;
 -- Chỉnh tên trong chứng từ nhập sẵn khi tên snapshot còn trùng với tên hồ sơ cũ.
 UPDATE dbo.HoaDon SET tenNhanVienSnapshot=N''Võ Minh Khoa'' WHERE maNV=@manager AND tenNhanVienSnapshot=@oldName;
 UPDATE dbo.LichSuKho SET tenNguoiThaoTacSnapshot=N''Võ Minh Khoa'' WHERE maNV=@manager AND tenNguoiThaoTacSnapshot=@oldName;
 IF NOT EXISTS(SELECT 1 FROM sys.extended_properties WHERE class=0 AND name=N''BoSungDuLieu_20261107'')
 BEGIN
 DECLARE @i int,@j int,@n bigint,@code varchar(20),@sp varchar(20),@bt varchar(20),@name nvarchar(200),
         @color nvarchar(50),@size nvarchar(20),@path varchar(500),@source varchar(20),@brand varchar(20),
         @qty int,@before int,@version bigint,@price decimal(18,2),@cost decimal(18,2),
         @when datetime2(0),@op varchar(36),@finishOp varchar(36),@hash varchar(64),@pn varchar(20),
         @supplier varchar(20),@employee varchar(20),@customer varchar(20),@promotion varchar(20),@tax varchar(20),
         @discount decimal(5,2),@taxRate decimal(5,2),@subtotal decimal(18,2),@reduction decimal(18,2),
         @taxAmount decimal(18,2),@total decimal(18,2),@state varchar(20),@employeeName nvarchar(100),@customerName nvarchar(100);
 CREATE TABLE #Staff(n int PRIMARY KEY,hoTen nvarchar(100),username varchar(50),chucVu nvarchar(100),hash varchar(255),maNV varchar(20));
 INSERT #Staff(n,hoTen,username,chucVu,hash) VALUES
 (1,N''Trần Nam Hòa'',N''NAM_HOA'',N''Nhân viên bán hàng'',N''pbkdf2-sha256$600000$QBGMTlHebtqgnOgCuJZGWg==$EdEjKCG2z4nyO5CQ3IGKfvyUXivEXLwBAUyGp4E76Ro=''),
        (2,N''Nguyễn Thu Trang'',N''THU_TRANG'',N''Nhân viên bán hàng'',N''pbkdf2-sha256$600000$Tix2tDp/kD/DA5fOopw1rQ==$bO7p+cd/H+QlsCUKG+xbgZrLx3yPQvcpcW2+zpayK5I=''),
        (3,N''Phạm Minh Đức'',N''MINH_DUC'',N''Nhân viên bán hàng'',N''pbkdf2-sha256$600000$W4h7IRoVwViNojTJTyFzgA==$Jdsueuxny8dwqzLwzqy0G8zdsZre70lCdJBv/rKKO7s=''),
        (4,N''Lê Ngọc Hân'',N''NGOC_HAN'',N''Nhân viên bán hàng'',N''pbkdf2-sha256$600000$ytHlOWLytXvRMAaSGzRKGg==$tNZ9FgjNOzYq9ZlliBOW9k8FArxzVDeFNVyaUERG/Yg=''),
        (5,N''Đặng Quốc Bảo'',N''QUOC_BAO'',N''Nhân viên bán hàng'',N''pbkdf2-sha256$600000$S9Zkf8E4rYJ1bPNd+FIp+w==$eRHl35KpN3DG5nGUwijWb1F4VpoYQWeR/OG0o+IE5xQ=''),
        (6,N''Bùi Thanh Trúc'',N''THANH_TRUC'',N''Nhân viên bán hàng'',N''pbkdf2-sha256$600000$ZrYj2N63dPTVRR1HxDDUWA==$lBTVeJ7oi/1Uc6At3pvfqDWkfPz3cHh0tG+2Ydp9tH0=''),
        (7,N''Vũ Hoàng Long'',N''HOANG_LONG'',N''Nhân viên bán hàng'',N''pbkdf2-sha256$600000$Dy/c9hkoSTJMbEm0kI9ILw==$1JL7tqQT7RbQtDSUPFnamrcXZzG3rLFAPhd03WUvMK4=''),
        (8,N''Đỗ Mỹ Linh'',N''MY_LINH'',N''Nhân viên bán hàng'',N''pbkdf2-sha256$600000$GQHNM6tJl+JRcvfphx+96w==$3dOm94P8yOoBTJvU1AP5hFUws314mMAqqLWLdNZiAcQ='');
 SET @i=1;
 WHILE @i<=8
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_NV;
    SET @code=''NV''+CASE WHEN @n<1000 THEN RIGHT(''000''+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    INSERT dbo.NhanVien(maNV,hoTen,gioiTinh,ngaySinh,sdt,email,diaChi,chucVu,trangThai)
    SELECT @code,hoTen,CASE WHEN n IN(2,4,6,8) THEN 0 ELSE 1 END,DATEFROMPARTS(1996+n,2+n,10+n),
        ''09180000''+RIGHT(''00''+CONVERT(varchar(2),n),2),LOWER(username)+''@example.com'',N''TP. Hồ Chí Minh'',chucVu,1 FROM #Staff WHERE n=@i;
    INSERT dbo.TaiKhoan(username,maNV,matKhauHash,vaiTro,trangThai)
    SELECT username,@code,hash,''NHAN_VIEN_BAN_HANG'',1 FROM #Staff WHERE n=@i;
    UPDATE #Staff SET maNV=@code WHERE n=@i;
    SET @i+=1;
 END;
 CREATE TABLE #Customers(n int PRIMARY KEY,hoTen nvarchar(100));
 INSERT #Customers VALUES
 (1,N''Nguyễn Hoàng Anh''),
        (2,N''Trần Bảo Ngọc''),
        (3,N''Lê Minh Châu''),
        (4,N''Phạm Gia Huy''),
        (5,N''Hoàng Thu Uyên''),
        (6,N''Đặng Thanh Sơn''),
        (7,N''Bùi Khánh Linh''),
        (8,N''Võ Đức Minh''),
        (9,N''Đỗ Phương Thảo''),
        (10,N''Ngô Quang Huy''),
        (11,N''Dương Ngọc Ánh''),
        (12,N''Lý Thành Đạt''),
        (13,N''Nguyễn Mai Chi''),
        (14,N''Trần Hải Nam''),
        (15,N''Lê Tuệ Nhi''),
        (16,N''Phạm Nhật Quang''),
        (17,N''Hoàng Minh Tâm''),
        (18,N''Đặng Như Ý''),
        (19,N''Bùi Anh Tuấn''),
        (20,N''Võ Thanh Hà''),
        (21,N''Đỗ Gia Bảo''),
        (22,N''Ngô Khánh Vy''),
        (23,N''Dương Tiến Dũng''),
        (24,N''Lý Ngọc Mai''),
        (25,N''Nguyễn Thanh Tùng''),
        (26,N''Trần Diệu Linh''),
        (27,N''Lê Quốc Khánh''),
        (28,N''Phạm Thùy Dương''),
        (29,N''Hoàng Bảo Trâm''),
        (30,N''Đặng Minh Khang''),
        (31,N''Bùi Hồng Nhung''),
        (32,N''Võ Phúc An''),
        (33,N''Đỗ Minh Thư''),
        (34,N''Ngô Hoàng Phúc''),
        (35,N''Dương Trà My''),
        (36,N''Lý Anh Khoa''),
        (37,N''Nguyễn Hà My''),
        (38,N''Trần Nhật Minh''),
        (39,N''Lê Phương Anh''),
        (40,N''Phạm Đức Huy''),
        (41,N''Hoàng Ngọc Huyền''),
        (42,N''Đặng Tuấn Kiệt''),
        (43,N''Bùi Nhã Phương''),
        (44,N''Võ Gia Hân''),
        (45,N''Đỗ Quỳnh Anh''),
        (46,N''Ngô Chí Thành''),
        (47,N''Dương Bích Ngọc''),
        (48,N''Lý Minh Quân'');
 SET @i=1;
 WHILE @i<=48
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_KH;
    SET @code=''KH''+CASE WHEN @n<1000 THEN RIGHT(''000''+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    INSERT dbo.KhachHang(maKH,hoTen,sdt,email,diaChi,trangThai)
    SELECT @code,hoTen,''09260000''+RIGHT(''00''+CONVERT(varchar(2),n),2),''khachhang''+CONVERT(varchar(2),n)+''@example.com'',
        CONVERT(nvarchar(5),20+n*3)+N'' ''+CASE n%4 WHEN 0 THEN N''Nguyễn Trãi'' WHEN 1 THEN N''Lê Văn Sỹ'' WHEN 2 THEN N''Cách Mạng Tháng Tám'' ELSE N''Phan Văn Trị'' END+N'', TP. Hồ Chí Minh'',1
    FROM #Customers WHERE n=@i;
    SET @i+=1;
 END;
 CREATE TABLE #Suppliers(n int PRIMARY KEY,ten nvarchar(100));
 INSERT #Suppliers VALUES (1,N''Dệt may Hưng Thịnh''),(2,N''May mặc Phú An''),(3,N''Thời trang Việt Phong''),
    (4,N''Xưởng may Hoàng Gia''),(5,N''Dệt kim Tân Phú''),(6,N''May mặc Minh Tâm''),(7,N''Thời trang An Nhiên'');
 SET @i=1;
 WHILE @i<=7
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_NCC;
    SET @code=''NCC''+CASE WHEN @n<1000 THEN RIGHT(''000''+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    INSERT dbo.NhaCungCap(maNCC,tenNCC,sdt,email,diaChi,trangThai,phienBan)
    SELECT @code,ten,''028730000''+RIGHT(''00''+CONVERT(varchar(2),n),2),''lienhe''+CONVERT(varchar(2),n)+''@example.com'',
        CONVERT(nvarchar(5),50+n*11)+N'' Quốc lộ 13, TP. Hồ Chí Minh'',1,0 FROM #Suppliers WHERE n=@i;
    SET @i+=1;
 END;
 CREATE TABLE #Brands(n int PRIMARY KEY,ten nvarchar(100),ma varchar(20));
 INSERT #Brands(n,ten) VALUES (1,N''Routine''),(2,N''Yody''),(3,N''Aristino''),(4,N''Elise''),(5,N''NEM'');
 SET @i=1;
 WHILE @i<=5
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_TH;
    SET @code=''TH''+CASE WHEN @n<1000 THEN RIGHT(''000''+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    SELECT @brand=maThuongHieu FROM dbo.ThuongHieu WHERE tenThuongHieu=(SELECT ten FROM #Brands WHERE n=@i);
    IF @brand IS NULL BEGIN INSERT dbo.ThuongHieu SELECT @code,ten FROM #Brands WHERE n=@i; SET @brand=@code; END;
    UPDATE #Brands SET ma=@brand WHERE n=@i;
    SET @brand=NULL; SET @i+=1;
 END;
 CREATE TABLE #Categories(n int PRIMARY KEY,ten nvarchar(100),ma varchar(20));
 INSERT #Categories(n,ten) VALUES(1,N''Áo thể thao''),(2,N''Áo sơ mi công sở''),(3,N''Quần công sở''),(4,N''Quần jeans'');
 SET @i=1;
 WHILE @i<=4
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_LSP;
    SET @code=''LSP''+CASE WHEN @n<1000 THEN RIGHT(''000''+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    INSERT dbo.LoaiSanPham(maLoai,tenLoai) SELECT @code,ten FROM #Categories WHERE n=@i;
    UPDATE #Categories SET ma=@code WHERE n=@i; SET @i+=1;
 END;
 CREATE TABLE #Offers(n int PRIMARY KEY,ten nvarchar(200),rate decimal(5,2),ma varchar(20));
 INSERT #Offers(n,ten,rate) VALUES (1,N''Ưu đãi khách hàng thân thiết'',5),(2,N''Ngày hội thời trang'',10),
    (3,N''Tri ân khách hàng'',15),(4,N''Ưu đãi cuối tuần'',8),(5,N''Bộ sưu tập thu đông'',12),(6,N''Mừng khai trương'',20),(7,N''Đón mùa lễ hội'',10);
 SET @i=1;
 WHILE @i<=7
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_KM;
    SET @code=''KM''+CASE WHEN @n<1000 THEN RIGHT(''000''+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    INSERT dbo.KhuyenMai(maKM,tenKM,phanTramGiam,ngayBatDau,ngayKetThuc,trangThai)
    SELECT @code,ten,rate,DATEADD(day,-365,@today),DATEADD(day,90,@today),1 FROM #Offers WHERE n=@i;
    UPDATE #Offers SET ma=@code WHERE n=@i; SET @i+=1;
 END;
 SET @i=1;
 WHILE @i<=2
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_THUE;
    SET @code=''THUE''+CASE WHEN @n<1000 THEN RIGHT(''000''+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    SET @taxRate=CASE WHEN @i=1 THEN 8 ELSE 10 END;
    IF NOT EXISTS(SELECT 1 FROM dbo.Thue WHERE phanTram=@taxRate)
        INSERT dbo.Thue(maThue,tenThue,phanTram,moTa,trangThai) VALUES(@code,N''Thuế suất ''+CONVERT(nvarchar(5),CONVERT(int,@taxRate))+N''%'',@taxRate,N''Cấu hình thuế cho giao dịch'',1);
    SET @i+=1;
 END;
 CREATE TABLE #Products(n int PRIMARY KEY,sourceSP varchar(20),ten nvarchar(200),mau nvarchar(50),duongDan varchar(500),maSP varchar(20));
 INSERT #Products(n,sourceSP,ten,mau,duongDan) VALUES
 (1,N''SP001'',N''Áo phông nữ cổ tim cotton mềm'',N''Đỏ'',N''images/bosung/mau-01.jpg''),
        (2,N''SP002'',N''Áo phông nữ dáng suông màu trơn'',N''Nâu'',N''images/bosung/mau-02.jpg''),
        (3,N''SP005'',N''Áo phông unisex hình in dáng rộng'',N''Xanh'',N''images/bosung/mau-03.jpg''),
        (4,N''SP007'',N''Áo thể thao nam cổ tròn thoáng khí'',N''Đen'',N''images/bosung/mau-04.jpg''),
        (5,N''SP009'',N''Áo polo nam cổ bẻ cơ bản'',N''Đen'',N''images/bosung/mau-05.jpg''),
        (6,N''SP010'',N''Áo polo nam phối sọc ngang'',N''Xanh dương kẻ sọc'',N''images/bosung/mau-06.jpg''),
        (7,N''SP011'',N''Áo polo nam tay ngắn phối viền'',N''Xanh lơ'',N''images/bosung/mau-07.jpg''),
        (8,N''SP012'',N''Áo polo unisex dáng hộp trẻ trung'',N''Xanh dương'',N''images/bosung/mau-08.jpg''),
        (9,N''SP013'',N''Quần soóc cargo nam túi hộp'',N''Xanh lá'',N''images/bosung/mau-09.jpg''),
        (10,N''SP014'',N''Quần soóc âu nam phong cách công sở'',N''Be'',N''images/bosung/mau-10.jpg''),
        (11,N''SP017'',N''Quần soóc cotton nam lưng chun'',N''Đen'',N''images/bosung/mau-11.jpg''),
        (12,N''SP019'',N''Quần soóc nam dáng rộng mùa hè'',N''Xám'',N''images/bosung/mau-12.jpg''),
        (13,N''SP021'',N''Áo sơ mi nam cotton đi làm'',N''Be'',N''images/bosung/mau-13.jpg''),
        (14,N''SP023'',N''Áo sơ mi nam kẻ sọc thoải mái'',N''Xanh dương kẻ sọc'',N''images/bosung/mau-14.jpg''),
        (15,N''SP025'',N''Áo sơ mi nam dáng ôm thanh lịch'',N''Xanh lơ'',N''images/bosung/mau-15.jpg''),
        (16,N''SP027'',N''Áo sơ mi flannel nam dài tay'',N''Xanh lá caro'',N''images/bosung/mau-16.jpg''),
        (17,N''SP030'',N''Quần jeans nữ cạp cao ống suông'',N''Be'',N''images/bosung/mau-17.jpg''),
        (18,N''SP032'',N''Quần nữ linen dáng rộng'',N''Be'',N''images/bosung/mau-18.jpg''),
        (19,N''SP036'',N''Áo khoác nam chống nắng nhẹ'',N''Xanh dương'',N''images/bosung/mau-19.jpg''),
        (20,N''SP040'',N''Áo khoác nam thể thao interlock'',N''Be'',N''images/bosung/mau-20.jpg'');
 SET @i=1;
 WHILE @i<=20
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_SP;
    SET @sp=''SP''+CASE WHEN @n<1000 THEN RIGHT(''000''+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    SELECT @source=sourceSP,@name=ten,@color=mau,@path=duongDan FROM #Products WHERE n=@i;
    SELECT @brand=ma FROM #Brands WHERE n=1+(@i-1)%5;
    INSERT dbo.SanPham(maSP,tenSP,maLoai,maThuongHieu,giaBan,trangThai)
    SELECT @sp,@name,COALESCE((SELECT ma FROM #Categories WHERE n=CASE @source WHEN ''SP007'' THEN 1 WHEN ''SP021'' THEN 2 WHEN ''SP025'' THEN 2 WHEN ''SP032'' THEN 3 WHEN ''SP030'' THEN 4 END),maLoai),
        @brand,ROUND(giaBan*(0.9+(@i%5)*0.05)/1000,0)*1000,1 FROM dbo.SanPham WHERE maSP=@source;
    IF @@ROWCOUNT<>1 THROW 51112,N''Không tìm thấy sản phẩm nguồn để dùng lại ảnh.'',1;
    INSERT dbo.AnhSanPham(maAnh,maSP,mauSac,duongDan,thuTu,anhChinh) VALUES(''ANH_''+@sp,@sp,@color,@path,1,1);
    UPDATE #Products SET maSP=@sp WHERE n=@i; SET @i+=1;
 END;
 CREATE TABLE #Wanted(n int IDENTITY PRIMARY KEY,maSP varchar(20),mau nvarchar(50),size nvarchar(20));
 INSERT #Wanted(maSP,mau,size)
 SELECT c.maSP,c.mauSac,s.size FROM (SELECT DISTINCT maSP,mauSac FROM dbo.AnhSanPham) c
 CROSS JOIN(VALUES(N''S''),(N''M''),(N''L''),(N''XL'')) s(size)
 WHERE NOT EXISTS(SELECT 1 FROM dbo.BienThe b WHERE b.maSP=c.maSP AND b.mauSac=c.mauSac AND b.kichCo=s.size)
 ORDER BY c.maSP,c.mauSac,s.size;
 CREATE TABLE #Variants(n int IDENTITY PRIMARY KEY,maBienThe varchar(20),maSP varchar(20));
 SET @i=1;
 WHILE @i<=(SELECT COUNT(*) FROM #Wanted)
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_BT;
    SET @bt=''BT''+CASE WHEN @n<1000 THEN RIGHT(''000''+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    SELECT @sp=maSP,@color=mau,@size=size FROM #Wanted WHERE n=@i;
    SET @qty=30+@i%25;
    INSERT dbo.BienThe(maBienThe,maSP,mauSac,kichCo,soLuongTon,trangThai,phienBan) VALUES(@bt,@sp,@color,@size,@qty,1,0);
    INSERT #Variants(maBienThe,maSP) VALUES(@bt,@sp);
    INSERT dbo.LichSuKho(maBienThe,maNV,thoiGian,nguon,thamChieu,operationId,soLuongTruoc,soLuongThayDoi,soLuongSau,phienBanTruoc,tenNguoiThaoTacSnapshot,lyDo)
    VALUES(@bt,@manager,DATEADD(hour,8,CONVERT(datetime2(0),DATEADD(day,-201,@today))),''KHOI_TAO_BIEN_THE'',@bt,''INIT:''+@bt,0,@qty,@qty,NULL,N''Võ Minh Khoa'',N''Tồn đầu kỳ cho màu và kích cỡ mới.'');
    SET @i+=1;
 END;
 DECLARE @variantCount int=(SELECT COUNT(*) FROM #Variants),@day int=1,@receiptNo int=0,@lineCount int,
         @invoiceCount int=200-(SELECT COUNT(*) FROM dbo.HoaDon),@timeBase datetime2(0)=DATEADD(day,-180,SYSDATETIME()),@invoiceTime datetime2(0),
         @supplierCount int,@customerCount int,@taxCount int,@expected varchar(20),@moneyInput varchar(40);
 SELECT ROW_NUMBER() OVER(ORDER BY maNCC) n,maNCC INTO #SupplierCodes FROM dbo.NhaCungCap WHERE trangThai=1;
 SELECT ROW_NUMBER() OVER(ORDER BY maKH) n,maKH,hoTen INTO #CustomerCodes FROM dbo.KhachHang WHERE trangThai=1;
 SELECT ROW_NUMBER() OVER(ORDER BY maThue) n,maThue,phanTram INTO #TaxCodes FROM dbo.Thue WHERE trangThai=1;
 SELECT @supplierCount=COUNT(*) FROM #SupplierCodes; SELECT @customerCount=COUNT(*) FROM #CustomerCodes; SELECT @taxCount=COUNT(*) FROM #TaxCodes;
 IF @variantCount<4 THROW 51113,N''Không đủ biến thể mới để bổ sung giao dịch.'',1;
 IF @invoiceCount<180 SET @invoiceCount=180;
 WHILE @day<=@invoiceCount
 BEGIN
    SET @invoiceTime=DATEADD(minute,(@day*(180*1440-5))/@invoiceCount,@timeBase);
    -- Nhập hàng trước giờ bán; thứ tự phiên bản khớp thứ tự thời gian sổ kho.
    IF @day%8=0 AND @receiptNo<20
    BEGIN
      SET @receiptNo+=1;
      SET @when=DATEADD(minute,-10,@invoiceTime);
      SET @n=NEXT VALUE FOR dbo.Seq_PHIEUNHAP;
      SET @pn=''PHIEUNHAP''+CASE WHEN @n<1000 THEN RIGHT(''000''+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
      SELECT @supplier=maNCC FROM #SupplierCodes WHERE n=1+(@receiptNo-1)%@supplierCount;
      SET @op=LOWER(CONVERT(varchar(36),NEWID())); SET @finishOp=LOWER(CONVERT(varchar(36),NEWID()));
      SET @state=CASE WHEN @receiptNo%10=9 THEN ''NHAP'' WHEN @receiptNo%10=0 THEN ''DA_HUY'' ELSE ''DA_XAC_NHAN'' END;
      INSERT dbo.PhieuNhap(maPhieuNhap,maNCC,maNVTao,ngayLap,trangThai,tongTien,operationIdTao,phienBan)
      VALUES(@pn,@supplier,@manager,@when,''NHAP'',0,@op,0);
      SET @hash=LOWER(CONVERT(varchar(64),HASHBYTES(''SHA2_256'',CONVERT(varbinary(max),''CREATE''+CHAR(0)+@pn+CHAR(0)+@supplier+CHAR(0)+@manager+CHAR(0))),2));
      INSERT dbo.PhieuNhapThaoTac VALUES(@op,@pn,''CREATE'',@hash,NULL,0,@when);
      SET @subtotal=0; SET @j=1;
      WHILE @j<=4
      BEGIN
        SELECT @bt=v.maBienThe,@price=p.giaBan FROM #Variants v JOIN dbo.SanPham p ON p.maSP=v.maSP WHERE v.n=1+(@receiptNo*11+@j*17)%@variantCount;
        SET @qty=8+(@receiptNo+@j)%13; SET @cost=ROUND(@price*0.6,2); SET @subtotal+=@qty*@cost;
        INSERT dbo.ChiTietPhieuNhap(maPhieuNhap,maBienThe,soLuong,donGiaNhap) VALUES(@pn,@bt,@qty,@cost);
        SET @op=LOWER(CONVERT(varchar(36),NEWID())); SET @expected=CONVERT(varchar(20),@j-1); SET @moneyInput=CONVERT(varchar(40),@cost);
        SET @hash=LOWER(CONVERT(varchar(64),HASHBYTES(''SHA2_256'',CONVERT(varbinary(max),''ADD_LINE''+CHAR(0)+@pn+CHAR(0)+@bt+CHAR(0)+@expected+CHAR(0)+CONVERT(varchar(20),@qty)+CHAR(0)+@moneyInput+CHAR(0))),2));
        INSERT dbo.PhieuNhapThaoTac VALUES(@op,@pn,''ADD_LINE'',@hash,@j-1,@j,@when);
        IF @state=''DA_XAC_NHAN''
        BEGIN
          SELECT @before=soLuongTon,@version=phienBan FROM dbo.BienThe WHERE maBienThe=@bt;
          UPDATE dbo.BienThe SET soLuongTon=soLuongTon+@qty,phienBan=phienBan+1 WHERE maBienThe=@bt;
          INSERT dbo.LichSuKho(maBienThe,maNV,thoiGian,nguon,thamChieu,operationId,soLuongTruoc,soLuongThayDoi,soLuongSau,phienBanTruoc,tenNguoiThaoTacSnapshot,lyDo)
          VALUES(@bt,@manager,@when,''NHAP_HANG'',@pn,''PN:''+@pn+'':''+@bt,@before,@qty,@before+@qty,@version,N''Võ Minh Khoa'',N''Nhập hàng theo phiếu ''+@pn);
        END;
        SET @j+=1;
      END;
      UPDATE dbo.PhieuNhap SET tongTien=@subtotal,trangThai=@state,phienBan=CASE WHEN @state=''NHAP'' THEN 4 ELSE 5 END,
        ngayXacNhan=CASE WHEN @state=''DA_XAC_NHAN'' THEN @when END,maNVXacNhan=CASE WHEN @state=''DA_XAC_NHAN'' THEN @manager END,
        operationIdXacNhan=CASE WHEN @state=''DA_XAC_NHAN'' THEN @finishOp END,phienBanTruocXacNhan=CASE WHEN @state=''DA_XAC_NHAN'' THEN 4 END WHERE maPhieuNhap=@pn;
      IF @state<>''NHAP''
      BEGIN
        SET @name=CASE WHEN @state=''DA_XAC_NHAN'' THEN ''CONFIRM'' ELSE ''CANCEL'' END;
        SET @hash=LOWER(CONVERT(varchar(64),HASHBYTES(''SHA2_256'',CONVERT(varbinary(max),CONVERT(varchar(20),@name)+CHAR(0)+@pn+CHAR(0)+''4''+CHAR(0))),2));
        INSERT dbo.PhieuNhapThaoTac VALUES(@finishOp,@pn,@name,@hash,4,5,@when);
      END;
    END;
    SET @when=@invoiceTime;
    SET @n=NEXT VALUE FOR dbo.Seq_HD;
    SET @code=''HD''+CASE WHEN @n<1000 THEN RIGHT(''000''+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    SELECT @employee=maNV,@employeeName=hoTen FROM #Staff WHERE n=1+(@day-1)%8;
    SELECT @customer=maKH,@customerName=hoTen FROM #CustomerCodes WHERE n=1+(@day*7)%@customerCount;
    SELECT @tax=maThue,@taxRate=phanTram FROM #TaxCodes WHERE n=1+@day%@taxCount;
    SELECT @promotion=ma,@discount=rate FROM #Offers WHERE n=1+@day%7;
    IF @day%3=0 BEGIN SET @promotion=NULL; SET @discount=0; END;
    SET @state=CASE WHEN @day%20=0 THEN ''NHAP'' WHEN @day%10=0 THEN ''DA_HUY'' ELSE ''DA_THANH_TOAN'' END;
    INSERT dbo.HoaDon(maHD,maNV,maKH,maKM,maThue,ngayLap,trangThai,phanTramGiam,phanTramThue,phienBan)
    VALUES(@code,@employee,@customer,@promotion,@tax,@when,''NHAP'',@discount,@taxRate,0);
    SET @subtotal=0; SET @j=1; SET @lineCount=2+@day%3;
    WHILE @j<=@lineCount
    BEGIN
      SELECT @bt=v.maBienThe,@price=p.giaBan FROM #Variants v JOIN dbo.SanPham p ON p.maSP=v.maSP WHERE v.n=1+(@day*7+@j*17)%@variantCount;
      SET @qty=1+(@day+@j)%3; SET @subtotal+=@qty*@price;
      INSERT dbo.ChiTietHoaDon(maHD,maBienThe,soLuong,donGia) VALUES(@code,@bt,@qty,@price);
      IF @state=''DA_THANH_TOAN''
      BEGIN
        SELECT @before=soLuongTon,@version=phienBan FROM dbo.BienThe WHERE maBienThe=@bt;
        IF @before<@qty THROW 51114,N''Tồn kho không đủ để bổ sung hóa đơn.'',1;
        UPDATE dbo.BienThe SET soLuongTon=soLuongTon-@qty,phienBan=phienBan+1 WHERE maBienThe=@bt;
        INSERT dbo.LichSuKho(maBienThe,maNV,thoiGian,nguon,thamChieu,operationId,soLuongTruoc,soLuongThayDoi,soLuongSau,phienBanTruoc,tenNguoiThaoTacSnapshot,lyDo)
        VALUES(@bt,@employee,@when,''BAN_HANG'',@code,''SALE:''+@code+'':''+@bt,@before,-@qty,@before-@qty,@version,@employeeName,N''Trừ tồn khi thanh toán hóa đơn.'');
      END;
      SET @j+=1;
    END;
    SET @reduction=ROUND(@subtotal*@discount/100,2); SET @taxAmount=ROUND((@subtotal-@reduction)*@taxRate/100,2); SET @total=@subtotal-@reduction+@taxAmount;
    UPDATE dbo.HoaDon SET tamTinh=@subtotal,tienGiam=@reduction,tienThue=@taxAmount,tongTien=@total,trangThai=@state,
      phienBan=@lineCount+CASE WHEN @state=''NHAP'' THEN 0 ELSE 1 END,
      ngayThanhToan=CASE WHEN @state=''DA_THANH_TOAN'' THEN @when END,
      tenNhanVienSnapshot=CASE WHEN @state=''DA_THANH_TOAN'' THEN @employeeName END,tenKhachHangSnapshot=CASE WHEN @state=''DA_THANH_TOAN'' THEN @customerName END,
      phuongThucThanhToan=CASE WHEN @state=''DA_THANH_TOAN'' THEN CASE WHEN @day%2=0 THEN ''TIEN_MAT'' ELSE ''CHUYEN_KHOAN'' END END,
      tienKhachDua=CASE WHEN @state=''DA_THANH_TOAN'' AND @day%2=0 THEN CEILING(@total/100000)*100000 END,
      tienThua=CASE WHEN @state=''DA_THANH_TOAN'' AND @day%2=0 THEN CEILING(@total/100000)*100000-@total END,
      nhanVienXacNhanChuyenKhoan=CASE WHEN @state=''DA_THANH_TOAN'' AND @day%2=1 THEN 1 END WHERE maHD=@code;
    SET @day+=1;
 END;
 -- Kiểm tra sổ kho và tổng tiền ngay trong transaction, trước khi lưu vĩnh viễn.
 IF EXISTS(SELECT 1 FROM dbo.BienThe b JOIN #Variants v ON v.maBienThe=b.maBienThe
    CROSS APPLY(SELECT TOP 1 l.soLuongSau FROM dbo.LichSuKho l WHERE l.maBienThe=b.maBienThe ORDER BY l.thoiGian DESC,l.maLichSuKho DESC) lastEntry
    WHERE b.soLuongTon<>lastEntry.soLuongSau)
    THROW 51115,N''Sổ kho không khớp tồn hiện tại.'',1;
 IF EXISTS(SELECT 1 FROM dbo.HoaDon h CROSS APPLY(SELECT SUM(thanhTien) total FROM dbo.ChiTietHoaDon d WHERE d.maHD=h.maHD) x WHERE h.tamTinh<>COALESCE(x.total,0))
    THROW 51116,N''Tổng hóa đơn không khớp chi tiết.'',1;
 IF EXISTS(SELECT 1 FROM dbo.PhieuNhap p CROSS APPLY(SELECT SUM(thanhTien) total FROM dbo.ChiTietPhieuNhap d WHERE d.maPhieuNhap=p.maPhieuNhap) x WHERE p.tongTien<>COALESCE(x.total,0))
    THROW 51117,N''Tổng phiếu nhập không khớp chi tiết.'',1;
 EXEC sys.sp_addextendedproperty @name=N''BoSungDuLieu_20261107'',@value=N''Đã bổ sung nhân viên, khách hàng, sản phẩm và 180 ngày giao dịch.'';
 END;
 COMMIT TRANSACTION;
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
');
