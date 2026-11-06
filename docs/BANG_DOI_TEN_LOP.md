# Bảng đổi tên lớp và package

Tên cũ → vị trí mới sau khi sắp xếp ngày 06/11/2026. Bảng gồm mã nguồn và kiểm thử.

| Lớp trước đây | Lớp hiện tại |
| --- | --- |
| `client.api.ApiException` | `client.api.NgoaiLeGoiDichVu` |
| `client.api.AuthApi` | `client.api.GoiDichVuXacThuc` |
| `client.api.HoaDonApi` | `client.api.GoiDichVuHoaDon` |
| `client.api.KhachHangApi` | `client.api.GoiDichVuKhachHang` |
| `client.api.KhuyenMaiApi` | `client.api.GoiDichVuKhuyenMai` |
| `client.api.LichSuKhoApi` | `client.api.GoiDichVuLichSuKho` |
| `client.api.NhaCungCapApi` | `client.api.GoiDichVuNhaCungCap` |
| `client.api.NhanVienApi` | `client.api.GoiDichVuNhanVien` |
| `client.api.PhieuNhapApi` | `client.api.GoiDichVuPhieuNhap` |
| `client.api.SanPhamApi` | `client.api.GoiDichVuSanPham` |
| `client.api.ThongKeApi` | `client.api.GoiDichVuThongKe` |
| `client.ClientMain` | `client.app.ClientMain` |
| `client.config.ClientConfig` | `client.config.ClientConfig` |
| `client.controller.BanHangController` | `client.controllers.DieuKhienBanHang` |
| `client.controller.DangNhapController` | `client.controllers.DieuKhienDangNhap` |
| `client.controller.DoiMatKhauController` | `client.controllers.DieuKhienDoiMatKhau` |
| `client.controller.KhachHangController` | `client.controllers.DieuKhienKhachHang` |
| `client.controller.KhuyenMaiController` | `client.controllers.DieuKhienKhuyenMai` |
| `client.controller.NhaCungCapController` | `client.controllers.DieuKhienNhaCungCap` |
| `client.controller.NhanVienController` | `client.controllers.DieuKhienNhanVien` |
| `client.controller.PhieuNhapController` | `client.controllers.DieuKhienPhieuNhap` |
| `client.controller.QuanLySanPhamController` | `client.controllers.DieuKhienQuanLySanPham` |
| `client.controller.SanPhamController` | `client.controllers.DieuKhienSanPham` |
| `client.controller.ThongKeController` | `client.controllers.DieuKhienThongKe` |
| `client.controller.ThongTinCaNhanController` | `client.controllers.DieuKhienThongTinCaNhan` |
| `client.network.SocketClient` | `client.network.KetNoiMayChu` |
| `client.session.ClientSession` | `client.session.PhienDangNhap` |
| `client.ui.BanHangPanel` | `client.gui.banhang.KhungBanHang` |
| `client.ui.BienTheManagementPanel` | `client.gui.sanpham.KhungQuanLyBienThe` |
| `client.ui.BrandIcon` | `client.gui.components.BieuTuongCuaHang` |
| `client.ui.DangNhapForm` | `client.gui.hethong.ManHinhDangNhap` |
| `client.ui.DangNhapPanel` | `client.gui.hethong.KhungDangNhap` |
| `client.ui.DanhMucPanel` | `client.gui.danhmuc.KhungDanhMuc` |
| `client.ui.DoiMatKhauForm` | `client.gui.hethong.ManHinhDoiMatKhau` |
| `client.ui.DoiMatKhauPanel` | `client.gui.hethong.KhungDoiMatKhau` |
| `client.ui.KhachHangForm` | `client.gui.khachhang.ManHinhKhachHang` |
| `client.ui.KhuyenMaiForm` | `client.gui.khuyenmai.ManHinhKhuyenMai` |
| `client.ui.LichSuKhoPanel` | `client.gui.kho.KhungLichSuKho` |
| `client.ui.MainForm` | `client.gui.hethong.ManHinhChinh` |
| `client.ui.MainShellPanel` | `client.gui.hethong.KhungUngDung` |
| `client.ui.NhaCungCapForm` | `client.gui.nhapkho.ManHinhNhaCungCap` |
| `client.ui.NhanVienForm` | `client.gui.nhanvien.ManHinhNhanVien` |
| `client.ui.NhanVienProfilePanel` | `client.gui.nhanvien.KhungHoSoNhanVien` |
| `client.ui.PhieuNhapForm` | `client.gui.nhapkho.ManHinhPhieuNhap` |
| `client.ui.ProductImagePanel` | `client.gui.sanpham.KhungAnhSanPham` |
| `client.ui.ProductTableSupport` | `client.gui.components.TienIchBangSanPham` |
| `client.ui.QuanLySanPhamPanel` | `client.gui.sanpham.KhungQuanLySanPham` |
| `client.ui.SanPhamDetailForm` | `client.gui.sanpham.HopThoaiChiTietSanPham` |
| `client.ui.SanPhamDetailPanel` | `client.gui.sanpham.KhungChiTietSanPham` |
| `client.ui.SanPhamEditorPanel` | `client.gui.sanpham.KhungChinhSuaSanPham` |
| `client.ui.SanPhamPanel` | `client.gui.sanpham.KhungSanPham` |
| `client.ui.ThongKeForm` | `client.gui.thongke.ManHinhThongKe` |
| `client.ui.ThongTinCaNhanForm` | `client.gui.hethong.ManHinhThongTinCaNhan` |
| `client.ui.UiTask` | `client.gui.components.TacVuGiaoDien` |
| `client.ui.UiTheme` | `client.gui.components.ChuDeGiaoDien` |
| `client.util.ImageLoader` | `client.utils.TaiAnh` |
| `common.config.ApplicationConfig` | `common.config.CauHinhUngDung` |
| `common.dto.AnhSanPhamDTO` | `common.dto.sanpham.DuLieuAnhSanPham` |
| `common.dto.BienTheDTO` | `common.dto.sanpham.DuLieuBienThe` |
| `common.dto.ChiTietHoaDonDTO` | `common.dto.hoadon.DuLieuChiTietHoaDon` |
| `common.dto.ChiTietPhieuNhapDTO` | `common.dto.phieunhap.DuLieuChiTietPhieuNhap` |
| `common.dto.DanhMucSaveRequest` | `common.dto.danhmuc.YeuCauLuuDanhMuc` |
| `common.dto.DieuChinhTonKhoRequest` | `common.dto.kho.YeuCauDieuChinhTonKho` |
| `common.dto.DoiMatKhauRequest` | `common.dto.taikhoan.YeuCauDoiMatKhau` |
| `common.dto.HoaDonCreateRequest` | `common.dto.hoadon.YeuCauTaoHoaDon` |
| `common.dto.HoaDonDetailDTO` | `common.dto.hoadon.DuLieuDayDuHoaDon` |
| `common.dto.HoaDonDTO` | `common.dto.hoadon.DuLieuHoaDon` |
| `common.dto.HoaDonKhachHangRequest` | `common.dto.hoadon.YeuCauGanKhachHangHoaDon` |
| `common.dto.HoaDonLineRequest` | `common.dto.hoadon.YeuCauDongHoaDon` |
| `common.dto.HoaDonSearchRequest` | `common.dto.hoadon.YeuCauTraCuuHoaDon` |
| `common.dto.HoaDonSearchResult` | `common.dto.hoadon.KetQuaTraCuuHoaDon` |
| `common.dto.HoaDonThanhToanRequest` | `common.dto.hoadon.YeuCauThanhToanHoaDon` |
| `common.dto.HoaDonVersionRequest` | `common.dto.hoadon.YeuCauPhienBanHoaDon` |
| `common.dto.KhachHangDetailRequest` | `common.dto.khachhang.YeuCauChiTietKhachHang` |
| `common.dto.KhachHangDTO` | `common.dto.khachhang.DuLieuKhachHang` |
| `common.dto.KhachHangSaveRequest` | `common.dto.khachhang.YeuCauLuuKhachHang` |
| `common.dto.KhachHangSearchRequest` | `common.dto.khachhang.YeuCauTraCuuKhachHang` |
| `common.dto.KhachHangSearchResult` | `common.dto.khachhang.KetQuaTraCuuKhachHang` |
| `common.dto.KhachHangStatusRequest` | `common.dto.khachhang.YeuCauTrangThaiKhachHang` |
| `common.dto.KhuyenMaiDetailRequest` | `common.dto.khuyenmai.YeuCauChiTietKhuyenMai` |
| `common.dto.KhuyenMaiDTO` | `common.dto.khuyenmai.DuLieuKhuyenMai` |
| `common.dto.KhuyenMaiSaveRequest` | `common.dto.khuyenmai.YeuCauLuuKhuyenMai` |
| `common.dto.KhuyenMaiSearchRequest` | `common.dto.khuyenmai.YeuCauTraCuuKhuyenMai` |
| `common.dto.KhuyenMaiSearchResult` | `common.dto.khuyenmai.KetQuaTraCuuKhuyenMai` |
| `common.dto.KhuyenMaiStatusRequest` | `common.dto.khuyenmai.YeuCauTrangThaiKhuyenMai` |
| `common.dto.LichSuKhoDTO` | `common.dto.kho.DuLieuLichSuKho` |
| `common.dto.LichSuKhoSearchRequest` | `common.dto.kho.YeuCauTraCuuLichSuKho` |
| `common.dto.LichSuKhoSearchResult` | `common.dto.kho.KetQuaTraCuuLichSuKho` |
| `common.dto.LoaiSanPhamDTO` | `common.dto.danhmuc.DuLieuLoaiSanPham` |
| `common.dto.LoginRequest` | `common.dto.taikhoan.YeuCauDangNhap` |
| `common.dto.LoginResponse` | `common.dto.taikhoan.KetQuaDangNhap` |
| `common.dto.NhaCungCapDTO` | `common.dto.nhacungcap.DuLieuNhaCungCap` |
| `common.dto.NhaCungCapSaveRequest` | `common.dto.nhacungcap.YeuCauLuuNhaCungCap` |
| `common.dto.NhaCungCapSearchRequest` | `common.dto.nhacungcap.YeuCauTraCuuNhaCungCap` |
| `common.dto.NhaCungCapSearchResult` | `common.dto.nhacungcap.KetQuaTraCuuNhaCungCap` |
| `common.dto.NhaCungCapStatusRequest` | `common.dto.nhacungcap.YeuCauTrangThaiNhaCungCap` |
| `common.dto.NhanVienCreateRequest` | `common.dto.nhanvien.YeuCauTaoNhanVien` |
| `common.dto.NhanVienDetailRequest` | `common.dto.nhanvien.YeuCauChiTietNhanVien` |
| `common.dto.NhanVienDTO` | `common.dto.nhanvien.DuLieuNhanVien` |
| `common.dto.NhanVienRecordDTO` | `common.dto.nhanvien.DuLieuHoSoNhanVien` |
| `common.dto.NhanVienSaveRequest` | `common.dto.nhanvien.YeuCauLuuNhanVien` |
| `common.dto.NhanVienSearchRequest` | `common.dto.nhanvien.YeuCauTraCuuNhanVien` |
| `common.dto.NhanVienSearchResult` | `common.dto.nhanvien.KetQuaTraCuuNhanVien` |
| `common.dto.NhanVienStatusRequest` | `common.dto.nhanvien.YeuCauTrangThaiNhanVien` |
| `common.dto.PhieuNhapConfirmRequest` | `common.dto.phieunhap.YeuCauXacNhanPhieuNhap` |
| `common.dto.PhieuNhapCreateRequest` | `common.dto.phieunhap.YeuCauTaoPhieuNhap` |
| `common.dto.PhieuNhapDetailDTO` | `common.dto.phieunhap.DuLieuDayDuPhieuNhap` |
| `common.dto.PhieuNhapDTO` | `common.dto.phieunhap.DuLieuPhieuNhap` |
| `common.dto.PhieuNhapLineRequest` | `common.dto.phieunhap.YeuCauDongPhieuNhap` |
| `common.dto.PhieuNhapSearchRequest` | `common.dto.phieunhap.YeuCauTraCuuPhieuNhap` |
| `common.dto.PhieuNhapSearchResult` | `common.dto.phieunhap.KetQuaTraCuuPhieuNhap` |
| `common.dto.PhieuNhapVersionRequest` | `common.dto.phieunhap.YeuCauPhienBanPhieuNhap` |
| `common.dto.SanPhamDetailDTO` | `common.dto.sanpham.DuLieuDayDuSanPham` |
| `common.dto.SanPhamDetailRequest` | `common.dto.sanpham.YeuCauChiTietSanPham` |
| `common.dto.SanPhamDTO` | `common.dto.sanpham.DuLieuSanPham` |
| `common.dto.SanPhamSaveRequest` | `common.dto.sanpham.YeuCauLuuSanPham` |
| `common.dto.SanPhamSearchRequest` | `common.dto.sanpham.YeuCauTraCuuSanPham` |
| `common.dto.SanPhamSearchResult` | `common.dto.sanpham.KetQuaTraCuuSanPham` |
| `common.dto.TaiKhoanCreateRequest` | `common.dto.taikhoan.YeuCauTaoTaiKhoan` |
| `common.dto.TaiKhoanDTO` | `common.dto.taikhoan.DuLieuTaiKhoan` |
| `common.dto.TaiKhoanRoleRequest` | `common.dto.taikhoan.YeuCauVaiTroTaiKhoan` |
| `common.dto.TaiKhoanStatusRequest` | `common.dto.taikhoan.YeuCauTrangThaiTaiKhoan` |
| `common.dto.ThongKeDoanhThuDongDTO` | `common.dto.thongke.DuLieuThongKeDoanhThuDong` |
| `common.dto.ThongKeDoanhThuDTO` | `common.dto.thongke.DuLieuThongKeDoanhThu` |
| `common.dto.ThongKeDoanhThuRequest` | `common.dto.thongke.YeuCauThongKeDoanhThu` |
| `common.dto.ThueDTO` | `common.dto.danhmuc.DuLieuThue` |
| `common.dto.ThuongHieuDTO` | `common.dto.danhmuc.DuLieuThuongHieu` |
| `common.enums.NguonBienDongKho` | `common.enums.NguonBienDongKho` |
| `common.enums.NguonDuLieuHoaDon` | `common.enums.NguonDuLieuHoaDon` |
| `common.enums.NhomThongKe` | `common.enums.NhomThongKe` |
| `common.enums.PhuongThucThanhToan` | `common.enums.PhuongThucThanhToan` |
| `common.enums.TrangThaiHieuLucKhuyenMai` | `common.enums.TrangThaiHieuLucKhuyenMai` |
| `common.enums.TrangThaiHoaDon` | `common.enums.TrangThaiHoaDon` |
| `common.enums.TrangThaiPhieuNhap` | `common.enums.TrangThaiPhieuNhap` |
| `common.enums.VaiTro` | `common.enums.VaiTro` |
| `common.protocol.Action` | `common.protocol.HanhDong` |
| `common.protocol.ErrorCode` | `common.protocol.MaLoi` |
| `common.protocol.FrameIO` | `common.protocol.TruyenKhungTin` |
| `common.protocol.JsonCodec` | `common.protocol.BoMaHoaJson` |
| `common.protocol.Request` | `common.protocol.YeuCau` |
| `common.protocol.Response` | `common.protocol.PhanHoi` |
| `common.util.InvoiceIds` | `common.utils.SinhMaHoaDon` |
| `common.util.MoneyUtils` | `common.utils.TienIchTienTe` |
| `common.util.PropertiesLoader` | `common.utils.DocThuocTinh` |
| `server.AccountSetupMain` | `server.app.TaoTaiKhoanQuanLy` |
| `server.config.DatabaseConfig` | `server.config.CauHinhCoSoDuLieu` |
| `server.config.ServerConfig` | `server.config.ServerConfig` |
| `server.dispatcher.RequestDispatcher` | `server.dispatcher.DieuPhoiYeuCau` |
| `server.entity.AnhSanPham` | `server.entities.AnhSanPham` |
| `server.entity.BienThe` | `server.entities.BienThe` |
| `server.entity.ChiTietHoaDon` | `server.entities.ChiTietHoaDon` |
| `server.entity.ChiTietPhieuNhap` | `server.entities.ChiTietPhieuNhap` |
| `server.entity.HoaDon` | `server.entities.HoaDon` |
| `server.entity.KhachHang` | `server.entities.KhachHang` |
| `server.entity.KhuyenMai` | `server.entities.KhuyenMai` |
| `server.entity.LichSuKho` | `server.entities.LichSuKho` |
| `server.entity.LoaiSanPham` | `server.entities.LoaiSanPham` |
| `server.entity.NhaCungCap` | `server.entities.NhaCungCap` |
| `server.entity.NhanVien` | `server.entities.NhanVien` |
| `server.entity.PhieuNhap` | `server.entities.PhieuNhap` |
| `server.entity.PhieuNhapThaoTac` | `server.entities.PhieuNhapThaoTac` |
| `server.entity.SanPham` | `server.entities.SanPham` |
| `server.entity.TaiKhoan` | `server.entities.TaiKhoan` |
| `server.entity.Thue` | `server.entities.Thue` |
| `server.entity.ThuongHieu` | `server.entities.ThuongHieu` |
| `server.network.ClientHandler` | `server.network.XuLyKetNoi` |
| `server.network.SocketServer` | `server.network.MayChuTcp` |
| `server.persistence.JpaManager` | `server.utils.QuanLyJpa` |
| `server.repository.AnhSanPhamDao` | `server.repositories.LuuTruAnhSanPham` |
| `server.repository.BienTheDao` | `server.repositories.LuuTruBienThe` |
| `server.repository.ChiTietHoaDonDao` | `server.repositories.LuuTruChiTietHoaDon` |
| `server.repository.GenericDao` | `server.repositories.LuuTruCoBan` |
| `server.repository.HoaDonDao` | `server.repositories.LuuTruHoaDon` |
| `server.repository.JpaAnhSanPhamDao` | `server.repositories.impl.LuuTruAnhSanPhamJpa` |
| `server.repository.JpaBienTheDao` | `server.repositories.impl.LuuTruBienTheJpa` |
| `server.repository.JpaChiTietHoaDonDao` | `server.repositories.impl.LuuTruChiTietHoaDonJpa` |
| `server.repository.JpaGenericDao` | `server.repositories.impl.LuuTruCoBanJpa` |
| `server.repository.JpaHoaDonDao` | `server.repositories.impl.LuuTruHoaDonJpa` |
| `server.repository.JpaKhachHangDao` | `server.repositories.impl.LuuTruKhachHangJpa` |
| `server.repository.JpaKhuyenMaiDao` | `server.repositories.impl.LuuTruKhuyenMaiJpa` |
| `server.repository.JpaLichSuKhoDao` | `server.repositories.impl.LuuTruLichSuKhoJpa` |
| `server.repository.JpaLoaiSanPhamDao` | `server.repositories.impl.LuuTruLoaiSanPhamJpa` |
| `server.repository.JpaNhaCungCapDao` | `server.repositories.impl.LuuTruNhaCungCapJpa` |
| `server.repository.JpaNhanVienDao` | `server.repositories.impl.LuuTruNhanVienJpa` |
| `server.repository.JpaPhieuNhapDao` | `server.repositories.impl.LuuTruPhieuNhapJpa` |
| `server.repository.JpaSanPhamDao` | `server.repositories.impl.LuuTruSanPhamJpa` |
| `server.repository.JpaTaiKhoanDao` | `server.repositories.impl.LuuTruTaiKhoanJpa` |
| `server.repository.JpaThongKeDao` | `server.repositories.impl.LuuTruThongKeJpa` |
| `server.repository.JpaThuongHieuDao` | `server.repositories.impl.LuuTruThuongHieuJpa` |
| `server.repository.KhachHangDao` | `server.repositories.LuuTruKhachHang` |
| `server.repository.KhuyenMaiDao` | `server.repositories.LuuTruKhuyenMai` |
| `server.repository.LichSuKhoDao` | `server.repositories.LuuTruLichSuKho` |
| `server.repository.LoaiSanPhamDao` | `server.repositories.LuuTruLoaiSanPham` |
| `server.repository.NhaCungCapDao` | `server.repositories.LuuTruNhaCungCap` |
| `server.repository.NhanVienDao` | `server.repositories.LuuTruNhanVien` |
| `server.repository.PhieuNhapDao` | `server.repositories.LuuTruPhieuNhap` |
| `server.repository.SanPhamDao` | `server.repositories.LuuTruSanPham` |
| `server.repository.TaiKhoanDao` | `server.repositories.LuuTruTaiKhoan` |
| `server.repository.ThongKeDao` | `server.repositories.LuuTruThongKe` |
| `server.repository.ThuongHieuDao` | `server.repositories.LuuTruThuongHieu` |
| `server.security.PasswordHasher` | `server.security.BoBamMatKhau` |
| `server.security.Pbkdf2PasswordHasher` | `server.security.BoBamMatKhauPbkdf2` |
| `server.security.PermissionChecker` | `server.security.KiemTraPhanQuyen` |
| `server.ServerMain` | `server.app.ServerMain` |
| `server.service.AuthService` | `server.services.DichVuXacThuc` |
| `server.service.BusinessException` | `server.services.NgoaiLeNghiepVu` |
| `server.service.HoaDonService` | `server.services.DichVuHoaDon` |
| `server.service.KhachHangService` | `server.services.DichVuKhachHang` |
| `server.service.KhuyenMaiService` | `server.services.DichVuKhuyenMai` |
| `server.service.LichSuKhoService` | `server.services.DichVuLichSuKho` |
| `server.service.NhaCungCapService` | `server.services.DichVuNhaCungCap` |
| `server.service.NhanVienService` | `server.services.DichVuNhanVien` |
| `server.service.PhieuNhapService` | `server.services.DichVuPhieuNhap` |
| `server.service.QuanLySanPhamService` | `server.services.DichVuQuanLySanPham` |
| `server.service.SanPhamService` | `server.services.DichVuSanPham` |
| `server.service.ThongKeService` | `server.services.DichVuThongKe` |
| `server.session.SessionManager` | `server.session.QuanLyPhienDangNhap` |
| `ApplicationConfigTest` | `kiemthu.KiemThuCauHinhUngDung` |
| `AssetSmokeCheck` | `kiemthu.KiemTraTaiNguyenAnh` |
| `AuthIntegrationTest` | `kiemthu.KiemThuTichHopXacThuc` |
| `CatalogManagementIntegrationTest` | `kiemthu.KiemThuTichHopDanhMuc` |
| `CatalogManagementUiTest` | `kiemthu.KiemThuGiaoDienDanhMuc` |
| `CatalogTestSupport` | `kiemthu.HoTroKiemThuDanhMuc` |
| `client.network.SocketClientIdleIntegrationTest` | `client.network.KiemThuKetNoiKhongHoatDong` |
| `DependencySmokeTest` | `kiemthu.KiemThuThuVien` |
| `FlatLafUiTest` | `kiemthu.KiemThuGiaoDienChung` |
| `InvoiceIntegrationTest` | `kiemthu.KiemThuTichHopHoaDon` |
| `InvoiceUiTest` | `kiemthu.KiemThuGiaoDienHoaDon` |
| `KhachHangManagementIntegrationTest` | `kiemthu.KiemThuTichHopKhachHang` |
| `KhuyenMaiManagementIntegrationTest` | `kiemthu.KiemThuTichHopKhuyenMai` |
| `NhanVienManagementIntegrationTest` | `kiemthu.KiemThuTichHopNhanVien` |
| `NormalizedCatalogTest` | `kiemthu.KiemThuDuLieuSanPham` |
| `PhieuNhapIntegrationTest` | `kiemthu.KiemThuTichHopPhieuNhap` |
| `ProductIntegrationTest` | `kiemthu.KiemThuTichHopSanPham` |
| `ProductUiTest` | `kiemthu.KiemThuGiaoDienSanPham` |
| `SecurityProtocolTest` | `kiemthu.KiemThuBaoMatGiaoThuc` |
| `ThongKeIntegrationTest` | `kiemthu.KiemThuTichHopThongKe` |
| `vn.edu.iuh.quanao.ScaffoldSmokeTest` | `kiemthu.KiemTraNenTang` |

## File Java đã bỏ

Các lớp sau không có tham chiếu từ mã chạy hoặc bộ kiểm thử; là khung rỗng hoặc cửa sổ thay thế đã bỏ dùng.

- `src/main/java/client/api/ThueApi.java`
- `src/main/java/client/controller/BienTheController.java`
- `src/main/java/client/controller/HoaDonController.java`
- `src/main/java/client/controller/LoaiSanPhamController.java`
- `src/main/java/client/controller/MainController.java`
- `src/main/java/client/controller/ThuongHieuController.java`
- `src/main/java/client/ui/BanHangForm.java`
- `src/main/java/client/ui/BienTheForm.java`
- `src/main/java/client/ui/HoaDonForm.java`
- `src/main/java/client/ui/LoaiSanPhamForm.java`
- `src/main/java/client/ui/SanPhamForm.java`
- `src/main/java/client/ui/ThuongHieuForm.java`
- `src/main/java/server/repository/ThueDao.java`
- `src/main/java/server/service/BienTheService.java`
- `src/main/java/server/service/LoaiSanPhamService.java`
- `src/main/java/server/service/ThueService.java`
- `src/main/java/server/service/ThuongHieuService.java`
