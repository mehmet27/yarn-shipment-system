package com.yarnshipment.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.app.FactoryHttp.Reply;
import com.yarnshipment.app.FactoryHttp.Staff;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

/**
 * Fabrikanın iş akışı, testlerin okuyacağı dille: her adımı o işi fabrikada yapan rol, uygulamanın HTTP ucundan yapar.
 * Hesap içermez; sayıları yalnızca uygulamadan okur.
 */
final class Factory {

    private static final ZoneId FACTORY_ZONE = ZoneId.of("Europe/Istanbul");
    private static final String TARE_KG = "8000.000";
    private static final int CODE_LENGTH = 12;
    private static final int WAYBILL_LENGTH = 8;
    private static final String SHIPMENT = "/api/v1/shipments/";
    private static final byte[] DOCUMENT = "%PDF-1.4\n%factory test\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

    private final FactoryHttp http;
    private @Nullable UUID siteId;

    Factory(FactoryHttp http) {
        this.http = http;
    }

    static LocalDate today() {
        return LocalDate.now(FACTORY_ZONE);
    }

    /** Fire dosyasının durumu ve sırasıyla T, B, H (faturalanacak) ve gerçekleşen fire yüzdesi. */
    static void assertFireFile(JsonNode file, String status, String... figures) {
        assertThat(file.required("status").asString()).isEqualTo(status);
        assertThat(List.of(
                        file.required("baseKg").asString(),
                        file.required("expectedFabricKg").asString(),
                        file.required("faultyKg").asString(),
                        file.required("actualFirePercent").asString()))
                .containsExactly(figures);
    }

    Party newSubcontractor(String label) {
        site();
        String title = FactoryHttp.unique(label);
        Reply created = http.send(
                        Staff.MANAGER, HttpMethod.POST, "/api/v1/master-data/subcontractors", Map.of("title", title))
                .ok();
        return new Party(created.id("id"), title);
    }

    Yarn newYarn(String label) {
        String name = FactoryHttp.unique(label);
        String code = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, CODE_LENGTH);
        Reply created = http.send(
                        Staff.MANAGER,
                        HttpMethod.POST,
                        "/api/v1/master-data/products",
                        Map.of("code", code, "name", name))
                .ok();
        return new Yarn(created.id("id"), name);
    }

    /** Depo, tedarikçiden gelen ipliği merkez stoğa girer. */
    void receiveYarnAtCentre(Yarn yarn, String kg) {
        Reply supplier = http.send(
                        Staff.MANAGER,
                        HttpMethod.POST,
                        "/api/v1/master-data/yarn-suppliers",
                        Map.of("name", FactoryHttp.unique("Tedarikçi")))
                .ok();
        Map<String, Object> receipt = Map.of("productId", yarn.id(), "supplierId", supplier.id("id"), "netKg", kg);
        http.send(Staff.WAREHOUSE, HttpMethod.POST, "/api/v1/central-stock/yarn-receipts", receipt)
                .ok();
    }

    /** Müdür taslağı açar. */
    UUID draftShipment(Party to, String fireRate, List<Line> lines) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("siteId", site());
        request.put("subcontractorId", to.id());
        request.put("subcontractorTitle", to.title());
        request.put("shipmentDate", today().toString());
        request.put("fireRate", fireRate);
        request.put("lines", lines.stream().map(Line::toRequest).toList());
        return http.send(Staff.MANAGER, HttpMethod.POST, "/api/v1/shipments", request)
                .ok()
                .id("id");
    }

    /** Depo her iplik satırını bir çuval olarak ekler ve kantarda reçeteyle aynı neti tartar. */
    void dispatch(UUID shipmentId, List<Line> lines) {
        List<String> bags = lines.stream().map(Line::kg).toList();
        http.send(Staff.WAREHOUSE, HttpMethod.POST, SHIPMENT + shipmentId + "/bags", Map.of("weightsKg", bags))
                .ok();
        BigDecimal net = lines.stream().map(line -> new BigDecimal(line.kg())).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> weighing = Map.of(
                "emptyVehicleKg",
                TARE_KG,
                "loadedVehicleKg",
                new BigDecimal(TARE_KG).add(net).toPlainString(),
                "bagCount",
                bags.size());
        http.send(Staff.WAREHOUSE, HttpMethod.POST, SHIPMENT + shipmentId + "/dispatch-weighing", weighing)
                .ok();
    }

    /** Müdür irsaliyeyi yükleyip sevkiyatı tamamlar. */
    void complete(UUID shipmentId) {
        UUID waybill = uploadEvidence(Staff.MANAGER, shipmentId, "WAYBILL");
        Map<String, Object> completion = Map.of(
                "waybillNo", "IRS-" + shipmentId.toString().substring(0, WAYBILL_LENGTH), "waybillEvidenceId", waybill);
        http.send(Staff.MANAGER, HttpMethod.POST, SHIPMENT + shipmentId + "/completion", completion)
                .ok();
    }

    /** Merkeze tam gereken iplik girilir; taslak, çıkış tartısı ve irsaliyeyle tamamlama: iplik fasoncunun elinde. */
    UUID sendYarn(Party to, String fireRate, List<Line> lines) {
        lines.forEach(line -> receiveYarnAtCentre(line.yarn(), line.kg()));
        return sendStockedYarn(to, fireRate, lines);
    }

    /** İpliği merkez stokta zaten bulunan sevkiyat: taslak, çıkış tartısı ve irsaliyeyle tamamlama. */
    UUID sendStockedYarn(Party to, String fireRate, List<Line> lines) {
        UUID shipmentId = draftShipment(to, fireRate, lines);
        dispatch(shipmentId, lines);
        complete(shipmentId);
        return shipmentId;
    }

    /** Fasoncu kalite gelen kumaşı girer; her iplik için kumaştaki yüzde payı (iplik adı → oran) yazılır. */
    Reply receiveFabric(UUID shipmentId, String kg, Map<String, String> ratioByYarnName) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("fabricType", "Süprem");
        request.put("netKg", kg);
        request.put("waybillNo", FactoryHttp.unique("KMS"));
        List<Map<String, String>> ratios = new ArrayList<>();
        ratioByYarnName.forEach((yarn, ratio) -> ratios.add(Map.of("yarnType", yarn, "ratioPct", ratio)));
        request.put("yarns", ratios);
        return http.send(Staff.QUALITY, HttpMethod.POST, SHIPMENT + shipmentId + "/fabric-receipts", request);
    }

    /** İki iplikli kumaş karışımı: iplik adı → yüzde. */
    static Map<String, String> ratios(Yarn first, String firstPct, Yarn second, String secondPct) {
        return Map.of(first.name(), firstPct, second.name(), secondPct);
    }

    /** Depo fasoncudan dönen ipliği girer. */
    Reply returnYarn(UUID shipmentId, String yarnType, String kg) {
        Map<String, Object> request = Map.of("yarnType", yarnType, "netKg", kg);
        return http.send(Staff.WAREHOUSE, HttpMethod.POST, SHIPMENT + shipmentId + "/yarn-returns", request);
    }

    /** Müdür fatura bilgisini girer; satır verilmezse kalan kilonun tamamı faturalanır. */
    Reply invoice(UUID shipmentId, String invoiceNo, Map<String, String> yarnLines) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("invoiceNo", invoiceNo);
        request.put("invoicedOn", today().toString());
        if (!yarnLines.isEmpty()) {
            request.put("lines", shares(yarnLines));
        }
        return http.send(Staff.MANAGER, HttpMethod.POST, SHIPMENT + shipmentId + "/fire-settlement/invoice", request);
    }

    /** Kalite fotoğrafla itiraz açar; dönen değer karara bağlanacak uyuşmazlığın kimliğidir. */
    UUID openQualityClaim(UUID shipmentId, String kg) {
        UUID photo = uploadEvidence(Staff.QUALITY, shipmentId, "QUALITY_CLAIM");
        Map<String, Object> request = Map.of(
                "shipmentId",
                shipmentId,
                "description",
                "Kumaşta leke",
                "claimedKg",
                kg,
                "evidenceIds",
                List.of(photo));
        return http.send(Staff.QUALITY, HttpMethod.POST, "/api/v1/quality-claims", request)
                .ok()
                .id("discrepancyId");
    }

    /** Müdür itirazı "Haklı" (CLAIM_JUSTIFIED) ya da "Haksız" (CLAIM_UNJUSTIFIED) olarak karara bağlar. */
    Reply decideClaim(UUID discrepancyId, String resolution) {
        Map<String, Object> request = Map.of("resolution", resolution, "note", "Müdür kararı");
        return http.send(
                Staff.MANAGER, HttpMethod.POST, "/api/v1/discrepancies/" + discrepancyId + "/resolve", request);
    }

    JsonNode fireFile(UUID shipmentId) {
        return http.get(Staff.MANAGER, SHIPMENT + shipmentId + "/fire-settlement")
                .ok()
                .body();
    }

    /** Fasoncu bakiyesi ekranı: iplik adı → kg; toplam {@code "TOPLAM"} anahtarındadır. */
    Map<String, String> balanceOf(Party party) {
        JsonNode body = http.get(Staff.MANAGER, "/api/v1/balances?subcontractorId={id}", party.id())
                .ok()
                .body();
        Map<String, String> byYarn = new LinkedHashMap<>();
        body.required("items")
                .forEach(row -> byYarn.put(
                        row.required("yarnType").asString(),
                        row.required("balanceKg").asString()));
        byYarn.put("TOPLAM", body.required("totalKg").asString());
        return byYarn;
    }

    /** Raporlardaki bakiye matrisi: bu fasoncunun satırları, iplik adı → kg. */
    Map<String, String> matrixOf(Party party) {
        Map<String, String> byYarn = new LinkedHashMap<>();
        for (JsonNode row :
                http.get(Staff.MANAGER, "/api/v1/reports/balance-matrix").ok().body()) {
            if (party.id().toString().equals(row.required("subcontractorId").asString())) {
                byYarn.put(
                        row.required("yarnType").asString(),
                        row.required("balanceKg").asString());
            }
        }
        return byYarn;
    }

    /** Merkez iplik stoku ekranında bu ipliğin eldeki kilosu; satırı yoksa {@code null}. */
    @Nullable
    String centralStockOf(Yarn yarn) {
        for (JsonNode row :
                http.get(Staff.MANAGER, "/api/v1/central-stock").ok().body().required("rows")) {
            if (yarn.name().equals(row.required("yarnType").asString())) {
                return row.required("stockKg").asString();
            }
        }
        return null;
    }

    /** Fire raporunda (Raporlar) bu sevkiyatın satırı. */
    JsonNode fireReportRow(Party party, UUID shipmentId) {
        return rowOf(http.get(Staff.MANAGER, "/api/v1/reports/fire?subcontractorId={id}", party.id()), shipmentId)
                .orElseThrow(() -> new AssertionError("Fire report row not found shipmentId=" + shipmentId));
    }

    /** Faturalanacaklar listesinde bu sevkiyatın satırı; listede yoksa boş. */
    Optional<JsonNode> toInvoiceRow(UUID shipmentId) {
        return rowOf(http.get(Staff.MANAGER, "/api/v1/reports/to-invoice"), shipmentId);
    }

    /** Faturalananlar listesi: bu fasoncunun fatura satırları ve toplamı. */
    JsonNode invoicedList(Party party) {
        return http.get(Staff.MANAGER, "/api/v1/fire-invoices?subcontractorId={id}&size=100", party.id())
                .ok()
                .body();
    }

    /** Fire dosyaları listesinde (sevkiyat modülü) bu sevkiyatın satırı. */
    JsonNode fireListRow(Party party, UUID shipmentId) {
        Reply page = http.get(Staff.MANAGER, "/api/v1/fire-settlements?subcontractorId={id}&size=100", party.id());
        for (JsonNode row : page.ok().body().required("items")) {
            if (shipmentId.toString().equals(row.required("shipmentId").asString())) {
                return row;
            }
        }
        throw new AssertionError("Fire settlement row not found shipmentId=" + shipmentId);
    }

    private static Optional<JsonNode> rowOf(Reply rows, UUID shipmentId) {
        for (JsonNode row : rows.ok().body()) {
            if (shipmentId.toString().equals(row.required("shipmentId").asString())) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    private static List<Map<String, String>> shares(Map<String, String> kgByYarn) {
        List<Map<String, String>> shares = new ArrayList<>();
        kgByYarn.forEach((yarn, kg) -> shares.add(Map.of("yarnType", yarn, "netKg", kg)));
        return shares;
    }

    private UUID uploadEvidence(Staff staff, UUID shipmentId, String evidenceType) {
        Map<String, Object> request = Map.of(
                "shipmentId",
                shipmentId,
                "evidenceType",
                evidenceType,
                "mimeType",
                "application/pdf",
                "sizeBytes",
                DOCUMENT.length);
        Reply ticket = http.send(staff, HttpMethod.POST, "/api/v1/evidence/uploads", request)
                .ok();
        http.upload(ticket.text("uploadUrl"), "application/pdf", DOCUMENT);
        UUID evidenceId = ticket.id("evidenceId");
        http.send(staff, HttpMethod.POST, "/api/v1/evidence/uploads/" + evidenceId + "/confirm", null)
                .ok();
        return evidenceId;
    }

    /** Fabrikanın tek merkez lokasyonu kurulumla gelir. */
    private synchronized UUID site() {
        UUID existing = siteId;
        if (existing != null) {
            return existing;
        }
        JsonNode sites =
                http.get(Staff.MANAGER, "/api/v1/master-data/sites").ok().body().required("items");
        UUID created = UUID.fromString(sites.required(0).required("id").asString());
        siteId = created;
        return created;
    }

    /** Fasoncu. */
    record Party(UUID id, String title) {}

    /** Tanımlar'daki iplik cinsi. */
    record Yarn(UUID id, String name) {

        Line kg(String netKg) {
            return new Line(this, netKg);
        }
    }

    /** Sevkiyatın iplik satırı. */
    record Line(Yarn yarn, String kg) {

        Map<String, Object> toRequest() {
            return Map.of("yarnTypeId", yarn.id(), "netKg", kg, "unitPrice", "10");
        }
    }
}
