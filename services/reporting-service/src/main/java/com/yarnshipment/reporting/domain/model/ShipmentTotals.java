package com.yarnshipment.reporting.domain.model;

/**
 * Dönemde oluşturulan sevkiyatların duruma göre dağılımı.
 *
 * @param total hepsi
 * @param inProgress taslak, yüklendi, yolda, teslim bekliyor ve kontrol edilen
 * @param inReview uyuşmazlık incelemesinde
 * @param completed tamamlanan
 * @param cancelled iptal edilen
 */
public record ShipmentTotals(int total, int inProgress, int inReview, int completed, int cancelled) {}
