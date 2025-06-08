package com.example.bigdata;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/*
Source:
https://github.com/apache/kafka/blob/2.0/streams/examples/src/main/java/org/apache/kafka/streams/examples/pageview/JsonPOJODeserializer.java
 */

public class JsonPOJODeserializer<T> implements Deserializer<T> {
    private ObjectMapper objectMapper;
    private Class<T> tClass;

    public JsonPOJODeserializer() {
        objectMapper = new ObjectMapper();
        // ignorujemy nieznane properties
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
        this.tClass = (Class<T>) props.get("JsonPOJOClass");
    }

    @Override
    public T deserialize(String topic, byte[] bytes) {
        if (bytes == null) return null;
        try {
            // Parsujemy drzewo JSON
            JsonNode root = objectMapper.readTree(bytes);
            // Wyciągamy payload
            JsonNode payload = (root.has("payload") ? root.get("payload") : root);
            // Deserializujemy payload do T
            return objectMapper.treeToValue(payload, tClass);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing JSON", e);
        }
    }

    @Override
    public void close() { }
}