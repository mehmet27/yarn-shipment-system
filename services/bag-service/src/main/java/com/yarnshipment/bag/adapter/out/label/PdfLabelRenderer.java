package com.yarnshipment.bag.adapter.out.label;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Utilities;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import com.yarnshipment.bag.application.port.out.LabelRenderer;
import com.yarnshipment.bag.domain.model.BagLabel;
import com.yarnshipment.bag.domain.model.PackageType;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 100 × 70 mm etiket, her çuval bir sayfa: kalite bandı, QR kod, okunur Unique Key, parti ve ambalaj. Kalite renkle
 * ayrılır (A yeşil, B turuncu); tek renk yazıcıda da ayırt edilsin diye A bandı dolu, diğer kaliteler çerçeveli basılır
 * (rapor Bölüm 10, "etiketlerde kalite ayrımı görsel olarak da yapılmalıdır"). Yeniden baskı etikette yazar.
 */
@Component
final class PdfLabelRenderer implements LabelRenderer {

    private static final float WIDTH = Utilities.millimetersToPoints(100);
    private static final float HEIGHT = Utilities.millimetersToPoints(70);
    private static final float MARGIN = Utilities.millimetersToPoints(4);
    private static final float GAP = Utilities.millimetersToPoints(4);
    private static final float BAND_HEIGHT = Utilities.millimetersToPoints(11);
    private static final float BAND_BOTTOM = HEIGHT - MARGIN - BAND_HEIGHT;
    private static final float BAND_BORDER = 2.5f;
    private static final float QR_SIZE = Utilities.millimetersToPoints(40);
    private static final float CONTENT_TOP = BAND_BOTTOM - GAP;
    private static final float DETAILS_LEFT = MARGIN + QR_SIZE + GAP;

    private static final float GRADE_SIZE = 20;
    private static final float CAPTION_SIZE = 7;
    private static final float KEY_SIZE = 13;
    private static final float VALUE_SIZE = 12;
    private static final float NOTE_SIZE = 10;
    private static final float FOOTER_KEY_SIZE = 11;
    private static final float LINE_SPACING = 14;
    /** Yazı tabanı ile bant ortası arasındaki fark; büyük harfler bantta ortalı görünür. */
    private static final float GRADE_BASELINE_OFFSET = 7;

    private static final String FILLED_GRADE = "A";
    private static final Map<String, Color> GRADE_COLORS =
            Map.of("A", new Color(0x1B, 0x5E, 0x20), "B", new Color(0xE6, 0x51, 0x00));
    private static final Color MUTED = new Color(0x42, 0x42, 0x42);

    private final LabelFonts fonts = LabelFonts.load();

    @Override
    public byte[] render(List<BagLabel> labels) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(new Rectangle(WIDTH, HEIGHT), 0, 0, 0, 0);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.addTitle("Çuval etiketleri");
        document.open();
        for (BagLabel label : labels) {
            document.newPage();
            PdfContentByte canvas = writer.getDirectContent();
            drawQualityBand(canvas, label.bag().qualityGradeCode());
            new QrSymbol(label.bag().uniqueKey()).draw(canvas, MARGIN, CONTENT_TOP, QR_SIZE);
            drawDetails(canvas, label);
        }
        document.close();
        return out.toByteArray();
    }

    private void drawQualityBand(PdfContentByte canvas, String grade) {
        Color color = GRADE_COLORS.getOrDefault(grade, MUTED);
        boolean filled = FILLED_GRADE.equals(grade);
        canvas.saveState();
        canvas.setLineWidth(BAND_BORDER);
        canvas.setColorStroke(color);
        canvas.setColorFill(color);
        canvas.rectangle(MARGIN, BAND_BOTTOM, WIDTH - 2 * MARGIN, BAND_HEIGHT);
        if (filled) {
            canvas.fillStroke();
        } else {
            canvas.stroke();
        }
        canvas.restoreState();
        String text = "KALİTE " + grade.toUpperCase(Locale.ROOT);
        Line line = new Line(text, fonts.bold(), GRADE_SIZE, filled ? Color.WHITE : color);
        write(canvas, line, Anchor.centered(WIDTH / 2, BAND_BOTTOM + BAND_HEIGHT / 2 - GRADE_BASELINE_OFFSET));
    }

    private void drawDetails(PdfContentByte canvas, BagLabel label) {
        float baseline = CONTENT_TOP - CAPTION_SIZE;
        baseline = field(canvas, "Unique Key", value(label.bag().uniqueKey(), fonts.mono(), KEY_SIZE), baseline);
        baseline = field(canvas, "Parti", value(label.bag().lotNo(), fonts.bold(), VALUE_SIZE), baseline);
        String packageName = packageName(label.bag().packageType());
        baseline = field(canvas, "Ambalaj", value(packageName, fonts.bold(), VALUE_SIZE), baseline);
        if (label.isReprint()) {
            String note = "YENİDEN BASKI · " + label.printNo() + ". baskı";
            write(canvas, value(note, fonts.bold(), NOTE_SIZE), Anchor.left(DETAILS_LEFT, baseline));
        }
        Line footer = value(label.bag().uniqueKey(), fonts.mono(), FOOTER_KEY_SIZE);
        write(canvas, footer, Anchor.centered(MARGIN + QR_SIZE / 2, MARGIN));
    }

    /** Başlık ve değeri sağ sütuna yazar; bir sonraki alanın taban çizgisini döner. */
    private float field(PdfContentByte canvas, String caption, Line value, float baseline) {
        write(canvas, new Line(caption, fonts.regular(), CAPTION_SIZE, MUTED), Anchor.left(DETAILS_LEFT, baseline));
        float valueBaseline = baseline - value.size() - 1;
        write(canvas, value, Anchor.left(DETAILS_LEFT, valueBaseline));
        return valueBaseline - LINE_SPACING;
    }

    private static String packageName(PackageType packageType) {
        return switch (packageType) {
            case BAG -> "Çuval";
            case BOX -> "Koli";
            case PALLET -> "Palet";
            case BOBBIN -> "Bobin";
        };
    }

    private static Line value(String text, BaseFont font, float size) {
        return new Line(text, font, size, Color.BLACK);
    }

    private static void write(PdfContentByte canvas, Line line, Anchor anchor) {
        canvas.beginText();
        canvas.setFontAndSize(line.font(), line.size());
        canvas.setColorFill(line.color());
        canvas.showTextAligned(anchor.alignment(), line.text(), anchor.horizontal(), anchor.baseline(), 0);
        canvas.endText();
    }

    /** Yazılacak bir satır ve biçimi. */
    private record Line(String text, BaseFont font, float size, Color color) {}

    /** Satırın yeri: hizalama, yatay konum ve taban çizgisi. */
    private record Anchor(int alignment, float horizontal, float baseline) {

        static Anchor left(float horizontal, float baseline) {
            return new Anchor(PdfContentByte.ALIGN_LEFT, horizontal, baseline);
        }

        static Anchor centered(float horizontal, float baseline) {
            return new Anchor(PdfContentByte.ALIGN_CENTER, horizontal, baseline);
        }
    }
}
