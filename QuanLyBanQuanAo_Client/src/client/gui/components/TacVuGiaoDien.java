package client.gui.components;

import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.AbstractButton;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import client.api.NgoaiLeGoiDichVu;

public final class TacVuGiaoDien {
    private TacVuGiaoDien() {
    }

    public static <T> void run(Component parent, AbstractButton button, Callable<T> job,
            Consumer<T> success) {
        run(button, job, success, message -> JOptionPane.showMessageDialog(parent, message, "Thông báo",
                JOptionPane.WARNING_MESSAGE));
    }

    public static <T> void run(AbstractButton button, Callable<T> job, Consumer<T> success,
            Consumer<String> error) {
        button.setEnabled(false);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return job.call();
            }

            @Override
            protected void done() {
                button.setEnabled(true);
                try {
                    success.accept(get());
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException failure) {
                    Throwable cause = failure.getCause();
                    String message = cause instanceof NgoaiLeGoiDichVu ? cause.getMessage()
                            : "Không kết nối/xử lý được máy chủ. Kiểm tra ServerMain và cấu hình mạng.";
                    error.accept(message);
                }
            }
        }.execute();
    }
}
