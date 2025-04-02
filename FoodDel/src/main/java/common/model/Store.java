package common.model;

import java.io.Serializable;
import java.util.List;

public class Store implements Serializable {
    private String storeName;
    private double latitude;
    private double longitude;
    private String foodCategory;
    private int stars;
    private int noOfVotes;
    private String storeLogo;
    private List<Product> products;

    // Δεν έρχεται από JSON
    private String priceCategory;
    private double totalRevenue;

    // ✅ Default constructor (ΑΠΑΡΑΙΤΗΤΟΣ για Jackson)
    public Store() {
    }

    // ✅ Constructor με όλα τα πεδία
    public Store(String storeName, double latitude, double longitude, String foodCategory,
                 int stars, int noOfVotes, String storeLogo, List<Product> products) {
        this.storeName = storeName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.foodCategory = foodCategory;
        this.stars = stars;
        this.noOfVotes = noOfVotes;
        this.storeLogo = storeLogo;
        this.products = products;
        this.totalRevenue = 0;
        calculatePriceCategory();
    }

    // Υπολογισμός κατηγορίας τιμής
    private void calculatePriceCategory() {
        if (products == null || products.isEmpty()) {
            priceCategory = "$";
            return;
        }

        double avg = products.stream().mapToDouble(Product::getPrice).average().orElse(0);
        if (avg <= 5) {
            priceCategory = "$";
        } else if (avg <= 15) {
            priceCategory = "$$";
        } else {
            priceCategory = "$$$";
        }
    }

    public String getStoreName() {
        return storeName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public int getStars() {
        return stars;
    }

    public int getNoOfVotes() {
        return noOfVotes;
    }

    public String getStoreLogo() {
        return storeLogo;
    }

    public List<Product> getProducts() {
        return products;
    }

    public String getPriceCategory() {
        return priceCategory;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void addRevenue(double amount) {
        this.totalRevenue += amount;
    }

    @Override
    public String toString() {
        return "[" + storeName + "] - " + foodCategory + " - " + stars + "★ (" + priceCategory + ")";
    }
}
