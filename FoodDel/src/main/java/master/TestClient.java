package master;

import common.model.Request;
import common.model.Response;

import java.io.*;
import java.net.Socket;

public class TestClient {

    public static void main(String[] args) {
        String serverAddress = "localhost"; // ή IP αν τρέχει αλλού
        int port = 5000;

        try (
                Socket socket = new Socket(serverAddress, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            System.out.println("🔵 Συνδέθηκα με τον Master Server!");

            // Δημιουργία dummy request
            Request pingRequest = new Request("PING", null);
            out.writeObject(pingRequest);
            System.out.println("📤 Έστειλα request: " + pingRequest.getType());

            // Λήψη response
            Response response = (Response) in.readObject();
            System.out.println("📥 Λήφθηκε απάντηση: " + response.getMessage());

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Σφάλμα στον Test Client: " + e.getMessage());
        }
    }
}
