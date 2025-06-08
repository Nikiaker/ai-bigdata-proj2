wget https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/\
8.0.33/mysql-connector-j-8.0.33.jar

cp mysql-connector-j-8.0.33.jar /opt/kafka/libs/

wget https://packages.confluent.io/maven/io/confluent/kafka-connect-jdbc/\
10.7.0/kafka-connect-jdbc-10.7.0.jar

mkdir /opt/kafka/plugin
cp kafka-connect-jdbc-10.7.0.jar /opt/kafka/plugin/

cp connect-standalone.properties /opt/kafka/config/
cp connect-jdbc-sink.properties /opt/kafka/config/