# Minimal Algorithm

This is implementation of algorithm discussed in article ["Towards minimal algorithms for big data analytics with spreadsheets"](https://dl.acm.org/doi/abs/10.1145/3070607.3075961). As input takes data in CSV file - coordinates (integers) of points 2D/3D (with small customization can take arbitrary number of dimensions). There two types of points: 1. data points; 2. query points. For each query point algorithm calculates how many there are data points that are "<=" on every dimension. List of query points with answers are in output. It is implemented in scala and hadoop. There are also simple ansible and bash scripts to make life easier. If you want to test you should have at least 3 (linux) computers - 1 hadoop master and 2 hadoop workers.

Code tested with:
* linux x86_64
* jdk-8u241-linux-x64
* hadoop-2.10.0
* spark-2.4.5-bin-without-hadoop-scala-2.12

Notes: My ansible script assumes that hadoop, java and spark archives are in `*.tar.xz` format. You may want to modify ansible script or repackage archives. And more importantly fix URLs! You will want to compile scala code using sbt. This step is skipped in instruction but it's so generic you should not have problems googling it.

Below is my original Readme in polish.

# --- Original Readme ---

## Konfiguracja
Przed uruchomieniem hadoopa/sparka trzeba wyedytować kilka plików.

### `ansible/hadoop.yaml`
Trzeba zmienić nazwę użytkownika (z `kotoko`) na swoją. Wszystkie wystąpienia w „pre_tasks”.

Dodatkowo jeśli w systemie nie ma zainstalowanego programu `zsync`, to trzeba zmienić sposób pobierania plików. W kodzie są zakomentowane linijki, które korzystają ze zintegrowanego w ansiblu pobierania. Należy zakomentować taski pobierające zsync-iem i odkomentować taski pobierające ansiblem. Nie testowałem, ale powinny działać ;)

### `ansible/inventory`
Trzeba wpisać adresy IP slave-ów i mastera. Master musi być tylko jeden. Master powinien być w sekcji mastera i nie powinno go być w sekcji slave.

### `scripts/paths.sh`
Trzeba zmienić nazwę użytkownika (z `kotoko`) na swoją. Warto się upewnić, że ścieżka do jar-a jest prawidłowa. Ścieżka względna startuje z wnętrza katalogu `scripts/`.

## Uruchomienie hadoopa/sparka

Do pobierania i uruchamiania hadoopa/sparka jest przygotowany skrypt w ansible. Ponadto jest kilka prostych skryptów w bashu, żeby ułatwić sobie życie.

1. Trzeba mieć zainstalowanego ansible. Najłatwiej stworzyć sobie virtualenva, aktywować go i zainstalować ansible przez pip.
    ```
    virtualenv -p python3.7 venv3.7/
    source venv3.7/bin/activate
    pip install ansible
    ```
    W pliku `scripts/ansible.sh` zaktualizuj ścieżkę do virtualenva. Jeśli nie korzystasz z virtualenva, to usuń linię aktywującą virtualenva z tego skryptu.

2. Teraz ręcznie zaloguj się na komputery z pliku `ansible/inventory` przez ssh, tak żeby komputery zcachowały sobie hasło/token czy co tam potrzebują i żeby dalej można się było logować przy pomocy klucza (może być w ssh-agent) już bez pytania o hasło.

3. Ściągnij i rozpakuj binarkę hadoopa. Sformatuj również „dysk” hdfs.
    ```
    scripts/ansible.sh setup
    ```

4. Uruchom instancję hadoopa/sparka.
    ```
    scripts/ansible.sh start
    ```

Już!

## Uruchomienie programu

Przykład uruchomienia programu na jakimś wejściu.
```
scripts/run-solution.sh 2 path/to/in2D.csv
scripts/run-solution.sh 3 path/to/in3D.csv
```

Pierwszy argument to liczba wymiarów (2 albo 3). Drugi argument to ścieżka do wejściowego pliku csv. Skrypt skopiuje csv do hdfsa, uruchomi program (program zapisze wynik w hdfs-ie), skopiuje wynik z hdfsa do katalogu tymczasowego i na końcu wypisze zawartość pliku wynikowego na standardowe wyjście.

Format wejściowego pliku csv w wersji 2D:
```
D;1;1
D;2;2
Q;99;99
```
Separatorem jest średnik. Pierwsza litera to wielkie D albo wielkie Q. Oznaczają czy dany punkt jest daną, czy zapytaniem. Kolejne liczby całkowite to współrzędne.

## Przykłady

Kilka przykładów jest w katalogu `sample_data/`. Ponadto są tam również skrypty w pythonie, które obliczają prawidłową odpowiedź jak i generują losowe dane z zapytaniami.

Uruchomiłem test `2D/rand1.csv` i po posortowaniu wyników programy w sparku jak i w pythonie dały identyczne odpowiedzi. Zakładam, że program działa poprawnie.

## Wyłączenie hadoopa i usuwanie plików

1. Wyłącz hadoopa/sparka.
    ```
    scripts/ansible.sh stop
    ```

2. Usuń wszystkie stworzone pliki.
    ```
    scripts/ansible.sh clean
    ```
