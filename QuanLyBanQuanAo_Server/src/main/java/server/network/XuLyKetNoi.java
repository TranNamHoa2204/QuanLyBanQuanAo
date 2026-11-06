package server.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;
import common.protocol.TruyenKhungTin;
import common.protocol.YeuCau;
import server.dispatcher.DieuPhoiYeuCau;

public final class XuLyKetNoi implements Runnable {
    private final Socket socket;
    private final DieuPhoiYeuCau dispatcher;
    private final Runnable onClose;

    public XuLyKetNoi(Socket socket, DieuPhoiYeuCau dispatcher, Runnable onClose) {
        this.socket = socket;
        this.dispatcher = dispatcher;
        this.onClose = onClose;
    }

    @Override
    public void run() {
        try (Socket connection = socket;
                DataInputStream input = new DataInputStream(
                        new BufferedInputStream(connection.getInputStream()));
                DataOutputStream output = new DataOutputStream(
                        new BufferedOutputStream(connection.getOutputStream()))) {
            connection.setSoTimeout(60000);
            connection.setTcpNoDelay(true);
            int failedLogins = 0;
            while (!Thread.currentThread().isInterrupted()) {
                byte[] bytes = TruyenKhungTin.read(input);
                if (bytes == null) {
                    return;
                }
                YeuCau<JsonNode> request;
                try {
                    request = BoMaHoaJson.mapper().readValue(bytes, new TypeReference<YeuCau<JsonNode>>() {
                    });
                } catch (com.fasterxml.jackson.core.JsonProcessingException invalid) {
                    TruyenKhungTin.write(output, BoMaHoaJson.mapper().writeValueAsBytes(
                            PhanHoi.failure(null, MaLoi.INVALID_REQUEST, "JSON không hợp lệ.")));
                    return;
                }
                PhanHoi<?> response = dispatcher.dispatch(request);
                TruyenKhungTin.write(output, BoMaHoaJson.mapper().writeValueAsBytes(response));
                if (request != null && request.getAction() == HanhDong.DANG_NHAP && !response.isSuccess()
                        && ++failedLogins >= 5) {
                    return;
                }
            }
        } catch (IOException ignored) {
            // EOF giữa frame, timeout hoặc client đóng socket: kết thúc kết nối.
        } finally {
            onClose.run();
        }
    }
}
