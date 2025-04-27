package worker;

import common.model.*;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class WorkerNode {

    private final Map<String, Store> storeMap = new HashMap<>();
    private final Map<String, List<Order>> orderMap = new HashMap<>();
    private final Map<String, Integer> productSales = new HashMap<>();

    public void start(String masterHost, int masterPort) {
        try {
            System.out.println("Σύνδεση με τον Master στο " + masterHost + ":" + masterPort + " ...");

            Socket socket = new Socket(masterHost, masterPort);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(new Request("REGISTER_WORKER", null));
            out.flush();

            Response resp = (Response) in.readObject();
            String workerId = (String) resp.getData();

            System.out.println("Συνδέθηκα με τον Master στη θύρα " + masterPort);
            System.out.println("Δήλωσα τον εαυτό μου ως Worker στον Master");
            System.out.println("[" + workerId + "] Έτοιμος για αιτήματα.");

            while (true) {
                try {
                    System.out.println("Περιμένω νέο request...");
                    Request request = (Request) in.readObject();
                    System.out.println(" [" + workerId + "] Νέο αίτημα: " + request.getType());

                    switch (request.getType()) {


                        case "CATEGORY_REVENUE":
                            Map<String, Double> categoryRevenue = new HashMap<>();

                            for (Map.Entry<String, List<Order>> entry : orderMap.entrySet()) {
                                String currentStoreName = entry.getKey(); //  νέο όνομα
                                List<Order> orders = entry.getValue();
                                Store currentstore = storeMap.get(currentStoreName);

                                if (currentstore == null) continue;

                                String category = currentstore.getFoodCategory();
                                double total = categoryRevenue.getOrDefault(category, 0.0);

                                for (Order o : orders) {
                                    total += o.getTotalCost();
                                }

                                categoryRevenue.put(category, total);
                            }
                            System.out.println("DEBUG CATEGORY_REVENUE:");
                            System.out.println(" -> Πλήθος entries: " + categoryRevenue.size());
                            categoryRevenue.forEach((cat, rev) ->
                                    System.out.printf(" - %s: %.2f€\n", cat, rev)
                            );

                            out.writeObject(new Response(true, " Έσοδα ανά κατηγορία καταστήματος", categoryRevenue));
                            out.flush();
                            break;

                        case "SEARCH_ALL_STORES":
                            List<Store> liveStores = new ArrayList<>();
                            synchronized (storeMap) {
                                for (Store s : storeMap.values()) {
                                    Store liveStore = new Store(
                                            s.getStoreName(),
                                            s.getLatitude(),
                                            s.getLongitude(),
                                            s.getFoodCategory(),
                                            s.getStars(),
                                            s.getNoOfVotes(),
                                            s.getStoreLogo(),
                                            new ArrayList<>(s.getProducts())
                                    );
                                    liveStores.add(liveStore);
                                }
                            }
                            out.writeObject(new Response(true, "Όλα τα καταστήματα (live snapshot)", liveStores));
                            out.flush();
                            break;

                        case "SEARCH_5KM_RANGE":
                            SearchFilters filtersFor5kmRange = (SearchFilters) request.getPayload();
                            List<Store> nearbyStores = new ArrayList<>();
                            synchronized (storeMap) {
                                for (Store store5km : storeMap.values()) {
                                    double distance = distanceInKm(
                                            filtersFor5kmRange.getClientLatitude(),
                                            filtersFor5kmRange.getClientLongitude(),
                                            store5km.getLatitude(),
                                            store5km.getLongitude());

                                    if (distance <= 5) {
                                        Store liveStore5km = new Store(
                                            store5km.getStoreName(),
                                            store5km.getLatitude(),
                                            store5km.getLongitude(),
                                            store5km.getFoodCategory(),
                                            store5km.getStars(),
                                            store5km.getNoOfVotes(),
                                            store5km.getStoreLogo(),
                                            new ArrayList<>(store5km.getProducts())
                                    );
                                        nearbyStores.add(liveStore5km);
                                    }
                                }
                            }
                            System.out.println("Αποτελέσματα Search σε ακτίνα 5km: " + nearbyStores.size());
                            Response response = new Response(true, "Καταστήματα σε ακτίνα 5km", nearbyStores);
                            out.writeObject(response);
                            out.flush();
                            
                            out.flush();
                            break;

                        case "FILTER_STORES":
                            System.out.println(" Worker έλαβε αίτημα: FILTER_STORES");
                            SearchFilters filtersForStores = (SearchFilters) request.getPayload();
                            List<Store> filteredStores = new ArrayList<>();
                            synchronized (storeMap) {
                                List<Store> filteredStore = storeMap.values().stream()
                                        .filter(store ->
                                                filtersForStores.getFoodCategories() == null ||
                                                        filtersForStores.getFoodCategories().isEmpty() ||
                                                        filtersForStores.getFoodCategories().contains(store.getFoodCategory())
                                        )
                                        .filter(store ->
                                                store.getStars() >= filtersForStores.getMinStars()
                                        )
                                        .filter(store ->
                                                filtersForStores.getPriceCategories() == null ||
                                                        filtersForStores.getPriceCategories().isEmpty() ||
                                                        filtersForStores.getPriceCategories().contains(store.getPriceCategory())
                                        )
                                        .collect(Collectors.toList());

                                    for (Store st : filteredStore){
                                        Store liveStore = new Store(
                                                st.getStoreName(),
                                                st.getLatitude(),
                                                st.getLongitude(),
                                                st.getFoodCategory(),
                                                st.getStars(),
                                                st.getNoOfVotes(),
                                                st.getStoreLogo(),
                                                new ArrayList<>(st.getProducts())
                                        );
                                        filteredStores.add(liveStore);
                                    }
                                
                                    System.out.println("Αποτελέσματα φίλτρων: " + filteredStores.size());
                                    out.writeObject(new Response(true, "Φιλτραρισμένα καταστήματα", filteredStores));
                                    out.flush();
                            }
                            out.flush();
                            break;


                        default:
                            Response error = new Response(false, "Άγνωστο αίτημα", null);
                            out.writeObject(error);
                            out.flush();
                            break;


                        case "UPDATE_PRODUCTS":
                            UpdateProductRequest upr = (UpdateProductRequest) request.getPayload();
                            String sName = upr.getStoreName();

                            Store s1 = storeMap.get(sName);
                            if (s1 == null) {
                                out.writeObject(new Response(false, "Το κατάστημα δεν υπάρχει", null));
                                out.flush();
                                break;
                            }

                            if ("ADD".equalsIgnoreCase(upr.getAction())) {
                                boolean updated = false;

                                for (Product p : s1.getProducts()) {
                                    if (p.getProductName().equalsIgnoreCase(upr.getProductName())) {

                                        // Prosthiki posotitas
                                        if (upr.getAvailableAmount() > 0) {
                                            int prev = p.getAvailableAmount();
                                            int newAmount = prev + upr.getAvailableAmount();
                                            p.setAvailableAmount(newAmount);
                                            System.out.println("Προστέθηκαν " + upr.getAvailableAmount() + " τεμάχια στο προϊόν '" + p.getProductName() + "'. Νέα ποσότητα: " + newAmount);
                                        }

                                        // Enimerwsi timis
                                        if (upr.getPrice() > 0 && p.getPrice() != upr.getPrice()) {
                                            System.out.println("Ενημερώθηκε η τιμή προϊόντος '" + p.getProductName() + "' από " + p.getPrice() + "€ σε " + upr.getPrice() + "€");
                                            p.setPrice(upr.getPrice());
                                        }

                                        // Enimerwsi typou
                                        if (upr.getProductType() != null && !upr.getProductType().equalsIgnoreCase("null") &&
                                                !p.getProductType().equalsIgnoreCase(upr.getProductType())) {
                                            System.out.println("Ενημερώθηκε ο τύπος προϊόντος '" + p.getProductName() + "' από " + p.getProductType() + " σε " + upr.getProductType());
                                            p.setProductType(upr.getProductType());
                                        }

                                        updated = true;
                                        break;
                                    }
                                }

                                if (!updated) {
                                    if (upr.getAvailableAmount() <= 0 || upr.getPrice() <= 0 ||
                                            upr.getProductType() == null || upr.getProductType().equalsIgnoreCase("null")) {
                                        out.writeObject(new Response(false, "Για νέο προϊόν απαιτείται θετική ποσότητα, τιμή και τύπος", null));
                                        out.flush();
                                    } else {
                                        Product newProd = new Product(
                                                upr.getProductName(),
                                                upr.getProductType(),
                                                upr.getAvailableAmount(),
                                                upr.getPrice()
                                        );
                                        s1.getProducts().add(newProd);
                                        s1.getPriceCategory();

                                        System.out.println("Προστέθηκε νέο προϊόν '" + newProd.getProductName() + "' στο κατάστημα '" + s1.getStoreName() + "'");
                                        out.writeObject(new Response(true, "Προστέθηκε νέο προϊόν", null));
                                        out.flush();
                                    }
                                } else {
                                    out.writeObject(new Response(true, "Ενημερώθηκε το προϊόν", null));
                                    out.flush();
                                }

                            } else if ("REMOVE".equalsIgnoreCase(upr.getAction())) {
                                boolean removed = s1.removeProduct(upr.getProductName());
                                if (removed) {
                                    s1.getPriceCategory();
                                    System.out.println("Αφαιρέθηκε το προϊόν '" + upr.getProductName() + "' από το κατάστημα '" + s1.getStoreName() + "'");
                                    out.writeObject(new Response(true, "Αφαιρέθηκε το προϊόν '" + upr.getProductName() + "'", null));
                                } else {
                                    out.writeObject(new Response(false, "Το προϊόν δεν βρέθηκε", null));
                                }
                                out.flush();

                            } else if ("REDUCE".equalsIgnoreCase(upr.getAction())) {
                                List<Product> products = s1.getProducts();
                                boolean found = false;

                                for (Product p : products) {
                                    if (p.getProductName().equalsIgnoreCase(upr.getProductName())) {
                                        int currentAmount = p.getAvailableAmount();
                                        int reduceBy = upr.getAvailableAmount();

                                        if (reduceBy <= 0) {
                                            out.writeObject(new Response(false, "Η ποσότητα προς αφαίρεση πρέπει να είναι θετική", null));
                                            out.flush();
                                            break;
                                        }

                                        if (reduceBy > currentAmount) {
                                            out.writeObject(new Response(false, "Δεν μπορεί να αφαιρεθεί ποσότητα μεγαλύτερη από το απόθεμα (" + currentAmount + " διαθέσιμα)", null));
                                            out.flush();
                                            break;
                                        }

                                        p.setAvailableAmount(currentAmount - reduceBy);

                                        System.out.printf("Μειώθηκε ποσότητα για %s: -%d (από %d → %d)\n",
                                                p.getProductName(), reduceBy, currentAmount, p.getAvailableAmount());

                                        out.writeObject(new Response(true, "Η ποσότητα μειώθηκε επιτυχώς", null));
                                        out.flush();
                                        found = true;
                                        break;
                                    }
                                }

                                if (!found) {
                                    out.writeObject(new Response(false, "Το προϊόν δεν βρέθηκε στο κατάστημα", null));
                                    out.flush();
                                }
                            }
                            break;
                        case "CATEGORY_PRODUCT_SALES":
                            String categoryRequested = (String) request.getPayload();
                            Map<String, Double> result = new HashMap<>();
                            double totalRevenue = 0.0;

                            for (Map.Entry<String, List<Order>> entry : orderMap.entrySet()) {
                                String storeName = entry.getKey();
                                Store store = storeMap.get(storeName);
                                if (store == null) continue;

                                double storeRevenue = 0.0;

                                for (Order order : entry.getValue()) {
                                    Map<String, Integer> ordered = order.getProductsOrdered();

                                    for (Map.Entry<String, Integer> prodEntry : ordered.entrySet()) {
                                        String productName = prodEntry.getKey();
                                        int quantity = prodEntry.getValue();

                                        Product product = store.getProducts().stream()
                                                .filter(p -> p.getProductName().equalsIgnoreCase(productName))
                                                .findFirst()
                                                .orElse(null);

                                        if (product != null && categoryRequested.equalsIgnoreCase(product.getProductType())) {
                                            double revenue = quantity * product.getPrice();
                                            storeRevenue += revenue;

                                            System.out.printf(" Match: %s (%s) → %.2f€\n", product.getProductName(), product.getProductType(), revenue);
                                        }
                                    }
                                }

                                if (storeRevenue > 0.0) {
                                    result.put(storeName, storeRevenue);
                                    totalRevenue += storeRevenue;
                                }
                            }

                            result.put("total", totalRevenue);
                            out.writeObject(new Response(true, " Έσοδα ανά κατηγορία προϊόντος", result));
                            out.flush();
                            break;

                        case "PRODUCT_SALES":
                            Map<String, Map<String, Object>> productStats = new HashMap<>();

                            for (Map.Entry<String, List<Order>> entry : orderMap.entrySet()) {
                                String storeKey = entry.getKey();
                                List<Order> orders = entry.getValue();
                                Store storeObj = storeMap.get(storeKey);
                                if (storeObj == null) continue;

                                for (Order order : orders) {
                                    Map<String, Integer> ordered = order.getProductsOrdered();

                                    for (Map.Entry<String, Integer> e : ordered.entrySet()) {
                                        String prodName = e.getKey();
                                        int qty = e.getValue();

                                        Product p = storeObj.getProducts().stream()
                                                .filter(prod -> prod.getProductName().equalsIgnoreCase(prodName))
                                                .findFirst()
                                                .orElse(null);

                                        if (p != null) {
                                            double revenue = qty * p.getPrice();

                                            if (!productStats.containsKey(prodName)) {
                                                Map<String, Object> data = new HashMap<>();
                                                data.put("quantity", qty);
                                                data.put("revenue", revenue);
                                                productStats.put(prodName, data);
                                            } else {
                                                Map<String, Object> data = productStats.get(prodName);
                                                int oldQty = (Integer) data.get("quantity");
                                                double oldRev = (Double) data.get("revenue");
                                                data.put("quantity", oldQty + qty);
                                                data.put("revenue", oldRev + revenue);
                                            }
                                        }
                                    }
                                }
                            }

                            out.writeObject(new Response(true, "Πωλήσεις προϊόντων", productStats));
                            out.flush();
                            break;
                        case "ADD_STORE":
                            Store store = (Store) request.getPayload();
                            String storeName = store.getStoreName();
                            synchronized (storeMap) {
                                if (storeMap.containsKey(storeName)) {
                                    out.writeObject(new Response(false, "Το κατάστημα υπάρχει ήδη", null));
                                } else {
                                    storeMap.put(storeName, store);
                                    out.writeObject(new Response(true, "Το κατάστημα αποθηκεύτηκε", null));
                                    System.out.println("Προστέθηκε κατάστημα: " + storeName);
                                }
                            }
                            out.flush();
                            break;

                        case "GET_PRODUCTS":
                            String requestedStore = (String) request.getPayload();
                            
                            Store s = storeMap.get(requestedStore);

                            if (s == null) {
                                out.writeObject(new Response(false, " Το κατάστημα δεν βρέθηκε", null));
                            } else {
                                for (Product p : s.getProducts()) {
                                    System.out.printf(" - %s (%s) %.2f€, Διαθέσιμα: %d\n",
                                            p.getProductName(), p.getProductType(), p.getPrice(), p.getAvailableAmount());
                                }
                                s.getPriceCategory();

                                List<Product> copyProducts = new ArrayList<>();
                                for (Product p : s.getProducts()) {
                                    Product copy = new Product(
                                            p.getProductName(),
                                            p.getProductType(),
                                            p.getAvailableAmount(),
                                            p.getPrice()
                                    );
                                    copyProducts.add(copy);
                                }

                                out.writeObject(new Response(true, " Προϊόντα καταστήματος", copyProducts));
                                out.flush();
                            }
                            
                            out.flush();
                            break;

                        case "ADD_ORDER":
                            Order order = (Order) request.getPayload();
                            String targetStore = order.getStoreName();
                            System.out.println("Ζητήθηκε κατάστημα: '" + targetStore + "'");
                            System.out.println("Καταστήματα που έχει ο Worker: " + storeMap.keySet());

                            Store target = storeMap.get(targetStore);

                            if (target == null) {
                                out.writeObject(new Response(false, " Το κατάστημα δεν υπάρχει", null));
                                out.flush();
                                break;
                            }

                            List<Product> products = target.getProducts();
                            Map<String, Integer> ordered = order.getProductsOrdered();

                            System.out.println("Ποσότητες πριν την παραγγελία:");
                            products.forEach(p -> System.out.println(p.getProductName() + " → " + p.getAvailableAmount()));

                            boolean invalid = false;
                            for (Map.Entry<String, Integer> entry : ordered.entrySet()) {
                                String productName = entry.getKey();
                                int quantity = entry.getValue();

                                Product product = products.stream()
                                        .filter(p -> p.getProductName().equalsIgnoreCase(productName))
                                        .findFirst().orElse(null);

                                if (product == null || product.getAvailableAmount() < quantity) {
                                    out.writeObject(new Response(false, " Το προϊόν '" + productName + "' δεν είναι διαθέσιμο", null));
                                    out.flush();
                                    invalid = true;
                                    break;
                                }
                            }
                            if (invalid) break;

                            synchronized (target) {
                                for (Map.Entry<String, Integer> entry : ordered.entrySet()) {
                                    String productName = entry.getKey();
                                    int quantity = entry.getValue();

                                    Product product = products.stream()
                                            .filter(p -> p.getProductName().equalsIgnoreCase(productName))
                                            .findFirst().get();

                                    int before = product.getAvailableAmount();
                                    product.setAvailableAmount(before - quantity);

                                    System.out.printf("Αφαιρώ %d από '%s' → %d → %d\n", quantity, productName, before, product.getAvailableAmount());

                                    productSales.put(productName, productSales.getOrDefault(productName, 0) + quantity);
                                }

                                target.addRevenue(order.getTotalCost());
                                orderMap.computeIfAbsent(targetStore, k -> new ArrayList<>()).add(order);
                            }

                            System.out.println("Ποσότητες μετά την παραγγελία:");
                            products.forEach(p -> System.out.println(p.getProductName() + " → " + p.getAvailableAmount()));

                            out.writeObject(new Response(true, " Η παραγγελία καταχωρήθηκε", null));
                            out.flush();
                            break;
                        
                        case "RATE_STORE":
                            RateStoreRequest rateRequest = (RateStoreRequest) request.getPayload();
                            handleRateStore(rateRequest, out);
                            out.flush();
                            break;

                    }
                } catch (Exception ex) {
                    System.err.println("EXCEPTION ΣΤΟ LOOP: " + ex.getMessage());
                    ex.printStackTrace();
                    break;
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Worker σφάλμα: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Sinartisi gia tin metatropi apostasis se k m
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

    private void handleRateStore(RateStoreRequest rateRequest, ObjectOutputStream out) throws IOException {
        String storeName = rateRequest.getStoreName();
        int rating = rateRequest.getRating();
    
        synchronized (storeMap) {
            Store store = storeMap.get(storeName);
            if (store != null) {
                double totalStars = store.getStars() * store.getNoOfVotes();
                totalStars += rating;
                store.setNoOfVotes(store.getNoOfVotes() + 1);
                System.out.println(store.getNoOfVotes());
                store.setStars(totalStars / store.getNoOfVotes());
                System.out.println("Αστέρια : " + store.getStars());
    
                out.writeObject(new Response(true, "Η βαθμολογία καταχωρήθηκε", null));
            } else {
                out.writeObject(new Response(false, "Το κατάστημα δεν βρέθηκε", null));
            }
        }
        out.flush();
    }

    public static void main(String[] args) {
        String masterHost = "localhost";
        int masterPort = 5000;

        try {
            WorkerNode worker = new WorkerNode();
            worker.start(masterHost, masterPort);
        } catch (Exception e) {
            System.err.println("Σφάλμα κατά την εκκίνηση Worker: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
