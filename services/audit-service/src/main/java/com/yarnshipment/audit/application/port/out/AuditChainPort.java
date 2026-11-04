package com.yarnshipment.audit.application.port.out;

import java.util.OptionalLong;

/** Zincir doğrulama; sonucu {@code audit_chain_check}'e kendisi yazar. */
public interface AuditChainPort {

    /** {@code fromId..toId} aralığını doğrular, ilk bozuk kaydın sırasını döner. */
    OptionalLong verify(long fromId, long toId);
}
