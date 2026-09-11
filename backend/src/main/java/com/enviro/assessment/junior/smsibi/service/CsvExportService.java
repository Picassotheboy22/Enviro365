package com.enviro.assessment.junior.smsibi.service;

import com.enviro.assessment.junior.smsibi.dto.WithdrawalResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

/**
 * Renders withdrawal history as a CSV statement.
 *
 * <p>It receives already-filtered rows rather than querying the database itself, so it has a single
 * responsibility (formatting) and can be unit-tested without a database. Apache Commons CSV takes care of
 * quoting values that contain commas, quotes or line breaks, which hand-built string joining gets wrong.
 */
@Service
public class CsvExportService {

    static final String[] HEADERS = {
        "Notice ID", "Date", "Investor", "Product", "Product Type", "Amount", "Balance Before", "Balance After"
    };

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Clock clock;

    public CsvExportService(Clock clock) {
        this.clock = clock;
    }

    public CsvFile export(List<WithdrawalResponse> rows) {
        StringWriter out = new StringWriter();
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(HEADERS).get();

        try (CSVPrinter printer = new CSVPrinter(out, format)) {
            for (WithdrawalResponse row : rows) {
                printer.printRecord(
                        row.id(),
                        row.createdAt().format(DATE_TIME),
                        safeText(row.investorName()),
                        safeText(row.productName()),
                        row.productType(),
                        // toPlainString avoids scientific notation (e.g. 1E+3) that spreadsheets might misread.
                        row.amount().toPlainString(),
                        row.balanceBefore().toPlainString(),
                        row.balanceAfter().toPlainString());
            }
        } catch (IOException e) {
            // A StringWriter never actually throws, but CSVPrinter's API declares IOException.
            throw new UncheckedIOException("Failed to write CSV", e);
        }

        String filename = "withdrawal-statement-" + LocalDate.now(clock) + ".csv";
        return new CsvFile(filename, out.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Guards against "CSV injection": Excel runs a cell starting with =, +, - or @ as a formula. Free-text
     * values are prefixed with an apostrophe so they are always displayed as plain text.
     */
    static String safeText(String value) {
        if (value != null && !value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            return "'" + value;
        }
        return value;
    }

    /** The generated file: a suggested download name plus UTF-8 bytes. */
    public record CsvFile(String filename, byte[] content) {}
}
