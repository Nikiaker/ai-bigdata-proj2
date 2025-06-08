### Pobranie zestawu danych (jeśli jeszcze nie masz)
```
wget https://www.cs.put.poznan.pl/kjankiewicz/bigdata/stream_project/movie_titles.csv
wget https://www.cs.put.poznan.pl/kjankiewicz/bigdata/stream_project/netflix-prize-data.zip
```

### Uruchomienie środowiska w Docker
```
git clone https://github.com/BigDataStreamProcessing/ApacheKafkaDocker.git
cd ApacheKafkaDocker
docker compose up -d
cd ..
```

### Uruchomienie kontenera z bazą MySQL
```
mkdir datadir

docker run --name mymysql -v datadir:/var/lib/mysql -p 6033:3306 \
 -e MYSQL_ROOT_PASSWORD=my-secret-pw -d mysql:debian
```

### Włączenie kontenerów do tej samej siecie
```
docker network connect kafka-net mymysql
docker network connect kafka-net broker-1
```

### Instalacja potrzebynch bibliotek na Dockerze
```
docker exec --workdir /root -u 0 -it broker-1 /bin/bash
apk add libstdc++
exit
```

### Podłączenie się do klastra (helper)
```
# zwykły użytkownik (odpalanie skryptów)
docker exec --workdir /opt/kafka/bin/ -it broker-1 bash
# zwykły użytkownik (katalog domowy)
docker exec --workdir /home/appuser -it broker-1 bash
# root
docker exec -u 0 -it broker-1 /bin/bash
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
```

### Podłączenie się do klastra
Pora dostać się do klastra do katalogu głównego, gdzie są wszystkie skrypty
```
docker exec --workdir /home/appuser -it broker-1 bash
chmod +x *.sh
```

## Odpalenie skryptów
Będąc podłączonym do klastra w folderze domowym /home/appuser odpalmy wszystkie skrypty w danej kolejności:
### Stworzenie tematów kafki
```
./create_topics.sh
```
### Uruchomienie przetwarzania (ETL+Anomalie)
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
```
./run_producer.sh <sleep>
```
gdzie:
- sleep - czas w sekundach na ile ma zasypiać producent

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

# TODO
- [x] - Działający producer (wygląda że działa i się odpala)
- [x] - napisać skrypt tworzący tematy kafki
- [x] - w skrypcie ma się znaleźć uzupełnienie pierwszego tematu kafki
- [] - Działający program przetwarzający (ETL+Anomalie)
- [x] - napisać skrypt uruchomiający przetwarzanie
- [] - napisać skrypt tworzący bazkę
- [] - napisać skrypt odczytujący wyniki
- [x] - napisać skrypt resetujący wszystko
- [] - Działająca bazka danych (MySQL)