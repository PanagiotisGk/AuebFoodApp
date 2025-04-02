package master;

import common.model.Request;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class WorkerConnection {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public WorkerConnection(Socket socket, ObjectOutputStream out, ObjectInputStream in) {
        this.socket = socket;
        this.out = out;
        this.in = in;
    }

    public void sendRequest(Request request) throws IOException {
        System.out.println("📤 Στέλνω στον Worker: " + request.getType());
        out.writeObject(request);
        out.flush();
    }

    public ObjectInputStream getInputStream() {
        return in;
    }

    public ObjectOutputStream getOutputStream() {
        return out;
    }

    public Socket getSocket() {
        return socket;
    }
}
