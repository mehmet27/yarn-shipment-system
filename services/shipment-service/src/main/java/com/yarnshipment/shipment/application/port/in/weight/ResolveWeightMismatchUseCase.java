package com.yarnshipment.shipment.application.port.in.weight;

/** Kantar uyuşmazlığındaki sevkiyata müdür karar verir (iptal, çuval ekle/çıkar, reçeteyi güncelle). */
public interface ResolveWeightMismatchUseCase {

    void resolve(ResolveWeightMismatchCommand command);
}
