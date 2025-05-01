package com.example.fooddeliverysystem;

import common.model.*;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvaluationStoreActivity extends AppCompatActivity {

    private EditText edtStoreName, edtStars;
    private Button btnRegisterEval;
    private ImageButton btnBackHome;
    private RecyclerView recyclerStores;

    private StoreAdapter storeAdapter;
    private static final String SERVER_ADDRESS = "192.168.1.46"; // Ip masterServer
    private static final int PORT = 5000; // Port masterServer

    @Override
    protected void onStart() {
        super.onStart();
        fetchAvailableStores();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evaluationstore);

        edtStoreName = findViewById(R.id.edtStoreName);
        edtStars = findViewById(R.id.edtStars);
        btnRegisterEval = findViewById(R.id.btnRegisterEval);
        btnBackHome = findViewById(R.id.btnBackHome);

        recyclerStores = findViewById(R.id.recyclerStores);
        recyclerStores.setLayoutManager(new LinearLayoutManager(this));

        btnRegisterEval.setOnClickListener(v -> fetchRegisterEvaluation());

        // Return Home Button
        btnBackHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(EvaluationStoreActivity.this, HomeActivity.class));
            }
        });
    }
        private void fetchRegisterEvaluation() {
            new Thread(() -> {
                try {
                    Log.d("Order", "Ξεκινά σύνδεση με τον server...");
                    Socket socket = new Socket(SERVER_ADDRESS, PORT);
                    Log.d("Order", "Συνδέθηκε με τον server");

                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                    // Values from EditTexts
                    String storeName = edtStoreName.getText().toString().trim();
                    int stars = Integer.parseInt(edtStars.getText().toString().trim());

                    // Check that all fields are full
                    if (storeName.isEmpty() || stars == 0) {
                        runOnUiThread(() -> Toast.makeText(this, "Συμπλήρωσε όλα τα πεδία", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    // Check that given stars are in the allowed range
                    if (stars < 1 || stars > 5) {
                        runOnUiThread(() -> Toast.makeText(this, "Η αξιολόγηση θα πρέπει να είναι από 1 έως 5 αστέρια", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    // Register evaluation
                    RateStoreRequest rateStore = new RateStoreRequest(storeName, stars);

                    // Send request for register of the evaluation
                    Request request = new Request("RATE_STORE", rateStore);
                    out.writeObject(request);
                    out.flush();

                    // backend response
                    Response response = (Response) in.readObject();
                    String message = response.getMessage();

                    runOnUiThread(() ->
                            Toast.makeText(this, "Απάντηση: " + message, Toast.LENGTH_LONG).show()
                    );

                    // Refresh data of the stores after the evaluation
                    fetchAvailableStores();

                    // Clear Fields
                    edtStoreName.setText("");
                    edtStars.setText("");

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

    private void fetchAvailableStores() {
        new Thread(() -> {
            try {
                Socket socket = new Socket(SERVER_ADDRESS, PORT);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                Request request = new Request("SEARCH_ALL_STORES", null); // ή αντίστοιχο command
                out.writeObject(request);
                out.flush();

                Response response = (Response) in.readObject();
                List<Store> stores = (List<Store>) response.getData();

                runOnUiThread(() -> {
                    if (stores == null || stores.isEmpty()) {
                        Toast.makeText(this, "Δεν βρέθηκαν καταστήματα", Toast.LENGTH_SHORT).show();
                    } else {
                        storeAdapter = new StoreAdapter(stores);
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
