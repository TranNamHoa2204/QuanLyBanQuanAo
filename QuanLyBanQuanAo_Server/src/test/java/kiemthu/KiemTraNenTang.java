package kiemthu;

import java.math.BigDecimal;

import client.config.ClientConfig;
import common.dto.sanpham.DuLieuSanPham;
import common.enums.VaiTro;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;
import common.utils.TienIchTienTe;
import server.config.ServerConfig;
import server.security.KiemTraPhanQuyen;

/** Chạy bằng Java Application; chưa cần JUnit. Maven không tự chạy test main này. */
public final class KiemTraNenTang {
    private KiemTraNenTang() {
    }

    public static void main(String[] args) throws Exception {
        check(TienIchTienTe.lineTotal(new BigDecimal("123.45"), 3).equals(new BigDecimal("370.35")),
                "line total");
        check(TienIchTienTe.percentage(new BigDecimal("250000"), new BigDecimal("10"))
                .equals(new BigDecimal("25000.00")), "percentage");
        check(TienIchTienTe.round(new BigDecimal("1.005")).equals(new BigDecimal("1.01")), "rounding");
        check(TienIchTienTe.percentage(BigDecimal.TEN, BigDecimal.ZERO).equals(new BigDecimal("0.00")),
                "zero percentage");
        expectIllegalArgument(() -> TienIchTienTe.lineTotal(BigDecimal.ONE, 0));
        expectIllegalArgument(() -> TienIchTienTe.lineTotal(new BigDecimal("-1"), 2));
        expectIllegalArgument(() -> TienIchTienTe.percentage(BigDecimal.ONE, new BigDecimal("101")));
        expectIllegalArgument(() -> TienIchTienTe.round(null));

        DuLieuSanPham product = new DuLieuSanPham();
        product.setMaSP("SP001");
        check("SP001".equals(product.getMaSP()), "DTO getter/setter");
        PhanHoi<String> success = PhanHoi.success("r1", "ok");
        check(success.isSuccess() && "ok".equals(success.getPayload()), "success response");
        PhanHoi<String> failure = PhanHoi.failure("r2", MaLoi.FORBIDDEN, "denied");
        check(!failure.isSuccess() && failure.getErrorCode() == MaLoi.FORBIDDEN, "failure response");
        expectIllegalArgument(() -> PhanHoi.failure("r3", MaLoi.NONE, "invalid"));

        KiemTraPhanQuyen.requireManager(VaiTro.NHAN_VIEN_QUAN_LY);
        expectSecurity(() -> KiemTraPhanQuyen.requireManager(VaiTro.NHAN_VIEN_BAN_HANG));
        expectSecurity(() -> KiemTraPhanQuyen.requireManager(null));
        check(ServerConfig.load().getPort() == 8888, "server config");
        check("localhost".equals(ClientConfig.load().getHost()), "client config");
        check(ClientConfig.load().getPort() == ServerConfig.load().getPort(), "matching ports");
        System.out.println("PASS: scaffold utilities, DTO, response, permissions and config.");
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError(name);
        }
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void expectSecurity(Runnable action) {
        try {
            action.run();
        } catch (SecurityException expected) {
            return;
        }
        throw new AssertionError("Expected SecurityException");
    }
}
