import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientMain extends JFrame {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private int playerId = -1;
    private boolean iAmAlive = true;

    // The local player's own position (no flicker, purely local)
    private double localX = 400, localY = 300;

    // We store ephemeral light events from the server
    // Each frame, we draw them once, then discard them
    private List<LightEvent> visibleLightEvents = new ArrayList<>();

    // Movement flags
    private boolean upPressed, downPressed, leftPressed, rightPressed;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientMain("localhost", 12345));
    }

    public ClientMain(String host, int port) {
        setTitle("Relativistic Fight Simulator (Ephemeral Events)");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ClientPanel panel = new ClientPanel();
        add(panel);

        setVisible(true);

        try {
            socket = new Socket(host, port);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // read "ID <playerId>"
            String line = in.readLine();
            if (line != null && line.startsWith("ID ")) {
                playerId = Integer.parseInt(line.substring(3).trim());
                System.out.println("DEBUG: My playerId = " + playerId);
            } else {
                throw new IOException("No valid ID from server. Got: " + line);
            }

            // read server messages in background
            new Thread(this::listenForServerMessages).start();

            // local loop to move & repaint
            Timer t = new Timer(16, e -> {
                sendMovement();
                repaint();
            });
            t.start();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not connect to server");
            System.exit(0);
        }
    }

    private void listenForServerMessages() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("LIGHT ")) {
                    parseLightMessage(line.substring(6));
                } else if (line.equals("YOU_DEAD")) {
                    iAmAlive = false;
                    System.out.println("DEBUG: We died!");
                }
            }
        } catch (IOException e) {
            System.out.println("DEBUG: Server disconnected.");
        } finally {
            try { socket.close(); } catch (Exception ex) {}
        }
    }

    /**
     * We treat all LIGHT events as ephemeral, storing them in visibleLightEvents for
     * one render frame only. Then we clear them.
     */
    private void parseLightMessage(String data) {
        // e.g. "P 1 500.0 300.0"
        //      "BULLET 2 520.0 320.0"
        //      "SHOT 1 500.0 300.0"
        //      "EXPLOSION 1 500.0 300.0"
        String[] parts = data.split(" ");
        String type = parts[0];
        int eid     = Integer.parseInt(parts[1]);
        double ex   = Double.parseDouble(parts[2]);
        double ey   = Double.parseDouble(parts[3]);

        // We store it in a ephemeral list
        visibleLightEvents.add(new LightEvent(type, eid, ex, ey));
    }

    private void sendMovement() {
        if (!iAmAlive) return;

        double speed = 2.0;
        double dx = 0, dy = 0;
        if (upPressed)    dy -= speed;
        if (downPressed)  dy += speed;
        if (leftPressed)  dx -= speed;
        if (rightPressed) dx += speed;

        // Update our local position
        localX += dx;
        localY += dy;

        // Let the server know we moved
        if (dx != 0 || dy != 0) {
            out.println("MOVE " + dx + " " + dy);
        }
    }

    private void sendShoot(double mx, double my) {
        if (!iAmAlive) return;
        out.println("SHOOT " + mx + " " + my);
    }

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

            // Background
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            // Draw the local player's position (green circle, no flicker)
            if (iAmAlive) {
                g.setColor(Color.GREEN);
            } else {
                g.setColor(Color.RED);
            }
            int rx = (int)(localX - 10);
            int ry = (int)(localY - 10);
            g.fillOval(rx, ry, 20, 20);
            g.setColor(Color.WHITE);
            g.drawString("Me (P" + playerId + ")", rx, ry - 2);
            if (!iAmAlive) {
                g.drawString("(DEAD)", rx, ry - 14);
            }

            // Now draw ephemeral Light events:
            // For example, "P <id>" => draw a BLUE circle for that player once
            // "BULLET <bid>" => small YELLOW circle
            // "EXPLOSION" => ORANGE burst
            // "SHOT" => muzzle flash
            for (LightEvent evt : visibleLightEvents) {
                switch (evt.type) {
                    case "P":
                        // Show a player in BLUE, ignoring if it's our ID
                        if (evt.id == playerId) {
                            // We skip, because we handle ourselves locally
                            break;
                        }
                        g.setColor(Color.BLUE);
                        g.fillOval((int)(evt.x - 10), (int)(evt.y - 10), 20, 20);
                        g.setColor(Color.WHITE);
                        g.drawString("P" + evt.id, (int)(evt.x - 10), (int)(evt.y - 12));
                        break;

                    case "BULLET":
                        g.setColor(Color.YELLOW);
                        g.fillOval((int)(evt.x - 3), (int)(evt.y - 3), 6, 6);
                        break;

                    case "SHOT":
                        // muzzle flash
                        g.setColor(Color.MAGENTA);
                        g.fillOval((int)(evt.x - 5), (int)(evt.y - 5), 10, 10);
                        break;

                    case "EXPLOSION":
                        g.setColor(Color.ORANGE);
                        g.fillOval((int)(evt.x - 15), (int)(evt.y - 15), 30, 30);
                        break;
                }
            }

            // If I'm dead, show a label
            if (!iAmAlive) {
                g.setColor(Color.ORANGE);
                g.drawString("YOU ARE DEAD", getWidth()/2 - 40, getHeight()/2);
            }

            // CLEAR ephemeral events now that we've drawn them
            visibleLightEvents.clear();
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

    // Ephemeral event container for the client
    private static class LightEvent {
        String type;
        int id;
        double x, y;
        public LightEvent(String type, int id, double x, double y) {
            this.type = type;
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }
}
