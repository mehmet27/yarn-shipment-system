package com.yarnshipment.assistant.adapter.out.data;

import java.util.Map;
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

    /** Liste ya da içindeki uzun listeler {@value #MAX_ROWS} satırı geçerse ilk satırları ve toplam sayıyı verir. */
    String limit(String body) {
        JsonNode node = json.readTree(body);
        if (node.isArray()) {
            return node.size() <= MAX_ROWS ? body : shortened(node).toString();
        }
        if (!node.isObject() || node.valueStream().noneMatch(field -> field.isArray() && field.size() > MAX_ROWS)) {
            return body;
        }
        ObjectNode copy = json.createObjectNode();
        for (Map.Entry<String, JsonNode> field : node.properties()) {
            JsonNode value = field.getValue();
            copy.set(field.getKey(), value.isArray() && value.size() > MAX_ROWS ? shortened(value) : value);
        }
        return copy.toString();
    }

    private ObjectNode shortened(JsonNode rows) {
        ObjectNode wrapper = json.createObjectNode();
        wrapper.put(
                "not",
                "Liste kısaltıldı: toplam " + rows.size() + " satırın ilk " + MAX_ROWS
                        + " tanesi var. Toplam için özet kaynaklarını kullan ya da tarih aralığını daralt.");
        wrapper.put("toplamSatir", rows.size());
        wrapper.putArray("satirlar").addAll(rows.valueStream().limit(MAX_ROWS).toList());
        return wrapper;
    }
}
