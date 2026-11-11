package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import java.util.Optional;
import java.util.UUID;

/** Haklı itirazın bakiye hareketlerini defterine yazar. */
public interface ClaimLedgerPort {

    /** Sevkiyatın fasoncusu; sevkiyat yoksa ya da fasoncusuz ise boş. */
    Optional<UUID> findSubcontractor(UUID shipmentId);

    /** Hareketi ekler; aynı kaynak daha önce yazılmışsa dokunmaz. */
    void append(BalanceMovement movement);
}
