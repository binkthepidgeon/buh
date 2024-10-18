package inkball;

/**
 * A hole object with a position and a colour code.
 */
public class Hole extends GameObject {
    private final int colourCode;

    /**
     * Creates a Hole with the given position and colour code.
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param colourCode the hole's colour code
     */
    public Hole(int x, int y, int colourCode) {
        super(x, y);
        this.colourCode = colourCode;
    }

    /**
     * Gets the colour code of the hole.
     * @return the colour code
     */
    public int getColour() {
        return this.colourCode;
    }
}
