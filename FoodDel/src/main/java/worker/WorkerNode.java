package worker;

import common.model.Request;
import common.model.Response;
import common.model.SearchFilters;
import common.model.Store;
import common.model.Order;
import common.model.UpdateProductRequest;
import common.model.Product;

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

    private Map<String, Integer> productSales = new HashMap<>();



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


                    case "GET_PRODUCTS":
                        String requestedStore = (String) request.getPayload();
                        System.out.println("🔍 Ζητήθηκε κατάστημα: '" + requestedStore + "'");
                        System.out.println("📦 Καταστήματα στον Worker: " + storeMap.keySet());

                        Store s = storeMap.get(requestedStore);
                        if (s == null) {
                            System.out.println("❌ Δεν βρέθηκε το κατάστημα στον Worker.");
                            out.writeObject(new Response(false, "❌ Το κατάστημα δεν βρέθηκε", null));
                        } else {
                            System.out.println("✅ Βρέθηκε! Επιστρέφονται προϊόντα: " + s.getProducts());
                            out.writeObject(new Response(true, "📦 Προϊόντα καταστήματος", s.getProducts()));
                        }
                        System.out.println("➡️ Πλήθος προϊόντων: " + (s.getProducts() != null ? s.getProducts().size() : "null"));
                        out.flush();
                        break;


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

                    case "REDUCE":
                        UpdateProductRequest reduceReq = (UpdateProductRequest) request.getPayload();
                        Store targetStore = storeMap.get(reduceReq.getStoreName());

                        if (targetStore == null) {
                            out.writeObject(new Response(false, "❌ Το κατάστημα δεν υπάρχει", null));
                            out.flush();
                            break;
                        }

                        boolean found = false;
                        final int MIN_AMOUNT = 5; // 🟠 Ελάχιστο όριο ποσότητας

                        for (Product p : targetStore.getProducts()) {
                            if (p.getProductName().equalsIgnoreCase(reduceReq.getProductName())) {
                                int current = p.getAvailableAmount();
                                int toRemove = reduceReq.getAvailableAmount();

                                if (toRemove <= 0) {
                                    out.writeObject(new Response(false, "❌ Η ποσότητα προς αφαίρεση πρέπει να είναι θετική", null));
                                    out.flush();
                                    break;
                                }

                                if (current < toRemove) {
                                    out.writeObject(new Response(false, "❌ Δεν υπάρχει αρκετό απόθεμα για αφαίρεση (" + current + " διαθέσιμα)", null));
                                    out.flush();
                                    break;
                                }

                                int newAmount = current - toRemove;
                                p.setAvailableAmount(newAmount);

                                System.out.println("📉 Αφαιρέθηκαν " + toRemove + " τεμάχια από '" + p.getProductName() +
                                        "'. Νέα ποσότητα: " + newAmount);

                                if (newAmount <= MIN_AMOUNT) {
                                    System.out.println("⚠️ ΠΡΟΕΙΔΟΠΟΙΗΣΗ: Το προϊόν '" + p.getProductName() +
                                            "' έχει πολύ χαμηλό απόθεμα (" + newAmount + " τεμάχια)");
                                }

                                out.writeObject(new Response(true, "✅ Αφαιρέθηκαν " + toRemove + " τεμάχια από '" + p.getProductName() + "'", null));
                                out.flush();
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            out.writeObject(new Response(false, "❌ Το προϊόν δεν βρέθηκε στο κατάστημα", null));
                            out.flush();
                        }

                        break;


                    case "ADD_ORDER":
                        Order order = (Order) request.getPayload();
                        String targetStore1 = order.getStoreName();

                        if (!storeMap.containsKey(targetStore1)) {
                            out.writeObject(new Response(false, "❌ Το κατάστημα δεν υπάρχει", null));
                            out.flush();
                            break;
                        }

                        Store store = storeMap.get(targetStore1);
                        List<Product> storeProducts = store.getProducts();
                        Map<String, Integer> orderedProducts = order.getProductsOrdered();

                        // Έλεγχος διαθεσιμότητας
                        for (Map.Entry<String, Integer> entry : orderedProducts.entrySet()) {
                            String productName = entry.getKey();
                            int quantityRequested = entry.getValue();

                            Product matchingProduct = storeProducts.stream()
                                    .filter(p -> p.getProductName().equalsIgnoreCase(productName))
                                    .findFirst()
                                    .orElse(null);

                            if (matchingProduct == null) {
                                out.writeObject(new Response(false, "❌ Το προϊόν '" + productName + "' δεν βρέθηκε στο κατάστημα", null));
                                out.flush();
                                break;
                            }

                            if (matchingProduct.getAvailableAmount() < quantityRequested) {
                                out.writeObject(new Response(false, "❌ Δεν υπάρχει επαρκές απόθεμα για το προϊόν '" + productName + "'", null));
                                out.flush();
                                break;
                            }
                        }

                        // ✅ Εκτέλεση παραγγελίας: αφαίρεση ποσότητας, ενημέρωση εσόδων & καταγραφή
                        synchronized (store) {
                            for (Map.Entry<String, Integer> entry : orderedProducts.entrySet()) {
                                String productName = entry.getKey();
                                int quantity = entry.getValue();

                                for (Product p : storeProducts) {
                                    if (p.getProductName().equalsIgnoreCase(productName)) {
                                        int oldAmount = p.getAvailableAmount();
                                        p.setAvailableAmount(oldAmount - quantity);
                                        System.out.println("🧾 " + productName + ": -" + quantity + " (από " + oldAmount + " → " + p.getAvailableAmount() + ")");
                                    }
                                }

                                // Ενημέρωση πωλήσεων ανά προϊόν (προαιρετικό)
                                productSales.put(productName, productSales.getOrDefault(productName, 0) + quantity);
                            }

                            // Ενημέρωση συνολικών εσόδων καταστήματος
                            store.addRevenue(order.getTotalCost());

                            // Καταγραφή παραγγελίας
                            orderMap.computeIfAbsent(targetStore1, k -> new ArrayList<>()).add(order);
                        }

                        out.writeObject(new Response(true, "✅ Η παραγγελία καταχωρήθηκε επιτυχώς!", null));
                        System.out.println("📥 Καταχώρηση παραγγελίας:");
                        System.out.println(" - Κατάστημα: " + order.getStoreName());
                        System.out.println(" - Προϊόντα: " + order.getProductsOrdered());
                        System.out.println(" - Κόστος: " + order.getTotalCost());
                        System.out.println("📥 Καταγραφή στο orderMap...");
                        System.out.println("📦 Τρέχουσες παραγγελίες στο store:");
                        orderMap.get(order.getStoreName()).forEach(o -> System.out.println(" - " + o));

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


                        Response ok = new Response(true, "Το κατάστημα αποθηκεύτηκε", null);
                        System.out.println("📨 Λήφθηκε αίτημα προσθήκης καταστήματος...");
                        System.out.println("➡️ Όνομα: " + store1.getStoreName());
                        System.out.println("➡️ Προϊόντα: " + store1.getProducts());
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

                    case "CATEGORY_PRODUCT_SALES":
                        String categoryRequested = (String) request.getPayload();
                        Map<String, Double> result = new HashMap<>();
                        double totalRevenue = 0.0;

                        for (Map.Entry<String, List<Order>> entry : orderMap.entrySet()) {
                            String astoreName = entry.getKey();
                            List<Order> orders = entry.getValue();
                            Store astore = storeMap.get(astoreName);
                            if (astore == null) continue;

                            double storeRevenue = 0.0;

                            System.out.println("🧪 DEBUG Πωλήσεις για κατηγορία: " + categoryRequested);
                            System.out.println("🧪 Καταχώρηση προϊόντων παραγγελιών για Pizza Fun:");

                            for (Order anorder : orders) {
                                Map<String, Integer> ordered = anorder.getProductsOrdered();

                                for (Map.Entry<String, Integer> prodEntry : ordered.entrySet()) {
                                    String productName = prodEntry.getKey();
                                    int quantity = prodEntry.getValue();

                                    Product matched = astore.getProducts().stream()
                                            .filter(p -> p.getProductName().equalsIgnoreCase(productName))
                                            .findFirst()
                                            .orElse(null);

                                    if (matched != null) {
                                        System.out.println("🧪 Βρέθηκε προϊόν: " + matched.getProductName() +
                                                " (" + matched.getProductType() + "), ζητήθηκε: " + categoryRequested);

                                        if (matched.getProductType() != null &&
                                                matched.getProductType().equalsIgnoreCase(categoryRequested)) {

                                            storeRevenue += quantity * matched.getPrice();
                                        }
                                    }

                                }
                            }

                            if (storeRevenue > 0.0) {
                                result.put(astoreName, storeRevenue);
                                totalRevenue += storeRevenue;
                            }
                        }

                        result.put("total", totalRevenue);
                        out.writeObject(new Response(true, "💰 Έσοδα ανά κατηγορία προϊόντος", result));

                        System.out.println("💰 Έσοδα ανά κατάστημα:");
                        result.forEach((astore, rev) -> System.out.println(" - " + astore + ": " + rev + "€"));
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
