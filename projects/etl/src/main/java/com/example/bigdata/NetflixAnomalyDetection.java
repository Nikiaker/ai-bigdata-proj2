package com.example.bigdata;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.TimestampExtractor;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;

public class NetflixAnomalyDetection {

    // --- POJOs --------------------------------------------------------
    public static class InputScore {
        public String date;     // "YYYY-MM-DD"
        public int    filmId;
        public int    userId;
        public int    rate;
    }

    public static class MovieTitle {
        public int    id;
        public int    year;
        public String title;
    }

    public static class MonthlyStats {
        public int    filmId;
        public String title;
        public long   count;
        public long   sum;
        public long   uniques;
    }

    public static class MovieAlert {
        public int     filmId;
        public String  title;
        public String windowStart;
        public String windowEnd;
        public long    count;
        public double  avg;
    }

    // --- TimestampExtractor (event‐time z pola date) -----------------
    public static class DateExtractor implements TimestampExtractor {
        private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        @Override
        public long extract(ConsumerRecord<Object, Object> rec, long partitionTime) {
            if (rec.value() instanceof String) {
                String[] parts = ((String)rec.value()).split(",");
                try {
                    Date d = fmt.parse(parts[0]);
                    return d.getTime();
                } catch(ParseException e) {
                    // parsowanie nieudane → fallback
                }
            }
            return partitionTime;
        }
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("Usage: <bootstrap> <D-days> <L-count> <O-avg> <delay=A|C>");
            System.exit(1);
        }
        final String bootstrap = args[0];
        final int    D         = Integer.parseInt(args[1]);
        final long   L         = Long.parseLong(args[2]);
        final double O         = Double.parseDouble(args[3]);
        final boolean complete = args[4].equalsIgnoreCase("C");

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG,       "netflix-anomaly");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,    bootstrap);
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, DateExtractor.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,   "earliest");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,   Serdes.Integer().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        final Serde<String>           stringSerde    = Serdes.String();
        final Serde<Integer>          intSerde       = Serdes.Integer();
        final JsonPOJOSerde<InputScore> inputSerde   = new JsonPOJOSerde<>(InputScore.class);
        final JsonPOJOSerde<MovieTitle> titleSerde   = new JsonPOJOSerde<>(MovieTitle.class);
        final JsonPOJOSerde<MonthlyStats> statsSerde = new JsonPOJOSerde<>(MonthlyStats.class);
        final JsonPOJOSerde<MovieAlert>   alertSerde  = new JsonPOJOSerde<>(MovieAlert.class);

        StreamsBuilder builder = new StreamsBuilder();

        // --- 1) static KTable z tytułami filmów ------------------------
        KTable<Integer,MovieTitle> titles = builder
                .table("movie-titles",
                        Consumed.with(intSerde, stringSerde))
                .mapValues(raw -> {
                    String[] p = raw.split(",");
                    MovieTitle mt = new MovieTitle();
                    mt.id    = Integer.parseInt(p[0]);
                    mt.year  = Integer.parseInt(p[1]);
                    mt.title = p[2];
                    return mt;
                }, Materialized.with(intSerde, titleSerde));

        // --- 2) źródło ocen jako InputScore + klucz=filmId ------------
        KStream<Integer,InputScore> scores = builder
                .stream("kafka-input",
                        Consumed.with(stringSerde, stringSerde))
                .mapValues(raw -> {
                    String[] p = raw.split(",");
                    InputScore s = new InputScore();
                    s.date   = p[0];
                    s.filmId = Integer.parseInt(p[1]);
                    s.userId = Integer.parseInt(p[2]);
                    s.rate   = Integer.parseInt(p[3]);
                    return s;
                })
                .selectKey((x, s) -> s.filmId, Named.as("select-filmId"));

        // --- 3) miesięczna agregacja ETL → topic "etl-monthly-stats" ---
        scores
                .groupByKey(Grouped.with(intSerde, inputSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(30)).advanceBy(Duration.ofDays(30)))
                .aggregate(
                        () -> {
                            MonthlyStats m = new MonthlyStats();
                            m.count = 0; m.sum = 0; m.uniques = 0;
                            return m;
                        },
                        (filmId, s, agg) -> {
                            agg.filmId = filmId;
                            agg.count++;
                            agg.sum    += s.rate;
                            // prosta unikalność: liczymy każdy rating jako unikat
                            // zaawansowanie: można trzymać Set<userId> w stanie
                            agg.uniques++;
                            return agg;
                        },
                        Materialized.with(intSerde, statsSerde)
                )
                // dopełniamy tytułem
                .toStream((winKey, stats) -> stats.filmId, Named.as("toStringKey"))
                .leftJoin(titles,
                        (stats, mt) -> {
                            stats.title = (mt!=null?mt.title:"UNKNOWN");
                            return stats;
                        },
                        Joined.with(intSerde, statsSerde, titleSerde)
                )
                .to("etl-monthly-stats", Produced.with(intSerde, statsSerde));

        // --- 4) Wykrywanie anomalii: D‐dniowe okno przesuwane co dzień ---
        scores
                .groupByKey(Grouped.with(intSerde, inputSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(D)).advanceBy(Duration.ofDays(1)))
                .aggregate(
                        () -> {
                            MonthlyStats m = new MonthlyStats();
                            m.count = 0; m.sum = 0; m.uniques = 0;
                            return m;
                        },
                        (filmId, s, agg) -> {
                            agg.filmId = filmId;
                            agg.count++;
                            agg.sum    += s.rate;
                            agg.uniques++;
                            return agg;
                        },
                        Materialized.with(intSerde, statsSerde)
                )
                // w trybie C czekamy aż okno się zamknie i dopiero emitujemy:
                .suppress(
                        complete
                                ? Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded())
                                : Suppressed.untilTimeLimit(Duration.ZERO, Suppressed.BufferConfig.unbounded())
                )
                .toStream()
                // filtrujemy według L i O
                .filter((winKey, agg) -> agg.count >= L
                        && ((double)agg.sum/agg.count) >= O)
                // ubogacamy tytułem
                .map((winKey, agg) -> {
                    MovieAlert alert = new MovieAlert();
                    alert.filmId     = winKey.key();
                    alert.title      = agg.title;
                    alert.windowStart= winKey.window().startTime().toString();
                    alert.windowEnd  = winKey.window().endTime().toString();
                    alert.count      = agg.count;
                    alert.avg        = (double)agg.sum/agg.count;
                    return KeyValue.pair(alert.filmId, alert);
                })
                .to("movie-alerts", Produced.with(intSerde, alertSerde));

        // --- 5) startujemy streams -----------------------------------
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.setUncaughtExceptionHandler((Throwable e) -> {
            e.printStackTrace();
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION;
        });
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
