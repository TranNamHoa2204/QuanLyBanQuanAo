package kiemthu;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import client.api.GoiDichVuSanPham;
import client.api.GoiDichVuXacThuc;
import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import client.utils.TaiAnh;
import common.dto.sanpham.DuLieuAnhSanPham;
import common.dto.sanpham.DuLieuBienThe;
import common.dto.sanpham.DuLieuDayDuSanPham;
import common.dto.sanpham.DuLieuSanPham;
import common.dto.sanpham.KetQuaTraCuuSanPham;
import common.dto.sanpham.YeuCauTraCuuSanPham;
import common.enums.VaiTro;
import common.protocol.BoMaHoaJson;
import server.dispatcher.DieuPhoiYeuCau;
import server.network.MayChuTcp;
import server.security.BoBamMatKhauPbkdf2;
import server.services.DichVuSanPham;
import server.services.DichVuXacThuc;
import server.session.QuanLyPhienDangNhap;
import server.utils.QuanLyJpa;

public class KiemThuDuLieuSanPham {
    @Test
    public void entireUserCatalogSurvivesJpaSocketAndImageThumbnail() throws Exception {
        JsonNode source;
        try (InputStream input = getClass().getResourceAsStream("/data/catalog.json")) {
            assertNotNull(input);
            source = BoMaHoaJson.mapper().readTree(input);
        }
        try (QuanLyJpa jpa = new QuanLyJpa(HoTroKiemThuDanhMuc.memoryProperties("normalized_catalog_test"))) {
            BoBamMatKhauPbkdf2 hasher = new BoBamMatKhauPbkdf2();
            String hash = hasher.hash("CatalogPass123!".toCharArray());
            jpa.transaction(em -> {
                HoTroKiemThuDanhMuc.catalog(em, source);
                HoTroKiemThuDanhMuc.user(em, "catalog", "NVTEST", VaiTro.NHAN_VIEN_BAN_HANG, hash);
                return null;
            });
            QuanLyPhienDangNhap sessions = new QuanLyPhienDangNhap();
            DichVuXacThuc auth = new DichVuXacThuc(jpa, sessions, hasher);
            try (MayChuTcp server = new MayChuTcp("127.0.0.1", 0,
                    new DieuPhoiYeuCau(auth, new DichVuSanPham(jpa, auth)))) {
                server.start();
                try (KetNoiMayChu socket = new KetNoiMayChu("127.0.0.1", server.getPort())) {
                    PhienDangNhap session = new PhienDangNhap();
                    new GoiDichVuXacThuc(socket, session).login("catalog", "CatalogPass123!");
                    GoiDichVuSanPham api = new GoiDichVuSanPham(socket, session);
                    YeuCauTraCuuSanPham query = new YeuCauTraCuuSanPham();
                    query.setPageSize(100);
                    KetQuaTraCuuSanPham all = api.search(query);
                    assertEquals(40, all.getTotal());
                    assertEquals(40, all.getItems().size());
                    assertEquals(6, api.categories().size());
                    assertEquals(3, api.brands().size());
                    int variants = 0, images = 0;
                    for (DuLieuSanPham row : all.getItems()) {
                        DuLieuDayDuSanPham detail = api.detail(row.getMaSP());
                        assertEquals(row.getGiaBan(), detail.getSanPham().getGiaBan());
                        assertEquals(row.getTongTon(), detail.getSanPham().getTongTon());
                        variants += detail.getBienThe().size();
                        images += detail.getHinhAnh().size();
                        Set<String> colors = new HashSet<>();
                        for (DuLieuBienThe b : detail.getBienThe()) {
                            colors.add(b.getMauSac());
                        }
                        for (DuLieuAnhSanPham a : detail.getHinhAnh()) {
                            assertTrue(colors.contains(a.getMauSac()));
                            assertNotNull(getClass().getClassLoader().getResource(a.getDuongDan()));
                        }
                    }
                    assertEquals(152, variants);
                    assertEquals(255, images);
                    java.awt.image.BufferedImage thumbnail = TaiAnh
                            .thumbnail(api.detail("SP1001").getHinhAnh().get(0).getDuongDan(), 350, 400);
                    assertTrue(thumbnail.getWidth() <= 350);
                    assertTrue(thumbnail.getHeight() <= 400);
                    try {
                        TaiAnh.load("images/products/missing.jpg");
                        fail("Missing image");
                    } catch (java.io.IOException expected) {
                    }
                    try {
                        TaiAnh.load("../secrets.png");
                        fail("Unsafe path");
                    } catch (IllegalArgumentException expected) {
                    }
                }
            }
        }
    }
}
