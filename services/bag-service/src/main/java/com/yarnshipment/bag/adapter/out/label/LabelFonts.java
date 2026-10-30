package com.yarnshipment.bag.adapter.out.label;

import com.lowagie.text.pdf.BaseFont;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Etiketin yazı tipleri. Türkçe karakterler (İ, Ş, Ğ) için Windows-1254 kodlu standart PDF yazı tipleri kullanılır;
 * dosyaya gömülmez, her PDF okuyucuda bulunur.
 */
record LabelFonts(BaseFont regular, BaseFont bold, BaseFont mono) {

    private static final String TURKISH = "Cp1254";

    static LabelFonts load() {
        return new LabelFonts(font(BaseFont.HELVETICA), font(BaseFont.HELVETICA_BOLD), font(BaseFont.COURIER_BOLD));
    }

    private static BaseFont font(String name) {
        try {
            return BaseFont.createFont(name, TURKISH, BaseFont.NOT_EMBEDDED);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
