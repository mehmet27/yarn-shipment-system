package com.yarnshipment.common.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventRoutesTest {

    private static final EventsProperties.Route SHIPMENT =
            new EventsProperties.Route("com.yarnshipment.shipment.", "shipment.events.v1", "/s/shipment-service");
    private static final EventsProperties.Route MASTERDATA =
            new EventsProperties.Route("com.yarnshipment.masterdata.", "shipment.events.v1", "/s/shipment-service");
    private static final EventsProperties.Route NARROW =
            new EventsProperties.Route("com.yarnshipment.shipment.special.", "special.events.v1", "/s/special");

    @Test
    @DisplayName("Olay türü en uzun eşleşen önekin konusuna ve kaynağına gider")
    void shouldPickLongestPrefix() {
        EventRoutes routes = new EventRoutes(List.of(SHIPMENT, NARROW, MASTERDATA));

        assertThat(routes.find("com.yarnshipment.shipment.shipment-created.v1")).contains(SHIPMENT);
        assertThat(routes.find("com.yarnshipment.shipment.special.x.v1")).contains(NARROW);
        assertThat(routes.find("com.yarnshipment.masterdata.lot-quality-locked.v1"))
                .contains(MASTERDATA);
    }

    @Test
    @DisplayName("Hiçbir önek uymuyorsa boş döner")
    void shouldReturnEmptyWhenNothingMatches() {
        assertThat(new EventRoutes(List.of(SHIPMENT)).find("com.yarnshipment.evidence.evidence-uploaded.v1"))
                .isEmpty();
    }
}
