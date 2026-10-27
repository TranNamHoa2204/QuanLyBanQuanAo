package server.service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;
import common.dto.*;
import common.enums.VaiTro;
import common.protocol.ErrorCode;
import server.entity.*;
import server.persistence.JpaManager;
import server.repository.*;

/** Tra cứu sản phẩm và cung cấp dịch vụ quản lý; client chỉ nhận DTO. */
public final class SanPhamService {
    private final JpaManager jpa;
    private final AuthService auth;

    public SanPhamService(JpaManager jpa, AuthService auth) {
        this.jpa = jpa;
        this.auth = auth;
    }

    public QuanLySanPhamService management() {
        return new QuanLySanPhamService(jpa, auth);
    }

    public SanPhamSearchResult search(String token, SanPhamSearchRequest input) {
        return jpa.transaction(em -> {
            TaiKhoan account = auth.requireAuthenticated(em, token);
            SanPhamSearchRequest request = normalize(input);
            if (request.getIncludeInactive() && account.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ quản lý được xem sản phẩm/biến thể ngừng bán.");
            }
            JpaSanPhamDao dao = new JpaSanPhamDao(em);
            List<SanPham> products = dao.search(request);
            Map<String, Long> stock = dao
                    .activeStock(products.stream().map(SanPham::getMaSP).collect(Collectors.toList()));
            SanPhamSearchResult result = new SanPhamSearchResult();
            result.setTotal(dao.count(request));
            result.setPage(request.getPage());
            result.setPageSize(request.getPageSize());
            result.setItems(products.stream().map(p -> productDto(p, stock.getOrDefault(p.getMaSP(), 0L)))
                    .collect(Collectors.toList()));
            return result;
        });
    }

    public SanPhamDetailDTO detail(String token, String id) {
        return jpa.transaction(em -> {
            TaiKhoan account = auth.requireAuthenticated(em, token);
            String code = text(id, 20, "Mã sản phẩm");
            if (code == null) {
                throw invalid("Mã sản phẩm không được trống.");
            }
            boolean manager = account.getVaiTro() == VaiTro.NHAN_VIEN_QUAN_LY;
            JpaSanPhamDao dao = new JpaSanPhamDao(em);
            SanPham product = dao.findById(code)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy sản phẩm."));
            if (!manager && !product.getTrangThai()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy sản phẩm đang bán.");
            }
            List<BienThe> variants = new JpaBienTheDao(em).byProduct(code, manager);
            List<AnhSanPham> images = new JpaAnhSanPhamDao(em).byProduct(code, manager);
            if (variants.size() > 500 || images.size() > 500) {
                throw invalid("Sản phẩm có quá nhiều biến thể/ảnh. Cần phân trang chi tiết.");
            }
            SanPhamDetailDTO result = new SanPhamDetailDTO();
            result.setSanPham(
                    productDto(product, dao.activeStock(Collections.singletonList(code)).getOrDefault(code, 0L)));
            result.setBienThe(variants.stream().map(SanPhamService::variantDto).collect(Collectors.toList()));
            result.setHinhAnh(images.stream().map(SanPhamService::imageDto).collect(Collectors.toList()));
            return result;
        });
    }

    public List<LoaiSanPhamDTO> categories(String token) {
        return jpa.transaction(em -> {
            auth.requireAuthenticated(em, token);
            List<LoaiSanPham> rows = new JpaLoaiSanPhamDao(em).ordered();
            if (rows.size() > 1000) {
                throw invalid("Danh mục quá lớn; cần phân trang.");
            }
            return rows.stream().map(row -> {
                LoaiSanPhamDTO dto = new LoaiSanPhamDTO();
                dto.setMaLoai(row.getMaLoai());
                dto.setTenLoai(row.getTenLoai());
                return dto;
            }).collect(Collectors.toList());
        });
    }

    public List<ThuongHieuDTO> brands(String token) {
        return jpa.transaction(em -> {
            auth.requireAuthenticated(em, token);
            List<ThuongHieu> rows = new JpaThuongHieuDao(em).ordered();
            if (rows.size() > 1000) {
                throw invalid("Danh mục quá lớn; cần phân trang.");
            }
            return rows.stream().map(row -> {
                ThuongHieuDTO dto = new ThuongHieuDTO();
                dto.setMaThuongHieu(row.getMaThuongHieu());
                dto.setTenThuongHieu(row.getTenThuongHieu());
                return dto;
            }).collect(Collectors.toList());
        });
    }

    private static SanPhamSearchRequest normalize(SanPhamSearchRequest input) {
        if (input == null) {
            throw invalid("Thiếu điều kiện tra cứu.");
        }
        if (input.getPage() < 0 || input.getPage() > 1000000 || input.getPageSize() < 1 || input.getPageSize() > 100) {
            throw invalid("Trang phải từ 0 đến 1.000.000; số dòng mỗi trang từ 1 đến 100.");
        }
        SanPhamSearchRequest result = new SanPhamSearchRequest();
        result.setMaSP(text(input.getMaSP(), 20, "Mã sản phẩm"));
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

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message);
    }

    private static SanPhamDTO productDto(SanPham p, long stock) {
        SanPhamDTO dto = new SanPhamDTO();
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

    private static BienTheDTO variantDto(BienThe b) {
        BienTheDTO dto = new BienTheDTO();
        dto.setMaBienThe(b.getMaBienThe());
        dto.setMaSP(b.getSanPham().getMaSP());
        dto.setMauSac(b.getMauSac());
        dto.setKichCo(b.getKichCo());
        dto.setSoLuongTon(b.getSoLuongTon());
        dto.setTrangThai(b.getTrangThai());
        dto.setPhienBan(b.getPhienBan());
        return dto;
    }

    private static AnhSanPhamDTO imageDto(AnhSanPham a) {
        AnhSanPhamDTO dto = new AnhSanPhamDTO();
        dto.setMaAnh(a.getMaAnh());
        dto.setMaSP(a.getSanPham().getMaSP());
        dto.setMauSac(a.getMauSac());
        dto.setDuongDan(a.getDuongDan());
        dto.setThuTu(a.getThuTu());
        dto.setAnhChinh(a.getAnhChinh());
        return dto;
    }
}
