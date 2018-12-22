package mixingmachine;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import mixingmachine.controler.GuiControler;
import mixingmachine.machine.Machine;
import mixingmachine.machine.MyMachine;

/**
 * Główna klasa uruchamiająca aplikację.
 */
public class Main extends Application {
	private static Machine machine = new MyMachine();

	@Override
	public void start(Stage primaryStage) throws Exception{
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gui.fxml"));
		Parent root = loader.load();

		machine.initilize("src/config/maszyna.conf");

		GuiControler guiControler = loader.getController();
		guiControler.machine(machine);
		guiControler.initializeControler();

		Scene scene = new Scene(root);

		primaryStage.setTitle("Obiektowa Maszyna do Produkcji Farb");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
