### Uruchomienie środowiska w Docker
```
docker compose up -d
```
### Podłączenie się do klastra
```
# zwykły użytkownik (odpalanie skryptów)
docker exec --workdir /opt/kafka/bin/ -it broker-1 bash
# przetwarzanie strumieni (techniczny)
docker exec --workdir /home/appuser -it broker-1 bash
# root
docker exec -u 0 -it broker-1 /bin/bash
```

### Kopiowanie na dockera
```
docker cp netflix-prize-producer.jar broker-1:/opt/kafka/bin/
docker cp movie_titles.csv broker-1:/opt/kafka/bin/
docker cp netflix-prize-data.zip broker-1:/opt/kafka/bin/
docker cp netflix-prize-app.jar broker-1:/home/appuser
```

# TODO
- [x] - Działający producer (wygląda że działa i się odpala)
- [] - napisać skrypt tworzący tematy kafki
- [] - w skrypcie ma się znaleźć uzupełnienie pierwszego tematu kafki
- [] - Działający program przetwarzający (ETL+Anomalie)
- [] - napisać skrypt uruchomiający przetwarzanie
- [] - napisać skrypt tworzący bazkę
- [] - napisać skrypt odczytujący wyniki
- [] - napisać skrypt resetujący wszystko
- [] - Działająca bazka danych (MySQL)