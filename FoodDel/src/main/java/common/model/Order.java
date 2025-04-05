package common.model;

import java.io.Serializable;
import java.util.Map;

public class Order implements Serializable {
    private String storeName;
    private Map<String, Integer> productsOrdered;
    private double totalCost;

    // 🟢 Default constructor (για Jackson)
    public Order() {
    }

    public Order(String storeName, Map<String, Integer> productsOrdered, double totalCost) {
        this.storeName = storeName;
        this.productsOrdered = productsOrdered;
        this.totalCost = totalCost;
    }

    public String getStoreName() {
        return storeName;
    }

    public Map<String, Integer> getProductsOrdered() {
        return productsOrdered;
    }

    public double getTotalCost() {
        return totalCost;
    }

    @Override
    public String toString() {
        return "Παραγγελία προς: " + storeName + ", Προϊόντα: " + productsOrdered + ", Κόστος: " + totalCost + "€";
    }
}
