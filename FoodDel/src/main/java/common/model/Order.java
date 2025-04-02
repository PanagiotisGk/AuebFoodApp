package common.model;

import java.io.Serializable;
import java.util.Map;

public class Order implements Serializable {
    private String storeName; // σε ποιο κατάστημα πάει η παραγγελία
    private Map<String, Integer> productsOrdered; // όνομα προϊόντος -> ποσότητα
    private double totalCost;

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

