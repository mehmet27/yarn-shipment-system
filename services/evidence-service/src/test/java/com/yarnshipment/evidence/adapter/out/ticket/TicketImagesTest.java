package com.yarnshipment.evidence.adapter.out.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TicketImagesTest {

    static final int SMALL_WIDTH = 40;
    static final int SMALL_HEIGHT = 80;
    static final byte[] NOT_AN_IMAGE = {0, 1, 2};
    private static final int WIDE = 1000;
    private static final int TALL = 3136;
    private static final int HALF_OF_WIDE = 500;
    private static final int PHOTO_WIDTH = 400;
    private static final int PHOTO_HEIGHT = 300;

    static byte[] png(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", out);
        return out.toByteArray();
    }

    private static BufferedImage decode(byte[] jpeg) throws IOException {
        return Objects.requireNonNull(ImageIO.read(new ByteArrayInputStream(jpeg)));
    }

    @Test
    @DisplayName("Büyük fotoğraf uzun kenarı 1568 piksele indirilir ve JPEG olur; küçük fotoğraf büyütülmez")
    void shouldResizeToModelSize() throws IOException {
        BufferedImage large = decode(TicketImages.toModelJpeg(png(WIDE, TALL)).orElseThrow());
        BufferedImage small =
                decode(TicketImages.toModelJpeg(png(PHOTO_WIDTH, PHOTO_HEIGHT)).orElseThrow());

        assertThat(large.getHeight()).isEqualTo(TicketImages.MAX_EDGE_PX);
        assertThat(large.getWidth()).isEqualTo(HALF_OF_WIDE);
        assertThat(small.getWidth()).isEqualTo(PHOTO_WIDTH);
        assertThat(small.getHeight()).isEqualTo(PHOTO_HEIGHT);
    }

    @Test
    @DisplayName("Çözülemeyen biçim boş döner")
    void shouldSkipUnsupportedFormat() {
        assertThat(TicketImages.toModelJpeg(NOT_AN_IMAGE)).isEmpty();
    }
}
