package com.traintrack.coreapi.certification.storage;

import com.traintrack.coreapi.domain.Certification;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/**
 * Renders a single-page certificate PDF straight from a persisted
 * {@link Certification} — no template engine or external renderer, since the
 * layout is fixed and simple enough that PDFBox's own content-stream API is
 * less machinery than pulling in one.
 */
@Component
public class CertificatePdfGenerator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy").withZone(ZoneOffset.UTC);
    private static final PDType1Font HELVETICA = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font HELVETICA_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] generate(Certification certification) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float centerY = page.getMediaBox().getHeight() / 2;

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeCentered(
                        stream, pageWidth, centerY + 140, "Certificate of Completion",
                        HELVETICA_BOLD, 24);
                writeCentered(stream, pageWidth, centerY + 90, "This certifies that", HELVETICA, 12);
                writeCentered(
                        stream,
                        pageWidth,
                        centerY + 60,
                        certification.getUser().getFullName(),
                        HELVETICA_BOLD,
                        18);
                writeCentered(
                        stream, pageWidth, centerY + 20, "has successfully completed", HELVETICA, 12);
                writeCentered(
                        stream,
                        pageWidth,
                        centerY - 10,
                        certification.getCourse().getTitle(),
                        HELVETICA_BOLD,
                        18);
                writeCentered(
                        stream,
                        pageWidth,
                        centerY - 60,
                        "Issued " + DATE_FORMAT.format(certification.getIssuedAt())
                                + "  ·  Valid until " + DATE_FORMAT.format(certification.getExpiresAt()),
                        HELVETICA,
                        11);
                writeCentered(
                        stream,
                        pageWidth,
                        centerY - 100,
                        "Certificate ID: " + certification.getId(),
                        HELVETICA,
                        9);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate certificate PDF for " + certification.getId(), e);
        }
    }

    private void writeCentered(
            PDPageContentStream stream, float pageWidth, float y, String text, PDType1Font font, int fontSize)
            throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float x = (pageWidth - textWidth) / 2;
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }
}
