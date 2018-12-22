package mixingmachine.controler;

import mixingmachine.machine.Machine;

/**
 * Interfejs graficznego interfejsu aplikacji.
 */
public interface GuiControler {
	/**
	 * Funkcja służy do poinformowania GuiControlera o maszynie mieszającej.
	 * @param machine maszyna mieszająca
	 */
	void machine(Machine machine);

	/**
	 * Funkcja służy do zainicjalizowania elementów interfejsu
	 * (w tym pobrania farb i pigmentów z maszyny).
	 */
	void initializeControler();
}
