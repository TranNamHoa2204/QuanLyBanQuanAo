-- Migration bổ sung bảng ảnh, không xóa hoặc tạo lại 11 bảng gốc.
-- Chạy trong SSMS trước 02_NhapDuLieuQuanAo.sql. Không thay đổi database tự động từ Java.
USE [QuanLyCuaHangQuanAo];
GO
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.SanPham', N'U') IS NULL
        THROW 51001, N'Chưa có bảng SanPham. Hãy tạo database bằng script gốc trước.', 1;
    IF OBJECT_ID(N'dbo.AnhSanPham', N'U') IS NULL
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
                duongDan LIKE 'images/%' AND duongDan NOT LIKE '%..%'
                AND CHARINDEX(CHAR(92),duongDan) = 0)
        );
        CREATE UNIQUE INDEX UX_AnhSanPham_Chinh
            ON dbo.AnhSanPham(maSP, mauSac) WHERE anhChinh = 1;
        PRINT N'Đã bổ sung bảng AnhSanPham. Database hiện có 12 bảng.';
    END
    ELSE
        PRINT N'AnhSanPham đã tồn tại: không thay đổi cấu trúc hiện có.';
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
