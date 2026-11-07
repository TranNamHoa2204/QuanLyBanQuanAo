-- Bổ sung dữ liệu cửa hàng, giữ lại toàn bộ dữ liệu hiện có.
-- Chạy sau 09; toàn bộ cập nhật nằm trong một transaction, có đánh dấu để chạy lại an toàn.
-- Tài khoản bổ sung dùng mật khẩu 123456, mỗi tài khoản có PBKDF2 salt riêng.
-- Giao dịch phân bố trong 180 ngày, chỉ thay đổi tồn của các biến thể được thêm ở bước này.
USE [QuanLyCuaHangQuanAo];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET LOCK_TIMEOUT 15000;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID(N'dbo.Seq_NV',N'SO') IS NULL
    THROW 51110,N'Chạy 09_ChuanHoaMaVaTen.sql trước khi bổ sung dữ liệu.',1;
 DECLARE @locked bigint,@table sysname,@sql nvarchar(max);
 DECLARE locks CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM sys.tables WHERE schema_id=SCHEMA_ID('dbo') ORDER BY name;
 OPEN locks; FETCH NEXT FROM locks INTO @table;
 WHILE @@FETCH_STATUS=0
 BEGIN
    SET @sql=N'SELECT @n=COUNT_BIG(*) FROM dbo.'+QUOTENAME(@table)+N' WITH(TABLOCKX,HOLDLOCK);';
    EXEC sys.sp_executesql @sql,N'@n bigint OUTPUT',@locked OUTPUT;
    FETCH NEXT FROM locks INTO @table;
 END;
 CLOSE locks; DEALLOCATE locks;
 DECLARE @manager varchar(20),@oldName nvarchar(100),@today date=CONVERT(date,SYSDATETIME());
 SELECT @manager=n.maNV,@oldName=n.hoTen FROM dbo.NhanVien n JOIN dbo.TaiKhoan t ON t.maNV=n.maNV WHERE t.username='QUAN_LY';
 IF @manager IS NULL THROW 51111,N'Không tìm thấy nhân viên gắn với tài khoản QUAN_LY.',1;
 UPDATE dbo.NhanVien SET hoTen=N'Võ Minh Khoa',chucVu=N'Quản lý cửa hàng' WHERE maNV=@manager;
 -- Chỉnh tên trong chứng từ nhập sẵn khi tên snapshot còn trùng với tên hồ sơ cũ.
 UPDATE dbo.HoaDon SET tenNhanVienSnapshot=N'Võ Minh Khoa' WHERE maNV=@manager AND tenNhanVienSnapshot=@oldName;
 UPDATE dbo.LichSuKho SET tenNguoiThaoTacSnapshot=N'Võ Minh Khoa' WHERE maNV=@manager AND tenNguoiThaoTacSnapshot=@oldName;
 IF NOT EXISTS(SELECT 1 FROM sys.extended_properties WHERE class=0 AND name=N'BoSungDuLieu_20261107')
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
 (1,N'Trần Nam Hòa',N'NAM_HOA',N'Nhân viên bán hàng',N'pbkdf2-sha256$600000$QBGMTlHebtqgnOgCuJZGWg==$EdEjKCG2z4nyO5CQ3IGKfvyUXivEXLwBAUyGp4E76Ro='),
        (2,N'Nguyễn Thu Trang',N'THU_TRANG',N'Nhân viên bán hàng',N'pbkdf2-sha256$600000$Tix2tDp/kD/DA5fOopw1rQ==$bO7p+cd/H+QlsCUKG+xbgZrLx3yPQvcpcW2+zpayK5I='),
        (3,N'Phạm Minh Đức',N'MINH_DUC',N'Nhân viên bán hàng',N'pbkdf2-sha256$600000$W4h7IRoVwViNojTJTyFzgA==$Jdsueuxny8dwqzLwzqy0G8zdsZre70lCdJBv/rKKO7s='),
        (4,N'Lê Ngọc Hân',N'NGOC_HAN',N'Nhân viên bán hàng',N'pbkdf2-sha256$600000$ytHlOWLytXvRMAaSGzRKGg==$tNZ9FgjNOzYq9ZlliBOW9k8FArxzVDeFNVyaUERG/Yg='),
        (5,N'Đặng Quốc Bảo',N'QUOC_BAO',N'Nhân viên bán hàng',N'pbkdf2-sha256$600000$S9Zkf8E4rYJ1bPNd+FIp+w==$eRHl35KpN3DG5nGUwijWb1F4VpoYQWeR/OG0o+IE5xQ='),
        (6,N'Bùi Thanh Trúc',N'THANH_TRUC',N'Nhân viên bán hàng',N'pbkdf2-sha256$600000$ZrYj2N63dPTVRR1HxDDUWA==$lBTVeJ7oi/1Uc6At3pvfqDWkfPz3cHh0tG+2Ydp9tH0='),
        (7,N'Vũ Hoàng Long',N'HOANG_LONG',N'Nhân viên bán hàng',N'pbkdf2-sha256$600000$Dy/c9hkoSTJMbEm0kI9ILw==$1JL7tqQT7RbQtDSUPFnamrcXZzG3rLFAPhd03WUvMK4='),
        (8,N'Đỗ Mỹ Linh',N'MY_LINH',N'Nhân viên bán hàng',N'pbkdf2-sha256$600000$GQHNM6tJl+JRcvfphx+96w==$3dOm94P8yOoBTJvU1AP5hFUws314mMAqqLWLdNZiAcQ=');
 SET @i=1;
 WHILE @i<=8
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_NV;
    SET @code='NV'+CASE WHEN @n<1000 THEN RIGHT('000'+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    INSERT dbo.NhanVien(maNV,hoTen,gioiTinh,ngaySinh,sdt,email,diaChi,chucVu,trangThai)
    SELECT @code,hoTen,CASE WHEN n IN(2,4,6,8) THEN 0 ELSE 1 END,DATEFROMPARTS(1996+n,2+n,10+n),
        '09180000'+RIGHT('00'+CONVERT(varchar(2),n),2),LOWER(username)+'@example.com',N'TP. Hồ Chí Minh',chucVu,1 FROM #Staff WHERE n=@i;
    INSERT dbo.TaiKhoan(username,maNV,matKhauHash,vaiTro,trangThai)
    SELECT username,@code,hash,'NHAN_VIEN_BAN_HANG',1 FROM #Staff WHERE n=@i;
    UPDATE #Staff SET maNV=@code WHERE n=@i;
    SET @i+=1;
 END;
 CREATE TABLE #Customers(n int PRIMARY KEY,hoTen nvarchar(100));
 INSERT #Customers VALUES
 (1,N'Nguyễn Hoàng Anh'),
        (2,N'Trần Bảo Ngọc'),
        (3,N'Lê Minh Châu'),
        (4,N'Phạm Gia Huy'),
        (5,N'Hoàng Thu Uyên'),
        (6,N'Đặng Thanh Sơn'),
        (7,N'Bùi Khánh Linh'),
        (8,N'Võ Đức Minh'),
        (9,N'Đỗ Phương Thảo'),
        (10,N'Ngô Quang Huy'),
        (11,N'Dương Ngọc Ánh'),
        (12,N'Lý Thành Đạt'),
        (13,N'Nguyễn Mai Chi'),
        (14,N'Trần Hải Nam'),
        (15,N'Lê Tuệ Nhi'),
        (16,N'Phạm Nhật Quang'),
        (17,N'Hoàng Minh Tâm'),
        (18,N'Đặng Như Ý'),
        (19,N'Bùi Anh Tuấn'),
        (20,N'Võ Thanh Hà'),
        (21,N'Đỗ Gia Bảo'),
        (22,N'Ngô Khánh Vy'),
        (23,N'Dương Tiến Dũng'),
        (24,N'Lý Ngọc Mai'),
        (25,N'Nguyễn Thanh Tùng'),
        (26,N'Trần Diệu Linh'),
        (27,N'Lê Quốc Khánh'),
        (28,N'Phạm Thùy Dương'),
        (29,N'Hoàng Bảo Trâm'),
        (30,N'Đặng Minh Khang'),
        (31,N'Bùi Hồng Nhung'),
        (32,N'Võ Phúc An'),
        (33,N'Đỗ Minh Thư'),
        (34,N'Ngô Hoàng Phúc'),
        (35,N'Dương Trà My'),
        (36,N'Lý Anh Khoa'),
        (37,N'Nguyễn Hà My'),
        (38,N'Trần Nhật Minh'),
        (39,N'Lê Phương Anh'),
        (40,N'Phạm Đức Huy'),
        (41,N'Hoàng Ngọc Huyền'),
        (42,N'Đặng Tuấn Kiệt'),
        (43,N'Bùi Nhã Phương'),
        (44,N'Võ Gia Hân'),
        (45,N'Đỗ Quỳnh Anh'),
        (46,N'Ngô Chí Thành'),
        (47,N'Dương Bích Ngọc'),
        (48,N'Lý Minh Quân');
 SET @i=1;
 WHILE @i<=48
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_KH;
    SET @code='KH'+CASE WHEN @n<1000 THEN RIGHT('000'+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    INSERT dbo.KhachHang(maKH,hoTen,sdt,email,diaChi,trangThai)
    SELECT @code,hoTen,'09260000'+RIGHT('00'+CONVERT(varchar(2),n),2),'khachhang'+CONVERT(varchar(2),n)+'@example.com',
        CONVERT(nvarchar(5),20+n*3)+N' '+CASE n%4 WHEN 0 THEN N'Nguyễn Trãi' WHEN 1 THEN N'Lê Văn Sỹ' WHEN 2 THEN N'Cách Mạng Tháng Tám' ELSE N'Phan Văn Trị' END+N', TP. Hồ Chí Minh',1
    FROM #Customers WHERE n=@i;
    SET @i+=1;
 END;
 CREATE TABLE #Suppliers(n int PRIMARY KEY,ten nvarchar(100));
 INSERT #Suppliers VALUES (1,N'Dệt may Hưng Thịnh'),(2,N'May mặc Phú An'),(3,N'Thời trang Việt Phong'),
    (4,N'Xưởng may Hoàng Gia'),(5,N'Dệt kim Tân Phú'),(6,N'May mặc Minh Tâm'),(7,N'Thời trang An Nhiên');
 SET @i=1;
 WHILE @i<=7
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_NCC;
    SET @code='NCC'+CASE WHEN @n<1000 THEN RIGHT('000'+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    INSERT dbo.NhaCungCap(maNCC,tenNCC,sdt,email,diaChi,trangThai,phienBan)
    SELECT @code,ten,'028730000'+RIGHT('00'+CONVERT(varchar(2),n),2),'lienhe'+CONVERT(varchar(2),n)+'@example.com',
        CONVERT(nvarchar(5),50+n*11)+N' Quốc lộ 13, TP. Hồ Chí Minh',1,0 FROM #Suppliers WHERE n=@i;
    SET @i+=1;
 END;
 CREATE TABLE #Brands(n int PRIMARY KEY,ten nvarchar(100),ma varchar(20));
 INSERT #Brands(n,ten) VALUES (1,N'Routine'),(2,N'Yody'),(3,N'Aristino'),(4,N'Elise'),(5,N'NEM');
 SET @i=1;
 WHILE @i<=5
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_TH;
    SET @code='TH'+CASE WHEN @n<1000 THEN RIGHT('000'+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    SELECT @brand=maThuongHieu FROM dbo.ThuongHieu WHERE tenThuongHieu=(SELECT ten FROM #Brands WHERE n=@i);
    IF @brand IS NULL BEGIN INSERT dbo.ThuongHieu SELECT @code,ten FROM #Brands WHERE n=@i; SET @brand=@code; END;
    UPDATE #Brands SET ma=@brand WHERE n=@i;
    SET @brand=NULL; SET @i+=1;
 END;
 CREATE TABLE #Categories(n int PRIMARY KEY,ten nvarchar(100),ma varchar(20));
 INSERT #Categories(n,ten) VALUES(1,N'Áo thể thao'),(2,N'Áo sơ mi công sở'),(3,N'Quần công sở'),(4,N'Quần jeans');
 SET @i=1;
 WHILE @i<=4
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_LSP;
    SET @code='LSP'+CASE WHEN @n<1000 THEN RIGHT('000'+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    INSERT dbo.LoaiSanPham(maLoai,tenLoai) SELECT @code,ten FROM #Categories WHERE n=@i;
    UPDATE #Categories SET ma=@code WHERE n=@i; SET @i+=1;
 END;
 CREATE TABLE #Offers(n int PRIMARY KEY,ten nvarchar(200),rate decimal(5,2),ma varchar(20));
 INSERT #Offers(n,ten,rate) VALUES (1,N'Ưu đãi khách hàng thân thiết',5),(2,N'Ngày hội thời trang',10),
    (3,N'Tri ân khách hàng',15),(4,N'Ưu đãi cuối tuần',8),(5,N'Bộ sưu tập thu đông',12),(6,N'Mừng khai trương',20),(7,N'Đón mùa lễ hội',10);
 SET @i=1;
 WHILE @i<=7
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_KM;
    SET @code='KM'+CASE WHEN @n<1000 THEN RIGHT('000'+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    INSERT dbo.KhuyenMai(maKM,tenKM,phanTramGiam,ngayBatDau,ngayKetThuc,trangThai)
    SELECT @code,ten,rate,DATEADD(day,-365,@today),DATEADD(day,90,@today),1 FROM #Offers WHERE n=@i;
    UPDATE #Offers SET ma=@code WHERE n=@i; SET @i+=1;
 END;
 SET @i=1;
 WHILE @i<=2
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_THUE;
    SET @code='THUE'+CASE WHEN @n<1000 THEN RIGHT('000'+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    SET @taxRate=CASE WHEN @i=1 THEN 8 ELSE 10 END;
    IF NOT EXISTS(SELECT 1 FROM dbo.Thue WHERE phanTram=@taxRate)
        INSERT dbo.Thue(maThue,tenThue,phanTram,moTa,trangThai) VALUES(@code,N'Thuế suất '+CONVERT(nvarchar(5),CONVERT(int,@taxRate))+N'%',@taxRate,N'Cấu hình thuế cho giao dịch',1);
    SET @i+=1;
 END;
 CREATE TABLE #Products(n int PRIMARY KEY,sourceSP varchar(20),ten nvarchar(200),mau nvarchar(50),duongDan varchar(500),maSP varchar(20));
 INSERT #Products(n,sourceSP,ten,mau,duongDan) VALUES
 (1,N'SP001',N'Áo phông nữ cổ tim cotton mềm',N'Đỏ',N'images/bosung/mau-01.jpg'),
        (2,N'SP002',N'Áo phông nữ dáng suông màu trơn',N'Nâu',N'images/bosung/mau-02.jpg'),
        (3,N'SP005',N'Áo phông unisex hình in dáng rộng',N'Xanh',N'images/bosung/mau-03.jpg'),
        (4,N'SP007',N'Áo thể thao nam cổ tròn thoáng khí',N'Đen',N'images/bosung/mau-04.jpg'),
        (5,N'SP009',N'Áo polo nam cổ bẻ cơ bản',N'Đen',N'images/bosung/mau-05.jpg'),
        (6,N'SP010',N'Áo polo nam phối sọc ngang',N'Xanh dương kẻ sọc',N'images/bosung/mau-06.jpg'),
        (7,N'SP011',N'Áo polo nam tay ngắn phối viền',N'Xanh lơ',N'images/bosung/mau-07.jpg'),
        (8,N'SP012',N'Áo polo unisex dáng hộp trẻ trung',N'Xanh dương',N'images/bosung/mau-08.jpg'),
        (9,N'SP013',N'Quần soóc cargo nam túi hộp',N'Xanh lá',N'images/bosung/mau-09.jpg'),
        (10,N'SP014',N'Quần soóc âu nam phong cách công sở',N'Be',N'images/bosung/mau-10.jpg'),
        (11,N'SP017',N'Quần soóc cotton nam lưng chun',N'Đen',N'images/bosung/mau-11.jpg'),
        (12,N'SP019',N'Quần soóc nam dáng rộng mùa hè',N'Xám',N'images/bosung/mau-12.jpg'),
        (13,N'SP021',N'Áo sơ mi nam cotton đi làm',N'Be',N'images/bosung/mau-13.jpg'),
        (14,N'SP023',N'Áo sơ mi nam kẻ sọc thoải mái',N'Xanh dương kẻ sọc',N'images/bosung/mau-14.jpg'),
        (15,N'SP025',N'Áo sơ mi nam dáng ôm thanh lịch',N'Xanh lơ',N'images/bosung/mau-15.jpg'),
        (16,N'SP027',N'Áo sơ mi flannel nam dài tay',N'Xanh lá caro',N'images/bosung/mau-16.jpg'),
        (17,N'SP030',N'Quần jeans nữ cạp cao ống suông',N'Be',N'images/bosung/mau-17.jpg'),
        (18,N'SP032',N'Quần nữ linen dáng rộng',N'Be',N'images/bosung/mau-18.jpg'),
        (19,N'SP036',N'Áo khoác nam chống nắng nhẹ',N'Xanh dương',N'images/bosung/mau-19.jpg'),
        (20,N'SP040',N'Áo khoác nam thể thao interlock',N'Be',N'images/bosung/mau-20.jpg');
 SET @i=1;
 WHILE @i<=20
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_SP;
    SET @sp='SP'+CASE WHEN @n<1000 THEN RIGHT('000'+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    SELECT @source=sourceSP,@name=ten,@color=mau,@path=duongDan FROM #Products WHERE n=@i;
    SELECT @brand=ma FROM #Brands WHERE n=1+(@i-1)%5;
    INSERT dbo.SanPham(maSP,tenSP,maLoai,maThuongHieu,giaBan,trangThai)
    SELECT @sp,@name,COALESCE((SELECT ma FROM #Categories WHERE n=CASE @source WHEN 'SP007' THEN 1 WHEN 'SP021' THEN 2 WHEN 'SP025' THEN 2 WHEN 'SP032' THEN 3 WHEN 'SP030' THEN 4 END),maLoai),
        @brand,ROUND(giaBan*(0.9+(@i%5)*0.05)/1000,0)*1000,1 FROM dbo.SanPham WHERE maSP=@source;
    IF @@ROWCOUNT<>1 THROW 51112,N'Không tìm thấy sản phẩm nguồn để dùng lại ảnh.',1;
    INSERT dbo.AnhSanPham(maAnh,maSP,mauSac,duongDan,thuTu,anhChinh) VALUES('ANH_'+@sp,@sp,@color,@path,1,1);
    UPDATE #Products SET maSP=@sp WHERE n=@i; SET @i+=1;
 END;
 CREATE TABLE #Wanted(n int IDENTITY PRIMARY KEY,maSP varchar(20),mau nvarchar(50),size nvarchar(20));
 INSERT #Wanted(maSP,mau,size)
 SELECT c.maSP,c.mauSac,s.size FROM (SELECT DISTINCT maSP,mauSac FROM dbo.AnhSanPham) c
 CROSS JOIN(VALUES(N'S'),(N'M'),(N'L'),(N'XL')) s(size)
 WHERE NOT EXISTS(SELECT 1 FROM dbo.BienThe b WHERE b.maSP=c.maSP AND b.mauSac=c.mauSac AND b.kichCo=s.size)
 ORDER BY c.maSP,c.mauSac,s.size;
 CREATE TABLE #Variants(n int IDENTITY PRIMARY KEY,maBienThe varchar(20),maSP varchar(20));
 SET @i=1;
 WHILE @i<=(SELECT COUNT(*) FROM #Wanted)
 BEGIN
    SET @n=NEXT VALUE FOR dbo.Seq_BT;
    SET @bt='BT'+CASE WHEN @n<1000 THEN RIGHT('000'+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    SELECT @sp=maSP,@color=mau,@size=size FROM #Wanted WHERE n=@i;
    SET @qty=30+@i%25;
    INSERT dbo.BienThe(maBienThe,maSP,mauSac,kichCo,soLuongTon,trangThai,phienBan) VALUES(@bt,@sp,@color,@size,@qty,1,0);
    INSERT #Variants(maBienThe,maSP) VALUES(@bt,@sp);
    INSERT dbo.LichSuKho(maBienThe,maNV,thoiGian,nguon,thamChieu,operationId,soLuongTruoc,soLuongThayDoi,soLuongSau,phienBanTruoc,tenNguoiThaoTacSnapshot,lyDo)
    VALUES(@bt,@manager,DATEADD(hour,8,CONVERT(datetime2(0),DATEADD(day,-201,@today))),'KHOI_TAO_BIEN_THE',@bt,'INIT:'+@bt,0,@qty,@qty,NULL,N'Võ Minh Khoa',N'Tồn đầu kỳ cho màu và kích cỡ mới.');
    SET @i+=1;
 END;
 DECLARE @variantCount int=(SELECT COUNT(*) FROM #Variants),@day int=1,@receiptNo int=0,@lineCount int,
         @invoiceCount int=200-(SELECT COUNT(*) FROM dbo.HoaDon),@timeBase datetime2(0)=DATEADD(day,-180,SYSDATETIME()),@invoiceTime datetime2(0),
         @supplierCount int,@customerCount int,@taxCount int,@expected varchar(20),@moneyInput varchar(40);
 SELECT ROW_NUMBER() OVER(ORDER BY maNCC) n,maNCC INTO #SupplierCodes FROM dbo.NhaCungCap WHERE trangThai=1;
 SELECT ROW_NUMBER() OVER(ORDER BY maKH) n,maKH,hoTen INTO #CustomerCodes FROM dbo.KhachHang WHERE trangThai=1;
 SELECT ROW_NUMBER() OVER(ORDER BY maThue) n,maThue,phanTram INTO #TaxCodes FROM dbo.Thue WHERE trangThai=1;
 SELECT @supplierCount=COUNT(*) FROM #SupplierCodes; SELECT @customerCount=COUNT(*) FROM #CustomerCodes; SELECT @taxCount=COUNT(*) FROM #TaxCodes;
 IF @variantCount<4 THROW 51113,N'Không đủ biến thể mới để bổ sung giao dịch.',1;
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
      SET @pn='PHIEUNHAP'+CASE WHEN @n<1000 THEN RIGHT('000'+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
      SELECT @supplier=maNCC FROM #SupplierCodes WHERE n=1+(@receiptNo-1)%@supplierCount;
      SET @op=LOWER(CONVERT(varchar(36),NEWID())); SET @finishOp=LOWER(CONVERT(varchar(36),NEWID()));
      SET @state=CASE WHEN @receiptNo%10=9 THEN 'NHAP' WHEN @receiptNo%10=0 THEN 'DA_HUY' ELSE 'DA_XAC_NHAN' END;
      INSERT dbo.PhieuNhap(maPhieuNhap,maNCC,maNVTao,ngayLap,trangThai,tongTien,operationIdTao,phienBan)
      VALUES(@pn,@supplier,@manager,@when,'NHAP',0,@op,0);
      SET @hash=LOWER(CONVERT(varchar(64),HASHBYTES('SHA2_256',CONVERT(varbinary(max),'CREATE'+CHAR(0)+@pn+CHAR(0)+@supplier+CHAR(0)+@manager+CHAR(0))),2));
      INSERT dbo.PhieuNhapThaoTac VALUES(@op,@pn,'CREATE',@hash,NULL,0,@when);
      SET @subtotal=0; SET @j=1;
      WHILE @j<=4
      BEGIN
        SELECT @bt=v.maBienThe,@price=p.giaBan FROM #Variants v JOIN dbo.SanPham p ON p.maSP=v.maSP WHERE v.n=1+(@receiptNo*11+@j*17)%@variantCount;
        SET @qty=8+(@receiptNo+@j)%13; SET @cost=ROUND(@price*0.6,2); SET @subtotal+=@qty*@cost;
        INSERT dbo.ChiTietPhieuNhap(maPhieuNhap,maBienThe,soLuong,donGiaNhap) VALUES(@pn,@bt,@qty,@cost);
        SET @op=LOWER(CONVERT(varchar(36),NEWID())); SET @expected=CONVERT(varchar(20),@j-1); SET @moneyInput=CONVERT(varchar(40),@cost);
        SET @hash=LOWER(CONVERT(varchar(64),HASHBYTES('SHA2_256',CONVERT(varbinary(max),'ADD_LINE'+CHAR(0)+@pn+CHAR(0)+@bt+CHAR(0)+@expected+CHAR(0)+CONVERT(varchar(20),@qty)+CHAR(0)+@moneyInput+CHAR(0))),2));
        INSERT dbo.PhieuNhapThaoTac VALUES(@op,@pn,'ADD_LINE',@hash,@j-1,@j,@when);
        IF @state='DA_XAC_NHAN'
        BEGIN
          SELECT @before=soLuongTon,@version=phienBan FROM dbo.BienThe WHERE maBienThe=@bt;
          UPDATE dbo.BienThe SET soLuongTon=soLuongTon+@qty,phienBan=phienBan+1 WHERE maBienThe=@bt;
          INSERT dbo.LichSuKho(maBienThe,maNV,thoiGian,nguon,thamChieu,operationId,soLuongTruoc,soLuongThayDoi,soLuongSau,phienBanTruoc,tenNguoiThaoTacSnapshot,lyDo)
          VALUES(@bt,@manager,@when,'NHAP_HANG',@pn,'PN:'+@pn+':'+@bt,@before,@qty,@before+@qty,@version,N'Võ Minh Khoa',N'Nhập hàng theo phiếu '+@pn);
        END;
        SET @j+=1;
      END;
      UPDATE dbo.PhieuNhap SET tongTien=@subtotal,trangThai=@state,phienBan=CASE WHEN @state='NHAP' THEN 4 ELSE 5 END,
        ngayXacNhan=CASE WHEN @state='DA_XAC_NHAN' THEN @when END,maNVXacNhan=CASE WHEN @state='DA_XAC_NHAN' THEN @manager END,
        operationIdXacNhan=CASE WHEN @state='DA_XAC_NHAN' THEN @finishOp END,phienBanTruocXacNhan=CASE WHEN @state='DA_XAC_NHAN' THEN 4 END WHERE maPhieuNhap=@pn;
      IF @state<>'NHAP'
      BEGIN
        SET @name=CASE WHEN @state='DA_XAC_NHAN' THEN 'CONFIRM' ELSE 'CANCEL' END;
        SET @hash=LOWER(CONVERT(varchar(64),HASHBYTES('SHA2_256',CONVERT(varbinary(max),CONVERT(varchar(20),@name)+CHAR(0)+@pn+CHAR(0)+'4'+CHAR(0))),2));
        INSERT dbo.PhieuNhapThaoTac VALUES(@finishOp,@pn,@name,@hash,4,5,@when);
      END;
    END;
    SET @when=@invoiceTime;
    SET @n=NEXT VALUE FOR dbo.Seq_HD;
    SET @code='HD'+CASE WHEN @n<1000 THEN RIGHT('000'+CONVERT(varchar(17),@n),3) ELSE CONVERT(varchar(17),@n) END;
    SELECT @employee=maNV,@employeeName=hoTen FROM #Staff WHERE n=1+(@day-1)%8;
    SELECT @customer=maKH,@customerName=hoTen FROM #CustomerCodes WHERE n=1+(@day*7)%@customerCount;
    SELECT @tax=maThue,@taxRate=phanTram FROM #TaxCodes WHERE n=1+@day%@taxCount;
    SELECT @promotion=ma,@discount=rate FROM #Offers WHERE n=1+@day%7;
    IF @day%3=0 BEGIN SET @promotion=NULL; SET @discount=0; END;
    SET @state=CASE WHEN @day%20=0 THEN 'NHAP' WHEN @day%10=0 THEN 'DA_HUY' ELSE 'DA_THANH_TOAN' END;
    INSERT dbo.HoaDon(maHD,maNV,maKH,maKM,maThue,ngayLap,trangThai,phanTramGiam,phanTramThue,phienBan)
    VALUES(@code,@employee,@customer,@promotion,@tax,@when,'NHAP',@discount,@taxRate,0);
    SET @subtotal=0; SET @j=1; SET @lineCount=2+@day%3;
    WHILE @j<=@lineCount
    BEGIN
      SELECT @bt=v.maBienThe,@price=p.giaBan FROM #Variants v JOIN dbo.SanPham p ON p.maSP=v.maSP WHERE v.n=1+(@day*7+@j*17)%@variantCount;
      SET @qty=1+(@day+@j)%3; SET @subtotal+=@qty*@price;
      INSERT dbo.ChiTietHoaDon(maHD,maBienThe,soLuong,donGia) VALUES(@code,@bt,@qty,@price);
      IF @state='DA_THANH_TOAN'
      BEGIN
        SELECT @before=soLuongTon,@version=phienBan FROM dbo.BienThe WHERE maBienThe=@bt;
        IF @before<@qty THROW 51114,N'Tồn kho không đủ để bổ sung hóa đơn.',1;
        UPDATE dbo.BienThe SET soLuongTon=soLuongTon-@qty,phienBan=phienBan+1 WHERE maBienThe=@bt;
        INSERT dbo.LichSuKho(maBienThe,maNV,thoiGian,nguon,thamChieu,operationId,soLuongTruoc,soLuongThayDoi,soLuongSau,phienBanTruoc,tenNguoiThaoTacSnapshot,lyDo)
        VALUES(@bt,@employee,@when,'BAN_HANG',@code,'SALE:'+@code+':'+@bt,@before,-@qty,@before-@qty,@version,@employeeName,N'Trừ tồn khi thanh toán hóa đơn.');
      END;
      SET @j+=1;
    END;
    SET @reduction=ROUND(@subtotal*@discount/100,2); SET @taxAmount=ROUND((@subtotal-@reduction)*@taxRate/100,2); SET @total=@subtotal-@reduction+@taxAmount;
    UPDATE dbo.HoaDon SET tamTinh=@subtotal,tienGiam=@reduction,tienThue=@taxAmount,tongTien=@total,trangThai=@state,
      phienBan=@lineCount+CASE WHEN @state='NHAP' THEN 0 ELSE 1 END,
      ngayThanhToan=CASE WHEN @state='DA_THANH_TOAN' THEN @when END,
      tenNhanVienSnapshot=CASE WHEN @state='DA_THANH_TOAN' THEN @employeeName END,tenKhachHangSnapshot=CASE WHEN @state='DA_THANH_TOAN' THEN @customerName END,
      phuongThucThanhToan=CASE WHEN @state='DA_THANH_TOAN' THEN CASE WHEN @day%2=0 THEN 'TIEN_MAT' ELSE 'CHUYEN_KHOAN' END END,
      tienKhachDua=CASE WHEN @state='DA_THANH_TOAN' AND @day%2=0 THEN CEILING(@total/100000)*100000 END,
      tienThua=CASE WHEN @state='DA_THANH_TOAN' AND @day%2=0 THEN CEILING(@total/100000)*100000-@total END,
      nhanVienXacNhanChuyenKhoan=CASE WHEN @state='DA_THANH_TOAN' AND @day%2=1 THEN 1 END WHERE maHD=@code;
    SET @day+=1;
 END;
 -- Kiểm tra sổ kho và tổng tiền ngay trong transaction, trước khi lưu vĩnh viễn.
 IF EXISTS(SELECT 1 FROM dbo.BienThe b JOIN #Variants v ON v.maBienThe=b.maBienThe
    CROSS APPLY(SELECT TOP 1 l.soLuongSau FROM dbo.LichSuKho l WHERE l.maBienThe=b.maBienThe ORDER BY l.thoiGian DESC,l.maLichSuKho DESC) lastEntry
    WHERE b.soLuongTon<>lastEntry.soLuongSau)
    THROW 51115,N'Sổ kho không khớp tồn hiện tại.',1;
 IF EXISTS(SELECT 1 FROM dbo.HoaDon h CROSS APPLY(SELECT SUM(thanhTien) total FROM dbo.ChiTietHoaDon d WHERE d.maHD=h.maHD) x WHERE h.tamTinh<>COALESCE(x.total,0))
    THROW 51116,N'Tổng hóa đơn không khớp chi tiết.',1;
 IF EXISTS(SELECT 1 FROM dbo.PhieuNhap p CROSS APPLY(SELECT SUM(thanhTien) total FROM dbo.ChiTietPhieuNhap d WHERE d.maPhieuNhap=p.maPhieuNhap) x WHERE p.tongTien<>COALESCE(x.total,0))
    THROW 51117,N'Tổng phiếu nhập không khớp chi tiết.',1;
 EXEC sys.sp_addextendedproperty @name=N'BoSungDuLieu_20261107',@value=N'Đã bổ sung nhân viên, khách hàng, sản phẩm và 180 ngày giao dịch.';
 END;
 COMMIT TRANSACTION;
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
GO
