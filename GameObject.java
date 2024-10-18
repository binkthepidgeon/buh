package inkball;

/**
 * A basic game object with x and y coordinates.
 */
public class GameObject {
    protected int x, y;

    /**
     * Creates a GameObject with the given coordinates.
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public GameObject(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Gets the x-coordinate.
     * @return the x-coordinate
     */
    public int getX() {
        return this.x;
    }

    /**
     * Gets the y-coordinate.
     * @return the y-coordinate
     */
    public int getY() {
        return this.y;
    }

    /**
     * Sets the x-coordinate.
     * @param x the new x-coordinate
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Sets the y-coordinate.
     * @param y the new y-coordinate
     */
    public void setY(int y) {
        this.y = y;
    }
}
