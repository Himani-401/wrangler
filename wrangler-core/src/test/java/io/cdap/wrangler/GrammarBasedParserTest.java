// GrammarBasedParserTest.java

package io.cdap.wrangler;

import org.junit.Test;
import static org.junit.Assert.*;

public class GrammarBasedParserTest {

    @Test
    public void testByteSizeTokenParsing() {
        String input = "10KB";
        ByteSize byteSize = new ByteSize(input);
        assertEquals(10240, byteSize.getBytes());  // Verify parsed value in bytes
    }

    @Test
    public void testTimeDurationTokenParsing() {
        String input = "1.5s";
        TimeDuration timeDuration = new TimeDuration(input);
        assertEquals(1500, timeDuration.getMilliseconds());  // Verify parsed value in milliseconds
    }

    @Test
    public void testInvalidByteSize() {
        String input = "10GB";  // Invalid unit for this example
        try {
            new ByteSize(input);
            fail("Expected an exception for unsupported byte size unit");
        } catch (IllegalArgumentException e) {
            // Expected exception
        }
    }
}
