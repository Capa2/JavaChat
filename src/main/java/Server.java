import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class Server implements Runnable, Closeable {
    ServerSocket serverSocket;
    LinkedList<Thread> sessions;
    int port;

    public Server(int port) {
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sessions = new LinkedList<Thread>();
    }

    public void run() {
        while (!serverSocket.isClosed()) {
            try {
                System.out.println("Waiting for client... ");
                Socket socket = serverSocket.accept();
                System.out.println("Connection established.");
                sessions.addFirst(new Thread(new Session(socket)));
                sessions.getFirst().start();
                System.out.println("Client online @ T#" + sessions.getFirst().getId());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                for (Thread t : sessions) {
                    if (t.getState() == Thread.State.TERMINATED) {
                        System.out.println("T#" + t.getId() + " terminated.");
                        sessions.remove(t);
                    }
                }
                if (sessions.size() == 0) {
                    System.out.print("All sessions terminated. ");
                    close();
                }
            }
        }
    }

    public void close() {
        System.out.println("Exiting...");
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}