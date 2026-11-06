package client.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;
import common.protocol.TruyenKhungTin;
import common.protocol.YeuCau;

/** Một request tại một thời điểm; không tự gửi lại thao tác sau lỗi mạng. */
public final class KetNoiMayChu implements AutoCloseable {
    // Client tự mở kết nối mới trước khi handler phía server hết hạn ở mốc 60 giây.
    private static final long DEFAULT_IDLE_RECONNECT_NANOS = TimeUnit.SECONDS.toNanos(45);
    private final String host;
    private final int port;
    private final long idleReconnectAfterNanos;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private long lastResponseNanos;

    public KetNoiMayChu(String host, int port) {
        this(host, port, DEFAULT_IDLE_RECONNECT_NANOS);
    }

    KetNoiMayChu(String host, int port, long idleReconnectAfterNanos) {
        if (idleReconnectAfterNanos <= 0) {
            throw new IllegalArgumentException("Idle reconnect interval must be positive.");
        }
        this.host = host;
        this.port = port;
        this.idleReconnectAfterNanos = idleReconnectAfterNanos;
    }

    public synchronized PhanHoi<JsonNode> send(HanhDong action, String token, Object payload)
            throws IOException {
        try {
            if (socket != null && lastResponseNanos != 0
                    && System.nanoTime() - lastResponseNanos >= idleReconnectAfterNanos) {
                close();
            }
            if (socket == null) {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 5000);
                socket.setSoTimeout(15000);
                socket.setTcpNoDelay(true);
                input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            }
            String id = UUID.randomUUID().toString();
            TruyenKhungTin.write(output,
                    BoMaHoaJson.mapper().writeValueAsBytes(new YeuCau<>(id, action, token, payload)));
            byte[] frame = TruyenKhungTin.read(input);
            if (frame == null) {
                throw new EOFException("Máy chủ đã ngắt kết nối.");
            }
            PhanHoi<JsonNode> response = BoMaHoaJson.mapper().readValue(frame,
                    new TypeReference<PhanHoi<JsonNode>>() {
                    });
            if (response == null || !id.equals(response.getRequestId()) || response.getErrorCode() == null
                    || (response.isSuccess() != (response.getErrorCode() == MaLoi.NONE))) {
                throw new IOException("Phản hồi máy chủ không hợp lệ.");
            }
            lastResponseNanos = System.nanoTime();
            return response;
        } catch (IOException | RuntimeException failure) {
            close();
            throw failure;
        }
    }

    @Override
    public synchronized void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        socket = null;
        input = null;
        output = null;
        lastResponseNanos = 0;
    }
}
