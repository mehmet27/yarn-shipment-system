package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.domain.stock.WorkArea;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkAreaServiceTest {

    private Optional<WorkArea> assigned = Optional.empty();
    private final WorkAreaService service = new WorkAreaService(userId -> assigned);

    @Test
    @DisplayName("Fasoncu kalite merkez kumaşına erişemez, kendi alanına erişir")
    void shouldBlockOtherAreaForQuality() {
        assigned = Optional.of(WorkArea.SUBCONTRACTOR);
        Actor quality = TestTokens.actor("quality");

        assertThatThrownBy(() -> service.requireAccess(quality, WorkArea.CENTRAL))
                .isInstanceOf(OutOfScopeAccessException.class);
        assertThatCode(() -> service.requireAccess(quality, WorkArea.SUBCONTRACTOR))
                .doesNotThrowAnyException();
        assertThat(service.restrictedTo(quality)).contains(WorkArea.SUBCONTRACTOR);
    }

    @Test
    @DisplayName("Alanı atanmamış kalite, yönetici ve diğer roller kısıtlanmaz")
    void shouldNotRestrictOthers() {
        assigned = Optional.of(WorkArea.CENTRAL);

        assertThat(service.restrictedTo(TestTokens.actor("admin", "quality"))).isEmpty();
        assertThat(service.restrictedTo(TestTokens.actor("manager"))).isEmpty();
        assigned = Optional.empty();
        assertThat(service.restrictedTo(TestTokens.actor("quality"))).isEmpty();
        assertThatCode(() -> service.requireAccess(TestTokens.actor("quality"), WorkArea.CENTRAL))
                .doesNotThrowAnyException();
    }
}
