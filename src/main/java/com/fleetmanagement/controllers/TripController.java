package com.fleetmanagement.controllers;

import com.fleetmanagement.db.DatabaseConnection;
import com.fleetmanagement.dao.TripDAO;
import com.fleetmanagement.dao.CargoDAO;
import com.fleetmanagement.dao.FuelDAO;
import com.fleetmanagement.dao.TollDAO;
import com.fleetmanagement.dao.ExpenseDAO;
import com.fleetmanagement.model.Trip;
import com.fleetmanagement.model.Cargo;
import com.fleetmanagement.model.Fuel;
import com.fleetmanagement.model.Toll;
import com.fleetmanagement.model.Expense;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import com.fleetmanagement.model.Checkpoint;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.ResultSet;

import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar.ButtonData;
import java.util.Optional;

public class TripController implements Initializable {


    @FXML private WebView mapWebView;
    // ===== MAIN FORM FIELDS =====
    @FXML private TextField tripIdField, vehicleIdField, driverIdField, reqCapacityField;
    @FXML private TextField sourceField, destinationField, distanceField;
    @FXML private DatePicker tripDatePicker;
    @FXML private TextField startOdoField, endOdoField;
    @FXML private ComboBox<String> statusComboBox;

    // ===== FILTER & TABLE (TRIP) =====
    @FXML private ComboBox<String> filterCategoryComboBox;
    @FXML private TextField searchField;
    @FXML private TableView<Trip> tripTable;
    @FXML private TableColumn<Trip, String> colTripId, colVehicleId, colDriverId, colSource, colDestination;
    @FXML private TableColumn<Trip, LocalDate> colDate;
    @FXML private TableColumn<Trip, String> colStatus;
    @FXML private TableColumn<Trip, Double> colTotalCost;

    // ===== PANE SWITCHING VARIABLES =====
    @FXML private VBox sidebar;
    @FXML private Button btnHamburger;
    @FXML private VBox paneTripDetails, paneCargo, paneFuel, paneToll, paneExpenses;
    @FXML private Button btnTabTrip, btnTabCargo, btnTabFuel, btnTabToll, btnTabExpenses;
    @FXML private Label headerLabel;

    // ===== CARGO FIELDS & FILTER =====
    @FXML private TextField cargoTripIdField, cargoTypeField, cargoQtyField, cargoWeightField, cargoDescField;
    @FXML private ComboBox<String> cargoFilterCategoryComboBox;
    @FXML private TextField cargoSearchField;
    @FXML private TableView<Cargo> cargoTable;
    @FXML private TableColumn<Cargo, String> colCargoId;
    @FXML private TableColumn<Cargo, Integer> colCargoQty;
    @FXML private TableColumn<Cargo, String> colCargoTripId, colCargoType, colCargoDesc;
    @FXML private TableColumn<Cargo, Double> colCargoWeight;

    // ===== FUEL MODULE FIELDS =====
    @FXML private TextField fuelIdField, fuelTripIdField, fuelVehicleIdField, fuelTypeField, fuelDistanceField, fuelUsedField, fuelPriceField, fuelCostField, fuelMileageField;
    @FXML private ComboBox<String> fuelFilterCategoryComboBox;
    @FXML private TextField fuelSearchField;
    @FXML private TableView<Fuel> fuelTable;
    @FXML private TableColumn<Fuel, String> colFuelId, colFuelTripId, colFuelVehicleId, colFuelType;
    @FXML private TableColumn<Fuel, Double> colFuelDistance, colFuelUsed, colFuelPrice, colFuelCost, colFuelMileage;

    // ===== TOLL MODULE FIELDS =====
    @FXML private TextField tollIdField, tollTripIdField, tollNameField, tollLocationField, tollAmountField;
    @FXML private DatePicker tollDatePicker;
    @FXML private ComboBox<String> tollFilterCategoryComboBox;
    @FXML private TextField tollSearchField;
    @FXML private TableView<Toll> tollTable;
    @FXML private TableColumn<Toll, String> colTollId; // Changed from Integer to String
    @FXML private TableColumn<Toll, String> colTollTripId, colTollName, colTollLocation;
    @FXML private TableColumn<Toll, Double> colTollAmount;
    @FXML private TableColumn<Toll, LocalDate> colTollDate;

    // ===== EXPENSE MODULE FIELDS =====
    @FXML private TextField expIdField, expTripIdField, expAmountField, expDescField;
    @FXML private ComboBox<String> expTypeComboBox, expFilterCategoryComboBox;
    @FXML private DatePicker expDatePicker;
    @FXML private TextField expSearchField;
    @FXML private TableView<Expense> expTable;
    @FXML private TableColumn<Expense, String> colExpId; // Changed from Integer to String
    @FXML private TableColumn<Expense, String> colExpTripId, colExpType, colExpDesc;
    @FXML private TableColumn<Expense, Double> colExpAmount;
    @FXML private TableColumn<Expense, LocalDate> colExpDate;


    @FXML private TableView<Checkpoint> trackingTable;
    @FXML private TableColumn<Checkpoint, Integer> colTrackSeq;
    @FXML private TableColumn<Checkpoint, String> colTrackLocation;
    @FXML private TableColumn<Checkpoint, String> colTrackExpected;
    @FXML private TableColumn<Checkpoint, String> colTrackActual;
    @FXML private TableColumn<Checkpoint, String> colTrackStatus;

    @FXML private TableColumn<Checkpoint, String> colTrackTripId;

    private final TripDAO tripDAO = new TripDAO();
    private final ObservableList<Trip> tripList = FXCollections.observableArrayList();
    private FilteredList<Trip> filteredTripList;

    private final CargoDAO cargoDAO = new CargoDAO();
    private final ObservableList<Cargo> cargoList = FXCollections.observableArrayList();
    private FilteredList<Cargo> filteredCargoList;

    private final FuelDAO fuelDAO = new FuelDAO();
    private final ObservableList<Fuel> fuelList = FXCollections.observableArrayList();
    private FilteredList<Fuel> filteredFuelList;

    private final TollDAO tollDAO = new TollDAO();
    private final ObservableList<Toll> tollList = FXCollections.observableArrayList();
    private FilteredList<Toll> filteredTollList;

    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final ObservableList<Expense> expenseList = FXCollections.observableArrayList();
    private FilteredList<Expense> filteredExpenseList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusComboBox.setItems(FXCollections.observableArrayList("Scheduled", "In Progress", "Completed"));

        // Setup Trip Filters
        filterCategoryComboBox.setItems(FXCollections.observableArrayList("All Fields", "Trip ID", "Vehicle ID", "Driver ID", "Status"));
        filterCategoryComboBox.setValue("All Fields");

        colTripId.setCellValueFactory(new PropertyValueFactory<>("tripId"));
        colVehicleId.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));
        colDriverId.setCellValueFactory(new PropertyValueFactory<>("driverId"));
        colSource.setCellValueFactory(new PropertyValueFactory<>("source"));
        colDestination.setCellValueFactory(new PropertyValueFactory<>("destination"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("tripDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 1. Link Table Columns to Checkpoint Model properties
        colTrackSeq.setCellValueFactory(new PropertyValueFactory<>("sequence"));
        colTrackLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colTrackExpected.setCellValueFactory(new PropertyValueFactory<>("expectedTime"));
        colTrackActual.setCellValueFactory(new PropertyValueFactory<>("actualTime"));
        colTrackStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        // Add this right under colTrackSeq...
        colTrackSeq.setCellValueFactory(new PropertyValueFactory<>("sequence"));
        colTrackTripId.setCellValueFactory(new PropertyValueFactory<>("tripId")); // <-- NEW LINE
        colTrackLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        // ... rest of your columns
        // 2. Load the map
        setupLiveMap();

        // 3. Load the table data
        loadAllCheckpoints();

        colTotalCost.setCellValueFactory(cellData ->



                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getTotalTripCost()));

        filteredTripList = new FilteredList<>(tripList, t -> true);
        tripTable.setItems(filteredTripList);

        // Setup Cargo Filters & Table
        cargoFilterCategoryComboBox.setItems(FXCollections.observableArrayList("All Fields", "Cargo ID", "Trip ID", "Goods Type", "Quantity", "Weight"));
        cargoFilterCategoryComboBox.setValue("All Fields");

        colCargoId.setCellValueFactory(new PropertyValueFactory<>("cargoId"));
        colCargoTripId.setCellValueFactory(new PropertyValueFactory<>("tripId"));
        colCargoType.setCellValueFactory(new PropertyValueFactory<>("goodsType"));
        colCargoQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colCargoWeight.setCellValueFactory(new PropertyValueFactory<>("weightKg"));
        colCargoDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        filteredCargoList = new FilteredList<>(cargoList, c -> true);
        cargoTable.setItems(filteredCargoList);

        // Setup Fuel Filters & Table
        fuelFilterCategoryComboBox.setItems(FXCollections.observableArrayList("All Fields", "Fuel ID", "Trip ID", "Vehicle ID", "Fuel Type"));
        fuelFilterCategoryComboBox.setValue("All Fields");

        colFuelId.setCellValueFactory(new PropertyValueFactory<>("fuelId"));
        colFuelTripId.setCellValueFactory(new PropertyValueFactory<>("tripId"));
        colFuelVehicleId.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));
        colFuelType.setCellValueFactory(new PropertyValueFactory<>("fuelType"));
        colFuelDistance.setCellValueFactory(new PropertyValueFactory<>("distance"));
        colFuelUsed.setCellValueFactory(new PropertyValueFactory<>("fuelUsed"));
        colFuelPrice.setCellValueFactory(new PropertyValueFactory<>("fuelPrice"));
        colFuelCost.setCellValueFactory(new PropertyValueFactory<>("fuelCost"));
        colFuelMileage.setCellValueFactory(new PropertyValueFactory<>("mileage"));

        filteredFuelList = new FilteredList<>(fuelList, f -> true);
        fuelTable.setItems(filteredFuelList);

        // Setup Toll Filters & Table
        tollFilterCategoryComboBox.setItems(FXCollections.observableArrayList("All Fields", "Toll ID", "Trip ID", "Toll Name", "Location"));
        tollFilterCategoryComboBox.setValue("All Fields");

        colTollId.setCellValueFactory(new PropertyValueFactory<>("tollId"));
        colTollTripId.setCellValueFactory(new PropertyValueFactory<>("tripId"));
        colTollName.setCellValueFactory(new PropertyValueFactory<>("tollName"));
        colTollLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colTollAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colTollDate.setCellValueFactory(new PropertyValueFactory<>("tollDate"));

        filteredTollList = new FilteredList<>(tollList, t -> true);
        tollTable.setItems(filteredTollList);

        // Setup Expense Type ComboBox & Filters
        expTypeComboBox.setItems(FXCollections.observableArrayList("Parking", "Food", "Loading", "Unloading", "Repair", "Permit", "Other"));
        expFilterCategoryComboBox.setItems(FXCollections.observableArrayList("All Fields", "Expense ID", "Trip ID", "Expense Type", "Description"));
        expFilterCategoryComboBox.setValue("All Fields");

        colExpId.setCellValueFactory(new PropertyValueFactory<>("expenseId"));
        colExpTripId.setCellValueFactory(new PropertyValueFactory<>("tripId"));
        colExpType.setCellValueFactory(new PropertyValueFactory<>("expenseType"));
        colExpAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colExpDate.setCellValueFactory(new PropertyValueFactory<>("expenseDate"));
        colExpDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        filteredExpenseList = new FilteredList<>(expenseList, e -> true);
        expTable.setItems(filteredExpenseList);

        loadTripData();
        setupSearchFilter();
        setupCargoSearchFilter();
        setupRowSelectionListener();
        setupDistanceCalculator();
        loadAllCargo();
        loadFuelData();
        setupFuelListeners();
        setupFuelRowSelection();
        loadTollData();
        setupTollRowSelection();
        loadExpenseData();
        setupExpenseRowSelection();
    }

    private void setActiveTabButton(Button activeButton) {
        // 1. Define the default (gray) and active (blue) styles
        String defaultStyle = "-fx-background-color: #e2e8f0; -fx-text-fill: #0f172a; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;";
        String activeStyle = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;";

        // 2. Reset ALL buttons to gray
        btnTabTrip.setStyle(defaultStyle);
        btnTabCargo.setStyle(defaultStyle);
        btnTabFuel.setStyle(defaultStyle);
        btnTabToll.setStyle(defaultStyle);
        btnTabExpenses.setStyle(defaultStyle);
        btnTabTracking.setStyle(defaultStyle);

        // 3. Set the specifically clicked button to blue
        activeButton.setStyle(activeStyle);
    }

    private void setupDistanceCalculator() {
        startOdoField.textProperty().addListener((obs, oldVal, newVal) -> calculateDistance());
        endOdoField.textProperty().addListener((obs, oldVal, newVal) -> calculateDistance());
        statusComboBox.valueProperty().addListener((obs, oldVal, newVal) -> calculateDistance());
    }

    private void calculateDistance() {
        if ("Completed".equals(statusComboBox.getValue())) {
            try {
                double start = Double.parseDouble(startOdoField.getText().trim());
                double end = Double.parseDouble(endOdoField.getText().trim());
                double distance = end - start;

                if (distance >= 0) {
                    distanceField.setText(String.format("%.1f", distance));
                } else {
                    distanceField.setText("Invalid Odometer");
                }
            } catch (NumberFormatException e) {
                if (distanceField != null) distanceField.clear();
            }
        } else {
            if (distanceField != null) distanceField.clear();
        }
    }

    private void loadTripData() {
        List<Trip> trips = tripDAO.getAllTrips();
        tripList.setAll(trips);
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyTripFilter(newVal, filterCategoryComboBox.getValue()));
        filterCategoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyTripFilter(searchField.getText(), newVal));
    }

    private void applyTripFilter(String searchText, String category) {
        filteredTripList.setPredicate(trip -> {
            if (searchText == null || searchText.isBlank()) return true;
            String lower = searchText.toLowerCase();
            String cat = category == null ? "All Fields" : category;

            switch (cat) {
                case "Trip ID": return trip.getTripId() != null && trip.getTripId().toLowerCase().contains(lower);
                case "Vehicle ID": return trip.getVehicleId() != null && trip.getVehicleId().toLowerCase().contains(lower);
                case "Driver ID": return trip.getDriverId() != null && trip.getDriverId().toLowerCase().contains(lower);
                case "Status": return trip.getStatus() != null && trip.getStatus().toLowerCase().contains(lower);
                default:
                    return (trip.getTripId() != null && trip.getTripId().toLowerCase().contains(lower)) ||
                            (trip.getVehicleId() != null && trip.getVehicleId().toLowerCase().contains(lower)) ||
                            (trip.getDriverId() != null && trip.getDriverId().toLowerCase().contains(lower)) ||
                            (trip.getSource() != null && trip.getSource().toLowerCase().contains(lower)) ||
                            (trip.getDestination() != null && trip.getDestination().toLowerCase().contains(lower)) ||
                            (trip.getStatus() != null && trip.getStatus().toLowerCase().contains(lower));
            }
        });
    }

    private void setupCargoSearchFilter() {
        cargoSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyCargoFilter(newVal, cargoFilterCategoryComboBox.getValue()));
        cargoFilterCategoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyCargoFilter(cargoSearchField.getText(), newVal));
    }

    private void applyCargoFilter(String searchText, String category) {
        filteredCargoList.setPredicate(cargo -> {
            if (searchText == null || searchText.isBlank()) return true;
            String lower = searchText.toLowerCase();
            String cat = category == null ? "All Fields" : category;

            switch (cat) {
                case "Cargo ID": return String.valueOf(cargo.getCargoId()).contains(lower);
                case "Trip ID": return cargo.getTripId() != null && cargo.getTripId().toLowerCase().contains(lower);
                case "Goods Type": return cargo.getGoodsType() != null && cargo.getGoodsType().toLowerCase().contains(lower);
                case "Quantity": return String.valueOf(cargo.getQuantity()).contains(lower);
                case "Weight": return String.valueOf(cargo.getWeightKg()).contains(lower);
                default:
                    return (String.valueOf(cargo.getCargoId()).contains(lower)) ||
                            (cargo.getTripId() != null && cargo.getTripId().toLowerCase().contains(lower)) ||
                            (cargo.getGoodsType() != null && cargo.getGoodsType().toLowerCase().contains(lower)) ||
                            (String.valueOf(cargo.getQuantity()).contains(lower)) ||
                            (String.valueOf(cargo.getWeightKg()).contains(lower)) ||
                            (cargo.getDescription() != null && cargo.getDescription().toLowerCase().contains(lower));
            }
        });
    }

    private void setupRowSelectionListener() {
        tripTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                tripIdField.setText(newSel.getTripId());
                vehicleIdField.setText(newSel.getVehicleId());
                driverIdField.setText(newSel.getDriverId());
                sourceField.setText(newSel.getSource());
                destinationField.setText(newSel.getDestination());
                tripDatePicker.setValue(newSel.getTripDate());
                startOdoField.setText(newSel.getStartOdometer() != null ? String.valueOf(newSel.getStartOdometer()) : "");
                endOdoField.setText(newSel.getEndOdometer() != null ? String.valueOf(newSel.getEndOdometer()) : "");
                statusComboBox.setValue(newSel.getStatus());
                calculateDistance();
            }
        });

        cargoTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                cargoTripIdField.setText(newSel.getTripId());
                cargoTypeField.setText(newSel.getGoodsType());
                cargoQtyField.setText(String.valueOf(newSel.getQuantity()));
                cargoWeightField.setText(String.valueOf(newSel.getWeightKg()));
                cargoDescField.setText(newSel.getDescription());
            }
        });
    }

    // ================= TRIP LOGIC =================
    @FXML
    private void showAvailableVehiclesPopup() {
        double reqCap = 0;
        if (!reqCapacityField.getText().isBlank()) {
            try {
                reqCap = Double.parseDouble(reqCapacityField.getText().trim());
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Required Capacity must be a number.");
                return;
            }
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Available Vehicles");
        dialog.setHeaderText("Showing available vehicles with capacity >= " + reqCap + " kg");

        ButtonType selectButtonType = new ButtonType("Select Vehicle", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        TableView<String[]> table = new TableView<>();
        table.setPrefWidth(450); table.setPrefHeight(200);

        TableColumn<String[], String> colId = new TableColumn<>("Vehicle ID");
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colId.setPrefWidth(80);

        TableColumn<String[], String> colReg = new TableColumn<>("Registration");
        colReg.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colReg.setPrefWidth(120);

        TableColumn<String[], String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        colType.setPrefWidth(120);

        TableColumn<String[], String> colCap = new TableColumn<>("Capacity (kg)");
        colCap.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[3]));
        colCap.setPrefWidth(100);

        table.getColumns().addAll(colId, colReg, colType, colCap);

        List<String[]> availableVehicles = tripDAO.getAvailableVehiclesWithCapacity(reqCap);
        table.getItems().addAll(availableVehicles);

        dialog.getDialogPane().setContent(table);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType && table.getSelectionModel().getSelectedItem() != null) {
                return table.getSelectionModel().getSelectedItem()[0];
            }
            return null;
        });

        dialog.showAndWait().ifPresent(selectedId -> vehicleIdField.setText(selectedId));
    }

    @FXML private void handleAddTrip() {
        if (!validateForm()) return;
        String vId = vehicleIdField.getText().trim();
        String dId = driverIdField.getText().trim();
        if (!tripDAO.isVehicleAvailable(vId)) { showAlert(Alert.AlertType.ERROR, "Validation", "Vehicle is not available."); return; }
        if (!tripDAO.isDriverValid(dId)) { showAlert(Alert.AlertType.ERROR, "Validation", "Driver is not available/valid."); return; }

        if (tripDAO.addTrip(createTripFromFields())) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Trip added! Vehicle & Driver marked as 'On Trip'.");
            loadTripData(); handleClearForm();
        } else showAlert(Alert.AlertType.ERROR, "Error", "Failed to add trip.");
    }

    @FXML private void handleUpdateTrip() {
        if (tripIdField.getText().trim().isEmpty()) { showAlert(Alert.AlertType.WARNING, "No Selection", "Select a trip."); return; }
        if (tripDAO.updateTrip(createTripFromFields())) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Trip updated!");
            loadTripData(); handleClearForm();
        } else showAlert(Alert.AlertType.ERROR, "Error", "Failed to update trip.");
    }

    @FXML private void handleDeleteTrip() {
        if (tripIdField.getText().trim().isEmpty()) { showAlert(Alert.AlertType.WARNING, "No Selection", "Select a trip."); return; }
        if (tripDAO.deleteTrip(tripIdField.getText().trim())) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Trip deleted!");
            loadTripData(); handleClearForm(); loadAllCargo();
        } else showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete.");
    }

    @FXML private void handleClearAllTrips() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete ALL Trips?");
        alert.setContentText("Are you sure you want to delete all trips? This will reset all vehicles and drivers to Available.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (tripDAO.clearAllTrips()) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "All trips cleared.");
                loadTripData(); handleClearForm(); loadAllCargo();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to clear trips.");
            }
        }
    }

    @FXML private void handleClearForm() {
        tripIdField.clear(); vehicleIdField.clear(); driverIdField.clear();
        reqCapacityField.clear(); sourceField.clear(); destinationField.clear();
        tripDatePicker.setValue(null); startOdoField.clear(); endOdoField.clear();
        statusComboBox.setValue(null); tripTable.getSelectionModel().clearSelection();
        if (distanceField != null) distanceField.clear();
    }

    @FXML private void handleCalculateCosts() {
        if (tripIdField.getText().trim().isEmpty()) { showAlert(Alert.AlertType.WARNING, "No Selection", "Select a trip."); return; }
        if (tripDAO.calculateAndSetTripCosts(tripIdField.getText().trim())) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Trip costs calculated!"); loadTripData();
        } else showAlert(Alert.AlertType.ERROR, "Error", "Failed to calculate costs.");
    }

    private Trip createTripFromFields() {
        Double startOdo = startOdoField.getText().isBlank() ? null : Double.parseDouble(startOdoField.getText().trim());
        Double endOdo = endOdoField.getText().isBlank() ? null : Double.parseDouble(endOdoField.getText().trim());
        return new Trip(tripIdField.getText().trim(), vehicleIdField.getText().trim(), driverIdField.getText().trim(),
                sourceField.getText().trim(), destinationField.getText().trim(), tripDatePicker.getValue(),
                startOdo, endOdo, statusComboBox.getValue(), 0.0, 0.0, 0.0);
    }

    private boolean validateForm() {
        return !tripIdField.getText().isBlank() && !vehicleIdField.getText().isBlank() &&
                !driverIdField.getText().isBlank() && statusComboBox.getValue() != null;
    }

    // ================= CARGO LOGIC =================
    @FXML private void loadAllCargo() { cargoList.setAll(cargoDAO.getAllCargo()); }

    @FXML
    private void handleCheckVehicleCapacity() {
        String tripId = cargoTripIdField.getText().trim();
        if (tripId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Trip ID", "Please enter a Trip ID to check its vehicle capacity.");
            return;
        }

        String[] vehicleInfo = cargoDAO.getVehicleCapacityInfoForTrip(tripId);
        if (vehicleInfo == null) {
            showAlert(Alert.AlertType.ERROR, "Not Found", "No trip found with ID '" + tripId + "' or no vehicle is assigned to it yet.");
            return;
        }

        String vehicleId = vehicleInfo[0];
        String vehicleType = vehicleInfo[1];
        double maxCapacity = Double.parseDouble(vehicleInfo[2]);

        List<Cargo> existingCargo = cargoDAO.getCargoByTripId(tripId);
        double currentTotalWeight = existingCargo.stream().mapToDouble(Cargo::getWeightKg).sum();
        double remainingCapacity = maxCapacity - currentTotalWeight;

        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("Vehicle Capacity Check");
        infoAlert.setHeaderText("Trip: " + tripId + " | Vehicle: " + vehicleId + " (" + vehicleType + ")");
        infoAlert.setContentText(
                "• Maximum Capacity: " + maxCapacity + " kg\n" +
                        "• Currently Used: " + currentTotalWeight + " kg\n" +
                        "• Remaining Capacity: " + remainingCapacity + " kg"
        );
        infoAlert.showAndWait();
    }

    @FXML
    private void showAvailableTripsForCargoPopup() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Trip for Cargo");
        dialog.setHeaderText("Available Trips & Remaining Capacity");

        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        TableView<String[]> table = new TableView<>();
        table.setPrefWidth(480);
        table.setPrefHeight(250);

        TableColumn<String[], String> colTId = new TableColumn<>("Trip ID");
        colTId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colTId.setPrefWidth(90);

        TableColumn<String[], String> colVId = new TableColumn<>("Vehicle");
        colVId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colVId.setPrefWidth(90);

        TableColumn<String[], String> colCap = new TableColumn<>("Total Cap (kg)");
        colCap.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        colCap.setPrefWidth(120);

        TableColumn<String[], String> colRem = new TableColumn<>("Remaining (kg)");
        colRem.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[3]));
        colRem.setPrefWidth(120);

        table.getColumns().addAll(colTId, colVId, colCap, colRem);
        table.getItems().addAll(cargoDAO.getScheduledTripsWithCapacity());

        dialog.getDialogPane().setContent(table);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType && table.getSelectionModel().getSelectedItem() != null) {
                return table.getSelectionModel().getSelectedItem()[0];
            }
            return null;
        });

        dialog.showAndWait().ifPresent(selectedId -> cargoTripIdField.setText(selectedId));
    }

    @FXML private void handleAddCargo() {
        if (cargoTripIdField.getText().isBlank() || cargoTypeField.getText().isBlank() || cargoWeightField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Missing Info", "Trip ID, Goods Type, and Weight are required."); return;
        }
        try {
            Cargo c = new Cargo();
            c.setTripId(cargoTripIdField.getText().trim()); c.setGoodsType(cargoTypeField.getText().trim());
            c.setQuantity(cargoQtyField.getText().isBlank() ? 0 : Integer.parseInt(cargoQtyField.getText().trim()));
            c.setWeightKg(Double.parseDouble(cargoWeightField.getText().trim()));
            c.setDescription(cargoDescField.getText().trim());
            if (cargoDAO.addCargo(c)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Cargo mapped to Trip!");
                loadAllCargo(); handleClearCargoForm();
            } else showAlert(Alert.AlertType.ERROR, "Mapping Error", "Invalid Trip ID or weight exceeds vehicle capacity.");
        } catch (NumberFormatException e) { showAlert(Alert.AlertType.ERROR, "Invalid Input", "Quantity and Weight must be numbers."); }
    }

    @FXML private void handleUpdateCargo() {
        Cargo selectedCargo = cargoTable.getSelectionModel().getSelectedItem();
        if (selectedCargo == null) { showAlert(Alert.AlertType.WARNING, "No Selection", "Select a cargo record to update."); return; }
        try {
            selectedCargo.setTripId(cargoTripIdField.getText().trim());
            selectedCargo.setGoodsType(cargoTypeField.getText().trim());
            selectedCargo.setQuantity(cargoQtyField.getText().isBlank() ? 0 : Integer.parseInt(cargoQtyField.getText().trim()));
            selectedCargo.setWeightKg(Double.parseDouble(cargoWeightField.getText().trim()));
            selectedCargo.setDescription(cargoDescField.getText().trim());
            if (cargoDAO.updateCargo(selectedCargo)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Cargo updated!");
                loadAllCargo(); handleClearCargoForm();
            } else showAlert(Alert.AlertType.ERROR, "Mapping Error", "Invalid Trip ID or weight exceeds vehicle capacity.");
        } catch (NumberFormatException e) { showAlert(Alert.AlertType.ERROR, "Invalid Input", "Quantity and Weight must be numbers."); }
    }

    @FXML private void handleDeleteCargo() {
        Cargo selectedCargo = cargoTable.getSelectionModel().getSelectedItem();
        if (selectedCargo == null) { showAlert(Alert.AlertType.WARNING, "No Selection", "Select cargo to delete."); return; }
        if (cargoDAO.deleteCargo(selectedCargo.getCargoId())) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Cargo deleted."); loadAllCargo(); handleClearCargoForm();
        } else showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete cargo.");
    }

    @FXML private void handleClearCargoForm() {
        cargoTripIdField.clear(); cargoTypeField.clear(); cargoQtyField.clear();
        cargoWeightField.clear(); cargoDescField.clear(); cargoTable.getSelectionModel().clearSelection();
    }

    @FXML private void handleClearAllCargo() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion"); alert.setHeaderText("Delete ALL Cargo?");
        alert.setContentText("Are you sure you want to clear all cargo records?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (cargoDAO.clearAllCargo()) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "All cargo records cleared.");
                loadAllCargo(); handleClearCargoForm();
            } else showAlert(Alert.AlertType.ERROR, "Error", "Failed to clear cargo.");
        }
    }

    @FXML
    private void handleTripSearch() {
        applyTripFilter(searchField.getText(), filterCategoryComboBox.getValue());
    }

    @FXML
    private void handleTripSearchReset() {
        searchField.clear();
        filterCategoryComboBox.setValue("All Fields");
        filteredTripList.setPredicate(t -> true);
    }

    // ================= FUEL MODULE LOGIC =================
    @FXML
    private void loadFuelData() {
        fuelList.setAll(fuelDAO.getAllFuelRecords());
    }

    @FXML
    private void showAvailableTripsForFuelPopup() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Completed Trip");
        dialog.setHeaderText("Choose a completed trip for fuel logging");

        ButtonType selectButtonType = new ButtonType("Select Trip", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        TableView<String[]> table = new TableView<>();
        table.setPrefWidth(400); table.setPrefHeight(200);

        TableColumn<String[], String> colTId = new TableColumn<>("Trip ID");
        colTId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colTId.setPrefWidth(130);

        TableColumn<String[], String> colVId = new TableColumn<>("Vehicle ID");
        colVId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colVId.setPrefWidth(130);

        TableColumn<String[], String> colSt = new TableColumn<>("Status");
        colSt.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        colSt.setPrefWidth(130);

        table.getColumns().addAll(colTId, colVId, colSt);

        table.getItems().addAll(fuelDAO.getAllTripsForSelection());

        dialog.getDialogPane().setContent(table);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType && table.getSelectionModel().getSelectedItem() != null) {
                return table.getSelectionModel().getSelectedItem()[0];
            }
            return null;
        });

        dialog.showAndWait().ifPresent(tripId -> {
            fuelTripIdField.setText(tripId);
            handleFetchTripForFuel();
        });
    }

    @FXML
    private void handleFetchTripForFuel() {
        String tripId = fuelTripIdField.getText().trim();
        if (tripId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Trip ID", "Please enter a Trip ID first.");
            return;
        }

        String[] details = fuelDAO.getCompletedTripDetails(tripId);
        if (details == null) {
            showAlert(Alert.AlertType.ERROR, "Not Found", "Trip ID not found.");
            clearFuelAutoFields();
            return;
        }

        String vehicleId = details[0];
        String fuelType = details[1];
        String status = details[2];
        String distanceStr = details[3];

        if (!"Completed".equalsIgnoreCase(status)) {
            showAlert(Alert.AlertType.WARNING, "Trip In Progress", "Trip is not completed yet. Distance and mileage are not available.");
            fuelVehicleIdField.setText(vehicleId);
            fuelTypeField.setText(fuelType);
            fuelDistanceField.setText("Not Available");
            fuelMileageField.setText("Not Available");
            return;
        }

        fuelVehicleIdField.setText(vehicleId);
        fuelTypeField.setText(fuelType);
        fuelDistanceField.setText(distanceStr);
        calculateFuelMetrics();
    }

    private void clearFuelAutoFields() {
        fuelVehicleIdField.clear();
        fuelTypeField.clear();
        fuelDistanceField.clear();
        fuelCostField.clear();
        fuelMileageField.clear();
    }

    private void setupFuelListeners() {
        fuelUsedField.textProperty().addListener((obs, oldVal, newVal) -> calculateFuelMetrics());
        fuelPriceField.textProperty().addListener((obs, oldVal, newVal) -> calculateFuelMetrics());
    }

    private void calculateFuelMetrics() {
        try {
            if (!fuelUsedField.getText().isBlank() && !fuelPriceField.getText().isBlank()) {
                double fuelUsed = Double.parseDouble(fuelUsedField.getText().trim());
                double price = Double.parseDouble(fuelPriceField.getText().trim());

                if (fuelUsed > 0 && price > 0) {
                    double cost = fuelUsed * price;
                    fuelCostField.setText(String.format("%.2f", cost));
                }
            }

            if (!fuelDistanceField.getText().isBlank() && !fuelDistanceField.getText().equals("Not Available") && !fuelUsedField.getText().isBlank()) {
                double distance = Double.parseDouble(fuelDistanceField.getText().trim());
                double fuelUsed = Double.parseDouble(fuelUsedField.getText().trim());

                if (fuelUsed > 0) {
                    double mileage = distance / fuelUsed;
                    fuelMileageField.setText(String.format("%.2f", mileage));
                }
            }
        } catch (NumberFormatException e) {
            fuelCostField.clear();
            fuelMileageField.clear();
        }
    }

    private void setupFuelRowSelection() {
        fuelTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                fuelIdField.setText(newSel.getFuelId());
                fuelTripIdField.setText(newSel.getTripId());
                fuelVehicleIdField.setText(newSel.getVehicleId());
                fuelTypeField.setText(newSel.getFuelType());
                fuelDistanceField.setText(String.valueOf(newSel.getDistance()));
                fuelUsedField.setText(String.valueOf(newSel.getFuelUsed()));
                fuelPriceField.setText(String.valueOf(newSel.getFuelPrice()));
                fuelCostField.setText(String.valueOf(newSel.getFuelCost()));
                fuelMileageField.setText(String.valueOf(newSel.getMileage()));
            }
        });
    }

    @FXML
    private void handleAddFuel() {
        if (fuelTripIdField.getText().isBlank() || fuelUsedField.getText().isBlank() || fuelPriceField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill in all required fields (Trip ID, Fuel Used, Fuel Price).");
            return;
        }

        try {
            double fuelUsed = Double.parseDouble(fuelUsedField.getText().trim());
            double price = Double.parseDouble(fuelPriceField.getText().trim());

            if (fuelUsed <= 0) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Enter a valid fuel quantity greater than zero.");
                return;
            }
            if (price <= 0) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Fuel Price must be greater than zero.");
                return;
            }

            String tripId = fuelTripIdField.getText().trim();
            String[] tripDetails = fuelDAO.getCompletedTripDetails(tripId);
            if (tripDetails == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Trip ID not found.");
                return;
            }
            if (!"Completed".equalsIgnoreCase(tripDetails[2])) {
                showAlert(Alert.AlertType.ERROR, "Error", "Fuel can only be added to a completed trip.");
                return;
            }

            Fuel fuel = new Fuel();
            fuel.setFuelId(fuelDAO.generateNextFuelId());
            fuel.setTripId(tripId);
            fuel.setVehicleId(tripDetails[0]);
            fuel.setFuelType(tripDetails[1]);
            fuel.setDistance(Double.parseDouble(tripDetails[3]));
            fuel.setFuelUsed(fuelUsed);
            fuel.setFuelPrice(price);
            fuel.setFuelCost(Double.parseDouble(fuelCostField.getText().trim()));
            fuel.setMileage(Double.parseDouble(fuelMileageField.getText().isBlank() ? "0" : fuelMileageField.getText().trim()));

            if (fuelDAO.addFuelRecord(fuel)) {
                tripDAO.calculateAndSetTripCosts(tripId);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Fuel record " + fuel.getFuelId() + " added & Trip Total updated.");
                loadFuelData();
                loadTripData();
                handleClearFuelForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add fuel record.");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numerical quantities.");
        }
    }

    @FXML
    private void handleUpdateFuel() {
        Fuel selected = fuelTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a fuel record from the table to update.");
            return;
        }
        try {
            selected.setFuelUsed(Double.parseDouble(fuelUsedField.getText().trim()));
            selected.setFuelPrice(Double.parseDouble(fuelPriceField.getText().trim()));
            selected.setFuelCost(Double.parseDouble(fuelCostField.getText().trim()));
            selected.setMileage(Double.parseDouble(fuelMileageField.getText().isBlank() ? "0" : fuelMileageField.getText().trim()));

            if (fuelDAO.updateFuelRecord(selected)) {
                tripDAO.calculateAndSetTripCosts(selected.getTripId());
                showAlert(Alert.AlertType.INFORMATION, "Success", "Fuel record updated & Trip Total updated!");
                loadFuelData();
                loadTripData();
                handleClearFuelForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update fuel record.");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Check numerical entries.");
        }
    }

    @FXML
    private void handleDeleteFuel() {
        Fuel selected = fuelTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a fuel record to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Fuel Record " + selected.getFuelId() + "?");
        confirm.setContentText("Are you sure you want to delete this fuel record?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String associatedTripId = selected.getTripId();
            if (fuelDAO.deleteFuelRecord(selected.getFuelId())) {
                tripDAO.calculateAndSetTripCosts(associatedTripId);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Fuel record deleted & Trip Total updated.");
                loadFuelData();
                loadTripData();
                handleClearFuelForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete fuel record.");
            }
        }
    }

    @FXML
    private void handleClearFuelForm() {
        fuelIdField.clear();
        fuelTripIdField.clear();
        fuelVehicleIdField.clear();
        fuelTypeField.clear();
        fuelDistanceField.clear();
        fuelUsedField.clear();
        fuelPriceField.clear();
        fuelCostField.clear();
        fuelMileageField.clear();
        fuelTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleClearAllFuel() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete ALL Fuel Records?");
        alert.setContentText("Are you sure you want to delete ALL fuel records from the database?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (fuelDAO.clearAllFuelRecords()) {
                for (Trip t : tripList) {
                    tripDAO.calculateAndSetTripCosts(t.getTripId());
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "All fuel records cleared & Trip Totals updated.");
                loadFuelData();
                loadTripData();
                handleClearFuelForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to clear fuel records.");
            }
        }
    }

    @FXML
    private void handleFuelSearch() {
        String keyword = fuelSearchField.getText() == null ? "" : fuelSearchField.getText().trim().toLowerCase();
        String category = fuelFilterCategoryComboBox.getValue();

        filteredFuelList.setPredicate(fuel -> {
            if (keyword.isEmpty()) return true;

            switch (category) {
                case "Fuel ID":
                    return fuel.getFuelId() != null && fuel.getFuelId().toLowerCase().contains(keyword);
                case "Trip ID":
                    return fuel.getTripId() != null && fuel.getTripId().toLowerCase().contains(keyword);
                case "Vehicle ID":
                    return fuel.getVehicleId() != null && fuel.getVehicleId().toLowerCase().contains(keyword);
                case "Fuel Type":
                    return fuel.getFuelType() != null && fuel.getFuelType().toLowerCase().contains(keyword);
                default:
                    return (fuel.getFuelId() != null && fuel.getFuelId().toLowerCase().contains(keyword)) ||
                            (fuel.getTripId() != null && fuel.getTripId().toLowerCase().contains(keyword)) ||
                            (fuel.getVehicleId() != null && fuel.getVehicleId().toLowerCase().contains(keyword)) ||
                            (fuel.getFuelType() != null && fuel.getFuelType().toLowerCase().contains(keyword));
            }
        });
    }

    @FXML
    private void handleFuelSearchReset() {
        fuelSearchField.clear();
        fuelFilterCategoryComboBox.setValue("All Fields");
        filteredFuelList.setPredicate(f -> true);
    }

    // ================= TOLL MODULE LOGIC =================
    @FXML
    private void loadTollData() {
        tollList.setAll(tollDAO.getAllTolls());
    }

    private void setupTollRowSelection() {
        tollTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                tollIdField.setText(String.valueOf(newSel.getTollId()));
                tollTripIdField.setText(newSel.getTripId());
                tollNameField.setText(newSel.getTollName());
                tollLocationField.setText(newSel.getLocation());
                tollAmountField.setText(String.valueOf(newSel.getAmount()));
                tollDatePicker.setValue(newSel.getTollDate());
            }
        });
    }

    @FXML
    private void showAvailableTripsForTollPopup() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Active or Completed Trip");
        dialog.setHeaderText("Choose a valid trip to add a toll record");

        ButtonType selectButtonType = new ButtonType("Select Trip", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        TableView<String[]> table = new TableView<>();
        table.setPrefWidth(400); table.setPrefHeight(200);

        TableColumn<String[], String> colTId = new TableColumn<>("Trip ID");
        colTId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colTId.setPrefWidth(130);
        TableColumn<String[], String> colVId = new TableColumn<>("Vehicle ID");
        colVId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colVId.setPrefWidth(130);
        TableColumn<String[], String> colSt = new TableColumn<>("Status");
        colSt.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        colSt.setPrefWidth(130);

        table.getColumns().addAll(colTId, colVId, colSt);

        List<String[]> validTrips = new ArrayList<>();
        for (Trip t : tripDAO.getAllTrips()) {
            if (!"Scheduled".equalsIgnoreCase(t.getStatus())) {
                validTrips.add(new String[]{t.getTripId(), t.getVehicleId(), t.getStatus()});
            }
        }
        table.getItems().addAll(validTrips);

        dialog.getDialogPane().setContent(table);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType && table.getSelectionModel().getSelectedItem() != null) {
                return table.getSelectionModel().getSelectedItem()[0];
            }
            return null;
        });

        dialog.showAndWait().ifPresent(tripId -> tollTripIdField.setText(tripId));
    }

    @FXML
    private void handleAddToll() {
        if (!validateTollForm()) return;

        Toll toll = new Toll();
        toll.setTripId(tollTripIdField.getText().trim());
        toll.setTollName(tollNameField.getText().trim());
        toll.setLocation(tollLocationField.getText().trim());
        toll.setAmount(Double.parseDouble(tollAmountField.getText().trim()));
        toll.setTollDate(tollDatePicker.getValue());

        if (tollDAO.addToll(toll)) {
            tripDAO.calculateAndSetTripCosts(toll.getTripId());
            showAlert(Alert.AlertType.INFORMATION, "Success", "Toll record added & Trip Total updated.");
            loadTollData();
            loadTripData();
            handleClearTollForm();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add toll record.");
        }
    }

    @FXML
    private void handleUpdateToll() {
        Toll selected = tollTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a toll record to update.");
            return;
        }
        if (!validateTollForm()) return;

        selected.setTripId(tollTripIdField.getText().trim());
        selected.setTollName(tollNameField.getText().trim());
        selected.setLocation(tollLocationField.getText().trim());
        selected.setAmount(Double.parseDouble(tollAmountField.getText().trim()));
        selected.setTollDate(tollDatePicker.getValue());

        if (tollDAO.updateToll(selected)) {
            tripDAO.calculateAndSetTripCosts(selected.getTripId());
            showAlert(Alert.AlertType.INFORMATION, "Success", "Toll record updated & Trip Total updated.");
            loadTollData();
            loadTripData();
            handleClearTollForm();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update toll record.");
        }
    }

    @FXML
    private void handleDeleteToll() {
        Toll selected = tollTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a toll record to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Toll Record ID " + selected.getTollId() + "?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String associatedTripId = selected.getTripId();
            if (tollDAO.deleteToll(selected.getTollId())) {
                tripDAO.calculateAndSetTripCosts(associatedTripId);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Toll deleted & Trip Total updated.");
                loadTollData();
                loadTripData();
                handleClearTollForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete toll.");
            }
        }
    }

    @FXML
    private void handleClearAllTolls() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete ALL Toll Records?");
        alert.setContentText("Are you sure you want to delete ALL toll records from the database?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (tollDAO.clearAllTolls()) {
                for (Trip t : tripList) {
                    tripDAO.calculateAndSetTripCosts(t.getTripId());
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "All toll records cleared & Trip Totals updated.");
                loadTollData();
                loadTripData();
                handleClearTollForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to clear toll records.");
            }
        }
    }

    @FXML
    private void handleTollSearch() {
        String keyword = tollSearchField.getText() == null ? "" : tollSearchField.getText().trim().toLowerCase();
        String category = tollFilterCategoryComboBox.getValue();

        filteredTollList.setPredicate(toll -> {
            if (keyword.isEmpty()) return true;
            switch (category) {
                case "Toll ID": return String.valueOf(toll.getTollId()).contains(keyword);
                case "Trip ID": return toll.getTripId().toLowerCase().contains(keyword);
                case "Toll Name": return toll.getTollName().toLowerCase().contains(keyword);
                case "Location": return toll.getLocation().toLowerCase().contains(keyword);
                default:
                    return String.valueOf(toll.getTollId()).contains(keyword) ||
                            toll.getTripId().toLowerCase().contains(keyword) ||
                            toll.getTollName().toLowerCase().contains(keyword) ||
                            toll.getLocation().toLowerCase().contains(keyword);
            }
        });
    }

    @FXML
    private void handleTollSearchReset() {
        tollSearchField.clear();
        tollFilterCategoryComboBox.setValue("All Fields");
        filteredTollList.setPredicate(t -> true);
    }

    @FXML
    private void handleClearTollForm() {
        tollIdField.clear();
        tollTripIdField.clear();
        tollNameField.clear();
        tollLocationField.clear();
        tollAmountField.clear();
        tollDatePicker.setValue(null);
        tollTable.getSelectionModel().clearSelection();
    }

    private boolean validateTollForm() {
        if (tollTripIdField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Select a Trip ID.");
            return false;
        }
        if (tollDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Select a Date.");
            return false;
        }
        if (tollNameField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Enter Toll Name.");
            return false;
        }
        try {
            double amount = Double.parseDouble(tollAmountField.getText().trim());
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Enter a valid amount (greater than 0).");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Amount must be a number.");
            return false;
        }
        return true;
    }

    // ================= EXPENSE MODULE LOGIC =================
    @FXML
    private void loadExpenseData() {
        expenseList.setAll(expenseDAO.getAllExpenses());
    }

    private void setupExpenseRowSelection() {
        expTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                expIdField.setText(String.valueOf(newSel.getExpenseId()));
                expTripIdField.setText(newSel.getTripId());
                expTypeComboBox.setValue(newSel.getExpenseType());
                expAmountField.setText(String.valueOf(newSel.getAmount()));
                expDatePicker.setValue(newSel.getExpenseDate());
                expDescField.setText(newSel.getDescription());
            }
        });
    }

    @FXML
    private void showAvailableTripsForExpensePopup() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Trip");
        dialog.setHeaderText("Choose an active or completed trip for expense logging");

        ButtonType selectButtonType = new ButtonType("Select Trip", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        TableView<String[]> table = new TableView<>();
        table.setPrefWidth(400); table.setPrefHeight(200);

        TableColumn<String[], String> colTId = new TableColumn<>("Trip ID");
        colTId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colTId.setPrefWidth(130);
        TableColumn<String[], String> colVId = new TableColumn<>("Vehicle ID");
        colVId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colVId.setPrefWidth(130);
        TableColumn<String[], String> colSt = new TableColumn<>("Status");
        colSt.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        colSt.setPrefWidth(130);

        table.getColumns().addAll(colTId, colVId, colSt);

        List<String[]> validTrips = new ArrayList<>();
        for (Trip t : tripDAO.getAllTrips()) {
            if (!"Scheduled".equalsIgnoreCase(t.getStatus())) {
                validTrips.add(new String[]{t.getTripId(), t.getVehicleId(), t.getStatus()});
            }
        }
        table.getItems().addAll(validTrips);
        dialog.getDialogPane().setContent(table);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType && table.getSelectionModel().getSelectedItem() != null) {
                return table.getSelectionModel().getSelectedItem()[0];
            }
            return null;
        });

        dialog.showAndWait().ifPresent(tripId -> expTripIdField.setText(tripId));
    }

    @FXML
    private void handleAddExpense() {
        if (!validateExpenseForm()) return;

        Expense expense = new Expense();
        expense.setTripId(expTripIdField.getText().trim());
        expense.setExpenseType(expTypeComboBox.getValue());
        expense.setDescription(expDescField.getText().trim());
        expense.setAmount(Double.parseDouble(expAmountField.getText().trim()));
        expense.setExpenseDate(expDatePicker.getValue());

        if (expenseDAO.addExpense(expense)) {
            tripDAO.calculateAndSetTripCosts(expense.getTripId());
            showAlert(Alert.AlertType.INFORMATION, "Success", "Expense added & Trip Total updated.");
            loadExpenseData();
            loadTripData();
            handleClearExpenseForm();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add expense record.");
        }
    }

    @FXML
    private void handleUpdateExpense() {
        Expense selected = expTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an expense record to update.");
            return;
        }
        if (!validateExpenseForm()) return;

        selected.setTripId(expTripIdField.getText().trim());
        selected.setExpenseType(expTypeComboBox.getValue());
        selected.setDescription(expDescField.getText().trim());
        selected.setAmount(Double.parseDouble(expAmountField.getText().trim()));
        selected.setExpenseDate(expDatePicker.getValue());

        if (expenseDAO.updateExpense(selected)) {
            tripDAO.calculateAndSetTripCosts(selected.getTripId());
            showAlert(Alert.AlertType.INFORMATION, "Success", "Expense updated & Trip Total updated.");
            loadExpenseData();
            loadTripData();
            handleClearExpenseForm();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update expense record.");
        }
    }

    @FXML
    private void handleDeleteExpense() {
        Expense selected = expTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an expense record to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Expense ID " + selected.getExpenseId() + "?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String associatedTripId = selected.getTripId();
            if (expenseDAO.deleteExpense(selected.getExpenseId())) {
                tripDAO.calculateAndSetTripCosts(associatedTripId);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Expense deleted & Trip Total updated.");
                loadExpenseData();
                loadTripData();
                handleClearExpenseForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete expense record.");
            }
        }
    }

    @FXML
    private void handleClearAllExpenses() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete ALL Expense Records?");
        alert.setContentText("Are you sure you want to delete all expense records?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (expenseDAO.clearAllExpenses()) {
                for (Trip t : tripList) {
                    tripDAO.calculateAndSetTripCosts(t.getTripId());
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "All expenses cleared & Trip Totals updated.");
                loadExpenseData();
                loadTripData();
                handleClearExpenseForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to clear expenses.");
            }
        }
    }

    @FXML
    private void handleExpenseSearch() {
        String keyword = expSearchField.getText() == null ? "" : expSearchField.getText().trim().toLowerCase();
        String category = expFilterCategoryComboBox.getValue();

        filteredExpenseList.setPredicate(exp -> {
            if (keyword.isEmpty()) return true;
            switch (category) {
                case "Expense ID": return String.valueOf(exp.getExpenseId()).contains(keyword);
                case "Trip ID": return exp.getTripId().toLowerCase().contains(keyword);
                case "Expense Type": return exp.getExpenseType().toLowerCase().contains(keyword);
                case "Description": return exp.getDescription().toLowerCase().contains(keyword);
                default:
                    return String.valueOf(exp.getExpenseId()).contains(keyword) ||
                            exp.getTripId().toLowerCase().contains(keyword) ||
                            exp.getExpenseType().toLowerCase().contains(keyword) ||
                            exp.getDescription().toLowerCase().contains(keyword);
            }
        });
    }

    @FXML
    private void handleExpenseSearchReset() {
        expSearchField.clear();
        expFilterCategoryComboBox.setValue("All Fields");
        filteredExpenseList.setPredicate(e -> true);
    }

    @FXML
    private void handleClearExpenseForm() {
        expIdField.clear();
        expTripIdField.clear();
        expTypeComboBox.setValue(null);
        expDatePicker.setValue(null);
        expAmountField.clear();
        expDescField.clear();
        expTable.getSelectionModel().clearSelection();
    }

    private boolean validateExpenseForm() {
        if (expTripIdField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Select a Trip ID.");
            return false;
        }
        if (expTypeComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Select an Expense Type.");
            return false;
        }
        if (expDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Select a Date.");
            return false;
        }
        try {
            double amount = Double.parseDouble(expAmountField.getText().trim());
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Enter a valid amount greater than 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Amount must be a numerical value.");
            return false;
        }
        return true;
    }

    // ================= PANE SWITCHING LOGIC =================
    @FXML private void showTripPane() {
        switchPanes(paneTripDetails, btnTabTrip, "Trip Management - Trip Details");
    }
    @FXML private void showCargoPane() {
        switchPanes(paneCargo, btnTabCargo, "Trip Management - Cargo / Goods");
    }
    @FXML private void showFuelPane() {
        switchPanes(paneFuel, btnTabFuel, "Trip Management - Fuel");
        // loadFuelData(); // Uncomment if you have this method
    }
    @FXML private void showTollPane() {
        switchPanes(paneToll, btnTabToll, "Trip Management - Toll");
        // loadTollData(); // Uncomment if you have this method
    }
    @FXML private void showExpensesPane() {
        switchPanes(paneExpenses, btnTabExpenses, "Trip Management - Expenses");
        // loadExpenseData(); // Uncomment if you have this method
    }
    @FXML private void showTrackingPane() {
        switchPanes(paneTracking, btnTabTracking, "Trip Management - Route Tracking");
    }

    private void switchPanes(VBox activePane, Button activeButton, String headerText) {
        // 1. Hide ALL panes
        paneTripDetails.setVisible(false);
        paneCargo.setVisible(false);
        paneFuel.setVisible(false);
        paneToll.setVisible(false);
        paneExpenses.setVisible(false);
        paneTracking.setVisible(false); // Added Tracking

        // 2. Reset ALL buttons to gray
        String inactiveStyle = "-fx-background-color: #e2e8f0; -fx-text-fill: #0f172a; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;";
        btnTabTrip.setStyle(inactiveStyle);
        btnTabCargo.setStyle(inactiveStyle);
        btnTabFuel.setStyle(inactiveStyle);
        btnTabToll.setStyle(inactiveStyle);
        btnTabExpenses.setStyle(inactiveStyle);
        btnTabTracking.setStyle(inactiveStyle); // Added Tracking

        // 3. Set the active pane, active button to blue, and update title
        activePane.setVisible(true);
        activeButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        headerLabel.setText(headerText);
    }

    private void setupLiveMap() {
        javafx.application.Platform.runLater(() -> {
            WebEngine webEngine = mapWebView.getEngine();
            webEngine.setJavaScriptEnabled(true);

            String mapHTML = "<!DOCTYPE html><html><head>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css' />"
                    + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                    + "<style>"
                    + "  html, body { height: 100%; margin: 0; padding: 0; background-color: #f1f5f9; } "
                    + "  /* Absolute positioning forces the map to strictly fit the JavaFX container */ "
                    + "  #map { position: absolute; top: 0; bottom: 0; left: 0; right: 0; } "
                    + "</style>"
                    + "</head><body>"
                    + "<div id='map'></div>"
                    + "<script>"
                    + "  var map = L.map('map').setView([12.9165, 79.1325], 10); " // Centered on Vellore
                    + "  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { "
                    + "      maxZoom: 19, "
                    + "      attribution: '© OpenStreetMap' "
                    + "  }).addTo(map);"
                    + "  "
                    + "  // The Magic Fix: Forces Leaflet to fetch the missing grey tiles after 300ms "
                    + "  setTimeout(function() { map.invalidateSize(); }, 300);"
                    + "</script></body></html>";

            webEngine.loadContent(mapHTML);
        });
    }
    // ================= SIDEBAR & NAVIGATION =================

    @FXML
    private void toggleSidebar() {
        boolean isVisible = sidebar.isVisible();
        sidebar.setVisible(!isVisible);
        sidebar.setManaged(!isVisible);
        if (btnHamburger != null) {
            btnHamburger.setVisible(isVisible);
            btnHamburger.setManaged(isVisible);
        }
    }

    @FXML private void switchToDashboard() { loadScene("/dashboard_view.fxml", "Dashboard"); }
    @FXML private void switchToVehicles() { loadScene("/vehicle_view.fxml", "Vehicle Management"); }
    @FXML private void switchToDrivers() { loadScene("/driver_view.fxml", "Driver Management"); }
    @FXML private void switchToTrips() { loadScene("/trip_view.fxml", "Trip Management"); }
    @FXML private void switchToMaintenance() { loadScene("/maintenance_view.fxml", "Maintenance Management"); }
    @FXML private void switchToReports() { loadScene("/report_view.fxml", "Reports"); }
    @FXML private void switchToSettings() { loadScene("/settings_view.fxml", "Settings"); }
    @FXML private void switchToIncidents() { loadScene("/incident_view.fxml", "Incident Management"); }


    // --- ROUTE TRACKING TAB ---
    @FXML private Button btnTabTracking;
    @FXML private VBox paneTracking;

    @FXML private TextField trackTripIdField;
    @FXML private TextField trackSequenceField;
    @FXML private TextField trackLocationField;
    @FXML private DatePicker trackExpectedDatePicker;



    private void loadScene(String fxmlPath, String title) {
        try {
            URL fileUrl = getClass().getResource(fxmlPath);
            if (fileUrl == null) return;
            javafx.stage.Stage stage = (javafx.stage.Stage) tripTable.getScene().getWindow();
            javafx.scene.Parent root = new javafx.fxml.FXMLLoader(fileUrl).load();
            stage.setScene(new javafx.scene.Scene(root, 1050, 680));
            stage.setTitle("Fleet Management System V2 - " + title);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg); alert.showAndWait();
    }




    // --- ROUTE TRACKING ACTION HANDLERS ---

    @FXML
    private void showAvailableTripsForTrackingPopup() {
        // 1. Create the Dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Trip for Tracking");
        dialog.setHeaderText("Available Trips (Scheduled / In Progress)");

        // 2. Set the buttons (Select and Cancel)
        ButtonType selectButtonType = new ButtonType("Select", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        // 3. Create the TableView for the Popup
        TableView<TrackingTripModel> table = new TableView<>();
        table.setPrefWidth(500);
        table.setPrefHeight(250);

        TableColumn<TrackingTripModel, String> colId = new TableColumn<>("Trip ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("tripId"));

        TableColumn<TrackingTripModel, String> colVeh = new TableColumn<>("Vehicle");
        colVeh.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));

        TableColumn<TrackingTripModel, String> colSrc = new TableColumn<>("Source");
        colSrc.setCellValueFactory(new PropertyValueFactory<>("source"));

        TableColumn<TrackingTripModel, String> colDest = new TableColumn<>("Destination");
        colDest.setCellValueFactory(new PropertyValueFactory<>("dest"));

        TableColumn<TrackingTripModel, String> colStat = new TableColumn<>("Status");
        colStat.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(colId, colVeh, colSrc, colDest, colStat);

        // 4. Fetch Active Trips from Database
        ObservableList<TrackingTripModel> activeTrips = FXCollections.observableArrayList();
        String query = "SELECT trip_id, vehicle_id, source, destination, status FROM trips WHERE status IN ('Scheduled', 'In Progress')";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                activeTrips.add(new TrackingTripModel(
                        rs.getString("trip_id"),
                        rs.getString("vehicle_id"),
                        rs.getString("source"),
                        rs.getString("destination"),
                        rs.getString("status")
                ));
            }
            table.setItems(activeTrips);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 5. Add Table to Dialog and Handle Result
        dialog.getDialogPane().setContent(table);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType && table.getSelectionModel().getSelectedItem() != null) {
                return table.getSelectionModel().getSelectedItem().getTripId(); // Return selected ID
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(tripId -> trackTripIdField.setText(tripId)); // Auto-fill the text box!
    }
    @FXML
    private void handleAddCheckpoint() {
        String tripId = trackTripIdField.getText();
        String seqStr = trackSequenceField.getText();
        String location = trackLocationField.getText();

        // 1. Validation
        if (tripId.isEmpty() || seqStr.isEmpty() || location.isEmpty() || trackExpectedDatePicker.getValue() == null) {
            Alert alert = new Alert(AlertType.WARNING, "Please fill all fields before adding a checkpoint!");
            alert.show();
            return;
        }

        // 2. Database Insert
        String query = "INSERT INTO trip_checkpoints (trip_id, sequence_num, location_name, expected_time, status) VALUES (?, ?, ?, ?, 'Pending')";

        // 3. Using your exact Singleton Database Connection
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, tripId);
            pstmt.setInt(2, Integer.parseInt(seqStr));
            pstmt.setString(3, location);
            pstmt.setString(4, trackExpectedDatePicker.getValue().toString() + " 00:00:00");

            pstmt.executeUpdate();

            Alert success = new Alert(AlertType.INFORMATION, "Checkpoint added successfully!");
            success.show();

            // Clear the form after success
            handleClearTrackingForm();
            loadAllCheckpoints();

        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(AlertType.ERROR, "Database Error: " + e.getMessage());
            error.show();
        }
    }

    @FXML
    private void handleDeleteCheckpoint() {
        Checkpoint selected = trackingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(AlertType.WARNING, "Please select a checkpoint from the table to delete!").show();
            return;
        }

        String query = "DELETE FROM trip_checkpoints WHERE trip_id = ? AND sequence_num = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, selected.getTripId());
            pstmt.setInt(2, selected.getSequence());
            pstmt.executeUpdate();

            new Alert(AlertType.INFORMATION, "Checkpoint deleted!").show();
            loadAllCheckpoints(); // Refresh table

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearTrackingForm() {
        trackTripIdField.clear();
        trackSequenceField.clear();
        trackLocationField.clear();
        trackExpectedDatePicker.setValue(null);
    }

    @FXML
    private void loadAllCheckpoints() {
        ObservableList<Checkpoint> checkpointList = FXCollections.observableArrayList();
        String query = "SELECT * FROM trip_checkpoints ORDER BY trip_id, sequence_num ASC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                checkpointList.add(new Checkpoint(
                        rs.getInt("sequence_num"),
                        rs.getString("trip_id"),
                        rs.getString("location_name"),
                        rs.getString("expected_time"),
                        rs.getString("actual_time"),
                        rs.getString("status")
                ));
            }
            trackingTable.setItems(checkpointList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void showAvailableDriversPopup() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Available Drivers");
        dialog.setHeaderText("Showing available drivers with valid licenses");

        ButtonType selectButtonType = new ButtonType("Select Driver", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        TableView<String[]> table = new TableView<>();
        table.setPrefWidth(480);
        table.setPrefHeight(200);

        TableColumn<String[], String> colId = new TableColumn<>("Driver ID");
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colId.setPrefWidth(80);

        TableColumn<String[], String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colName.setPrefWidth(120);

        TableColumn<String[], String> colLic = new TableColumn<>("License No");
        colLic.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        colLic.setPrefWidth(140);

        TableColumn<String[], String> colExp = new TableColumn<>("Expiry Date");
        colExp.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[3]));
        colExp.setPrefWidth(100);

        table.getColumns().addAll(colId, colName, colLic, colExp);

        List<String[]> availableDrivers = tripDAO.getAvailableDrivers();
        table.getItems().addAll(availableDrivers);

        dialog.getDialogPane().setContent(table);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType && table.getSelectionModel().getSelectedItem() != null) {
                return table.getSelectionModel().getSelectedItem()[0];
            }
            return null;
        });

        dialog.showAndWait().ifPresent(selectedId -> driverIdField.setText(selectedId));
    }
    // --- Helper Class for the Popup Table ---
    public static class TrackingTripModel {
        private String tripId, vehicleId, source, dest, status;

        public TrackingTripModel(String tripId, String vehicleId, String source, String dest, String status) {
            this.tripId = tripId;
            this.vehicleId = vehicleId;
            this.source = source;
            this.dest = dest;
            this.status = status;
        }

        public String getTripId() { return tripId; }
        public String getVehicleId() { return vehicleId; }
        public String getSource() { return source; }
        public String getDest() { return dest; }
        public String getStatus() { return status; }
    }
}

