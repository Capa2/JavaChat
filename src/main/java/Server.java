import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

import static java.lang.System.currentTimeMillis;

public class Server implements Runnable, Closeable {
    private volatile ServerSocket serverSocket;
    final private LinkedList<Thread> sThreads;
    final private LinkedList<Session> sessions;
    final private Users users;
    final private int secondsWaitingForClients;

    public Server(int port) {
        users = new Users(true);
        secondsWaitingForClients = 10;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sessions = new LinkedList<Session>();
        sThreads = new LinkedList<Thread>();
    }

    public void run() {
        long idleSince = 0;
        clientListener();
        while (!serverSocket.isClosed()) {
            synchronized (sessions) {
                for (Session sender : sessions) {
                    while (sender.countStack() > 0) {
                        String line = sender.popStack();
                        if (pullReader(sender, line)) {
                            for (Session reciever : sessions) {
                                reciever.push(sender.getUser().getUsername() + ": " + line);
                            }
                        }
                    }
                }
            }
            synchronized (sThreads) {
                for (Thread t : sThreads) {
                    if (t.getState() == Thread.State.TERMINATED) {
                        System.out.println("T#" + t.getId() + " terminated.");
                        sThreads.remove(t);
                    }
                }
                if (sThreads.size() == 0) {
                    if (idleSince == 0) idleSince = currentTimeMillis();
                    if (currentTimeMillis() - idleSince > secondsWaitingForClients * 1000) {
                        System.out.print("All sessions terminated. ");
                        close();
                    }
                } else idleSince = 0;
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
                    } else session.push("Error: username/password not recognized.");
                } else session.push("Error: already logged in as" + session.getUser().getUsername());
                return false;
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
                    synchronized (sessions) {
                        sessions.add(new Session(socket));

                        synchronized (sThreads) {
                            sThreads.add(new Thread(sessions.getLast()));
                            sThreads.getLast().start();
                            System.out.println("Client online on T#" + sThreads.getLast().getId());
                        }
                    }
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
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}