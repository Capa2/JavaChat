import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

import static java.lang.System.currentTimeMillis;

public class Server implements Runnable, Closeable {
    private volatile ServerSocket serverSocket;
    final private  LinkedList<Thread> sThreads;
    final private LinkedList<Session> sessions;
    final private int port;
    final private int secondsWaitingForClients;

    public Server(int port) {
        this.port = port;
        secondsWaitingForClients = 10;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sessions = new LinkedList<>();
        sThreads = new LinkedList<>();
    }

    public void run() {
        long idleSince = 0;
        clientListener();
        while (!serverSocket.isClosed()) {
            synchronized (sessions) {
                for (Session s : sessions) {
                    while (s.countStack() > 0) {
                        String line = s.popStack();
                        for (Session ss : sessions) {
                            ss.push(line);
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
                    e.printStackTrace();
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