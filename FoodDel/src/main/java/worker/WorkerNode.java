package worker;

import common.model.Request;
import common.model.Response;
import common.model.SearchFilters;
import common.model.Store;
import common.model.Order;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;


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
                        // orderMap.computeIfAbsent(store, k -> new ArrayList<>()).add(order);

                        // // Έλεγχος αν υπάρχει το κατάστημα
                        // if (!storeMap.containsKey(store)) {
                        //     System.out.println("❌ Παραγγελία για άγνωστο κατάστημα: " + store);
                        //     Response fail = new Response(false, "Το κατάστημα δεν υπάρχει στον Worker", null);
                        //     out.writeObject(fail);
                        //     out.flush();
                        //     break;
                        // }

                        // // Ενημέρωση εσόδων καταστήματος
                        // storeMap.get(store).addRevenue(order.getTotalCost());

                        // Έλεγχος ύπαρξης καταστήματος
                        Store storeObj = storeMap.get(store);
                        if (storeObj == null) {
                            System.out.println("❌ Παραγγελία για άγνωστο κατάστημα: " + store);
                            Response fail = new Response(false, "Το κατάστημα δεν υπάρχει στον Worker", null);
                            out.writeObject(fail);
                            out.flush();
                            break;
                        }

                        // 🚨 Συγχρονισμός σε κάθε store ξεχωριστά
                        synchronized (storeObj) {
                            // Καταγραφή παραγγελίας
                            orderMap.computeIfAbsent(store, k -> new ArrayList<>()).add(order);

                            // Ενημέρωση εσόδων
                            storeObj.addRevenue(order.getTotalCost());

                            System.out.println("📥 Παραγγελία καταχωρήθηκε για: " + store);
                        }

                        System.out.println("📥 Αποθηκεύτηκε παραγγελία για το κατάστημα: " + store);
                        Response okOrder = new Response(true, "Η παραγγελία καταχωρήθηκε", null);
                        out.writeObject(okOrder);
                        out.flush();
                        break;

                    case "ADD_STORE":
                        Store store1 = (Store) request.getPayload();
                        String storeName = store1.getStoreName();

                        // if (storeMap.containsKey(storeName)) {
                        //     System.out.println("⚠️ Το κατάστημα ήδη υπάρχει: " + storeName);
                        //     Response alreadyExists = new Response(false, "Το κατάστημα υπάρχει ήδη", null);
                        //     out.writeObject(alreadyExists);
                        //     out.flush();
                        //     break;
                        // }

                        // storeMap.put(storeName, store1);

                        synchronized (storeMap) {
                            if (storeMap.containsKey(storeName)) {
                                System.out.println("⚠️ Το κατάστημα ήδη υπάρχει: " + storeName);
                                Response alreadyExists = new Response(false, "Το κατάστημα υπάρχει ήδη", null);
                                out.writeObject(alreadyExists);
                                out.flush();
                                break;
                            }
                    
                            storeMap.put(storeName, store1);
                        }

                        System.out.println("✅ Αποθηκεύτηκε το κατάστημα: " + storeName);
                        Response ok = new Response(true, "Το κατάστημα αποθηκεύτηκε", null);
                        out.writeObject(ok);
                        out.flush();
                        break;
                    case "SEARCH_5KM_RANGE":
                        SearchFilters filtersFor5kmRange = (SearchFilters) request.getPayload();
                        List<Store> nearbyStores = new ArrayList<>();

                        for (Store store5km : storeMap.values()) {
                            double distance = distanceInKm(
                                filtersFor5kmRange.getClientLatitude(),
                                filtersFor5kmRange.getClientLongitude(),
                                    store5km.getLatitude(),
                                    store5km.getLongitude());

                            if (distance <= 5) {
                                nearbyStores.add(store5km);
                            }
                        }
                        System.out.println("📦 Αποτελέσματα Search σε ακτίνα 5km: " + nearbyStores.size());
                        Response response = new Response(true, "Καταστήματα σε ακτίνα 5km", nearbyStores);
                        out.writeObject(response);
                        out.flush();
                        break;
                    case "FILTER_STORES":
                        System.out.println("📩 Worker έλαβε αίτημα: FILTER_STORES");
                        SearchFilters filtersForStores = (SearchFilters) request.getPayload();
                        List<Store> filteredStores = storeMap.values().stream()
                            .filter(store2 -> (filtersForStores.getFoodCategories() == null || filtersForStores.getFoodCategories().contains(store2.getFoodCategory())))
                            .filter(store2 -> store2.getStars() >= filtersForStores.getMinStars())
                            .filter(store2 -> (filtersForStores.getPriceCategories() == null || filtersForStores.getPriceCategories().contains(store2.getPriceCategory())))
                            .collect(Collectors.toList());

                        System.out.println("📦 Αποτελέσματα φίλτρων: " + filteredStores.size());
                        System.out.println("📦 Αποτελέσματα φίλτρων: " + filteredStores);
                        Response filterResponse = new Response(true, "Φιλτραρισμένα καταστήματα", filteredStores);
                        out.writeObject(filterResponse);
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

    // Util for distance in km
    private double distanceInKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    // main
    // εδώ δηλώνουμε την ip και την port από την οποία θα τρέχει ο κάθε worker
    public static void main(String[] args) {
        WorkerNode worker = new WorkerNode();
        worker.start("localhost", 5000);
    }
}
