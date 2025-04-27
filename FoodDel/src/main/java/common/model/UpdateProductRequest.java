package common.model;

import java.io.Serializable;

public class UpdateProductRequest implements Serializable {

        // Xreiazetai na yparxei to idio serialVersionUID sto UI kai sto backend
    private static final long serialVersionUID = 1L;

    private String storeName;
    private String productName;
    private String productType;
    private int availableAmount;
    private double price;
    private String action; // "ADD" ή "REMOVE"

    public UpdateProductRequest() {
    }

    public UpdateProductRequest(String storeName, String productName, String productType, int availableAmount, double price, String action) {
        this.storeName = storeName;
        this.productName = productName;
        this.productType = productType;
        this.availableAmount = availableAmount;
        this.price = price;
        this.action = action;
    }

    public String getStoreName() {
        return storeName;
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

    public String getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "Εντολή: " + action + " προϊόν " + productName + " στο κατάστημα " + storeName;
    }
}
