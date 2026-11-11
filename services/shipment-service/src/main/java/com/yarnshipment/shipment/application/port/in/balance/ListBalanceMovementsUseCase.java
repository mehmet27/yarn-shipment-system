package com.yarnshipment.shipment.application.port.in.balance;

/** Bakiye hareketlerini sayfalı okur (BALANCE_VIEW). */
public interface ListBalanceMovementsUseCase {

    MovementPage listBalanceMovements(ListMovementsQuery query);
}
