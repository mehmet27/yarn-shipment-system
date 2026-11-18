package com.yarnshipment.assistant.application.port.out;

import com.yarnshipment.assistant.domain.model.HelpDataArgument;
import com.yarnshipment.assistant.domain.model.HelpDataSource;
import java.util.Map;

/** Canlı veriyi yalnızca okur; soran kullanıcının yetkisiyle çalışır. */
public interface HelpDataPort {

    /**
     * Bir veri kaynağını okur. Yetki yoksa ya da kayıt bulunamazsa hata fırlatmaz; modele okunacak açıklayıcı bir
     * metin döner (asistan bunu kullanıcıya anlatır).
     *
     * @param source okunacak kaynak
     * @param arguments süzgeçler; kaynağın tanımadıkları yok sayılır
     * @return modele verilecek metin (JSON ya da açıklama)
     */
    String read(HelpDataSource source, Map<HelpDataArgument, String> arguments);
}
