package com.yarnshipment.bag.adapter.out.label;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.pdf.PdfContentByte;
import java.awt.Color;
import java.util.Map;

/** Bir metnin QR kodu; modüller PDF'e vektör kare olarak çizilir. Hata düzeltme M: yıpranmış etiket de okunur. */
final class QrSymbol {

    private final BitMatrix matrix;

    QrSymbol(String content) {
        try {
            this.matrix = new QRCodeWriter()
                    .encode(
                            content,
                            BarcodeFormat.QR_CODE,
                            0,
                            0,
                            Map.of(
                                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                                    EncodeHintType.MARGIN, 0,
                                    EncodeHintType.CHARACTER_SET, "UTF-8"));
        } catch (WriterException e) {
            throw new IllegalArgumentException("Content cannot be encoded as QR", e);
        }
    }

    /** Sol üst köşesi verilen noktada, {@code size} kenarlı kare olarak çizer. */
    void draw(PdfContentByte canvas, float left, float top, float size) {
        float module = size / matrix.getWidth();
        canvas.saveState();
        canvas.setColorFill(Color.BLACK);
        for (int row = 0; row < matrix.getHeight(); row++) {
            for (int column = 0; column < matrix.getWidth(); column++) {
                if (matrix.get(column, row)) {
                    canvas.rectangle(left + column * module, top - (row + 1) * module, module, module);
                }
            }
        }
        canvas.fill();
        canvas.restoreState();
    }
}
