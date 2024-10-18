package inkball;

/**
 * A wall object with a colour and health.
 */
public class Wall extends GameObject {
    private final int colourCode;
    private int health;

    /**
     * Creates a Wall with the specified position and colour.
     * @param x the x-coordinate of the wall
     * @param y the y-coordinate of the wall
     * @param colourCode the colour code of the wall
     */
    public Wall(int x, int y, int colourCode) {
        super(x, y);
        this.colourCode = colourCode;
        this.health = 6;
    }

    /**
     * Returns the colour code of the wall.
     * @return the colour code
     */
    public int getColour() {
        return this.colourCode;
    }

    /**
     * Returns the current health of the wall.
     * @return the health of the wall
     */
    public int getHealth() {
        return this.health;
    }

    /**
     * Decreases the health of the wall by 1.
     */
    public void damage() {
        this.health--;
    }
}
