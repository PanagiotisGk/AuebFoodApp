package master;

import common.model.Request;
import common.model.Response;
import common.model.Product;
// import common.model.SearchFilters;
// import common.model.Store;
import common.model.SearchFilters;
import common.model.Store;

import java.io.*;
import java.net.Socket;
// import java.util.List;
import java.util.Comparator;
import java.util.List;

public class TestClient {

    public static void main(String[] args) {
        String serverAddress = "localhost"; // IP στην οποία τρέχει ο MasterServer
        int port = 5000;

        try (
                Socket socket = new Socket(serverAddress, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            System.out.println("Συνδέθηκα με τον Master Server!");

            // ---------- Δημιουργία dummy request ------------
            Request pingRequest = new Request("PING", null);
            out.writeObject(pingRequest);
            System.out.println("Έστειλα request: " + pingRequest.getType());

            // Λήψη response
            Response response = (Response) in.readObject();
            System.out.println("Λήφθηκε απάντηση: " + response.getMessage());
            // Τέλος dummy request

            // ------------- Εύρεση όλων των Καταχωρημένων Καταστημάτων ----------------
            SearchFilters filtersForAllStores = new SearchFilters(37.9755, 23.7348, null, 0, null);
            Request searchRequestForAllStores = new Request("SEARCH_ALL_STORES", filtersForAllStores);
            out.writeObject(searchRequestForAllStores);

            // Λήψη response
            Response responseAllStores = (Response) in.readObject();
            List<Store> allStores = (List<Store>) responseAllStores.getData();
            if (allStores.isEmpty()) {
                System.out.println(" \n ---------- Δεν βρέθηκαν καταχωρημένα καταστήματα ----------");
            } else {
                System.out.println(" \n ---------- Καταχωρημένα Καταστήματα ----------");
                allStores.sort(Comparator.comparingDouble(Store::getStars).reversed());
                int i = 1;
                for (Store store : allStores) {
                    System.out.println("\n" + i + ". " + store.getStoreName() + "  " + store.getStoreLogo());
                    System.out.println("    Κατηγορία: " + store.getFoodCategory());
                    System.out.println("    Βαθμολογία: " + store.getStars() );
                    System.out.println("  Διαθέσιμα Προϊόντα :");
                    int j = 1;
                    for (Product product : store.getProducts()){
                        System.out.println("    " + j +": " + product );
                        j++;
                    }
                    i++;
                }
            }
            // ------------- Τέος Εύρεσης Καταστημάτων σε απόσταση 5km από τον Πελάτη ----------------

            // ------------- Εύρεση Καταστημάτων σε απόσταση 5km από τον Πελάτη ----------------
            SearchFilters filtersFor5kmRange = new SearchFilters(37.9755, 23.7348, null, 0, null);
            Request searchRequest5km = new Request("SEARCH_5KM_RANGE", filtersFor5kmRange);
            out.writeObject(searchRequest5km);

            // Λήψη response
            Response response5km = (Response) in.readObject();
            List<Store> stores = (List<Store>) response5km.getData();
            if (stores.isEmpty()) {
                System.out.println(" \n ---------- Δεν βρέθηκαν κοντινά καταστήματα σε ακτίνα 5km. ----------");
            } else {
                System.out.println(" \n ---------- Κοντινά Καταστήματα (σε ακτίνα 5km) ----------");
                stores.sort(Comparator.comparingDouble(Store::getStars).reversed());
                int i = 1;
                for (Store store : stores) {
                    System.out.println("\n" + i + ". " + store.getStoreName() + "  " + store.getStoreLogo());
                    System.out.println("    Κατηγορία: " + store.getFoodCategory());
                    System.out.println("    Βαθμολογία: " + store.getStars() );
                    System.out.println("  Διαθέσιμα Προϊόντα : ");
                    int j = 1;
                    for (Product product : store.getProducts()){
                        System.out.println("    " + j +": " + product );
                        j++;
                    }
                    i++;
                }
            }
            // ------------- Τέος Εύρεσης Καταστημάτων σε απόσταση 5km από τον Πελάτη ----------------

            // ------------- Εύρεση καταστημάτων με βάση τα φίλτρα (Απόσταση, Τύπος Καταστήματος, Ακρίβεια Καταστήματος) -------------
            SearchFilters filtersForStores = new SearchFilters(
                37.9755, 23.7348, 
                List.of("pizzeria"), 
                3,                         
                List.of("$$", "$$$") 
            );

            Request filterRequest = new Request("FILTER_STORES", filtersForStores);
            out.writeObject(filterRequest);

            // Λήψη απάντησης
            Response filterResponse = (Response) in.readObject();
            List<Store> filteredStores = (List<Store>) filterResponse.getData();

            if (filteredStores.isEmpty()) {
                System.out.println(" ----------Δεν βρέθηκαν καταστήματα που να ταιριάζουν στα φίλτρα. ----------");
            } else {
                System.out.println("\n Βρέθηκαν " + filteredStores.size() + " καταστήματα με βάση τα φίλτρα σας: \n");
                int i = 1;
                for (Store store : filteredStores) {
                    System.out.println(i + ". " + store.getStoreName());
                    System.out.println("Κατηγορία: " + store.getFoodCategory());
                    System.out.println("Βαθμολογία: " + store.getStars() );
                    System.out.println("Τιμή: " + store.getPriceCategory());
                    i++;
                }
            }
            // ------------- Τέλος Εύρεσης καταστημάτων με βάση τα φίλτρα -------------
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Σφάλμα στον Test Client: " + e.getMessage());
        }
    }
}