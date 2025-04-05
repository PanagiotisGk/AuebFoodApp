package managerapp;


import common.model.Order;
import common.model.Request;
import common.model.Response;
import common.model.Store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
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
                System.out.println("1. Καταχώρηση καταστημάτων (store.json)");
                System.out.println("2. Καταχώρηση παραγγελίας (order.json)");
                System.out.println("0. Έξοδος");
                System.out.print("👉 Επιλογή: ");
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        List<Store> stores = readStoresFromJson("store.json");
                        if (stores == null) break;

                        for (Store store : stores) {
                            System.out.println("📦 Κατάστημα διαβάστηκε: " + store);
                            Request reqStore = new Request("ADD_STORE", store);
                            out.writeObject(reqStore);
                            out.flush();

                            Response resp1 = (Response) in.readObject();
                            System.out.println("📥 Απάντηση: " + resp1.getMessage());

                            Thread.sleep(100); // μικρή καθυστέρηση
                        }
                        break;

                    case 2:
                        Order order = readOrderFromJson("order.json");
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


    private static List<Store> readStoresFromJson(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(filename), new TypeReference<List<Store>>() {});
        } catch (IOException e) {
            System.err.println("❌ Σφάλμα ανάγνωσης store.json: " + e.getMessage());
            return null;
        }
    }


    private static Order readOrderFromJson(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(filename), Order.class);
        } catch (IOException e) {
            System.err.println("❌ Σφάλμα ανάγνωσης order.json: " + e.getMessage());
            return null;
        }
    }
}
