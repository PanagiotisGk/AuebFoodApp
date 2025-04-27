package master;

import common.model.Request;
import common.model.Response;
import common.model.SearchFilters;
import common.model.Store;
import common.model.Order;
import common.model.RateStoreRequest;
import common.model.UpdateProductRequest;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    private boolean isWorker = false;

    @Override
    public void run() {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;


        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            Request firstRequest = (Request) in.readObject();
            System.out.println("Λήφθηκε request τύπου: " + firstRequest.getType());

            if ("REGISTER_WORKER".equals(firstRequest.getType())) {
                isWorker = true;
                String workerId = "Worker-" + MasterServer.getNextWorkerId();

                WorkerConnection worker = new WorkerConnection(socket, out, in, workerId);
                MasterServer.addWorker(worker);

                System.out.println("Νέος Worker καταχωρήθηκε με ID: " + workerId);
                out.writeObject(new Response(true, "Εγγραφή επιτυχής", workerId));
                out.flush();
                return;

            }

            // Apo edw kai katw diaxeirisi aitimatwn gia Manager h allous clients
            Request request = firstRequest;

            while (true) {
                switch (request.getType()) {
                    // Epistrofi proiontwn
                    case "GET_PRODUCTS":
                        String storeName = (String) request.getPayload();
                        WorkerConnection worker = MasterServer.getWorkerForStore(storeName);
                        if (worker == null) {
                            out.writeObject(new Response(false, "Δεν υπάρχει διαθέσιμος Worker", null));
                            break;
                        }
                        worker.sendRequest(request);
                        Response resp = (Response) worker.getInputStream().readObject();
                        out.writeObject(resp);
                        break;
                    // Kataxwrisi paraggelias
                    case "ADD_ORDER":
                        Order order = (Order) request.getPayload();
                        WorkerConnection worker1 = MasterServer.getWorkerForStore(order.getStoreName());
                        if (worker1 == null) {
                            out.writeObject(new Response(false, "Δεν υπάρχει διαθέσιμος Worker", null));
                            break;
                        }
                        worker1.sendRequest(request);
                        Response orderResp = (Response) worker1.getInputStream().readObject();
                        out.writeObject(orderResp);
                        break;
                    // Enimerwsi proiontwn
                    case "UPDATE_PRODUCTS":
                        String storeName1 = ((UpdateProductRequest) request.getPayload()).getStoreName();
                        WorkerConnection chosenWorker = MasterServer.getWorkerForStore(storeName1);
                        if (chosenWorker == null) {
                            out.writeObject(new Response(false, "Δεν υπάρχει διαθέσιμος Worker", null));
                            break;
                        }
                        chosenWorker.sendRequest(request);
                        Response updateResp = (Response) chosenWorker.getInputStream().readObject();
                        out.writeObject(updateResp);
                        break;
                    // Prosthiki katastimatos
                    case "ADD_STORE":
                        // Break ean den yparxei diathesimos worker
                        WorkerConnection availableWorker = MasterServer.getAnyAvailableWorker();
                        if (availableWorker == null) {
                            out.writeObject(new Response(false, "Δεν υπάρχει διαθέσιμος Worker", null));
                            break;
                        }
                        boolean remainingWorkers = MasterServer.removeWorkersForUse();
                        if (remainingWorkers == false){
                            out.writeObject(new Response(false, "Δεν υπάρχει διαθέσιμος Worker για να του προσθέσουμε κατάστημα", null));
                            break;
                        }
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
                    // Evresi olwn twn kataxwrimenwn katastimatwn
                    case "SEARCH_ALL_STORES":
                        List<Store> resultsForSearchAllStores = new ArrayList<>();
                        for (WorkerConnection w : MasterServer.getWorkers()) {
                            w.sendRequest(request);
                            Response workerResp = (Response) w.getInputStream().readObject();
                            if (workerResp.isSuccess()) {
                                resultsForSearchAllStores.addAll((List<Store>) workerResp.getData());
                            }
                        }
                        out.writeObject(new Response(true, "Αποτελέσματα όλων των καταχωρημένων καταστημάτων", resultsForSearchAllStores));
                        break;
                    // Evresi katastimatwn se aktina 5km
                    case "SEARCH_5KM_RANGE":
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
                    // Evresi katastimatwn me vasi custom filtra
                    case "FILTER_STORES":
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
                    // Emfanisi esodwn ana katigoria katastimatos
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
                        out.writeObject(new Response(true, " Συγκεντρωτικά έσοδα ανά κατηγορία", totalRevenue));
                        break;
                    // Emfanisi esodwon ana katigoria proiontos
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
                        out.writeObject(new Response(true, " Συγκεντρωτικά έσοδα προϊόντος " + category, finalMap));
                        break;
                    //  Emfanisi polisewn ana proion
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
                        out.writeObject(new Response(true, " Πωλήσεις ανά προϊόν", totalSales));
                        break;

                    //  Axiologisi katastimatwn
                    case "RATE_STORE":
                        Object payloadRate = request.getPayload();
                        if (payloadRate instanceof RateStoreRequest) {
                            RateStoreRequest rateRequest = (RateStoreRequest) payloadRate;
                            processStoreRating(rateRequest, out);
                        }
                        break;

                    default:
                        out.writeObject(new Response(false, "Άγνωστο αίτημα", null));
                        break;
                }
                request = (Request) in.readObject();
                System.out.println("Νέο request τύπου: " + request.getType());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Σύνδεση έκλεισε: " + e.getMessage());
        } finally {
            // 🛑 Mono an DEN einai Worker kleinoume to socket
            if (!isWorker) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                        System.out.println("🧹 Έκλεισα το socket του ClientHandler: " + socket.getRemoteSocketAddress());
                    }
                } catch (IOException e) {
                    System.out.println("❗ Σφάλμα στο κλείσιμο του socket: " + e.getMessage());
                }
            } else {
                System.out.println("🔵 Worker ClientHandler παραμένει ενεργός.");
            }
            System.out.println("🧨 ClientHandler για " + socket.getRemoteSocketAddress() + " τερματίστηκε.");
        }
    }

    private void processStore(Store store, ObjectOutputStream out) throws IOException {
        System.out.println(" Επεξεργασία store: " + store.getStoreName());

        WorkerConnection chosenWorker = MasterServer.getNextWorker();

        if (chosenWorker == null) {
            out.writeObject(new Response(false, " Δεν υπάρχει διαθέσιμος Worker", null));
            return;
        }

        //  Stelnoume to katastima ston Worker
        chosenWorker.sendRequest(new Request("ADD_STORE", store));

        //  Kataxoroume poios Worker exei auto to store
        MasterServer.registerStoreForWorker(store.getStoreName(), chosenWorker);

        System.out.println(" Κατάστημα '" + store.getStoreName() + "' ανατέθηκε στον Worker: " + chosenWorker.getWorkerId());

        try {
            Response workerResp = (Response) chosenWorker.getInputStream().readObject();
            out.writeObject(workerResp);
        } catch (ClassNotFoundException e) {
            out.writeObject(new Response(false, " Σφάλμα κατά την ανάγνωση της απάντησης του Worker", null));
        }
    }

    // Methodos gia tin axiologisi katastimatos
    private void processStoreRating(RateStoreRequest rateRequest, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        String storeName = rateRequest.getStoreName();
        WorkerConnection worker = MasterServer.getWorkerForStore(storeName);
    
        if (worker == null) {
            out.writeObject(new Response(false, "Το κατάστημα δεν βρέθηκε", null));
            out.flush();
            return;
        }
    
        worker.sendRequest(new Request("RATE_STORE", rateRequest));
        Response workerResponse = worker.readResponse();
        out.writeObject(workerResponse);
        out.flush();
    }

}
