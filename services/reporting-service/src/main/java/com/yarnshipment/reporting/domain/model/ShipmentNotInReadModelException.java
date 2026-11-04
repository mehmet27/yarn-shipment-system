package com.yarnshipment.reporting.domain.model;

import java.util.UUID;

/**
 * Olay okuma modelinde olmayan bir sevkiyata ait. Sevkiyat oluşturma olayı henüz işlenmemiş olabilir; dinleyici
 * hata politikası olayı bekleyip tekrar dener, sonunda ölü mektup konusuna alır.
 */
public final class ShipmentNotInReadModelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ShipmentNotInReadModelException(UUID shipmentId) {
        super("Shipment not in read model shipmentId=" + shipmentId);
    }
}
