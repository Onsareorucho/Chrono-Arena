public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java Main [server|client]");
            return;
        }
        switch (args[0]) {
            case "server" -> System.out.println("Server starting...");
            case "client" -> System.out.println("Client starting...");
            default -> System.out.println("Unknown mode: " + args[0]);
        }
    }
}