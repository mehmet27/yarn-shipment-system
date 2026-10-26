package com.yarnshipment.common.outbox;

import java.util.List;

/** Outbox tablosuna erişim; yayıncı yalnızca bu arayüzü bilir. */
public interface OutboxStore {

    /**
     * Gönderilmemiş en eski olayları kilitleyerek okur. Başka bir örneğin kilitlediği satırlar atlanır, böylece
     * birden çok servis örneği aynı olayı iki kez göndermez. Çağıran transaction içinde olmalıdır.
     */
    List<OutboxEvent> lockUnpublished(int limit);

    /** Olayları şimdiki zamanla gönderildi işaretler. */
    void markPublished(List<OutboxEvent> events);
}
