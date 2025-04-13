import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ByteSizeTest {

    @Test
    public void testByteSizeParsing() {
        ByteSize size = new ByteSize("10B");
        assertEquals(10, size.getBytes());
    }

    @Test
    public void testKilobytesParsing() {
        ByteSize size = new ByteSize("2.5KB");
        assertEquals(2.5 * 1024, size.getBytes());  // 2.5 KB in bytes
    }

    @Test
    public void testMegabytesParsing() {
        ByteSize size = new ByteSize("1MB");
        assertEquals(1 * 1024 * 1024, size.getBytes());  // 1 MB in bytes
    }

    @Test
    public void testGigabytesParsing() {
        ByteSize size = new ByteSize("1GB");
        assertEquals(1 * 1024 * 1024 * 1024, size.getBytes());  // 1 GB in bytes
    }

    @Test
    public void testInvalidUnit() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new ByteSize("10XYZ");
        });
        assertEquals("Unknown byte size unit: 10XYZ", exception.getMessage());
    }

    @Test
    public void testFractionalKilobytes() {
        ByteSize size = new ByteSize("2.5KB");
        assertEquals(2.5 * 1024, size.getBytes());  // 2.5 KB in bytes
    }
}
