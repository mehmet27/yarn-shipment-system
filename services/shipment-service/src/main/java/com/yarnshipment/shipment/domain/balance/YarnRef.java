package com.yarnshipment.shipment.domain.balance;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * İplik cinsi ve varsa lot.
 *
 * @param yarnType iplik cinsi, boş olamaz
 * @param lotId lot
 */
public record YarnRef(String yarnType, @Nullable UUID lotId) {

    public YarnRef {
        yarnType = yarnType.strip();
        if (yarnType.isEmpty()) {
            throw new InvalidLedgerInputException("yarnType");
        }
    }
}
