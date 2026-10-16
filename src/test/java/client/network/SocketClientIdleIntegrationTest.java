package client.network;

import static org.junit.Assert.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import common.protocol.Action;
import common.protocol.FrameIO;
import common.protocol.JsonCodec;
import common.protocol.Request;
import common.protocol.Response;

/** Kiểm tra vòng đời socket cục bộ; không kết nối database. */
public class SocketClientIdleIntegrationTest {
    @Test public void reconnectsAfterIdleWithoutReplayingThePreviousRequest() throws Exception {
        try (ServerSocket listener = new ServerSocket(0)) {
            listener.setSoTimeout(5000);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try (SocketClient client = new SocketClient("127.0.0.1", listener.getLocalPort(),
                    TimeUnit.MILLISECONDS.toNanos(100))) {
                Future<long[]> acceptedPorts = executor.submit(() -> {
                    Socket first = listener.accept();
                    try {
                        long firstPort = replyToOne(first);
                        try (Socket second = listener.accept()) {
                            return new long[] {firstPort, replyToOne(second)};
                        }
                    } finally { first.close(); }
                });

                assertTrue(client.send(Action.TRA_CUU_SAN_PHAM, "token", null).isSuccess());
                Thread.sleep(150);
                assertTrue(client.send(Action.TRA_CUU_SAN_PHAM, "token", null).isSuccess());
                long[] ports = acceptedPorts.get(5, TimeUnit.SECONDS);
                assertNotEquals("idle request should use a newly accepted TCP connection", ports[0], ports[1]);
            } finally { executor.shutdownNow(); }
        }
    }

    private static long replyToOne(Socket socket) throws Exception {
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        byte[] frame = FrameIO.read(input);
        Request<JsonNode> request = JsonCodec.mapper().readValue(frame,
                new TypeReference<Request<JsonNode>>() { });
        FrameIO.write(output, JsonCodec.mapper().writeValueAsBytes(Response.success(request.getRequestId(), null)));
        return socket.getPort();
    }
}
