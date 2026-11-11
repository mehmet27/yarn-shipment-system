package com.yarnshipment.shipment.application.port.in.fire;

import com.yarnshipment.shipment.domain.fire.FireCalculation;
import com.yarnshipment.shipment.domain.fire.FireSettlement;

/**
 * Fire dosyası ve güncel hesabı.
 *
 * @param settlement dosya
 * @param calculation kayıtlardan hesaplanan fire
 */
public record FireSettlementDetails(FireSettlement settlement, FireCalculation calculation) {}
