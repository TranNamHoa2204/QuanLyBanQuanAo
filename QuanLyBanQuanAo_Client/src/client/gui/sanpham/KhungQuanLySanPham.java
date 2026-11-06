package client.gui.sanpham;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import client.controllers.DieuKhienQuanLySanPham;
import client.controllers.DieuKhienSanPham;
import client.gui.components.ChuDeGiaoDien;
import client.gui.components.TacVuGiaoDien;
import client.gui.danhmuc.KhungDanhMuc;
import common.dto.danhmuc.DuLieuLoaiSanPham;
import common.dto.danhmuc.DuLieuThuongHieu;
import common.dto.sanpham.DuLieuSanPham;
import common.dto.sanpham.KetQuaTraCuuSanPham;
import common.dto.sanpham.YeuCauLuuSanPham;
import common.enums.VaiTro;

public final class KhungQuanLySanPham extends JPanel {
    private static final long serialVersionUID = 1L;

    public enum Section {
        PRODUCTS("Danh sách sản phẩm"), CATEGORIES("Loại sản phẩm"), BRANDS("Thương hiệu"),
        VARIANTS("Màu, size và tồn kho");

        private final String title;

        Section(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    private final DieuKhienQuanLySanPham controller;
    private final KhungSanPham list;
    private final KhungDanhMuc categories, brands;
    private final KhungQuanLyBienThe variants;
    private final JTabbedPane tabs = new JTabbedPane();
    private final Runnable refreshActions;
    private boolean actionBusy;

    public KhungQuanLySanPham(DieuKhienQuanLySanPham controller) {
        super(new BorderLayout());
        this.controller = controller;
        list = new KhungSanPham(new DieuKhienSanPham(controller.products()), VaiTro.NHAN_VIEN_QUAN_LY);
        categories = new KhungDanhMuc(controller, false);
        brands = new KhungDanhMuc(controller, true);
        variants = new KhungQuanLyBienThe(controller);
        JPanel products = new JPanel(new BorderLayout());
        products.add(list, BorderLayout.CENTER);
        JButton add = new JButton("Thêm sản phẩm"), edit = new JButton("Sửa sản phẩm"),
                stop = new JButton("Ngừng bán"), variant = new JButton("Màu, size và tồn kho"),
                reload = new JButton("Làm mới");
        ChuDeGiaoDien.primary(add);
        ChuDeGiaoDien.danger(stop);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setBorder(BorderFactory.createEmptyBorder(16, 24, 0, 24));
        for (JButton b : new JButton[] { add, edit, stop, variant, reload }) {
            actions.add(b);
        }
        products.add(actions, BorderLayout.NORTH);
        tabs.addTab("Sản phẩm", products);
        tabs.addTab("Loại sản phẩm", categories);
        tabs.addTab("Thương hiệu", brands);
        tabs.addTab(Section.VARIANTS.getTitle(), variants);
        add(tabs, BorderLayout.CENTER);
        refreshActions = () -> {
            DuLieuSanPham selected = list.selectedProduct();
            boolean ready = !list.isSearching() && !actionBusy;
            add.setEnabled(ready);
            reload.setEnabled(ready);
            edit.setEnabled(ready && selected != null);
            variant.setEnabled(ready && selected != null);
            stop.setEnabled(ready && selected != null && selected.getTrangThai());
        };
        list.onSelectionChanged(e -> refreshActions.run());
        list.addPropertyChangeListener("searching", e -> refreshActions.run());
        refreshActions.run();
        add.addActionListener(e -> openEditor(null, add));
        edit.addActionListener(e -> {
            DuLieuSanPham selected = selected();
            if (selected != null) {
                openEditor(selected.getMaSP(), edit);
            }
        });
        stop.addActionListener(e -> {
            DuLieuSanPham selected = selected();
            if (selected == null) {
                return;
            }
            if (JOptionPane.showConfirmDialog(this,
                    "Ngừng bán " + selected.getMaSP() + "? Không xóa dữ liệu hoặc hóa đơn.", "Xác nhận",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            YeuCauLuuSanPham r = new YeuCauLuuSanPham();
            r.setValue(selected);
            r.setExpected(selected);
            runAction(stop, () -> controller.product(r, 2), out -> list.reload());
        });
        variant.addActionListener(e -> {
            DuLieuSanPham selected = selected();
            if (selected != null) {
                variants.loadProduct(selected.getMaSP());
                tabs.setSelectedComponent(variants);
            }
        });
        reload.addActionListener(e -> list.reload());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == products) {
                list.reload();
            } else if (tabs.getSelectedComponent() == categories) {
                categories.initialize();
            } else if (tabs.getSelectedComponent() == brands) {
                brands.initialize();
            } else if (tabs.getSelectedComponent() == variants) {
                variants.open();
            }
        });
    }

    public void initialize() {
        list.reload();
    }

    public void showSection(Section section) {
        boolean same = tabs.getSelectedIndex() == section.ordinal();
        tabs.setSelectedIndex(section.ordinal());
        if (same && section == Section.PRODUCTS) {
            list.reload();
        } else if (same && section == Section.CATEGORIES) {
            categories.initialize();
        } else if (same && section == Section.BRANDS) {
            brands.initialize();
        } else if (same && section == Section.VARIANTS) {
            variants.open();
        }
    }

    public void showProducts(KetQuaTraCuuSanPham result) {
        list.showSearchResult(result);
    }

    private DuLieuSanPham selected() {
        return list.selectedProduct();
    }

    private void openEditor(String id, JButton trigger) {
        runAction(trigger, () -> {
            Object[] data = new Object[3];
            data[0] = id == null ? null : controller.detail(id).getSanPham();
            data[1] = controller.categories();
            data[2] = controller.brands();
            return data;
        }, this::editor);
    }

    private <T> void runAction(JButton trigger, Callable<T> work, Consumer<T> done) {
        actionBusy = true;
        refreshActions.run();
        TacVuGiaoDien.run(trigger, work, value -> {
            actionBusy = false;
            refreshActions.run();
            done.accept(value);
        }, message -> {
            actionBusy = false;
            refreshActions.run();
            JOptionPane.showMessageDialog(this, message, "Sản phẩm", JOptionPane.WARNING_MESSAGE);
        });
    }

    @SuppressWarnings("unchecked")
    private void editor(Object[] data) {
        DuLieuSanPham original = (DuLieuSanPham) data[0];
        KhungChinhSuaSanPham editor = new KhungChinhSuaSanPham(original,
                (java.util.List<DuLieuLoaiSanPham>) data[1], (java.util.List<DuLieuThuongHieu>) data[2]);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                original == null ? "Thêm sản phẩm" : "Sửa sản phẩm", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel body = new JPanel(new BorderLayout(0, 20));
        body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        body.add(editor, BorderLayout.CENTER);
        JButton save = new JButton("Lưu sản phẩm");
        ChuDeGiaoDien.primary(save);
        body.add(save, BorderLayout.SOUTH);
        save.addActionListener(e -> {
            YeuCauLuuSanPham r;
            try {
                r = editor.request();
            } catch (IllegalArgumentException invalid) {
                JOptionPane.showMessageDialog(dialog, invalid.getMessage());
                return;
            }
            TacVuGiaoDien.run(dialog, save, () -> controller.product(r, original == null ? 0 : 1), out -> {
                dialog.dispose();
                list.reload();
            });
        });
        dialog.add(body);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.getRootPane().setDefaultButton(save);
        dialog.setVisible(true);
    }
}
