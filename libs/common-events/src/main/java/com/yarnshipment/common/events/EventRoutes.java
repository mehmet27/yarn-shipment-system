package com.yarnshipment.common.events;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Olay türünden modülün konusunu ve kaynağını bulur. */
public final class EventRoutes {

    private final List<EventsProperties.Route> routes;

    public EventRoutes(List<EventsProperties.Route> routes) {
        this.routes = routes.stream()
                .sorted(Comparator.comparingInt(
                                (EventsProperties.Route r) -> r.typePrefix().length())
                        .reversed())
                .toList();
    }

    /** Tek modüllük yönlendirme; testlerde ve tek modüllü kullanımda kolaylık. */
    public static EventRoutes of(String typePrefix, String topic, String source) {
        return new EventRoutes(List.of(new EventsProperties.Route(typePrefix, topic, source)));
    }

    /** Türü karşılayan en uzun önekli yönlendirme; hiçbiri uymazsa boş. */
    public Optional<EventsProperties.Route> find(String eventType) {
        return routes.stream().filter(r -> eventType.startsWith(r.typePrefix())).findFirst();
    }
}
