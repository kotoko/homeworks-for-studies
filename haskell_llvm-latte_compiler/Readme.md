# Latte compiler

This is simple Latte compiler. Produces llvm code + bytecode. Code is written in haskell.

Original [task description is here](https://www.mimuw.edu.pl/~ben/Zajecia/Mrj2019/Latte/).

Build requirements:
* ghc
* haskell libraries
    * mtl
    * filepath
    * temporary
    * deepseq

Below is original Readme in polish.

# --- Original Readme ---

Użycie
======
Po skompilowaniu poleceniem make w głównym katalogu pojawi się binarka. Aby skompilować kod trzeba podać ścieżkę do pliku jako pierwszy parametr. Binarka obsługuje flagę --help.

Jeśli ścieżka do pliku to -, kompilator wczytuje kod ze standardowego wejścia i wypisuje skompilowany kod na standardowe wyjście wraz z drzewem abstrakcyjnym w komentarzu. Dodatkowo później kompiluje kod llvm w katalogu tymczasowym i zwraca informację czy się udało.

Jeśli ścieżka do pliku nie jest -, kompilator tworzy pliki *.ll oraz *.bc.


Biblioteki
==========
Użyłem następujących haskelowych bibliotek:
* mtl       - obsługa monad
* filepath  - manipulacja ścieżkami do plików
* temporary - tworzenie plików tymczasowych
* deepseq   - gorliwa ewaluacja (wczytanie zawartości pliku *.lat na raz)


Pliki i katalogi
================
* src/           - kod programu napisny ręcznie
* src_generated/ - kod programu wygenerowany przez bnfc
* lib/           - pliki pomocnicze (runtime)
* bin/           - katalog, w którym jest kompilowany program
* docs/          - katalog z dokumentacją
* cabal.project  - plik, który istnieje tylko po to, żeby cabal na studentsie nie rzucał błędów


Makefile
========
Przydatne polecenia make:
* all        - wyczyszczenie, pobranie bibliotek, ponowne skompilowanie programu, skopiowanie binarek
* release    - skompilowanie programu z flagą -O2
* debug      - skompilowanie programu z ostrzeżeniami i bez -O2
* copyBinary - skopiowanie binarek z katalogu bin/ do katalogu głównego
* clean      - usunięcie wszystkich plików stworzonych podczas komilacji
* doc        - wygenerowanie dokumentacji stworzonej przez bnfc


Runtime
=======
Plik w języku C znajduje się w lib/runtime.c. Plik lib/runtime.ll jest wygenerowany przy pomocy clanga i później naniesione są drobne poprawki. Aby wygenerować plik jeszcze raz:

1. clang -O3 -S -emit-llvm -std=c17 runtime.c
2. Zmień parametr w printBoolean() z i32 na i1.
3. Zmień wynik w strEqual() z i32 na i1.


Opis
====
Napisałem kompilator Latte do języka LLVM IR.

Uproszczony przepływ sterowanie wygląda tak: Main.hs -> Compiler.hs -> ParserGlue.hs -> Frontend.hs -> Backend.hs. Ciekawe rzeczy dzieją się w Frontend.hs oraz Backend.hs.

Do obsługi wypisywania numerów linii w komunikatach o błędach wykorzystałem binarkę bnfc ze students, która potrafi zamienić wszystkie typy na funktory. Oficjalne numery linii w najnowszym bnfc były dla mnie niewystarczające, bo tam można było dostać numer linii tylko w miejscach gdzie pojawia się nazwa zmiennej/funkcji.

Frontend. Przerabiam drzewo abstrakcyjne kilka razy na nowe drzewo abstrakcyjne.
1. Dodaję do if-ów i while-ów bloki jeśli ich nie miały, żeby ułatwić sobie pracę.
2. Wykonuję optymalizację pod test "if(true)". Robię to osobno, bo chciałem najpierw wyłapać błędy, a dopiero później optymalizować zhardkodowane wyrażenia.
3. Sprawdzam czy nie ma duplikatów funkcji oraz czy istnieje funkcja main().
4. Liczę i sprawdzam typy wyrażeń. W tym momencie generuję drzewo abstrakcyjne w nowym typie (AbsL -> AbsT). Ten typ z typami jest prawie taki sam, ale dodatkowo pamięta typy wyrażeń w sobie, żeby mieć je później pod ręką.
5. Sprawdzam czy nie ma zduplikowanych zmiennych w obrębie jednego bloku.
6. Sprawdzan czy nie zmiennych typu void.
7. Obliczam zhardkodowane wyrażenia i upraszczam "if(true)", "while(false)".
8. Sprawdzam czy każda funkcja kończy się instrukcją "return". Usuwam martwy kod po return-ie.

Backend.
1. Nadaję unikatowe nazwy zmiennom. Po pierwsze, żeby pozbyć się przesłaniania zmiennych. Po drugie, żeby mieć pewność, że znaki w nazwie zmiennej są akceptowane przez LLVM-a.
2. Zapamiętuje wszystkie zhardkodowane napisy, żeby je zdefiniować na samym początku, żeby móc ich użyć później w kodzie. Tworzę nazwy zmiennych dla tych napisów.
3. Zapamiętuję też typy wszystkich zmiennych. (W jednym miejscu nie miałem skąd wziąć informacji o typie i musiałem to dodać).
4. Generuję napisy.
5. Generuję deklaracje funkcji wbudowanych w język.
6. Generuję definicje funkcji napisanych przez programistę.

Starałem się, żeby w wygenerowanym kodzie możliwe rzadko korzystać z pamięci i preferować wykorzystanie rejestrów. Tak na prawdę korzystam z pamięci tylko gdy pojawi się "if" lub "while". W związku z tym czasem może się zdarzyć, że jakieś wyrażenie nie generuje kodu LLVM, bo kompilator sprytnie podstawi coś lepszego w miejscu użycia wyniku. Np. (mniej-więcej):
  int a = 4; // Brak kodu bo a=4.
  a++;  // Brak kodu bo a=5.
  f(a); // Wywołanie z podstawieniem parametru jako "i32 5".
        // Zapisanie zmodyfikowanych zmiennych (a=5) do pamięci przed while-em.
  while (...) {...}

Optymalizacja wzięła się z tego, że nie można napisać "%reg = i32 0".


SSA
===
Brak postaci single static assignment.


Latte
=====
Napisałem minimalistyczną wersję Latte skopiowaną ze strony z zadaniem.
https://www.mimuw.edu.pl/~ben/Zajecia/Mrj2019/Latte/

Rozszerzyłem troszkę listę funkcji wbudowanych w język:

    void printInt(int)
    void printString(string)
 -> void printBoolean(boolean) <-
    void error()
    int readInt()
    string readString()

Dodatkowo są funkcje, których używam w wygenerowaym kodzie, ale na upartego można je też ręcznie wywołać (nie testowałem):

  boolean strEqual(string, string)
  string strConcat(string, string)

Zawsze można wywołać 'make doc' i wygenerować dokumentację z bnfc, żeby zobaczyć możliwe konstrukcje językowe. Pojawią się w katalogu docs/.

Napisy można porównać ==, != oraz zkonkatenować przy użyciu +. Każda konkatenacja tworzy nową tablicę w pamięci, a stara nie jest zwalniana.
