package com.yarnshipment.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.app.Factory.Line;
import com.yarnshipment.app.Factory.Party;
import com.yarnshipment.app.Factory.Yarn;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * Üç iplikli bir sevkiyatın baştan sona sayıları: her adımdan sonra fasoncu bakiyesi, fire dosyası, merkez stok ve
 * raporlar elle hesaplanan değerlerle ve birbirleriyle tutmalıdır. Beklenen değerler analiz 8.1 formüllerinden elle
 * hesaplanmıştır; üretim kodundaki formül kopyalanmamıştır.
 */
@FactoryApplicationTest
class ShipmentLifecycleNumbersTest {

    private final Factory factory;

    @Autowired
    ShipmentLifecycleNumbersTest(Factory factory) {
        this.factory = factory;
    }

    @Test
    @DisplayName("Tamamlanan sevkiyat bakiyeye iplik satırları kadar yazılır, merkez stoktan aynı kilo düşer")
    void shouldMoveYarnFromCentreToSubcontractorWhenShipmentCompletes() {
        // given: merkezde A 400, B 200, C 1 kg var; G = 333,333 + 166,667 + 0,001 = 500,001 kg, fire %4
        Party party = factory.newSubcontractor("Örme");
        List<Line> lines = stockedLines();
        Yarn penye = lines.get(0).yarn();

        // when: taslak ve çıkış tartısı; yoldaki iplik bakiyeye ve merkez stoğa yazılmaz
        UUID shipmentId = factory.draftShipment(party, "0.0400", lines);
        factory.dispatch(shipmentId, lines);
        assertThat(factory.balanceOf(party)).containsEntry("TOPLAM", "0.000");
        assertThat(factory.centralStockOf(penye)).isEqualTo("400.000");
        factory.complete(shipmentId);

        // then: bakiye iplik satırları kadar artar; merkez stok aynı kilolar kadar düşer
        assertThat(factory.balanceOf(party))
                .containsEntry(penye.name(), "333.333")
                .containsEntry(lines.get(1).yarn().name(), "166.667")
                .containsEntry(lines.get(2).yarn().name(), "0.001")
                .containsEntry("TOPLAM", "500.001");
        assertThat(factory.centralStockOf(penye)).isEqualTo("66.667");
        assertThat(factory.centralStockOf(lines.get(1).yarn())).isEqualTo("33.333");
        assertThat(factory.centralStockOf(lines.get(2).yarn())).isEqualTo("0.999");
        // T = 500,001; B = 500,001 x 0,96 = 480,00096 -> 480,001; kumaş yok: E = H = 480,001; fire %100
        Factory.assertFireFile(
                factory.fireFile(shipmentId), "AWAITING_FABRIC", "500.001", "480.001", "480.001", "100.00");
    }

    @Test
    @DisplayName("Kumaş iki parça gelir, iplik döner: bakiye, fire dosyası, merkez stok ve raporlar birbirini tutar")
    void shouldReconcileEveryScreenAfterFabricReceiptsAndYarnReturn() {
        // given: G = 500,001 kg (A 333,333 + B 166,667 + C 0,001), fire %4
        Party party = factory.newSubcontractor("Örme");
        List<Line> lines = stockedLines();
        Yarn penye = lines.get(0).yarn();
        Yarn poly = lines.get(1).yarn();
        Yarn likra = lines.get(2).yarn();
        UUID shipmentId = factory.sendStockedYarn(party, "0.0400", lines);

        // when: ilk kumaş 100 kg, dağılım uygulamaya bırakılır
        // 100.000 g x 333.333/500.001 = 66.666,47; x 166.667/500.001 = 33.333,33; x 1/500.001 = 0,2
        // tabanlar 66.666 + 33.333 + 0 = 99.999 g; artan 1 g en büyük kesirli paya (A) gider
        JsonNode first =
                factory.receiveFabric(shipmentId, "100.000", Map.of()).ok().body();

        // then: dağılımın toplamı kumaş kilosuna gram hassasiyetinde eşittir
        assertThat(shares(first))
                .containsOnly(
                        Map.entry(penye.id().toString(), "66.667"),
                        Map.entry(poly.id().toString(), "33.333"));
        assertThat(factory.balanceOf(party))
                .containsEntry(penye.name(), "266.666")
                .containsEntry(poly.name(), "133.334")
                .containsEntry("TOPLAM", "400.001");
        // E = 480,00096 - 100 = 380,001; fire = (500,001 - 100) / 500,001 = %80,00
        Factory.assertFireFile(factory.fireFile(shipmentId), "TO_INVOICE", "500.001", "480.001", "380.001", "80.00");

        // when: 16,667 kg B ipliği depoya döner
        factory.returnYarn(shipmentId, poly.id().toString(), "16.667").ok();

        // then: T = 500,001 - 16,667 = 483,334; B = 483,334 x 0,96 = 464,00064 -> 464,001; E = 364,001
        // fire = (483,334 - 100) / 483,334 = %79,31
        assertThat(factory.balanceOf(party))
                .containsEntry(poly.name(), "116.667")
                .containsEntry("TOPLAM", "383.334");
        Factory.assertFireFile(factory.fireFile(shipmentId), "TO_INVOICE", "483.334", "464.001", "364.001", "79.31");
        // Bugünkü davranış: dönen iplik fasoncu bakiyesinden düşer ama merkez stoğa kendiliğinden eklenmez
        // (200 - 166,667 = 33,333 kalır); depo ayrıca iplik girişi yapmazsa 16,667 kg hiçbir ekranda görünmez.
        assertThat(factory.centralStockOf(poly)).isEqualTo("33.333");

        // when: ikinci kumaş 300 kg, dağılımı kalite elle yazar
        factory.receiveFabric(shipmentId, "300.000", explicitShares(penye, poly, likra))
                .ok();

        // then: A 266,666 - 200 = 66,666; B 116,667 - 99,999 = 16,668; C 0; toplam 83,334 = T - K = 483,334 - 400
        Map<String, String> balance = factory.balanceOf(party);
        assertThat(balance)
                .containsEntry(penye.name(), "66.666")
                .containsEntry(poly.name(), "16.668")
                .containsEntry(likra.name(), "0.000")
                .containsEntry("TOPLAM", "83.334");
        // E = 464,00064 - 400 = 64,001; fire = 83,334 / 483,334 = %17,24
        JsonNode file = factory.fireFile(shipmentId);
        Factory.assertFireFile(file, "TO_INVOICE", "483.334", "464.001", "64.001", "17.24");
        assertThat(file.required("remainingKg").asString()).isEqualTo("64.001");
        assertReadSidesAgree(party, shipmentId, balance);
        // merkez stok yalnız sevkiyatla değişti: giriş - gönderilen
        assertThat(factory.centralStockOf(penye)).isEqualTo("66.667");
        assertThat(factory.centralStockOf(likra)).isEqualTo("0.999");
    }

    /** Merkeze A 400, B 200, C 1 kg girilir; sevkiyat satırları 333,333 + 166,667 + 0,001 = 500,001 kg. */
    private List<Line> stockedLines() {
        Yarn penye = factory.newYarn("30/1 Penye");
        Yarn poly = factory.newYarn("70 Denye Poly");
        Yarn likra = factory.newYarn("20 Denye Likra");
        factory.receiveYarnAtCentre(penye, "400.000");
        factory.receiveYarnAtCentre(poly, "200.000");
        factory.receiveYarnAtCentre(likra, "1.000");
        return List.of(penye.kg("333.333"), poly.kg("166.667"), likra.kg("0.001"));
    }

    private static Map<String, String> explicitShares(Yarn penye, Yarn poly, Yarn likra) {
        Map<String, String> explicit = new LinkedHashMap<>();
        explicit.put(penye.id().toString(), "200.000");
        explicit.put(poly.id().toString(), "99.999");
        explicit.put(likra.id().toString(), "0.001");
        return explicit;
    }

    /** Aynı sayılar bakiye matrisinde, fire raporunda, Faturalanacaklar'da ve fire dosyaları listesinde de yazar. */
    private void assertReadSidesAgree(Party party, UUID shipmentId, Map<String, String> balance) {
        Map<String, String> matrix = factory.matrixOf(party);
        Map<String, String> balanceRows = new LinkedHashMap<>(balance);
        balanceRows.remove("TOPLAM");
        assertThat(matrix).isEqualTo(balanceRows);
        BigDecimal matrixTotal = matrix.values().stream().map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(matrixTotal).isEqualByComparingTo("83.334");
        JsonNode report = factory.fireReportRow(party, shipmentId);
        assertThat(report.required("sentKg").asString()).isEqualTo("500.001");
        assertThat(report.required("returnedYarnKg").asString()).isEqualTo("16.667");
        assertThat(report.required("fabricKg").asString()).isEqualTo("400.000");
        assertThat(report.required("expectedFabricKg").asString()).isEqualTo("464.001");
        assertThat(report.required("shortageKg").asString()).isEqualTo("64.001");
        assertThat(report.required("actualFireRate").asString()).isEqualTo("0.1724");
        assertThat(factory.toInvoiceRow(shipmentId))
                .hasValueSatisfying(
                        row -> assertThat(row.required("shortageKg").asString()).isEqualTo("64.001"));
        JsonNode listed = factory.fireListRow(party, shipmentId);
        assertThat(listed.required("faultyKg").asString()).isEqualTo("64.001");
        assertThat(listed.required("remainingKg").asString()).isEqualTo("64.001");
        assertThat(listed.required("actualFirePercent").asString()).isEqualTo("17.24");
    }

    private static Map<String, String> shares(JsonNode receipt) {
        Map<String, String> shares = new LinkedHashMap<>();
        receipt.required("yarns")
                .forEach(share -> shares.put(
                        share.required("yarnType").asString(),
                        share.required("netKg").asString()));
        return shares;
    }
}
