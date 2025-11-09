package common.model;

import java.io.Serializable;

public class RateStoreRequest implements Serializable {

    // Χρειάζεται να υπάρχει το ίδιο serialVersionUID στο UI και στο backend
    private static final long serialVersionUID = 1L;

    private String storeName;
    private int rating; // Αστέρια Αξιολόγησης

    public RateStoreRequest(String storeName, int rating) {
        this.storeName = storeName;
        this.rating = rating;
    }

    public String getStoreName() {
        return storeName;
    }

    public int getRating() {
        return rating;
    }
}