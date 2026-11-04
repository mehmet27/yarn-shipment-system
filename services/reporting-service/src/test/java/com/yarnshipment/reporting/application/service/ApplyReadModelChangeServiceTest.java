package com.yarnshipment.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.reporting.application.port.in.ApplyReadModelChangeCommand;
import com.yarnshipment.reporting.application.port.out.ProcessedEventPort;
import com.yarnshipment.reporting.application.port.out.ReadModelPort;
import com.yarnshipment.reporting.application.port.out.ReadModelPort.PairComparison;
import com.yarnshipment.reporting.domain.model.ReadModelChange;
import com.yarnshipment.reporting.domain.model.ShipmentNotInReadModelException;
import com.yarnshipment.reporting.domain.model.ShipmentParties;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Olayların okuma modeline uygulanması. */
class ApplyReadModelChangeServiceTest {

    private static final UUID SHIPMENT = UUID.randomUUID();
    private static final UUID SITE = UUID.randomUUID();
    private static final UUID DRIVER = UUID.randomUUID();
    private static final UUID SUBCONTRACTOR = UUID.randomUUID();
    private static final Instant COMPARED_AT = Instant.parse("2026-11-03T21:30:00Z");

    private final ProcessedEventPort processed = mock(ProcessedEventPort.class);
    private final ReadModelPort readModel = mock(ReadModelPort.class);
    private final ApplyReadModelChangeService service =
            new ApplyReadModelChangeService(processed, readModel, new ComparisonRecorder(readModel));

    @Test
    @DisplayName("Daha önce işlenmiş olay okuma modeline tekrar uygulanmaz")
    void shouldSkipDuplicateEvent() {
        when(processed.markProcessed(any(), any())).thenReturn(false);

        service.applyChange(command(new ReadModelChange.ShipmentCancelled(SHIPMENT)));

        verify(readModel, never()).saveCancelled(any());
    }

    @Test
    @DisplayName("Sevkiyat oluşturma olayı satırı ekler")
    void shouldInsertCreatedShipment() {
        when(processed.markProcessed(any(), any())).thenReturn(true);
        var created = TestChanges.created(SHIPMENT, SITE, SUBCONTRACTOR);

        service.applyChange(command(created));

        verify(readModel).insertShipment(created);
    }

    @Test
    @DisplayName("Okuma modelinde olmayan sevkiyatın olayı hata verir ki tekrar denensin")
    void shouldFailWhenShipmentMissing() {
        when(processed.markProcessed(any(), any())).thenReturn(true);
        when(readModel.saveCompleted(any())).thenReturn(false);
        var completed = new ReadModelChange.ShipmentCompleted(SHIPMENT, COMPARED_AT);

        assertThatThrownBy(() -> service.applyChange(command(completed)))
                .isInstanceOf(ShipmentNotInReadModelException.class);
    }

    @Test
    @DisplayName("Karşılaştırma şoför–fasoncu sayacına Istanbul gününe göre ve mutlak farkla eklenir")
    void shouldAddPairComparison() {
        when(processed.markProcessed(any(), any())).thenReturn(true);
        when(readModel.findParties(SHIPMENT)).thenReturn(Optional.of(new ShipmentParties(SITE, DRIVER, SUBCONTRACTOR)));
        var compared = TestChanges.comparedRed(SHIPMENT, COMPARED_AT);

        service.applyChange(command(compared));

        ArgumentCaptor<PairComparison> captor = ArgumentCaptor.forClass(PairComparison.class);
        verify(readModel).addPairComparison(captor.capture());
        assertThat(captor.getValue().day()).isEqualTo(LocalDate.parse("2026-11-04"));
        assertThat(captor.getValue().absoluteDiffKg()).isEqualByComparingTo("40.000");
        assertThat(captor.getValue().discrepancyCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Şoförü bilinmeyen sevkiyat eşleşme sayacına eklenmez")
    void shouldSkipPairWithoutDriver() {
        when(processed.markProcessed(any(), any())).thenReturn(true);
        when(readModel.findParties(SHIPMENT)).thenReturn(Optional.of(new ShipmentParties(SITE, null, SUBCONTRACTOR)));
        var compared = TestChanges.comparedGreen(SHIPMENT, COMPARED_AT);

        service.applyChange(command(compared));

        verify(readModel).saveComparison(compared);
        verify(readModel, never()).addPairComparison(any());
    }

    @Test
    @DisplayName("Karşılaştırması gelen sevkiyat okuma modelinde yoksa hata verir")
    void shouldFailComparisonWhenShipmentMissing() {
        when(processed.markProcessed(any(), any())).thenReturn(true);
        when(readModel.findParties(SHIPMENT)).thenReturn(Optional.empty());
        var compared = TestChanges.comparedGreen(SHIPMENT, COMPARED_AT);

        assertThatThrownBy(() -> service.applyChange(command(compared)))
                .isInstanceOf(ShipmentNotInReadModelException.class);
    }

    private static ApplyReadModelChangeCommand command(ReadModelChange change) {
        return new ApplyReadModelChangeCommand(UUID.randomUUID(), "test.event", change);
    }
}
