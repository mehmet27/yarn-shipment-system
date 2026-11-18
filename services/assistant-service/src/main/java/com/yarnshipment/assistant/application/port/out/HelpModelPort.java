package com.yarnshipment.assistant.application.port.out;

import com.yarnshipment.assistant.domain.model.ChatMessage;
import java.util.List;

/** Cevabı üreten dil modeli. */
public interface HelpModelPort {

    /** Model kullanıma hazır mı (anahtar tanımlı mı). */
    boolean isConfigured();

    /**
     * Cevap üretir.
     *
     * @param knowledge değişmeyen proje dokümanları (önbelleğe alınır)
     * @param userContext soran kullanıcıya özel bilgi (önbelleğe alınmaz)
     * @param conversation sohbet; sonuncusu kullanıcının yeni sorusudur
     * @throws com.yarnshipment.assistant.domain.model.HelpUnavailableException cevap alınamazsa
     */
    String complete(String knowledge, String userContext, List<ChatMessage> conversation);
}
