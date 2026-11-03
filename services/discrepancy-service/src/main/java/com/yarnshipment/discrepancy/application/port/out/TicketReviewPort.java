package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.discrepancy.domain.ticket.EnteredNets;
import com.yarnshipment.discrepancy.domain.ticket.TicketMisread;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Uyuşmazlık ekranındaki fiş paneli: okumalar, elle girilen netler ve "AI yanıldı" işaretleri. */
public interface TicketReviewPort {

    /** Sevkiyatın tüm fiş okumaları, okunamayanlar dahil, en yeniden eskiye. */
    List<TicketReading> listReadings(UUID shipmentId);

    /** Teslim karşılaştırmasına giren netler; karşılaştırma yapılmadıysa boş. */
    Optional<EnteredNets> findEnteredNets(UUID shipmentId);

    List<TicketMisread> findMisreads(UUID shipmentId);

    /** İşareti ve uyuşmazlık geçmişine kaydını yazar; okuma zaten işaretliyse ilki kalır. */
    void saveMisread(TicketMisread misread);
}
