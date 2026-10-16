/*
  CỬA HÀNG QUẦN ÁO — SQL SERVER / SSMS
  Chạy toàn bộ script bằng Execute (F5), không chỉ chạy một đoạn.
  Không xóa database hoặc bảng hiện có. Nếu database đã có bảng người dùng,
  script từ chối tạo để tránh trộn với cấu trúc cũ.
  Mã nghiệp vụ VARCHAR do ứng dụng sinh; riêng chi tiết dùng IDENTITY.
  Phiên bản BIGINT dành cho @Version của JPA, KHÔNG phải ROWVERSION.
  Vai trò: NHAN_VIEN_BAN_HANG / NHAN_VIEN_QUAN_LY.
*/
USE [master];
GO
IF DB_ID(N'QuanLyCuaHangQuanAo') IS NULL
    EXEC(N'CREATE DATABASE [QuanLyCuaHangQuanAo]');
GO
USE [QuanLyCuaHangQuanAo];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;
    IF EXISTS (SELECT 1 FROM sys.tables WHERE is_ms_shipped = 0)
        THROW 50001, N'Database đã có bảng. Script không thay đổi cấu trúc hiện có. Hãy dùng database trống hoặc đổi tên database trong script.', 1;

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
        passwordHash VARCHAR(255) NOT NULL, -- lưu chuỗi hash gồm tham số/salt, KHÔNG lưu mật khẩu rõ
        vaiTro VARCHAR(30) NOT NULL,
        trangThai BIT NOT NULL CONSTRAINT DF_TaiKhoan_TrangThai DEFAULT (1),
        CONSTRAINT PK_TaiKhoan PRIMARY KEY (username),
        CONSTRAINT UQ_TaiKhoan_NhanVien UNIQUE (maNV),
        CONSTRAINT FK_TaiKhoan_NhanVien FOREIGN KEY (maNV) REFERENCES dbo.NhanVien(maNV),
        CONSTRAINT CK_TaiKhoan_VaiTro CHECK (vaiTro IN ('NHAN_VIEN_BAN_HANG', 'NHAN_VIEN_QUAN_LY')),
        CONSTRAINT CK_TaiKhoan_Username CHECK (LEN(LTRIM(RTRIM(username))) > 0),
        CONSTRAINT CK_TaiKhoan_Hash CHECK (LEN(LTRIM(RTRIM(passwordHash))) > 0)
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
        trangThai VARCHAR(20) NOT NULL CONSTRAINT DF_HoaDon_TrangThai DEFAULT ('NHAP'),
        ghiChu NVARCHAR(500) NULL,
        phienBan BIGINT NOT NULL CONSTRAINT DF_HoaDon_PhienBan DEFAULT (0),
        CONSTRAINT PK_HoaDon PRIMARY KEY (maHD),
        CONSTRAINT FK_HoaDon_NhanVien FOREIGN KEY (maNV) REFERENCES dbo.NhanVien(maNV),
        CONSTRAINT FK_HoaDon_KhachHang FOREIGN KEY (maKH) REFERENCES dbo.KhachHang(maKH),
        CONSTRAINT FK_HoaDon_KhuyenMai FOREIGN KEY (maKM) REFERENCES dbo.KhuyenMai(maKM),
        CONSTRAINT FK_HoaDon_Thue FOREIGN KEY (maThue) REFERENCES dbo.Thue(maThue),
        CONSTRAINT CK_HoaDon_TrangThai CHECK (trangThai IN ('NHAP','DA_THANH_TOAN','DA_HUY')),
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
    PRINT N'Đã tạo 11 bảng và 10 khóa ngoại. Có thể tạo Database Diagram trong SSMS.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/*
  LƯU Ý CHO CÀI ĐẶT JAVA / JPA
  - NVARCHAR -> String; DECIMAL -> BigDecimal; DATETIME2 -> LocalDateTime.
  - maCTHD: Long và @GeneratedValue(strategy = GenerationType.IDENTITY).
  - phienBan: Long và @Version. JPA cập nhật giá trị khi sửa thực thể.
  - thanhTien: cột tính toán; dùng @Column(insertable=false, updatable=false).
    Sau khi ghi cần refresh nếu muốn đọc giá trị do database tính ngay.
  - TaiKhoan.maNV UNIQUE: mỗi nhân viên có tối đa 1 tài khoản; tài khoản luôn
    thuộc 1 nhân viên. Việc tạo nhân viên + tài khoản có thể nằm cùng giao dịch.
  - Không ON DELETE CASCADE: tránh xóa dây chuyền lịch sử giao dịch.
  - Giá, tỷ lệ giảm/thuế và số tiền trên hóa đơn là dữ liệu chốt tại thời điểm bán.
  - Service ở SERVER phải kiểm tra hiệu lực khuyến mãi, quyền và tồn kho;
    hóa đơn thanh toán có ít nhất 1 chi tiết; tamTinh khớp tổng các chi tiết.
    CHECK không kiểm tra được các điều kiện liên bảng này.
  - Khi thanh toán: lưu hóa đơn/chi tiết và trừ tồn trong cùng giao dịch JPA;
    xử lý xung đột @Version bằng rollback, không chỉ dựa vào kiểm tra trên Client.
  - Script chỉ tạo cấu trúc, không tạo tài khoản hay dữ liệu giả.
*/
