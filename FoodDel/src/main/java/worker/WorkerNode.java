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
            System.out.println("\uD83D\uDD0C Σύνδεση με τον Master στο " + masterHost + ":" + masterPort + " ...");

            Socket socket = new Socket(masterHost, masterPort);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(new Request("REGISTER_WORKER", null));
            out.flush();

            Response resp = (Response) in.readObject();
            String workerId = (String) resp.getData();

            System.out.println("\uD83D\uDFE2 Συνδέθηκα με τον Master στη θύρα " + masterPort);
            System.out.println("\uD83D\uDCE1 Δήλωσα τον εαυτό μου ως Worker στον Master");
            System.out.println("\uD83D\uDC77 [" + workerId + "] Έτοιμος για αιτήματα.");

            while (true) {
                try {
                    System.out.println("🌀 Περιμένω νέο request...");
                    Request request = (Request) in.readObject();
                    System.out.println("📩 [" + workerId + "] Νέο αίτημα: " + request.getType());

                    switch (request.getType()) {


                        case "CATEGORY_REVENUE":
                            Map<String, Double> categoryRevenue = new HashMap<>();

                            for (Map.Entry<String, List<Order>> entry : orderMap.entrySet()) {
                                String currentStoreName = entry.getKey(); // ✅ νέο όνομα
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
                            System.out.println("📤 DEBUG CATEGORY_REVENUE:");
                            System.out.println(" -> Πλήθος entries: " + categoryRevenue.size());
                            categoryRevenue.forEach((cat, rev) ->
                                    System.out.printf(" - %s: %.2f€\n", cat, rev)
                            );

                            out.writeObject(new Response(true, "💰 Έσοδα ανά κατηγορία καταστήματος", categoryRevenue));
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

//                        case "REDUCE":
//                            UpdateProductRequest reduceReq = (UpdateProductRequest) request.getPayload();
//                            Store targetStore = storeMap.get(reduceReq.getStoreName());
//
//                            if (targetStore == null) {
//                                out.writeObject(new Response(false, "❌ Το κατάστημα δεν υπάρχει", null));
//                                out.flush();
//                                break;
//                            }
//
//                            boolean found = false;
//                            final int MIN_AMOUNT = 5; // 🟠 Ελάχιστο όριο ποσότητας
//
//                            for (Product p : targetStore.getProducts()) {
//                                if (p.getProductName().equalsIgnoreCase(reduceReq.getProductName())) {
//                                    int current = p.getAvailableAmount();
//                                    int toRemove = reduceReq.getAvailableAmount();
//
//                                    if (toRemove <= 0) {
//                                        out.writeObject(new Response(false, "❌ Η ποσότητα προς αφαίρεση πρέπει να είναι θετική", null));
//                                        out.flush();
//                                        break;
//                                    }
//
//                                    if (current < toRemove) {
//                                        out.writeObject(new Response(false, "❌ Δεν υπάρχει αρκετό απόθεμα για αφαίρεση (" + current + " διαθέσιμα)", null));
//                                        out.flush();
//                                        break;
//                                    }
//
//                                    int newAmount = current - toRemove;
//                                    p.setAvailableAmount(newAmount);
//
//                                    System.out.println("📉 Αφαιρέθηκαν " + toRemove + " τεμάχια από '" + p.getProductName() +
//                                            "'. Νέα ποσότητα: " + newAmount);
//
//                                    if (newAmount <= MIN_AMOUNT) {
//                                        System.out.println("⚠️ ΠΡΟΕΙΔΟΠΟΙΗΣΗ: Το προϊόν '" + p.getProductName() +
//                                                "' έχει πολύ χαμηλό απόθεμα (" + newAmount + " τεμάχια)");
//                                    }
//
//                                    out.writeObject(new Response(true, "✅ Αφαιρέθηκαν " + toRemove + " τεμάχια από '" + p.getProductName() + "'", null));
//                                    out.flush();
//                                    found = true;
//                                    break;
//                                }
//                            }
//
//                            if (!found) {
//                                out.writeObject(new Response(false, "❌ Το προϊόν δεν βρέθηκε στο κατάστημα", null));
//                                out.flush();
//                            }
//
//                            break;
                        case "UPDATE_PRODUCTS":
                            UpdateProductRequest upr = (UpdateProductRequest) request.getPayload();
                            String sName = upr.getStoreName();

                            Store s1 = storeMap.get(sName);
                            if (s1 == null) {
                                out.writeObject(new Response(false, "❌ Το κατάστημα δεν υπάρχει", null));
                                out.flush();
                                break;
                            }

                            if ("ADD".equalsIgnoreCase(upr.getAction())) {
                                boolean updated = false;

                                for (Product p : s1.getProducts()) {
                                    if (p.getProductName().equalsIgnoreCase(upr.getProductName())) {

                                        // 🔢 Προσθήκη ποσότητας
                                        if (upr.getAvailableAmount() > 0) {
                                            int prev = p.getAvailableAmount();
                                            int newAmount = prev + upr.getAvailableAmount();
                                            p.setAvailableAmount(newAmount);
                                            System.out.println("📦 Προστέθηκαν " + upr.getAvailableAmount() + " τεμάχια στο προϊόν '" + p.getProductName() + "'. Νέα ποσότητα: " + newAmount);
                                        }

                                        // 💰 Ενημέρωση τιμής
                                        if (upr.getPrice() > 0 && p.getPrice() != upr.getPrice()) {
                                            System.out.println("ℹ️ Ενημερώθηκε η τιμή προϊόντος '" + p.getProductName() + "' από " + p.getPrice() + "€ σε " + upr.getPrice() + "€");
                                            p.setPrice(upr.getPrice());
                                        }

                                        // 🏷️ Ενημέρωση τύπου
                                        if (upr.getProductType() != null && !upr.getProductType().equalsIgnoreCase("null") &&
                                                !p.getProductType().equalsIgnoreCase(upr.getProductType())) {
                                            System.out.println("ℹ️ Ενημερώθηκε ο τύπος προϊόντος '" + p.getProductName() + "' από " + p.getProductType() + " σε " + upr.getProductType());
                                            p.setProductType(upr.getProductType());
                                        }

                                        updated = true;
                                        break;
                                    }
                                }

                                if (!updated) {
                                    if (upr.getAvailableAmount() <= 0 || upr.getPrice() <= 0 ||
                                            upr.getProductType() == null || upr.getProductType().equalsIgnoreCase("null")) {
                                        out.writeObject(new Response(false, "❌ Για νέο προϊόν απαιτείται θετική ποσότητα, τιμή και τύπος", null));
                                    } else {
                                        Product newProd = new Product(
                                                upr.getProductName(),
                                                upr.getProductType(),
                                                upr.getAvailableAmount(),
                                                upr.getPrice()
                                        );
                                        s1.getProducts().add(newProd);
                                        out.writeObject(new Response(true, "✅ Προστέθηκε νέο προϊόν", null));
                                    }
                                } else {
                                    out.writeObject(new Response(true, "♻️ Ενημερώθηκε το προϊόν", null));
                                }

                            } else if ("REMOVE".equalsIgnoreCase(upr.getAction())) {
                                boolean removed = s1.removeProduct(upr.getProductName());
                                if (removed) {
                                    System.out.println("🗑️ Αφαιρέθηκε το προϊόν '" + upr.getProductName() + "' από το κατάστημα '" + s1.getStoreName() + "'");
                                    out.writeObject(new Response(true, "✅ Αφαιρέθηκε το προϊόν '" + upr.getProductName() + "'", null));
                                } else {
                                    System.out.println("⚠️ Το προϊόν '" + upr.getProductName() + "' δεν βρέθηκε στο κατάστημα '" + s1.getStoreName() + "'");
                                    out.writeObject(new Response(false, "❌ Το προϊόν δεν βρέθηκε", null));
                                }

                            } else if ("REDUCE".equalsIgnoreCase(upr.getAction())) {
                                List<Product> products = s1.getProducts();

                                boolean found = false;

                                for (Product p : products) {
                                    if (p.getProductName().equalsIgnoreCase(upr.getProductName())) {
                                        int currentAmount = p.getAvailableAmount();
                                        int reduceBy = upr.getAvailableAmount();

                                        if (reduceBy <= 0) {
                                            out.writeObject(new Response(false, "❌ Η ποσότητα προς αφαίρεση πρέπει να είναι θετική", null));
                                            out.flush();
                                            break;
                                        }

                                        if (reduceBy > currentAmount) {
                                            out.writeObject(new Response(false,
                                                    "❌ Δεν μπορεί να αφαιρεθεί ποσότητα μεγαλύτερη από το απόθεμα (" + currentAmount + " διαθέσιμα)", null));
                                            out.flush();
                                            break;
                                        }

                                        p.setAvailableAmount(currentAmount - reduceBy);

                                        System.out.printf("🔽 Μειώθηκε ποσότητα για %s: -%d (από %d → %d)\n",
                                                p.getProductName(), reduceBy, currentAmount, p.getAvailableAmount());

                                        out.writeObject(new Response(true, "✅ Η ποσότητα μειώθηκε επιτυχώς", null));
                                        out.flush();
                                        found = true;
                                        break;
                                    }
                                }

                                if (!found) {
                                    out.writeObject(new Response(false, "❌ Το προϊόν δεν βρέθηκε στο κατάστημα", null));
                                    out.flush();
                                }
                            }

                            out.flush();
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

                                            System.out.printf("✅ Match: %s (%s) → %.2f€\n", product.getProductName(), product.getProductType(), revenue);
                                        }
                                    }
                                }

                                if (storeRevenue > 0.0) {
                                    result.put(storeName, storeRevenue);
                                    totalRevenue += storeRevenue;
                                }
                            }

                            result.put("total", totalRevenue);
                            out.writeObject(new Response(true, "💰 Έσοδα ανά κατηγορία προϊόντος", result));
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

                            out.writeObject(new Response(true, "📊 Πωλήσεις προϊόντων", productStats));
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
                                    System.out.println("📥 Προστέθηκε κατάστημα: " + storeName);
                                }
                            }
                            out.flush();
                            break;

                        case "GET_PRODUCTS":
                            String requestedStore = (String) request.getPayload();
                            Store s = storeMap.get(requestedStore);
                            if (s == null) {
                                out.writeObject(new Response(false, "❌ Το κατάστημα δεν βρέθηκε", null));
                            } else {
                                out.writeObject(new Response(true, "📦 Προϊόντα καταστήματος", s.getProducts()));
                            }
                            out.flush();
                            break;

                        case "ADD_ORDER":
                            Order order = (Order) request.getPayload();
                            String targetStore = order.getStoreName();
                            System.out.println("🔍 Ζητήθηκε κατάστημα: '" + targetStore + "'");
                            System.out.println("📦 Καταστήματα που έχει ο Worker: " + storeMap.keySet());

                            Store target = storeMap.get(targetStore);

                            if (target == null) {
                                out.writeObject(new Response(false, "❌ Το κατάστημα δεν υπάρχει", null));
                                out.flush();
                                break;
                            }

                            List<Product> products = target.getProducts();
                            Map<String, Integer> ordered = order.getProductsOrdered();

                            boolean invalid = false;
                            for (Map.Entry<String, Integer> entry : ordered.entrySet()) {
                                String productName = entry.getKey();
                                int quantity = entry.getValue();

                                Product product = products.stream()
                                        .filter(p -> p.getProductName().equalsIgnoreCase(productName))
                                        .findFirst().orElse(null);

                                if (product == null || product.getAvailableAmount() < quantity) {
                                    out.writeObject(new Response(false, "❌ Το προϊόν '" + productName + "' δεν είναι διαθέσιμο", null));
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

                                    product.setAvailableAmount(product.getAvailableAmount() - quantity);
                                    productSales.put(productName, productSales.getOrDefault(productName, 0) + quantity);
                                }

                                target.addRevenue(order.getTotalCost());
                                orderMap.computeIfAbsent(targetStore, k -> new ArrayList<>()).add(order);
                            }

                            out.writeObject(new Response(true, "✅ Η παραγγελία καταχωρήθηκε", null));
                            out.flush();
                            break;

                    }
                } catch (Exception ex) {
                    System.err.println("❌ EXCEPTION ΣΤΟ LOOP: " + ex.getMessage());
                    ex.printStackTrace();
                    break;
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Worker σφάλμα: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    public static void main(String[] args) {
        String masterHost = "localhost";
        int masterPort = 5000;

        try {
            WorkerNode worker = new WorkerNode();
            worker.start(masterHost, masterPort);
        } catch (Exception e) {
            System.err.println("❌ Σφάλμα κατά την εκκίνηση Worker: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
