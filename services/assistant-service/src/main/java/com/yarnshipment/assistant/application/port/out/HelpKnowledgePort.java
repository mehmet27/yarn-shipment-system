package com.yarnshipment.assistant.application.port.out;

/** Asistanın bildiği proje dokümanları. */
public interface HelpKnowledgePort {

    /** Tüm dokümanlar tek metin olarak; her çağrıda aynı sırada ve aynı içerikte döner (önbellek bunu şart koşar). */
    String documents();
}
