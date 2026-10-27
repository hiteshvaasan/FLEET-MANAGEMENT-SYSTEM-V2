package com.fleetmanagement.controllers;

import com.fleetmanagement.dao.VehicleDAO;
import com.fleetmanagement.model.Vehicle;
import com.fleetmanagement.utils.ThemeManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class VehicleController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Button btnHamburger;

    @FXML private TextField vehicleIdField;
    @FXML private TextField regNoField;
    @FXML private ComboBox<String> vehicleTypeComboBox;
    @FXML private TextField modelField;
    @FXML private TextField capacityField;
    @FXML private ComboBox<String> fuelTypeComboBox;
    @FXML private ComboBox<String> statusComboBox;

    @FXML private Button btnAdd, btnSearch, btnUpdate, btnDelete, btnClear, btnClearAll;

    @FXML private ComboBox<String> filterCategoryComboBox;
    @FXML private TextField searchField;
    @FXML private TableView<Vehicle> vehicleTable;
    @FXML private TableColumn<Vehicle, String> colVehicleId;
    @FXML private TableColumn<Vehicle, String> colRegNo;
    @FXML private TableColumn<Vehicle, String> colVehicleType;
    @FXML private TableColumn<Vehicle, String> colModel;
    @FXML private TableColumn<Vehicle, Double> colCapacity;
    @FXML private TableColumn<Vehicle, String> colFuelType;
    @FXML private TableColumn<Vehicle, String> colStatus;

    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final ObservableList<Vehicle> vehicleList = FXCollections.observableArrayList();
    private FilteredList<Vehicle> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ThemeManager.isDarkMode() && rootPane != null) {
            rootPane.setStyle("-fx-background-color: #0f172a;");
        }

        vehicleTypeComboBox.setItems(FXCollections.observableArrayList("Truck", "Lorry", "Van", "Container"));
        fuelTypeComboBox.setItems(FXCollections.observableArrayList("Diesel", "Petrol", "Electric", "CNG"));
        statusComboBox.setItems(FXCollections.observableArrayList("Available", "On Trip", "Under Maintenance"));

        filterCategoryComboBox.setItems(FXCollections.observableArrayList("All Fields", "Vehicle ID", "Vehicle Type", "Fuel Type", "Status"));
        filterCategoryComboBox.setValue("All Fields");

        colVehicleId.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));
        colRegNo.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        colVehicleType.setCellValueFactory(new PropertyValueFactory<>("vehicleType"));
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacityKg"));
        colFuelType.setCellValueFactory(new PropertyValueFactory<>("fuelType"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        filteredList = new FilteredList<>(vehicleList, v -> true);
        vehicleTable.setItems(filteredList);

        loadVehicleData();
        setupRowSelectionListener();
    }

    private void loadVehicleData() {
        List<Vehicle> vehicles = vehicleDAO.getAllVehicles();
        vehicleList.setAll(vehicles);
    }

    private void setupRowSelectionListener() {
        vehicleTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                vehicleIdField.setText(newSel.getVehicleId());
                regNoField.setText(newSel.getRegistrationNumber());
                vehicleTypeComboBox.setValue(newSel.getVehicleType());
                modelField.setText(newSel.getModel());
                capacityField.setText(String.valueOf(newSel.getCapacityKg()));
                fuelTypeComboBox.setValue(newSel.getFuelType());
                statusComboBox.setValue(newSel.getStatus());
            }
        });
    }

    @FXML
    private void handleAddVehicle() {
        if (!validateForm()) return;
        try {
            Vehicle vehicle = new Vehicle(
                    vehicleIdField.getText().trim(),
                    regNoField.getText().trim(),
                    vehicleTypeComboBox.getValue(),
                    modelField.getText().trim(),
                    Double.parseDouble(capacityField.getText().trim()),
                    fuelTypeComboBox.getValue(),
                    statusComboBox.getValue()
            );
            if (vehicleDAO.addVehicle(vehicle)) {
                showAlert(AlertType.INFORMATION, "Success", "Vehicle added successfully!");
                loadVehicleData();
                handleClearForm();
            } else {
                showAlert(AlertType.ERROR, "Error", "Failed to add vehicle.");
            }
        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Invalid Input", "Capacity must be a valid number.");
        }
    }

    @FXML
    private void handleUpdateVehicle() {
        String vehicleId = vehicleIdField.getText().trim();
        if (vehicleId.isEmpty()) {
            showAlert(AlertType.WARNING, "No Selection", "Please enter or select a Vehicle ID.");
            return;
        }
        if (vehicleDAO.updateVehicleStatus(vehicleId, statusComboBox.getValue())) {
            showAlert(AlertType.INFORMATION, "Success", "Vehicle status updated!");
            loadVehicleData();
            handleClearForm();
        } else {
            showAlert(AlertType.ERROR, "Error", "Update failed.");
        }
    }

    @FXML
    private void handleDeleteVehicle() {
        String vehicleId = vehicleIdField.getText().trim();
        if (vehicleId.isEmpty()) {
            showAlert(AlertType.WARNING, "No Selection", "Please select a vehicle to delete.");
            return;
        }
        if (vehicleDAO.deleteVehicle(vehicleId)) {
            showAlert(AlertType.INFORMATION, "Success", "Vehicle deleted!");
            loadVehicleData();
            handleClearForm();
        } else {
            showAlert(AlertType.ERROR, "Error", "Delete failed. May be linked to active trips.");
        }
    }

    @FXML
    private void handleSearchVehicle() {
        String searchText = searchField.getText();
        String category = filterCategoryComboBox.getValue();

        filteredList.setPredicate(vehicle -> {
            if (searchText == null || searchText.isBlank()) return true;
            String lower = searchText.toLowerCase();
            String cat = category == null ? "All Fields" : category;

            switch (cat) {
                case "Vehicle ID":
                    return vehicle.getVehicleId() != null && vehicle.getVehicleId().toLowerCase().contains(lower);
                case "Vehicle Type":
                    return vehicle.getVehicleType() != null && vehicle.getVehicleType().toLowerCase().contains(lower);
                case "Fuel Type":
                    return vehicle.getFuelType() != null && vehicle.getFuelType().toLowerCase().contains(lower);
                case "Status":
                    return vehicle.getStatus() != null && vehicle.getStatus().toLowerCase().contains(lower);
                case "All Fields":
                default:
                    return (vehicle.getVehicleId() != null && vehicle.getVehicleId().toLowerCase().contains(lower)) ||
                            (vehicle.getRegistrationNumber() != null && vehicle.getRegistrationNumber().toLowerCase().contains(lower)) ||
                            (vehicle.getVehicleType() != null && vehicle.getVehicleType().toLowerCase().contains(lower)) ||
                            (vehicle.getModel() != null && vehicle.getModel().toLowerCase().contains(lower)) ||
                            (vehicle.getFuelType() != null && vehicle.getFuelType().toLowerCase().contains(lower)) ||
                            (vehicle.getStatus() != null && vehicle.getStatus().toLowerCase().contains(lower));
            }
        });
    }

    @FXML
    private void handleResetSearchAction() {
        searchField.clear();
        filterCategoryComboBox.setValue("All Fields");
        filteredList.setPredicate(v -> true);
    }

    @FXML
    private void handleClearForm() {
        vehicleIdField.clear();
        regNoField.clear();
        vehicleTypeComboBox.setValue(null);
        modelField.clear();
        capacityField.clear();
        fuelTypeComboBox.setValue(null);
        statusComboBox.setValue(null);
        vehicleTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleClearAllRecords() {
        if (vehicleDAO.deleteAllVehicles()) {
            showAlert(AlertType.INFORMATION, "Success", "All vehicle records wiped.");
            loadVehicleData();
            handleClearForm();
        } else {
            showAlert(AlertType.ERROR, "Error", "Could not clear records.");
        }
    }

    private boolean validateForm() {
        return !vehicleIdField.getText().isBlank() &&
                !regNoField.getText().isBlank() &&
                vehicleTypeComboBox.getValue() != null &&
                !capacityField.getText().isBlank() &&
                fuelTypeComboBox.getValue() != null &&
                statusComboBox.getValue() != null;
    }

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
    @FXML private void switchToOrderRequests() { loadScene("/orderrequest_view.fxml", "Order Requests"); }
    @FXML private void switchToOrderProof() { loadScene("/orderproof_view.fxml", "Order Proof"); }
    @FXML
    private void switchToReceivers() {
        loadScene("/receiver_view.fxml", "Receiver Management");
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            URL fileUrl = getClass().getResource(fxmlPath);
            if (fileUrl == null) return;

            Stage stage = (Stage) rootPane.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(fileUrl);
            Parent root = loader.load();
            stage.setScene(new Scene(root, 1050, 680));
            stage.setTitle("Fleet Management System V2 - " + title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}