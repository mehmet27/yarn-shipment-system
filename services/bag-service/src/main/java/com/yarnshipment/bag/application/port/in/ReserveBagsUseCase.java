package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.bag.domain.model.Bag;
import java.util.List;

/** Çuvalları sevkiyata ayırır. Hepsi ya da hiçbiri; aynı sevkiyata zaten ayrılmış çuval hata vermez. */
public interface ReserveBagsUseCase {

    List<Bag> reserveBags(ReserveBagsCommand command);
}
