package com.yarnshipment.shipment.application.port.in.fire;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fire dosyasına girilmiş bir fatura.
 *
 * @param invoiceNo fatura numarası
 * @param invoicedOn fatura tarihi
 * @param kg faturadaki toplam kilo
 */
public record InvoiceEntry(String invoiceNo, LocalDate invoicedOn, BigDecimal kg) {}
