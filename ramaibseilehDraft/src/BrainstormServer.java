import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class BrainstormServer {
    private static final int PORT = 8080;
    private ServerSocket serverSocket;
    private CanvasState canvasState;
    private List<ClientHandler> clients;
    private ExecutorService threadPool;
    private boolean running;

    public BrainstormServer() {
        this.canvasState = new CanvasState();
        this.clients = new CopyOnWriteArrayList<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.running = true;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("BrainstormServer started on port " + PORT);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress());

                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    clients.add(handler);
                    threadPool.execute(handler);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }

    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender && client.isConnected()) {
                client.sendMessage(message);
            }
        }
    }

    public void broadcastToAll(String message) {
        for (ClientHandler client : clients) {
            if (client.isConnected()) {
                client.sendMessage(message);
            }
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client disconnected. Active clients: " + clients.size());
    }

    public CanvasState getCanvasState() {
        return canvasState;
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler client : clients) {
                client.disconnect();
            }
            threadPool.shutdown();
            threadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
}