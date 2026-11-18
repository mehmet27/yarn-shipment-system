package com.yarnshipment.discrepancy.application.port.in;

/** Sevkiyata girilen kumaşı izler; kalite itirazı yalnızca girilmiş kumaş kadar açılabilir. */
public interface RecordFabricReceiptUseCase {

    void recordFabricReceipt(RecordFabricReceiptCommand command);
}
