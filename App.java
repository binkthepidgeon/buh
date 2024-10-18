package inkball;

import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.io.*;
import java.util.*;

/**
 * Inkball game.
 */
public class App extends PApplet {

    public static final int CELLSIZE = 32; //8;
    public static final int TOPBAR = 64;
    public static final int WIDTH = 576; //CELLSIZE*BOARD_WIDTH;
    public static final int HEIGHT = 640; //BOARD_HEIGHT*CELLSIZE+TOPBAR;

    public static final int FPS = 60;

    public String configPath;
    public int currentLevel = 0; // Current level index

    public static Random random = new Random();

    PImage ball0, ball1, ball2, ball3, ball4, entrypoint, hole0, hole1, hole2, hole3, hole4,
            inkball_spritesheet, tile, wall0, wall1, wall2, wall3, wall4,
            wall0damaged, wall1damaged, wall2damaged, wall3damaged, wall4damaged;
    public String[][] layout;
    public ArrayList<Spawner> spawners = new ArrayList<>();
    public ArrayList<Ball> balls = new ArrayList<>();
    public ArrayList<Hole> holes = new ArrayList<>();
    public ArrayList<Wall> walls = new ArrayList<>();
    public ArrayList<String> unspawnedBalls = new ArrayList<>();
    public ArrayList<String> capturedBalls = new ArrayList<>();
    ArrayList<Line> lines = new ArrayList<>();
    Line line = new Line();
    int unspawnedBallOffset = 0;
    public int levelTime;
    public int spawnInterval;
    public double scoreIncreaseModifier;
    public double scoreDecreaseModifier;
    public long remainingTime;
    public long spawnTime;
    private long lastMillis;
    long lastSpawnTime = 0;
    public double score = 0;
    public double resetScore = 0;
    public boolean gameWon = false;
    public boolean levelLost = false;
    public boolean levelWon = false;
    public boolean gamePaused = false;
    public int grey_inc;
    public int orange_inc;
    public int blue_inc;
    public int green_inc;
    public int yellow_inc;
    public int grey_dec;
    public int orange_dec;
    public int blue_dec;
    public int green_dec;
    public int yellow_dec;
    public double scoreTime = 0;
    Wall yellowTile1 = new Wall(0, 64, 4);
    Wall yellowTile2 = new Wall(544, 608, 4);

	// Feel free to add any additional methods or attributes you want. Please put classes in different files.
    /**
     * Constructor for the App class.
     * Initializes the configuration path for the game.
     */
    public App() {
        this.configPath = "config.json";
    }

    /**
     * Initialise the setting of the window size.
     */
	@Override
    public void settings() {
        size(WIDTH, HEIGHT);
    }

    /**
     * Load all resources such as images. Initialise map elements and timers.
     */
	@Override
    public void setup() {
        frameRate(FPS);
        loadImages();
        readConfigAndLayout();
        remainingTime = levelTime;
        lastMillis = millis();
        spawnTime = (long) spawnInterval * FPS;
    }

    /**
     * Receive key pressed signal from the keyboard.
     * Facilitates restarting and pausing the game.
     * @param event the key event that triggered this method
     */
	@Override
    public void keyPressed(KeyEvent event){
        if (event.getKey() == 'r' || event.getKey() == 'R') {
            if (gameWon) {
                currentLevel = 0;
                resetScore = 0;
                readConfigAndLayout();
            }
            resetLevel();
        }
        if (event.getKey() == 'w' || event.getKey() == 'W') {
            balls.clear();
            unspawnedBalls.clear();
        }
        if (key == ' ' && !levelLost && !gameWon) {
            gamePaused = !gamePaused;
        }
    }

    /**
     * Receive button pressed signal from the mouse.
     * Facilitates the removal of lines.
     */
    @Override
    public void mousePressed() {
        if (levelLost || gameWon || levelWon) return;
        if (mouseButton == RIGHT || (mouseButton == LEFT && (keyPressed && key == CODED && keyCode == CONTROL))) {
            for (int i = lines.size() - 1; i >= 0; i--) {
                Line line = lines.get(i);
                boolean lineRemoved = false;

                // Check each segment of the line
                for (int j = 0; j < line.getSize() - 1; j++) {
                    int[] start = line.getPoint(j);
                    int[] end = line.getPoint(j + 1);

                    // Calculate the distance from the mouse click to the line segment
                    float x1 = start[0];
                    float y1 = start[1];
                    float x2 = end[0];
                    float y2 = end[1];
                    float px = mouseX;
                    float py = mouseY;

                    // Calculate the length of the line segment
                    float lineLengthSquared = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);

                    // Check if the line segment is actually a point
                    if (lineLengthSquared == 0) {
                        float distance = dist(px, py, x1, y1);
                        if (distance < 10) {
                            lines.remove(i);
                            lineRemoved = true;
                            break; // Exit after removing one line
                        }
                    } else {
                        // Calculate t, the projection factor
                        float t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / lineLengthSquared;
                        t = constrain(t, 0, 1); // Clamp t to the range [0, 1]

                        // Calculate the closest point on the line segment
                        float closestX = x1 + t * (x2 - x1);
                        float closestY = y1 + t * (y2 - y1);

                        // Calculate the distance from the mouse to the closest point
                        float distance = dist(px, py, closestX, closestY);
                        if (distance < 10) {
                            lines.remove(i);
                            lineRemoved = true;
                            break; // Exit after removing one line
                        }
                    }
                }
                if (lineRemoved) {
                    break; // Exit the outer loop if a line was removed
                }
            }
        }
    }

    /**
     * Receive sensor movement signal from the mouse.
     * Facilitates drawing a line.
     * @param e the mouse event that triggered this method
     */
	@Override
    public void mouseDragged(MouseEvent e) {
        if (levelLost || gameWon || levelWon) return;
        if (mouseButton == LEFT) {  // Only add points if left mouse button is held
            line.addPoint(e.getX(), e.getY());  // Add point to current line
        }
    }


    /**
     * Receive button released signal from the mouse.
     * Facilitates termination of current line and adds it to lines array.
     * @param e the mouse event that triggered this method
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (levelLost || gameWon || levelWon) return;
        if (mouseButton == LEFT && line.getSize() > 0) {
            lines.add(line);  // Add the finished line to the list
            line = new Line();  // Start a new line for the next drawing
        }
    }

    /**
     * Draw all elements in the game by current frame and manages timers.
     */
    @Override
    public void draw() {
        background(255);
        drawBoard();
        drawBalls();
        spawnBalls();
        drawLines();
        processWin();
        drawTopbar();

        if (!gamePaused && remainingTime > 0) {
            if (millis() - lastMillis >= 1000) {
                remainingTime--;
                lastMillis = millis();
            }
        }

        if (remainingTime == 0 && !(balls.isEmpty() && unspawnedBalls.isEmpty())) {
            levelLost = true;
        }

        if (!gamePaused && !levelLost && spawnTime > 0 && !unspawnedBalls.isEmpty()) {
            spawnTime --;
        }

        if (unspawnedBallOffset > 0) {
            unspawnedBallOffset -= 1;
        }

        if (unspawnedBalls.isEmpty() && balls.isEmpty()) {
            levelWon = true;
        }
    }

    /**
     * Loads level timers, modifiers, balls, and layout.
     */
    public void readConfigAndLayout() {
        try {
            // Read config.json
            JSONObject config = loadJSONObject(configPath);
            JSONArray levels = config.getJSONArray("levels");

            // Load the current level
            JSONObject levelConfig = levels.getJSONObject(currentLevel);
            String layoutFile = levelConfig.getString("layout");

            // Set up the level variables
            levelTime = levelConfig.getInt("time");
            spawnInterval = levelConfig.getInt("spawn_interval");
            scoreIncreaseModifier = levelConfig.getDouble("score_increase_from_hole_capture_modifier");
            scoreDecreaseModifier = levelConfig.getDouble("score_decrease_from_wrong_hole_modifier");
            JSONArray jsonballs = levelConfig.getJSONArray("balls");
            for (int i = 0; i < jsonballs.size(); i++) {
                unspawnedBalls.add(jsonballs.getString(i));
            }

            // Set up score increases
            JSONObject scoreIncreaseFromHoleCapture = config.getJSONObject("score_increase_from_hole_capture");
            grey_inc = scoreIncreaseFromHoleCapture.getInt("grey");
            orange_inc = scoreIncreaseFromHoleCapture.getInt("orange");
            blue_inc = scoreIncreaseFromHoleCapture.getInt("blue");
            green_inc = scoreIncreaseFromHoleCapture.getInt("green");
            yellow_inc = scoreIncreaseFromHoleCapture.getInt("yellow");

            // Set up score decreases
            JSONObject scoreDecreaseFromWrongHole = config.getJSONObject("score_decrease_from_wrong_hole");
            grey_dec = scoreDecreaseFromWrongHole.getInt("grey");
            orange_dec = scoreDecreaseFromWrongHole.getInt("orange");
            blue_dec = scoreDecreaseFromWrongHole.getInt("blue");
            green_dec = scoreDecreaseFromWrongHole.getInt("green");
            yellow_dec = scoreDecreaseFromWrongHole.getInt("yellow");

            // Read layout file
            BufferedReader layoutReader = new BufferedReader(new FileReader(layoutFile));
            String line;
            int row = 0;
            layout = new String[18][18];
            while ((line = layoutReader.readLine()) != null) {
                for (int col = 0; col < line.length(); col++) {
                    layout[row][col] = Character.toString(line.charAt(col));
                    if (line.charAt(col) == 'S') {
                        Spawner spawner = new Spawner(col * CELLSIZE, row * CELLSIZE + TOPBAR);
                        spawners.add(spawner);
                    } else if (line.charAt(col) == 'B') {
                        Ball ball = new Ball(col * CELLSIZE, row * CELLSIZE + TOPBAR, Character.getNumericValue(line.charAt(col + 1)));
                        balls.add(ball);
                    } else if (line.charAt(col) == 'H') {
                        Hole hole = new Hole(col * CELLSIZE, row * CELLSIZE + TOPBAR, Character.getNumericValue(line.charAt(col + 1)));
                        holes.add(hole);
                    } else if (line.charAt(col) == 'X') {
                        Wall wall = new Wall(col * CELLSIZE, row * CELLSIZE + TOPBAR, 0);
                        walls.add(wall);
                    } if (line.charAt(col) == '1' && (col == 0 || (line.charAt(col - 1) != 'H' && line.charAt(col - 1) != 'B'))) {
                        Wall wall = new Wall(col * CELLSIZE, row * CELLSIZE + TOPBAR, 1);
                        walls.add(wall);
                    } else if (line.charAt(col) == '2' && (col == 0 || (line.charAt(col - 1) != 'H' && line.charAt(col - 1) != 'B'))) {
                        Wall wall = new Wall(col * CELLSIZE, row * CELLSIZE + TOPBAR, 2);
                        walls.add(wall);
                    } else if (line.charAt(col) == '3' && (col == 0 || (line.charAt(col - 1) != 'H' && line.charAt(col - 1) != 'B'))) {
                        Wall wall = new Wall(col * CELLSIZE, row * CELLSIZE + TOPBAR, 3);
                        walls.add(wall);
                    } else if (line.charAt(col) == '4' && (col == 0 || (line.charAt(col - 1) != 'H' && line.charAt(col - 1) != 'B'))) {
                        Wall wall = new Wall(col * CELLSIZE, row * CELLSIZE + TOPBAR, 4);
                        walls.add(wall);
                    }
                }
                row++;
            }
            layoutReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads images for game elements.
     */
    public void loadImages() {
        ball0 = loadImage("inkball/ball0.png");
        ball1 = loadImage("inkball/ball1.png");
        ball2 = loadImage("inkball/ball2.png");
        ball3 = loadImage("inkball/ball3.png");
        ball4 = loadImage("inkball/ball4.png");
        entrypoint = loadImage("inkball/entrypoint.png");
        hole0 = loadImage("inkball/hole0.png");
        hole1 = loadImage("inkball/hole1.png");
        hole2 = loadImage("inkball/hole2.png");
        hole3 = loadImage("inkball/hole3.png");
        hole4 = loadImage("inkball/hole4.png");
        inkball_spritesheet = loadImage("inkball/inkball_spritesheet.png");
        tile = loadImage("inkball/tile.png");
        wall0 = loadImage("inkball/wall0.png");
        wall1 = loadImage("inkball/wall1.png");
        wall2 = loadImage("inkball/wall2.png");
        wall3 = loadImage("inkball/wall3.png");
        wall4 = loadImage("inkball/wall4.png");
        wall0damaged = loadImage("inkball/wall0damaged.png");
        wall1damaged = loadImage("inkball/wall1damaged.png");
        wall2damaged = loadImage("inkball/wall2damaged.png");
        wall3damaged = loadImage("inkball/wall3damaged.png");
        wall4damaged = loadImage("inkball/wall4damaged.png");

    }

    /**
     * Draws balls to be spawned, timers, and game status e.g. paused, time's up, ended.
     */
    public void drawTopbar() {
        fill(200);
        stroke(200);
        rect(0, 0, WIDTH, 58);
        fill(0);
        stroke(0);
        rect(18, 17, 157, 32);
        drawunspawnedBalls();
        fill(200);
        stroke(200);
        rect(185, 0, WIDTH - 185, 58);

        fill(0);
        textSize(21);
        text("Score: " + (int) score, 457, 30);
        text("Time: " + remainingTime, 466, 55);

        if (gameWon){
            fill(0);
            textSize(24);
            text("=== ENDED ===", 223, 42);
        }
        else if (levelLost){
            fill(0);
            textSize(24);
            text("=== TIME'S UP ===", 213, 42);
        }

        if (gamePaused) {
            fill(0);
            textSize(24);
            text("*** PAUSED *** ", 250, 42);
        }

        if (!unspawnedBalls.isEmpty() && !levelLost) {
            fill(0); // Set text colour to black
            textSize(24); // Set font size
            text(String.format("%.1f", (float) spawnTime / FPS), 190, 42);
        }
    }

    /**
     * Draws tiles, then holes, walls and spawners on top.
     */
    public void drawBoard() {
        for (int row = 0; row < layout.length; row++) {
            for (int col = 0; col < layout[row].length; col++) {
                int x = col * CELLSIZE;
                int y = row * CELLSIZE + TOPBAR;
                image(tile, x, y);
            }
        }
        drawHoles();
        drawWalls();
        drawSpawners();
    }

    /**
     * Iterates through walls array list and draws each wall appropriately.
     */
    public void drawWalls() {
        List<Wall> wallsToRemove = new ArrayList<>();
        for (Wall wall : walls) {
            int colourCode = wall.getColour();

            int x = wall.getX();
            int y = wall.getY();
            int health = wall.getHealth();
            if (health <= 0) {
                wallsToRemove.add(wall);
            }
            else if (health == 6) {
                switch (colourCode) {
                    case 0:
                        image(wall0, x, y, CELLSIZE, CELLSIZE);
                        break;
                    case 1:
                        image(wall1, x, y, CELLSIZE, CELLSIZE);
                        break;
                    case 2:
                        image(wall2, x, y, CELLSIZE, CELLSIZE);
                        break;
                    case 3:
                        image(wall3, x, y, CELLSIZE, CELLSIZE);
                        break;
                    case 4:
                        image(wall4, x, y, CELLSIZE, CELLSIZE);
                        break;
                }
            }
            else {
                switch (colourCode) {
                    case 0:
                        image(wall0damaged, x, y, CELLSIZE, CELLSIZE);
                        break;
                    case 1:
                        image(wall1damaged, x, y, CELLSIZE, CELLSIZE);
                        break;
                    case 2:
                        image(wall2damaged, x, y, CELLSIZE, CELLSIZE);
                        break;
                    case 3:
                        image(wall3damaged, x, y, CELLSIZE, CELLSIZE);
                        break;
                    case 4:
                        image(wall4damaged, x, y, CELLSIZE, CELLSIZE);
                        break;
                }
            }
        }
        walls.removeAll(wallsToRemove);
    }

    /**
     * Iterates through holes array list and draws each hole appropriately.
     */
    public void drawHoles() {
        for (Hole hole : holes) {
            int colourCode = hole.getColour();

            int x = hole.getX();
            int y = hole.getY();

            switch (colourCode) {
                case 0:
                    image(hole0, x, y, CELLSIZE * 2, CELLSIZE * 2);
                    break;
                case 1:
                    image(hole1, x, y, CELLSIZE * 2, CELLSIZE * 2);
                    break;
                case 2:
                    image(hole2, x, y, CELLSIZE * 2, CELLSIZE * 2);
                    break;
                case 3:
                    image(hole3, x, y, CELLSIZE * 2, CELLSIZE * 2);
                    break;
                case 4:
                    image(hole4, x, y, CELLSIZE * 2, CELLSIZE * 2);
                    break;
            }
        }
    }

    /**
     * Iterates through spawners array list and draws each spawner appropriately.
     */
    public void drawSpawners() {
        for (Spawner spawner : spawners) {

            int x = spawner.getX();
            int y = spawner.getY();

            image(entrypoint, x, y, CELLSIZE, CELLSIZE);
        }
    }

    /**
     * Iterates through balls array list and draws each ball appropriately.
     */
    public void drawBalls() {
        List<Ball> ballsToRemove = new ArrayList<>();
        for (Ball ball : balls) {
            int colourCode = ball.getColour();
            int x = ball.getX() + 4;
            int y = ball.getY() + 4;

            switch (colourCode) {
                case 0:
                    image(ball0, x, y, (int)(26 * ball.getScaleFactor()), (int)(26 * ball.getScaleFactor()));
                    break;
                case 1:
                    image(ball1, x, y, (int)(26 * ball.getScaleFactor()), (int)(26 * ball.getScaleFactor()));
                    break;
                case 2:
                    image(ball2, x, y, (int)(26 * ball.getScaleFactor()), (int)(26 * ball.getScaleFactor()));
                    break;
                case 3:
                    image(ball3, x, y, (int)(26 * ball.getScaleFactor()), (int)(26 * ball.getScaleFactor()));
                    break;
                case 4:
                    image(ball4, x, y, (int)(26 * ball.getScaleFactor()), (int)(26 * ball.getScaleFactor()));
                    break;
            }
            if (!levelLost && !gamePaused) ball.move();

            if (x < -20 || x > 580 || y < 52 || y > 644) {
                String colour = null;
                switch (colourCode) {
                    case 0:
                        colour = "grey";
                        break;
                    case 1:
                        colour = "orange";
                        break;
                    case 2:
                        colour = "blue";
                        break;
                    case 3:
                        colour = "green";
                        break;
                    case 4:
                        colour = "yellow";
                        break;
                }
                unspawnedBalls.add(colour);
                ballsToRemove.add(ball);
            }

            List<Line> linesToRemove = new ArrayList<>(); // Create a list to collect lines to remove
            for (Line line : lines) {
                // Check for collision
                if (checkLineCollision(ball, line) != null) {
                    reflectBall(ball, checkLineCollision(ball, line));
                    if (!gamePaused) {
                        linesToRemove.add(line);
                    }
                }
            }
            lines.removeAll(linesToRemove);

            for (Wall wall : walls) {
                if (checkWallCollision(ball, wall) != null) {
                    reflectBall(ball, checkWallCollision(ball, wall));
                    if (wall.getColour() != 0) {
                        ball.setColour(wall.getColour());
                    }
                }
            }

            for (Hole hole : holes) {
                if (checkHoleCapture(ball, hole)) {
                    int holeColourCode = hole.getColour();
                    int ballColourCode = ball.getColour();
                    boolean success = false;

                    switch (ballColourCode) {
                        case 0:  // Grey ball (0) can enter any hole
                            success = true;
                            score += grey_inc * scoreIncreaseModifier;
                            break;
                        case 1:  // Orange ball (1)
                            if (holeColourCode == 1 || holeColourCode == 0) {  // Orange or Grey hole
                                success = true;
                                score += orange_inc * scoreIncreaseModifier;
                            } else {
                                score -= orange_dec * scoreDecreaseModifier;
                            }
                            break;
                        case 2:  // Blue ball (2)
                            if (holeColourCode == 2 || holeColourCode == 0) {  // Blue or Grey hole
                                success = true;
                                score += blue_inc * scoreIncreaseModifier;
                            } else {
                                score -= blue_dec * scoreDecreaseModifier;
                            }
                            break;
                        case 3:  // Green ball (3)
                            if (holeColourCode == 3 || holeColourCode == 0) {  // Green or Grey hole
                                success = true;
                                score += green_inc * scoreIncreaseModifier;
                            } else {
                                score -= green_dec * scoreDecreaseModifier;
                            }
                            break;
                        case 4:  // Yellow ball (4)
                            if (holeColourCode == 4 || holeColourCode == 0) {  // Yellow or Grey hole
                                success = true;
                                score += yellow_inc * scoreIncreaseModifier;
                            } else {
                                score -= yellow_dec * scoreDecreaseModifier;
                            }
                            break;
                    }

                    // If successful, remove the ball; if not, rejoin the queue
                    ballsToRemove.add(ball);
                    if (!success) {
                        String colour = null;
                        int colourcode = ball.getColour();

                        switch (colourcode) {
                            case 0:
                                colour = "grey";
                                break;
                            case 1:
                                colour = "orange";
                                break;
                            case 2:
                                colour = "blue";
                                break;
                            case 3:
                                colour = "green";
                                break;
                            case 4:
                                colour = "yellow";
                                break;
                        }
                        unspawnedBalls.add(colour);  // Method to re-add the color of the ball to the queue
                    }
                }
            }
        }
        balls.removeAll(ballsToRemove);
    }

    /**
     * Iterates through lines array list and draws each line appropriately.
     */
    public void drawLines() {
        if (levelWon) return;
        stroke(0);
        strokeWeight(10);

        // Draw the lines that the player has already finished drawing
        for (Line line : lines) {
            for (int i = 0; i < line.getSize() - 1; i++) {
                int[] point1 = line.getPoint(i);
                int[] point2 = line.getPoint(i + 1);
                line(point1[0], point1[1], point2[0], point2[1]);
            }
        }

        // Draw the current line that is still being drawn
        if (line.getSize() > 0) {
            for (int i = 0; i < line.getSize() - 1; i++) {
                int[] point1 = line.getPoint(i);
                int[] point2 = line.getPoint(i + 1);
                line(point1[0], point1[1], point2[0], point2[1]);  // Draw the line in progress
            }
        }
    }

    /**
     * Spawns balls at a predetermined interval, at a random spawner.
     */
    public void spawnBalls() {
        // Only attempt to spawn a ball if the game is not paused and the spawn interval has reached 0
        if (!gamePaused && spawnTime == 0) {
            if (!unspawnedBalls.isEmpty()) {
                String ballColour = unspawnedBalls.get(0);
                int colourCode = -1;
                switch (ballColour) {
                    case "grey":
                        colourCode = 0;
                        break;
                    case "orange":
                        colourCode = 1;
                        break;
                    case "blue":
                        colourCode = 2;
                        break;
                    case "green":
                        colourCode = 3;
                        break;
                    case "yellow":
                        colourCode = 4;
                        break;
                }

                int randomSpawnerIndex = random.nextInt(spawners.size());
                Spawner randomSpawner = spawners.get(randomSpawnerIndex);

                int spawnX = randomSpawner.getX();
                int spawnY = randomSpawner.getY();

                Ball ball = new Ball(spawnX, spawnY, colourCode);
                balls.add(ball);
                unspawnedBalls.remove(0);
                unspawnedBallOffset = 32;
            }

            // Reset spawnTime to the calculated interval based on spawnInterval and FPS
            spawnTime = (long) spawnInterval * FPS;
        }
    }

    /**
     * Displays unspawned balls in the topbar.
     */
    public void drawunspawnedBalls() {
        for (int i = 0; i < unspawnedBalls.size(); i++) {
            String ballColour = unspawnedBalls.get(i);
            int x = 21 + i * 32 + unspawnedBallOffset;

            switch (ballColour) {
                case "grey":
                    image(ball0, x, 22);
                    break;
                case "orange":
                    image(ball1, x, 22);
                    break;
                case "blue":
                    image(ball2, x, 22);
                    break;
                case "green":
                    image(ball3, x, 22);
                    break;
                case "yellow":
                    image(ball4, x, 22);
                    break;
            }
        }
    }

    /**
     * Resets necessary game elements to restart the level.
     */
    private void resetLevel() {
        score = resetScore;
        remainingTime = levelTime;
        lastMillis = millis();
        lastSpawnTime = lastMillis;
        gameWon = false;
        gamePaused = false;
        levelLost = false;
        levelWon = false;
        spawners.clear();
        balls.clear();
        holes.clear();
        walls.clear();
        unspawnedBalls.clear();
        capturedBalls.clear();
        spawnTime = (long) spawnInterval * FPS;
        lines.clear();
        line = new Line();
        yellowTile1.setX(0);
        yellowTile1.setY(64);
        yellowTile2.setX(544);
        yellowTile2.setY(608);
        scoreTime = 0;
        readConfigAndLayout();
    }

    /**
     * Checks if a ball is colliding with a line. Returns
     * the segment of the line where the collision occurred.
     * @param ball the ball object whose collision is being checked
     * @param line the line object that may be collided with
     * @return a 2D array containing the segment points of the collision
     *         or {@code null} if no collision is detected
     */
    public int[][] checkLineCollision(Ball ball, Line line) {
        for (int i = 0; i < line.getSize() - 1; i++) {
            // Ball's next position after moving
            double ballNextX = ball.getX() + 2 * ball.getVelocityX() + 16;
            double ballNextY = ball.getY() + 2 * ball.getVelocityY() + 16;

            // Get line segment points
            int[] p1 = line.getPoint(i);
            int[] p2 = line.getPoint(i + 1);

            // Calculate distances from the ball's next position to both points
            double distanceBallP1 = Math.hypot(ballNextX - p1[0], ballNextY - p1[1]);
            double distanceBallP2 = Math.hypot(ballNextX - p2[0], ballNextY - p2[1]);

            // Distance between P1 and P2
            double distanceP1P2 = Math.hypot(p2[0] - p1[0], p2[1] - p1[1]);

            // Check collision condition including ball radius
            if (distanceBallP1 + distanceBallP2 < distanceP1P2 + 24) {
                // Collision detected
                return new int[][] {p1, p2};
            }
        }
        return null;
    }

    /**
     * Checks if a ball is colliding with a wall. If a collision is detected,
     * it applies damage to the wall based on the ball's color and returns
     * the segment of the wall where the collision occurred.
     * @param ball the ball object whose collision is being checked
     * @param wall the wall object that may be collided with
     * @return a 2D array containing the segment points of the collision
     *         or {@code null} if no collision is detected
     */
    public int[][] checkWallCollision(Ball ball, Wall wall) {
        // Calculate the ball's next position
        double ballNextX = ball.getX() + 2 * ball.getVelocityX() + 16;
        double ballNextY = ball.getY() + 2 * ball.getVelocityY() + 16;
        int wallX = wall.getX();
        int wallY = wall.getY();
        int wallColourCode = wall.getColour();
        int ballColourCode = ball.getColour();

        // Define the wall segments as points (P1, P2)
        int[][] wallSegments = {
                {wallX, wallY},                     // Top-left (P1 of top segment)
                {wallX + CELLSIZE, wallY},         // Top-right (P2 of top segment)
                {wallX + CELLSIZE, wallY + CELLSIZE}, // Bottom-right (P2 of right segment)
                {wallX, wallY + CELLSIZE},         // Bottom-left (P1 of bottom segment)
                {wallX, wallY},                     // Repeated for bottom-left (P1 of left segment)
                {wallX, wallY + CELLSIZE},         // Bottom-left (P2 of left segment)
                {wallX, wallY + CELLSIZE},         // Bottom-left (P1 of right segment)
                {wallX + CELLSIZE, wallY + CELLSIZE} // Bottom-right (P2 of bottom segment)
        };

        // Check each segment for a collision
        for (int i = 0; i < 4; i++) {
            // Get the segment points (P1 and P2)
            int[] p1 = wallSegments[i];
            int[] p2 = wallSegments[i + 1];

            // Calculate distances from the ball's next position to both points
            double distanceBallP1 = Math.hypot(ballNextX - p1[0], ballNextY - p1[1]);
            double distanceBallP2 = Math.hypot(ballNextX - p2[0], ballNextY - p2[1]);

            // Distance between P1 and P2
            double distanceP1P2 = Math.hypot(p2[0] - p1[0], p2[1] - p1[1]);

            // Check collision condition including ball radius
            if (distanceBallP1 + distanceBallP2 < distanceP1P2 + 8) {
                // Collision detected, return the segment points
                switch (wallColourCode) {
                    case 0:
                        // Grey brick can be damaged by any ball
                        wall.damage();
                        break;
                    case 1:
                        if (ballColourCode == 1) {
                            wall.damage(); // Only damage if the ball is red
                        }
                        break;
                    case 2:
                        if (ballColourCode == 2) {
                            wall.damage(); // Only damage if the ball is blue
                        }
                        break;
                    case 3:
                        if (ballColourCode == 3) {
                            wall.damage(); // Only damage if the ball is green
                        }
                        break;
                    case 4:
                        if (ballColourCode == 4) {
                            wall.damage(); // Only damage if the ball is green
                        }
                        break;
                }
                return new int[][] {p1, p2};
            }
        }
        return null;
    }

    /**
     * Reflects the ball off a surface based on a collision with a line segment.
     * The method calculates the normal vector of the segment, determines the
     * correct reflection direction, and updates the ball's velocity accordingly.
     *
     * @param ball the ball object to reflect
     * @param collisionPoints a 2D array containing the points of the line
     *                        segment where the collision occurred
     */
    public void reflectBall(Ball ball, int[][] collisionPoints) {
        // Extract points P1 and P2 from collision points
        int[] p1 = collisionPoints[0];
        int[] p2 = collisionPoints[1];

        // Calculate dx and dy
        double dx = p2[0] - p1[0];
        double dy = p2[1] - p1[1];

        // Calculate the normal vectors (N1 and N2)
        double normalX1 = -dy; // Normal vector N1
        double normalY1 = dx;
        double normalX2 = dy;  // Normal vector N2
        double normalY2 = -dx;

        // Normalize the normal vectors
        double length1 = Math.hypot(normalX1, normalY1);
        double length2 = Math.hypot(normalX2, normalY2);

        if (length1 != 0) {
            normalX1 /= length1;
            normalY1 /= length1;
        }
        if (length2 != 0) {
            normalX2 /= length2;
            normalY2 /= length2;
        }

        // Calculate the midpoint of the line segment
        double midpointX = (p1[0] + p2[0]) / 2.0;
        double midpointY = (p1[1] + p2[1]) / 2.0;

        // Calculate positions M1 and M2
        double m1X = midpointX + normalX1;
        double m1Y = midpointY + normalY1;
        double m2X = midpointX + normalX2;
        double m2Y = midpointY + normalY2;

        // Get the ball's position
        double ballX = ball.getX();
        double ballY = ball.getY();

        // Calculate distances from the ball to M1 and M2
        double distanceToM1 = Math.hypot(ballX - m1X, ballY - m1Y);
        double distanceToM2 = Math.hypot(ballX - m2X, ballY - m2Y);

        // Choose the normal vector that is closer to the ball
        double chosenNormalX, chosenNormalY;
        if (distanceToM1 < distanceToM2) {
            chosenNormalX = normalX1;
            chosenNormalY = normalY1;
        } else {
            chosenNormalX = normalX2;
            chosenNormalY = normalY2;
        }

        // Get the current velocity vector v of the ball
        double vX = ball.getVelocityX();
        double vY = ball.getVelocityY();

        // Calculate the dot product v ⋅ n
        double dotProduct = vX * chosenNormalX + vY * chosenNormalY;

        // Calculate the new trajectory u
        double newVx = vX - 2 * dotProduct * chosenNormalX;
        double newVy = vY - 2 * dotProduct * chosenNormalY;

        // Set the new velocity for the ball
        ball.setVelocityX(newVx);
        ball.setVelocityY(newVy);
    }

    /**
     * Checks if a ball is being captured by a hole. Applies an attraction force
     * towards the center of the hole if the ball is within range. Ball size
     * is also reduced proportionally based on the distance.
     * @param ball the ball object to check and potentially attract to the hole
     * @param hole the hole object that might capture the ball
     * @return {@code true} if the ball is captured by the hole
     *         {@code false} otherwise
     */
    public boolean checkHoleCapture(Ball ball, Hole hole) {
        double ballX = ball.getX() + ball.getVelocityX() + 15;
        double ballY = ball.getY() + ball.getVelocityY() + 15;
        int holeX = hole.getX() + 32;
        int holeY = hole.getY() + 32;

        // Calculate vector from ball to hole center
        double deltaX = holeX - ballX;
        double deltaY = holeY - ballY;

        double distanceToHole = Math.hypot(deltaX, deltaY);

        if (distanceToHole <= 32) {
            // Attraction force: 0.5% of the vector towards the hole
            double attractionForceX = deltaX * 0.005;
            double attractionForceY = deltaY * 0.005;

            // Apply the attraction force to the ball's velocity
            ball.setVelocityX(ball.getVelocityX() + attractionForceX);
            ball.setVelocityY(ball.getVelocityY() + attractionForceY);

            // Reduce the size of the ball proportionally to the distance
            double scaleFactor = distanceToHole / 32.0; // Scale factor: 1 when at 32 pixels, 0 when at center
            ball.setScaleFactor(scaleFactor);

            // If the ball is at the center of the hole (or very close), capture it
            return distanceToHole < 12.5;
        }
        return false;
    }

    /**
     * Facilitates adding remaining time to score and win animation during this process.
     */
    public void processWin() {
        if (levelWon) {
            resetScore = score;
            if (remainingTime > 0) {
                scoreTime += 1.0 / FPS;  // Accumulate time using the frame rate
                if (scoreTime >= (double) 1 / 15) {
                    score += 1;  // Add 1 unit to score
                    remainingTime -= 1;  // Decrease remaining time
                    scoreTime = 0;  // Reset accumulator

                    // Move yellowTile1 clockwise
                    if (yellowTile1.getY() == 64 && yellowTile1.getX() < 576 - 32) {
                        yellowTile1.setX(yellowTile1.getX() + 32);  // Move right along the top edge
                    } else if (yellowTile1.getX() == 576 - 32 && yellowTile1.getY() < 640 - 32) {
                        yellowTile1.setY(yellowTile1.getY() + 32);  // Move down along the right edge
                    } else if (yellowTile1.getY() == 640 - 32 && yellowTile1.getX() > 0) {
                        yellowTile1.setX(yellowTile1.getX() - 32);  // Move left along the bottom edge
                    } else if (yellowTile1.getX() == 0 && yellowTile1.getY() > 64) {
                        yellowTile1.setY(yellowTile1.getY() - 32);  // Move up along the left edge
                    }

                    // Move yellowTile2 clockwise (starting from bottom right)
                    if (yellowTile2.getY() == 640 - 32 && yellowTile2.getX() > 0) {
                        yellowTile2.setX(yellowTile2.getX() - 32);  // Move left along the bottom edge
                    } else if (yellowTile2.getX() == 0 && yellowTile2.getY() > 64) {
                        yellowTile2.setY(yellowTile2.getY() - 32);  // Move up along the left edge
                    } else if (yellowTile2.getY() == 64 && yellowTile2.getX() < 576 - 32) {
                        yellowTile2.setX(yellowTile2.getX() + 32);  // Move right along the top edge
                    } else if (yellowTile2.getX() == 576 - 32 && yellowTile2.getY() < 640 - 32) {
                        yellowTile2.setY(yellowTile2.getY() + 32);  // Move down along the right edge
                    }
                }
                image(wall4, yellowTile1.getX(), yellowTile1.getY(), CELLSIZE, CELLSIZE);
                image(wall4, yellowTile2.getX(), yellowTile2.getY(), CELLSIZE, CELLSIZE);

            }
            else if (currentLevel == 2) {
                image(wall4, yellowTile1.getX(), yellowTile1.getY(), CELLSIZE, CELLSIZE);
                image(wall4, yellowTile2.getX(), yellowTile2.getY(), CELLSIZE, CELLSIZE);
                gameWon = true;
            } else {
                scoreTime += 1.0 / FPS;
                image(wall4, yellowTile1.getX(), yellowTile1.getY(), CELLSIZE, CELLSIZE);
                image(wall4, yellowTile2.getX(), yellowTile2.getY(), CELLSIZE, CELLSIZE);
                if (scoreTime >= (double) 1) {
                    currentLevel ++;
                    readConfigAndLayout();
                    resetLevel();
                }
            }
        }
    }

    public static void main(String[] args) {PApplet.main("inkball.App");}
}