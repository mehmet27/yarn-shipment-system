package com.yarnshipment.shipment.application.port.in.stock;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.stock.WorkArea;
import java.util.Optional;

/**
 * Kalite kullanıcısının çalışma alanı kuralı: merkez kalite yalnız merkez kumaşını, fasoncu kalite yalnız fasoncu
 * kumaşını görür. Başka roller ve alanı atanmamış kullanıcılar kısıtlanmaz.
 */
public interface WorkAreaUseCase {

    /** Kullanıcının kısıtlı olduğu alan; kısıtlı değilse boş. */
    Optional<WorkArea> restrictedTo(Actor actor);

    /** Kullanıcı başka bir alana kısıtlıysa kapsam dışı erişim olarak reddeder. */
    void requireAccess(Actor actor, WorkArea area);
}
