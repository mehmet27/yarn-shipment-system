package com.yarnshipment.discrepancy.application.port.in.ticket;

import com.yarnshipment.discrepancy.domain.ticket.ReviewedTicket;
import java.util.List;

/** İnceleyen kişi fiş okumasının yanlış olduğunu işaretler; işaret isabet ölçümüne girer. */
public interface MarkTicketMisreadUseCase {

    /** İşaretten sonraki fiş paneli. */
    List<ReviewedTicket> markMisread(MarkTicketMisreadCommand command);
}
