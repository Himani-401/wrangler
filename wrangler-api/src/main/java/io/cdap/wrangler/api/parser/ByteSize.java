package io.cdap.wrangler.api.parser;

public class ByteSize extends Token {
    private long bytes;

    public ByteSize(String value) {
        this.bytes = parseByteSize(value);
    }

    private long parseByteSize(String value) {
        if (value.endsWith("KB")) {
            return Long.parseLong(value.replace("KB", "")) * 1024;
        } else if (value.endsWith("MB")) {
            return Long.parseLong(value.replace("MB", "")) * 1024 * 1024;
        } else if (value.endsWith("GB")) {
            return Long.parseLong(value.replace("GB", "")) * 1024 * 1024 * 1024;
        } else if (value.endsWith("B")) {
            return Long.parseLong(value.replace("B", ""));
        } else {
            throw new IllegalArgumentException("Unknown byte size unit: " + value);
        }
    }

    public long getBytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return bytes + " bytes";
    }
}
