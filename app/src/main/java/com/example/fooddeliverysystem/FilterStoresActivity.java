package com.example.fooddeliverysystem;

import common.model.*;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;


public class FilterStoresActivity extends AppCompatActivity {

    private EditText edtCategory, edtStars, edtPrice;
    private Button btnSearch, btnBackHome;
    private RecyclerView recyclerView;
    private StoreAdapter storeAdapter;

    private static final String SERVER_ADDRESS = "192.168.1.46";
    private static final int PORT = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_stores);

        edtCategory = findViewById(R.id.edtCategory);
        edtStars = findViewById(R.id.edtStars);
        edtPrice = findViewById(R.id.edtValuation);
        btnSearch = findViewById(R.id.btnSearch);
        btnBackHome = findViewById(R.id.btnBackHome);
        recyclerView = findViewById(R.id.recyclerViewFiltered);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Custom Search Filters Button
        btnSearch.setOnClickListener(v -> fetchFilteredStores());

        // Return Home Button
        btnBackHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(FilterStoresActivity.this, HomeActivity.class));
            }
        });
    }

    private void fetchFilteredStores() {
        new Thread(() -> {
            try {
                Socket socket = new Socket(SERVER_ADDRESS, PORT);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                String category = edtCategory.getText().toString().trim();
                int stars = Integer.parseInt(edtStars.getText().toString().trim());
                String[] priceLevels = edtPrice.getText().toString().trim().split(",");

                SearchFilters filters = new SearchFilters(
                        37.9755, 23.7348,
                        category.isEmpty() ? null : List.of(category),
                        stars,
                        Arrays.asList(priceLevels)
                );

                Request request = new Request("FILTER_STORES", filters);
                out.writeObject(request);

                Response response = (Response) in.readObject();
                List<Store> stores = (List<Store>) response.getData();

                runOnUiThread(() -> {
                    if (stores == null || stores.isEmpty()) {
                        Toast.makeText(this, "Δεν βρέθηκαν καταστήματα με αυτά τα φίλτρα", Toast.LENGTH_LONG).show();
                    } else {
                        storeAdapter = new StoreAdapter(stores);
                        recyclerView.setAdapter(storeAdapter);
                    }
                });

                in.close();
                out.close();
                socket.close();
            } catch (Exception e) {
                Log.e("FilterStores", "Σφάλμα: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "Σφάλμα σύνδεσης", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}