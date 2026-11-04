package com.yarnshipment.shipment.masterdata.application.port.in.account;

/** "Şifremi unuttum": giriş adına kayıtlı telefona kod gönderir. */
public interface RequestPasswordResetUseCase {

    /**
     * Hesap varsa, açıksa ve telefonu kayıtlıysa kodu SMS'le gönderir. Hesap yoksa da aynı şekilde sessizce döner;
     * böylece giriş adlarının var olup olmadığı dışarıdan anlaşılmaz.
     */
    void requestPasswordReset(String username);
}
