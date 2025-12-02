import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*; // For thread-safe collections and thread management

//the brainStromServer manages all client connections
//  and coordinates communication between multiple users

public class Main {
    public static void main(String[] args) {
        BrainstormServer server = new BrainstormServer();

        // Shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.shutdown();
        }));

        server.start();

    }
}