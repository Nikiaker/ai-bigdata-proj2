./kafka-topics.sh --create --bootstrap-server broker-1:19092 \
 --replication-factor 2 --partitions 3 --topic kafka-input

./kafka-topics.sh --create --bootstrap-server broker-1:19092 \
 --replication-factor 2 --partitions 3 --topic movie-titles

./kafka-topics.sh --create --bootstrap-server broker-1:19092 \
 --replication-factor 2 --partitions 3 --topic movie-stats

./kafka-topics.sh --create --bootstrap-server broker-1:19092 \
 --replication-factor 2 --partitions 3 --topic movie-alerts



# Delete the kafka-input topic if it exists
./kafka-topics.sh --delete --bootstrap-server broker-1:19092 \
 --topic kafka-input

./kafka-console-producer.sh --bootstrap-server broker-1:19092 --topic movie-titles < movie_titles.csv

java -cp /opt/kafka/libs/*:datafaker-1.4.0.jar:netflix-prize-producer.jar \
 com.example.bigdata.Producer netflix-prize-data 15 kafka-input 0 broker-1:19092,broker-2:19092

java -cp /opt/kafka/libs/*:netflix-prize-app.jar \
 com.example.bigdata.NetflixAnomalyDetection broker-1:19092 30 100 4.0 A


/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server broker-1:19092,broker-2:19092 \
  --topic kafka-input \
  --property print.key=true \
  --from-beginning