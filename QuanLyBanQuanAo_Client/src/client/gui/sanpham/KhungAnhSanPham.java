package client.gui.sanpham;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import client.gui.components.ChuDeGiaoDien;
import client.utils.TaiAnh;
import common.dto.sanpham.DuLieuAnhSanPham;

/** Đọc ảnh từ classpath ở background; ảnh thiếu không làm hỏng màn hình. */
public final class KhungAnhSanPham extends JPanel {
    private static final long serialVersionUID = 1L;
    private final List<DuLieuAnhSanPham> allImages;
    private List<DuLieuAnhSanPham> current = new ArrayList<>();
    private final JComboBox<String> color = new JComboBox<>();
    private final JLabel image = new JLabel("", SwingConstants.CENTER) {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics graphics) {
            if (!(getIcon() instanceof ImageIcon)) {
                super.paintComponent(graphics);
                return;
            }
            ImageIcon icon = (ImageIcon) getIcon();
            Insets padding = getInsets();
            int availableWidth = getWidth() - padding.left - padding.right;
            int availableHeight = getHeight() - padding.top - padding.bottom;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                if (availableWidth > 0 && availableHeight > 0) {
                    double ratio = Math.min(1, Math.min((double) availableWidth / icon.getIconWidth(),
                            (double) availableHeight / icon.getIconHeight()));
                    int width = Math.max(1, (int) (icon.getIconWidth() * ratio));
                    int height = Math.max(1, (int) (icon.getIconHeight() * ratio));
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(icon.getImage(), padding.left + (availableWidth - width) / 2,
                            padding.top + (availableHeight - height) / 2, width, height, this);
                }
            } finally {
                g.dispose();
            }
        }
    };
    private final JLabel caption = new JLabel(" ", SwingConstants.CENTER);
    private final JButton previous = new JButton("‹"), next = new JButton("›");
    private final int imageWidth, imageHeight;
    private int index, generation;
    private SwingWorker<ImageIcon, Void> worker;
    private final Map<String, ImageIcon> cache = Collections
            .synchronizedMap(new LinkedHashMap<String, ImageIcon>(16, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> entry) {
                    return size() > 16;
                }
            });

    public KhungAnhSanPham(List<DuLieuAnhSanPham> images, Set<String> colors) {
        this(images, colors, 360, 410);
    }

    KhungAnhSanPham(List<DuLieuAnhSanPham> images, Set<String> colors, int width, int height) {
        super(new BorderLayout(8, 8));
        setOpaque(false);
        imageWidth = width;
        imageHeight = height;
        allImages = new ArrayList<>(images);
        LinkedHashSet<String> options = new LinkedHashSet<>();
        for (DuLieuAnhSanPham row : images) {
            options.add(row.getMauSac());
        }
        options.addAll(colors);
        for (String option : options) {
            color.addItem(option);
        }
        JPanel selector = new JPanel(new BorderLayout(8, 0));
        selector.setOpaque(false);
        selector.add(new JLabel("Màu ảnh"), BorderLayout.WEST);
        selector.add(color, BorderLayout.CENTER);
        add(selector, BorderLayout.NORTH);
        image.setPreferredSize(new Dimension(width, height));
        image.setOpaque(true);
        image.setBackground(ChuDeGiaoDien.SURFACE);
        image.setBorder(BorderFactory.createLineBorder(ChuDeGiaoDien.LINE));
        add(image, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        bottom.setOpaque(false);
        previous.setToolTipText("Ảnh trước");
        previous.getAccessibleContext().setAccessibleName("Ảnh trước");
        next.setToolTipText("Ảnh sau");
        next.getAccessibleContext().setAccessibleName("Ảnh sau");
        bottom.add(previous, BorderLayout.WEST);
        bottom.add(caption, BorderLayout.CENTER);
        bottom.add(next, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        color.addActionListener(e -> filter());
        previous.addActionListener(e -> {
            if (!current.isEmpty()) {
                index = (index + current.size() - 1) % current.size();
                showImage();
            }
        });
        next.addActionListener(e -> {
            if (!current.isEmpty()) {
                index = (index + 1) % current.size();
                showImage();
            }
        });
        filter();
    }

    public void selectColor(String selected) {
        color.setSelectedItem(selected);
    }

    private void filter() {
        current = new ArrayList<>();
        for (DuLieuAnhSanPham row : allImages) {
            if (Objects.equals(row.getMauSac(), color.getSelectedItem())) {
                current.add(row);
            }
        }
        current.sort(Comparator.comparingInt(DuLieuAnhSanPham::getThuTu)
                .thenComparing(DuLieuAnhSanPham::getMaAnh));
        index = 0;
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).getAnhChinh()) {
                index = i;
                break;
            }
        }
        showImage();
    }

    private void showImage() {
        int revision = ++generation;
        if (worker != null) {
            worker.cancel(true);
        }
        image.setIcon(null);
        image.setToolTipText(null);
        previous.setEnabled(current.size() > 1);
        next.setEnabled(current.size() > 1);
        if (current.isEmpty()) {
            image.setText("Chưa có ảnh cho màu này.");
            caption.setText("0 ảnh");
            return;
        }
        DuLieuAnhSanPham selected = current.get(index);
        caption.setText(
                "Ảnh " + (index + 1) + "/" + current.size() + (selected.getAnhChinh() ? " • Ảnh chính" : ""));
        image.setText("Đang tải ảnh...");
        String path = selected.getDuongDan();
        worker = new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                ImageIcon cached = cache.get(path);
                if (cached != null) {
                    return cached;
                }
                BufferedImage thumbnail = TaiAnh.thumbnail(path, imageWidth - 10, imageHeight - 10);
                ImageIcon icon = new ImageIcon(thumbnail);
                cache.put(path, icon);
                return icon;
            }

            @Override
            protected void done() {
                if (revision != generation) {
                    return;
                }
                try {
                    image.setIcon(get());
                    image.setText("");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | CancellationException failure) {
                    image.setText("Ảnh này hiện không hiển thị được.");
                }
            }
        };
        worker.execute();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (worker != null && worker.isDone() && image.getIcon() == null && !current.isEmpty()) {
            showImage();
        }
    }

    @Override
    public void removeNotify() {
        generation++;
        if (worker != null) {
            worker.cancel(true);
        }
        super.removeNotify();
    }
}
