package managerapp;

import common.model.Order;
import common.model.Request;
import common.model.Response;
import common.model.Store;
import common.model.UpdateProductRequest;
import common.model.Product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class ManagerApp {

    private static final String MASTER_HOST = "localhost";
    private static final int MASTER_PORT = 5000;

    public static void main(String[] args) {

        try (
                Socket socket = new Socket(MASTER_HOST, MASTER_PORT);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                Scanner scanner = new Scanner(System.in)
        ) {
            out.flush();
            System.out.println("Συνδέθηκα με τον Master");
            List<String> addedStores = new ArrayList<>();

            while (true) {
                System.out.println("\n===== MENU =====");
                System.out.println("1. Καταχώρηση καταστημάτων ");
                System.out.println("2. Καταχώρηση παραγγελίας");
                System.out.println("3. Ενημέρωση προϊόντων καταστήματος (ADD / REMOVE / REDUCE)");
                System.out.println("4. Προβολή προϊόντων καταστήματος");
                System.out.println("5. Προβολή συνολικών πωλήσεων ανά προϊόν");
                System.out.println("6. Προβολή συνολικών πωλήσεων ανα τύπο καταστημάτων");
                System.out.println("7. Προβολή συνολικών πωλήσεων ανα κατηγορία προϊόντος");
                System.out.println("0. Έξοδος");
                System.out.print("Επιλογή: ");
                int choice = Integer.parseInt(scanner.nextLine());


                switch (choice) {
                    case 1:

                        while (true) {
                            printAvailableStores("/home/dimitris/Documents/OPA/DS/Ergasia/AuebFoodApp/resources/stores/", addedStores);

                            System.out.print(" Δώσε το όνομα του καταστήματος τύπου Pizza_Fun (ή 'τέλος' για έξοδο): ");
                            String storeName = scanner.nextLine().trim();

                            if (storeName.equalsIgnoreCase("τέλος") || storeName.equalsIgnoreCase("τελος")) break;

                            if (addedStores.contains(storeName)) {
                                System.out.println(" Το κατάστημα '" + storeName + "' έχει ήδη προστεθεί.");
                                continue;
                            }

                            String storeFilePath = "/home/dimitris/Documents/OPA/DS/Ergasia/AuebFoodApp/resources/stores/" + storeName + ".json";
                            System.out.println(" Διαβάζω από αρχείο: " + storeFilePath);

                            Store store = readStoreFromJson(storeFilePath);
                            if (store == null) {
                                System.out.println(" Δεν βρέθηκε το αρχείο ή είχε σφάλμα.");
                                continue;
                            }

                            System.out.println(" Κατάστημα διαβάστηκε: " + store);
                            Request reqStore = new Request("ADD_STORE", store);
                            out.writeObject(reqStore);
                            out.flush();

                            Response resp = (Response) in.readObject();
                            System.out.println(" Απάντηση: " + resp.getMessage());

                            addedStores.add(storeName);
                            Thread.sleep(100);
                        }
                        break;


                    case 2:

                        printAvailableOrders("/home/dimitris/Documents/OPA/DS/Ergasia/AuebFoodApp/resources/orders/");

                        System.out.print(" Δώσε όνομα παραγγελίας (χωρίς .json): ");
                        String orderName = scanner.nextLine().trim();

                        String orderPath = "/home/dimitris/Documents/OPA/DS/Ergasia/AuebFoodApp/resources/orders/" + orderName + ".json";
                        Order order = readOrderFromJson(orderPath);

                        if (order == null) break;

                        System.out.println("Παραγγελία διαβάστηκε: " + order);
                        Request reqOrder = new Request("ADD_ORDER", order);
                        out.writeObject(reqOrder);
                        out.flush();

                        Response resp2 = (Response) in.readObject();
                        System.out.println(" Επιστροφή στο μενού μετά την παραγγελία");

                        System.out.println("Απάντηση: " + resp2.getMessage());

                        break;

                    case 3:
                        System.out.print("Κατάστημα: ");
                        String storeName = scanner.nextLine();

                        System.out.print("Όνομα προϊόντος: ");
                        String productName = scanner.nextLine();

                        System.out.print("Ενέργεια (ADD / REMOVE / REDUCE): ");
                        String action = scanner.nextLine().toUpperCase();

                        String productType = "unknown";
                        int quantity = 1;
                        double price = 0.0;

                        if ("ADD".equals(action)) {
                            System.out.print("Τύπος προϊόντος (ή Enter για καμία αλλαγή): ");
                            productType = scanner.nextLine().trim();
                            if (productType.isEmpty()) productType = "null";

                            System.out.print("Ποσότητα διαθέσιμη (ή -1 για καμία αλλαγή): ");
                            quantity = Integer.parseInt(scanner.nextLine().trim());

                            System.out.print("Τιμή προϊόντος (ή -1 για καμία αλλαγή): ");
                            price = Double.parseDouble(scanner.nextLine().trim());
                        } else if ("REDUCE".equals(action)) {
                            System.out.print("Ποσότητα προς αφαίρεση: ");
                            quantity = Integer.parseInt(scanner.nextLine().trim());
                        }

                        UpdateProductRequest upr = new UpdateProductRequest(
                                storeName, productName, productType, quantity, price, action
                        );

                        Request req = new Request("UPDATE_PRODUCTS", upr);
                        out.writeObject(req);
                        out.flush();

                        Response resp = (Response) in.readObject();
                        System.out.println("Απάντηση: " + resp.getMessage());
                        break;


                    case 4:
                        if (addedStores.isEmpty()) {
                            System.out.println(" Δεν υπάρχουν καταχωρημένα καταστήματα.");
                            break;
                        }

                        System.out.println(" Καταχωρημένα καταστήματα:");
                        for (String storeKey : addedStores) {
                            System.out.println(" - " + storeKey);
                        }

                        System.out.print(" Δώσε όνομα καταστήματος (π.χ. Pizza Fun): ");
                        String storeKey = scanner.nextLine().trim();

                        Request getProductsReq = new Request("GET_PRODUCTS", storeKey);
                        out.writeObject(getProductsReq);
                        out.flush();

                        Response productResp = (Response) in.readObject();
                        Object payload = productResp.getData(); //  χρησιμοποιούμε getData()

                        if (productResp.isSuccess()) {
                            if (payload instanceof List<?>) {
                                List<Product> products = new ArrayList<>();
                                for (Object o : (List<?>) payload) {
                                    if (o instanceof Product) {
                                        products.add((Product) o);
                                    }
                                }

                                if (products.isEmpty()) {
                                    System.out.println(" Το κατάστημα δεν έχει προϊόντα.");
                                } else {
                                    System.out.println(" Προϊόντα καταστήματος " + storeKey + ":");
                                    for (Product p : products) {
                                        System.out.printf(" - %s (%s) - %.2f€, Διαθέσιμα: %d\n",
                                                p.getProductName(), p.getProductType(), p.getPrice(), p.getAvailableAmount());
                                    }
                                }

                            } else {
                                System.out.println(" Το payload δεν ήταν λίστα προϊόντων.");
                            }

                        } else {
                            System.out.println(productResp.getMessage());
                        }

                        break;

                    case 5:
                        Request req1 = new Request("PRODUCT_SALES", null);
                        out.writeObject(req1);
                        out.flush();

                        Response resp1 = (Response) in.readObject();
                        Object Prodpayload = resp1.getData();

                        if (Prodpayload instanceof Map<?, ?> rawMap) {
                            Map<String, Integer> qtyMap = new HashMap<>();
                            Map<String, Double> revMap = new HashMap<>();

                            System.out.println(" Πωλήσεις ανά προϊόν:");

                            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                                String product = (String) entry.getKey();
                                Object value = entry.getValue();

                                if (value instanceof Map<?, ?> dataMap) {
                                    Integer qty = ((Number) dataMap.get("quantity")).intValue();
                                    Double rev = ((Number) dataMap.get("revenue")).doubleValue();

                                    qtyMap.put(product, qty);
                                    revMap.put(product, rev);
                                }
                            }

                            if (qtyMap.isEmpty()) {
                                System.out.println(" Δεν υπάρχουν καταγεγραμμένες πωλήσεις.");
                            } else {
                                qtyMap.keySet().stream()
                                        .sorted()
                                        .forEach(prod -> {
                                            int qty = qtyMap.get(prod);
                                            double rev = revMap.getOrDefault(prod, 0.0);
                                            System.out.printf(" - %s: %d τεμάχια / %.2f€\n", prod, qty, rev);
                                        });
                            }

                        } else {
                            System.out.println(" Το αποτέλεσμα δεν ήταν έγκυρο.");
                            System.out.println(" Payload class: " + (Prodpayload != null ? Prodpayload.getClass().getName() : "null"));
                            System.out.println(" Περιεχόμενο: " + Prodpayload);
                        }
                        break;

                    case 6:
                        Request revReq = new Request("CATEGORY_REVENUE", null);
                        out.writeObject(revReq);
                        out.flush();

                        Response revResp = (Response) in.readObject();
                        Object payloadRev = revResp.getData();

                        if (payloadRev instanceof Map<?, ?>) {
                           Map<String, Double> revenues = new HashMap<>();
                           Map<?, ?> raw = (Map<?, ?>) payloadRev;

                            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                               if (entry.getKey() instanceof String && entry.getValue() instanceof Double) {
                                    revenues.put((String) entry.getKey(), (Double) entry.getValue());
                                }
                            }

                            if (revenues.isEmpty()) {
                                System.out.println(" Δεν υπάρχουν ακόμα καταχωρημένα έσοδα.");
                            } else {
                                System.out.println(" Έσοδα ανά τύπο καταστήματος:");
                               revenues.forEach((cat, total) ->
                                    System.out.printf(" - %s: %.2f€\n", cat, total));
                           }
                        } else {
                           System.out.println(" Το payload δεν ήταν έγκυρος πίνακας.");
                        }
                       break;


                    case 7:
                        System.out.print("Δώσε κατηγορία προϊόντος (π.χ. salad, pizza, coffee): ");
                        String prodCategory = scanner.nextLine();

                        Request reqCat = new Request("CATEGORY_PRODUCT_SALES", prodCategory);
                        out.writeObject(reqCat);
                        out.flush();

                        Response respCat = (Response) in.readObject();
                        Object apayload = respCat.getData();

                        if (apayload instanceof Map<?, ?>) {
                            Map<String, Double> revenueMap = new HashMap<>();
                            Map<?, ?> raw = (Map<?, ?>) apayload;

                            System.out.println(" Έσοδα προϊόντων κατηγορίας: " + prodCategory);
                            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                                String store = (String) entry.getKey();
                                double rev = ((Number) entry.getValue()).doubleValue();

                                if (!store.equals("total")) {
                                    System.out.printf(" - %s: %.2f€\n", store, rev);
                                } else {
                                    System.out.printf("Σύνολο: %.2f€\n", rev);
                                }
                            }

                        } else {
                            System.out.println(" Το αποτέλεσμα δεν ήταν έγκυρο.");
                        }
                        break;


                    case 0:
                        System.out.println("Έξοδος...");
                        return;

                    default:
                        System.out.println(" Μη έγκυρη επιλογή");
                }
            }

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.err.println(" Σφάλμα στο Manager: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Εισαγωγή καταστημάτων από το αντίστοιχο json αρχείο
    private static Store readStoreFromJson(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(filename), Store.class);
        } catch (IOException e) {
            System.err.println(" Σφάλμα ανάγνωσης store.json: " + e.getMessage());
            return null;
        }
    }
    // Εισαγωγή παραγγελίας από το αντίστοιχο json αρχείο
    private static Order readOrderFromJson(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(filename), Order.class);
        } catch (IOException e) {
            System.err.println(" Σφάλμα ανάγνωσης order.json: " + e.getMessage());
            return null;
        }
    }
    // Εμφάνιση διαθέσιμων καταστημάτων για εισαγωγή
    private static void printAvailableStores(String folderPath, List<String> exclude) {
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println(" Ο φάκελος δεν υπάρχει ή δεν είναι φάκελος.");
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

        if (files == null || files.length == 0) {
            System.out.println(" Δεν βρέθηκαν διαθέσιμα καταστήματα στο φάκελο.");
            return;
        }

        System.out.println(" Διαθέσιμα καταστήματα για εισαγωγή:");

        for (File file : files) {
            String fileName = file.getName().replace(".json", "");
            if (!exclude.contains(fileName)) {
                System.out.println("  - " + fileName);
            }
        }
    }
    // Εμφάνιση διαθέσιμων παραγγελιών για καταχώρηση
    private static void printAvailableOrders(String folderPath) {
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println(" Ο φάκελος παραγγελιών δεν υπάρχει ή δεν είναι φάκελος.");
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

        if (files == null || files.length == 0) {
            System.out.println(" Δεν βρέθηκαν διαθέσιμες παραγγελίες στο φάκελο.");
            return;
        }

        System.out.println(" Διαθέσιμες παραγγελίες:");
        for (File file : files) {
            String fileName = file.getName().replace(".json", "");
            System.out.println("  - " + fileName);
        }
    }


}
