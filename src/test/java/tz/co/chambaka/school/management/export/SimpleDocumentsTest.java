package tz.co.chambaka.school.management.export;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleDocumentsTest {

    @Test
    void csvHandlesPlainNullAndQuotedCells() {
        byte[] bytes = SimpleDocuments.csv(
                List.of("Name", "Note"),
                List.of(List.of("Alice", "plain"), java.util.Arrays.asList(null, "a,\"b\"\nline")));

        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(bytes).isNotEmpty();
        assertThat(csv).startsWith("\uFEFFName,Note\n");
        assertThat(csv).contains("\"a,\"\"b\"\"\nline\"");
    }

    @Test
    void pdfEscapesTextAndStopsAfterOnePage() {
        List<String> lines = java.util.stream.IntStream.range(0, 60)
                .mapToObj(i -> i == 0 ? "A (test) \\ path\ncontinued" : "Line " + i)
                .toList();

        byte[] bytes = SimpleDocuments.pdf(null, lines);
        String pdf = new String(bytes, StandardCharsets.ISO_8859_1);
        assertThat(bytes).isNotEmpty();
        assertThat(pdf).startsWith("%PDF-1.4");
        assertThat(pdf).contains("A \\(test\\) \\\\ path continued");
        assertThat(pdf).endsWith("%%EOF");
        assertThat(pdf).doesNotContain("Line 59");
    }
}
