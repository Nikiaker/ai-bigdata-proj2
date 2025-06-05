package com.example.bigdata;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class NetflixAnomalyDetection {
    public static class InputScores {
        public int film_id;
        public String date;
        public int user_id;
        public int rate;

        public long getTimestampInMillis() {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date parsedDate = sdf.parse(this.date);
                return parsedDate.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
                return -1;
            }
        }
    }

    public static class MovieTitles {
        public  int id;
        public int year;
        public String title;
    }

    public  static class MovieStats {
        public int film_id;
        public  String title;
        public int number_of_rating;
        public int sum_of_ratings;
        public Set<Integer> unique_users;
    }

    public static class MovieAlerts{
        public int film_id;
        public String start_ts;
        public String end_ts;
        public String title;
        public int number_of_rating;
        public int average_rating;
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Four parameters are required: bootstrapServer D L O Delay(e.g., localhost:9092 30 100 4.0 A)");
            System.exit(0);
        }

        // Parse the arguments
        final String bootstrapServer = args[0];
        final int D = Integer.parseInt(args[1]);  // Period length in days
        final int L = Integer.parseInt(args[2]); // Minimum number of ratings
        final double O = Double.parseDouble(args[3]); // Minimum average rating
        final String delay = args[4];  // Delay mode: A or C


        // Set up the configuration.
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "netflix-anomaly-detection");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, MyEventTimeExtractor.class.getName());
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        // setting offset reset to earliest so that we can re-run the code with the same pre-loaded data
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Integer().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // Serdes for serializing and deserializing key and value from and to Kafka
        final Serde<String> stringSerde = Serdes.String();
        final Serde<Integer> integerSerde = Serdes.Integer();
        final Serde<MovieStats> movieStatsSerde = new JsonPOJOSerde<>(MovieStats.class);
        final Serde<MovieTitles> movieTitlesSerde = new JsonPOJOSerde<>(MovieTitles.class);
        final Serde<MovieAlerts> movieAlertsSerde = new JsonPOJOSerde<>(MovieAlerts.class);
        StreamsBuilder builder = new StreamsBuilder();

        // Creating the KTable for movie titles
        KTable<Integer, MovieTitles> movieTitlesStream = builder.table("movie-titles", Consumed.with(integerSerde, stringSerde))
                .mapValues(value -> {
                    String[] parts = value.split(",");
                    MovieTitles movieTitle = new MovieTitles();
                    movieTitle.id = Integer.parseInt(parts[0]);
                    movieTitle.year = Integer.parseInt(parts[1]);
                    movieTitle.title = parts[2];
                    return movieTitle;
                }, Materialized.with(integerSerde, movieTitlesSerde));

        // Creating the KStream for input scores
        KStream<Integer, InputScores> inputScoresStream = builder.stream("kafka-input", Consumed.with(integerSerde, stringSerde))
                .mapValues(value -> {
                    String[] parts = value.split(",");
                    InputScores inputScore = new InputScores();
                    inputScore.film_id = Integer.parseInt(parts[1]);
                    inputScore.date = parts[0];
                    inputScore.user_id = Integer.parseInt(parts[2]);
                    inputScore.rate = Integer.parseInt(parts[3]);
                    return inputScore;
                }).selectKey((key, value) -> value.film_id);

        //Joining the KStream with the KTable
        KStream<Integer, String> movieStatsStream = inputScoresStream
                .join(movieTitlesStream, (inputScore, movieTitle) -> inputScore.date + "," + inputScore.user_id + "," + movieTitle.title + "," + inputScore.rate)
                .selectKey((key, value) -> {
                    String[] parts = value.split(",");
                    return Integer.parseInt(parts[1]);
                });

        // Creating the KTable for movie stats
        KTable<Windowed<Integer>, MovieStats> movieStatsTable = movieStatsStream
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(D)))
                .aggregate(
                        () -> new MovieStats(),
                        (key, value, aggregate) -> {
                            String[] parts = value.split(",");
                            aggregate.film_id = Integer.parseInt(parts[1]);
                            aggregate.title = parts[2];
                            aggregate.number_of_rating++;
                            aggregate.sum_of_ratings += Integer.parseInt(parts[4]);
                            aggregate.unique_users.add(Integer.parseInt(parts[3]));
                            return aggregate;
                        },
                        Materialized.with(integerSerde, movieStatsSerde)
                );

        //
        if (delay.equals("A")) {
            movieStatsTable.toStream().to("movie-stats", Produced.with(WindowedSerdes.sessionWindowedSerdeFrom(Integer.class), movieStatsSerde));
        }

        if (delay.equals("C")) {
            // Creating the KStream for movie alerts
            KStream<Integer, MovieAlerts> movieAlertsStream = movieStatsTable
                .toStream()
                .filter((key, value) -> value.number_of_rating >= L && (double) value.sum_of_ratings / value.number_of_rating >= O)
                .map((key, value) -> {
                    MovieAlerts movieAlert = new MovieAlerts();
                    movieAlert.film_id = value.film_id;
                    movieAlert.start_ts = key.window().startTime().toString();
                    movieAlert.end_ts = key.window().endTime().toString();
                    movieAlert.title = value.title;
                    movieAlert.number_of_rating = value.number_of_rating;
                    movieAlert.average_rating = value.sum_of_ratings / value.number_of_rating;
                    return KeyValue.pair(value.film_id, movieAlert);
                });

        movieAlertsStream.to("movie-alerts", Produced.with(integerSerde,movieAlertsSerde));
        }

        final Topology topology = builder.build();
        System.out.println(topology.describe());

        try (KafkaStreams streams = new KafkaStreams(topology, props)) {
            final CountDownLatch latch = new CountDownLatch(1);
            // attach shutdown handler to catch control-c
            Runtime.getRuntime().addShutdownHook(new Thread("streams-pipe-shutdown-hook") {
                @Override
                public void run() {
                    streams.close();
                    latch.countDown();
                }
            });
            try {
                streams.start();
                latch.await();
            } catch (Throwable e) {
                System.exit(1);
            }
        }
        System.exit(0);
    }

}