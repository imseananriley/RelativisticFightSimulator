import java.util.*;

public class Game {
    public static double currentTime = 0.0;
    public static final double SPEED_OF_LIGHT = 5.0;

    private List<Player> players = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<Light> lightEvents = new ArrayList<>();

    // Each player's outgoing messages
    private Map<Integer, List<String>> pendingMessages = new HashMap<>();

    public synchronized void addPlayer(Player p) {
        players.add(p);
        pendingMessages.put(p.getId(), new ArrayList<>());
    }

    public synchronized List<Player> getPlayers() {
        return players;
    }

    public synchronized List<Bullet> getBullets() {
        return bullets;
    }

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

            // check collision
            for (Player p : players) {
                if (p.isAlive() && b.canCollideWith(p.getX(), p.getY())) {
                    // bullet kills the player
                    p.kill();
                    System.out.println("DEBUG: Player " + p.getId() + " was hit!");

                    // 1) Immediately tell the victim they died
                    enqueueImmediateMessage(p.getId(), "YOU_DEAD");

                    // 2) Create an explosion Light event so OTHERS see it w/ time delay
                    addLightEvent(new Light("EXPLOSION", p.getId(), p.getX(), p.getY(), currentTime));

                    it.remove();
                    break;
                }
            }
        }
    }

    /**
     * Player movement, plus a Light event so others see after delay.
     */
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
            // record a Light event
            addLightEvent(new Light("P", p.getId(), p.getX(), p.getY(), currentTime));
        }
    }

    /**
     * Shooting spawns a bullet, plus a muzzle-flash Light event.
     */
    public synchronized void playerShoot(int playerId, double tx, double ty) {
        for (Player p : players) {
            if (p.getId() == playerId && p.isAlive()) {
                if (p.getShotsRemaining() > 0) {
                    p.decrementShots();
                    Bullet b = new Bullet(p.getX(), p.getY(), tx, ty, currentTime);
                    bullets.add(b);

                    // muzzle flash for others
                    addLightEvent(new Light("SHOT", p.getId(), p.getX(), p.getY(), currentTime));
                } else {
                    System.out.println("DEBUG: Player " + playerId + " tried to shoot but has no ammo left!");
                }
                break;
            }
        }
    }

    /**
     * The crucial method that checks if each Light event is visible to each player.
     * If so, queue a "LIGHT ..." message.
     * BUT if the event is from the same player, they see it immediately (no delay).
     */
    public synchronized void processLightVisibility() {
        Iterator<Light> it = lightEvents.iterator();
        while (it.hasNext()) {
            Light evt = it.next();
            List<Player> notYetSeen = new ArrayList<>();

            for (Player p : players) {
                if (!p.isAlive()) continue; // dead players see nothing

                // If this event belongs to p, p sees it immediately (self-visibility).
                // Example: "P" event or "SHOT" event from player p
                // Or you might selectively skip "EXPLOSION" self-visibility if you prefer.
                if (evt.id == p.getId()) {
                    enqueueLightMessage(p.getId(), evt);
                }
                else {
                    // Speed-of-light check
                    double dist = distance(evt.x, evt.y, p.getX(), p.getY());
                    double travelTime = currentTime - evt.timestamp;
                    if (SPEED_OF_LIGHT * travelTime >= dist) {
                        enqueueLightMessage(p.getId(), evt);
                    } else {
                        notYetSeen.add(p);
                    }
                }
            }

            // If all alive players have seen it, remove from list
            if (notYetSeen.isEmpty()) {
                it.remove();
            }
        }
    }

    /**
     * Each frame, the server calls this on the ClientHandler to get messages for a specific player.
     */
    public synchronized List<String> consumePendingMessages(int playerId) {
        List<String> msgs = pendingMessages.get(playerId);
        if (msgs == null) return new ArrayList<>();
        List<String> copy = new ArrayList<>(msgs);
        msgs.clear();
        return copy;
    }

    // Helpers:

    private void enqueueLightMessage(int playerId, Light evt) {
        List<String> queue = pendingMessages.get(playerId);
        if (queue == null) return;

        // "LIGHT <type> <id> <x> <y>"
        // e.g. "LIGHT P 0 405.2 295.1"
        String line = "LIGHT " + evt.type + " " + evt.id + " " + evt.x + " " + evt.y;
        queue.add(line);
    }

    // If we want to send an immediate out-of-band message (e.g. "YOU_DEAD") to the victim
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
