package com.yarnshipment.shipment.application.port.in.fabric;

import com.yarnshipment.shipment.domain.fabric.YarnReturn;

/** Dönen iplik ya da fasoncudan gelen ipliği kaydeder (YARN_RETURN_RECORD); dönen iplik fire tabanından düşer. */
public interface RecordYarnReturnUseCase {

    YarnReturn recordYarnReturn(RecordYarnReturnCommand command);
}
