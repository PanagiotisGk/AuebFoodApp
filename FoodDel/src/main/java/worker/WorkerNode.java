package worker;

import common.model.Request;
import common.model.Response;
import common.model.Store;
import common.model.Order;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class WorkerNode {

    private final Map<String, Store> storeMap = new HashMap<>();

    private final Map<String, List<Order>> orderMap = new HashMap<>();



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

                    case "ADD_ORDER":
                        Order order = (Order) request.getPayload();
                        String store = order.getStoreName();
                        orderMap.computeIfAbsent(store, k -> new ArrayList<>()).add(order);

                        // Έλεγχος αν υπάρχει το κατάστημα
                        if (!storeMap.containsKey(store)) {
                            System.out.println("❌ Παραγγελία για άγνωστο κατάστημα: " + store);
                            Response fail = new Response(false, "Το κατάστημα δεν υπάρχει στον Worker", null);
                            out.writeObject(fail);
                            out.flush();
                            break;
                        }

                        // Ενημέρωση εσόδων καταστήματος
                        storeMap.get(store).addRevenue(order.getTotalCost());

                        System.out.println("📥 Αποθηκεύτηκε παραγγελία για το κατάστημα: " + store);
                        Response okOrder = new Response(true, "Η παραγγελία καταχωρήθηκε", null);
                        out.writeObject(okOrder);
                        out.flush();
                        break;

                    case "ADD_STORE":
                        Store store1 = (Store) request.getPayload();
                        String storeName = store1.getStoreName();

                        if (storeMap.containsKey(storeName)) {
                            System.out.println("⚠️ Το κατάστημα ήδη υπάρχει: " + storeName);
                            Response alreadyExists = new Response(false, "Το κατάστημα υπάρχει ήδη", null);
                            out.writeObject(alreadyExists);
                            out.flush();
                            break;
                        }

                        storeMap.put(storeName, store1);
                        System.out.println("✅ Αποθηκεύτηκε το κατάστημα: " + storeName);

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
