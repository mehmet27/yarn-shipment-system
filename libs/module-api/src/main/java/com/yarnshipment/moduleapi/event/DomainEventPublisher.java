package com.yarnshipment.moduleapi.event;

/**
 * Olay yayın portu. Üreten modül iş satırını yazdığı transaction içinde çağırır; olay o transaction ile birlikte
 * kalıcılaşır (outbox) ya da o transaction'a bağlı olarak tüketicilere iletilir (süreç içi).
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
