package server.services;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import common.dto.danhmuc.YeuCauLuuDanhMuc;
import common.dto.sanpham.DuLieuBienThe;
import common.dto.sanpham.DuLieuSanPham;
import common.dto.sanpham.YeuCauLuuSanPham;
import common.enums.NguonBienDongKho;
import common.enums.VaiTro;
import common.protocol.MaLoi;
import server.entities.BienThe;
import server.entities.LoaiSanPham;
import server.entities.SanPham;
import server.entities.TaiKhoan;
import server.entities.ThuongHieu;
import server.repositories.impl.LuuTruLoaiSanPhamJpa;
import server.repositories.impl.LuuTruThuongHieuJpa;
import server.utils.QuanLyJpa;

/** Ghi danh mục không xóa cứng; service sở hữu transaction và kiểm tra quyền. */
public final class DichVuQuanLySanPham {
    private final QuanLyJpa jpa;
    private final DichVuXacThuc auth;

    public DichVuQuanLySanPham(QuanLyJpa jpa, DichVuXacThuc auth) {
        this.jpa = jpa;
        this.auth = auth;
    }

    public String category(String token, YeuCauLuuDanhMuc r, boolean create) {
        return jpa.transaction(em -> {
            manager(em, token);
            if (r == null) {
                throw invalid("Thiếu dữ liệu loại.");
            }
            String id = code(r.getCode()), name = text(r.getName(), 100);
            LoaiSanPham entity = em.find(LoaiSanPham.class, id,
                    create ? LockModeType.NONE : LockModeType.PESSIMISTIC_WRITE);
            if (create) {
                if (entity != null) {
                    throw invalid("Mã loại đã tồn tại.");
                }
                if (new LuuTruLoaiSanPhamJpa(em).ordered().size() >= 1000) {
                    throw invalid("Danh mục tối đa 1.000 loại.");
                }
                entity = new LoaiSanPham();
                entity.setMaLoai(id);
            } else {
                if (entity == null) {
                    throw missing();
                }
                expected(r.getExpectedName(), entity.getTenLoai());
            }
            uniqueName(em, "LoaiSanPham", "maLoai", "tenLoai", id, name);
            entity.setTenLoai(name);
            if (create) {
                em.persist(entity);
            }
            em.flush();
            return id;
        });
    }

    public String brand(String token, YeuCauLuuDanhMuc r, boolean create) {
        return jpa.transaction(em -> {
            manager(em, token);
            if (r == null) {
                throw invalid("Thiếu dữ liệu thương hiệu.");
            }
            String id = code(r.getCode()), name = text(r.getName(), 100);
            ThuongHieu entity = em.find(ThuongHieu.class, id,
                    create ? LockModeType.NONE : LockModeType.PESSIMISTIC_WRITE);
            if (create) {
                if (entity != null) {
                    throw invalid("Mã thương hiệu đã tồn tại.");
                }
                if (new LuuTruThuongHieuJpa(em).ordered().size() >= 1000) {
                    throw invalid("Danh mục tối đa 1.000 thương hiệu.");
                }
                entity = new ThuongHieu();
                entity.setMaThuongHieu(id);
            } else {
                if (entity == null) {
                    throw missing();
                }
                expected(r.getExpectedName(), entity.getTenThuongHieu());
            }
            uniqueName(em, "ThuongHieu", "maThuongHieu", "tenThuongHieu", id, name);
            entity.setTenThuongHieu(name);
            if (create) {
                em.persist(entity);
            }
            em.flush();
            return id;
        });
    }

    public String product(String token, YeuCauLuuSanPham r, int mode) {
        return jpa.transaction(em -> {
            manager(em, token);
            if (r == null || r.getValue() == null || mode < 0 || mode > 2) {
                throw invalid("Thiếu dữ liệu sản phẩm.");
            }
            DuLieuSanPham value = r.getValue();
            String id = code(value.getMaSP());
            SanPham entity = em.find(SanPham.class, id,
                    mode == 0 ? LockModeType.NONE : LockModeType.PESSIMISTIC_WRITE);
            if (mode == 0) {
                if (entity != null) {
                    throw invalid("Mã sản phẩm đã tồn tại.");
                }
                entity = new SanPham();
                entity.setMaSP(id);
            } else {
                if (entity == null) {
                    throw missing();
                }
                DuLieuSanPham e = r.getExpected();
                if (e == null || !Objects.equals(e.getMaSP(), id)
                        || !Objects.equals(e.getTenSP(), entity.getTenSP())
                        || !Objects.equals(e.getMaLoai(), entity.getLoaiSanPham().getMaLoai())
                        || !Objects.equals(e.getMaThuongHieu(), entity.getThuongHieu().getMaThuongHieu())
                        || e.getGiaBan() == null || e.getGiaBan().compareTo(entity.getGiaBan()) != 0
                        || e.getTrangThai() != entity.getTrangThai()) {
                    throw conflict();
                }
            }
            if (mode == 2) {
                entity.setTrangThai(false);
            } else {
                String name = text(value.getTenSP(), 200);
                BigDecimal price = value.getGiaBan();
                if (price == null || price.signum() < 0
                        || price.compareTo(new BigDecimal("9999999999999999.99")) > 0
                        || price.stripTrailingZeros().scale() > 2) {
                    throw invalid("Giá không âm, tối đa 2 chữ số thập phân và thuộc DECIMAL(18,2).");
                }
                LoaiSanPham category = em.find(LoaiSanPham.class, code(value.getMaLoai()));
                ThuongHieu brand = em.find(ThuongHieu.class, code(value.getMaThuongHieu()));
                if (category == null || brand == null) {
                    throw invalid("Loại hoặc thương hiệu không tồn tại.");
                }
                entity.setTenSP(name);
                entity.setGiaBan(price.setScale(2));
                entity.setLoaiSanPham(category);
                entity.setThuongHieu(brand);
                entity.setTrangThai(value.getTrangThai());
            }
            if (mode == 0) {
                em.persist(entity);
            }
            em.flush();
            return id;
        });
    }

    public DuLieuBienThe variant(String token, DuLieuBienThe r, boolean create) {
        return jpa.transaction(em -> {
            TaiKhoan actor = manager(em, token);
            if (r == null) {
                throw invalid("Thiếu biến thể.");
            }
            String id = code(r.getMaBienThe()), productId = code(r.getMaSP());
            String color = text(r.getMauSac(), 50), size = text(r.getKichCo(), 20);
            if (r.getSoLuongTon() < 0) {
                throw invalid("Tồn kho không được âm.");
            }
            BienThe entity;
            if (create) {
                // Cùng thứ tự với thanh toán: khóa sản phẩm trước biến thể.
                SanPham parent = em.find(SanPham.class, productId, LockModeType.PESSIMISTIC_WRITE);
                if (parent == null || !parent.getTrangThai()) {
                    throw invalid("Sản phẩm không tồn tại hoặc ngừng bán.");
                }
                if (em.find(BienThe.class, id) != null) {
                    throw invalid("Mã biến thể đã tồn tại.");
                }
                long count = em
                        .createQuery("select count(b) from BienThe b where b.sanPham.maSP = :p", Long.class)
                        .setParameter("p", productId).getSingleResult();
                if (count >= 500) {
                    throw invalid("Một sản phẩm tối đa 500 biến thể.");
                }
                long duplicate = em.createQuery(
                        "select count(b) from BienThe b where b.sanPham.maSP = :p and lower(b.mauSac) = :c and lower(b.kichCo) = :s",
                        Long.class).setParameter("p", productId)
                        .setParameter("c", color.toLowerCase(java.util.Locale.ROOT))
                        .setParameter("s", size.toLowerCase(java.util.Locale.ROOT)).getSingleResult();
                if (duplicate > 0) {
                    throw invalid("Màu và size đã tồn tại cho sản phẩm này (kể cả biến thể ngừng bán).");
                }
                entity = new BienThe();
                entity.setMaBienThe(id);
                entity.setSanPham(parent);
                entity.setMauSac(color);
                entity.setKichCo(size);
            } else {
                entity = em.find(BienThe.class, id, LockModeType.PESSIMISTIC_WRITE);
                if (entity == null) {
                    throw missing();
                }
                if (r.getPhienBan() == null || !r.getPhienBan().equals(entity.getPhienBan())) {
                    throw conflict();
                }
                if (!Objects.equals(productId, entity.getSanPham().getMaSP())
                        || !Objects.equals(color, entity.getMauSac())
                        || !Objects.equals(size, entity.getKichCo())) {
                    throw invalid(
                            "Không chuyển sản phẩm/đổi màu hoặc size của biến thể đã tạo. Hãy thêm biến thể mới.");
                }
                if (r.getSoLuongTon() != entity.getSoLuongTon()) {
                    throw invalid(
                            "Không sửa tồn qua cập nhật biến thể. Dùng chức năng điều chỉnh tồn có lý do và version.");
                }
            }
            entity.setSoLuongTon(r.getSoLuongTon());
            entity.setTrangThai(r.getTrangThai());
            if (create) {
                em.persist(entity);
                DichVuLichSuKho.record(em, entity, actor.getNhanVien(), actor.getNhanVien().getHoTen(),
                        LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).withNano(0),
                        NguonBienDongKho.KHOI_TAO_BIEN_THE, id, "VARIANT_CREATE:" + id, 0, r.getSoLuongTon(),
                        r.getSoLuongTon(), entity.getPhienBan(), "Tồn khởi tạo khi tạo biến thể.");
            }
            em.flush();
            DuLieuBienThe out = new DuLieuBienThe();
            out.setMaBienThe(id);
            out.setMaSP(productId);
            out.setMauSac(color);
            out.setKichCo(size);
            out.setSoLuongTon(entity.getSoLuongTon());
            out.setTrangThai(entity.getTrangThai());
            out.setPhienBan(entity.getPhienBan());
            return out;
        });
    }

    private TaiKhoan manager(EntityManager em, String token) {
        TaiKhoan account = auth.requireAuthenticated(em, token);
        if (account.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY) {
            throw new NgoaiLeNghiepVu(MaLoi.FORBIDDEN,
                    "Chỉ nhân viên quản lý được thay đổi danh mục/sản phẩm.");
        }
        return account;
    }

    private static void uniqueName(EntityManager em, String entity, String idColumn, String nameColumn,
            String id, String name) {
        Long count = em
                .createQuery("select count(e) from " + entity + " e where lower(e." + nameColumn
                        + ") = :n and e." + idColumn + " <> :id", Long.class)
                .setParameter("n", name.toLowerCase(java.util.Locale.ROOT)).setParameter("id", id)
                .getSingleResult();
        if (count > 0) {
            throw invalid("Tên đã tồn tại.");
        }
    }

    private static String text(String value, int max) {
        if (value == null) {
            throw invalid("Không được để trống trường bắt buộc.");
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).trim();
        if (normalized.isEmpty() || normalized.length() > max
                || normalized.chars().anyMatch(Character::isISOControl)) {
            throw invalid("Nội dung trống, quá dài hoặc có ký tự điều khiển.");
        }
        return normalized;
    }

    private static String code(String value) {
        String code = text(value, 20);
        if (!code.matches("[A-Za-z0-9_-]{1,20}")) {
            throw invalid("Mã chỉ chứa chữ Latin không dấu, số, dấu gạch ngang/gạch dưới, tối đa 20 ký tự.");
        }
        return code;
    }

    private static void expected(String before, String now) {
        if (!Objects.equals(before, now)) {
            throw conflict();
        }
    }

    private static NgoaiLeNghiepVu conflict() {
        return new NgoaiLeNghiepVu(MaLoi.VERSION_CONFLICT, "Dữ liệu đã đổi. Tải lại trước khi lưu.");
    }

    private static NgoaiLeNghiepVu missing() {
        return new NgoaiLeNghiepVu(MaLoi.NOT_FOUND, "Không tìm thấy dữ liệu cần sửa.");
    }

    private static NgoaiLeNghiepVu invalid(String text) {
        return new NgoaiLeNghiepVu(MaLoi.VALIDATION_ERROR, text);
    }
}
