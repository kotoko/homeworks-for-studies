% Jakub Romanowski

% Będę przechodził po drzewie jak DFS. Zapamiętuję ścieżkę z aktualnego wierzchołka do korzenia. W każdym wierzchołku sprawdzam czy wierzchołek o k w górę i 2k w górę są OK (takie same).


imiennicy(D, W) :- im(D, [], W).

% Parametry im(...):
% (drzewo, ścieżka do korzenia, odpowiedź)

% Aktualny wierzchołek jest wynikiem.
im(tree(_, W, _), LL, W) :- saOK(LL, W).

% Wynik w lewym poddrzewie.
im(tree(L, X, _), LL, W) :- im(L, [X | LL], W).

% Wynik w prawym poddrzewie.
im(tree(_, X, R), LL, W) :- im(R, [X | LL], W).


% Idę po liście dwoma wskaźniczkami. Pierwszy porusza się o 1 do przodu, drugi porusza się o dwa do przodu.
saOK([X | L], W) :- saOK([X|L], L, W).
saOK([W | _], [W | _], W).
saOK([_ | L], [_, _ | LL], W) :- saOK(L, LL, W).
