package mixingmachine.controler;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import mixingmachine.machine.*;

import java.util.logging.LogManager;
import java.util.stream.Collectors;

/**
 * Implementacja GuiControlera.
 * Konwencja nazw funkcji:
 * - update...() - funkcje, które aktualizują wyświetlane informacje w aplikacji
 * - showError...() - funkcje, które pokazują okienko z informacją o błędzie
 * - initialize...() - funkcje, które ustawiają listenery na elementach interfejsu
 */
public class MyGuiControler implements GuiControler {
	@FXML private AnchorPane main;
	@FXML private Label labelPaints;
	@FXML private ListView<String> listPaints;
	@FXML private Button buttonAddPaint;
	@FXML private VBox firstColumn;
	@FXML private VBox secondColumn;
	@FXML private Label labelPigments;
	@FXML private ListView<String> listPigments;
	@FXML private Button buttonUsePigment;
	@FXML private Button buttonAddPigment;
	@FXML private VBox thirdColumn;
	@FXML private Label labelPaintSelected;
	@FXML private Label labelPaintName;
	@FXML private Label labelPaintToxicity;
	@FXML private Label labelPaintQuality;
	@FXML private TextField textFieldPaintName;
	@FXML private TextField textFieldPaintToxicity;
	@FXML private TextField textFieldPaintQuality;
	@FXML private Label labelPigmentSelected;
	@FXML private Label labelPigmentFromColor;
	@FXML private Label labelPigmentToColor;
	@FXML private Label labelPigmentToxicityType;
	@FXML private Label labelPigmentToxicityValue;
	@FXML private TextField textFieldPigmentBeginPaint;
	@FXML private TextField textFieldPigmentEndPaint;
	@FXML private ChoiceBox<String> choiceBoxPigmentToxicityType;
	@FXML private TextField textFieldPigmentToxicityValue;
	@FXML private Button buttonStartxMixing;
	@FXML private ChoiceBox<String> choiceBoxPigmentQualityType;
	@FXML private TextField textFieldPigmentQualityValue;
	@FXML private Label labelPigmentQualityType;
	@FXML private Label labelPigmentQualityValue;
	@FXML private Label labelPigmentName;
	@FXML private TextField textFieldPigmentName;

	private Machine machine;
	private ObservableList<String> observablePaints;
	private ObservableList<String> observablePigments;
	private String selectedPaint;
	private String selectedPigment;
	/*
	 * Poniższa stała wzięła się z tego, że listenery są zbyt dokładne. Wychwytują
	 * też sytuacje gdy przeładowuję listę farb i/lub pigmentów. Wychodzi wtedy
	 * około ~10 okienek, mówiące że dane w polach są błędne (pola tekstowe zaznaczonej
	 * farby i pigmentu), bo okazuje, że javafx wypełnia je nullami. Nie miałem lepszego
	 * pomysłu, więc na czas aktualizacji list(y) „wyłączam” te listenery poprzez
	 * tą stałą.
	 */
	private boolean noUpdateEvents = false;


	public void machine(Machine machine){
		this.machine = machine;
	}

	private void updateInfoSelectedPaint() {
		if(selectedPaint == null) {
			textFieldPaintName.setText("");
			textFieldPaintToxicity.setText("");
			textFieldPaintQuality.setText("");
		}
		else {
			textFieldPaintName.setText(machine.paintName(selectedPaint));
			textFieldPaintToxicity.setText(String.valueOf(machine.paintToxicity(selectedPaint)));
			textFieldPaintQuality.setText(String.valueOf(machine.paintQuality(selectedPaint)));
		}
	}

	private void updateInfoSelectedPigment() {
		if(selectedPigment == null) {
			textFieldPigmentName.setText("");
			textFieldPigmentBeginPaint.setText("");
			textFieldPigmentEndPaint.setText("");
			choiceBoxPigmentToxicityType.setValue(null);
			textFieldPigmentToxicityValue.setText("");
			choiceBoxPigmentQualityType.setValue(null);
			textFieldPigmentQualityValue.setText("");
		}
		else {
			textFieldPigmentName.setText(machine.pigmentName(selectedPigment));
			textFieldPigmentBeginPaint.setText(machine.pigmentBeginPaint(selectedPigment));
			textFieldPigmentEndPaint.setText(machine.pigmentEndPaint(selectedPigment));

			switch(machine.pigmentToxicityType(selectedPigment)) {
				case CONSTANT_MINUS:
					choiceBoxPigmentToxicityType.setValue("-");
					break;

				case CONSTANT_PLUS:
					choiceBoxPigmentToxicityType.setValue("+");
					break;

				case MULTIPLIER:
					choiceBoxPigmentToxicityType.setValue("x");
					break;

				default:
					choiceBoxPigmentToxicityType.setValue(null);
					break;
			}

			textFieldPigmentToxicityValue.setText(String.valueOf(machine.pigmentToxicity(selectedPigment)));

			switch(machine.pigmentQualityType(selectedPigment)) {
				case CONSTANT_MINUS:
					choiceBoxPigmentQualityType.setValue("-");
					break;

				case CONSTANT_PLUS:
					choiceBoxPigmentQualityType.setValue("+");
					break;

				case MULTIPLIER:
					choiceBoxPigmentQualityType.setValue("x");
					break;

				default:
					choiceBoxPigmentQualityType.setValue(null);
					break;
			}

			textFieldPigmentQualityValue.setText(String.valueOf(machine.pigmentQuality(selectedPigment)));
		}
	}

	private void updateInfoPaints() {
		noUpdateEvents = true;

		this.observablePaints = FXCollections.observableArrayList(
				machine.paints().stream().map(Paint::name).collect(Collectors.toList())
		);

		listPaints.setItems(this.observablePaints);

		if(selectedPaint != null) {
			listPaints.getSelectionModel().clearSelection();
			listPaints.scrollTo(selectedPaint);
			listPaints.getSelectionModel().select(selectedPaint);
		}

		noUpdateEvents = false;
	}

	private void updateInfoPigments() {
		noUpdateEvents = true;

		this.observablePigments = FXCollections.observableArrayList(
				machine.pigments().stream().map(Pigment::name).collect(Collectors.toList())
		);

		listPigments.setItems(this.observablePigments);

		if(selectedPigment != null) {
			listPigments.getSelectionModel().clearSelection();
			listPigments.scrollTo(selectedPigment);
			listPigments.getSelectionModel().select(selectedPigment);
		}

		noUpdateEvents = false;
	}

	private void stopMixing() {
		machine.stopMixing();
		buttonStartxMixing.setDisable(false);
		buttonUsePigment.setDisable(true);
	}

	private void startMixing() {
		machine.startxMixing(selectedPaint);
		buttonStartxMixing.setDisable(true);
		buttonUsePigment.setDisable(false);
	}

	private void showErrorDialog(String windowTitle, String headerText, String text) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(windowTitle);
		alert.setHeaderText(headerText);
		alert.setContentText(text);

		// https://stackoverflow.com/a/33905734
		alert.getDialogPane().getChildren().stream().filter(node -> node instanceof Label).forEach(node -> ((Label)node).setMinHeight(Region.USE_PREF_SIZE));

		alert.showAndWait();
	}

	private void showErrorNullPaintEdit() {
		showErrorDialog("Błąd", "Błąd", "Próba edycji niezaznaczonej farby.");
	}

	private void showErrorNullPigmentEdit() {
		showErrorDialog("Błąd","Błąd", "Próba edycji niezaznaczonego pigmentu.");
	}

	private void showErrorInvalidPaintEdit() {
		showErrorDialog("Błąd","Błąd", "Podano nieprawidłowe wartości podczas edycji farby.");
	}

	private void showErrorInvalidPigmentEdit() {
		showErrorDialog("Błąd","Błąd", "Podano nieprawidłowe wartości podczas edycji pigmentu.");
	}

	private void showErrorMixingNoSelectedPaint() {
		showErrorDialog("Błąd", "Błąd", "Aby mieszać zaznacz najpierw farbę.");
	}

	private void showErrorMixingNoSelectedPigment() {
		showErrorDialog("Błąd", "Błąd", "Aby mieszać zaznacz najpierw pigment.");
	}

	private void showErrorMixingInvalidPigment() {
		showErrorDialog("Błąd", "Błąd", "Zaznaczony pigment nie może zostać użyty.");
	}

	private void initializePaintsList(){
		listPaints.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
				if(noUpdateEvents){return;}

				selectedPaint = t1;
				updateInfoSelectedPaint();
			}
		});
	}

	private void initializePigmentsList(){
		listPigments.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
				if(noUpdateEvents){return;}

				selectedPigment = t1;
				updateInfoSelectedPigment();
			}
		});
	}

	private void initializePaintSelectedArea(){
		textFieldPaintToxicity.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
				if(t1) {
					stopMixing();
				}
			}
		});

		textFieldPaintToxicity.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
				if(noUpdateEvents){return;}

				if(selectedPaint == null) {
					showErrorNullPaintEdit();
					return;
				}

				if(t1 == null) {
					showErrorInvalidPaintEdit();
					return;
				}

				int toxicity;

				try{
					toxicity = Integer.parseInt(t1);
				}
				catch(NumberFormatException e) {
					showErrorInvalidPaintEdit();
					return;
				}

				if(!Paint.correctToxicity(toxicity)) {
					showErrorInvalidPaintEdit();
					return;
				}

				machine.paintToxicity(selectedPaint, toxicity);
				updateInfoSelectedPaint();
			}
		});

		textFieldPaintQuality.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
				if(t1) {
					stopMixing();
				}
			}
		});

		textFieldPaintQuality.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
				if(noUpdateEvents){return;}

				if(selectedPaint == null) {
					showErrorNullPaintEdit();
					return;
				}

				if(t1 == null) {
					showErrorInvalidPaintEdit();
					return;
				}

				int quality;

				try{
					quality = Integer.parseInt(t1);
				}
				catch(NumberFormatException e) {
					showErrorInvalidPaintEdit();
					return;
				}

				if(!Paint.correctQuality(quality)) {
					showErrorInvalidPaintEdit();
					return;
				}

				machine.paintQuality(selectedPaint, quality);
				updateInfoSelectedPaint();
			}
		});
	}

	private void initializePigmentSelectedArea(){
		textFieldPigmentBeginPaint.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
				if(t1) {
					stopMixing();
				}
			}
		});

		textFieldPigmentBeginPaint.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
				if(noUpdateEvents){return;}

				if(selectedPigment == null) {
					showErrorNullPigmentEdit();
					return;
				}

				if(t1 == null || !Paint.correctName(t1)) {
					showErrorInvalidPigmentEdit();
					return;
				}

				machine.pigmentBeginPaint(selectedPigment, t1);
				updateInfoSelectedPigment();
			}
		});
		
		textFieldPigmentEndPaint.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
				if(t1) {
					stopMixing();
				}
			}
		});

		textFieldPigmentEndPaint.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
				if(noUpdateEvents){return;}

				if(selectedPigment == null) {
					showErrorNullPigmentEdit();
					return;
				}

				if(t1 == null || !Paint.correctName(t1)) {
					showErrorInvalidPigmentEdit();
					return;
				}

				machine.pigmentEndPaint(selectedPigment, t1);
				updateInfoSelectedPigment();
			}
		});

		choiceBoxPigmentToxicityType.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
				if(t1) {
					stopMixing();
				}
			}
		});

		choiceBoxPigmentToxicityType.setItems(FXCollections.observableArrayList("+","-","x"));

		choiceBoxPigmentToxicityType.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
				if(noUpdateEvents){return;}

				if(selectedPigment == null) {
					showErrorNullPigmentEdit();
					return;
				}

				switch(t1.intValue()){
					case 0:
						machine.pigmentToxicityType(selectedPigment, ToxicityChangerType.CONSTANT_PLUS);
						break;

					case 1:
						machine.pigmentToxicityType(selectedPigment, ToxicityChangerType.CONSTANT_MINUS);
						break;

					case 2:
						machine.pigmentToxicityType(selectedPigment, ToxicityChangerType.MULTIPLIER);
						break;
				}

				updateInfoSelectedPigment();
			}
		});

		textFieldPigmentToxicityValue.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
				if(t1) {
					stopMixing();
				}
			}
		});

		textFieldPigmentToxicityValue.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
				if(noUpdateEvents){return;}

				if(selectedPigment == null) {
					showErrorNullPigmentEdit();
					return;
				}

				double toxicity;

				try{
					toxicity = Double.parseDouble(t1);
				}
				catch(NumberFormatException e) {
					showErrorInvalidPigmentEdit();
					return;
				}

				if(!Pigment.correctToxicity(toxicity)) {
					showErrorInvalidPigmentEdit();
					return;
				}

				machine.pigmentToxicity(selectedPigment, toxicity);
				updateInfoSelectedPigment();
			}
		});

		choiceBoxPigmentQualityType.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
				if(t1) {
					stopMixing();
				}
			}
		});

		choiceBoxPigmentQualityType.setItems(FXCollections.observableArrayList("+","-","x"));

		choiceBoxPigmentQualityType.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
				if(noUpdateEvents){return;}

				if(selectedPigment == null) {
					showErrorNullPigmentEdit();
					return;
				}

				switch(t1.intValue()){
					case 0:
						machine.pigmentQualityType(selectedPigment, QualityChangerType.CONSTANT_PLUS);
						break;

					case 1:
						machine.pigmentQualityType(selectedPigment, QualityChangerType.CONSTANT_MINUS);
						break;

					case 2:
						machine.pigmentQualityType(selectedPigment, QualityChangerType.MULTIPLIER);
						break;
				}

				updateInfoSelectedPigment();

			}
		});

		textFieldPigmentQualityValue.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
				if(t1) {
					stopMixing();
				}
			}
		});

		textFieldPigmentQualityValue.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
				if(noUpdateEvents){return;}

				if(selectedPigment == null) {
					showErrorNullPigmentEdit();
					return;
				}

				double quality;

				try{
					quality = Double.parseDouble(t1);
				}
				catch(NumberFormatException e) {
					showErrorInvalidPigmentEdit();
					return;
				}

				if(!Pigment.correctQuality(quality)) {
					showErrorInvalidPigmentEdit();
					return;
				}

				machine.pigmentQuality(selectedPigment, quality);
				updateInfoSelectedPigment();
			}
		});
	}

	private void initializeButtons(){
		buttonAddPigment.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				if(noUpdateEvents){return;}

				stopMixing();

				String name = machine.addRandomPigment();
				selectedPigment = name;
				updateInfoPigments();
				updateInfoSelectedPigment();

				// https://stackoverflow.com/a/41476462
				LogManager.getLogManager().reset();

				actionEvent.consume();
			}
		});

		buttonAddPaint.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				if(noUpdateEvents){return;}

				stopMixing();

				String name = machine.addRandomPaint();
				selectedPaint = name;
				updateInfoPaints();
				updateInfoSelectedPaint();

				// https://stackoverflow.com/a/41476462
				LogManager.getLogManager().reset();

				actionEvent.consume();
			}
		});

		buttonStartxMixing.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				if(noUpdateEvents){return;}

				if(selectedPaint == null) {
					showErrorMixingNoSelectedPaint();
					return;
				}

				startMixing();
			}
		});

		buttonUsePigment.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				if(noUpdateEvents){return;}

				if(selectedPigment == null) {
					showErrorMixingNoSelectedPigment();
					return;
				}

				boolean result = machine.usePigment(selectedPigment);

				if(!result) {
					showErrorMixingInvalidPigment();
					return;
				}
			}
		});
	}

	public void initializeControler() {
		initializePaintsList();
		initializePigmentsList();
		initializePaintSelectedArea();
		initializePigmentSelectedArea();
		initializeButtons();

		updateInfoPaints();
		updateInfoPigments();
	}


}
