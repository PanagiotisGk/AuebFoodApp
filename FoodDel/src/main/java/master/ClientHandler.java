package master;

import common.model.Request;
import common.model.Response;
import common.model.SearchFilters;
import common.model.Store;
import common.model.Order;
import common.model.UpdateProductRequest;

import java.io.*;
import java.net.Socket;
import java.util.*;

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

            Request firstRequest = (Request) in.readObject();
            System.out.println("📩 Λήφθηκε request τύπου: " + firstRequest.getType());

            if ("REGISTER_WORKER".equals(firstRequest.getType())) {
                String workerId = "Worker-" + MasterServer.getNextWorkerId();

                WorkerConnection worker = new WorkerConnection(socket, out, in, workerId);
                MasterServer.addWorker(worker);

                System.out.println("📍 Νέος Worker καταχωρήθηκε με ID: " + workerId);
                out.writeObject(new Response(true, "Εγγραφή επιτυχής", workerId));
                out.flush();
                return;

            }

            // Από εδώ και κάτω διαχείριση για Manager ή άλλους clients
            Request request = firstRequest;

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
                        out.writeObject(resp);
                        break;

                    case "ADD_ORDER":
                        Order order = (Order) request.getPayload();
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
                            List<?> storesList = (List<?>) payload;
                            for (Object obj : storesList) {
                                if (obj instanceof Store) {
                                    processStore((Store) obj, out);
                                }
                            }
                        } else if (payload instanceof Store) {
                            processStore((Store) payload, out);
                        }
                        break;

                    case "SEARCH_5KM_RANGE":
                        SearchFilters filters = (SearchFilters) request.getPayload();
                        List<Store> resultsForSearch5kmRange = new ArrayList<>();
                        for (WorkerConnection w : MasterServer.getWorkers()) {
                            w.sendRequest(request);
                            Response workerResp = (Response) w.getInputStream().readObject();
                            if (workerResp.isSuccess()) {
                                resultsForSearch5kmRange.addAll((List<Store>) workerResp.getData());
                            }
                        }
                        out.writeObject(new Response(true, "Αποτελέσματα κοντινών καταστημάτων", resultsForSearch5kmRange));
                        break;

                    case "FILTER_STORES":
                        SearchFilters filterCriteria = (SearchFilters) request.getPayload();
                        List<Store> filteredStores = new ArrayList<>();
                        for (WorkerConnection w : MasterServer.getWorkers()) {
                            w.sendRequest(request);
                            Response workerResp = (Response) w.getInputStream().readObject();
                            if (workerResp.isSuccess()) {
                                filteredStores.addAll((List<Store>) workerResp.getData());
                            }
                        }
                        out.writeObject(new Response(true, "Αποτελέσματα φιλτραρίσματος", filteredStores));
                        break;

                    case "CATEGORY_REVENUE":
                        Map<String, Double> totalRevenue = new HashMap<>();
                        for (WorkerConnection w : MasterServer.getWorkers()) {
                            w.sendRequest(request);
                            Response workerResp = (Response) w.getInputStream().readObject();
                            if (workerResp.isSuccess()) {
                                Map<?, ?> rawMap = (Map<?, ?>) workerResp.getData();
                                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                                    String category = (String) entry.getKey();
                                    Double revenue = ((Number) entry.getValue()).doubleValue();
                                    totalRevenue.put(category, totalRevenue.getOrDefault(category, 0.0) + revenue);
                                }
                            }
                        }
                        out.writeObject(new Response(true, "💰 Συγκεντρωτικά έσοδα ανά κατηγορία", totalRevenue));
                        break;

                    case "CATEGORY_PRODUCT_SALES":
                        String category = (String) request.getPayload();
                        Map<String, Double> finalMap = new HashMap<>();
                        double total = 0.0;
                        for (WorkerConnection w : MasterServer.getWorkers()) {
                            w.sendRequest(request);
                            Response workerResp = (Response) w.getInputStream().readObject();
                            if (workerResp.isSuccess()) {
                                Map<?, ?> data = (Map<?, ?>) workerResp.getData();
                                for (Map.Entry<?, ?> entry : data.entrySet()) {
                                    String name = (String) entry.getKey();
                                    double revenue = ((Number) entry.getValue()).doubleValue();
                                    if (name.equals("total")) total += revenue;
                                    else finalMap.put(name, finalMap.getOrDefault(name, 0.0) + revenue);
                                }
                            }
                        }
                        finalMap.put("total", total);
                        out.writeObject(new Response(true, "📊 Συγκεντρωτικά έσοδα κατηγορίας " + category, finalMap));
                        break;

                    case "PRODUCT_SALES":
                        Map<String, Map<String, Object>> totalSales = new HashMap<>();
                        for (WorkerConnection w : MasterServer.getWorkers()) {
                            w.sendRequest(request);
                            Response workerResp = (Response) w.getInputStream().readObject();
                            if (workerResp.isSuccess()) {
                                Map<?, ?> data = (Map<?, ?>) workerResp.getData();
                                for (Map.Entry<?, ?> entry : data.entrySet()) {
                                    String product = (String) entry.getKey();
                                    Map<?, ?> values = (Map<?, ?>) entry.getValue();
                                    int qty = ((Number) values.get("quantity")).intValue();
                                    double rev = ((Number) values.get("revenue")).doubleValue();
                                    Map<String, Object> stats = totalSales.computeIfAbsent(product, k -> new HashMap<>());
                                    stats.put("quantity", (int) stats.getOrDefault("quantity", 0) + qty);
                                    stats.put("revenue", (double) stats.getOrDefault("revenue", 0.0) + rev);
                                }
                            }
                        }
                        out.writeObject(new Response(true, "📊 Πωλήσεις ανά προϊόν", totalSales));
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

        WorkerConnection chosenWorker = MasterServer.getNextWorker();

        if (chosenWorker == null) {
            out.writeObject(new Response(false, "❗ Δεν υπάρχει διαθέσιμος Worker", null));
            return;
        }

        // ✅ Στείλε το κατάστημα στον Worker
        chosenWorker.sendRequest(new Request("ADD_STORE", store));

        // ✅ Καταχώρισε ποιος Worker έχει αυτό το store
        MasterServer.registerStoreForWorker(store.getStoreName(), chosenWorker);

        System.out.println("📦 Κατάστημα '" + store.getStoreName() + "' ανατέθηκε στον Worker: " + chosenWorker.getWorkerId());

        try {
            Response workerResp = (Response) chosenWorker.getInputStream().readObject();
            out.writeObject(workerResp);
        } catch (ClassNotFoundException e) {
            out.writeObject(new Response(false, "❌ Σφάλμα κατά την ανάγνωση της απάντησης του Worker", null));
        }
    }

}
