package com.yarnshipment.shipment.application.port.in.balance;

import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import java.util.List;

/**
 * Sayfalı hareket listesi, en yeni önce (API-06).
 *
 * @param items bu sayfadaki hareketler
 * @param number sayfa numarası, 0'dan başlar
 * @param size sayfa boyu
 * @param totalElements toplam kayıt
 */
public record MovementPage(List<BalanceMovement> items, int number, int size, long totalElements) {

    public MovementPage {
        items = List.copyOf(items);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}
