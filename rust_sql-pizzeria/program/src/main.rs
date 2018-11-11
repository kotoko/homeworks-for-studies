mod pizzeria;

use pizzeria::Connection;
use std::error::Error;
use std::io;
use std::io::Write;
use std::io::stdout;

fn print_help() {
	println!("Lista poleceń:");
	println!("(?  | pomoc)");
	println!("(q  | wyjście)");
	println!("(0  | dodaj_pizzę) PIZZA [CENA]");
	println!("(1  | usuń_pizzę) PIZZA");
	println!("(2  | wypisz_pizze)");
	println!("(3  | wypisz_składniki) PIZZA");
	println!("(4  | ustaw_przepis) PIZZA");
	println!("(5  | wypisz_przepis) PIZZA");
	println!("(10 | dodaj_składnik_do_magazynu) SKŁADNIK ILOŚĆ");
	println!("(11 | usuń_składnik_z_magazynu) SKŁADNIK ILOŚĆ");
	println!("(12 | wypisz_składniki_w_magazynie)");
	println!("(20 | dodaj_klienta)");
	println!("(21 | usuń_klienta) ID_KLIENTA");
	println!("(22 | wypisz_klientów)");
	println!("(23 | ustaw_telefon_klienta) ID_KLIENTA [TELEFON]");
	println!("(24 | ustaw_nr_rach_klienta) ID_KLIENTA [NR_RACH]");
	println!("(30 | złóż_zamówienie) ID_KLIENTA");
	println!("(31 | dokończ_zamówienie) ID_ZAMÓWIENIA");
	println!("(32 | wypisz_zamówienie) ID_ZAMÓWIENIA");
	println!("(33 | wypisz_listę_zamówień)");
}

fn add_pizza(conn: &Connection, tokens: &Vec<&str>) {
	if tokens.len() == 3 || tokens.len() == 2 {
		let mut price: Option<f64> = None;

		if tokens.len() == 3 {
			match tokens[2].parse::<f64>() {
				Err(_) => {
					println!("Błąd: oczekiwano ceny jako liczby");
					return;
				},
				Ok(n) => price = Some(n),
			};
		}

		let mut line: String = String::new();
		let mut line2: String = String::new();
		let mut ingredients: Vec<&str> = Vec::new();
		let mut amounts: Vec<f64> = Vec::new();

		print!("Lista składników $ ");
		stdout().flush().unwrap();

		io::stdin().read_line(&mut line).unwrap();

		let new_len = line.trim_right().len();
		line.truncate(new_len);

		let list_str: Vec<_> = (line).split_whitespace().collect();
		for token in list_str.iter() {
			ingredients.push(&token);
		}

		print!("Ilości składników $ ");
		stdout().flush().unwrap();

		io::stdin().read_line(&mut line2).unwrap();

		let new_len = line2.trim_right().len();
		line2.truncate(new_len);

		let list_f64: Vec<_> = (line2).split_whitespace().collect();
		for token in list_f64.iter() {
			match token.parse::<f64>() {
				Ok(n) => amounts.push(n),
				Err(_) => {
					println!("Błąd: oczekiwano ilości składnika jako liczby");
					return;
				},
			};
		}
		match pizzeria::add_pizza(&conn, &tokens[1], price, None, &ingredients, &amounts) {
			Ok(_) => println!("Dodano pizzę: {}", tokens[1]),
			Err(err) => println!("Błąd: {}", err),
		}

	}
	else {
		println!("Błąd: Oczekiwano 2 albo 3 parametrów");
	}
}

fn delete_pizza(conn: &Connection, tokens: &Vec<&str>) {
	if tokens.len() >= 2 {
		match pizzeria::delete_pizza(&conn, &tokens[1]) {
			Ok(_) => println!("Usunięto pizzę: {}", tokens[1]),
			Err(err) => println!("Błąd: {}", err),
		}
	}
	else {
		println!("Błąd: Oczekiwano nazwy pizzy")
	}
}

fn print_pizzas(conn: &Connection) {
	match pizzeria::list_pizzas(&conn) {
		Err(err) => println!("Błąd: {}", err.description()),
		Ok(pizzas) => {
			println!("Lista pizz (nazwa, cena, przepis):");

			for pizza in pizzas.iter() {
				println!(" * {}, {}, {}",
						 pizza.name,
						 match pizza.price {
							 None => String::from("✘"),
							 Some(price) => price.to_string(),
						 },
						 match pizza.recipe {
							 None => String::from("✘"),
							 Some(ref recipe) => recipe.to_string(),
						 }
				);
			}
		},
	};
}

fn print_ingredients(conn: &Connection, tokens: &Vec<&str>) {
	if tokens.len() >= 2 {
		match pizzeria::list_pizza_ingredients(&conn, &tokens[1]) {
			Err(err) => println!("Błąd: {}", err.description()),
			Ok(ingredients) => {
				println!("Lista składników (nazwa, ilość): ");

				for ingredient in ingredients.iter() {
					println!(" * {}, x{}", ingredient.name, ingredient.amount);
				}
			},
		}
	}
	else {
		println!("Błąd: Oczekiwano nazwy pizzy")
	}
}

fn set_recipe(conn: &Connection, tokens: &Vec<&str>) {
	if tokens.len() == 2 {
		let mut line: String = String::new();
		let mut recipe: Option<&str> = None;

		print!("Przepis $ ");
		stdout().flush().unwrap();

		io::stdin().read_line(&mut line).unwrap();

		let new_len = line.trim_right().len();
		line.truncate(new_len);

		if line.len() > 0 {
			recipe = Some(&line);
		}

		match pizzeria::set_recipe(&conn, &tokens[1], recipe) {
			Err(err) => println!("Błąd: {}", err),
			Ok(_) => println!("Ustawiono przepis dla {}!", tokens[1]),
		}
	}
	else {
		println!("Błąd: oczekiwano jednego parametru - nazwy pizzy");
	}
}

fn print_recipe(conn: &Connection, tokens: &Vec<&str>) {
	if tokens.len() == 2 {
		match pizzeria::get_recipe(&conn, &tokens[1]) {
			Err(err) => println!("Błąd: {}", err),
			Ok(recipe) => println!("Przepis: {}",
								   match recipe {
									   None => String::from("✘"),
									   Some(recipe) => recipe.to_string(),
								   }
			),
		}
	}
	else {
		println!("Błąd: oczekiwano jednego parametru - nazwy pizzy");
	}
}

fn add_ingredient_stock(conn: &Connection, tokens: &Vec<&str>) {
	if tokens.len() == 3 {
		match tokens[2].parse::<f64>() {
			Err(_) => println!("Błąd: oczekiwano ilości jako liczby"),
			Ok(amount) => {
				match pizzeria::add_ingredient_stock(&conn, &tokens[1], amount) {
					Err(err) => println!("Błąd: {}", err),
					Ok(_) => println!("Do {} dodano {}!", &tokens[1], amount),
				}
			},
		}
	}
	else {
		println!("Błąd: oczekiwano dwóch parametrów - nazwy składnika oraz ilości");
	}
}

fn delete_ingredient_stock(conn: &Connection, tokens: &Vec<&str>) {
	if tokens.len() == 3 {
		match tokens[2].parse::<f64>() {
			Err(_) => println!("Błąd: oczekiwano ilości jako liczby"),
			Ok(amount) => {
				match pizzeria::delete_ingredient_stock(&conn, &tokens[1], amount) {
					Err(err) => println!("Błąd: {}", err),
					Ok(_) => println!("Z {} usunięto {}!", &tokens[1], amount),
				}
			},
		}
	}
	else {
		println!("Błąd: oczekiwano dwóch parametrów - nazwy składnika oraz ilości");
	}
}

fn print_ingredients_stock(conn: &Connection) {
	match pizzeria::list_ingredients_stock(&conn) {
		Err(err) => println!("Błąd: {}", err),
		Ok(ingredients) => {
			println!("Lista składników w magazynie (nazwa, ilość):");

			for ingredient in ingredients.iter() {
				println!(" * {}, x{}", ingredient.name, ingredient.amount);
			}
		},
	}
}

fn add_client(conn: &Connection) {
	match pizzeria::add_client(&conn, None, None) {
		Err(err) => println!("Błąd: {}", err),
		Ok(id) => println!("Dodano klienta: {}", id),
	}
}

fn delete_client(conn: &Connection, tokens: &Vec<&str>) {
	if tokens.len() >= 2 {
		match tokens[1].parse::<i32>() {
			Err(_) => println!("Błąd: oczekiwano id klienta jako liczby"),
			Ok(id) => {
				match pizzeria::delete_client(&conn, id) {
					Err(err) => println!("Błąd: {}", err),
					Ok(_) => println!("Usunięto klienta: {}", id),
				}
			},
		}
	}
	else {
		println!("Błąd: oczekiwano id klienta");
	}
}

fn print_clients(conn: &Connection) {
	match pizzeria::list_clients(&conn) {
		Err(err) => println!("Błąd: {}", err),
		Ok(clients) => {
			println!("Lista klientów (id, nr_rachunku_bankowego, telefon):");

			for client in clients.iter() {
				println!(" * {}, {}, {}",
						 client.id,
						 match client.bank_number {
							 None => String::from("✘"),
							 Some(ref client) => client.to_string(),
						 },
						 match client.phone {
							 None => String::from("✘"),
							 Some(ref phone) => phone.to_string(),
						 }
				);
			}
		},
	}
}

fn set_client_phone(conn: &Connection, tokens: &Vec<&str>) {
	if tokens.len() == 2 || tokens.len() == 3 {
		let mut phone: Option<&str> = None;

		if tokens.len() == 3 {
			phone = Some(&tokens[2]);
		}

		match tokens[1].parse::<i32>() {
			Err(_) => println!("Błąd: oczekiwano id klienta jako liczby"),
			Ok(id) => {
				match pizzeria::set_client_phone(&conn, id, phone) {
					Err(err) => println!("Błąd: {}", err),
					Ok(_) => println!("Ustawiono telefon dla klienta: {}", id),
				};
			},
		}
	}
	else {
		println!("Błąd: oczekiwano jednego lub dwóch parametrów - id klienta oraz ew. telefonu");
	}
}

fn set_client_bank_number(conn: &Connection, tokens: &Vec<&str>) {
	if tokens.len() == 2 || tokens.len() == 3 {
		let mut bank_number: Option<&str> = None;

		if tokens.len() == 3 {
			bank_number = Some(&tokens[2]);
		}

		match tokens[1].parse::<i32>() {
			Err(_) => println!("Błąd: oczekiwano id klienta jako liczby"),
			Ok(id) => {
				match pizzeria::set_client_bank_number(&conn, id, bank_number) {
					Err(err) => println!("Błąd: {}", err),
					Ok(_) => println!("Ustawiono nr rachunku dla klienta: {}", id),
				};
			},
		}
	}
	else {
		println!("Błąd: oczekiwano jednego lub dwóch parametrów - id klienta oraz ew. nr rachunku");
	}
}

fn make_order(conn: &Connection, tokens: &Vec<&str>) {
	if tokens.len() >= 2 {
		match tokens[1].parse::<i32>() {
			Err(_) => println!("Błąd: oczekiwano id klienta jako liczby"),
			Ok(client_id) => {
				let mut line = String::new();

				print!("Lista pizz $ ");
				stdout().flush().unwrap();

				io::stdin().read_line(&mut line).unwrap();

				let new_len = line.trim_right().len();
				line.truncate(new_len);

				let pizzas: Vec<_> = (line).split_whitespace().collect();

				match pizzeria::make_order(&conn, client_id, &pizzas) {
					Err(err) => println!("Błąd: {}", err),
					Ok(order_id) => println!("Złożono zamówienie: {}", order_id),
				}
			},
		}
	}
	else {
		println!("Błąd: oczekiwano jednego parametru - id klienta");
	}
}

fn finish_order(conn: &Connection, tokens: &Vec<&str>) {
	if tokens.len() >= 2 {
		match tokens[1].parse::<i32>() {
			Err(_) => println!("Błąd: oczekiwano id zamówienia jako liczby"),
			Ok(id) => {
				match pizzeria::finish_order(&conn, id) {
					Err(err) => println!("Błąd: {}", err),
					Ok(_) => println!("Dokończono zamówienie: {}", id),
				}
			},
		}
	}
	else {
		println!("Błąd: oczekiwano jednego parametru - id zamówienia");
	}
}

fn print_order(conn: &Connection, tokens: &Vec<&str>) {
	if tokens.len() >= 2 {
		match tokens[1].parse::<i32>() {
			Err(_) => println!("Błąd: oczekiwano id zamówienia jako liczby"),
			Ok(id) => {
				match pizzeria::get_order(&conn, id) {
					Err(err) => println!("Błąd: {}", err),
					Ok((order, items)) => {
						println!("Zamówienie: {}", order.order);
						println!("Klient: {}", order.client);
						println!("Data złożenia: {}", order.order_date);
						println!("Data realizacji: {}",
								 match order.finish_date {
									 None => String::from("✘"),
									 Some(finish_date) => finish_date.to_string(),
								 }
						);

						let mut price = 0 as f64;
						for item in items.iter() {
							price += item.price * item.amount;
						}
						println!("Cena: {}", price);

						println!("Lista pizz (pizza, ilość, cena 1 szt.):");
						for item in items.iter() {
							println!(" * {}, x{}, {}", item.pizza, item.amount, item.price);
						}
					},
				}
			},
		}
	}
	else {
		println!("Błąd: oczekiwano jednego parametru - id zamówienia");
	}
}

fn print_orders(conn: &Connection) {
	match pizzeria::get_orders(&conn) {
		Err(err) => println!("Błąd: {}", err),
		Ok(orders) => {
			println!("Lista zamówień (id, klient, data złożenia, data realizacji):");

			for order in orders.iter() {
				println!(" * {}, {}, {}, {}",
						 order.order,
						 order.client,
						 order.order_date,
						 match order.finish_date {
							 None => String::from("✘"),
							 Some(finish_date) => finish_date.to_string(),
						 }
				);
			}
		},
	}
}

fn print_easter_egg() {
	println!("                                     ._");
	println!("                                   ,(  `-.");
	println!("                                 ,': `.   `.");
	println!("                               ,` *   `-.   \\");
	println!("                             ,'  ` :+  = `.  `.");
	println!("                           ,~  (o):  .,   `.  `.");
	println!("                         ,'  ; :   ,(__) x;`.  ;");
	println!("                       ,'  :'  itz  ;  ; ; _,-'");
	println!("                     .'O ; = _' C ; ;'_,_ ;");
	println!("                   ,;  _;   ` : ;'_,-'   i'");
	println!("                 ,` `;(_)  0 ; ','       :");
	println!("               .';6     ; ' ,-'~");
	println!("             ,' Q  ,& ;',-.'");
	println!("           ,( :` ; _,-'~  ;");
	println!("         ,~.`c _','");
	println!("       .';^_,-' ~");
	println!("     ,'_;-''");
	println!("    ,,~");
	println!("    i'");
	println!("    :");
	println!("            o8o");
	println!("            `\"'");
	println!("oo.ooooo.  oooo    oooooooo   oooooooo  .oooo.");
	println!(" 888' `88b `888   d'\"\"7d8P   d'\"\"7d8P  `P  )88b");
	println!(" 888   888  888     .d8P'      .d8P'    .oP\"888");
	println!(" 888   888  888   .d8P'  .P  .d8P'  .P d8(  888");
	println!(" 888bod8P' o888o d8888888P  d8888888P  `Y888\"\"8o");
	println!(" 888");
	println!("o888o");
}

fn read_user_stdin(conn: &Connection) {
	println!("Aby uzyskać listę możliwych poleceń wpisz 'pomoc'");
	println!();

	let mut line: String = String::new();
	loop {
		line.clear();
		print!("$ ");
		io::stdout().flush().unwrap();
		let result = io::stdin().read_line(&mut line);

		match result {
			Err(err) => {
				println!("Błąd: {}", err.description());
				return;
			},
			//EOF
			Ok(0) => {
				println!("Kończę...");
				return;
			},
			//empty line
			Ok(1) => (),
			Ok(_) => {
				let new_len = line.trim_right().len();
				line.truncate(new_len);

				let tokens: Vec<_> = (line).split_whitespace().collect();

				if tokens.len() > 0 {
					match tokens[0].as_ref() {
						"wyjście" | "exit" | "quit" | "q" | ":q" | ":q!" => {
							println!("Kończę...");
							return;
						},
						"pomoc" | "help" | "h" | "p" | "?" => print_help(),
						"0" | "dodaj_pizzę" => add_pizza(&conn, &tokens),
						"1" | "usuń_pizzę" => delete_pizza(&conn, &tokens),
						"2" | "wypisz_pizze" => print_pizzas(&conn),
						"3" | "wypisz_składniki" => print_ingredients(&conn, &tokens),
						"4" | "ustaw_przepis" => set_recipe(&conn, &tokens),
						"5" | "wypisz_przepis" => print_recipe(&conn, &tokens),
						"10" | "dodaj_składnik_do_magazynu" => add_ingredient_stock(&conn, &tokens),
						"11" | "usuń_składnik_z_magazynu" => delete_ingredient_stock(&conn, &tokens),
						"12" | "wypisz_składniki_w_magazynie" => print_ingredients_stock(&conn),
						"13" => print_easter_egg(),
						"20" | "dodaj_klienta" => add_client(&conn),
						"21" | "usuń_klienta" => delete_client(&conn, &tokens),
						"22" | "wypisz_klientów" => print_clients(&conn),
						"23" | "ustaw_telefon_klienta" => set_client_phone(&conn, &tokens),
						"24" | "ustaw_nr_rach_klienta" => set_client_bank_number(&conn, &tokens),
						"30" | "złóż_zamówienie" => make_order(&conn, &tokens),
						"31" | "dokończ_zamówienie" => finish_order(&conn, &tokens),
						"32" | "wypisz_zamówienie" => print_order(&conn, &tokens),
						"33" | "wypisz_listę_zamówień" => print_orders(&conn),
						_ => println!("Nieznane polecenie: {}", tokens[0]),
					}
				}
			},
		}

		println!();
	}
}

fn main() {
	let user = "admin";
	let password = "admin";
	let database = "bd";
	let host = "localhost";
	let port: u16 = 5432;

	let conn = pizzeria::connect(database, user, Some(password), host, port);

	match conn {
		Ok(conn) => {
			println!("Połączono z bazą danych!");
			println!();
			read_user_stdin(&conn);
			pizzeria::disconnect(conn).unwrap();
		},
		Err(err) => {
			println!("Wystąpił błąd podczas łączenia się z bazą danych: {}", err.description());
			match err.code() {
				Some(err) => println!("{:?}", err),
				None => (),
			};
		},
	}
}
