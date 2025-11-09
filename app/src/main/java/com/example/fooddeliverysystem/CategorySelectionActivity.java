package com.example.fooddeliverysystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CategorySelectionActivity extends AppCompatActivity {

    ImageButton btnPizzeria, btnCrepa, btnSouvlaki, btnCoffee, btnBackHome;
    TextView txtQuestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_selection);

        txtQuestion = findViewById(R.id.txtQuestion);
        btnPizzeria = findViewById(R.id.btnPizzeria);
        btnCrepa = findViewById(R.id.btnCrepa);
        btnSouvlaki = findViewById(R.id.btnSouvlaki);
        btnCoffee = findViewById(R.id.btnCoffee);
        btnBackHome = findViewById(R.id.btnBackHome);

        View.OnClickListener categoryClickListener = view -> {
            String category = "";

            if (view == btnPizzeria) category = "pizzeria";
            else if (view == btnCrepa) category = "crepa";
            else if (view == btnSouvlaki) category = "souvlaki";
            else if (view == btnCoffee) category = "coffee";

            Intent intent = new Intent(CategorySelectionActivity.this, OrderActivity.class);
            intent.putExtra("CATEGORY_SELECTED", category);
            startActivity(intent);
        };

        // Return Home Button
        btnBackHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(CategorySelectionActivity.this, HomeActivity.class));
            }
        });

        btnPizzeria.setOnClickListener(categoryClickListener);
        btnCrepa.setOnClickListener(categoryClickListener);
        btnSouvlaki.setOnClickListener(categoryClickListener);
        btnCoffee.setOnClickListener(categoryClickListener);
    }
}
