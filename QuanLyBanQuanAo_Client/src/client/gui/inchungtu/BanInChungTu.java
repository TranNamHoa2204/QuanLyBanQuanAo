package client.gui.inchungtu;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.text.AttributedString;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import client.gui.components.ChuDeGiaoDien;
import common.dto.hoadon.DuLieuChiTietHoaDon;
import common.dto.hoadon.DuLieuDayDuHoaDon;
import common.dto.hoadon.DuLieuHoaDon;
import common.dto.phieunhap.DuLieuChiTietPhieuNhap;
import common.dto.phieunhap.DuLieuDayDuPhieuNhap;
import common.dto.phieunhap.DuLieuPhieuNhap;
import common.enums.PhuongThucThanhToan;
import common.enums.TrangThaiHoaDon;
import common.enums.TrangThaiPhieuNhap;

/** Bản chụp dữ liệu chứng từ đã chốt, dùng chung cho xem trước và in. */
public final class BanInChungTu implements Printable {
    private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    private static final Font BOLD = BODY.deriveFont(Font.BOLD);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int FOOTER = 28;
    private final String title;
    private final String code;
    private final List<String> info;
    private final List<DongHang> lines;
    private final List<String> totals;

    private BanInChungTu(String title, String code, List<String> info, List<DongHang> lines, List<String> totals) {
        this.title = title;
        this.code = code;
        this.info = info;
        this.lines = lines;
        this.totals = totals;
    }

    public static BanInChungTu hoaDon(DuLieuDayDuHoaDon detail) {
        DuLieuHoaDon invoice = detail.getHoaDon();
        if (invoice.getTrangThai() != TrangThaiHoaDon.DA_THANH_TOAN) {
            throw new IllegalArgumentException("Hóa đơn chưa thanh toán.");
        }
        List<String> info = new ArrayList<>();
        info.add("Thanh toán: " + date(invoice.getNgayThanhToan()));
        info.add("Nhân viên: " + name(invoice.getTenNhanVien(), invoice.getMaNV()));
        info.add("Khách hàng: " + name(invoice.getTenKhachHang(), invoice.getMaKH() == null ? "Khách lẻ" : invoice.getMaKH()));
        PhuongThucThanhToan method = invoice.getPhuongThucThanhToan();
        if (method == PhuongThucThanhToan.TIEN_MAT) {
            info.add("Tiền mặt  |  Khách đưa: " + money(invoice.getTienKhachDua()) + "  |  Tiền thừa: "
                    + money(invoice.getTienThua()));
        } else if (method == PhuongThucThanhToan.CHUYEN_KHOAN) {
            info.add("Chuyển khoản" + (Boolean.TRUE.equals(invoice.getNhanVienXacNhanChuyenKhoan())
                    ? " (nhân viên xác nhận đã nhận)" : ""));
        } else {
            info.add("Phương thức thu tiền: Chưa có dữ liệu lịch sử");
        }
        List<DongHang> lines = new ArrayList<>();
        for (DuLieuChiTietHoaDon line : detail.getChiTiet()) {
            lines.add(new DongHang(line.getTenSP(), line.getMaBienThe(), line.getMauSac(), line.getKichCo(),
                    line.getSoLuong(), line.getDonGia(), line.getThanhTien()));
        }
        List<String> totals = new ArrayList<>();
        totals.add("Tạm tính: " + money(invoice.getTamTinh()));
        totals.add("Giảm giá: " + money(invoice.getTienGiam()));
        totals.add("Thuế: " + money(invoice.getTienThue()));
        totals.add("Đã thanh toán: " + money(invoice.getTongTien()));
        return new BanInChungTu("HÓA ĐƠN BÁN HÀNG", invoice.getMaHD(), info, lines, totals);
    }

    public static BanInChungTu phieuNhap(DuLieuDayDuPhieuNhap detail) {
        DuLieuPhieuNhap receipt = detail.getPhieuNhap();
        if (receipt.getTrangThai() != TrangThaiPhieuNhap.DA_XAC_NHAN) {
            throw new IllegalArgumentException("Phiếu nhập chưa xác nhận.");
        }
        List<String> info = new ArrayList<>();
        info.add("Nhà cung cấp: " + name(receipt.getTenNCC(), receipt.getMaNCC()));
        info.add("Ngày lập: " + date(receipt.getNgayLap()) + "  |  Người lập: "
                + name(receipt.getTenNVTao(), receipt.getMaNVTao()));
        info.add("Nhập kho: " + date(receipt.getNgayXacNhan()) + "  |  Người xác nhận: "
                + name(receipt.getTenNVXacNhan(), receipt.getMaNVXacNhan()));
        List<DongHang> lines = new ArrayList<>();
        for (DuLieuChiTietPhieuNhap line : detail.getChiTiet()) {
            lines.add(new DongHang(line.getTenSP(), line.getMaBienThe(), line.getMauSac(), line.getKichCo(),
                    line.getSoLuong(), line.getDonGiaNhap(), line.getThanhTien()));
        }
        List<String> totals = new ArrayList<>();
        totals.add("Tổng tiền nhập: " + money(receipt.getTongTien()));
        return new BanInChungTu("PHIẾU NHẬP KHO", receipt.getMaPhieuNhap(), info, lines, totals);
    }

    public String getName() {
        return title + " · " + code;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int requestedPage) throws PrinterException {
        int width = (int) pageFormat.getImageableWidth();
        int height = (int) pageFormat.getImageableHeight();
        if (requestedPage < 0) {
            return NO_SUCH_PAGE;
        }
        if (width < 330 || height < 300) {
            throw new PrinterException("Vùng in quá nhỏ. Vui lòng chọn khổ A4.");
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            int index = 0;
            for (int page = 0; page <= requestedPage; page++) {
                boolean paint = page == requestedPage;
                int y = heading(g, width, paint);
                if (index < lines.size()) {
                    if (paint) {
                        g.setColor(new Color(0xE8EEF5));
                        g.fillRect(0, y, width, 22);
                        g.setColor(Color.BLACK);
                        g.setFont(BOLD);
                        g.drawString("Sản phẩm / Biến thể", 5, y + 15);
                        right(g, "SL", width * 60 / 100, y + 15, 42);
                        right(g, "Đơn giá", width * 79 / 100, y + 15, 80);
                        right(g, "Thành tiền", width - 5, y + 15, 85);
                    }
                    y += 22;
                }
                int first = index;
                while (index < lines.size()) {
                    DongHang line = lines.get(index);
                    List<TextLayout> description = wrapped(g, line.label, width * 48 / 100);
                    List<TextLayout> variant = wrapped(g, line.variant, width * 48 / 100);
                    int lineHeight = 8 + 13 * (description.size() + variant.size());
                    if (y + lineHeight > height - FOOTER) {
                        break;
                    }
                    if (paint) {
                        drawLayouts(g, description, 5, y + 13);
                        drawLayouts(g, variant, 5, y + 13 + 13 * description.size());
                        g.setFont(BODY);
                        right(g, line.quantity, width * 60 / 100, y + 13, width * 10 / 100);
                        right(g, line.price, width * 79 / 100, y + 13, width * 18 / 100);
                        right(g, line.total, width - 5, y + 13, width * 19 / 100);
                        g.setColor(new Color(0xCAD3DF));
                        g.drawLine(0, y + lineHeight - 1, width, y + lineHeight - 1);
                        g.setColor(Color.BLACK);
                    }
                    y += lineHeight;
                    index++;
                }
                boolean finished = index == lines.size() && y + 10 + totals.size() * 17 <= height - FOOTER;
                if (index == first && !finished) {
                    throw new PrinterException("Vùng in không đủ chỗ cho nội dung chứng từ.");
                }
                if (paint) {
                    if (finished) {
                        y += 8;
                        for (int i = 0; i < totals.size(); i++) {
                            g.setFont(i == totals.size() - 1 ? BOLD.deriveFont(12f) : BODY);
                            right(g, totals.get(i), width - 5, y += 17, width - 10);
                        }
                    }
                    g.setFont(BODY);
                    g.drawLine(0, height - 18, width, height - 18);
                    g.drawString("THREAD · " + code, 0, height - 4);
                    right(g, "Trang " + (page + 1), width, height - 4, 70);
                    return PAGE_EXISTS;
                }
                if (finished) {
                    return NO_SUCH_PAGE;
                }
            }
            return NO_SUCH_PAGE;
        } finally {
            g.dispose();
        }
    }

    private int heading(Graphics2D g, int width, boolean paint) {
        if (paint) {
            g.setColor(Color.BLACK);
            g.setFont(BOLD.deriveFont(11f));
            g.drawString("THREAD  /  CỬA HÀNG QUẦN ÁO", 0, 15);
            g.setFont(BOLD.deriveFont(19f));
            g.drawString(title, 0, 41);
            g.setFont(BOLD);
            g.drawString("Mã chứng từ: " + code, 0, 59);
        }
        int y = 76;
        for (String entry : info) {
            List<TextLayout> layouts = wrapped(g, entry, width - 5);
            if (paint) {
                drawLayouts(g, layouts, 0, y);
            }
            y += 13 * layouts.size();
        }
        return y + 3;
    }

    private static List<TextLayout> wrapped(Graphics2D g, String text, int width) {
        String normalized = text.replaceAll("\\s+", " ");
        AttributedString attributed = new AttributedString(normalized);
        attributed.addAttribute(TextAttribute.FONT, BODY);
        LineBreakMeasurer measurer = new LineBreakMeasurer(attributed.getIterator(), g.getFontRenderContext());
        List<TextLayout> layouts = new ArrayList<>();
        while (measurer.getPosition() < normalized.length()) {
            layouts.add(measurer.nextLayout(width));
        }
        return layouts;
    }

    private static void drawLayouts(Graphics2D g, List<TextLayout> layouts, int x, int y) {
        for (TextLayout layout : layouts) {
            layout.draw(g, x, y);
            y += 13;
        }
    }

    private static void right(Graphics2D g, String text, int x, int y, int maxWidth) {
        Font font = g.getFont();
        int width = g.getFontMetrics().stringWidth(text);
        if (width > maxWidth) {
            g.setFont(font.deriveFont(font.getSize2D() * maxWidth / width));
        }
        g.drawString(text, x - g.getFontMetrics().stringWidth(text), y);
        g.setFont(font);
    }

    private static String name(String text, String fallback) {
        return text == null || text.trim().isEmpty() ? (fallback == null ? "—" : fallback) : text.trim();
    }

    private static String date(LocalDateTime value) {
        return value == null ? "—" : DATE.format(value);
    }

    private static String money(BigDecimal value) {
        return ChuDeGiaoDien.money(value);
    }

    private static final class DongHang {
        private final String label, variant, quantity, price, total;

        private DongHang(String name, String code, String color, String size, int quantity,
                BigDecimal price, BigDecimal total) {
            label = name(name, name(code, "Sản phẩm"));
            variant = name(code, "—") + "  |  " + name(color, "—") + "  /  " + name(size, "—");
            this.quantity = Integer.toString(quantity);
            this.price = money(price);
            this.total = money(total);
        }
    }
}
