package com.yarnshipment.common.events;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Hangi olay türünün hangi modülden çıktığını söyler. Tek uygulamada yayıncı tektir; olayın konusu ve kaynağı
 * (denetim kaydındaki varlık türü ve servis adı) olay türünün öneki üzerinden modül başına verilir.
 *
 * @param routes olay türü önekinden konu ve kaynağa eşleme; en uzun eşleşen önek kazanır
 */
@ConfigurationProperties(prefix = "yarnshipment.events")
public record EventsProperties(List<Route> routes) {

    public EventsProperties {
        routes = routes == null ? List.of() : List.copyOf(routes);
    }

    /**
     * Bir modülün olay yönlendirmesi.
     *
     * @param typePrefix olay türünün başı, ör. {@code com.yarnshipment.shipment.}
     * @param topic olayın konusu, ör. {@code shipment.events.v1}; denetim kaydında varlık türünü belirler (EVT-04)
     * @param source CloudEvents {@code source}, ör. {@code /yarn-shipment/shipment-service} (EVT-02)
     */
    public record Route(String typePrefix, String topic, String source) {

        public Route {
            Objects.requireNonNull(typePrefix, "typePrefix");
            Objects.requireNonNull(topic, "topic");
            Objects.requireNonNull(source, "source");
        }
    }
}
