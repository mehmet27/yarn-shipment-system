package com.yarnshipment.common.outbox;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/** Kaynak tutmayan transaction yöneticisi; commit ve rollback sayılarını tutar. */
final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

    private static final long serialVersionUID = 1L;

    private int commits;
    private int rollbacks;

    int commits() {
        return commits;
    }

    int rollbacks() {
        return rollbacks;
    }

    @Override
    protected Object doGetTransaction() {
        return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        // Kaynak yok.
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        commits++;
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        rollbacks++;
    }
}
