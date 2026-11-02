package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.bag.domain.model.Bag;
import java.util.List;

/** Kalitesi kilitli partiye çuval üretir; parti ve kalite partiden kopyalanır. Hepsi ya da hiçbiri. */
public interface CreateBagsUseCase {

    List<Bag> createBags(CreateBagsCommand command);
}
