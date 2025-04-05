package common.model;

import java.io.Serializable;

public class Product implements Serializable {
    private String productName;
    private String productType;
    private int availableAmount;
    private double price;

    // ✅ Default constructor (για Jackson)
    public Product() {
    }

    // ✅ Full constructor (για σένα)
    public Product(String productName, String productType, int availableAmount, double price) {
        this.productName = productName;
        this.productType = productType;
        this.availableAmount = availableAmount;
        this.price = price;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductType() {
        return productType;
    }

    public int getAvailableAmount() {
        return availableAmount;
    }

    public double getPrice() {
        return price;
    }

    public void setAvailableAmount(int availableAmount) {
        this.availableAmount = availableAmount;
    }

    @Override
    public String toString() {
        return productName + " (" + productType + ") - " + price + "€, Διαθέσιμα: " + availableAmount;
    }
}
