package client.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;
import client.util.ImageLoader;
import common.dto.AnhSanPhamDTO;

/** Đọc ảnh từ classpath ở background; ảnh thiếu không làm hỏng màn hình. */
public final class ProductImagePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final List<AnhSanPhamDTO> allImages;
    private List<AnhSanPhamDTO> current = new ArrayList<>();
    private final JComboBox<String> color = new JComboBox<>();
    private final JLabel image = new JLabel("", SwingConstants.CENTER),
            caption = new JLabel(" ", SwingConstants.CENTER);
    private final JButton previous = new JButton("Ảnh trước"), next = new JButton("Ảnh sau");
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

    public ProductImagePanel(List<AnhSanPhamDTO> images, Set<String> colors) {
        super(new BorderLayout(8, 8));
        allImages = new ArrayList<>(images);
        LinkedHashSet<String> options = new LinkedHashSet<>(colors);
        for (AnhSanPhamDTO row : images) {
            options.add(row.getMauSac());
        }
        for (String option : options) {
            color.addItem(option);
        }
        JPanel selector = new JPanel(new BorderLayout(8, 0));
        selector.add(new JLabel("Ảnh theo màu"), BorderLayout.WEST);
        selector.add(color, BorderLayout.CENTER);
        add(selector, BorderLayout.NORTH);
        image.setPreferredSize(new Dimension(360, 410));
        image.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        add(image, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(6, 6)), buttons = new JPanel(new FlowLayout());
        buttons.add(previous);
        buttons.add(next);
        bottom.add(caption, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.SOUTH);
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
        for (AnhSanPhamDTO row : allImages) {
            if (Objects.equals(row.getMauSac(), color.getSelectedItem())) {
                current.add(row);
            }
        }
        current.sort(Comparator.comparingInt(AnhSanPhamDTO::getThuTu).thenComparing(AnhSanPhamDTO::getMaAnh));
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
        AnhSanPhamDTO selected = current.get(index);
        caption.setText("Ảnh " + (index + 1) + "/" + current.size() + (selected.getAnhChinh() ? " • Ảnh chính" : ""));
        image.setText("Đang tải ảnh...");
        image.setToolTipText(selected.getDuongDan());
        String path = selected.getDuongDan();
        worker = new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                ImageIcon cached = cache.get(path);
                if (cached != null) {
                    return cached;
                }
                BufferedImage thumbnail = ImageLoader.thumbnail(path, 350, 400);
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
                    image.setText("Không đọc được ảnh trong resources.");
                }
            }
        };
        worker.execute();
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
