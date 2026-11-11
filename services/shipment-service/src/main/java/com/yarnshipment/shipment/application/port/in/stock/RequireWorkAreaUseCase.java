package com.yarnshipment.shipment.application.port.in.stock;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.stock.WorkArea;

/** Kullanıcı başka bir çalışma alanına kısıtlıysa kapsam dışı erişim olarak reddeder. */
public interface RequireWorkAreaUseCase {

    void requireAccess(Actor actor, WorkArea area);
}
