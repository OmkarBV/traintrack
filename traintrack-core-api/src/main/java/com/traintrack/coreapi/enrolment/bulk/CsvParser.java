package com.traintrack.coreapi.enrolment.bulk;

import com.traintrack.coreapi.common.exception.BadRequestException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Deliberately hand-rolled rather than a CSV library: the format is fixed at
 * two plain columns with no quoting/escaping needs, and pulling in a
 * dependency for that would be more code (and more surface area) than this.
 */
final class CsvParser {

    static final int MAX_ROWS = 5000;
    private static final String EXPECTED_HEADER = "userid,courseid";

    private CsvParser() {
    }

    static List<CsvRow> parse(InputStream input) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || !header.strip().equalsIgnoreCase(EXPECTED_HEADER)) {
                throw new BadRequestException("CSV must start with the header 'userId,courseId'");
            }

            List<CsvRow> rows = new ArrayList<>();
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length != 2) {
                    throw new BadRequestException("Row " + rowNumber + " does not have exactly 2 columns");
                }
                rows.add(new CsvRow(rowNumber, parts[0].strip(), parts[1].strip()));
                rowNumber++;
                if (rows.size() > MAX_ROWS) {
                    throw new BadRequestException("CSV exceeds the maximum of " + MAX_ROWS + " rows");
                }
            }

            if (rows.isEmpty()) {
                throw new BadRequestException("CSV contains no data rows");
            }
            return rows;
        } catch (IOException e) {
            throw new BadRequestException("Failed to read the uploaded file");
        }
    }
}
