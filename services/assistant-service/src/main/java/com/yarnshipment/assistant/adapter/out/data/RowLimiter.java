package com.yarnshipment.assistant.adapter.out.data;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Uzun listeleri kısaltır: modele giden her satır token maliyetidir. */
final class RowLimiter {

    /** Bir cevapta modele verilen en çok satır; fazlası "kısaltıldı" notuyla kesilir. */
    static final int MAX_ROWS = 100;

    private final JsonMapper json;

    RowLimiter(JsonMapper json) {
        this.json = json;
    }

    /** Liste {@value #MAX_ROWS} satırı geçerse ilk satırları ve toplam sayıyı, kesildiği notuyla verir. */
    String limit(String body) {
        JsonNode node = json.readTree(body);
        if (!node.isArray() || node.size() <= MAX_ROWS) {
            return body;
        }
        ObjectNode wrapper = json.createObjectNode();
        wrapper.put(
                "not",
                "Liste kısaltıldı: toplam " + node.size() + " satırın ilk " + MAX_ROWS
                        + " tanesi var. Toplam için özet kaynaklarını kullan ya da tarih aralığını daralt.");
        wrapper.put("toplamSatir", node.size());
        wrapper.putArray("satirlar").addAll(node.valueStream().limit(MAX_ROWS).toList());
        return wrapper.toString();
    }
}
