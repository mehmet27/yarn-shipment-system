package com.yarnshipment.moduleapi.event;

/**
 * Olay yayın portu. Üreten modül iş satırını yazdığı transaction içinde çağırır; olay o transaction ile birlikte
 * o transaction'a bağlı olarak tüketicilere süreç içinde iletilir.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
