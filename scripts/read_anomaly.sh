/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server broker-1:19092,broker-2:19092 \
  --topic movie-alerts \
  --from-beginning \
  --property print.key=true