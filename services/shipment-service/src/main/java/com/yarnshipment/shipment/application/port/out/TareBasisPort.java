package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.model.PackageType;
import com.yarnshipment.shipment.domain.model.Weighing.TareBasis;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Çıkış tartımında karşılaştırılacak standart dara (ambalaj türüne göre). */
public interface TareBasisPort {

    /**
     * Sevkiyattaki ambalajların türüne göre beklenen dara: her türün standart darası kendi adediyle çarpılıp toplanır.
     * Standardı tanımsız ya da sıfır olan tür toplama girmez; hiç türün standardı yoksa boş döner.
     *
     * @param packageCounts ambalaj türü başına adet
     */
    @Nullable
    TareBasis findTareBasis(UUID siteId, Map<PackageType, Integer> packageCounts);
}
