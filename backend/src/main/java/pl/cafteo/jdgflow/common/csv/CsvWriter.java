package pl.cafteo.jdgflow.common.csv;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class CsvWriter {

    private static final String SEPARATOR = ";";
    private static final String EOL = "\r\n";

    private final StringBuilder sb = new StringBuilder();
    private boolean firstColumn = true;

    public CsvWriter row() {
        if (sb.length() > 0) sb.append(EOL);
        firstColumn = true;
        return this;
    }

    public CsvWriter cell(String value) {
        if (!firstColumn) sb.append(SEPARATOR);
        firstColumn = false;
        sb.append(escape(value));
        return this;
    }

    public CsvWriter cell(BigDecimal value) {
        return cell(value == null ? "" : value.toPlainString().replace('.', ','));
    }

    public CsvWriter cell(LocalDate value) {
        return cell(value == null ? "" : value.toString());
    }

    public CsvWriter cell(boolean value) {
        return cell(value ? "tak" : "nie");
    }

    public CsvWriter cell(Enum<?> value) {
        return cell(value == null ? "" : value.name());
    }

    public byte[] toBytesWithBom() {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(body, 0, out, bom.length, body.length);
        return out;
    }

    private static String escape(String value) {
        if (value == null || value.isEmpty()) return "";
        boolean needsQuoting = value.contains(SEPARATOR) || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!needsQuoting) return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
