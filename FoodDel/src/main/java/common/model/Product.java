package common.model;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Product implements Serializable {
    
    // Χρειάζεται να υπάρχει το ίδιο serialVersionUID στο UI και στο backend
    private static final long serialVersionUID = 1L;
    
    private String productName;
    @JsonProperty("productType")
    private String productType;
    private int availableAmount;
    private double price;

    //  Default constructor (για Jackson)
    public Product() {
    }

    //  Constructor 
    public Product(String productName, String productType, int availableAmount, double price) {
        this.productName = productName;
        this.productType = productType;
        this.availableAmount = availableAmount;
        this.price = price;
    }

    public Product(String productName, double price) {
        this.productName = productName;
        this.productType = "unknown";
        this.availableAmount = 1;
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

    public void setPrice(double price) {
        this.price = price;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    @Override
    public String toString() {
        return productName + " (" + productType + ") - " + price + "€, Διαθέσιμα: " + availableAmount;
    }
}
