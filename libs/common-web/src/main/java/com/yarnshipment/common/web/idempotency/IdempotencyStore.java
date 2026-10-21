package com.yarnshipment.common.web.idempotency;

/** Idempotency-Key kayıtları. Anahtar kullanıcıya özeldir: aynı değer başka kullanıcıyla çakışmaz. */
public interface IdempotencyStore {

    /** Anahtarı sahiplenir; zaten varsa kaydın durumuna göre tekrar, çakışma veya devam eden işlem döner. */
    Claim claim(String actor, String key, String fingerprint);

    /** İstek başarıyla bitti; yanıt, aynı anahtarla gelen tekrarlar için saklanır. */
    void complete(String actor, String key, StoredResponse response);

    /** İstek başarısız oldu; anahtar bırakılır, istemci aynı anahtarla yeniden deneyebilir. */
    void release(String actor, String key);
}
