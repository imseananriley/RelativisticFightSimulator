public class Bullet {
    private double startX, startY;
    private double dirX, dirY;
    private double startTime;
    private boolean active = true;

    private double currentX, currentY;
    private static final double MAX_DISTANCE = 2000.0;

    // NEW: Minimum distance the bullet must travel from origin before collision checking
    private static final double SAFE_DISTANCE = 20.0;
    // Alternatively, you could store which player fired it, and skip collision with them
    // for e.g. half a second.

    public Bullet(double sx, double sy, double tx, double ty, double startTime) {
        this.startX = sx;
        this.startY = sy;
        this.startTime = startTime;

        double dx = tx - sx;
        double dy = ty - sy;
        double length = Math.sqrt(dx*dx + dy*dy);
        if (length == 0) length = 1;

        this.dirX = dx / length;
        this.dirY = dy / length;

        this.currentX = sx;
        this.currentY = sy;
    }

    public void updatePosition(double gameTime, double speedOfLight) {
        double timeElapsed = gameTime - startTime;
        double dist = speedOfLight * timeElapsed;

        currentX = startX + dirX * dist;
        currentY = startY + dirY * dist;

        double traveled = distance(startX, startY, currentX, currentY);
        if (traveled > MAX_DISTANCE) {
            active = false;
        }
    }

    public boolean isActive() { return active; }
    public double getX() { return currentX; }
    public double getY() { return currentY; }

    /**
     * Check if bullet can collide with a given position, ignoring collisions
     * within SAFE_DISTANCE from origin to avoid self-hits at spawn time.
     */
    public boolean canCollideWith(double px, double py) {
        double traveled = distance(startX, startY, currentX, currentY);
        if (traveled < SAFE_DISTANCE) {
            return false; // bullet hasn't gone far enough to collide with anything
        }
        // You can also skip collision if bullet belongs to a certain player
        // and px,py is that player's position.

        double dist = distance(currentX, currentY, px, py);
        return (dist < 15.0); // collision threshold
    }

    private double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx*dx + dy*dy);
    }
}
