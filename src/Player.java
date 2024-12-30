public class Player {
    private int id;
    private double x, y;
    private int shotsRemaining = 30;
    private boolean alive = true;

    public Player(int id, double startX, double startY) {
        this.id = id;
        this.x = startX;
        this.y = startY;
    }

    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getShotsRemaining() { return shotsRemaining; }
    public boolean isAlive() { return alive; }

    public void move(double dx, double dy) {
        x += dx;
        y += dy;
    }

    public void decrementShots() {
        if (shotsRemaining > 0) shotsRemaining--;
    }

    public void kill() {
        alive = false;
    }
}
