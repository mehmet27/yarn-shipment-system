package com.yarnshipment.shipment.application.port.in.balance;

import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sayfalı hareket listesi, en yeni önce (API-06).
 *
 * @param items bu sayfadaki hareketler
 * @param number sayfa numarası, 0'dan başlar
 * @param size sayfa boyu
 * @param totalElements toplam kayıt
 * @param actorNames girenlerin okunur adları; çözülemeyen kişi haritada yoktur
 */
public record MovementPage(
        List<BalanceMovement> items, int number, int size, long totalElements, Map<UUID, String> actorNames) {

    public MovementPage {
        items = List.copyOf(items);
        actorNames = Map.copyOf(actorNames);
    }

    public MovementPage(List<BalanceMovement> items, int number, int size, long totalElements) {
        this(items, number, size, totalElements, Map.of());
    }

    public MovementPage withActorNames(Map<UUID, String> names) {
        return new MovementPage(items, number, size, totalElements, names);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}
