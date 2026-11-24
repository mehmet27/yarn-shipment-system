package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyat listesi isteği.
 *
 * @param actor isteyen (SEC-04)
 * @param filter durum, arama metni ve sevkiyat tarihi süzgeci
 * @param page sayfa numarası, 0'dan başlar
 * @param size sayfa boyu, 1-100 (API-06)
 */
public record ListShipmentsQuery(Actor actor, Filter filter, int page, int size) {

    public static final int MAX_PAGE_SIZE = 100;

    public ListShipmentsQuery {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(filter, "filter");
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid page request page=" + page + " size=" + size);
        }
    }

    /**
     * Liste süzgeci; verilmeyen alan süzmez.
     *
     * @param statuses yalnızca bu durumlar; boşsa hepsi
     * @param search sevkiyat numarası, fasoncu adı, şoför adı ya da plakada aranan metin; baştaki ve sondaki boşluk
     *     kırpılır, boş metin yok sayılır
     * @param from sevkiyat tarihi bu günden önce olmayanlar
     * @param to sevkiyat tarihi bu günden sonra olmayanlar
     */
    public record Filter(
            Set<ShipmentStatus> statuses,
            @Nullable String search,
            @Nullable LocalDate from,
            @Nullable LocalDate to) {

        /** Hiçbir şeyi süzmeyen süzgeç. */
        public static final Filter NONE = new Filter(Set.of(), null, null, null);

        public Filter {
            statuses = Set.copyOf(statuses);
            search = search == null || search.isBlank() ? null : search.strip();
        }

        /** Yalnızca duruma göre süzer. */
        public static Filter byStatus(Set<ShipmentStatus> statuses) {
            return new Filter(statuses, null, null, null);
        }
    }
}
