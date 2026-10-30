package com.yarnshipment.evidence.adapter.out.storage;

import com.yarnshipment.evidence.domain.model.StoredObject;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Nesne akışını bir kez okuyup boyutunu ve SHA-256'sını hesaplar (SEC-09). */
final class ObjectDigest {

    private static final int BUFFER_SIZE = 8192;

    private ObjectDigest() {}

    static StoredObject measure(InputStream stream, String mimeType) throws IOException {
        MessageDigest digest = sha256();
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int read;
        while ((read = stream.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
            total += read;
        }
        return new StoredObject(total, HexFormat.of().formatHex(digest.digest()), mimeType);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
