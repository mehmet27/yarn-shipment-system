package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import com.yarnshipment.shipment.masterdata.domain.model.StaffAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.util.List;
import java.util.UUID;

/** Kimlik sağlayıcıda (Keycloak) kullanıcı açar ve uygulama rolü olan kullanıcıları listeler. */
public interface LoginAccountPort {

    /**
     * Kullanıcıyı geçici şifre, rol ve kimlik/lokasyon öznitelikleriyle açar. Kayıt için hesap zaten varsa ya da
     * giriş adı alınmışsa {@code LoginAccountExistsException}; sağlayıcı yanıt vermezse
     * {@code LoginAccountUnavailableException} fırlatır. Yarım kalan hesap bırakılmaz.
     */
    void create(LoginAccountSpec spec);

    /**
     * Uygulama rollerinden en az birine sahip kullanıcılar, giriş adına göre sıralı. Servis hesapları gelmez.
     * Sağlayıcı yanıt vermezse ya da yapılandırılmamışsa {@code LoginAccountUnavailableException} fırlatır.
     */
    List<LoginAccountSummary> findAll();

    /**
     * Fabrika içi kullanıcıyı şifresiz açar, rolünü verir ve şifre belirleme e-postasını gönderir. Herhangi bir adım
     * başarısız olursa kullanıcı silinir. Giriş adı ya da e-posta alınmışsa {@code LoginAccountExistsException};
     * sağlayıcı (ya da e-posta) yanıt vermezse {@code LoginAccountUnavailableException} fırlatır.
     */
    void createStaff(StaffAccountSpec spec);

    /**
     * Kullanıcının fabrika içi rolünü değiştirir ve açık oturumlarını kapatır. Kullanıcı yoksa
     * {@code LoginAccountNotFoundException}; şoför/fasoncu hesabıysa {@code LoginAccountRoleLockedException}.
     */
    void changeStaffRole(UUID userId, StaffRole role);
}
