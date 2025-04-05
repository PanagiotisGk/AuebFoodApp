package master;

import common.model.Request;
import common.model.Response;
import common.model.Store;
import common.model.Order;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            Request request = (Request) in.readObject();
            System.out.println("📩 Λήφθηκε request τύπου: " + request.getType());

            if ("REGISTER_WORKER".equals(request.getType())) {
                WorkerConnection worker = new WorkerConnection(socket, out, in);
                MasterServer.addWorker(worker);
                System.out.println("📍 Worker καταχωρήθηκε επιτυχώς.");
                // 🚫 Μην κάνεις return με try-with-resources — γιατί θα κλείσει socket!
                return;
            }

            while (true) {
                switch (request.getType()) {

                    case "ADD_ORDER":
                        Order order = (Order) request.getPayload();
                        System.out.println("📦 Παραγγελία προς: " + order.getStoreName());

                        WorkerConnection worker = MasterServer.getWorkerForStore(order.getStoreName());
                        if (worker == null) {
                            out.writeObject(new Response(false, "❗ Δεν υπάρχει διαθέσιμος Worker", null));
                            break;
                        }

                        worker.sendRequest(request);
                        Response orderResp = (Response) worker.getInputStream().readObject();
                        out.writeObject(orderResp);
                        break;


                    case "ADD_STORE":
                        Store store = (Store) request.getPayload();
                        System.out.println("📦 Επεξεργασία store: " + store.getStoreName());

                        WorkerConnection chosenWorker = MasterServer.getWorkerForStore(store.getStoreName());
                        if (chosenWorker == null) {
                            out.writeObject(new Response(false, "❗ Δεν υπάρχει διαθέσιμος Worker", null));
                            break;
                        }

                        chosenWorker.sendRequest(request);
                        Response workerResp = (Response) chosenWorker.getInputStream().readObject();
                        out.writeObject(workerResp);
                        break;

                    default:
                        out.writeObject(new Response(false, "Άγνωστο αίτημα", null));
                        break;
                }

                request = (Request) in.readObject();
                System.out.println("📩 Νέο request τύπου: " + request.getType());
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("🔴 Σύνδεση έκλεισε: " + e.getMessage());
        } finally {
            System.out.println("🧨 ClientHandler τελείωσε και έκλεισε socket: " + socket);
        }
    }
}
