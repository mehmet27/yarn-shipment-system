package com.yarnshipment.shipment.masterdata.application.port.out;

/** Şifre özetleme. Şifre hiçbir yerde loglanmaz ya da saklanmaz; yalnızca özeti yazılır. */
public interface PasswordHasherPort {

    /** Tuzlu, yavaş özet (BCrypt). */
    String hash(String password);

    boolean matches(String password, String passwordHash);

    /** Kullanıcı yokken de aynı süreyi harcar; böylece cevap süresinden hesabın varlığı anlaşılmaz. */
    void spendVerificationTime(String password);
}
