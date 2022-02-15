import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private List<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean finished;
    private ExecutorService pool;


    public Server() {
        connections = new ArrayList<>();
        finished = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(8080);
            pool = Executors.newCachedThreadPool();
            while (!finished) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }

        } catch (IOException e) {
            shutdown();
        }
    }

    public void shutdown() {
        try {
            finished = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;



        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Seja Bem Vindo ao Chat");
                out.println();
                out.println("Insira seu nome de usuario: ");
                nickname = in.readLine();
                System.out.println(nickname + " conectado!");
                broadcast(nickname + " entrou no chat");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/quit")) {
                        broadcast(nickname + " saiu do chat!");
                        System.out.println("Usuario " + nickname + " desconectado");
                        shutdown();
                    } else {
                        broadcast("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "] " + nickname + ": " + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void broadcast(String message) {
            for (ConnectionHandler ch : connections) {
                if (ch != null && !ch.nickname.equals(this.nickname)) {
                    ch.sendMessage(message);
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
