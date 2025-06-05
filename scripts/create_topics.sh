/opt/kafka/bin/kafka-topics.sh --create --bootstrap-server broker-1:19092 \
 --replication-factor 2 --partitions 3 --topic kafka-input

/opt/kafka/bin/kafka-topics.sh --create --bootstrap-server broker-1:19092 \
 --replication-factor 2 --partitions 3 --topic movie-titles

/opt/kafka/bin/kafka-topics.sh --create --bootstrap-server broker-1:19092 \
 --replication-factor 2 --partitions 3 --topic movie-stats

/opt/kafka/bin/kafka-topics.sh --create --bootstrap-server broker-1:19092 \
 --replication-factor 2 --partitions 3 --topic movie-alerts

/opt/kafka/bin/kafka-console-producer.sh --bootstrap-server broker-1:19092 --topic movie-titles < movie_titles.csv

unzip netflix-prize-data.zip