package com.yarnshipment.shipment.application.port.in.balance;

/** Fasoncu bakiyelerini okur (BALANCE_VIEW). */
public interface GetBalancesUseCase {

    BalanceSummary getBalances(GetBalancesQuery query);
}
