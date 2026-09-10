package com.traintrack.coreapi.enrolment.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.traintrack.coreapi.common.exception.BadRequestException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvParserTest {

    @Test
    void parsesValidRows() {
        List<CsvRow> rows = CsvParser.parse(stream("userId,courseId\nu1,c1\nu2,c2\n"));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).isEqualTo(new CsvRow(1, "u1", "c1"));
        assertThat(rows.get(1)).isEqualTo(new CsvRow(2, "u2", "c2"));
    }

    @Test
    void skipsBlankLines() {
        List<CsvRow> rows = CsvParser.parse(stream("userId,courseId\nu1,c1\n\nu2,c2\n"));

        assertThat(rows).hasSize(2);
    }

    @Test
    void rejectsAMissingOrWrongHeader() {
        assertThatThrownBy(() -> CsvParser.parse(stream("wrong,header\nu1,c1\n"))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> CsvParser.parse(stream(""))).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsARowWithTheWrongNumberOfColumns() {
        assertThatThrownBy(() -> CsvParser.parse(stream("userId,courseId\nu1,c1,extra\n")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsAnEmptyDataSet() {
        assertThatThrownBy(() -> CsvParser.parse(stream("userId,courseId\n"))).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsMoreThanTheMaxRows() {
        StringBuilder csv = new StringBuilder("userId,courseId\n");
        for (int i = 0; i <= CsvParser.MAX_ROWS; i++) {
            csv.append("u").append(i).append(",c").append(i).append('\n');
        }

        assertThatThrownBy(() -> CsvParser.parse(stream(csv.toString()))).isInstanceOf(BadRequestException.class);
    }

    private ByteArrayInputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
