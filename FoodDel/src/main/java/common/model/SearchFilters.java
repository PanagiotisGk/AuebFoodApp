package common.model;

import java.io.Serializable;
import java.util.List;

public class SearchFilters implements Serializable {

    // Xreiazetai na yparxei to idio serialVersionUID sto UI kai sto backend
    private static final long serialVersionUID = 1L;

    private double clientLatitude;
    private double clientLongitude;
    private List<String> foodCategories; // ["pizzeria", "souvlaki"]
    private int minStars; // 3 , 4 κτλ
    private List<String> priceCategories; // ["$", "$$"]

    public SearchFilters(double clientLatitude, double clientLongitude,
                         List<String> foodCategories, int minStars, List<String> priceCategories) {
        this.clientLatitude = clientLatitude;
        this.clientLongitude = clientLongitude;
        this.foodCategories = foodCategories;
        this.minStars = minStars;
        this.priceCategories = priceCategories;
    }

    public double getClientLatitude() {
        return clientLatitude;
    }

    public double getClientLongitude() {
        return clientLongitude;
    }

    public List<String> getFoodCategories() {
        return foodCategories;
    }

    public int getMinStars() {
        return minStars;
    }

    public List<String> getPriceCategories() {
        return priceCategories;
    }

    @Override
    public String toString() {
        return "SearchFilters{" +
                "latitude=" + clientLatitude +
                ", longitude=" + clientLongitude +
                ", foodCategories=" + foodCategories +
                ", minStars=" + minStars +
                ", priceCategories=" + priceCategories +
                '}';
    }
}

