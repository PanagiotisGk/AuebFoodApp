package managerapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import common.model.Request;
import common.model.Response;
import common.model.Store;

import java.io.*;
import java.net.Socket;

public class ManagerApp {

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 5000;
        String jsonFilePath = "/home/dimitris/Documents/9.OPA/DS/Ergasia/AuebFoodApp/FoodDel/store.json"; // φτιάξε αυτό το αρχείο με βάση την εκφώνηση

        try {
            // 1. Διάβασε το JSON και μετατροπή σε Store
            // ObjectMapper mapper = new ObjectMapper();
            // Store store = mapper.readValue(new File(jsonFilePath), Store.class);
            // System.out.println("📦 Κατάστημα διαβάστηκε: " + store);

            ObjectMapper mapper = new ObjectMapper();
            List<Store> stores = mapper.readValue(new File(jsonFilePath), new TypeReference<List<Store>>() {}
            );

            // Εκτύπωση των καταστημάτων για έλεγχο
            stores.forEach(store -> System.out.println("📍 Κατάστημα: " + store.getStoreName()));

            // 2. Συνδέσου στον Master
            Socket socket = new Socket(serverAddress, port);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // 3. Στείλε το κατάστημα στον Master
            // Request request = new Request("ADD_STORE", stores);
            // out.writeObject(request);
            // System.out.println("📤 Αίτημα ADD_STORE στάλθηκε");

            // // 4. Λάβε απάντηση
            // Response response = (Response) in.readObject();
            // System.out.println("📥 Απάντηση: " + response.getMessage());

            for (Store store : stores) {
                Request request = new Request("ADD_STORE", store);
                out.writeObject(request);
                out.flush();
            
                // 🔍 Περιμένουμε το Response πριν προχωρήσουμε
                Response response = (Response) in.readObject();
                System.out.println("📩 Απάντηση από server: " + response.getMessage());
            
                // 🚀 Προσθέτουμε μικρή καθυστέρηση για σταθερότητα
                Thread.sleep(100);
            }

            socket.close();

        } catch (Exception e) {
            System.err.println("❌ Σφάλμα: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

