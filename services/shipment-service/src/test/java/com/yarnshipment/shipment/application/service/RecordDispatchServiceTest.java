package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestDispatches;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.RecordDispatchCommand;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.out.ShipmentBagsPort;
import com.yarnshipment.shipment.domain.model.DispatchBag;
import com.yarnshipment.shipment.domain.model.DispatchMeasurement;
import com.yarnshipment.shipment.domain.model.InvalidDispatchException;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ScanMethod;
import com.yarnshipment.shipment.domain.model.ShipmentSettings;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.packaging.PackageType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecordDispatchServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T09:00:00Z");
    private static final UUID KEY = UUID.fromString("0192f0a0-0000-7000-8000-0000000000cc");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final RecordingDispatchCommandPort commandPort = new RecordingDispatchCommandPort();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final FixedBagCountPort bagsPort =
            new FixedBagCountPort(TestDispatches.measurement().bagCount());
    private final StubSettingsPort settings = new StubSettingsPort();
    private boolean hasTareStandard;
    private Map<PackageType, Integer> lastCounts = Map.of();
    private final RecordDispatchService service = new RecordDispatchService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
            commandPort,
            bagsPort,
            (siteId, counts) -> {
                lastCounts = counts;
                return hasTareStandard ? TestDispatches.tareBasis(total(counts)) : null;
            },
            settings,
            clock);
    private final Actor warehouse = TestTokens.actor("warehouse");

    private RecordDispatchCommand command(Actor actor) {
        return new RecordDispatchCommand(actor, TestShipments.SHIPMENT_ID, KEY, TestDispatches.measurement());
    }

    @Test
    @DisplayName("Depo çalışanı kendi lokasyonundaki taslak sevkiyatın çıkışını sunucu saatiyle kaydeder")
    void shouldRecordDispatchForWarehouse() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));

        service.recordDispatch(command(warehouse));

        assertThat(commandPort.recordings()).singleElement().satisfies(recording -> {
            assertThat(recording.recordedAt()).isEqualTo(NOW);
            assertThat(recording.scaleTicketNo()).isEqualTo("KF-1");
        });
    }

    @Test
    @DisplayName("Tartılan net beklenenden toleransın ötesinde saparsa kayıt uyuşmazlık olarak işaretlenir")
    void shouldFlagMismatchOutsideTolerance() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));
        settings.save(new ShipmentSettings(ShipmentSettings.DEFAULTS.defaultFireRate(), new BigDecimal("0.400")), UUID.randomUUID());

        service.recordDispatch(new RecordDispatchCommand(
                warehouse,
                TestShipments.SHIPMENT_ID,
                KEY,
                new DispatchMeasurement(new BigDecimal("2001.000"), new BigDecimal("18.000"), 40, "KF-1", "Stok yetmedi")));

        assertThat(commandPort.recordings()).singleElement().satisfies(recording -> {
            assertThat(recording.toStatus()).isEqualTo(ShipmentStatus.WEIGHT_MISMATCH);
            assertThat(Objects.requireNonNull(recording.mismatch()).diffKg()).isEqualByComparingTo("0.500");
            assertThat(Objects.requireNonNull(recording.mismatch()).shortfallReason()).isEqualTo("Stok yetmedi");
        });
    }

    @Test
    @DisplayName("Fark toleransın içindeyse sevkiyat normal şekilde yola çıkar")
    void shouldAcceptDifferenceWithinTolerance() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));
        settings.save(new ShipmentSettings(ShipmentSettings.DEFAULTS.defaultFireRate(), new BigDecimal("0.500")), UUID.randomUUID());

        service.recordDispatch(new RecordDispatchCommand(
                warehouse,
                TestShipments.SHIPMENT_ID,
                KEY,
                new DispatchMeasurement(new BigDecimal("2001.000"), new BigDecimal("18.000"), 40, "KF-1")));

        assertThat(commandPort.recordings()).singleElement().satisfies(recording -> {
            assertThat(recording.toStatus()).isEqualTo(ShipmentStatus.AWAITING_DELIVERY);
            assertThat(recording.mismatch()).isNull();
        });
    }

    @Test
    @DisplayName("Aynı çıkış kaydının tekrarı yeni kayıt oluşturmaz ve hata vermez")
    void shouldIgnoreRepeatedDispatch() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        commandPort.addExisting(TestShipments.shipment(ShipmentStatus.DRAFT)
                .recordDispatch(command(warehouse).measurement(), NOW));

        service.recordDispatch(command(warehouse));

        assertThat(commandPort.recordings()).hasSize(1);
    }

    @Test
    @DisplayName("Kaydı olmayan ve taslak olmayan sevkiyatta 409 hatası verilir")
    void shouldRejectDispatchOutsideDraft() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));

        assertThatThrownBy(() -> service.recordDispatch(command(warehouse)))
                .isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(commandPort.recordings()).isEmpty();
    }

    @Test
    @DisplayName("Başka lokasyonun deposu kapsam dışıdır")
    void shouldRejectWarehouseOfOtherSite() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));
        Actor otherSite = TestTokens.CONVERTER.toActor(TestTokens.token("warehouse")
                .claim("site_ids", List.of(UUID.randomUUID().toString()))
                .build());

        assertThatThrownBy(() -> service.recordDispatch(command(otherSite)))
                .isInstanceOf(OutOfScopeAccessException.class);
        assertThat(commandPort.recordings()).isEmpty();
    }

    @Test
    @DisplayName("Olmayan sevkiyat 404 verir")
    void shouldRejectUnknownShipment() {
        assertThatThrownBy(() -> service.recordDispatch(command(warehouse)))
                .isInstanceOf(ShipmentNotFoundException.class);
    }

    @Test
    @DisplayName("Tartıdaki çuval sayısı atanan çuval sayısından farklıysa kayıt reddedilir")
    void shouldRejectBagCountMismatch() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));
        bagsPort.assigned = TestDispatches.measurement().bagCount() - 1;

        assertThatThrownBy(() -> service.recordDispatch(command(warehouse)))
                .isInstanceOf(InvalidDispatchException.class)
                .extracting(e -> ((InvalidDispatchException) e).field())
                .isEqualTo("bagCount");
        assertThat(commandPort.recordings()).isEmpty();
    }

    @Test
    @DisplayName("Dara standardı sevkiyattaki ambalajların türüne göre sorulur")
    void shouldAskTareBasisPerPackageType() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));
        int total = TestDispatches.measurement().bagCount();
        bagsPort.boxes = 1;

        service.recordDispatch(command(warehouse));

        assertThat(lastCounts).containsEntry(PackageType.BOX, 1).containsEntry(PackageType.BAG, total - 1);
    }

    private static int total(Map<PackageType, Integer> counts) {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Test
    @DisplayName("Lokasyonun çuval standardı varsa çıkış tartımına beklenen dara ve sapma eklenir")
    void shouldAttachTareBasis() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));
        // çuval başı 0,5 kg × 40 çuval = 20 kg beklenen; girilen 18 kg → %-10 sapma, %5 sınırın dışında
        hasTareStandard = true;

        service.recordDispatch(command(warehouse));

        assertThat(commandPort.recordings()).singleElement().satisfies(recording -> {
            assertThat(recording.weighing().tareDeviationPct()).isEqualByComparingTo("-10.00");
            assertThat(recording.weighing().tareAlert()).isTrue();
        });
    }

    @Test
    @DisplayName("Standart tanımsızsa tartım sapma olmadan kaydedilir")
    void shouldRecordWithoutTareBasis() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));

        service.recordDispatch(command(warehouse));

        assertThat(commandPort.recordings()).singleElement().satisfies(recording -> {
            assertThat(recording.weighing().tareDeviationPct()).isNull();
            assertThat(recording.weighing().tareAlert()).isFalse();
        });
    }

    private static final class FixedBagCountPort implements ShipmentBagsPort {
        private int assigned;
        private int boxes;

        FixedBagCountPort(int assigned) {
            this.assigned = assigned;
        }

        @Override
        public List<DispatchBag> findDispatchBags(UUID shipmentId) {
            List<DispatchBag> bags = new ArrayList<>();
            for (int index = 0; index < assigned; index++) {
                PackageType type = index < boxes ? PackageType.BOX : PackageType.BAG;
                bags.add(new DispatchBag(UUID.randomUUID(), "K", type, "L", "A", ScanMethod.SCAN, NOW));
            }
            return bags;
        }
    }
}
