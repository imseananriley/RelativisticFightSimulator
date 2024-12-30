public class Light {
    public double x;
    public double y;
    public double timestamp;
    public String type; // e.g. "P", "BULLET", "SHOT", "EXPLOSION"
    public int id;      // playerId or bulletId

    public Light(String type, int id, double x, double y, double timestamp) {
        this.type = type;
        this.id = id;
        this.x = x;
        this.y = y;
        this.timestamp = timestamp;
    }
}
