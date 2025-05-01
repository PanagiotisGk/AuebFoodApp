package com.example.fooddeliverysystem;

import common.model.*;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;


public class NearbyStoresActivity extends AppCompatActivity {

    private ImageButton btnBackHome;
    private RecyclerView recyclerView;
    private StoreAdapter storeAdapter;
    private static final String SERVER_ADDRESS = "192.168.1.46"; // Ip masterServer
    private static final int PORT = 5000; // Port masterServer


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_stores);

        btnBackHome = findViewById(R.id.btnBackHome);
        recyclerView = findViewById(R.id.recyclerViewStores);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchNearbyStores();

        // Return Home Button
        btnBackHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(NearbyStoresActivity.this, HomeActivity.class));
            }
        });
    }

    private void fetchNearbyStores() {
        new Thread(() -> {
            try {
                Log.d("NearbyStores", "Ξεκινά σύνδεση με τον server...");
                Socket socket = new Socket(SERVER_ADDRESS, PORT);
                Log.d("NearbyStores", "Συνδέθηκε με τον server");
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                Log.d("NearbyStores", "Έστειλε φίλτρα...");
                SearchFilters filters = new SearchFilters(37.9755, 23.7348, null, 0, null);
                Request request = new Request("SEARCH_5KM_RANGE", filters);
                out.writeObject(request);

                Response response = (Response) in.readObject();
                Log.d("NearbyStores", "Έλαβε απάντηση από server");
                List<Store> stores = (List<Store>) response.getData();
                Log.d("NearbyStores", "Αριθμός καταστημάτων: " + (stores != null ? stores.size() : "null"));

                runOnUiThread(() -> {
                    if (stores == null || stores.isEmpty()) {
                        Toast.makeText(this, "Δεν βρέθηκαν κοντινά καταστήματα", Toast.LENGTH_LONG).show();
                    } else {
                        storeAdapter = new StoreAdapter(stores);
                        recyclerView.setAdapter(storeAdapter);
                    }
                });

                in.close();
                out.close();
                socket.close();
            } catch (Exception e) {
                Log.e("NearbyStores", "Σφάλμα: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Σφάλμα κατά την επικοινωνία με τον server", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
