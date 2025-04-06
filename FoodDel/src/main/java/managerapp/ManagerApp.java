package managerapp;

import common.model.Order;
import common.model.Request;
import common.model.Response;
import common.model.Store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;

import java.io.*;
import java.net.Socket;
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
            System.out.println("📡 Συνδέθηκα με τον Master");

            
            while (true) {
                System.out.println("\n===== MENU =====");
                System.out.println("1. Καταχώρηση καταστημάτων ");
                System.out.println("2. Καταχώρηση παραγγελίας (order.json)");
                System.out.println("0. Έξοδος");
                System.out.print("👉 Επιλογή: ");
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        List<String> addedStores = new ArrayList<>();

                        String path = "";
                        // Ερώτηση στον χρήστη για το άν θέλει να βρει το .json με το full path ή χωρίς
                        while (true) {
                            System.out.println("Χρειάζεται το full path για την εύρεση του καταστήματος ή όχι;;; (Απάντησε με YES ή NO) ");
                            String fullPathResources = scanner.nextLine().trim();

                            if (fullPathResources.isEmpty()) {
                                System.out.println("⚠️ Δεν έδωσες απάντηση. Προσπάθησε ξανά.");
                                continue;
                            }
                            if (fullPathResources.equalsIgnoreCase("yes") || fullPathResources.equalsIgnoreCase("y")) {
                                System.out.println("✅ Επιλέχθηκε χρήση full path για καταστήματα.");
                                path = "/home/dimitris/Documents/OPA/DS/Ergasia/AuebFoodApp/resources/";
                            } else {
                                System.out.println("❌ Δεν θα χρησιμοποιηθεί full path για καταστήματα.");
                                // path = storeName + ".json";
                            }
                            break;
                        }
                        // Εκτύπωση των διαθέσιμων καταστημάτων για εισαγωγή και επιλογή καταστήματος για προσθήκη
                        while (true) {
                            // ✅ Εκτύπωση διαθέσιμων αρχείων καταστημάτων
                            printAvailableStores(path);
                            System.out.print("📍 Δώσε το όνομα του καταστήματος με την μορφή π.χ. Dommino_Pizza (ή 'τέλος' για έξοδο στο menu): ");
                            String storeName = scanner.nextLine().trim();
                    
                            if (storeName.equalsIgnoreCase("τέλος") || storeName.equalsIgnoreCase("τελος")) break;
                            path += storeName + ".json"; 
                    
                            if (addedStores.contains(storeName)) {
                                System.out.println("⚠️ Το κατάστημα '" + storeName + "' έχει ήδη προστεθεί. Μπορείς να προσθέσεις κάποιο άλλο εάν θέλεις...");
                                continue;
                            }                            
                            Store store = readStoreFromJson(path);
                    
                            if (store == null) continue;
                    
                            System.out.println("📦 Κατάστημα διαβάστηκε: " + store);
                            Request reqStore = new Request("ADD_STORE", store);
                            out.writeObject(reqStore);
                            out.flush();
                    
                            Response resp = (Response) in.readObject();
                            System.out.println("📥 Απάντηση: " + resp.getMessage());
                    
                            addedStores.add(storeName);
                            Thread.sleep(100);
                            break;
                        }
                        break;
                        
                    case 2:
                        Order order = readOrderFromJson("/home/dimitris/Documents/OPA/DS/Ergasia/AuebFoodApp/order.json");
                        if (order == null) break;

                        System.out.println("🛒 Παραγγελία διαβάστηκε: " + order);
                        Request reqOrder = new Request("ADD_ORDER", order);
                        out.writeObject(reqOrder);
                        out.flush();

                        Response resp2 = (Response) in.readObject();
                        System.out.println("📥 Απάντηση: " + resp2.getMessage());
                        break;

                    case 0:
                        System.out.println("👋 Έξοδος...");
                        return;

                    default:
                        System.out.println("❌ Μη έγκυρη επιλογή");
                }
            }

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.err.println("❌ Σφάλμα στο Manager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Συνάρτηση η οποία διαβάζει τα στοιχεία κάθε καταστήματα από το αντίστοιχο json
    private static Store readStoreFromJson(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(filename), Store.class);
        } catch (IOException e) {
            System.err.println("❌ Σφάλμα ανάγνωσης store.json: " + e.getMessage());
            return null;
        }
    }

    // Συνάρτηση η οποία διαβάζει τα στοιχεία κάθε παραγγελίας από το αντίστοιχο json
    private static Order readOrderFromJson(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(filename), Order.class);
        } catch (IOException e) {
            System.err.println("❌ Σφάλμα ανάγνωσης order.json: " + e.getMessage());
            return null;
        }
    }
    // Συνάρτηση για την εμφάνιση των διαθέσιμων καταστημάτων για εισαγωγή
    private static void printAvailableStores(String folderPath) {
        File folder = new File(folderPath);
    
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("❌ Ο φάκελος δεν υπάρχει ή δεν είναι φάκελος.");
            return;
        }
    
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
    
        if (files == null || files.length == 0) {
            System.out.println("⚠️ Δεν βρέθηκαν διαθέσιμα καταστήματα στο φάκελο.");
            return;
        }
    
        System.out.println("📋 Διαθέσιμα καταστήματα για εισαγωγή:");
        for (File file : files) {
            String fileName = file.getName().replace(".json", "");
            System.out.println("  - " + fileName);
        }
    }
}
