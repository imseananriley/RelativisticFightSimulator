import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerMain {
    private static final int PORT = 12345;
    private Game game;
    private CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private boolean running = true;

    public static void main(String[] args) {
        new ServerMain().startServer();
    }

    public ServerMain() {
        game = new Game();
    }

    public void startServer() {
        Thread gameLoop = new Thread(this::gameLoop);
        gameLoop.start();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            int playerCount = 0;
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client: " + clientSocket);

                Player newPlayer = new Player(playerCount++, 400, 300);
                game.addPlayer(newPlayer);

                ClientHandler handler = new ClientHandler(clientSocket, newPlayer, game, this);
                clients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void gameLoop() {
        final int FPS = 60;
        long frameMillis = 1000 / FPS;

        while (running) {
            long start = System.currentTimeMillis();

            game.updateTime();
            game.updateBullets();
            game.processLightVisibility();

            for (ClientHandler ch : clients) {
                ch.sendUpdates();
            }

            long elapsed = System.currentTimeMillis() - start;
            long sleepTime = frameMillis - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }
}
