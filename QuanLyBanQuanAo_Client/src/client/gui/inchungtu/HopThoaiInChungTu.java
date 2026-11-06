package client.gui.inchungtu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/** Xem từng trang A4 rồi mở hộp thoại máy in của hệ điều hành. */
public final class HopThoaiInChungTu extends JDialog {
    private static final long serialVersionUID = 1L;
    private final BanInChungTu document;
    private final PageFormat paper = a4();
    private final JLabel page = new JLabel("Đang tạo bản xem trước…", SwingConstants.CENTER);
    private final JLabel status = new JLabel(" ");
    private final JButton previous = new JButton("Trang trước"), next = new JButton("Trang sau"),
            printButton = new JButton("In…");
    private final JScrollPane scroll = new JScrollPane(page);
    private int currentPage;
    private int lastPage = -1;

    public static void open(Component parent, BanInChungTu document) {
        new HopThoaiInChungTu(parent, document).setVisible(true);
    }

    private HopThoaiInChungTu(Component parent, BanInChungTu document) {
        super(SwingUtilities.getWindowAncestor(parent), "Xem trước khi in · " + document.getName(),
                ModalityType.APPLICATION_MODAL);
        this.document = document;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        page.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 6));
        footer.add(status, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton close = new JButton("Đóng");
        for (JButton button : new JButton[] { previous, next, printButton, close }) {
            actions.add(button);
        }
        footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        previous.addActionListener(e -> preview(currentPage - 1));
        next.addActionListener(e -> preview(currentPage + 1));
        printButton.addActionListener(e -> print());
        close.addActionListener(e -> dispose());
        Rectangle screen = parent.getGraphicsConfiguration().getBounds();
        setSize(Math.min(730, screen.width - 30), Math.min(770, screen.height - 85));
        setMinimumSize(new Dimension(520, 340));
        setLocationRelativeTo(parent);
        preview(0);
    }

    static PageFormat a4() {
        Paper paper = new Paper();
        paper.setSize(595.28, 841.89);
        paper.setImageableArea(36, 36, paper.getWidth() - 72, paper.getHeight() - 72);
        PageFormat format = new PageFormat();
        format.setPaper(paper);
        return format;
    }

    private void preview(int requested) {
        run(() -> render(requested), image -> {
            if (image == null) {
                lastPage = currentPage;
                status.setText("Đã đến trang cuối.");
                return;
            }
            currentPage = requested;
            page.setText(null);
            page.setIcon(new ImageIcon(image));
            scroll.getVerticalScrollBar().setValue(0);
            status.setText("A4  ·  Trang " + (currentPage + 1));
        });
    }

    private BufferedImage render(int index) throws PrinterException {
        double scale = 0.86;
        int width = (int) Math.ceil(paper.getWidth() * scale);
        int height = (int) Math.ceil(paper.getHeight() * scale);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.scale(scale, scale);
            return document.print(g, paper, index) == Printable.PAGE_EXISTS ? image : null;
        } finally {
            g.dispose();
        }
    }

    private void print() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName(document.getName());
            job.setPrintable(document, paper);
            PrintRequestAttributeSet settings = new HashPrintRequestAttributeSet();
            settings.add(MediaSizeName.ISO_A4);
            settings.add(OrientationRequested.PORTRAIT);
            if (job.printDialog(settings)) {
                run(() -> {
                    job.print(settings);
                    return null;
                }, ignored -> status.setText("Đã gửi lệnh in."));
            }
        } catch (RuntimeException failure) {
            status.setText("Không mở được hộp thoại máy in.");
        }
    }

    private <T> void run(Callable<T> job, Consumer<T> success) {
        previous.setEnabled(false);
        next.setEnabled(false);
        printButton.setEnabled(false);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return job.call();
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    return;
                }
                try {
                    success.accept(get());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    status.setText("Thao tác in đã bị gián đoạn.");
                } catch (ExecutionException failure) {
                    status.setText("Không thực hiện được: " + failure.getCause().getMessage());
                }
                previous.setEnabled(currentPage > 0);
                next.setEnabled(page.getIcon() != null && currentPage != lastPage);
                printButton.setEnabled(page.getIcon() != null);
            }
        }.execute();
    }
}
