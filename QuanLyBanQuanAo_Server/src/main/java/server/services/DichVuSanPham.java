package server.services;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import common.dto.danhmuc.DuLieuLoaiSanPham;
import common.dto.danhmuc.DuLieuThuongHieu;
import common.dto.sanpham.DuLieuAnhSanPham;
import common.dto.sanpham.DuLieuBienThe;
import common.dto.sanpham.DuLieuDayDuSanPham;
import common.dto.sanpham.DuLieuSanPham;
import common.dto.sanpham.KetQuaTraCuuSanPham;
import common.dto.sanpham.YeuCauTraCuuSanPham;
import common.enums.VaiTro;
import common.protocol.MaLoi;
import server.entities.AnhSanPham;
import server.entities.BienThe;
import server.entities.LoaiSanPham;
import server.entities.SanPham;
import server.entities.TaiKhoan;
import server.entities.ThuongHieu;
import server.repositories.impl.LuuTruAnhSanPhamJpa;
import server.repositories.impl.LuuTruBienTheJpa;
import server.repositories.impl.LuuTruLoaiSanPhamJpa;
import server.repositories.impl.LuuTruSanPhamJpa;
import server.repositories.impl.LuuTruThuongHieuJpa;
import server.utils.QuanLyJpa;

/** Tra cứu sản phẩm và cung cấp dịch vụ quản lý; client chỉ nhận DTO. */
public final class DichVuSanPham {
    private final QuanLyJpa jpa;
    private final DichVuXacThuc auth;

    public DichVuSanPham(QuanLyJpa jpa, DichVuXacThuc auth) {
        this.jpa = jpa;
        this.auth = auth;
    }

    public DichVuQuanLySanPham management() {
        return new DichVuQuanLySanPham(jpa, auth);
    }

    public KetQuaTraCuuSanPham search(String token, YeuCauTraCuuSanPham input) {
        return jpa.transaction(em -> {
            TaiKhoan account = auth.requireAuthenticated(em, token);
            YeuCauTraCuuSanPham request = normalize(input);
            if (request.getIncludeInactive() && account.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY) {
                throw new NgoaiLeNghiepVu(MaLoi.FORBIDDEN,
                        "Chỉ quản lý được xem sản phẩm/biến thể ngừng bán.");
            }
            LuuTruSanPhamJpa dao = new LuuTruSanPhamJpa(em);
            List<SanPham> products = dao.search(request);
            Map<String, Long> stock = dao
                    .activeStock(products.stream().map(SanPham::getMaSP).collect(Collectors.toList()));
            KetQuaTraCuuSanPham result = new KetQuaTraCuuSanPham();
            result.setTotal(dao.count(request));
            result.setPage(request.getPage());
            result.setPageSize(request.getPageSize());
            result.setItems(products.stream().map(p -> productDto(p, stock.getOrDefault(p.getMaSP(), 0L)))
                    .collect(Collectors.toList()));
            return result;
        });
    }

    public DuLieuDayDuSanPham detail(String token, String id) {
        return jpa.transaction(em -> {
            TaiKhoan account = auth.requireAuthenticated(em, token);
            String code = text(id, 20, "Mã sản phẩm");
            if (code == null) {
                throw invalid("Mã sản phẩm không được trống.");
            }
            boolean manager = account.getVaiTro() == VaiTro.NHAN_VIEN_QUAN_LY;
            LuuTruSanPhamJpa dao = new LuuTruSanPhamJpa(em);
            SanPham product = dao.findById(code)
                    .orElseThrow(() -> new NgoaiLeNghiepVu(MaLoi.NOT_FOUND, "Không tìm thấy sản phẩm."));
            if (!manager && !product.getTrangThai()) {
                throw new NgoaiLeNghiepVu(MaLoi.NOT_FOUND, "Không tìm thấy sản phẩm đang bán.");
            }
            List<BienThe> variants = new LuuTruBienTheJpa(em).byProduct(code, manager);
            List<AnhSanPham> images = new LuuTruAnhSanPhamJpa(em).byProduct(code, manager);
            if (variants.size() > 500 || images.size() > 500) {
                throw invalid("Sản phẩm có quá nhiều biến thể/ảnh. Cần phân trang chi tiết.");
            }
            DuLieuDayDuSanPham result = new DuLieuDayDuSanPham();
            result.setSanPham(productDto(product,
                    dao.activeStock(Collections.singletonList(code)).getOrDefault(code, 0L)));
            result.setBienThe(variants.stream().map(DichVuSanPham::variantDto).collect(Collectors.toList()));
            result.setHinhAnh(images.stream().map(DichVuSanPham::imageDto).collect(Collectors.toList()));
            return result;
        });
    }

    public List<DuLieuLoaiSanPham> categories(String token) {
        return jpa.transaction(em -> {
            auth.requireAuthenticated(em, token);
            List<LoaiSanPham> rows = new LuuTruLoaiSanPhamJpa(em).ordered();
            if (rows.size() > 1000) {
                throw invalid("Danh mục quá lớn; cần phân trang.");
            }
            return rows.stream().map(row -> {
                DuLieuLoaiSanPham dto = new DuLieuLoaiSanPham();
                dto.setMaLoai(row.getMaLoai());
                dto.setTenLoai(row.getTenLoai());
                return dto;
            }).collect(Collectors.toList());
        });
    }

    public List<DuLieuThuongHieu> brands(String token) {
        return jpa.transaction(em -> {
            auth.requireAuthenticated(em, token);
            List<ThuongHieu> rows = new LuuTruThuongHieuJpa(em).ordered();
            if (rows.size() > 1000) {
                throw invalid("Danh mục quá lớn; cần phân trang.");
            }
            return rows.stream().map(row -> {
                DuLieuThuongHieu dto = new DuLieuThuongHieu();
                dto.setMaThuongHieu(row.getMaThuongHieu());
                dto.setTenThuongHieu(row.getTenThuongHieu());
                return dto;
            }).collect(Collectors.toList());
        });
    }

    private static YeuCauTraCuuSanPham normalize(YeuCauTraCuuSanPham input) {
        if (input == null) {
            throw invalid("Thiếu điều kiện tra cứu.");
        }
        if (input.getPage() < 0 || input.getPage() > 1000000 || input.getPageSize() < 1
                || input.getPageSize() > 100) {
            throw invalid("Trang phải từ 0 đến 1.000.000; số dòng mỗi trang từ 1 đến 100.");
        }
        YeuCauTraCuuSanPham result = new YeuCauTraCuuSanPham();
        result.setTenSP(text(input.getTenSP(), 200, "Tên sản phẩm"));
        result.setMaLoai(text(input.getMaLoai(), 20, "Mã loại"));
        result.setMaThuongHieu(text(input.getMaThuongHieu(), 20, "Mã thương hiệu"));
        result.setMauSac(text(input.getMauSac(), 50, "Màu"));
        result.setKichCo(text(input.getKichCo(), 20, "Size"));
        result.setPage(input.getPage());
        result.setPageSize(input.getPageSize());
        result.setIncludeInactive(input.getIncludeInactive());
        return result;
    }

    private static String text(String value, int max, String label) {
        if (value == null) {
            return null;
        }
        if (value.length() > max) {
            throw invalid(label + " quá dài.");
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static NgoaiLeNghiepVu invalid(String message) {
        return new NgoaiLeNghiepVu(MaLoi.VALIDATION_ERROR, message);
    }

    private static DuLieuSanPham productDto(SanPham p, long stock) {
        DuLieuSanPham dto = new DuLieuSanPham();
        dto.setMaSP(p.getMaSP());
        dto.setTenSP(p.getTenSP());
        dto.setGiaBan(p.getGiaBan());
        dto.setMaLoai(p.getLoaiSanPham().getMaLoai());
        dto.setTenLoai(p.getLoaiSanPham().getTenLoai());
        dto.setMaThuongHieu(p.getThuongHieu().getMaThuongHieu());
        dto.setTenThuongHieu(p.getThuongHieu().getTenThuongHieu());
        dto.setTrangThai(p.getTrangThai());
        dto.setTongTon(stock);
        return dto;
    }

    private static DuLieuBienThe variantDto(BienThe b) {
        DuLieuBienThe dto = new DuLieuBienThe();
        dto.setMaBienThe(b.getMaBienThe());
        dto.setMaSP(b.getSanPham().getMaSP());
        dto.setMauSac(b.getMauSac());
        dto.setKichCo(b.getKichCo());
        dto.setSoLuongTon(b.getSoLuongTon());
        dto.setTrangThai(b.getTrangThai());
        dto.setPhienBan(b.getPhienBan());
        return dto;
    }

    private static DuLieuAnhSanPham imageDto(AnhSanPham a) {
        DuLieuAnhSanPham dto = new DuLieuAnhSanPham();
        dto.setMaAnh(a.getMaAnh());
        dto.setMaSP(a.getSanPham().getMaSP());
        dto.setMauSac(a.getMauSac());
        dto.setDuongDan(a.getDuongDan());
        dto.setThuTu(a.getThuTu());
        dto.setAnhChinh(a.getAnhChinh());
        return dto;
    }
}
