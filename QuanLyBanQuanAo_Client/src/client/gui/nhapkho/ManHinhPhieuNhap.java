package client.gui.nhapkho;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import client.api.GoiDichVuNhaCungCap;
import client.api.GoiDichVuSanPham;
import client.api.NgoaiLeGoiDichVu;
import client.controllers.DieuKhienPhieuNhap;
import client.gui.components.BieuTuongCoDien;
import client.gui.components.ChuDeGiaoDien;
import client.gui.components.TienIchBangSanPham;
import common.dto.nhacungcap.DuLieuNhaCungCap;
import common.dto.nhacungcap.YeuCauTraCuuNhaCungCap;
import common.dto.phieunhap.DuLieuChiTietPhieuNhap;
import common.dto.phieunhap.DuLieuDayDuPhieuNhap;
import common.dto.phieunhap.DuLieuPhieuNhap;
import common.dto.phieunhap.YeuCauDongPhieuNhap;
import common.dto.phieunhap.YeuCauPhienBanPhieuNhap;
import common.dto.phieunhap.YeuCauTaoPhieuNhap;
import common.dto.phieunhap.YeuCauTraCuuPhieuNhap;
import common.dto.phieunhap.YeuCauXacNhanPhieuNhap;
import common.enums.TrangThaiPhieuNhap;
import common.protocol.MaLoi;

/** Purchase receipt workflow; inventory is changed only after server confirmation. */
public final class ManHinhPhieuNhap extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 20;
    private final DieuKhienPhieuNhap controller;
    private final GoiDichVuNhaCungCap suppliers;
    private final GoiDichVuSanPham products;
    private final JTextField keyword = new JTextField(12), from = new JTextField(9), to = new JTextField(9);
    private final JComboBox<String> statusFilter = new JComboBox<String>(
            new String[] { "Tất cả", "Chưa nhập kho", "Đã nhập kho", "Đã hủy" });
    private final JButton search = new JButton("Tìm phiếu nhập", BieuTuongCoDien.TIM_KIEM), create = new JButton("Lập phiếu nhập", BieuTuongCoDien.THEM),
            previous = new JButton("Trang trước"), next = new JButton("Trang sau"),
            addLine = new JButton("Thêm hàng", BieuTuongCoDien.SAN_PHAM), updateLine = new JButton("Sửa số lượng / giá nhập"),
            removeLine = new JButton("Xóa khỏi phiếu"), confirm = new JButton("Xác nhận nhập kho", BieuTuongCoDien.XAC_NHAN),
            cancel = new JButton("Hủy phiếu"), retry = new JButton("Gửi lại thao tác", BieuTuongCoDien.LAM_MOI);
    private final JLabel summary = ChuDeGiaoDien.muted("Chọn hoặc tạo một phiếu nhập."),
            status = ChuDeGiaoDien.muted("Chọn phiếu để xem chi tiết hoặc lập phiếu mới.");
    private final DefaultTableModel receiptModel = TienIchBangSanPham.model(
            new String[] { "Mã phiếu", "Nhà cung cấp", "Ngày lập", "Trạng thái", "Tổng tiền" },
            new Class<?>[] { String.class, String.class, String.class, String.class, BigDecimal.class });
    private final JTable receiptTable = ChuDeGiaoDien.tableWithHorizontalScroll(receiptModel,
            165, 245, 175, 120, 135);
    private final DefaultTableModel lineModel = TienIchBangSanPham.model(
            new String[] { "Biến thể", "Sản phẩm", "Màu", "Size", "Số lượng", "Đơn giá nhập", "Thành tiền" },
            new Class<?>[] { String.class, String.class, String.class, String.class, Integer.class,
                    BigDecimal.class, BigDecimal.class });
    private final JTable lineTable = ChuDeGiaoDien.tableWithHorizontalScroll(lineModel,
            115, 265, 100, 70, 85, 125, 135);
    private List<DuLieuPhieuNhap> receipts = new ArrayList<DuLieuPhieuNhap>();
    private DuLieuDayDuPhieuNhap current;
    private int page;
    private long total, sequence;
    private boolean busy, replacingReceipts;
    private boolean loadingReceipt;
    private String requestedReceiptCode;
    private Object pendingRequest;
    private String pendingKind;

    public ManHinhPhieuNhap(DieuKhienPhieuNhap controller, GoiDichVuNhaCungCap suppliers,
            GoiDichVuSanPham products) {
        super(new BorderLayout(0, 12));
        this.controller = controller;
        this.suppliers = suppliers;
        this.products = products;
        setBorder(BorderFactory.createEmptyBorder(8, 16, 14, 16));
        JPanel top = ChuDeGiaoDien.card(new BorderLayout(0, 10), 14);
        top.add(ChuDeGiaoDien.heading("Phiếu nhập kho",
                "Chọn một phiếu để xem hàng đã nhập. Tồn kho cập nhật khi xác nhận phiếu."),
                BorderLayout.NORTH);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        filters.setOpaque(false);
        filters.add(ChuDeGiaoDien.muted("Mã phiếu/NCC"));
        filters.add(keyword);
        filters.add(ChuDeGiaoDien.muted("Trạng thái"));
        filters.add(statusFilter);
        filters.add(ChuDeGiaoDien.muted("Từ ngày"));
        filters.add(from);
        filters.add(ChuDeGiaoDien.muted("Đến ngày"));
        filters.add(to);
        filters.add(search);
        filters.add(create);
        ChuDeGiaoDien.primary(search);
        ChuDeGiaoDien.primary(create);
        top.add(filters, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        TienIchBangSanPham.configure(receiptTable);
        receiptTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TienIchBangSanPham.configure(lineTable);
        lineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel receiptsPanel = ChuDeGiaoDien.card(new BorderLayout(0, 7), 10);
        receiptsPanel.setPreferredSize(new Dimension(0, 210));
        JScrollPane receiptScroll = ChuDeGiaoDien.tableScroll(receiptTable);
        receiptsPanel.add(receiptScroll, BorderLayout.CENTER);
        JPanel pager = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pager.setOpaque(false);
        pager.add(previous);
        pager.add(next);
        receiptsPanel.add(pager, BorderLayout.SOUTH);
        JPanel detailPanel = ChuDeGiaoDien.card(new BorderLayout(0, 8), 10);
        detailPanel.add(summary, BorderLayout.NORTH);
        JScrollPane lineScroll = ChuDeGiaoDien.tableScroll(lineTable);
        detailPanel.add(lineScroll, BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, receiptsPanel, detailPanel);
        split.setResizeWeight(.32);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.setOpaque(false);
        actions.add(addLine);
        actions.add(updateLine);
        actions.add(removeLine);
        actions.add(confirm);
        actions.add(cancel);
        actions.add(retry);
        ChuDeGiaoDien.primary(confirm);
        ChuDeGiaoDien.danger(cancel);
        detailPanel.add(actions, BorderLayout.SOUTH);
        add(status, BorderLayout.SOUTH);
        search.addActionListener(e -> {
            page = 0;
            load();
        });
        for (JTextField input : new JTextField[] { keyword, from, to }) {
            input.addActionListener(e -> search.doClick());
        }
        statusFilter.addActionListener(e -> {
            if (!busy) {
                page = 0;
                load();
            }
        });
        previous.addActionListener(e -> {
            if (!busy && page > 0) {
                page--;
                load();
            }
        });
        next.addActionListener(e -> {
            if (!busy && (long) (page + 1) * PAGE_SIZE < total) {
                page++;
                load();
            }
        });
        create.addActionListener(e -> chooseSupplierAndCreate());
        addLine.addActionListener(e -> chooseProductAndAdd());
        updateLine.addActionListener(e -> editSelectedLine());
        removeLine.addActionListener(e -> removeSelectedLine());
        confirm.addActionListener(e -> confirmDraft());
        cancel.addActionListener(e -> cancelDraft());
        retry.addActionListener(e -> retryPending());
        receiptTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !busy && !replacingReceipts) {
                loadSelectedReceipt();
            }
        });
        lineTable.getSelectionModel().addListSelectionListener(e -> refreshControls());
        TienIchBangSanPham.onOpenRow(receiptTable, this::loadSelectedReceipt);
        TienIchBangSanPham.onOpenRow(lineTable, () -> updateLine.doClick());
        refreshControls();
    }

    public void reload() {
        if (!busy) {
            page = 0;
            load();
        }
    }

    private void load() {
        load(false);
    }

    private void load(boolean keepOpenReceipt) {
        final YeuCauTraCuuPhieuNhap r = new YeuCauTraCuuPhieuNhap();
        r.setTuKhoa(blank(keyword.getText()));
        try {
            r.setTuNgay(parseDate(from.getText()));
            r.setDenNgay(parseDate(to.getText()));
        } catch (DateTimeParseException invalid) {
            message("Ngày phải theo định dạng yyyy-MM-dd.");
            return;
        }
        int idx = statusFilter.getSelectedIndex();
        r.setTrangThai(idx == 0 ? null
                : idx == 1 ? TrangThaiPhieuNhap.NHAP
                        : idx == 2 ? TrangThaiPhieuNhap.DA_XAC_NHAN : TrangThaiPhieuNhap.DA_HUY);
        r.setPage(page);
        r.setPageSize(PAGE_SIZE);
        runRead(() -> controller.search(r), result -> {
            String openCode = current == null ? null : current.getPhieuNhap().getMaPhieuNhap();
            receipts = result.getItems() == null ? new ArrayList<DuLieuPhieuNhap>() : result.getItems();
            total = result.getTotal();
            page = result.getPage();
            replacingReceipts = true;
            receiptTable.clearSelection();
            receiptModel.setRowCount(0);
            int selected = -1;
            for (DuLieuPhieuNhap row : receipts) {
                if (row.getMaPhieuNhap().equals(openCode)) {
                    selected = receiptModel.getRowCount();
                }
                receiptModel.addRow(new Object[] { row.getMaPhieuNhap(), row.getTenNCC(), row.getNgayLap(),
                        statusName(row.getTrangThai()), row.getTongTien() });
            }
            if (selected >= 0) {
                int view = receiptTable.convertRowIndexToView(selected);
                receiptTable.setRowSelectionInterval(view, view);
            } else if (!keepOpenReceipt) {
                clearReceipt();
            }
            replacingReceipts = false;
            requestedReceiptCode = current == null ? null : current.getPhieuNhap().getMaPhieuNhap();
            status.setText(total + " phiếu • Trang " + (page + 1) + "/"
                    + Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE)
                    + (receipts.isEmpty() ? " • Không có kết quả." : "")
                    + (selected < 0 && keepOpenReceipt ? " • Phiếu " + openCode + " nằm ngoài bộ lọc." : ""));
            refreshControls();
        });
    }

    private void loadSelectedReceipt() {
        if (busy) {
            return;
        }
        DuLieuPhieuNhap selected = selectedReceipt();
        if (selected == null) {
            clearReceipt();
            status.setText("Chọn phiếu để xem chi tiết hoặc lập phiếu mới.");
            return;
        }
        String code = selected.getMaPhieuNhap();
        if (code.equals(requestedReceiptCode) && (loadingReceipt
                || current != null && code.equals(current.getPhieuNhap().getMaPhieuNhap()))) {
            return;
        }
        requestedReceiptCode = code;
        loadReceiptDetail();
        refreshControls();
    }

    private void loadReceiptDetail() {
        if (loadingReceipt || requestedReceiptCode == null) {
            return;
        }
        String code = requestedReceiptCode;
        long operation = sequence;
        loadingReceipt = true;
        new SwingWorker<DuLieuDayDuPhieuNhap, Void>() {
            @Override
            protected DuLieuDayDuPhieuNhap doInBackground() throws Exception {
                return controller.detail(code);
            }

            @Override
            protected void done() {
                loadingReceipt = false;
                if (operation != sequence) {
                    refreshControls();
                    return;
                }
                if (!code.equals(requestedReceiptCode)) {
                    loadReceiptDetail();
                } else {
                    try {
                        showReceipt(get());
                        status.setText("Đang xem phiếu " + code + ".");
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException failure) {
                        clearReceipt();
                        status.setText("Chưa mở được phiếu. Bấm đúp dòng phiếu để thử lại.");
                    }
                }
                refreshControls();
            }
        }.execute();
    }

    private void clearReceipt() {
        current = null;
        requestedReceiptCode = null;
        lineModel.setRowCount(0);
        summary.setText("Chọn hoặc lập một phiếu nhập.");
        refreshControls();
    }

    private DuLieuPhieuNhap selectedReceipt() {
        int index = receiptTable.getSelectedRow();
        return index < 0 ? null : receipts.get(receiptTable.convertRowIndexToModel(index));
    }

    private DuLieuChiTietPhieuNhap selectedLine() {
        int index = lineTable.getSelectedRow();
        if (current == null || index < 0)
            return null;
        return current.getChiTiet().get(lineTable.convertRowIndexToModel(index));
    }

    private void showReceipt(DuLieuDayDuPhieuNhap value) {
        current = value;
        requestedReceiptCode = value.getPhieuNhap().getMaPhieuNhap();
        lineModel.setRowCount(0);
        if (value.getChiTiet() != null)
            for (DuLieuChiTietPhieuNhap row : value.getChiTiet())
                lineModel.addRow(new Object[] { row.getMaBienThe(), row.getTenSP(), row.getMauSac(),
                        row.getKichCo(), row.getSoLuong(), row.getDonGiaNhap(), row.getThanhTien() });
        DuLieuPhieuNhap h = value.getPhieuNhap();
        summary.setText("<html><div style='width:1000px'><b>" + escape(h.getMaPhieuNhap()) + "</b> • "
                + escape(h.getTenNCC()) + " • " + statusName(h.getTrangThai()) + " • Tạo: "
                + escape(h.getTenNVTao()) + " • Xác nhận: "
                + escape(h.getTenNVXacNhan() == null ? "—" : h.getTenNVXacNhan()) + " • Tổng tiền nhập: <b>"
                + ChuDeGiaoDien.money(h.getTongTien()) + "</b>"
                + "</div></html>");
        refreshControls();
    }

    private void chooseSupplierAndCreate() {
        if (pendingRequest != null) {
            status.setText("Hãy xử lý lần gửi đang chờ trước.");
            return;
        }
        YeuCauTraCuuNhaCungCap r = new YeuCauTraCuuNhaCungCap();
        r.setTrangThai(Boolean.TRUE);
        r.setPage(0);
        r.setPageSize(100);
        runRead(() -> suppliers.search(r), result -> {
            List<DuLieuNhaCungCap> options = result.getItems() == null ? new ArrayList<DuLieuNhaCungCap>()
                    : result.getItems();
            if (options.isEmpty()) {
                message("Chưa có nhà cung cấp hoạt động. Hãy tạo nhà cung cấp trước.");
                return;
            }
            JComboBox<DuLieuNhaCungCap> combo = new JComboBox<DuLieuNhaCungCap>(
                    options.toArray(new DuLieuNhaCungCap[0]));
            combo.setRenderer(new DefaultListCellRenderer() {
                private static final long serialVersionUID = 1L;

                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean selected, boolean focus) {
                    super.getListCellRendererComponent(list, value, index, selected, focus);
                    if (value instanceof DuLieuNhaCungCap) {
                        DuLieuNhaCungCap d = (DuLieuNhaCungCap) value;
                        setText(d.getMaNCC() + " — " + d.getTenNCC());
                    }
                    return this;
                }
            });
            JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
            form.add(new JLabel("Chọn nhà cung cấp"));
            form.add(combo);
            if (JOptionPane.showConfirmDialog(this, form, "Tạo phiếu nhập nháp",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
                return;
            DuLieuNhaCungCap supplier = (DuLieuNhaCungCap) combo.getSelectedItem();
            if (supplier == null)
                return;
            YeuCauTaoPhieuNhap request = new YeuCauTaoPhieuNhap();
            request.setMaPhieuNhap(newReceiptCode());
            request.setMaNCC(supplier.getMaNCC());
            request.setOperationId(UUID.randomUUID().toString());
            pendingRequest = request;
            pendingKind = "CREATE";
            write(() -> controller.create(request), "Đã tạo phiếu nhập nháp.");
        });
    }

    private void chooseProductAndAdd() {
        if (!canEdit() || pendingRequest != null) {
            return;
        }
        HopThoaiThemHangNhap picker = new HopThoaiThemHangNhap(
                SwingUtilities.getWindowAncestor(this), products);
        picker.setVisible(true);
        HopThoaiThemHangNhap.Selection selected = picker.getSelection();
        if (selected == null) {
            return;
        }
        YeuCauDongPhieuNhap r = lineRequest(selected.variant, selected.quantity, selected.price);
        pendingRequest = r;
        pendingKind = "ADD";
        write(() -> controller.add(r), "Đã thêm hàng vào phiếu.");
    }

    private void editSelectedLine() {
        DuLieuChiTietPhieuNhap line = selectedLine();
        if (line == null || !canEdit() || pendingRequest != null)
            return;
        JSpinner quantity = new JSpinner(new SpinnerNumberModel(line.getSoLuong(), 1, 1000000, 1));
        JTextField price = new JTextField(line.getDonGiaNhap().toPlainString(), 12);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Số lượng"));
        form.add(quantity);
        form.add(new JLabel("Đơn giá nhập"));
        form.add(price);
        if (JOptionPane.showConfirmDialog(this, form, "Sửa dòng nhập",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return;
        BigDecimal amount;
        try {
            amount = new BigDecimal(price.getText().trim());
        } catch (RuntimeException bad) {
            message("Đơn giá nhập phải là số tiền hợp lệ.");
            return;
        }
        YeuCauDongPhieuNhap r = lineRequest(line.getMaBienThe(), (Integer) quantity.getValue(), amount);
        pendingRequest = r;
        pendingKind = "UPDATE";
        write(() -> controller.update(r), "Đã cập nhật dòng nháp.");
    }

    private void removeSelectedLine() {
        DuLieuChiTietPhieuNhap line = selectedLine();
        if (line == null || !canEdit() || pendingRequest != null)
            return;
        if (JOptionPane.showConfirmDialog(this, "Bỏ " + line.getMaBienThe() + " khỏi phiếu nháp?", "Xác nhận",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;
        YeuCauDongPhieuNhap r = lineRequest(line.getMaBienThe(), null, null);
        pendingRequest = r;
        pendingKind = "REMOVE";
        write(() -> controller.remove(r), "Đã bỏ dòng hàng.");
    }

    private void confirmDraft() {
        if (!canEdit() || pendingRequest != null)
            return;
        DuLieuPhieuNhap h = current.getPhieuNhap();
        if (current.getChiTiet() == null || current.getChiTiet().isEmpty()) {
            message("Phiếu nhập chưa có dòng hàng.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this,
                "Xác nhận nhập kho với tổng giá nhập " + ChuDeGiaoDien.money(h.getTongTien())
                        + "? Tồn kho sẽ tăng một lần.",
                "Xác nhận nhập", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;
        YeuCauXacNhanPhieuNhap r = new YeuCauXacNhanPhieuNhap();
        r.setMaPhieuNhap(h.getMaPhieuNhap());
        r.setPhienBan(h.getPhienBan());
        r.setOperationId(UUID.randomUUID().toString());
        pendingRequest = r;
        pendingKind = "CONFIRM";
        write(() -> controller.confirm(r), "Đã xác nhận phiếu và cập nhật tồn kho.");
    }

    private void cancelDraft() {
        if (!canEdit() || pendingRequest != null)
            return;
        DuLieuPhieuNhap h = current.getPhieuNhap();
        if (JOptionPane.showConfirmDialog(this, "Hủy phiếu nhập nháp? Tồn kho chưa bị thay đổi.", "Hủy nháp",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;
        YeuCauPhienBanPhieuNhap r = new YeuCauPhienBanPhieuNhap();
        r.setMaPhieuNhap(h.getMaPhieuNhap());
        r.setPhienBan(h.getPhienBan());
        r.setOperationId(UUID.randomUUID().toString());
        pendingRequest = r;
        pendingKind = "CANCEL";
        write(() -> controller.cancel(r), "Đã hủy phiếu nháp.");
    }

    private YeuCauDongPhieuNhap lineRequest(String variant, Integer qty, BigDecimal price) {
        DuLieuPhieuNhap h = current.getPhieuNhap();
        YeuCauDongPhieuNhap r = new YeuCauDongPhieuNhap();
        r.setMaPhieuNhap(h.getMaPhieuNhap());
        r.setPhienBan(h.getPhienBan());
        r.setMaBienThe(variant);
        r.setSoLuong(qty);
        r.setDonGiaNhap(price);
        r.setOperationId(UUID.randomUUID().toString());
        return r;
    }

    private boolean canEdit() {
        return !loadingReceipt && current != null
                && current.getPhieuNhap().getMaPhieuNhap().equals(requestedReceiptCode)
                && current.getPhieuNhap().getTrangThai() == TrangThaiPhieuNhap.NHAP;
    }

    private void retryPending() {
        if (busy || pendingRequest == null)
            return;
        Object request = pendingRequest;
        String kind = pendingKind;
        switch (kind) {
        case "CREATE":
            write(() -> controller.create((YeuCauTaoPhieuNhap) request), "Đã tạo phiếu nhập nháp.");
            break;
        case "ADD":
            write(() -> controller.add((YeuCauDongPhieuNhap) request), "Đã thêm dòng hàng vào nháp.");
            break;
        case "UPDATE":
            write(() -> controller.update((YeuCauDongPhieuNhap) request), "Đã cập nhật dòng nháp.");
            break;
        case "REMOVE":
            write(() -> controller.remove((YeuCauDongPhieuNhap) request), "Đã bỏ dòng hàng.");
            break;
        case "CONFIRM":
            write(() -> controller.confirm((YeuCauXacNhanPhieuNhap) request),
                    "Đã xác nhận phiếu và cập nhật tồn kho.");
            break;
        case "CANCEL":
            write(() -> controller.cancel((YeuCauPhienBanPhieuNhap) request), "Đã hủy phiếu nháp.");
            break;
        default:
            break;
        }
    }

    private void write(Callable<DuLieuDayDuPhieuNhap> work, String successMessage) {
        run(work, result -> {
            pendingRequest = null;
            pendingKind = null;
            showReceipt(result);
            status.setText(successMessage);
            load(true);
        }, true);
    }

    private <T> void runRead(Callable<T> work, Consumer<T> success) {
        run(work, success, false);
    }

    private <T> void run(Callable<T> work, Consumer<T> success, boolean changing) {
        if (busy)
            return;
        busy = true;
        long currentSequence = ++sequence;
        status.setText("Đang xử lý với máy chủ...");
        refreshControls();
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                if (currentSequence != sequence)
                    return;
                busy = false;
                refreshControls();
                try {
                    success.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (changing && cause instanceof NgoaiLeGoiDichVu) {
                        NgoaiLeGoiDichVu api = (NgoaiLeGoiDichVu) cause;
                        if (api.getCode() != MaLoi.SERVER_ERROR) {
                            pendingRequest = null;
                            pendingKind = null;
                        }
                    }
                    String message = cause instanceof NgoaiLeGoiDichVu ? cause.getMessage()
                            : changing ? "Chưa nhận được kết quả. Bấm Gửi lại thao tác để kiểm tra và gửi lại."
                                    : "Chưa mở được dữ liệu. Chọn lại phiếu hoặc bấm Tìm phiếu nhập để thử lại.";
                    status.setText(message);
                    JOptionPane.showMessageDialog(ManHinhPhieuNhap.this, message, "Phiếu nhập",
                            JOptionPane.WARNING_MESSAGE);
                    refreshControls();
                }
            }
        }.execute();
    }

    private void refreshControls() {
        boolean draft = canEdit(), pending = pendingRequest != null, selected = selectedLine() != null;
        search.setEnabled(!busy);
        create.setEnabled(!busy && !pending);
        keyword.setEnabled(!busy);
        from.setEnabled(!busy);
        to.setEnabled(!busy);
        statusFilter.setEnabled(!busy);
        previous.setEnabled(!busy && page > 0);
        next.setEnabled(!busy && (long) (page + 1) * PAGE_SIZE < total);
        addLine.setEnabled(!busy && !pending && draft);
        updateLine.setEnabled(!busy && !pending && draft && selected);
        removeLine.setEnabled(!busy && !pending && draft && selected);
        confirm.setEnabled(!busy && !pending && draft && current != null && !current.getChiTiet().isEmpty());
        cancel.setEnabled(!busy && !pending && draft);
        retry.setVisible(pending);
        retry.setEnabled(!busy && pending);
    }

    private static LocalDate parseDate(String text) {
        String s = blank(text);
        if (s == null)
            return null;
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException ex) {
            throw ex;
        }
    }

    private static String newReceiptCode() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "PN" + time + UUID.randomUUID().toString().replace("-", "").substring(0, 4);
    }

    private static String blank(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static String statusName(TrangThaiPhieuNhap s) {
        if (s == null)
            return "";
        switch (s) {
        case NHAP:
            return "Chưa nhập kho";
        case DA_XAC_NHAN:
            return "Đã nhập kho";
        case DA_HUY:
            return "Đã hủy";
        default:
            return s.name();
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void message(String s) {
        JOptionPane.showMessageDialog(this, s, "Phiếu nhập", JOptionPane.INFORMATION_MESSAGE);
    }
}
