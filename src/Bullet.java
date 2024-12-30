public class Bullet {
    private static int NEXT_BULLET_ID = 0;

    private int bulletId;
    private double startX, startY;
    private double dirX, dirY;
    private double startTime;
    private boolean active = true;

    private double currentX, currentY;
    private static final double MAX_DISTANCE = 2000.0;
    private static final double SAFE_DISTANCE = 20.0;

    public Bullet(double sx, double sy, double tx, double ty, double startTime) {
        this.bulletId = NEXT_BULLET_ID++;
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
        double elapsed = gameTime - startTime;
        double dist = speedOfLight * elapsed;

        currentX = startX + dirX * dist;
        currentY = startY + dirY * dist;

        double traveled = distance(startX, startY, currentX, currentY);
        if (traveled > MAX_DISTANCE) {
            active = false;
        }
    }

    public boolean canCollideWith(double px, double py) {
        double traveled = distance(startX, startY, currentX, currentY);
        if (traveled < SAFE_DISTANCE) {
            return false;
        }
        return distance(currentX, currentY, px, py) < 15.0;
    }

    public int    getBulletId() { return bulletId; }
    public double getX()        { return currentX; }
    public double getY()        { return currentY; }
    public boolean isActive()   { return active; }

    private double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx*dx + dy*dy);
    }
}
