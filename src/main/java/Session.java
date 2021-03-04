import java.io.*;
import java.net.Socket;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Session implements Runnable, Closeable {
    final private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    final private BlockingQueue<String> pulls;
    private User user;

    public Session(Socket socket) {
        this.socket = socket;
        pulls = new ArrayBlockingQueue<String>(100);
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
    public boolean haveMorePulls() {
        return !pulls.isEmpty();
    }
    public String poll() {
        return pulls.poll();
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
