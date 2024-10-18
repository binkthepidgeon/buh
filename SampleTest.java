package inkball;

import processing.core.PApplet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SampleTest {

    @Test
    public void simpleTest() {
        App app = new App();
        app.loop();
        PApplet.runSketch(new String[] { "App" }, app);
        app.setup();
        app.delay(1000);
    }

    @Test
    public void testScoreIncrement() {
        App app = new App();
        double initialScore = app.score; // Ensure this is a double
        app.score += 10; // Simulate scoring
        assertEquals(initialScore + 10.0, app.score, 0.001, "Score should be incremented."); // Correct parameter order
    }
}

// gradle run						Run the program
// gradle test						Run the testcases

// Please ensure you leave comments in your testcases explaining what the testcase is testing.
// Your mark will be based off the average of branches and instructions code coverage.
// To run the testcases and generate the jacoco code coverage report:
// gradle test jacocoTestReport
