package com.yarnshipment.shipment.masterdata.application.port.out;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Kullanıcıya SMS'le giden tek kullanımlık kodun saklanan hali. Kodun kendisi saklanmaz, yalnızca tuzlu özeti.
 *
 * @param userId kodun sahibi; kullanıcı başına tek geçerli kod vardır
 * @param codeHash tuzlu özet
 * @param salt rastgele tuz
 * @param issuedAt üretildiği an
 * @param expiresAt geçerlilik sonu
 * @param attempts yanlış deneme sayısı
 */
public record ResetCode(UUID userId, String codeHash, String salt, Instant issuedAt, Instant expiresAt, int attempts) {

    public ResetCode {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(codeHash, "codeHash");
        Objects.requireNonNull(salt, "salt");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    @Override
    public String toString() {
        return "ResetCode[userId=" + userId + ", expiresAt=" + expiresAt + "]";
    }
}
