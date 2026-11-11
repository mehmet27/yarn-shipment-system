package com.yarnshipment.shipment.application.port.in.stock;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.stock.WorkArea;
import java.util.Optional;

/**
 * Kalite kullanıcısının kısıtlı olduğu çalışma alanı: merkez kalite yalnız merkez kumaşını, fasoncu kalite yalnız
 * fasoncu kumaşını görür. Başka roller ve alanı atanmamış kullanıcılar kısıtlanmaz (boş döner).
 */
public interface GetWorkAreaUseCase {

    Optional<WorkArea> restrictedTo(Actor actor);
}
