java -cp /opt/kafka/libs/*:netflix-prize-producer.jar \
 com.example.bigdata.Producer netflix-prize-data $1 kafka-input 1 broker-1:19092,broker-2:19092