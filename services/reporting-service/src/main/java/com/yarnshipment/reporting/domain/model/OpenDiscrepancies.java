package com.yarnshipment.reporting.domain.model;

/**
 * Şu an açık uyuşmazlıklar; dönemden bağımsızdır.
 *
 * @param open açık toplam
 * @param red Kırmızı olanlar
 * @param yellow Sarı olanlar
 * @param overdue vadesi geçmiş olanlar
 */
public record OpenDiscrepancies(int open, int red, int yellow, int overdue) {}
