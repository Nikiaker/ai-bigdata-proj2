package com.example.bigdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.lang.reflect.Field;
import java.util.*;

public class JsonPOJOSerializer<T> implements Serializer<T> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Class<T> tClass;
    private Map<String, Object> schemaMap;  // przygotowany raz

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, ?> props, boolean isKey) {
        // Odczytaj klasę T przekazaną z JsonPOJOSerde
        tClass = (Class<T>) props.get("JsonPOJOClass");
        // Stwórz prosty opis schema
        schemaMap = new LinkedHashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        for (Field f : tClass.getDeclaredFields()) {
            f.setAccessible(true);
            Map<String, Object> fld = new HashMap<>();
            fld.put("field", f.getName());
            Class<?> type = f.getType();
            if (type == String.class)            fld.put("type", "string");
            else if (type == int.class || type == Integer.class
                    || type == long.class || type == Long.class)   fld.put("type", "int32");
            else if (type == double.class || type == Double.class
                    || type == float.class  || type == Float.class)  fld.put("type", "float32");
            else if (type == boolean.class|| type == Boolean.class)fld.put("type", "boolean");
            else fld.put("type", "object");
            fld.put("optional", true);
            fields.add(fld);
        }
        schemaMap.put("type", "struct");
        schemaMap.put("optional", false);
        schemaMap.put("version", 1);
        schemaMap.put("fields", fields);
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) return null;
        try {
            // Envelope: schema + payload
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("schema", schemaMap);
            envelope.put("payload", data);
            return objectMapper.writeValueAsBytes(envelope);
        } catch (Exception e) {
            throw new SerializationException("Error serializing Envelope JSON", e);
        }
    }

    @Override
    public void close() {}
}
