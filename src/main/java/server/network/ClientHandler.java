package server.network;

import java.io.*;
import java.net.Socket;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import common.protocol.*;
import server.dispatcher.RequestDispatcher;

public final class ClientHandler implements Runnable {
    private final Socket socket;
    private final RequestDispatcher dispatcher;
    private final Runnable onClose;
    public ClientHandler(Socket socket, RequestDispatcher dispatcher, Runnable onClose) {
        this.socket = socket; this.dispatcher = dispatcher; this.onClose = onClose;
    }
    @Override public void run() {
        try (Socket connection = socket;
             DataInputStream input = new DataInputStream(new BufferedInputStream(connection.getInputStream()));
             DataOutputStream output = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()))) {
            connection.setSoTimeout(60000); connection.setTcpNoDelay(true);
            int failedLogins = 0;
            while (!Thread.currentThread().isInterrupted()) {
                byte[] bytes = FrameIO.read(input);
                if (bytes == null) { return; }
                Request<JsonNode> request;
                try {
                    request = JsonCodec.mapper().readValue(bytes, new TypeReference<Request<JsonNode>>() { });
                } catch (com.fasterxml.jackson.core.JsonProcessingException invalid) {
                    FrameIO.write(output, JsonCodec.mapper().writeValueAsBytes(
                            Response.failure(null, ErrorCode.INVALID_REQUEST, "JSON không hợp lệ.")));
                    return;
                }
                Response<?> response = dispatcher.dispatch(request);
                FrameIO.write(output, JsonCodec.mapper().writeValueAsBytes(response));
                if (request != null && request.getAction() == Action.DANG_NHAP && !response.isSuccess()
                        && ++failedLogins >= 5) { return; }
            }
        } catch (IOException ignored) {
            // EOF giữa frame, timeout hoặc client đóng socket: kết thúc kết nối.
        } finally { onClose.run(); }
    }
}
