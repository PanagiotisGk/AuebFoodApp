package master;

import common.model.Request;
import common.model.Response;
import common.model.SearchFilters;
import common.model.Store;
import common.model.Order;
import common.model.UpdateProductRequest;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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

                    case "GET_PRODUCTS":
                        String storeName = (String) request.getPayload();
                        WorkerConnection worker = MasterServer.getWorkerForStore(storeName);
                        if (worker == null) {
                            out.writeObject(new Response(false, "❌ Δεν υπάρχει διαθέσιμος Worker", null));
                            break;
                        }

                        worker.sendRequest(request);
                        Response resp = (Response) worker.getInputStream().readObject();
                        System.out.println("⬅️ Worker Response: success=" + resp.isSuccess() + ", data=" + resp.getData());
                        out.writeObject(resp);
                        break;


                    case "ADD_ORDER":
                        Order order = (Order) request.getPayload();
                        System.out.println("📦 Παραγγελία προς: " + order.getStoreName());

                        WorkerConnection worker1 = MasterServer.getWorkerForStore(order.getStoreName());
                        if (worker1 == null) {
                            out.writeObject(new Response(false, "❗ Δεν υπάρχει διαθέσιμος Worker", null));
                            break;
                        }

                        worker1.sendRequest(request);
                        Response orderResp = (Response) worker1.getInputStream().readObject();
                        out.writeObject(orderResp);
                        break;

                    case "UPDATE_PRODUCTS":
                        String storeName1 = ((UpdateProductRequest) request.getPayload()).getStoreName();
                        WorkerConnection chosenWorker = MasterServer.getWorkerForStore(storeName1);
                        if (chosenWorker == null) {
                            out.writeObject(new Response(false, "❗ Δεν υπάρχει διαθέσιμος Worker", null));
                            break;
                        }

                        chosenWorker.sendRequest(request);
                        Response updateResp = (Response) chosenWorker.getInputStream().readObject();
                        out.writeObject(updateResp);
                        break;



                    case "ADD_STORE":
                        Object payload = request.getPayload();

                        if (payload instanceof List<?>) {
                            // Αν το payload είναι λίστα, κάνουμε cast σε List<Store>
                            List<?> storesList = (List<?>) payload;
                            for (Object obj : storesList) {
                                if (obj instanceof Store) {
                                    processStore((Store) obj, out);
                                } else {
                                    System.err.println("❌ Σφάλμα: Αντικείμενο στη λίστα δεν είναι Store!");
                                }
                            }
                        } else if (payload instanceof Store) {
                            // Αν το payload είναι μεμονωμένο Store
                            processStore((Store) payload, out);
                        } else {
                            System.err.println("❌ Σφάλμα: Άγνωστος τύπος δεδομένων στο ADD_STORE request!");
                        }
                        break;
                    case "SEARCH_5KM_RANGE":
                        SearchFilters filters = (SearchFilters) request.getPayload();
                        List<Store> resultsForSearch5kmRange = new ArrayList<>();
                    
                        for (WorkerConnection worker2 : MasterServer.getWorkers()) {
                            worker2.sendRequest(request);
                            Response workerResponse = (Response) worker2.getInputStream().readObject();
                    
                            if (workerResponse.isSuccess()) {
                                List<Store> partial = (List<Store>) workerResponse.getData();
                                resultsForSearch5kmRange.addAll(partial);
                            }
                        }
                        out.writeObject(new Response(true, "Αποτελέσματα κοντινών καταστημάτων", resultsForSearch5kmRange));
                        break;
                    case "FILTER_STORES":                 
                        SearchFilters filterCriteria = (SearchFilters) request.getPayload();
                        List<Store> filteredStores = new ArrayList<>();
                    
                        for (WorkerConnection worker2 : MasterServer.getWorkers()) {
                            worker2.sendRequest(request);
                            Response workerResponse = (Response) worker2.getInputStream().readObject();
                    
                            if (workerResponse.isSuccess()) {
                                List<Store> partialResults = (List<Store>) workerResponse.getData();
                                filteredStores.addAll(partialResults);
                            }
                        }
                        out.writeObject(new Response(true, "Αποτελέσματα φιλτραρίσματος", filteredStores));
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
    private void processStore(Store store, ObjectOutputStream out) throws IOException {
        System.out.println("📦 Επεξεργασία store: " + store.getStoreName());

        WorkerConnection chosenWorker = MasterServer.getWorkerForStore(store.getStoreName());
        if (chosenWorker == null) {
            out.writeObject(new Response(false, "❗ Δεν υπάρχει διαθέσιμος Worker", null));
            return;
        }

        chosenWorker.sendRequest(new Request("ADD_STORE", store));
        Response workerResp;
        try {
            workerResp = (Response) chosenWorker.getInputStream().readObject();
        } catch (ClassNotFoundException e) {
            workerResp = new Response(false, "❌ Σφάλμα κατά την ανάγνωση της απάντησης του Worker", null);
        }

        out.writeObject(workerResp);
    }

}