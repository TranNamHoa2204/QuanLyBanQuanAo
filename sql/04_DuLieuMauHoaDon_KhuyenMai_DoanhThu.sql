/*
  GIAO DỊCH BÁN LẺ minh họa cho màn Khách hàng / Khuyến mãi / Thuế / Hóa đơn / Thống kê.
  Cấu hình thuế phải được chọn theo đúng loại hàng và điều kiện pháp lý của giao dịch.
  Chạy một lần trong SSMS theo thứ tự:
    1) TaoCSDL_QuanLyCuaHangQuanAo_SQLServer.sql
    2) 02_NhapDuLieuQuanAo.sql (catalog và biến thể)
    3) 03_CauHinhThueBanLe.sql
    4) script này
  Chỉ INSERT khi mã demo chưa tồn tại. Không sửa/xóa dữ liệu có sẵn.
  Hóa đơn thanh toán mới trừ tồn đúng một lần, trong cùng transaction.
  Chạy lại không nhân đôi hóa đơn và không trừ tồn lần hai.
*/
/*
USE [QuanLyCuaHangQuanAo];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.HoaDon', N'U') IS NULL OR OBJECT_ID(N'dbo.ChiTietHoaDon', N'U') IS NULL
       OR OBJECT_ID(N'dbo.KhachHang', N'U') IS NULL OR OBJECT_ID(N'dbo.KhuyenMai', N'U') IS NULL
       OR OBJECT_ID(N'dbo.Thue', N'U') IS NULL OR OBJECT_ID(N'dbo.BienThe', N'U') IS NULL
       OR OBJECT_ID(N'dbo.SanPham', N'U') IS NULL OR OBJECT_ID(N'dbo.NhanVien', N'U') IS NULL
       OR OBJECT_ID(N'dbo.TaiKhoan', N'U') IS NULL
        THROW 52001, N'Chưa có đủ bảng. Chạy script tạo CSDL trước.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.Thue WHERE maThue='VAT0' AND phanTram=0 AND trangThai=1)
       OR NOT EXISTS (SELECT 1 FROM dbo.Thue WHERE maThue='VAT8' AND phanTram=8 AND trangThai=1)
       OR NOT EXISTS (SELECT 1 FROM dbo.Thue WHERE maThue='VAT10' AND phanTram=10 AND trangThai=1)
        THROW 52002, N'Chạy 03_CauHinhThueBanLe.sql trước để thêm các cấu hình thuế.', 1;

    DECLARE @MaNV VARCHAR(20);
    SELECT TOP (1) @MaNV = nv.maNV
    FROM dbo.NhanVien nv
    JOIN dbo.TaiKhoan tk ON tk.maNV=nv.maNV
    WHERE nv.trangThai=1 AND tk.trangThai=1
    ORDER BY CASE WHEN tk.vaiTro='NHAN_VIEN_BAN_HANG' THEN 0 ELSE 1 END, tk.username;
    IF @MaNV IS NULL
        THROW 52003, N'Cần ít nhất một tài khoản nhân viên đang hoạt động. Tạo tài khoản quản lý đầu tiên rồi chạy lại.', 1;

    DECLARE @Khach TABLE(maKH VARCHAR(20), hoTen NVARCHAR(100), email VARCHAR(254), diaChi NVARCHAR(255));
    INSERT INTO @Khach VALUES
      ('KH9001', N'Nguyễn Minh Anh', 'minhanh@example.com', N'TP. Hồ Chí Minh'),
      ('KH9002', N'Trần Gia Hân',    'giahan@example.com', N'TP. Hồ Chí Minh');

    IF EXISTS (SELECT 1 FROM @Khach s JOIN dbo.KhachHang k ON k.maKH=s.maKH
               WHERE k.hoTen<>s.hoTen OR ISNULL(k.email,'')<>s.email)
        THROW 52004, N'Mã khách demo đã được dùng cho dữ liệu khác; không ghi đè.', 1;
    INSERT INTO dbo.KhachHang(maKH,hoTen,sdt,email,diaChi,trangThai)
        SELECT s.maKH,s.hoTen,NULL,s.email,s.diaChi,1 FROM @Khach s
        WHERE NOT EXISTS (SELECT 1 FROM dbo.KhachHang k WHERE k.maKH=s.maKH);

    DECLARE @Promo TABLE(maKM VARCHAR(20), tenKM NVARCHAR(200), phanTramGiam DECIMAL(5,2));
    INSERT INTO @Promo VALUES
      ('KM9001', N'Ưu đãi mua sắm 10%', 10.00),
      ('KM9002', N'Ưu đãi thời trang 20%', 20.00);

    IF EXISTS (SELECT 1 FROM @Promo s JOIN dbo.KhuyenMai k ON k.maKM=s.maKM
               WHERE k.tenKM<>s.tenKM OR k.phanTramGiam<>s.phanTramGiam)
        THROW 52005, N'Mã khuyến mãi demo đã được dùng cho chương trình khác; không ghi đè.', 1;
    INSERT INTO dbo.KhuyenMai(maKM,tenKM,phanTramGiam,ngayBatDau,ngayKetThuc,trangThai)
        SELECT s.maKM,s.tenKM,s.phanTramGiam,DATEADD(DAY,-30,CONVERT(date,GETDATE())),
               DATEADD(DAY,365,CONVERT(date,GETDATE())),1
        FROM @Promo s
        WHERE NOT EXISTS (SELECT 1 FROM dbo.KhuyenMai k WHERE k.maKM=s.maKM);

    DECLARE @Seed TABLE(
        maHD VARCHAR(20) PRIMARY KEY, ngayLech INT, gio INT,
        maKH VARCHAR(20) NULL, maKM VARCHAR(20) NULL, maThue VARCHAR(20),
        maBienThe VARCHAR(20), soLuong INT, trangThai VARCHAR(20)
    );
    INSERT INTO @Seed VALUES
      ('HD900001',-3,10,'KH9001','KM9001','VAT8','BT000002',1,'DA_THANH_TOAN'),
      ('HD900002',-2,12,'KH9002','KM9002','VAT10','BT000008',2,'DA_THANH_TOAN'),
      ('HD900003',-1,14,NULL,     NULL,    'VAT10','BT000014',1,'DA_THANH_TOAN'),
      ('HD900004', 0,16,'KH9001','KM9001','VAT8','BT000020',1,'DA_THANH_TOAN'),
      ('HD900005', 0,17,'KH9002',NULL,    'VAT10','BT000003',1,'NHAP'),
      ('HD900006',-1,18,NULL,     NULL,    'VAT10','BT000004',1,'DA_HUY');

    IF EXISTS (SELECT 1 FROM @Seed s JOIN dbo.HoaDon h ON h.maHD=s.maHD
               WHERE ISNULL(h.ghiChu,'') <> N'Bán hàng tại cửa hàng | ' + s.maHD)
        THROW 52006, N'Mã hóa đơn demo đang được dữ liệu khác sử dụng; script không ghi đè.', 1;

    DECLARE @Source TABLE(
        maHD VARCHAR(20) PRIMARY KEY, ngayLap DATETIME2(0), maKH VARCHAR(20) NULL,
        maKM VARCHAR(20) NULL, maThue VARCHAR(20), phanTramGiam DECIMAL(5,2),
        phanTramThue DECIMAL(5,2), maBienThe VARCHAR(20), soLuong INT,
        donGia DECIMAL(18,2), tamTinh DECIMAL(18,2), tienGiam DECIMAL(18,2),
        tienThue DECIMAL(18,2), tongTien DECIMAL(18,2), trangThai VARCHAR(20)
    );

    INSERT INTO @Source
    SELECT s.maHD,
           DATEADD(HOUR,s.gio,DATEADD(DAY,s.ngayLech,CONVERT(datetime2(0),CONVERT(date,GETDATE())))),
           s.maKH,s.maKM,s.maThue,calc.phanTramGiam,t.phanTram,s.maBienThe,s.soLuong,
           p.giaBan,base.tamTinh,calc.tienGiam,calc.tienThue,
           base.tamTinh-calc.tienGiam+calc.tienThue,s.trangThai
    FROM @Seed s
    JOIN dbo.BienThe b ON b.maBienThe=s.maBienThe
    JOIN dbo.SanPham p ON p.maSP=b.maSP
    JOIN dbo.Thue t ON t.maThue=s.maThue
    LEFT JOIN dbo.KhuyenMai k ON k.maKM=s.maKM
    CROSS APPLY (SELECT CAST(p.giaBan * s.soLuong AS DECIMAL(18,2)) AS tamTinh) base
    CROSS APPLY (
        SELECT ISNULL(k.phanTramGiam,0) AS phanTramGiam,
               CAST(ROUND(base.tamTinh * ISNULL(k.phanTramGiam,0) / 100.0,2) AS DECIMAL(18,2)) AS tienGiam,
               CAST(ROUND((base.tamTinh - CAST(ROUND(base.tamTinh * ISNULL(k.phanTramGiam,0) / 100.0,2) AS DECIMAL(18,2))) * t.phanTram / 100.0,2) AS DECIMAL(18,2)) AS tienThue
    ) calc
    WHERE (s.maKH IS NULL OR EXISTS (SELECT 1 FROM dbo.KhachHang kh WHERE kh.maKH=s.maKH))
      AND (s.maKM IS NULL OR k.maKM IS NOT NULL);

    IF (SELECT COUNT(*) FROM @Source) <> (SELECT COUNT(*) FROM @Seed)
        THROW 52007, N'Sản phẩm/biến thể/khách/khuyến mãi/thuế demo đang thiếu; kiểm tra các mã trong script.', 1;

    IF EXISTS (
        SELECT 1 FROM @Source s
        WHERE NOT EXISTS (SELECT 1 FROM dbo.HoaDon h WHERE h.maHD=s.maHD)
          AND (NOT EXISTS (SELECT 1 FROM dbo.BienThe b JOIN dbo.SanPham p ON p.maSP=b.maSP
                           WHERE b.maBienThe=s.maBienThe AND b.trangThai=1 AND p.trangThai=1)
               OR NOT EXISTS (SELECT 1 FROM dbo.Thue t WHERE t.maThue=s.maThue AND t.trangThai=1)
               OR (s.maKH IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.KhachHang k WHERE k.maKH=s.maKH AND k.trangThai=1))
               OR (s.maKM IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.KhuyenMai k WHERE k.maKM=s.maKM AND k.trangThai=1
                        AND CONVERT(date,GETDATE()) BETWEEN k.ngayBatDau AND k.ngayKetThuc)))
        THROW 52008, N'Một cấu hình demo cần cho hóa đơn mới đang ngừng hoạt động hoặc ngoài thời hạn.', 1;

    IF EXISTS (SELECT 1 FROM @Source s JOIN dbo.HoaDon h ON h.maHD=s.maHD
               WHERE h.maNV<>@MaNV OR h.maThue<>s.maThue OR ISNULL(h.maKH,'')<>ISNULL(s.maKH,'')
                  OR ISNULL(h.maKM,'')<>ISNULL(s.maKM,'') OR h.trangThai<>s.trangThai)
        THROW 52009, N'Hóa đơn demo cùng mã có dữ liệu không khớp; script không ghi đè.', 1;

    IF EXISTS (
        SELECT 1 FROM @Source s
        WHERE s.trangThai='DA_THANH_TOAN'
          AND NOT EXISTS (SELECT 1 FROM dbo.HoaDon h WHERE h.maHD=s.maHD)
          AND ISNULL((SELECT b.soLuongTon FROM dbo.BienThe b WITH (UPDLOCK,HOLDLOCK)
                      WHERE b.maBienThe=s.maBienThe),-1) < s.soLuong
    )
        THROW 52010, N'Tồn kho không đủ cho dữ liệu minh họa; đã hủy toàn bộ thao tác.', 1;

    DECLARE @Created TABLE(maHD VARCHAR(20) PRIMARY KEY);
    INSERT INTO dbo.HoaDon(maHD,maNV,maKH,maKM,maThue,ngayLap,tamTinh,phanTramGiam,tienGiam,
                           phanTramThue,tienThue,tongTien,trangThai,ghiChu,phienBan)
    OUTPUT INSERTED.maHD INTO @Created(maHD)
    SELECT s.maHD,@MaNV,s.maKH,s.maKM,s.maThue,s.ngayLap,s.tamTinh,s.phanTramGiam,s.tienGiam,
           s.phanTramThue,s.tienThue,s.tongTien,s.trangThai,N'Bán hàng tại cửa hàng | '+s.maHD,0
    FROM @Source s
    WHERE NOT EXISTS (SELECT 1 FROM dbo.HoaDon h WITH (UPDLOCK,HOLDLOCK) WHERE h.maHD=s.maHD);

    INSERT INTO dbo.ChiTietHoaDon(maHD,maBienThe,soLuong,donGia)
    SELECT s.maHD,s.maBienThe,s.soLuong,s.donGia
    FROM @Source s JOIN @Created c ON c.maHD=s.maHD;

    DECLARE @ExpectedVariants INT = (
        SELECT COUNT(*) FROM (
            SELECT DISTINCT s.maBienThe FROM @Source s JOIN @Created c ON c.maHD=s.maHD
            WHERE s.trangThai='DA_THANH_TOAN'
        ) d
    );
    UPDATE b
       SET b.soLuongTon=b.soLuongTon-d.soLuong,
           b.phienBan=b.phienBan+1
    FROM dbo.BienThe b
    JOIN (
        SELECT s.maBienThe,SUM(s.soLuong) AS soLuong
        FROM @Source s JOIN @Created c ON c.maHD=s.maHD
        WHERE s.trangThai='DA_THANH_TOAN'
        GROUP BY s.maBienThe
    ) d ON d.maBienThe=b.maBienThe
    WHERE b.soLuongTon >= d.soLuong;

    IF @@ROWCOUNT <> @ExpectedVariants
        THROW 52011, N'Tồn kho vừa thay đổi; giao dịch demo đã rollback an toàn.', 1;

    COMMIT TRANSACTION;

    SELECT h.maHD,h.ngayLap,h.trangThai,h.tamTinh,h.tienGiam,h.tienThue,h.tongTien,h.ghiChu
    FROM dbo.HoaDon h WHERE h.maHD IN ('HD900001','HD900002','HD900003','HD900004','HD900005','HD900006')
    ORDER BY h.ngayLap,h.maHD;

    SELECT maKM,tenKM,phanTramGiam,ngayBatDau,ngayKetThuc,trangThai
    FROM dbo.KhuyenMai WHERE maKM IN ('KM9001','KM9002') ORDER BY maKM;

    SELECT maThue,tenThue,phanTram,trangThai
    FROM dbo.Thue WHERE maThue IN ('VAT0','VAT8','VAT10') ORDER BY maThue;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
*/

/*
  GIAO DỊCH BÁN LẺ minh họa cho màn Khách hàng / Khuyến mãi / Thuế / Hóa đơn / Thống kê.
  Cấu hình thuế phải được chọn theo đúng loại hàng và điều kiện pháp lý của giao dịch.
  Chạy một lần trong SSMS theo thứ tự:
    1) TaoCSDL_QuanLyCuaHangQuanAo_SQLServer.sql
    2) 02_NhapDuLieuQuanAo.sql (catalog và biến thể)
    3) 03_CauHinhThueBanLe.sql
    4) script này
  Chỉ INSERT khi mã demo chưa tồn tại. Không sửa/xóa dữ liệu có sẵn.
  Hóa đơn thanh toán mới trừ tồn đúng một lần, trong cùng transaction.
  Chạy lại không nhân đôi hóa đơn và không trừ tồn lần hai.
*/
USE [QuanLyCuaHangQuanAo];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.HoaDon', N'U') IS NULL OR OBJECT_ID(N'dbo.ChiTietHoaDon', N'U') IS NULL
       OR OBJECT_ID(N'dbo.KhachHang', N'U') IS NULL OR OBJECT_ID(N'dbo.KhuyenMai', N'U') IS NULL
       OR OBJECT_ID(N'dbo.Thue', N'U') IS NULL OR OBJECT_ID(N'dbo.BienThe', N'U') IS NULL
       OR OBJECT_ID(N'dbo.SanPham', N'U') IS NULL OR OBJECT_ID(N'dbo.NhanVien', N'U') IS NULL
       OR OBJECT_ID(N'dbo.TaiKhoan', N'U') IS NULL
    BEGIN
        THROW 52001, N'Chưa có đủ bảng. Chạy script tạo CSDL trước.', 1;
    END;

    IF NOT EXISTS (SELECT 1 FROM dbo.Thue WHERE maThue='VAT0' AND phanTram=0 AND trangThai=1)
       OR NOT EXISTS (SELECT 1 FROM dbo.Thue WHERE maThue='VAT8' AND phanTram=8 AND trangThai=1)
       OR NOT EXISTS (SELECT 1 FROM dbo.Thue WHERE maThue='VAT10' AND phanTram=10 AND trangThai=1)
    BEGIN
        THROW 52002, N'Chạy 03_CauHinhThueBanLe.sql trước để thêm các cấu hình thuế.', 1;
    END;

    DECLARE @MaNV VARCHAR(20);
    SELECT TOP (1) @MaNV = nv.maNV
    FROM dbo.NhanVien nv
    JOIN dbo.TaiKhoan tk ON tk.maNV=nv.maNV
    WHERE nv.trangThai=1 AND tk.trangThai=1
    ORDER BY CASE WHEN tk.vaiTro='NHAN_VIEN_BAN_HANG' THEN 0 ELSE 1 END, tk.username;

    IF @MaNV IS NULL
    BEGIN
        THROW 52003, N'Cần ít nhất một tài khoản nhân viên đang hoạt động. Tạo tài khoản quản lý đầu tiên rồi chạy lại.', 1;
    END;

    DECLARE @Khach TABLE(maKH VARCHAR(20), hoTen NVARCHAR(100), email VARCHAR(254), diaChi NVARCHAR(255));
    INSERT INTO @Khach VALUES
      ('KH9001', N'Nguyễn Minh Anh', 'minhanh@example.com', N'TP. Hồ Chí Minh'),
      ('KH9002', N'Trần Gia Hân',    'giahan@example.com', N'TP. Hồ Chí Minh');

    IF EXISTS (SELECT 1 FROM @Khach s JOIN dbo.KhachHang k ON k.maKH=s.maKH
               WHERE k.hoTen<>s.hoTen OR ISNULL(k.email,'')<>s.email)
    BEGIN
        THROW 52004, N'Mã khách demo đã được dùng cho dữ liệu khác; không ghi đè.', 1;
    END;

    INSERT INTO dbo.KhachHang(maKH,hoTen,sdt,email,diaChi,trangThai)
        SELECT s.maKH,s.hoTen,NULL,s.email,s.diaChi,1 FROM @Khach s
        WHERE NOT EXISTS (SELECT 1 FROM dbo.KhachHang k WHERE k.maKH=s.maKH);

    DECLARE @Promo TABLE(maKM VARCHAR(20), tenKM NVARCHAR(200), phanTramGiam DECIMAL(5,2));
    INSERT INTO @Promo VALUES
      ('KM9001', N'Ưu đãi mua sắm 10%', 10.00),
      ('KM9002', N'Ưu đãi thời trang 20%', 20.00);

    IF EXISTS (SELECT 1 FROM @Promo s JOIN dbo.KhuyenMai k ON k.maKM=s.maKM
               WHERE k.tenKM<>s.tenKM OR k.phanTramGiam<>s.phanTramGiam)
    BEGIN
        THROW 52005, N'Mã khuyến mãi demo đã được dùng cho chương trình khác; không ghi đè.', 1;
    END;

    INSERT INTO dbo.KhuyenMai(maKM,tenKM,phanTramGiam,ngayBatDau,ngayKetThuc,trangThai)
        SELECT s.maKM,s.tenKM,s.phanTramGiam,DATEADD(DAY,-30,CONVERT(date,GETDATE())),
               DATEADD(DAY,365,CONVERT(date,GETDATE())),1
        FROM @Promo s
        WHERE NOT EXISTS (SELECT 1 FROM dbo.KhuyenMai k WHERE k.maKM=s.maKM);

    DECLARE @Seed TABLE(
        maHD VARCHAR(20) PRIMARY KEY, ngayLech INT, gio INT,
        maKH VARCHAR(20) NULL, maKM VARCHAR(20) NULL, maThue VARCHAR(20),
        maBienThe VARCHAR(20), soLuong INT, trangThai VARCHAR(20)
    );
    INSERT INTO @Seed VALUES
      ('HD900001',-3,10,'KH9001','KM9001','VAT8','BT000002',1,'DA_THANH_TOAN'),
      ('HD900002',-2,12,'KH9002','KM9002','VAT10','BT000008',2,'DA_THANH_TOAN'),
      ('HD900003',-1,14,NULL,     NULL,    'VAT10','BT000014',1,'DA_THANH_TOAN'),
      ('HD900004', 0,16,'KH9001','KM9001','VAT8','BT000020',1,'DA_THANH_TOAN'),
      ('HD900005', 0,17,'KH9002',NULL,    'VAT10','BT000003',1,'NHAP'),
      ('HD900006',-1,18,NULL,     NULL,    'VAT10','BT000004',1,'DA_HUY');

    IF EXISTS (SELECT 1 FROM @Seed s JOIN dbo.HoaDon h ON h.maHD=s.maHD
               WHERE ISNULL(h.ghiChu,'') <> N'Bán hàng tại cửa hàng | ' + s.maHD)
    BEGIN
        THROW 52006, N'Mã hóa đơn demo đang được dữ liệu khác sử dụng; script không ghi đè.', 1;
    END;

    DECLARE @Source TABLE(
        maHD VARCHAR(20) PRIMARY KEY, ngayLap DATETIME2(0), maKH VARCHAR(20) NULL,
        maKM VARCHAR(20) NULL, maThue VARCHAR(20), phanTramGiam DECIMAL(5,2),
        phanTramThue DECIMAL(5,2), maBienThe VARCHAR(20), soLuong INT,
        donGia DECIMAL(18,2), tamTinh DECIMAL(18,2), tienGiam DECIMAL(18,2),
        tienThue DECIMAL(18,2), tongTien DECIMAL(18,2), trangThai VARCHAR(20)
    );

    INSERT INTO @Source
    SELECT s.maHD,
           DATEADD(HOUR,s.gio,DATEADD(DAY,s.ngayLech,CONVERT(datetime2(0),CONVERT(date,GETDATE())))),
           s.maKH,s.maKM,s.maThue,calc.phanTramGiam,t.phanTram,s.maBienThe,s.soLuong,
           p.giaBan,base.tamTinh,calc.tienGiam,calc.tienThue,
           base.tamTinh-calc.tienGiam+calc.tienThue,s.trangThai
    FROM @Seed s
    JOIN dbo.BienThe b ON b.maBienThe=s.maBienThe
    JOIN dbo.SanPham p ON p.maSP=b.maSP
    JOIN dbo.Thue t ON t.maThue=s.maThue
    LEFT JOIN dbo.KhuyenMai k ON k.maKM=s.maKM
    CROSS APPLY (SELECT CAST(p.giaBan * s.soLuong AS DECIMAL(18,2)) AS tamTinh) base
    CROSS APPLY (
        SELECT ISNULL(k.phanTramGiam,0) AS phanTramGiam,
               CAST(ROUND(base.tamTinh * ISNULL(k.phanTramGiam,0) / 100.0,2) AS DECIMAL(18,2)) AS tienGiam,
               CAST(ROUND((base.tamTinh - CAST(ROUND(base.tamTinh * ISNULL(k.phanTramGiam,0) / 100.0,2) AS DECIMAL(18,2))) * t.phanTram / 100.0,2) AS DECIMAL(18,2)) AS tienThue
    ) calc
    WHERE (s.maKH IS NULL OR EXISTS (SELECT 1 FROM dbo.KhachHang kh WHERE kh.maKH=s.maKH))
      AND (s.maKM IS NULL OR k.maKM IS NOT NULL);

    IF (SELECT COUNT(*) FROM @Source) <> (SELECT COUNT(*) FROM @Seed)
    BEGIN
        THROW 52007, N'Sản phẩm/biến thể/khách/khuyến mãi/thuế demo đang thiếu; kiểm tra các mã trong script.', 1;
    END;

    IF EXISTS (
        SELECT 1 FROM @Source s
        WHERE NOT EXISTS (SELECT 1 FROM dbo.HoaDon h WHERE h.maHD=s.maHD)
          AND (NOT EXISTS (SELECT 1 FROM dbo.BienThe b JOIN dbo.SanPham p ON p.maSP=b.maSP
                           WHERE b.maBienThe=s.maBienThe AND b.trangThai=1 AND p.trangThai=1)
               OR NOT EXISTS (SELECT 1 FROM dbo.Thue t WHERE t.maThue=s.maThue AND t.trangThai=1)
               OR (s.maKH IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.KhachHang k WHERE k.maKH=s.maKH AND k.trangThai=1))
               OR (s.maKM IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.KhuyenMai k WHERE k.maKM=s.maKM AND k.trangThai=1
                        AND CONVERT(date,GETDATE()) BETWEEN k.ngayBatDau AND k.ngayKetThuc)))
    )
    BEGIN
        THROW 52008, N'Một cấu hình demo cần cho hóa đơn mới đang ngừng hoạt động hoặc ngoài thời hạn.', 1;
    END;

    IF EXISTS (SELECT 1 FROM @Source s JOIN dbo.HoaDon h ON h.maHD=s.maHD
               WHERE h.maNV<>@MaNV OR h.maThue<>s.maThue OR ISNULL(h.maKH,'')<>ISNULL(s.maKH,'')
                  OR ISNULL(h.maKM,'')<>ISNULL(s.maKM,'') OR h.trangThai<>s.trangThai)
    BEGIN
        THROW 52009, N'Hóa đơn demo cùng mã có dữ liệu không khớp; script không ghi đè.', 1;
    END;

    IF EXISTS (
        SELECT 1 FROM @Source s
        WHERE s.trangThai='DA_THANH_TOAN'
          AND NOT EXISTS (SELECT 1 FROM dbo.HoaDon h WHERE h.maHD=s.maHD)
          AND ISNULL((SELECT b.soLuongTon FROM dbo.BienThe b WITH (UPDLOCK,HOLDLOCK)
                      WHERE b.maBienThe=s.maBienThe),-1) < s.soLuong
    )
    BEGIN
        THROW 52010, N'Tồn kho không đủ cho dữ liệu minh họa; đã hủy toàn bộ thao tác.', 1;
    END;

    DECLARE @Created TABLE(maHD VARCHAR(20) PRIMARY KEY);
    INSERT INTO dbo.HoaDon(maHD,maNV,maKH,maKM,maThue,ngayLap,tamTinh,phanTramGiam,tienGiam,
                           phanTramThue,tienThue,tongTien,trangThai,ghiChu,phienBan)
    OUTPUT INSERTED.maHD INTO @Created(maHD)
    SELECT s.maHD,@MaNV,s.maKH,s.maKM,s.maThue,s.ngayLap,s.tamTinh,s.phanTramGiam,s.tienGiam,
           s.phanTramThue,s.tienThue,s.tongTien,s.trangThai,N'Bán hàng tại cửa hàng | '+s.maHD,0
    FROM @Source s
    WHERE NOT EXISTS (SELECT 1 FROM dbo.HoaDon h WITH (UPDLOCK,HOLDLOCK) WHERE h.maHD=s.maHD);

    INSERT INTO dbo.ChiTietHoaDon(maHD,maBienThe,soLuong,donGia)
    SELECT s.maHD,s.maBienThe,s.soLuong,s.donGia
    FROM @Source s JOIN @Created c ON c.maHD=s.maHD;

    DECLARE @ExpectedVariants INT = (
        SELECT COUNT(*) FROM (
            SELECT DISTINCT s.maBienThe FROM @Source s JOIN @Created c ON c.maHD=s.maHD
            WHERE s.trangThai='DA_THANH_TOAN'
        ) d
    );
    UPDATE b
       SET b.soLuongTon=b.soLuongTon-d.soLuong,
           b.phienBan=b.phienBan+1
    FROM dbo.BienThe b
    JOIN (
        SELECT s.maBienThe,SUM(s.soLuong) AS soLuong
        FROM @Source s JOIN @Created c ON c.maHD=s.maHD
        WHERE s.trangThai='DA_THANH_TOAN'
        GROUP BY s.maBienThe
    ) d ON d.maBienThe=b.maBienThe
    WHERE b.soLuongTon >= d.soLuong;

    IF @@ROWCOUNT <> @ExpectedVariants
    BEGIN
        THROW 52011, N'Tồn kho vừa thay đổi; giao dịch demo đã rollback an toàn.', 1;
    END;

    COMMIT TRANSACTION;

    SELECT h.maHD,h.ngayLap,h.trangThai,h.tamTinh,h.tienGiam,h.tienThue,h.tongTien,h.ghiChu
    FROM dbo.HoaDon h WHERE h.maHD IN ('HD900001','HD900002','HD900003','HD900004','HD900005','HD900006')
    ORDER BY h.ngayLap,h.maHD;

    SELECT maKM,tenKM,phanTramGiam,ngayBatDau,ngayKetThuc,trangThai
    FROM dbo.KhuyenMai WHERE maKM IN ('KM9001','KM9002') ORDER BY maKM;

    SELECT maThue,tenThue,phanTram,trangThai
    FROM dbo.Thue WHERE maThue IN ('VAT0','VAT8','VAT10') ORDER BY maThue;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO