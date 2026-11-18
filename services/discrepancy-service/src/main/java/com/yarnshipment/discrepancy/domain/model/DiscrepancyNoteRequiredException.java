package com.yarnshipment.discrepancy.domain.model;

/** Karar gerekçesi boş olamaz (400, {@code DISCREPANCY_NOTE_REQUIRED}). */
public final class DiscrepancyNoteRequiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DiscrepancyNoteRequiredException() {
        super("Decision note is required");
    }
}
