package master;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MasterServer {

    private static final int PORT = 5000;
    private static final List<WorkerConnection> workers = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("🟢 Master Server ξεκίνησε στη θύρα " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("🔗 Νέα σύνδεση από: " + socket.getInetAddress());

                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("❌ Σφάλμα στον Master Server: " + e.getMessage());
        }
    }

    public static void addWorker(WorkerConnection worker) {
        workers.add(worker);
        System.out.println("🧱 Νέος Worker καταχωρήθηκε. Σύνολο Workers: " + workers.size());
    }

    public static WorkerConnection getWorkerForStore(String storeName) {
        if (workers.isEmpty()) {
            System.err.println("❗ Δεν υπάρχουν διαθέσιμοι Workers!");
            return null;
        }

        int hash = Math.abs(storeName.hashCode());
        int index = hash % workers.size();
        return workers.get(index);
    }
}
