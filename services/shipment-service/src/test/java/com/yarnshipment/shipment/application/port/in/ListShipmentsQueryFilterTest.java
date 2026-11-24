package com.yarnshipment.shipment.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.application.port.in.ListShipmentsQuery.Filter;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListShipmentsQueryFilterTest {

    @Test
    @DisplayName("Arama metninin başındaki ve sonundaki boşluk kırpılır")
    void shouldStripSearchText() {
        Filter filter = new Filter(Set.of(), "  34 ABC ", null, null);

        assertThat(filter.search()).isEqualTo("34 ABC");
    }

    @Test
    @DisplayName("Boş arama metni süzgeç sayılmaz")
    void shouldTreatBlankSearchAsNoFilter() {
        assertThat(new Filter(Set.of(), "   ", null, null)).isEqualTo(Filter.NONE);
    }

    @Test
    @DisplayName("Yalnızca duruma göre süzgeç diğer alanları boş bırakır")
    void shouldBuildStatusOnlyFilter() {
        Filter filter = Filter.byStatus(Set.of(ShipmentStatus.DRAFT));

        assertThat(filter).isEqualTo(new Filter(Set.of(ShipmentStatus.DRAFT), null, null, null));
    }
}
