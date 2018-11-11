/* Some known bugs:
*  https://github.com/npgsql/npgsql/issues/818
*/

extern crate postgres;
extern crate postgres_array;
extern crate chrono;

use self::postgres::Connection as PgConnection;
use self::postgres::Error as PgError;
use self::postgres::TlsMode;
use self::postgres::params::{Builder, Host};
use self::postgres_array::Array;
use self::chrono::NaiveDateTime;

pub enum Connection {
	Postgres(PgConnection),
}

pub struct Pizza {
	pub name: String,
	pub price: Option<f64>,
	pub recipe: Option<String>,
}

pub struct Ingredient {
	pub name: String,
	pub amount: f64,
}

pub struct Client {
	pub id: i32,
	pub bank_number: Option<String>,
	pub phone: Option<String>,
}

pub struct OrderItem {
	pub order: i32,
	pub pizza: String,
	pub price: f64,
	pub amount: f64,
}

pub struct Order {
	pub order: i32,
	pub client: i32,
	pub order_date: NaiveDateTime,
	pub finish_date: Option<NaiveDateTime>,
}

pub fn disconnect(conn: Connection) -> Result<(), PgError> {
	match conn {
		Connection::Postgres(conn) => {
			match conn.finish() {
				Ok(_) => Ok(()),
				Err(err) => Err(err),
			}
		},
	}
}

pub fn connect(database: &str, user: &str, password: Option<&str>, host: &str, port: u16)
	-> Result<Connection, PgError> {
	let params = Builder::new()
		.user(&user, password)
		.port(port)
		.database(database)
		.build(Host::Tcp(String::from(host)));

	let conn = PgConnection::connect(params, TlsMode::None) ?;

	Ok(Connection::Postgres(conn))
}

pub fn list_pizzas(conn: &Connection) -> Result<Vec<Pizza>, PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			let rows = conn.query("SELECT nazwa, cena, przepis FROM pizze", &[]) ?;

			let mut v: Vec<Pizza> = Vec::new();

			for row in rows.iter() {
				let pizza = Pizza {
					name: row.get(0),
					price: row.get(1),
					recipe: row.get(2),
				};

				v.push(pizza);
			}

			Ok(v)
		},
	}
}

pub fn add_pizza(conn: &Connection, name: &str, price: Option<f64>, recipe: Option<&str>,
				 ingredients: &Vec<&str>, amounts: &Vec<f64>) -> Result<(), PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			let ingredients_len = ingredients.len() as i32;
			let amounts_len = amounts.len() as i32;

			let ingredients = Array::from_vec(ingredients.clone(), ingredients_len);
			let amounts = Array::from_vec(amounts.clone(), amounts_len);

			conn.execute("SELECT dodaj_pizze_menu($1, $2, $3, $4, $5)::TEXT",
			&[&name, &price, &recipe, &ingredients, &amounts]) ?;
			Ok(())
		},
	}
}

pub fn delete_pizza(conn: &Connection, name: &str) -> Result<(), PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			conn.execute("SELECT usun_pizze_menu($1)::TEXT",&[&name]) ?;
			Ok(())
		},
	}
}

pub fn list_pizza_ingredients(conn: &Connection, name: &str) -> Result<Vec<Ingredient>, PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			let rows = conn.query("SELECT nazwa_skladnika, ilosc FROM skladniki_pizzy WHERE nazwa_pizzy = $1", &[&name]) ?;

			let mut v: Vec<Ingredient> = Vec::new();

			for row in rows.iter() {
				let ingredient = Ingredient {
					name: row.get(0),
					amount: row.get(1),
				};

				v.push(ingredient);
			}

			Ok(v)
		},
	}
}

pub fn set_recipe(conn: &Connection, name: &str, recipe: Option<&str>) -> Result<(), PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			conn.execute("SELECT ustaw_przepis($1, $2)::TEXT",&[&name, &recipe]) ?;

			Ok(())
		}
	}
}

pub fn get_recipe(conn: &Connection, name: &str) -> Result<Option<String>, PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			let rows = conn.query("SELECT zwroc_przepis($1)", &[&name]) ?;

			Ok(rows.get(0).get(0))
		}
	}
}

pub fn add_ingredient_stock(conn: &Connection, name: &str, amount: f64) -> Result<(), PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			conn.execute("SELECT dodaj_skladnik_magazyn($1, $2)::TEXT",&[&name, &amount]) ?;

			Ok(())
		},
	}
}

pub fn delete_ingredient_stock(conn: &Connection, name: &str, amount: f64) -> Result<(), PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			conn.execute("SELECT usun_skladnik_magazyn($1, $2)::TEXT",&[&name, &amount]) ?;

			Ok(())
		},
	}
}

pub fn list_ingredients_stock(conn: &Connection) -> Result<Vec<Ingredient>, PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			let rows = conn.query("SELECT nazwa_skladnika, ilosc FROM skladniki_w_magazynie", &[]) ?;

			let mut v: Vec<Ingredient> = Vec::new();

			for row in rows.iter() {
				let ingredient = Ingredient {
					name: row.get(0),
					amount: row.get(1),
				};

				v.push(ingredient);
			}

			Ok(v)
		},
	}
}

pub fn add_client(conn: &Connection, bank_number: Option<&str>, phone: Option<&str>) -> Result<i32, PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			let rows = conn.query("SELECT dodaj_klienta($1, $2)", &[&bank_number, &phone]) ?;

			Ok(rows.get(0).get(0))
		},
	}
}

pub fn delete_client(conn: &Connection, client: i32) -> Result<(), PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			conn.execute("SELECT usun_klienta($1)::TEXT", &[&client]) ?;

			Ok(())
		},
	}
}

pub fn list_clients(conn: &Connection) -> Result<Vec<Client>, PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			let rows = conn.query("SELECT id, nr_rachunku_bankowego, nr_telefonu FROM klienci", &[]) ?;

			let mut v: Vec<Client> = Vec::new();

			for row in rows.iter() {
				let client = Client {
					id: row.get(0),
					bank_number: row.get(1),
					phone: row.get(2),
				};

				v.push(client);
			}

			Ok(v)
		},
	}
}

pub fn set_client_phone(conn: &Connection, client: i32, phone: Option<&str>) -> Result<(), PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			conn.execute("SELECT ustaw_telefon($1, $2)::TEXT", &[&client, &phone]) ?;

			Ok(())
		},
	}
}

pub fn set_client_bank_number(conn: &Connection, client: i32, bank_number: Option<&str>) -> Result<(), PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			conn.execute("SELECT ustaw_nr_rachunku_bankowego($1, $2)::TEXT", &[&client, &bank_number]) ?;

			Ok(())
		},
	}
}

pub fn make_order(conn: &Connection, client: i32, pizzas: &Vec<&str>) -> Result<i32, PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			let pizzas_len = pizzas.len() as i32;
			let pizzas = Array::from_vec(pizzas.clone(), pizzas_len);

			let rows = conn.query("SELECT zloz_zamowienie($1, $2)", &[&client, &pizzas]) ?;

			Ok(rows.get(0).get(0))
		},
	}
}

pub fn finish_order(conn: &Connection, order: i32) -> Result<(), PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			conn.execute("SELECT dokoncz_zamowienie($1)::TEXT", &[&order]) ?;

			Ok(())
		},
	}
}

pub fn get_order(conn: &Connection, id: i32) -> Result<(Order, Vec<OrderItem>), PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			let rows = conn.query("SELECT data_zlozenia, data_realizacji, id_klienta FROM zamowienia WHERE id = $1", &[&id]) ?;
			let order = Order {
				order: id,
				client: rows.get(0).get(2),
				order_date: rows.get(0).get(0),
				finish_date: rows.get(0).get(1),
			};

			let mut items: Vec<OrderItem> = Vec::new();

			let rows = conn.query("SELECT nazwa_pizzy, cena, ilosc FROM zamowione_pizze WHERE id_zamowienia = $1", &[&id]) ?;

			for row in rows.iter() {
				let item = OrderItem {
					order: id,
					pizza: row.get(0),
					price: row.get(1),
					amount: row.get(2),
				};

				items.push(item);
			}

			Ok((order, items))
		},
	}
}

pub fn get_orders(conn: &Connection) -> Result<Vec<Order>, PgError> {
	match *conn {
		Connection::Postgres(ref conn) => {
			let mut orders: Vec<Order> = Vec::new();

			let rows = conn.query("SELECT id, data_zlozenia, data_realizacji, id_klienta FROM zamowienia", &[]) ?;

			for row in rows.iter() {
				let order = Order {
					order: row.get(0),
					client: row.get(3),
					order_date: row.get(1),
					finish_date: row.get(2),
				};

				orders.push(order);
			}

			Ok(orders)
		},
	}
}