public class Main {
    public static void main(String[] args) {
        final int port = (args.length > 1) ? Integer.parseInt(args[1]) : 5000;
        Server server = new Server(port);
        server.run();
    }
}
