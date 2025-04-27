package master;

import common.model.Request;
import common.model.Response;
import common.model.Product;
import common.model.Store;
import common.model.RateStoreRequest;
import common.model.SearchFilters;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.Comparator;

public class TestClient {

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 5000;

        try (
                Socket socket = new Socket(serverAddress, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Συνδέθηκα με τον Master Server!");

            while (true) {
                System.out.println("\n===== MENU =====");
                System.out.println("1. Δες όλα τα καταστήματα");
                System.out.println("2. Δες προϊόντα συγκεκριμένου καταστήματος");
                System.out.println("3. Βρες τα κοντινά καταστήματα σε σένα (Απόσταση < 5km )");
                System.out.println("4. Βρες καταστήματα με βάση τα φίλτρα της αναζήτησης");
                System.out.println("5. Αξιολόγησε ένα κατάστημα");
                System.out.println("0. Έξοδος");
                System.out.print("Επιλογή: ");
                int choice = Integer.parseInt(scanner.nextLine().trim());

                if (choice == 0) {
                    System.out.println("Έξοδος από TestClient...");
                    break;
                }

                switch (choice) {
                    case 1:
                        // Ζήτα όλα τα καταστήματα
                        Request searchAllStores = new Request("SEARCH_ALL_STORES", null);
                        out.writeObject(searchAllStores);
                        out.flush();

                        Response respStores = (Response) in.readObject();
                        List<Store> stores = (List<Store>) respStores.getData();

                        if (stores.isEmpty()) {
                            System.out.println(" Δεν υπάρχουν καταστήματα καταχωρημένα.");
                        } else {
                            System.out.println(" Όλα τα καταστήματα:");
                            for (Store store : stores) {
                                System.out.println("\n" + store.getStoreName() + "  " + store.getStoreLogo());
                                System.out.println("    Κατηγορία: " + store.getFoodCategory());
                                System.out.println("    Βαθμολογία: " + store.getStarsFormatted() );
                                System.out.println("    Πλήθος ψήφων: " + store.getNoOfVotes() );
                                System.out.println("  Διαθέσιμα Προϊόντα :");
                                int j = 1;
                                for (Product product : store.getProducts()){
                                    System.out.println("    " + j +": " + product );
                                    j++;
                                }
                            }
                        }
                        break;

                    case 2:
                        //  Ζήτα προϊόντα συγκεκριμένου καταστήματος
                        System.out.print("Δώσε όνομα καταστήματος: ");
                        String storeName = scanner.nextLine().trim();

                        Request getProductsReq = new Request("GET_PRODUCTS", storeName);
                        out.writeObject(getProductsReq);
                        out.flush();

                        Response prodResp = (Response) in.readObject();
                        Object payload = prodResp.getData();

                        if (prodResp.isSuccess()) {
                            if (payload instanceof List<?>) {
                                List<Product> products = (List<Product>) payload;

                                if (products.isEmpty()) {
                                    System.out.println(" Δεν υπάρχουν προϊόντα στο κατάστημα.");
                                } else {
                                    System.out.println(" Προϊόντα καταστήματος:");
                                    for (Product p : products) {
                                        System.out.printf("- %s (%s) - %.2f€, Διαθέσιμα: %d\n",
                                                p.getProductName(), p.getProductType(), p.getPrice(), p.getAvailableAmount());
                                    }
                                }
                            } else {
                                System.out.println(" Το payload δεν ήταν λίστα προϊόντων.");
                            }
                        } else {
                            System.out.println(" " + prodResp.getMessage());
                        }
                        break;

                    case 3:
                        // example of cordinates (37.9755, 23.7348)
                        //  Ζήτα τις συντεταγμένες του πελάτη
                        System.out.print("Δώσε clientLatitude: ");
                        double clLatitude = Double.parseDouble(scanner.nextLine().trim());
                        System.out.print("Δώσε clientLongitude: ");
                        double clLongitude = Double.parseDouble(scanner.nextLine().trim());

                        // SearchFilters filtersFor5kmRange = new SearchFilters(37.9755, 23.7348, null, 0, null);
                        SearchFilters filtersFor5kmRange = new SearchFilters(clLatitude, clLongitude, null, 0, null);
                        Request searchRequest5km = new Request("SEARCH_5KM_RANGE", filtersFor5kmRange);
                        out.writeObject(searchRequest5km);

                        // Λήψη response
                        Response response5km = (Response) in.readObject();
                        List<Store> stores5km = (List<Store>) response5km.getData();
                        if (stores5km.isEmpty()) {
                            System.out.println(" \n ---------- Δεν βρέθηκαν κοντινά καταστήματα σε ακτίνα 5km. ----------");
                        } else {
                            System.out.println(" \n ---------- Κοντινά Καταστήματα (σε ακτίνα 5km) ----------");
                            stores5km.sort(Comparator.comparingDouble(Store::getStars).reversed());
                            int i = 1;
                            for (Store store : stores5km) {
                                System.out.println("\n" + i + ". " + store.getStoreName() + "  " + store.getStoreLogo());
                                System.out.println("    Κατηγορία: " + store.getFoodCategory());
                                System.out.println("    Βαθμολογία: " + store.getStarsFormatted() );
                                System.out.println("    Πλήθος ψήφων: " + store.getNoOfVotes() );
                                System.out.println("  Διαθέσιμα Προϊόντα : ");
                                int j = 1;
                                for (Product product : store.getProducts()){
                                    System.out.println("    " + j +": " + product );
                                    j++;
                                }
                                i++;
                            }
                        }                        
                        break;

                    case 4: 
                        // example of cordinates (37.9755, 23.7348)

                        //  Ζήτα τις συντεταγμένες του πελάτη
                        System.out.print("Δώσε clientLatitude: ");
                        double cLatitude = Double.parseDouble(scanner.nextLine().trim());
                        System.out.print("Δώσε clientLongitude: ");
                        double cLongitude = Double.parseDouble(scanner.nextLine().trim());
                        System.out.print("Δώσε Τύπο μαγαζιού: ");
                        String storeType = scanner.nextLine().trim();
                        System.out.print("Δώσε ελάχιστη αξιολόγηση σε αστέρια: ");
                        int minstars = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("Δώσε ελάχιστη κατηγορία ακρίβειας σε $ π.χ $,$$,$$$ : ");
                        String priceCategory = scanner.nextLine().trim();

                        SearchFilters filtersForStores = new SearchFilters(
                            cLatitude, cLongitude, 
                            List.of(storeType), 
                            minstars,                         
                            List.of(priceCategory) 
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
                                System.out.println("Βαθμολογία: " + store.getStarsFormatted() );
                                System.out.println("Πλήθος ψήφων: " + store.getNoOfVotes() );
                                System.out.println("Τιμή: " + store.getPriceCategory());
                                i++;
                            }
                        }
                        break;

                    case 5:
                        // Βαθμολόγησης Καταστήματος
                        System.out.print("Δώσε όνομα καταστήματος: ");
                        String nameStore = scanner.nextLine().trim();
                        System.out.print("Δώσε την βαθμολογία που επιθυμείς να βάλεις στο κατάστημα (Η βαθμολογία πρέπει να είναι από 1 έως 5): ");
                        int stars = Integer.parseInt(scanner.nextLine().trim());

                        RateStoreRequest rateRequest = new RateStoreRequest(nameStore, stars);
                        Request ratingRequest = new Request("RATE_STORE", rateRequest);
                        out.writeObject(ratingRequest);

                        Response ratingResponse = (Response) in.readObject();
                        System.out.println("Αποτέλεσμα Βαθμολόγησης: " + ratingResponse.getMessage());
                        break;

                    default:
                        System.out.println(" Μη έγκυρη επιλογή. Δοκίμασε ξανά.");
                }
            }

            System.out.println(" Κλείσιμο σύνδεσης TestClient με τον Master...");

        } catch (IOException | ClassNotFoundException e) {
            System.err.println(" Σφάλμα στον TestClientV2: " + e.getMessage());
        }
    }
}
