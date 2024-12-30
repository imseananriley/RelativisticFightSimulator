import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler extends Thread {
    private Socket socket;
    private Player player;
    private Game game;
    private ServerMain server;

    private PrintWriter out;
    private BufferedReader in;
    private boolean running = true;

    public ClientHandler(Socket socket, Player player, Game game, ServerMain server) {
        this.socket = socket;
        this.player = player;
        this.game = game;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("ID " + player.getId());
            out.flush();

            System.out.println("DEBUG: Sent ID " + player.getId() + " to client.");

            String line;
            while (running && (line = in.readLine()) != null) {
                handleCommand(line);
            }
        } catch (IOException e) {
            System.out.println("DEBUG: Client disconnected: " + socket);
        } finally {
            closeConnections();
        }
    }

    private void handleCommand(String line) {
        System.out.println("DEBUG: From client " + player.getId() + ": " + line);
        try {
            String[] parts = line.split(" ");
            if ("MOVE".equals(parts[0])) {
                double dx = Double.parseDouble(parts[1]);
                double dy = Double.parseDouble(parts[2]);
                game.movePlayer(player.getId(), dx, dy);
            } else if ("SHOOT".equals(parts[0])) {
                double tx = Double.parseDouble(parts[1]);
                double ty = Double.parseDouble(parts[2]);
                game.playerShoot(player.getId(), tx, ty);
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Bad command: " + line + " => " + e);
        }
    }

    public void sendUpdates() {
        // Grab messages for this player
        List<String> lines = game.consumePendingMessages(player.getId());
        for (String msg : lines) {
            // Could be "LIGHT ..." or "YOU_DEAD"
            out.println(msg);
        }
        out.flush();
    }

    private void closeConnections() {
        running = false;
        server.removeClient(this);
        try {
            if (in  != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
