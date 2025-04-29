package com.example.fooddeliverysystem;

import common.model.*;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    private List<Store> stores;
    public StoreAdapter(List<Store> stores) {
        this.stores = stores;
    }
    @Override
    public StoreViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.store_item, parent, false);
        return new StoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(StoreViewHolder holder, int position) {
        Store store = stores.get(position);
        holder.txtName.setText(store.getStoreName());
        holder.txtCategory.setText("Κατηγορία: " + store.getFoodCategory());
        holder.txtPrice.setText("Τιμή: " + store.getPriceCategory());
        holder.txtStars.setText("Βαθμολογία: " + store.getStarsFormatted());
        holder.txtnoOfVotes.setText("Πλήθος Αξιολογήσεων: " + store.getNoOfVotes());

        holder.imgLogo.setImageResource(R.drawable.placeholder_logo);

        // Φόρτωσε αν υπάρχει σωστό logo
        int imageResource = holder.itemView.getContext().getResources().getIdentifier(
                store.getStoreLogo(), "drawable", holder.itemView.getContext().getPackageName());
        if (imageResource != 0) {
            holder.imgLogo.setImageResource(imageResource);
        }

        holder.imgDropdown.setOnClickListener(v -> {
            if (holder.layoutDetails.getVisibility() == View.VISIBLE) {
                holder.layoutDetails.setVisibility(View.GONE);
                holder.imgDropdown.setRotation(0); // poso grigora tha gyrnaei to velaki
            } else {
                holder.layoutDetails.setVisibility(View.VISIBLE);
                holder.imgDropdown.setRotation(180);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }

    public static class StoreViewHolder extends RecyclerView.ViewHolder {
        public ImageView imgLogo, imgDropdown;
        TextView txtName, txtCategory, txtStars, txtPrice, txtnoOfVotes;
        LinearLayout layoutDetails;

        public StoreViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtStoreName);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtStars = itemView.findViewById(R.id.txtStars);
            txtnoOfVotes = itemView.findViewById(R.id.txtnoOfVotes);
            imgLogo = itemView.findViewById(R.id.imgStoreLogo);
            imgDropdown = itemView.findViewById(R.id.imgDropdown);
            layoutDetails = itemView.findViewById(R.id.layoutDetails);
        }
    }
}