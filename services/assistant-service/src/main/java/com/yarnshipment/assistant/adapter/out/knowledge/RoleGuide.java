package com.yarnshipment.assistant.adapter.out.knowledge;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.common.security.RolePermissionMatrix;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Rollerin bugün neler yapabildiği: belgelerden değil doğrudan koddaki yetki matrisinden üretilir, bu yüzden
 * belgelerdeki eski "[Yapılacak]" notlarından etkilenmez. Yeni bir izin eklenirse açıklaması yazılana kadar test
 * kırılır; asistan o izni bilmeden bırakılmaz. Çıktı her seferinde aynıdır (önbellek için).
 */
final class RoleGuide {

    private static final Map<Permission, String> LABELS = createLabels();

    private RoleGuide() {}

    /** Her rolün varsayılan yetkileri, günlük dilde. */
    static String describe() {
        StringBuilder text = new StringBuilder("\n\n<belge adi=\"rol-yetkileri\">\n");
        text.append("Rollerin bugünkü varsayılan yetkileri (kodun güncel hali). Bir işi hangi rolün yapabildiğini ")
                .append("belgelerdeki \"[Yapılacak]\" notlarına değil bu listeye bakarak söyle; o notların çoğu ")
                .append("artık eskidir. Yönetici bu yetkileri Yetkiler ekranından değiştirebilir.\n");
        for (Role role : Role.values()) {
            if (role == Role.SYSTEM) {
                continue;
            }
            text.append("\n").append(label(role)).append(" rolü şunları yapabilir:\n");
            Set<Permission> permissions = EnumSet.noneOf(Permission.class);
            permissions.addAll(RolePermissionMatrix.defaultsOf(role));
            permissions.forEach(
                    permission -> text.append("- ").append(label(permission)).append("\n"));
        }
        return text.append("</belge>").toString();
    }

    private static String label(Role role) {
        return switch (role) {
            case ADMIN -> "Yönetici";
            case WAREHOUSE -> "Depo";
            case QUALITY -> "Kalite";
            case MANAGER -> "Müdür";
            case GENERAL_MANAGER -> "Genel müdür";
            case SYSTEM -> "Sistem";
        };
    }

    static String label(Permission permission) {
        String label = LABELS.get(permission);
        if (label == null) {
            throw new IllegalStateException("No help label for permission " + permission);
        }
        return label;
    }

    private static Map<Permission, String> createLabels() {
        Map<Permission, String> labels = new EnumMap<>(Permission.class);
        masterDataAndSystemLabels(labels);
        shipmentLabels(labels);
        qualityAndReportLabels(labels);
        settlementAndStockLabels(labels);
        return labels;
    }

    private static void masterDataAndSystemLabels(Map<Permission, String> labels) {
        labels.put(Permission.MASTER_DATA_VIEW, "Tanımları (fasoncu, şoför, araç, iplik, lokasyon) görür");
        labels.put(Permission.MASTER_DATA_MANAGE, "Tanımları ekler, düzenler ve pasife alır");
        labels.put(Permission.USER_MANAGE, "Kullanıcı hesaplarını açar ve yönetir");
        labels.put(Permission.SETTINGS_MANAGE, "Görünüm ve kural ayarlarını değiştirir");
        labels.put(Permission.PERMISSION_MANAGE, "Rollerin yetkilerini düzenler");
        labels.put(Permission.LOT_QUALITY_LOCK, "Partinin kalitesini kilitler");
    }

    private static void shipmentLabels(Map<Permission, String> labels) {
        labels.put(Permission.SHIPMENT_VIEW, "Sevkiyatları ve sevkiyat dosyalarını görür");
        labels.put(Permission.SHIPMENT_CREATE, "Yeni sevkiyat (taslak) açar");
        labels.put(Permission.SHIPMENT_CANCEL, "Sevkiyatı iptal eder");
        labels.put(Permission.SHIPMENT_COMPLETE, "İrsaliye gelince sevkiyatı tamamlar");
        labels.put(Permission.DISPATCH_RECORD, "Çuval atar ve çıkış tartısını girer");
        labels.put(Permission.DISPATCH_VIEW, "Çıkış tartısını ve çuvalları görür");
        labels.put(Permission.EVIDENCE_UPLOAD_DISPATCH, "Kantar fişi ve çıkış fotoğrafı yükler");
        labels.put(Permission.EVIDENCE_UPLOAD_CLAIM, "Kalite itirazına fotoğraf ve belge yükler");
        labels.put(Permission.EVIDENCE_VIEW, "Yüklenen belge ve fotoğrafları görür");
        labels.put(Permission.EVIDENCE_INVALIDATE, "Yanlış yüklenen belgeyi geçersiz kılar");
    }

    private static void qualityAndReportLabels(Map<Permission, String> labels) {
        labels.put(Permission.CORRECTION_CREATE, "Gerekçe yazarak düzeltme kaydı açar");
        labels.put(Permission.QUALITY_CLAIM_OPEN, "Kalite itirazı açar");
        labels.put(Permission.DISCREPANCY_VIEW, "Uyuşmazlıkları görür");
        labels.put(Permission.DISCREPANCY_RESOLVE, "Uyuşmazlığı karara bağlar, numune ve laboratuvar kontrolü ister");
        labels.put(Permission.DISCREPANCY_DECIDE, "Kalite itirazını haklı ya da haksız diye karara bağlar");
        labels.put(Permission.AUDIT_VIEW, "Denetim kaydını görür");
        labels.put(Permission.REPORT_VIEW, "Raporları görür");
        labels.put(Permission.AMOUNT_VIEW, "Raporlarda tutar (TL) bilgisini görür");
    }

    private static void settlementAndStockLabels(Map<Permission, String> labels) {
        labels.put(Permission.FABRIC_RECEIPT_RECORD, "Fasoncudan gelen kumaşı kaydeder (kumaş girişi)");
        labels.put(Permission.YARN_RETURN_RECORD, "Fasoncudan dönen ya da gelen ipliği kaydeder");
        labels.put(Permission.FIRE_RATE_EDIT, "Fire oranını değiştirir");
        labels.put(Permission.INVOICE_MARK, "\"Fatura edildi\" işaretler");
        labels.put(Permission.BALANCE_VIEW, "Fasoncu bakiyesini görür");
        labels.put(Permission.WEIGHT_MISMATCH_RESOLVE, "Kantar uyuşmazlığıyla duran sevkiyatı yeniden tartıya açar");
        labels.put(Permission.CENTRAL_STOCK_VIEW, "Merkez iplik stokunu ve hareketlerini görür");
        labels.put(Permission.CENTRAL_STOCK_RECORD, "Merkeze gelen ipliği stoğa yazar");
        labels.put(Permission.CENTRAL_PRODUCTION_RECORD, "Merkez fabrikada üretilen kumaşı girer");
    }
}
