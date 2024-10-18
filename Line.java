package inkball;
import java.util.*;

/**
 * Represents a line composed of multiple points.
 */
public class Line {
    private final ArrayList<int[]> points;

    /**
     * Constructs an empty Line.
     */
    public Line() {
        points = new ArrayList<>();
    }

    /**
     * Adds a point to the line.
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     */
    public void addPoint(int x, int y) {
        points.add(new int[]{x, y});
    }

    /**
     * Retrieves the point at the specified index.
     * @param i the index of the point
     * @return the point at the given index
     */
    public int[] getPoint(int i) {
        return points.get(i);
    }

    /**
     * Returns the number of points in the line.
     * @return the number of points
     */
    public int getSize() {
        return this.points.size();
    }
}
