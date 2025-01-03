import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Game {
    public static double currentTime = 0.0;
    public static final double SPEED_OF_LIGHT = 5.0;

    private List<Player> players = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<Light> lightEvents = new ArrayList<>();

    // Each player's pending messages
    private Map<Integer, List<String>> pendingMessages = new HashMap<>();

    public synchronized void addPlayer(Player p) {
        players.add(p);
        pendingMessages.put(p.getId(), new ArrayList<>());
    }

    public synchronized List<Player> getPlayers() { return players; }
    public synchronized List<Bullet> getBullets() { return bullets; }

    public synchronized void addLightEvent(Light e) {
        lightEvents.add(e);
    }

    public synchronized void updateTime() {
        currentTime += 1.0;
    }

    public synchronized void updateBullets() {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.updatePosition(currentTime, SPEED_OF_LIGHT);

            if (!b.isActive()) {
                it.remove();
                continue;
            }

            // Generate ephemeral bullet events each frame
            Light bulletLight = new Light("BULLET", b.getBulletId(), b.getX(), b.getY(), currentTime);
            addLightEvent(bulletLight);

            // Collision checks
            for (Player p : players) {
                if (p.isAlive() && b.canCollideWith(p.getX(), p.getY())) {
                    p.kill();
                    enqueueImmediateMessage(p.getId(), "YOU_DEAD");

                    // Explosion
                    addLightEvent(new Light("EXPLOSION", p.getId(), p.getX(), p.getY(), currentTime));
                    it.remove();
                    break;
                }
            }
        }
    }

    public synchronized void movePlayer(int playerId, double dx, double dy) {
        for (Player p : players) {
            if (p.getId() == playerId && p.isAlive()) {
                p.accelerate(dx, dy);
                break;
            }
        }
    }

    /**
     * update location for all players
     */
    public synchronized void updateLoc() {
        for (Player p : players) {
            double vx = p.getVx();
            double vy = p.getVy();
            p.move(vx, vy);
            // Generate a "P" event for that new position
            Light evt = new Light("P", p.getId(), p.getX(), p.getY(), currentTime);
            addLightEvent(evt);
        }
    }

    public synchronized void playerShoot(int playerId, double tx, double ty) {
        for (Player p : players) {
            if (p.getId() == playerId && p.isAlive()) {
                if (p.getShotsRemaining() > 0) {
                    p.decrementShots();
                    Bullet b = new Bullet(p.getX(), p.getY(), tx, ty, currentTime);
                    bullets.add(b);

                    Light muzzleFlash = new Light("SHOT", p.getId(), p.getX(), p.getY(), currentTime);
                    addLightEvent(muzzleFlash);
                } else {
                    System.out.println("DEBUG: Player " + playerId + " tried to shoot but has no ammo!");
                }
                break;
            }
        }
    }

    /**
     * The main visibility check.
     * "P" events are not sent back to the same player => no self flicker.
     * Once all alive players have seen the event, we remove it.
     */
    public synchronized void processLightVisibility() {
        Iterator<Light> it = lightEvents.iterator();
        while (it.hasNext()) {
            Light evt = it.next();
            List<Player> notYetSeen = new ArrayList<>();

            for (Player p : players) {
                if (!p.isAlive()) continue;

                // Skip sending "P" events to same player => ignore them
                if (evt.type.equals("P") && evt.id == p.getId()) {
                    // do nothing
                    continue;
                }

                double dist = distance(evt.x, evt.y, p.getX(), p.getY());
                double travelTime = currentTime - evt.timestamp;

                // If wavefront has arrived => enqueue
                if (travelTime * SPEED_OF_LIGHT >= dist) {
                    enqueueLightMessage(p.getId(), evt);
                } else {
                    notYetSeen.add(p);
                }
            }

            // Remove if no one left to see it
            if (notYetSeen.isEmpty()) {
                it.remove();
            }
        }
    }

    public synchronized List<String> consumePendingMessages(int playerId) {
        List<String> msgs = pendingMessages.get(playerId);
        if (msgs == null) return new ArrayList<>();
        List<String> copy = new ArrayList<>(msgs);
        msgs.clear();
        return copy;
    }

    private void enqueueLightMessage(int playerId, Light evt) {
        List<String> queue = pendingMessages.get(playerId);
        if (queue == null) return;

        String line = "LIGHT " + evt.type + " " + evt.id + " " + evt.x + " " + evt.y;
        queue.add(line);
    }

    private void enqueueImmediateMessage(int playerId, String message) {
        List<String> queue = pendingMessages.get(playerId);
        if (queue == null) return;
        queue.add(message);
    }

    private double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx*dx + dy*dy);
    }
}
