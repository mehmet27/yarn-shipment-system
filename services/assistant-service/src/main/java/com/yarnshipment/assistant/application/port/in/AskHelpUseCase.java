package com.yarnshipment.assistant.application.port.in;

/** Kullanıcının sorusunu proje dokümanlarına dayanarak cevaplar; veri okumaz, veri değiştirmez. */
public interface AskHelpUseCase {

    /** Cevap metni. */
    String ask(AskHelpCommand command);
}
