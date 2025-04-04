package worker;

import common.model.Request;
import common.model.Response;
import common.model.SearchFilters;
import common.model.Store;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                            .filter(store1 -> (filtersForStores.getFoodCategories() == null || filtersForStores.getFoodCategories().contains(store1.getFoodCategory())))
                            .filter(store1 -> store1.getStars() >= filtersForStores.getMinStars())
                            // .filter(store -> (filtersForStores.getPriceRanges() == null || filtersForStores.getPriceRanges().contains(store.getPriceRange())))
                            .collect(Collectors.toList());

                        System.out.println("📦 Αποτελέσματα φίλτρων: " + filteredStores.size());
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
    public static void main(String[] args) {
        WorkerNode worker = new WorkerNode();
        worker.start("localhost", 5000);
    }
}
