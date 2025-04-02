package common.model;

import java.io.Serializable;

public class Request implements Serializable {
    private String type;    // Π.χ. "ADD_STORE", "SEARCH", "BUY"
    private Object payload; // Τα δεδομένα που χρειάζονται για το request (Store, Order, SearchFilters, κλπ.)

    public Request(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Request{" +
                "type='" + type + '\'' +
                ", payload=" + payload +
                '}';
    }
}

