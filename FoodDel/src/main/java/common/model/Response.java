package common.model;

import java.io.Serializable;

public class Response implements Serializable {
    private boolean success;
    private String message;
    private Object data; // λίστα καταστημάτων, αποτελέσματα αναζήτησης κλπ.

    public Response(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    private Object payload;

    public Object getPayload() {
        return payload;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Response{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}

