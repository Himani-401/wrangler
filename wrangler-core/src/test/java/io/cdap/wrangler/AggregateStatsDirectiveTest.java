// AggregateStatsDirectiveTest.java

package io.cdap.wrangler;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Arrays;

public class AggregateStatsDirectiveTest {

    @Test
    public void testAggregateStats() {
        String[] recipe = new String[] {
            "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
        };

        List<Row> rows = createTestData();  // Create test data for aggregation
        List<Row> results = TestingRig.execute(recipe, rows);

        // Validate results
        assertEquals(1, results.size());
        assertEquals(expectedTotalSizeInMB, results.get(0).getValue("total_size_mb"), 0.001);
        assertEquals(expectedTotalTimeInSeconds, results.get(0).getValue("total_time_sec"), 0.001);
    }

    private List<Row> createTestData() {
        // Example data with byte size and time duration
        return Arrays.asList(
            new Row(1024, 1000),  // 1 KB, 1 second
            new Row(2048, 2000),  // 2 KB, 2 seconds
            new Row(512, 500)     // 0.5 KB, 0.5 second
        );
    }
}
