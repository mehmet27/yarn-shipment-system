package com.yarnshipment.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import com.yarnshipment.reporting.application.port.in.ApplyReadModelChangeCommand;
import com.yarnshipment.reporting.application.port.out.ProcessedEventPort;
import com.yarnshipment.reporting.application.port.out.ReadModelPort;
import com.yarnshipment.reporting.domain.model.ReadModelChange;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.invocation.InvocationOnMock;

/** Her değişiklik türü, sevkiyat okuma modelinde varsa hatasız uygulanır. */
class AllChangesTest {

    private static final UUID SHIPMENT = UUID.randomUUID();

    private final ProcessedEventPort processed = (eventId, eventType) -> true;
    private final ReadModelPort readModel = mock(ReadModelPort.class, AllChangesTest::answer);
    private final ApplyReadModelChangeService service = new ApplyReadModelChangeService(processed, readModel);

    /** Boolean dönenler "sevkiyat var" der, geri kalan boş (void). */
    private static @Nullable Object answer(InvocationOnMock invocation) {
        return invocation.getMethod().getReturnType() == boolean.class ? Boolean.TRUE : null;
    }

    static Stream<ReadModelChange> changes() {
        return AllChanges.every(SHIPMENT).stream();
    }

    @ParameterizedTest
    @MethodSource("changes")
    @DisplayName("Değişiklik türü hatasız uygulanır")
    void shouldApplyChange(ReadModelChange change) {
        assertThatCode(() -> service.applyChange(new ApplyReadModelChangeCommand(UUID.randomUUID(), "t", change)))
                .doesNotThrowAnyException();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Liste her türü kapsar")
    void shouldCoverEveryType() {
        List<ReadModelChange> all = AllChanges.every(SHIPMENT);
        org.assertj.core.api.Assertions.assertThat(all).hasSize(ReadModelChange.class.getPermittedSubclasses().length);
    }
}
