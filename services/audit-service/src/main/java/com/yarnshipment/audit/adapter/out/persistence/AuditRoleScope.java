package com.yarnshipment.audit.adapter.out.persistence;

import java.util.function.Supplier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bir işi veritabanı rolü {@code audit_writer} ya da {@code audit_reader} altında çalıştırır. Uygulamanın bağlandığı
 * kullanıcı iki rolün üyesidir ama yetkilerini miras almaz; yetki ancak {@code SET LOCAL ROLE} ile açılır ve iş
 * bitince {@code RESET ROLE} ile geri verilir. Böylece yazma yolu (yalnızca {@code audit_append()}) tabloyu
 * okuyamaz, okuma yolu (yalnızca SELECT) hiçbir şey ekleyemez.
 *
 * <p>Çağıranın transaction'ı varsa ona katılır (denetim kaydı iş işleminin içinde yazılır); yoksa kendi kısa
 * transaction'ını açar, çünkü {@code SET LOCAL} yalnızca transaction içinde geçerlidir.
 */
final class AuditRoleScope {

    static final String WRITER = "audit_writer";
    static final String READER = "audit_reader";

    private final JdbcClient jdbc;
    private final TransactionTemplate transaction;

    AuditRoleScope(JdbcClient jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * İşi verilen rolün altında çalıştırır. İş hata verirse rol geri verilmez: transaction geri alınacağı için
     * ({@code SET LOCAL} transaction ile biter) başka bir sorgu o rolle çalışamaz.
     *
     * @param role {@link #WRITER} ya da {@link #READER}; sabit değerdir, kullanıcı girdisi değildir
     */
    <T> T runAs(String role, Supplier<T> work) {
        return transaction.execute(status -> inRole(role, work));
    }

    private <T> T inRole(String role, Supplier<T> work) {
        jdbc.sql("SET LOCAL ROLE " + role).update();
        T result = work.get();
        jdbc.sql("RESET ROLE").update();
        return result;
    }
}
