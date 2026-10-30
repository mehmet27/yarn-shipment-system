package com.yarnshipment.bag.adapter.out.label;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagLabel;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.bag.domain.model.PackageType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PdfLabelRendererTest {

    private static final int HEADER_LENGTH = 5;
    private static final float LABEL_WIDTH_POINTS = 283.46f;
    private static final float TOLERANCE = 0.1f;
    private static final int THIRD_PRINT = 3;

    private final PdfLabelRenderer renderer = new PdfLabelRenderer();

    @Test
    @DisplayName("Her etiket 100 × 70 mm bir sayfadır; anahtar, parti, kalite ve ambalaj okunur yazılır")
    void shouldRenderOnePagePerLabel() throws IOException {
        byte[] pdf = renderer.render(List.of(
                new BagLabel(bag("BAG-7K3M-Q9XD", "A", PackageType.BAG), 1),
                new BagLabel(bag("BAG-2P4R-HJ8W", "B", PackageType.PALLET), 1)));

        assertThat(new String(pdf, 0, HEADER_LENGTH, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(2);
        assertThat(reader.getPageSize(1).getWidth()).isCloseTo(LABEL_WIDTH_POINTS, Offset.offset(TOLERANCE));
        PdfTextExtractor text = new PdfTextExtractor(reader);
        assertThat(text.getTextFromPage(1)).contains("KALİTE A", "BAG-7K3M-Q9XD", "P-2026-0001", "Çuval");
        assertThat(text.getTextFromPage(2)).contains("KALİTE B", "BAG-2P4R-HJ8W", "Palet");
        assertThat(text.getTextFromPage(1)).doesNotContain("YENİDEN BASKI");
    }

    @Test
    @DisplayName("Yeniden baskı etikette baskı numarasıyla yazar")
    void shouldMarkReprint() throws IOException {
        byte[] pdf = renderer.render(List.of(new BagLabel(bag("BAG-7K3M-Q9XD", "C", PackageType.BOBBIN), THIRD_PRINT)));

        String page = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
        assertThat(page).contains("YENİDEN BASKI", "3. baskı", "KALİTE C", "Bobin");
    }

    private static Bag bag(String key, String grade, PackageType packageType) {
        return new Bag(
                UUID.randomUUID(), UUID.randomUUID(), key, packageType, "P-2026-0001", grade, BagStatus.CREATED, null);
    }
}
