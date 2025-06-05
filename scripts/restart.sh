/opt/kafka/bin/kafka-topics.sh --delete --bootstrap-server broker-1:19092 \
 --topic kafka-input

/opt/kafka/bin/kafka-topics.sh --delete --bootstrap-server broker-1:19092 \
 --topic movie-titles

/opt/kafka/bin/kafka-topics.sh --delete --bootstrap-server broker-1:19092 \
 --topic movie-stats

/opt/kafka/bin/kafka-topics.sh --delete --bootstrap-server broker-1:19092 \
 --topic movie-alerts

rm -r netflix-prize-data

./create_topics.sh