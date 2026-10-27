package com.yarnshipment.evidence.adapter.out.ticket;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import javax.imageio.ImageIO;

/**
 * Fotoğrafı modele gidecek boyuta indirir. Uzun kenar 1568 pikseli geçmez; daha büyüğü okumayı iyileştirmez, yalnızca
 * maliyeti ve süreyi artırır. Çıktı her zaman JPEG'dir.
 */
final class TicketImages {

    static final int MAX_EDGE_PX = 1568;

    private TicketImages() {}

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /** Okunamayan biçimde (ör. HEIC) boş döner. */
    static Optional<byte[]> toModelJpeg(byte[] content) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(content));
            if (source == null) {
                return Optional.empty();
            }
            double scale = Math.min(1.0, (double) MAX_EDGE_PX / Math.max(source.getWidth(), source.getHeight()));
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            BufferedImage target = resize(source, width, height);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(target, "jpg", out);
            return Optional.of(out.toByteArray());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
