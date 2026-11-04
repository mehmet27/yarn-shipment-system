package com.yarnshipment.reporting.domain.model;

/**
 * Karşılaştırma bandına göre sevkiyat sayıları.
 *
 * @param green Yeşil
 * @param yellow Sarı
 * @param red Kırmızı
 */
public record BandCounts(int green, int yellow, int red) {}
