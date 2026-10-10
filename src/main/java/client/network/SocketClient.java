package client.network;

import java.io.*;
import java.net.*;
import java.util.UUID;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import common.protocol.*;

/** Một request tại một thời điểm; không tự gửi lại thao tác sau lỗi mạng. */
public final class SocketClient implements AutoCloseable {
    private final String host;
    private final int port;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    public SocketClient(String host, int port) { this.host = host; this.port = port; }
    public synchronized Response<JsonNode> send(Action action, String token, Object payload) throws IOException {
        try {
            if (socket == null) {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 5000);
                socket.setSoTimeout(15000);
                socket.setTcpNoDelay(true);
                input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            }
            String id = UUID.randomUUID().toString();
            FrameIO.write(output, JsonCodec.mapper().writeValueAsBytes(new Request<>(id, action, token, payload)));
            byte[] frame = FrameIO.read(input);
            if (frame == null) { throw new EOFException("Máy chủ đã ngắt kết nối."); }
            Response<JsonNode> response = JsonCodec.mapper().readValue(frame, new TypeReference<Response<JsonNode>>() { });
            if (response == null || !id.equals(response.getRequestId()) || response.getErrorCode() == null
                    || (response.isSuccess() != (response.getErrorCode() == ErrorCode.NONE))) {
                throw new IOException("Phản hồi máy chủ không hợp lệ.");
            }
            return response;
        } catch (IOException | RuntimeException failure) { close(); throw failure; }
    }
    @Override public synchronized void close() {
        if (socket != null) { try { socket.close(); } catch (IOException ignored) { } }
        socket = null; input = null; output = null;
    }
}
