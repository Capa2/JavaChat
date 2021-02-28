import java.io.*;
import java.net.Socket;

public class Session implements Runnable, Closeable {
    Socket socket;
    DataInputStream in;
    DataOutputStream out;

    public Session(Socket socket) {
        this.socket = socket;
        try {
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while(socket.isConnected()) {
            String line = pull();
            if (line.equals("quit")) break;
            System.out.println(line);
            push(": " + line);
        }
        try {
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        close();
    }

    private void push(String string) {
        try {
            out.writeUTF(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String pull() {
        try {
            return in.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
