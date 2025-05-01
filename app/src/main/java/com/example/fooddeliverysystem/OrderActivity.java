package com.example.fooddeliverysystem;

import common.model.*;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderActivity extends AppCompatActivity {

    private EditText edtStoreName, edtProduct1, edtQuantityProduct1, edtCost;
    private RecyclerView recyclerStores;
    private StoreAdapter storeAdapter;
    private String selectedCategory = null;

    private Button btnRegisterOrder, btnBackHome, btnAddProduct;
    private TextView txtAddedProducts;
    private Map<String, Integer> productsOrdered = new HashMap<>();
    private static final String SERVER_ADDRESS = "192.168.1.46";
    private static final int PORT = 5000;

    @Override
    protected void onStart() {
        super.onStart();
        fetchAvailableStores();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        edtStoreName = findViewById(R.id.edtStoreName);
        edtProduct1 = findViewById(R.id.edtProduct1);
        edtQuantityProduct1 = findViewById(R.id.edtQuantityProduct1);
        edtCost = findViewById(R.id.edtCost);
        btnRegisterOrder = findViewById(R.id.btnRegisterOrder);
        btnBackHome = findViewById(R.id.btnBackHome);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        txtAddedProducts = findViewById(R.id.txtAddedProducts);

        recyclerStores = findViewById(R.id.recyclerStores);
        recyclerStores.setLayoutManager(new LinearLayoutManager(this));

        selectedCategory = getIntent().getStringExtra("CATEGORY_SELECTED");

        btnRegisterOrder.setOnClickListener(v -> fetchRegisterOrder());

        // Return Home Button
        btnBackHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(OrderActivity.this, HomeActivity.class));
            }
        });

        btnAddProduct.setOnClickListener(v -> {
            String productName = edtProduct1.getText().toString().trim();
            String quantityStr = edtQuantityProduct1.getText().toString().trim();

            if (productName.isEmpty() || quantityStr.isEmpty()) {
                Toast.makeText(this, "Συμπλήρωσε όνομα προϊόντος και ποσότητα", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int quantity = Integer.parseInt(quantityStr);
                if (quantity <= 0) throw new NumberFormatException();

                // Inform map
                if (productsOrdered.containsKey(productName)) {
                    productsOrdered.put(productName, productsOrdered.get(productName) + quantity);
                } else {
                    productsOrdered.put(productName, quantity);
                }

                // refresh UI
                updateProductPreview();

                // clear fields
                edtProduct1.setText("");
                edtQuantityProduct1.setText("");

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Μη έγκυρη ποσότητα", Toast.LENGTH_SHORT).show();
            }
        });


    }
    // Void for creation Order
    private void fetchRegisterOrder() {
        new Thread(() -> {
            try {
                Log.d("Order", "Ξεκινά σύνδεση με τον server...");
                Socket socket = new Socket(SERVER_ADDRESS, PORT);
                Log.d("Order", "Συνδέθηκε με τον server");

                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                // Values from EditTexts
                String storeName = edtStoreName.getText().toString().trim();
                double cost = Double.parseDouble(edtCost.getText().toString().trim());

                if (storeName.isEmpty() || productsOrdered.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "Συμπλήρωσε όλα τα πεδία", Toast.LENGTH_SHORT).show());
                    return;
                }
                if (cost <= 0) {
                    runOnUiThread(() -> Toast.makeText(this, "Το κόστος πρέπει να είναι θετικά", Toast.LENGTH_SHORT).show());
                    return;
                }

                // creation Order
                Order order = new Order(storeName, productsOrdered, cost);

                // Send request Register Order
                Request request = new Request("ADD_ORDER", order);
                out.writeObject(request);
                out.flush();

                // response backend
                Response response = (Response) in.readObject();
                String message = response.getMessage();

                runOnUiThread(() ->
                        Toast.makeText(this, "Απάντηση: " + message, Toast.LENGTH_LONG).show()
                );

                in.close();
                out.close();
                socket.close();

            } catch (Exception e) {
                Log.e("Order", "Σφάλμα: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Σφάλμα σύνδεσης ή παραγγελίας", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    // Update the bag with the new amount of products
    private void updateProductPreview() {
        StringBuilder preview = new StringBuilder(" Προϊόντα:\n");
        for (Map.Entry<String, Integer> entry : productsOrdered.entrySet()) {
            preview.append("• ").append(entry.getKey()).append(" x").append(entry.getValue()).append("\n");
        }
        txtAddedProducts.setText(preview.toString());
    }

    private void fetchAvailableStores() {
        new Thread(() -> {
            try {
                Socket socket = new Socket(SERVER_ADDRESS, PORT);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                Request request = new Request("SEARCH_ALL_STORES", null);
                out.writeObject(request);
                out.flush();

                Response response = (Response) in.readObject();
                List<Store> allStores = (List<Store>) response.getData();

                // Φιλτράρισμα με βάση category
                List<Store> filteredStores = new ArrayList<>();
                if (selectedCategory != null && !selectedCategory.isEmpty()) {
                    for (Store store : allStores) {
                        if (store.getFoodCategory() != null &&
                                store.getFoodCategory().equalsIgnoreCase(selectedCategory)) {
                            filteredStores.add(store);
                        }
                    }
                } else {
                    filteredStores = allStores;
                }


                List<Store> finalStores = filteredStores;

                runOnUiThread(() -> {
                    if (finalStores == null || finalStores.isEmpty()) {
                        Toast.makeText(this, "Δεν βρέθηκαν καταστήματα", Toast.LENGTH_SHORT).show();
                    } else {
                        storeAdapter = new StoreAdapter(finalStores);
                        recyclerStores.setAdapter(storeAdapter);
                    }
                });

                in.close();
                out.close();
                socket.close();

            } catch (Exception e) {
                Log.e("StoreFetch", "Σφάλμα: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Αδυναμία φόρτωσης καταστημάτων", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}