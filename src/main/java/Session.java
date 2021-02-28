import java.io.*;
import java.net.Socket;
import java.util.Stack;

public class Session implements Runnable, Closeable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Stack<String> pulls;

    public Session(Socket socket) {
        this.socket = socket;
        pulls = new Stack<String>();
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
            pulls.add(line);
            push(": " + line);
        }
        try {
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        close();
    }

    public void push(String string) {
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
    public int countStack() {
        return pulls.size();
    }
    public String popStack() {
        return pulls.pop();
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
