package com.yarnshipment.common.web.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** İsteğin yöntem, adres ve gövdesinden SHA-256 parmak izi: aynı anahtarın aynı isteğe ait olduğunu doğrular. */
final class RequestFingerprint {

    private RequestFingerprint() {}

    static String of(HttpServletRequest request, byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String target = request.getMethod() + ' ' + request.getRequestURI() + '?' + request.getQueryString() + '\n';
            digest.update(target.getBytes(StandardCharsets.UTF_8));
            digest.update(body);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 her Java sürümünde bulunmak zorundadır.
            throw new IllegalStateException(e);
        }
    }
}
