package master;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import common.model.Request;
import common.model.Response;


public class MasterServer {

    private static final int PORT = 5000;
    private static int workerCounter = 1;
    private static final List<WorkerConnection> workers = new ArrayList<>();

    private static final Map<String, WorkerConnection> storeToWorkerMap = new HashMap<>();
    private static int roundRobinIndex = 0;



    public static void main(String[] args) {
        System.out.println("🟢 Master Server ξεκίνησε στη θύρα " + PORT);
        int workerCounter = 1;

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
        return storeToWorkerMap.get(storeName);
    }



    public static List<WorkerConnection> getWorkers() {
        return workers;
    }

    public static synchronized int getNextWorkerId() {
        return workerCounter++;
    }

    public static void registerStoreForWorker(String storeName, WorkerConnection worker) {
        storeToWorkerMap.put(storeName, worker);
    }
    public static WorkerConnection getAnyAvailableWorker() {
        if (workers.isEmpty()) return null;
        return workers.get(0); // ή Random, ή round-robin
    }

    public static synchronized WorkerConnection getNextWorker() {
        if (workers.isEmpty()) return null;

        WorkerConnection worker = workers.get(roundRobinIndex % workers.size());
        roundRobinIndex++;
        return worker;
    }



}