package com.example.bigdata;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Timestamp extractor dla rekordu o formacie CSV:
 * date,film_id,user_id,rate
 * gdzie date ma format "YYYY-MM-DD".
 */
public class MyEventTimeExtractor implements TimestampExtractor {

    private final SimpleDateFormat sdf;

    public MyEventTimeExtractor() {
        // Ustawienie formatu zgodnie z wejściowymi danymi
        this.sdf = new SimpleDateFormat("yyyy-MM-dd");
        // Opcjonalnie:
        // this.sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        Object value = record.value();
        if (value instanceof String) {
            String line = (String) value;
            String[] parts = line.split(",");
            if (parts.length >= 1) {
                String dateStr = parts[0];
                try {
                    Date parsed = sdf.parse(dateStr);
                    return parsed.getTime();
                } catch (ParseException e) {
                    // jeśli nie uda się sparsować, zwracamy partitionTime
                    return partitionTime;
                }
            } else {
                // brak odpowiednich pól – fallback na partitionTime
                return partitionTime;
            }
        }
        // jeśli wartość nie jest Stringiem – fallback na partitionTime
        return partitionTime;
    }
}
