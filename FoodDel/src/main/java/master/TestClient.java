package master;

import common.model.Request;
import common.model.Response;
import common.model.Product;
import common.model.Store;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

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
                            System.out.println("⚠️ Δεν υπάρχουν καταστήματα καταχωρημένα.");
                        } else {
                            System.out.println("📋 Όλα τα καταστήματα:");
                            for (Store store : stores) {
                                System.out.println("- " + store.getStoreName() + " (" + store.getFoodCategory() + ")");
                            }
                        }
                        break;

                    case 2:
                        // 🔍 Ζήτα προϊόντα συγκεκριμένου καταστήματος
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
                                    System.out.println("⚠️ Δεν υπάρχουν προϊόντα στο κατάστημα.");
                                } else {
                                    System.out.println("📦 Προϊόντα καταστήματος:");
                                    for (Product p : products) {
                                        System.out.printf("- %s (%s) - %.2f€, Διαθέσιμα: %d\n",
                                                p.getProductName(), p.getProductType(), p.getPrice(), p.getAvailableAmount());
                                    }
                                }
                            } else {
                                System.out.println("⚠️ Το payload δεν ήταν λίστα προϊόντων.");
                            }
                        } else {
                            System.out.println("❌ " + prodResp.getMessage());
                        }
                        break;

                    default:
                        System.out.println("❌ Μη έγκυρη επιλογή. Δοκίμασε ξανά.");
                }
            }

            System.out.println("✅ Κλείσιμο σύνδεσης TestClient με τον Master...");

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Σφάλμα στον TestClientV2: " + e.getMessage());
        }
    }
}
