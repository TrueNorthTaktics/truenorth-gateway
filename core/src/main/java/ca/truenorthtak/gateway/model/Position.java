package ca.truenorthtak.gateway.model;

/**
 * Geographic position held as integers in units of 1e-5 degrees, so every codec
 * round-trips it exactly.
 */
public final class Position {
    public static final double SCALE = 100000.0;

    public final int lat5;
    public final int lon5;

    public Position(int lat5, int lon5) {
        if (lat5 < -9000000 || lat5 > 9000000) throw new IllegalArgumentException("latitude out of range");
        if (lon5 < -18000000 || lon5 > 18000000) throw new IllegalArgumentException("longitude out of range");
        this.lat5 = lat5;
        this.lon5 = lon5;
    }

    public static Position of(double lat, double lon) {
        return new Position((int) Math.round(lat * SCALE), (int) Math.round(lon * SCALE));
    }

    public double lat() { return lat5 / SCALE; }
    public double lon() { return lon5 / SCALE; }

    @Override public boolean equals(Object o) {
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
        return p.lat5 == lat5 && p.lon5 == lon5;
    }

    @Override public int hashCode() { return 31 * lat5 + lon5; }

    @Override public String toString() {
        return String.format(java.util.Locale.ROOT, "%.5f,%.5f", lat(), lon());
    }
}
