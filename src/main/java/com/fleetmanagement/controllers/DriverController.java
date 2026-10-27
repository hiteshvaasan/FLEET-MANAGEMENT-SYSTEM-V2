package com.fleetmanagement.controllers;

import com.fleetmanagement.dao.DriverDAO;
import com.fleetmanagement.model.Driver;
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
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class DriverController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Button btnHamburger;

    @FXML private TextField driverIdField;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField licenseNoField;
    @FXML private DatePicker licenseExpiryPicker;
    @FXML private ComboBox<String> statusComboBox;

    @FXML private Button btnAdd, btnSearch, btnUpdate, btnDelete, btnClear, btnClearAll;

    @FXML private ComboBox<String> filterCategoryComboBox;
    @FXML private TextField searchField;

    @FXML private TableView<Driver> driverTable;
    @FXML private TableColumn<Driver, String> colDriverId;
    @FXML private TableColumn<Driver, String> colName;
    @FXML private TableColumn<Driver, String> colPhone;
    @FXML private TableColumn<Driver, String> colLicenseNo;
    @FXML private TableColumn<Driver, LocalDate> colLicenseExpiry;
    @FXML private TableColumn<Driver, String> colStatus;

    private final DriverDAO driverDAO = new DriverDAO();
    private final ObservableList<Driver> driverList = FXCollections.observableArrayList();
    private FilteredList<Driver> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ThemeManager.isDarkMode() && rootPane != null) {
            rootPane.setStyle("-fx-background-color: #0f172a;");
        }

        statusComboBox.setItems(FXCollections.observableArrayList("Available", "On Trip", "Inactive", "Off Duty"));
        filterCategoryComboBox.setItems(FXCollections.observableArrayList("All Fields", "Driver ID", "License Number", "Status"));
        filterCategoryComboBox.setValue("All Fields");

        colDriverId.setCellValueFactory(new PropertyValueFactory<>("driverId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("driverName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colLicenseNo.setCellValueFactory(new PropertyValueFactory<>("licenseNumber"));
        colLicenseExpiry.setCellValueFactory(new PropertyValueFactory<>("licenseExpiry"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        filteredList = new FilteredList<>(driverList, d -> true);
        driverTable.setItems(filteredList);

        loadDriverData();
        setupRowSelectionListener();
    }

    private void loadDriverData() {
        List<Driver> drivers = driverDAO.getAllDrivers();
        driverList.setAll(drivers);
    }

    private void setupRowSelectionListener() {
        driverTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                driverIdField.setText(newSel.getDriverId());
                nameField.setText(newSel.getDriverName());
                phoneField.setText(newSel.getPhone());
                licenseNoField.setText(newSel.getLicenseNumber());
                licenseExpiryPicker.setValue(newSel.getLicenseExpiry());
                statusComboBox.setValue(newSel.getStatus());
            }
        });
    }

    @FXML
    private void handleAddDriver() {
        if (!validateForm()) return;

        LocalDate expiryDate = licenseExpiryPicker.getValue();
        String status = statusComboBox.getValue();

        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            status = "Inactive";
            showAlert(AlertType.WARNING, "License Expired", "License is expired! Driver status automatically set to Inactive.");
        }

        Driver driver = new Driver(
                driverIdField.getText().trim(),
                nameField.getText().trim(),
                phoneField.getText().trim(),
                licenseNoField.getText().trim(),
                expiryDate,
                status,
                null
        );

        if (driverDAO.addDriver(driver)) {
            showAlert(AlertType.INFORMATION, "Success", "Driver added successfully!");
            loadDriverData();
            handleClearForm();
        } else {
            showAlert(AlertType.ERROR, "Error", "Failed to add driver. Check if ID or license number already exists.");
        }
    }

    @FXML
    private void handleUpdateDriver() {
        String driverId = driverIdField.getText().trim();
        if (driverId.isEmpty()) {
            showAlert(AlertType.WARNING, "No Selection", "Please enter or select a Driver ID.");
            return;
        }

        LocalDate expiryDate = licenseExpiryPicker.getValue();
        String status = statusComboBox.getValue();
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            status = "Inactive";
        }

        if (driverDAO.updateDriverStatus(driverId, status)) {
            showAlert(AlertType.INFORMATION, "Success", "Driver status updated successfully!");
            loadDriverData();
            handleClearForm();
        } else {
            showAlert(AlertType.ERROR, "Error", "Update failed.");
        }
    }

    @FXML
    private void handleDeleteDriver() {
        String driverId = driverIdField.getText().trim();
        if (driverId.isEmpty()) {
            showAlert(AlertType.WARNING, "No Selection", "Please select a driver to delete.");
            return;
        }
        if (driverDAO.deleteDriver(driverId)) {
            showAlert(AlertType.INFORMATION, "Success", "Driver deleted successfully!");
            loadDriverData();
            handleClearForm();
        } else {
            showAlert(AlertType.ERROR, "Error", "Delete failed. Driver may be linked to active trips.");
        }
    }

    @FXML
    private void handleSearchDriver() {
        String searchText = searchField.getText();
        String category = filterCategoryComboBox.getValue();

        filteredList.setPredicate(driver -> {
            if (searchText == null || searchText.isBlank()) return true;
            String lower = searchText.toLowerCase();
            String cat = category == null ? "All Fields" : category;

            switch (cat) {
                case "Driver ID":
                    return driver.getDriverId() != null && driver.getDriverId().toLowerCase().contains(lower);
                case "License Number":
                    return driver.getLicenseNumber() != null && driver.getLicenseNumber().toLowerCase().contains(lower);
                case "Status":
                    return driver.getStatus() != null && driver.getStatus().toLowerCase().contains(lower);
                case "All Fields":
                default:
                    return (driver.getDriverId() != null && driver.getDriverId().toLowerCase().contains(lower)) ||
                            (driver.getDriverName() != null && driver.getDriverName().toLowerCase().contains(lower)) ||
                            (driver.getPhone() != null && driver.getPhone().toLowerCase().contains(lower)) ||
                            (driver.getLicenseNumber() != null && driver.getLicenseNumber().toLowerCase().contains(lower)) ||
                            (driver.getStatus() != null && driver.getStatus().toLowerCase().contains(lower));
            }
        });
    }

    @FXML
    private void handleResetSearchAction() {
        searchField.clear();
        filterCategoryComboBox.setValue("All Fields");
        filteredList.setPredicate(d -> true);
    }

    @FXML
    private void handleClearForm() {
        driverIdField.clear();
        nameField.clear();
        phoneField.clear();
        licenseNoField.clear();
        licenseExpiryPicker.setValue(null);
        statusComboBox.setValue(null);
        driverTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleClearAllRecords() {
        if (driverDAO.deleteAllDrivers()) {
            showAlert(AlertType.INFORMATION, "Success", "All driver records wiped.");
            loadDriverData();
            handleClearForm();
        } else {
            showAlert(AlertType.ERROR, "Error", "Could not clear records.");
        }
    }

    private boolean validateForm() {
        return !driverIdField.getText().isBlank() &&
                !nameField.getText().isBlank() &&
                !phoneField.getText().isBlank() &&
                !licenseNoField.getText().isBlank() &&
                licenseExpiryPicker.getValue() != null &&
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

            if (ThemeManager.isDarkMode() && root instanceof BorderPane) {
                ((BorderPane) root).setStyle("-fx-background-color: #0f172a;");
            }

            // Swap the root smoothly without destroying the window
            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new Scene(root, 1050, 680));
            }

            stage.setTitle("Fleet Management System V2 - " + title);

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