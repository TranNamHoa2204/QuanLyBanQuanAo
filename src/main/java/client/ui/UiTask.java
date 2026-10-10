package client.ui;

import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import client.api.ApiException;

public final class UiTask {
    private UiTask() { }
    public static <T> void run(Component parent, JButton button, Callable<T> job, Consumer<T> success) {
        button.setEnabled(false);
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return job.call(); }
            @Override protected void done() {
                button.setEnabled(true);
                try { success.accept(get()); }
                catch (InterruptedException failure) { Thread.currentThread().interrupt(); }
                catch (ExecutionException failure) {
                    Throwable cause = failure.getCause();
                    String message = cause instanceof ApiException ? cause.getMessage()
                            : "Không kết nối/xử lý được máy chủ. Kiểm tra ServerMain và cấu hình mạng.";
                    JOptionPane.showMessageDialog(parent, message, "Thông báo", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }
}
