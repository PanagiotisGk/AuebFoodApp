package worker;

import common.model.Request;
import common.model.Response;
import common.model.Store;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class WorkerNode {

    private final Map<String, Store> storeMap = new HashMap<>();

    public void start(String masterHost, int masterPort) {
        try {
            Socket socket = new Socket(masterHost, masterPort);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            System.out.println("🟢 Συνδέθηκα με τον Master στη θύρα " + masterPort);

            Request register = new Request("REGISTER_WORKER", null);
            out.writeObject(register);
            out.flush();
            System.out.println("📡 Δήλωσα τον εαυτό μου ως Worker στον Master");

            while (true) {
                Request request = (Request) in.readObject();
                System.out.println("📩 Worker έλαβε αίτημα: " + request.getType());

                switch (request.getType()) {
                    case "ADD_STORE":
                        Store store = (Store) request.getPayload();
                        storeMap.put(store.getStoreName(), store);
                        System.out.println("✅ Αποθηκεύτηκε το κατάστημα: " + store.getStoreName());

                        Response ok = new Response(true, "Το κατάστημα αποθηκεύτηκε", null);
                        out.writeObject(ok);
                        out.flush();
                        break;

                    default:
                        Response error = new Response(false, "Άγνωστο αίτημα", null);
                        out.writeObject(error);
                        out.flush();
                        break;
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Worker σφάλμα: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        WorkerNode worker = new WorkerNode();
        worker.start("localhost", 5000);
    }
}
