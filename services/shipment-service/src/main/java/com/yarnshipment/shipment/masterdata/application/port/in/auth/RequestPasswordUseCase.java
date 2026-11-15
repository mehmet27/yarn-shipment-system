package com.yarnshipment.shipment.masterdata.application.port.in.auth;

/** Hesap sahibinin adminden şifre istemesi (giriş yapmadan). */
public interface RequestPasswordUseCase {

    /**
     * Talep yalnızca hesapta art arda en az 3 hatalı giriş varsa (ya da hesap kilitliyse) kaydedilir; hiçbir durumda
     * hata fırlatılmaz ve hesabın var olup olmadığı sızmaz.
     */
    void requestPassword(String username);
}
