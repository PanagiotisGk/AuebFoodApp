package com.example.fooddeliverysystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    Button btnSearchNearby, btnSearchWithFilters, btnOrder, btnEvaluationStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnSearchNearby = findViewById(R.id.btnSearchNearby);
        btnSearchWithFilters = findViewById(R.id.btnSearchWithFilters);
        btnOrder = findViewById(R.id.btnOrder);
        btnEvaluationStore = findViewById(R.id.btnEvaluationStore);


        btnSearchNearby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(HomeActivity.this, NearbyStoresActivity.class));
            }
        });

        btnSearchWithFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(HomeActivity.this, FilterStoresActivity.class));
            }
        });

        btnOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(HomeActivity.this, OrderActivity.class));
            }
        });

        btnEvaluationStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(HomeActivity.this, EvaluationStoreActivity.class));
            }
        });

    }
}