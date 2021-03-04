import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.currentTimeMillis;

public class Server implements Runnable, Closeable {
    private volatile ServerSocket serverSocket;
    final private ExecutorService threadPool;
    final private Vector<Session> sessions;
    final private Users users;
    final private int minutesWaitingForClients;

    public Server(int port) {
        users = new Users(true);
        minutesWaitingForClients = 30;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sessions = new Vector<>();
        threadPool = Executors.newFixedThreadPool(10);
    }

    public void run() {
        long terminatedSince = 0;
        clientListener();
        while (!serverSocket.isClosed()) {
            sessions.forEach((sender) -> {
                while (sender.haveMorePulls()) {
                    String line = sender.poll();
                    if (pullReader(sender, line)) {
                        sessions.forEach((receiver) -> {
                            if (!sender.equals(receiver))
                                receiver.push(sender.getUser().getUsername() + ": " + line);
                        });
                        System.out.println(sender.getUser().getUsername() + ": " + line);
                    }
                }
            });
        }
        if (threadPool.isTerminated()) {
            terminatedSince = (terminatedSince == 0) ? currentTimeMillis() : terminatedSince;
            if (currentTimeMillis() - terminatedSince > minutesWaitingForClients * 60 * 1000) {
                System.out.print("All sessions terminated. ");
                close();
            }
        }
    }


    private synchronized boolean pullReader(Session session, String string) {
        // return true to push to all clients.
        if (string.startsWith("login")) {
            String[] loginRequest = string.split(" ");
            if (loginRequest.length == 3) {
                if (session.getUser() == null) {
                    if (users.validateUser(loginRequest[1], loginRequest[2])) {
                        session.setUser(users.getValidUser(string.split(" ")[1], string.split(" ")[2]));
                        session.push("Success: logged in as " + session.getUser().getUsername());
                    } else {
                        session.push("Error: username/password not recognized.");
                        return false;
                    }
                } else {
                    session.push("Error: already logged in as" + session.getUser().getUsername());
                    return false;
                }
            }
        }
        if (session.getUser() == null) {
            session.push("Error: Please login with \"login <username> <password>\"");
            return false;
        }
        return true;
    }

    private void clientListener() {
        Runnable getClient = () -> {
            while (!serverSocket.isClosed()) {
                try {
                    System.out.println("Waiting for client... ");
                    Socket socket = serverSocket.accept();
                    System.out.println("Connection established.");
                    Session newcomer = new Session(socket);
                    sessions.add(newcomer);
                    threadPool.execute(newcomer);
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) e.printStackTrace();
                }
            }
        };
        Thread clientListener = new Thread(getClient);
        clientListener.start();
    }

    public void close() {
        System.out.println("Exiting...");
        try {
            threadPool.shutdown();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}