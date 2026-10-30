package com.yarnshipment.bag.application.port.in;

/** Etiketleri basar ve her baskıyı kaydeder; dönen değer basılacak PDF'tir. */
public interface PrintLabelsUseCase {

    byte[] printLabels(PrintLabelsCommand command);
}
