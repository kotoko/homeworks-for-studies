(** Reprezentacja przedziału od min (pierwsza liczba) do max (druga liczba).
Jeśli min <= max to reprezuntuje przedział [min,max].
Jeśli min > max to reprezentuje sumę przedziałów [-∞,max]∪[min,+∞]. *)
type wartosc = Przedzial of float * float


(* Konstruktory *)
(** Procedura przyjmuje dwie liczby x,p typu „float”. Procedura zwraca stałą
typu „wartosc” reprezuntującą przedział x ± p%. *)
let wartosc_dokladnosc x p =
	Przedzial(
		(min ((x*.(100.-.p))/.100.) ((x*.(100.+.p))/.100.)),
		(max ((x*.(100.-.p))/.100.) ((x*.(100.+.p))/.100.))
	)

(** Procedura przyjmuje dwie liczby x,y typu „float”. Procedura zwraca stałą
typu „wartosc” reprezuntującą przedział [x,y]. *)
let wartosc_od_do x y =
	Przedzial(x,y)

(** Procedura przyjmuje dwie liczby x,y typu „float”. Procedura zwraca stałą
typu „wartosc” reprezuntującą liaczbę x (przedział [x,x]). *)
let wartosc_dokladna x =
	Przedzial(x,x)


(* Pomocnicze procedury *)
(** Procedura przyjmuje liczbę typu „float” x i sprawdza czy jest równa „nan”.
Zwraca boola: „true” jeśli jest równa i „false” w przeciwnym przypadku. *)
let czy_nan x =
	if (compare x nan) = 0 then
		true
	else
		false

(** Procedura przyjmuje liczbę typu „float” x i sprawdza czy jest różna od ±∞.
Zwraca boola: „true” jeśli jest różna i „false” w przeciwnym przypadku. *)
let czy_liczba x =
	if (x = infinity) || (x = neg_infinity) then
		false
	else
		true

(** Procedura sprawdza czy stała typu „wartosc” reprezentuje przedział
[min,max]. Zwraca boola: „true” jeśli reprezentuje i „false” w przeciwnym
przypadku. *)
let czy_przedzial x =
	match x with
	| Przedzial(x_min,x_max) ->
		if x_min <= x_max then
			true
		else
			false

(** Procedura sprawdza czy stała typu „wartosc” reprezentuje przedział
[-∞,max]∪[min,+∞]. Zwraca boola: „true” jeśli reprezentuje i „false”
w przeciwnym przypadku. *)
let czy_antyprzedzial x =
	match x with
	| Przedzial(x_min,x_max) ->
		if x_min > x_max then
			true
		else
			false

(** Procedura zwraca stałą typu „wartosc” reprezentującą pusty przedział. *)
let pusty =
	Przedzial(nan,nan)

(** Procedura zwraca stałą typu „wartosc” reprezentującą przedział [-∞,+∞]. *)
let pelny =
	Przedzial(neg_infinity,infinity)

(** Procedura zwraca stałą typu float reprezentującą +0. *)
let zero_plus =
	+0.

(** Procedura zwraca stałą typu float reprezentującą -0. *)
let zero_minus =
	-0.

(** Procedura zwraca stałą typu „wartosc” reprezentującą przedział [-0,+0]. *)
let zero =
	Przedzial((zero_minus),(zero_plus))

(** Procedura sprawdza czy stała typu „wartosc” reprezentuje przedział
pusty. Zwraca boola: „true” jeśli reprezentuje i „false” w przeciwnym
przypadku. *)
let czy_pusty x =
	match x with
	| Przedzial(x_min,x_max) ->
		if czy_nan x_min then
			true
		else if czy_nan x_max then
			true
		else if (x_min = infinity) && (x_max = neg_infinity) then
			true
		else if (x_min = neg_infinity) && (x_max = neg_infinity) then
			true
		else if (x_min = infinity) && (x_max = infinity) then
			true
		else
			false

(** Procedura sprawdza czy stała typu „wartosc” reprezentuje przedział
[-∞,+∞]. Zwraca boola: „true” jeśli reprezentuje i „false” w przeciwnym
przypadku. *)
let czy_pelny x =
	if czy_pusty x then
		false
	else
		if x = (pelny) then
			true
		else
			false

(** Procedura sprawdza czy stała typu „wartosc” reprezentuje przedział
[±0,±0]. Zwraca boola: „true” jeśli reprezentuje i „false” w przeciwnym
przypadku. *)
let czy_zero x =
	if czy_pusty x then
		false
	else
		if x = (zero) then
			true
		else
			false

(** Procedura przyjmuje liczbę typu „float” x i sprawdza czy jest równa +0.
Zwraca boola: „true” jeśli jest równa i „false” w przeciwnym przypadku. *)
let czy_zero_plus x =
	if x = 0. then
		if (42./.x) = infinity then
			true
		else
			false
	else
		false

(** Procedura przyjmuje liczbę typu „float” i sprawdza czy jest równa -0.
Zwraca boola: „true” jeśli jest równa i „false” w przeciwnym przypadku. *)
let czy_zero_minus x =
	if x = 0. then
		if (42./.x) = neg_infinity then
			true
		else
			false
	else
		false

(** Procedura sprawdza dla stałej typu „wartosc” zwraca przedział przeciwny.
Zawiera wszystkie liczby -ax, gdzie ax należy do x. *)
let przedzial_przeciwny x =
	if czy_pusty x then
		pusty
	else
		match x with
		| Przedzial(x_min,x_max) ->
			Przedzial((-1.)*.x_max, (-1.)*.x_min)


(* Selektory *)
(** Procedura sprawdza czy liczba typu „float” y może być w przedziale
reprezentowanym przez stałą typu „wartosc” x. Zwraca boola: „true” jeśli
może i „false” w przeciwnym przypadku.*)
let in_wartosc x y =
	match x with
	| Przedzial(x_min,x_max) ->
		if czy_pusty x then
			false
		else
			if czy_przedzial x then
				if (y >= x_min) && (y <= x_max) then
					true
				else
					false
			else (* antyprzedział *)
				if (y <= x_max) || (y >= x_min) then
					true
				else
					false

(** Procedura dla stałej typu „wartosc” x zwraca najmniejszą liczbę typu
„float”, która zawiera się w przedziale reprezentowanym przez x. *)
let min_wartosc x =
	if czy_pusty x then
		infinity (* co tu zwrócić ??????*)
	else
		match x with
		| Przedzial(x_min,x_max) ->
			if czy_przedzial x then
				x_min
			else (* antyprzedział *)
				neg_infinity

(** Procedura dla stałej typu „wartosc” x zwraca największą liczbę typu
„float”, która zawiera się w przedziale reprezentowanym przez x. *)
let max_wartosc x =
	if czy_pusty x then
		neg_infinity (* co tu zwrócić ??????*)
	else
		match x with
		| Przedzial(x_min,x_max) ->
			if czy_przedzial x then
				x_max
			else (* antyprzedział *)
				infinity

(** Procedura dla stałej typu „wartosc” x zwraca liczbę typu „float”,
która jest średnią arytmetyczną największej i najmniejszej liczby z
przedziału reprezentowanego przez x. W przypadku gdy, któraś liczba
nie jest skończona zwraca „nan”.*)
let sr_wartosc x =
	if czy_pusty x then
		nan
	else
		match x with
		| Przedzial(_,_) ->
			let x_min = min_wartosc x and x_max = max_wartosc x
			in
				if (czy_liczba x_min) || (czy_liczba x_max) then
					(x_min+.x_max)/.2.
				else
					nan


(* Modyfikatory *)
(** Procedura dla dwóch stałych typu „wartosc” x,y zwraca stałą typu
„wartosc”, która reprezentuje sumę przedziałów reprezentowanych
przez x i y. ax+ay należy do sumy przedziałów wtedy i tylko wtedy
gdy ax należy x i ay należy y. *)
let rec plus x y =
	if (czy_pusty x) || (czy_pusty y) then
		pusty
	else
		match x,y with
		| Przedzial(x_min,x_max), Przedzial(y_min,y_max) ->
			if (czy_przedzial x) && (czy_przedzial y) then
				Przedzial( (x_min+.y_min), (x_max+.y_max) )
			else if (czy_przedzial x) && (czy_antyprzedzial y) then
				(plus y x)
			else if (czy_antyprzedzial x) && (czy_przedzial y) then
				let lewy = x_max+.y_max and prawy = x_min+.y_min
				in
					if lewy < prawy then
						Przedzial(prawy, lewy)
					else
						pelny
			else (* dwa antyprzedzialy *)
				pelny

(** Procedura dla dwóch stałych typu „wartosc” x,y zwraca stałą typu
„wartosc”, która reprezentuje różnicę przedziałów reprezentowanych
przez x i y. ax-ay należy do różnicy przedziałów wtedy i tylko wtedy
gdy ax należy x i ay należy y. *)
let minus x y =
	if (czy_pusty x) || (czy_pusty y) then
		pusty
	else
		plus x (przedzial_przeciwny y)

(** Procedura dla dwóch stałych typu „wartosc” x,y zwraca stałą typu
„wartosc”, która reprezentuje sumę przedziałów (w sensie matematycznym)
reprezentowanych przez x i y. k należy do sumy przedziałów wtedy i tylko wtedy
gdy k należy x lub k należy y. Zakładam, że wynik da się przedstawić w postaci
przedziału lub antyprzedziału.*)
let rec suma_przedzialow x y =
	if czy_pusty x then
		y
	else if czy_pusty y then
		x
	else if czy_pelny x then
		pelny
	else if czy_pelny y then
		pelny
	else match x,y with
	| Przedzial(x_min,x_max),Przedzial(y_min,y_max) ->
		if (czy_przedzial x) && (czy_przedzial y) then
			(* przedziały nachodzą na siebie *)
			if ((x_min <= y_min) && (y_min <= x_max))
				|| ((x_min <= y_max) && (y_max <= x_max)) then
				Przedzial( (min x_min y_min), (max x_max y_max) )
			else (* sumują się do antyprzedziału *)
				if x_max < y_min then
					Przedzial(y_min, x_max)
				else if (x_max = infinity) && (y_min = neg_infinity) then
					Przedzial(x_min,y_max)
				else
					Przedzial(x_max, y_min)
		else if (czy_przedzial x) && (czy_antyprzedzial y) then
			suma_przedzialow y x
		else if (czy_antyprzedzial x) && (czy_przedzial y) then
			(* przedział zapełnia całą dziurę w antyprzedziale *)
			if (y_min <= x_max) && (x_min <= y_min) then
				pelny
			else if y_max <= x_min then
				Przedzial(x_min, (max x_max y_max))
			else
				Przedzial((min y_min x_min), x_max)
		else (* dwa antyprzedzialy *)
			(* „dziury” w antyprzedziałach są rozłączne *)
			if (x_min <= y_max) || (y_min <= x_max) then
				pelny
			else
				Przedzial((min x_min y_min), (max x_max y_max))

(** Procedura dla dwóch stałych typu „wartosc” x,y zwraca stałą typu
„wartosc”, która reprezentuje iloczyn przedziałów reprezentowanych
przez x i y. ax*ay należy do iloczynu przedziałów wtedy i tylko wtedy
gdy ax należy x i ay należy y. *)
let rec razy x y =
	if (czy_pusty x) || (czy_pusty y) then
		pusty
	else
		match x,y with
		| Przedzial(x_min,x_max), Przedzial(y_min,y_max) ->
			if (czy_przedzial x) && (czy_przedzial y) then
				(*minimum z czterech liczb z uwzględnieniem (złośliwego) nana*)
				let rec min4 a b c d =
					if czy_nan a then
						min4 infinity b c d
					else if czy_nan b then
						min4 a infinity c d
					else if czy_nan c then
						min4 a b infinity d
					else if czy_nan d then
						min4 a b c infinity
					else
						min (min a b) (min c d)
				(*maksimum z czterech liczb z uwzględnieniem (złośliwego) nana*)
				and max4 a b c d =
					if czy_nan a then
						max4 neg_infinity b c d
					else if czy_nan b then
						max4 a neg_infinity c d
					else if czy_nan c then
						max4 a b neg_infinity d
					else if czy_nan d then
						max4 a b c neg_infinity
					else
						max (max a b) (max c d)
				in
					Przedzial(
						(min4 (x_min*.y_min) (x_max*.y_min) (x_min*.y_max) (x_max*.y_max) ),
						(max4 (x_min*.y_min) (x_max*.y_min) (x_min*.y_max) (x_max*.y_max) )
					)
			else if (czy_antyprzedzial x) && (czy_przedzial y) then
				razy y x
			else if (czy_przedzial x) && (czy_antyprzedzial y) then
				suma_przedzialow (razy x (Przedzial(neg_infinity,y_max)))
					(razy x (Przedzial(y_min,infinity)))
			else (* dwa antyprzedziały *)
				suma_przedzialow (razy x (Przedzial(neg_infinity,y_max)))
					(razy x (Przedzial(y_min,infinity)))

(** Procedura dla stałej typu „wartosc” zwraca przedział odwrotny. k
należy do przedziału odwrotnego wtedy i tylko wtedy gdy 1/k należy
do przedziału reprezentowanego przez stałą y.*)
let rec przedzial_odwrotny y =
	if czy_pusty y then
		pusty
	else if czy_pelny y then
		pelny
	else
		match y with
			| Przedzial(y_min,y_max) ->
				if czy_przedzial y then
					if czy_zero y then
						pusty
					else
						if in_wartosc y 0. then
							(* chcę mieć nieskończoności z dobrym znakiem *)
							if y_max = 0. then
								Przedzial( (1./.(zero_minus)),(1./.y_min) )
							else if y_min = 0. then
								Przedzial( (1./.(y_max)),(1./.(zero_plus)) )
							else
								suma_przedzialow (przedzial_odwrotny (Przedzial(y_min,(zero_minus))) )
									(przedzial_odwrotny (Przedzial((zero_plus),y_max)) )
						else
							Przedzial( (1./.y_max),(1./.y_min) )
				else (* antyprzedział *)
					suma_przedzialow (przedzial_odwrotny (Przedzial(neg_infinity,y_max)))
						(przedzial_odwrotny (Przedzial(y_min,infinity)))

(** Procedura dla dwóch stałych typu „wartosc” x,y zwraca stałą typu
„wartosc”, która reprezentuje iloraz przedziałów reprezentowanych
przez x i y. ax/ay należy do ilorazu przedziałów wtedy i tylko wtedy
gdy ax należy x i ay należy y. *)
let podzielic x y =
	if (czy_pusty x) || (czy_pusty y) then
		pusty
	else
		razy x (przedzial_odwrotny y)
