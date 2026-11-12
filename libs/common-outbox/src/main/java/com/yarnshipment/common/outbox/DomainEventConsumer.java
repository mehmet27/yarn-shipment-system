package com.yarnshipment.common.outbox;

/**
 * Taşıyıcıdan bağımsız olay tüketicisi. Kafka dinleyicisi de süreç içi dağıtıcı da aynı {@link #consume} metodunu
 * çağırır. Tüketici hangi transaction semantiğini istediğini {@link #delivery()} ile yazılı olarak bildirir.
 */
public interface DomainEventConsumer {

    /** Süreç içi teslimde tüketicinin üreten işlemle ilişkisi. Kafka'da her olay kendi transaction'ında işlenir. */
    enum Delivery {
        /**
         * Üreten işlemin commit'inden hemen önce, aynı transaction'da çalışır; tüketici hatası iş işlemini geri alır.
         * Olay kaybı kabul edilemeyen tüketiciler (denetim, kayıt, defter) için.
         */
        BEFORE_COMMIT,
        /**
         * Commit'ten sonra, kendi yeni transaction'ında çalışır; hata iş işlemini etkilemez, loglanır ve olay yeniden
         * üretilebilir. Okuma modelleri gibi kaybı telafi edilebilen tüketiciler için.
         */
        AFTER_COMMIT
    }

    Delivery delivery();

    /** Olayı işler; bu serviste kullanılmayan olay türlerini sessizce yok sayar. */
    void consume(IncomingEvent event);
}
