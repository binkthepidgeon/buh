package inkball;

/**
 * A ball object with a position, velocity, scale factor, and colour.
 */
public class Ball extends GameObject {
    private int colourCode;
    private double velocityX, velocityY;
    private double scaleFactor;

    /**
     * Creates a Ball with the given position and colour code.
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param colourCode the ball's colour code
     */
    public Ball(int x, int y, int colourCode) {
        super(x, y);
        this.colourCode = colourCode;
        this.velocityX = (Math.random() < 0.5) ? -1 : 1;
        this.velocityY = (Math.random() < 0.5) ? -1 : 1;
        this.scaleFactor = 1;
    }

    /**
     * Gets the x-velocity of the ball.
     * @return the x-velocity
     */
    public double getVelocityX() {
        return this.velocityX;
    }

    /**
     * Gets the y-velocity of the ball.
     * @return the y-velocity
     */
    public double getVelocityY() {
        return this.velocityY;
    }

    /**
     * Gets the scale factor of the ball.
     * @return the scale factor
     */
    public double getScaleFactor() {
        return this.scaleFactor;
    }

    /**
     * Gets the colour code of the ball.
     * @return the colour code
     */
    public int getColour() {
        return this.colourCode;
    }

    /**
     * Moves the ball based on its velocity.
     */
    public void move() {
        this.x += velocityX;
        this.y += velocityY;
    }

    /**
     * Sets the x-velocity of the ball.
     * @param x the new x-velocity
     */
    public void setVelocityX(double x) {
        this.velocityX = x;
    }

    /**
     * Sets the y-velocity of the ball.
     * @param y the new y-velocity
     */
    public void setVelocityY(double y) {
        this.velocityY = y;
    }

    /**
     * Sets the scale factor of the ball.
     * @param scaleFactor the new scale factor
     */
    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    /**
     * Sets the colour code of the ball.
     * @param colourCode the new colour code
     */
    public void setColour(int colourCode) {
        this.colourCode = colourCode;
    }
}
