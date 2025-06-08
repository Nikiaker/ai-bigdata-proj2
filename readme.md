# Tworzenie obrazów Dockerowych

### Pobranie zestawu danych (jeśli jeszcze nie masz)
```
mkdir data
wget https://www.cs.put.poznan.pl/kjankiewicz/bigdata/stream_project/movie_titles.csv data/
wget https://www.cs.put.poznan.pl/kjankiewicz/bigdata/stream_project/netflix-prize-data.zip data/
```

### Postawienie kontenerów Kafka
```
git clone https://github.com/BigDataStreamProcessing/ApacheKafkaDocker.git
cd ApacheKafkaDocker
docker compose up -d
cd ..
```

### Stworzenie kontenera z bazą MySQL
```
mkdir datadir

docker run --name mymysql -v datadir:/var/lib/mysql -p 6033:3306 \
 -e MYSQL_ROOT_PASSWORD=my-secret-pw -d mysql:debian
```

### Włączenie kontenerów do tej samej sieci
```
docker network connect kafka-net mymysql
docker network connect kafka-net broker-1
```

# Konfiguracja kontenera MySQL
### Kopiowanie potrzebnych skryptów na dockera MySQL
```
docker cp scripts/create_db.sh mymysql:/root
docker cp sql/create_user.sql mymysql:/root
docker cp sql/create_tables.sql mymysql:/root
```

### Podłączenie się do dockera z MySQL
```
docker exec --workdir /root -it mymysql bash
chmod +x *.sh
```

### Uruchomienie skryptu tworzącego użytkownika, bazę i table w MySQL
```
./create_db.sh
exit
```

# Konfiguracja głównego kontenera Kafka
### Instalacja potrzebynch bibliotek na Dockerze
```
docker exec --workdir /root -u 0 -it broker-1 /bin/bash
apk add libstdc++
exit
```

### Kopiowanie na dockera
```
docker cp jars/netflix-prize-producer.jar broker-1:/home/appuser
docker cp jars/netflix-prize-app.jar broker-1:/home/appuser

docker cp data/movie_titles.csv broker-1:/home/appuser
docker cp data/netflix-prize-data.zip broker-1:/home/appuser

docker cp properties/connect-standalone.properties broker-1:/home/appuser
docker cp properties/connect-jdbc-sink.properties broker-1:/home/appuser

docker cp scripts/create_topics.sh broker-1:/home/appuser
docker cp scripts/restart.sh broker-1:/home/appuser
docker cp scripts/setup_db.sh broker-1:/home/appuser
docker cp scripts/run_processing.sh broker-1:/home/appuser
docker cp scripts/run_connector.sh broker-1:/home/appuser
docker cp scripts/run_producer.sh broker-1:/home/appuser

docker cp scripts/read_etl.sh broker-1:/home/appuser
docker cp scripts/read_anomaly.sh broker-1:/home/appuser
```

### Podłączenie się do klastra
Pora dostać się do klastra do katalogu głównego, gdzie są wszystkie skrypty
```
docker exec --workdir /home/appuser -it broker-1 bash
chmod +x *.sh
```

### Stworzenie tematów kafki
```
./create_topics.sh
```

### Konfiguracja bibliotek do połączenia z bazą MySQL
```
./setup_db.sh
```

# Odpalenie skryptów
Miejmy otwarte 3 terminale połączone z dockerem w katalogu /home/appuser:
```
docker exec --workdir /home/appuser -it broker-1 bash
```

### Uruchomienie przetwarzania (ETL+Anomalie)
W terminalu-1 uruchom przetwarzanie
```
./run_processing.sh <D> <L> <O> <delay>
```
gdzie:
- D - długość okresu czasu wyrażona w dniach
- L - liczba ocen (minimalna)
- O - średnia ocenę (minimalna)
- delay - tryb działania (A - najmniejsze możliwe opóźnienie, C - najszybciej jak się da)

np:
```
./run_processing.sh 30 100 4.0 A
```
### Uruchomienie producenta
W terminalu-2 uruchom producenta
```
./run_producer.sh <sleep>
```
gdzie:
- sleep - czas w sekundach na ile ma zasypiać producent

np:
```
./run_producer.sh 15
```

### Odczytywanie wyników
Przy pomocy skryptów read_etl.sh lub read_anomaly.sh odczytuj wyniki na terminalu-3
```
./read_etl.sh
```

```
./read_anomaly.sh
```