package com.yarnshipment.assistant.adapter.out.data;

import java.util.Optional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Sevkiyatın kimliğini numarasından bulur; kullanıcının görebildiği son sevkiyatlar arasında arar. */
final class ShipmentFinder {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 5;
    private static final String LIST_PATH = "/api/v1/shipments?size=" + PAGE_SIZE + "&page=";

    /** Bir adresi okuyup metin döndürür (yetki ve hata durumları metin olarak gelir). */
    @FunctionalInterface
    interface Reader {
        String get(String pathAndQuery);
    }

    private final JsonMapper json;

    ShipmentFinder(JsonMapper json) {
        this.json = json;
    }

    /** Kimlik bulunursa döner; bulunamazsa boş. */
    Optional<String> find(Reader reader, String shipmentNo) {
        for (int page = 0; page < MAX_PAGES; page++) {
            JsonNode items = parse(reader.get(LIST_PATH + page)).path("items");
            Optional<String> found = items.valueStream()
                    .filter(item ->
                            shipmentNo.equalsIgnoreCase(item.path("shipmentNo").asString("")))
                    .map(item -> item.path("shipmentId").asString(""))
                    .findFirst();
            if (found.isPresent() || items.size() < PAGE_SIZE) {
                return found;
            }
        }
        return Optional.empty();
    }

    private JsonNode parse(String body) {
        try {
            return json.readTree(body);
        } catch (JacksonException e) {
            // Yetki ya da hata metni JSON değildir; liste boş sayılır.
            return json.createObjectNode();
        }
    }
}
