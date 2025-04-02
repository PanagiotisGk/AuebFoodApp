package managerapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.model.Request;
import common.model.Response;
import common.model.Store;

import java.io.*;
import java.net.Socket;

public class ManagerApp {

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 5000;
        String jsonFilePath = "store.json"; // φτιάξε αυτό το αρχείο με βάση την εκφώνηση

        try {
            // 1. Διάβασε το JSON και μετατροπή σε Store
            ObjectMapper mapper = new ObjectMapper();
            Store store = mapper.readValue(new File(jsonFilePath), Store.class);
            System.out.println("📦 Κατάστημα διαβάστηκε: " + store);

            // 2. Συνδέσου στον Master
            Socket socket = new Socket(serverAddress, port);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // 3. Στείλε το κατάστημα στον Master
            Request request = new Request("ADD_STORE", store);
            out.writeObject(request);
            System.out.println("📤 Αίτημα ADD_STORE στάλθηκε");

            // 4. Λάβε απάντηση
            Response response = (Response) in.readObject();
            System.out.println("📥 Απάντηση: " + response.getMessage());

            socket.close();

        } catch (Exception e) {
            System.err.println("❌ Σφάλμα: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

