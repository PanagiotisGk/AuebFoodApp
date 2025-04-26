package master;

import common.model.Request;
import common.model.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class WorkerConnection {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final String workerId;

    public WorkerConnection(Socket socket, ObjectOutputStream out, ObjectInputStream in,String workerId) {
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.workerId = workerId;
    }

    public void sendRequest(Request request) throws IOException {
        System.out.println("Στέλνω στον Worker: " + request.getType());
        out.writeObject(request);
        out.flush();
    }

    public String getWorkerId() {
        return workerId;
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

    public Response readResponse() throws IOException, ClassNotFoundException {
        Object response = in.readObject();
        if (response instanceof Response) {
            return (Response) response;
        } else {
            throw new IOException("Δεν λήφθηκε απάντηση από τον Worker.");
        }
    }
}
