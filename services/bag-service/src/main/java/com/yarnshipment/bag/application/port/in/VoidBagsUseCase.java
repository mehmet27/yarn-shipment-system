package com.yarnshipment.bag.application.port.in;

/** Yanlış üretilen, depodaki çuvalları iptal eder. Hepsi ya da hiçbiri; zaten iptal edilmiş olan sayılmaz. */
public interface VoidBagsUseCase {

    /** Bu istekte iptal edilen çuval sayısı. */
    int voidBags(VoidBagsCommand command);
}
