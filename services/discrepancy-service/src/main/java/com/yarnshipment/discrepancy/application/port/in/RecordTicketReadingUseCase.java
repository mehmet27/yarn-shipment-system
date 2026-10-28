package com.yarnshipment.discrepancy.application.port.in;

/** Kantar fişi okumasını teslim karşılaştırmasında kullanılmak üzere saklar. */
public interface RecordTicketReadingUseCase {

    void recordTicketReading(RecordTicketReadingCommand command);
}
