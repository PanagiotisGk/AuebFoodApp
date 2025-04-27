package common.model;

import java.io.Serializable;

public class RateStoreRequest implements Serializable {

    // Xreiazetai na yparxei to idio serialVersionUID sto UI kai sto backend
    private static final long serialVersionUID = 1L;

    private String storeName;
    private int rating; // Asteria axiologisis

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