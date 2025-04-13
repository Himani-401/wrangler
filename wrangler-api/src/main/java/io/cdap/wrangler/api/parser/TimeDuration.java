package io.cdap.wrangler.api.parser;

public class TimeDuration extends Token {
    private long milliseconds;

    public TimeDuration(String value) {
        this.milliseconds = parseTimeDuration(value);
    }

    private long parseTimeDuration(String value) {
        if (value.endsWith("ms")) {
            return Long.parseLong(value.replace("ms", ""));
        } else if (value.endsWith("s")) {
            return Long.parseLong(value.replace("s", "")) * 1000;
        } else if (value.endsWith("min")) {
            return Long.parseLong(value.replace("min", "")) * 60 * 1000;
        } else if (value.endsWith("h")) {
            return Long.parseLong(value.replace("h", "")) * 60 * 60 * 1000;
        } else {
            throw new IllegalArgumentException("Unknown time duration unit: " + value);
        }
    }

    public long getMilliseconds() {
        return milliseconds;
    }

    @Override
    public String toString() {
        return milliseconds + " ms";
    }
}
