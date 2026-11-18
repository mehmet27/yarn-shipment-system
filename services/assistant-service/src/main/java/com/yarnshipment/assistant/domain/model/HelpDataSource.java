package com.yarnshipment.assistant.domain.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Asistanın okuyabildiği canlı veri kaynakları. Hepsi yalnızca okuma uçlarıdır; asistan bunlara soran kullanıcının
 * kendi oturumuyla gider, yani o kullanıcının göremediği veriyi (ör. Kalite için tutarları) göremez.
 */
public enum HelpDataSource {
    DASHBOARD(
            "panel_ozeti",
            "/api/v1/reports/dashboard",
            "Dönemin özeti: sevkiyat sayıları, toplam gönderilen ve teslim alınan kilo, açık uyuşmazlıklar",
            HelpDataArgument.START,
            HelpDataArgument.END),
    SUBCONTRACTOR_BALANCE(
            "fasoncu_bakiye",
            "/api/v1/reports/subcontractor-balance",
            "Fasoncu başına gönderilen, teslim alınan ve yolda kalan kilo (dönem toplamları)",
            HelpDataArgument.START,
            HelpDataArgument.END),
    BALANCE_MATRIX(
            "bakiye_matrisi",
            "/api/v1/reports/balance-matrix",
            "İplik cinsi ve fasoncu bazında fasonculardaki güncel iplik bakiyesi"),
    YARN_VS_FABRIC(
            "iplik_kumas",
            "/api/v1/reports/yarn-vs-fabric",
            "Fasoncu başına giden iplik, dönen iplik, gelen kumaş, fire oranı ve eksik kilo",
            HelpDataArgument.START,
            HelpDataArgument.END),
    FIRE_FILES(
            "fire_dosyalari",
            "/api/v1/reports/fire",
            "Sevkiyat başına fire dosyaları: beklenen/gelen kumaş, fire oranı, eksik kilo, fatura durumu",
            HelpDataArgument.START,
            HelpDataArgument.END,
            HelpDataArgument.SUBCONTRACTOR),
    TO_INVOICE(
            "faturalanacaklar",
            "/api/v1/reports/to-invoice",
            "Faturası bekleyen fire dosyaları (eksik kilosu olup henüz faturalanmamış)",
            HelpDataArgument.START,
            HelpDataArgument.END),
    AMOUNTS(
            "tutarlar",
            "/api/v1/reports/amounts",
            "Tutar bilgisi (TL): gönderilen iplik, faturalanacak ve fasonculardaki iplik tutarı; yalnız yetkililer",
            HelpDataArgument.START,
            HelpDataArgument.END),
    SUBCONTRACTOR_STATS(
            "fasoncu_istatistik",
            "/api/v1/reports/subcontractors",
            "Fasoncu başına sevkiyat sayısı ve uyuşmazlık oranı",
            HelpDataArgument.START,
            HelpDataArgument.END),
    SHIPMENT_REPORT(
            "sevkiyat_dokumu",
            "/api/v1/reports/shipments",
            "Dönemdeki sevkiyatların tek tek dökümü (numara, durum, gönderilen ve teslim alınan kilo). Satır çoktur: "
                    + "toplam için özet kaynakları tercih et",
            HelpDataArgument.START,
            HelpDataArgument.END),
    SHIPMENTS(
            "sevkiyatlar",
            "/api/v1/shipments",
            "Sevkiyat listesi, en yeni önce (sayfa başına 20). durum: DRAFT, WEIGHT_MISMATCH, AWAITING_DELIVERY, "
                    + "COMPLETED, CANCELLED",
            HelpDataArgument.STATUS,
            HelpDataArgument.PAGE),
    SHIPMENT_DETAIL(
            "sevkiyat_ayrinti",
            "/api/v1/shipments/{shipmentId}",
            "Tek bir sevkiyatın ayrıntısı (sevkiyat_no ile)",
            HelpDataArgument.SHIPMENT_NO),
    SHIPMENT_HISTORY(
            "sevkiyat_gecmisi",
            "/api/v1/shipments/{shipmentId}/history",
            "Tek bir sevkiyatın durum geçmişi; neden beklediğini anlamak için (sevkiyat_no ile)",
            HelpDataArgument.SHIPMENT_NO),
    CENTRAL_STOCK(
            "merkez_stok",
            "/api/v1/central-stock",
            "Merkezdeki (fasoncuda olmayan) iplik stoku: iplik cinsi başına kilo, toplam, üretilen kumaş, "
                    + "iplik akışı, son hareketler ve tedarikçiden gelen iplik özeti",
            HelpDataArgument.PAGE),
    SUBCONTRACTOR_YARN(
            "fasoncu_iplik",
            "/api/v1/balances",
            "Fasonculardaki iplik bakiyesi ayrıntısı (iplik cinsi bazında); fasoncu_id ile tek fasoncu",
            HelpDataArgument.SUBCONTRACTOR),
    BALANCE_MOVEMENTS(
            "bakiye_hareketleri",
            "/api/v1/balances/movements",
            "Fasoncu iplik bakiyesinin hareketleri (gönderim, dönüş, kumaş, düzeltme); fasoncu_id ile süzülür",
            HelpDataArgument.SUBCONTRACTOR,
            HelpDataArgument.PAGE),
    YARN_SUPPLIERS(
            "tedarikciler",
            "/api/v1/master-data/yarn-suppliers",
            "İplik tedarikçileri listesi (kimlik ve ünvan)",
            HelpDataArgument.SEARCH,
            HelpDataArgument.PAGE),
    SUBCONTRACTORS(
            "fasoncular",
            "/api/v1/master-data/subcontractors",
            "Fasoncu listesi: kimlik ve ünvan. Diğer kaynaklardaki fasoncu kimliklerini isme çevirmek için",
            HelpDataArgument.SEARCH,
            HelpDataArgument.PAGE);

    private final String toolName;
    private final String path;
    private final String description;
    // List.copyOf değişmezdir; denetleyici bunu bilemez.
    @SuppressWarnings("ImmutableEnumChecker")
    private final List<HelpDataArgument> arguments;

    HelpDataSource(String toolName, String path, String description, HelpDataArgument... arguments) {
        this.toolName = toolName;
        this.path = path;
        this.description = description;
        this.arguments = List.of(arguments);
    }

    /** Modelin kullandığı ad. */
    public String toolName() {
        return toolName;
    }

    /** Uygulama ucunun yolu; sevkiyat kimliği yeri {@code {shipmentId}} ile gösterilir. */
    public String path() {
        return path;
    }

    /** Kaynağın modele anlatılan açıklaması. */
    public String description() {
        return description;
    }

    /** Bu kaynağın kabul ettiği süzgeçler; başkaları yok sayılır. */
    public List<HelpDataArgument> arguments() {
        return arguments;
    }

    /** Modelin verdiği adla kaynağı bulur. */
    public static Optional<HelpDataSource> byToolName(String name) {
        return Arrays.stream(values()).filter(s -> s.toolName.equals(name)).findFirst();
    }
}
