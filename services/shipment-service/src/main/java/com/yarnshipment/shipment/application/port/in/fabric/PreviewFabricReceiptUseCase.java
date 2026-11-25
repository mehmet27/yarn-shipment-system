package com.yarnshipment.shipment.application.port.in.fabric;

import com.yarnshipment.shipment.domain.fabric.YarnDrawPlanner.Plan;

/** Kumaş kaydedilmeden önce her ipliğin kaç kg, hangi sevkiyattan düşeceğini ve yetmeyen iplikleri gösterir. */
public interface PreviewFabricReceiptUseCase {

    Plan previewFabricReceipt(PreviewFabricReceiptCommand command);
}
