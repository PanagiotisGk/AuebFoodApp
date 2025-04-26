package common.model;

import java.io.Serializable;
import java.util.List;

public class Store implements Serializable {

    // Χρειάζεται να υπάρχει το ίδιο serialVersionUID στο UI και στο backend
    private static final long serialVersionUID = 1L;

    private String storeName;
    private double latitude;
    private double longitude;
    private String foodCategory;
    private double stars;
    private int noOfVotes;
    private String storeLogo;
    private List<Product> products;

    // Δεν έρχεται από JSON
    private String priceCategory;
    private double totalRevenue;

    public Store() {
    }

    public Store(String storeName, double latitude, double longitude, String foodCategory,
                 double stars, int noOfVotes, String storeLogo, List<Product> products) {
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

    // Συνάρτηση για την προσθήκη προϊόντων σε ένα καταστημα
    public void addProduct(String name, double price) {
        products.add(new Product(name, "unknown", 1, price));
        calculatePriceCategory();
    }

    public boolean removeProduct(String name) {
        boolean removed = products.removeIf(p -> p.getProductName().equalsIgnoreCase(name));
        if (removed) {
            calculatePriceCategory();
        }
        return removed;
    }

    // Συνάρτηση για τον υπολογισμό της ακρίβειας του καταστήματος
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

    public double getStars() {
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
        calculatePriceCategory(); // Κάθε φορά υπολογίζει με βάση τα τρέχοντα προϊόντα
    return priceCategory;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    // public void addRevenue(double amount) {
    //     this.totalRevenue += amount;
    // }

    public synchronized void addRevenue(double amount) {
        this.totalRevenue += amount;
    }

    public void setStars(double stars) {
        this.stars = stars;
    }
    
    public void setNoOfVotes(int noOfVotes) {
        this.noOfVotes = noOfVotes;
    }

    @Override
    public String toString() {
        return "[" + storeName + "] - " + foodCategory + " - " + stars + "★ (" + priceCategory + ")";
    }
}
