package com.yarnshipment.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CloudEventHeadersTest {

    private static final UUID EVENT_ID = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1");
    private static final String TOPIC = "shipment.events.v1";
    private static final String SOURCE = "/yarn-shipment/shipment-service";

    @Test
    @DisplayName("Outbox satırı ikili kip CloudEvents mesajına çevrilir; anahtar sevkiyat kimliğidir")
    void shouldBuildBinaryModeRecord() {
        // when
        ProducerRecord<String, String> record = CloudEventHeaders.toRecord(TOPIC, SOURCE, TestEvents.event(EVENT_ID));

        // then
        assertThat(record.topic()).isEqualTo(TOPIC);
        assertThat(record.key()).isEqualTo(TestEvents.SHIPMENT_ID.toString());
        assertThat(record.value()).isEqualTo("{\"bagCount\":40}");
        assertThat(header(record, CloudEventHeaders.ID)).isEqualTo(EVENT_ID.toString());
        assertThat(header(record, CloudEventHeaders.SOURCE)).isEqualTo(SOURCE);
        assertThat(header(record, CloudEventHeaders.TYPE)).isEqualTo(TestEvents.TYPE);
        assertThat(header(record, CloudEventHeaders.TIME)).isEqualTo("2026-10-21T09:00:00Z");
        assertThat(header(record, CloudEventHeaders.SPEC_VERSION)).isEqualTo("1.0");
        assertThat(header(record, "actorId")).isEqualTo("user-1");
    }

    @Test
    @DisplayName("Tüketici aynı mesajdan zarf alanlarını ve gövdeyi geri okur")
    void shouldReadWhatWasWritten() {
        // given
        ConsumerRecord<String, String> consumed =
                consumed(CloudEventHeaders.toRecord(TOPIC, SOURCE, TestEvents.event(EVENT_ID)));

        // when
        IncomingEvent event = CloudEventHeaders.read(consumed);

        // then
        assertThat(event.id()).isEqualTo(EVENT_ID);
        assertThat(event.type()).isEqualTo(TestEvents.TYPE);
        assertThat(event.subject()).isEqualTo(TestEvents.SHIPMENT_ID);
        assertThat(event.time()).isEqualTo(TestEvents.AT);
        assertThat(event.actorId()).isEqualTo("user-1");
        assertThat(event.data()).isEqualTo("{\"bagCount\":40}");
    }

    @Test
    @DisplayName("Zorunlu başlığı olmayan mesaj kalıcı hata verir")
    void shouldRejectMissingHeader() {
        // given
        ConsumerRecord<String, String> consumed = new ConsumerRecord<>(TOPIC, 0, 0L, "key", "{}");

        // when / then
        assertThatThrownBy(() -> CloudEventHeaders.read(consumed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CloudEventHeaders.ID);
    }

    private static ConsumerRecord<String, String> consumed(ProducerRecord<String, String> produced) {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(produced.topic(), 0, 0L, produced.key(), produced.value());
        for (Header header : produced.headers()) {
            record.headers().add(header);
        }
        return record;
    }

    private static String header(ProducerRecord<String, String> record, String key) {
        return new String(record.headers().lastHeader(key).value(), StandardCharsets.UTF_8);
    }
}
