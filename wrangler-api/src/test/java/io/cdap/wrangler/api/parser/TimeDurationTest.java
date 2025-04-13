import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TimeDurationTest {

    @Test
    public void testMillisecondsParsing() {
        TimeDuration duration = new TimeDuration("100ms");
        assertEquals(100, duration.getMilliseconds());
    }

    @Test
    public void testSecondsParsing() {
        TimeDuration duration = new TimeDuration("2.5s");
        assertEquals(2500, duration.getMilliseconds());
    }

    @Test
    public void testMinutesParsing() {
        TimeDuration duration = new TimeDuration("3min");
        assertEquals(180000, duration.getMilliseconds());  // 3 minutes in ms
    }

    @Test
    public void testHoursParsing() {
        TimeDuration duration = new TimeDuration("1h");
        assertEquals(3600000, duration.getMilliseconds());  // 1 hour in ms
    }

    @Test
    public void testInvalidUnit() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new TimeDuration("10xyz");
        });
        assertEquals("Unknown time duration unit: 10xyz", exception.getMessage());
    }

    @Test
    public void testFractionalSeconds() {
        TimeDuration duration = new TimeDuration("0.5s");
        assertEquals(500, duration.getMilliseconds());  // 0.5 seconds in ms
    }
}
