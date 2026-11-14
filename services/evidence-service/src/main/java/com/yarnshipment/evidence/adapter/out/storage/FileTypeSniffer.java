package com.yarnshipment.evidence.adapter.out.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

/** Dosya türünü istemcinin dediğine bakmadan ilk baytlarından çıkarır; izinli türlerin dışı {@code UNKNOWN} olur. */
final class FileTypeSniffer {

    static final String UNKNOWN = "application/octet-stream";

    private static final int HEADER_LENGTH = 12;
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final int BOX_OFFSET = 4;
    private static final int BRAND_OFFSET = 8;
    private static final int BRAND_LENGTH = 4;
    private static final Set<String> HEIC_BRANDS =
            Set.of("heic", "heix", "heim", "heis", "hevc", "hevx", "hevm", "hevs", "mif1", "msf1");

    private FileTypeSniffer() {}

    static String sniff(Path file) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            return sniff(stream.readNBytes(HEADER_LENGTH));
        }
    }

    static String sniff(byte[] header) {
        String type = UNKNOWN;
        if (startsWith(header, JPEG)) {
            type = "image/jpeg";
        } else if (startsWith(header, PNG)) {
            type = "image/png";
        } else if (startsWith(header, PDF)) {
            type = "application/pdf";
        } else if (isHeic(header)) {
            type = "image/heic";
        }
        return type;
    }

    private static boolean isHeic(byte[] header) {
        if (header.length < HEADER_LENGTH) {
            return false;
        }
        String box = new String(header, BOX_OFFSET, BRAND_LENGTH, StandardCharsets.US_ASCII);
        String brand = new String(header, BRAND_OFFSET, BRAND_LENGTH, StandardCharsets.US_ASCII);
        return "ftyp".equals(box) && HEIC_BRANDS.contains(brand);
    }

    private static boolean startsWith(byte[] header, byte[] prefix) {
        return header.length >= prefix.length && Arrays.equals(header, 0, prefix.length, prefix, 0, prefix.length);
    }
}
