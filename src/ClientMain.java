import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientMain extends JFrame {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private int playerId = -1;

    // We'll track the last-known positions of all players:
    private Map<Integer, PlayerInfo> knownPlayers = new HashMap<>();

    // Are we alive?
    private boolean iAmAlive = true;

    // Input flags
    private boolean upPressed, downPressed, leftPressed, rightPressed;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientMain("localhost", 12345));
    }

    public ClientMain(String host, int port) {
        setTitle("Low-Speed-of-Light Client");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ClientPanel panel = new ClientPanel();
        add(panel);
        setVisible(true);

        // Connect to server
        try {
            socket = new Socket(host, port);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // First line: "ID <playerId>"
            String line = in.readLine();
            if (line != null && line.startsWith("ID ")) {
                playerId = Integer.parseInt(line.substring(3).trim());
                System.out.println("DEBUG: Assigned playerId = " + playerId);
                // We'll add ourselves to knownPlayers so we at least see something
                knownPlayers.put(playerId, new PlayerInfo(playerId, 400, 300, true));
            } else {
                throw new IOException("No ID from server. Got: " + line);
            }

            // Listen in background
            new Thread(this::listenForServerMessages).start();

            Timer t = new Timer(16, e -> {
                sendMovement();
                repaint();
            });
            t.start();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to connect to server");
            System.exit(0);
        }
    }

    private void listenForServerMessages() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                // Could be "LIGHT ..." or "YOU_DEAD"
                if (line.startsWith("LIGHT ")) {
                    parseLightMessage(line.substring(6));
                }
                else if (line.equals("YOU_DEAD")) {
                    // This means we died immediately, no speed-of-light delay
                    iAmAlive = false;
                    System.out.println("DEBUG: We are dead. Stopping input.");
                }
            }
        } catch (IOException e) {
            System.out.println("DEBUG: Server disconnected.");
        } finally {
            try { socket.close(); } catch (Exception ex) {}
        }
    }

    /**
     * Format: "type id x y"
     * e.g. "P 0 398.3 302.7"
     *      "SHOT 1 405.0 300.1"
     *      "EXPLOSION 1 405.0 300.1"
     */
    private void parseLightMessage(String data) {
        String[] parts = data.split(" ");
        String type = parts[0];
        int eid     = Integer.parseInt(parts[1]);
        double ex   = Double.parseDouble(parts[2]);
        double ey   = Double.parseDouble(parts[3]);

        switch (type) {
            case "P":
                // We learned that player EID is at (ex, ey) and presumably alive
                knownPlayers.put(eid, new PlayerInfo(eid, ex, ey, true));
                break;

            case "SHOT":
                // Muzzle flash from player EID
                // We might store bullet info, but let's just log
                System.out.println("DEBUG: Saw SHOT from player " + eid + " at (" + ex + ", " + ey + ")");
                // If we have that player in knownPlayers, update position
                knownPlayers.put(eid, new PlayerInfo(eid, ex, ey, true));
                break;

            case "EXPLOSION":
                // Means player EID died at (ex, ey)
                System.out.println("DEBUG: Saw EXPLOSION for player " + eid + " at (" + ex + ", " + ey + ")");
                knownPlayers.put(eid, new PlayerInfo(eid, ex, ey, false));
                break;

            default:
                System.out.println("DEBUG: Unknown LIGHT type: " + type);
        }
    }

    private void sendMovement() {
        if (!iAmAlive) return; // If we're dead, stop sending

        double speed = 2.0;
        double dx = 0, dy = 0;
        if (upPressed)    dy -= speed;
        if (downPressed)  dy += speed;
        if (leftPressed)  dx -= speed;
        if (rightPressed) dx += speed;

        if (dx != 0 || dy != 0) {
            out.println("MOVE " + dx + " " + dy);
        }
    }

    private void sendShoot(double mx, double my) {
        if (!iAmAlive) return; // can't shoot if dead
        out.println("SHOOT " + mx + " " + my);
    }

    /**
     * The panel that draws known players
     */
    private class ClientPanel extends JPanel implements KeyListener, MouseListener {

        public ClientPanel() {
            setFocusable(true);
            requestFocusInWindow();
            addKeyListener(this);
            addMouseListener(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            for (PlayerInfo pi : knownPlayers.values()) {
                if (!pi.alive) {
                    g.setColor(Color.RED); // dead
                } else if (pi.id == playerId) {
                    g.setColor(Color.GREEN); // me
                } else {
                    g.setColor(Color.BLUE); // others
                }
                int rx = (int)(pi.x - 10);
                int ry = (int)(pi.y - 10);
                g.fillOval(rx, ry, 20, 20);

                g.setColor(Color.WHITE);
                g.drawString("P" + pi.id, rx, ry - 2);
                if (!pi.alive) {
                    g.drawString("(DEAD)", rx, ry - 14);
                }
            }

            if (!iAmAlive) {
                // If we want some overlay text
                g.setColor(Color.YELLOW);
                g.drawString("YOU ARE DEAD", 350, 300);
            }
        }

        // KeyListener
        @Override
        public void keyPressed(KeyEvent e) {
            if (!iAmAlive) return;
            int c = e.getKeyCode();
            if (c == KeyEvent.VK_W) upPressed = true;
            if (c == KeyEvent.VK_S) downPressed = true;
            if (c == KeyEvent.VK_A) leftPressed = true;
            if (c == KeyEvent.VK_D) rightPressed = true;
        }
        @Override
        public void keyReleased(KeyEvent e) {
            if (!iAmAlive) return;
            int c = e.getKeyCode();
            if (c == KeyEvent.VK_W) upPressed = false;
            if (c == KeyEvent.VK_S) downPressed = false;
            if (c == KeyEvent.VK_A) leftPressed = false;
            if (c == KeyEvent.VK_D) rightPressed = false;
        }
        @Override public void keyTyped(KeyEvent e) {}

        // MouseListener
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!iAmAlive) return;
            sendShoot(e.getX(), e.getY());
        }
        @Override public void mousePressed(MouseEvent e) {}
        @Override public void mouseReleased(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
    }

    private static class PlayerInfo {
        int id;
        double x, y;
        boolean alive;
        public PlayerInfo(int id, double x, double y, boolean alive) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.alive = alive;
        }
    }
}
