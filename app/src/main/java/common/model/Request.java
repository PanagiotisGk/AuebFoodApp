package common.model;

import java.io.Serializable;

public class Request implements Serializable {

    //    Χρειάζεται να υπάρχει το ίδιο serialVersionUID στο UI και στο backend για να μην υπάρχουν conflicts
    private static final long serialVersionUID = 1L;
    private String type;    // "ADD_STORE", "SEARCH", "BUY"
    private Object payload; // (Store, Order, SearchFilters, κλπ.)

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