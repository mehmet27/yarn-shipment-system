package com.yarnshipment.common.web.idempotency;

/** Bir anahtarı sahiplenme girişiminin sonucu. */
public sealed interface Claim {

    /** Anahtar bu istek için ayrıldı; istek çalıştırılır. */
    record Acquired() implements Claim {}

    /** Aynı anahtar ve aynı içerikle önceki istek tamamlanmış; yanıtı tekrar döner. */
    record Completed(StoredResponse response) implements Claim {}

    /** Aynı anahtarlı istek hâlâ işleniyor. */
    record InProgress() implements Claim {}

    /** Anahtar farklı içerikli bir istekle kullanılmış. */
    record Mismatch() implements Claim {}
}
