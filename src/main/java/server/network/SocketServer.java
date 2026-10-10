package server.network;

import java.io.IOException;
import java.net.*;
import java.util.Set;
import java.util.concurrent.*;
import server.dispatcher.RequestDispatcher;

public final class SocketServer implements AutoCloseable {
    private final RequestDispatcher dispatcher;
    private final ServerSocket listener = new ServerSocket();
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(16, 16, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(64));
    private final Set<Socket> clients = ConcurrentHashMap.newKeySet();
    private final CountDownLatch stopped = new CountDownLatch(1);
    private volatile boolean closed;
    private boolean started;
    public SocketServer(String host, int port, RequestDispatcher dispatcher) throws IOException {
        this.dispatcher = dispatcher;
        try { listener.bind(new InetSocketAddress(host, port), 64); }
        catch (IOException failure) { listener.close(); workers.shutdownNow(); throw failure; }
    }
    public synchronized void start() {
        if (started || closed) { throw new IllegalStateException("Server already started or closed."); }
        started = true;
        Thread acceptor = new Thread(this::acceptLoop, "socket-acceptor");
        acceptor.setDaemon(true); acceptor.start();
    }
    public int getPort() { return listener.getLocalPort(); }
    public void await() throws InterruptedException { stopped.await(); }
    private void acceptLoop() {
        try {
            while (!closed) {
                Socket client = listener.accept();
                clients.add(client);
                try { workers.execute(new ClientHandler(client, dispatcher, () -> clients.remove(client))); }
                catch (RejectedExecutionException full) { closeSocket(client); clients.remove(client); }
            }
        } catch (IOException ignored) {
            // listener.close() đánh thức accept() khi shutdown.
        } finally { close(); }
    }
    @Override public synchronized void close() {
        if (closed) { return; }
        closed = true;
        try { listener.close(); } catch (IOException ignored) { }
        clients.forEach(SocketServer::closeSocket);
        clients.clear(); workers.shutdownNow();
        try { workers.awaitTermination(5, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        stopped.countDown();
    }
    private static void closeSocket(Socket socket) { try { socket.close(); } catch (IOException ignored) { } }
}
