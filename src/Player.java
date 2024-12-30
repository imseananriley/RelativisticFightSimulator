public class Player {
    private int id;
    private double x, y;
    public double vx, vy;
    private int shotsRemaining = 3;
    private boolean alive = true;


    public Player(int id, double startX, double startY) {
        this.id = id;
        this.x = startX;
        this.y = startY;
    }

    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public int getShotsRemaining() { return shotsRemaining; }
    public boolean isAlive() { return alive; }

    public void accelerate(double dx, double dy) {
        vx += dx;
        vy += dy;
    }

    public void move(double dx, double dy) {
        x += vx;
        y += vy;
    }

    public void decrementShots() {
        if (shotsRemaining > 0) shotsRemaining--;
    }

    public void kill() {
        alive = false;
    }
}
