package com.yarnshipment.evidence.adapter.out.storage;

import com.yarnshipment.evidence.domain.model.SignedFileException;
import com.yarnshipment.evidence.domain.model.SignedFileException.Reason;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * {@link FileGrant} değerini HMAC-SHA256 ile imzalayıp {@code <yük>.<imza>} biçiminde bir belirtece çevirir. Belirteç
 * adresin parçası olduğundan oturum gerekmez; kurcalanan ya da süresi dolan belirteç reddedilir.
 */
final class FileGrantSigner {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SEPARATOR = "|";
    private static final int FIELD_COUNT = 5;
    private static final int SIZE_FIELD = 3;
    private static final int EXPIRY_FIELD = 4;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] key;

    FileGrantSigner(String signingKey) {
        this.key = signingKey.getBytes(StandardCharsets.UTF_8);
    }

    String sign(FileGrant grant) {
        String payload = String.join(
                SEPARATOR,
                grant.operation().name(),
                grant.storageKey(),
                grant.mimeType(),
                Long.toString(grant.sizeBytes()),
                Long.toString(grant.expiresAt().getEpochSecond()));
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        return ENCODER.encodeToString(body) + "." + ENCODER.encodeToString(mac(body));
    }

    /** İmzayı ve süreyi doğrular; sonra izni döner. */
    FileGrant verify(String token, Instant now) {
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            throw new SignedFileException(Reason.INVALID_LINK);
        }
        byte[] body;
        byte[] signature;
        try {
            body = DECODER.decode(token.substring(0, dot));
            signature = DECODER.decode(token.substring(dot + 1));
        } catch (IllegalArgumentException exception) {
            throw new SignedFileException(Reason.INVALID_LINK);
        }
        if (!MessageDigest.isEqual(mac(body), signature)) {
            throw new SignedFileException(Reason.INVALID_LINK);
        }
        FileGrant grant = parse(new String(body, StandardCharsets.UTF_8));
        if (!now.isBefore(grant.expiresAt())) {
            throw new SignedFileException(Reason.EXPIRED_LINK);
        }
        return grant;
    }

    private static FileGrant parse(String payload) {
        String[] fields = payload.split("\\" + SEPARATOR, -1);
        if (fields.length != FIELD_COUNT) {
            throw new SignedFileException(Reason.INVALID_LINK);
        }
        try {
            return new FileGrant(
                    FileGrant.Operation.valueOf(fields[0]),
                    fields[1],
                    fields[2],
                    Long.parseLong(fields[SIZE_FIELD]),
                    Instant.ofEpochSecond(Long.parseLong(fields[EXPIRY_FIELD])));
        } catch (IllegalArgumentException exception) {
            throw new SignedFileException(Reason.INVALID_LINK);
        }
    }

    private byte[] mac(byte[] body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return mac.doFinal(body);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
