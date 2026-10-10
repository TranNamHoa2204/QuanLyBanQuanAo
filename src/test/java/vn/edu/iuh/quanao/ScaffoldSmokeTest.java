package vn.edu.iuh.quanao;

import java.math.BigDecimal;

import client.config.ClientConfig;
import common.dto.SanPhamDTO;
import common.enums.VaiTro;
import common.protocol.ErrorCode;
import common.protocol.Response;
import common.util.MoneyUtils;
import server.config.ServerConfig;
import server.security.PermissionChecker;

/** Chạy bằng Java Application; chưa cần JUnit. Maven không tự chạy test main này. */
public final class ScaffoldSmokeTest {
    private ScaffoldSmokeTest() { }

    public static void main(String[] args) throws Exception {
        check(MoneyUtils.lineTotal(new BigDecimal("123.45"), 3)
                .equals(new BigDecimal("370.35")), "line total");
        check(MoneyUtils.percentage(new BigDecimal("250000"), new BigDecimal("10"))
                .equals(new BigDecimal("25000.00")), "percentage");
        check(MoneyUtils.round(new BigDecimal("1.005"))
                .equals(new BigDecimal("1.01")), "rounding");
        check(MoneyUtils.percentage(BigDecimal.TEN, BigDecimal.ZERO)
                .equals(new BigDecimal("0.00")), "zero percentage");
        expectIllegalArgument(() -> MoneyUtils.lineTotal(BigDecimal.ONE, 0));
        expectIllegalArgument(() -> MoneyUtils.lineTotal(new BigDecimal("-1"), 2));
        expectIllegalArgument(() -> MoneyUtils.percentage(BigDecimal.ONE, new BigDecimal("101")));
        expectIllegalArgument(() -> MoneyUtils.round(null));

        SanPhamDTO product = new SanPhamDTO();
        product.setMaSP("SP001");
        check("SP001".equals(product.getMaSP()), "DTO getter/setter");
        Response<String> success = Response.success("r1", "ok");
        check(success.isSuccess() && "ok".equals(success.getPayload()), "success response");
        Response<String> failure = Response.failure("r2", ErrorCode.FORBIDDEN, "denied");
        check(!failure.isSuccess() && failure.getErrorCode() == ErrorCode.FORBIDDEN,
                "failure response");
        expectIllegalArgument(() -> Response.failure("r3", ErrorCode.NONE, "invalid"));

        PermissionChecker.requireManager(VaiTro.NHAN_VIEN_QUAN_LY);
        expectSecurity(() -> PermissionChecker.requireManager(VaiTro.NHAN_VIEN_BAN_HANG));
        expectSecurity(() -> PermissionChecker.requireManager(null));
        check(ServerConfig.load().getPort() == 8888, "server config");
        check("localhost".equals(ClientConfig.load().getHost()), "client config");
        check(ClientConfig.load().getPort() == ServerConfig.load().getPort(), "matching ports");
        System.out.println("PASS: scaffold utilities, DTO, response, permissions and config.");
    }

    private static void check(boolean condition, String name) {
        if (!condition) { throw new AssertionError(name); }
    }
    private static void expectIllegalArgument(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected IllegalArgumentException");
    }
    private static void expectSecurity(Runnable action) {
        try { action.run(); }
        catch (SecurityException expected) { return; }
        throw new AssertionError("Expected SecurityException");
    }
}
