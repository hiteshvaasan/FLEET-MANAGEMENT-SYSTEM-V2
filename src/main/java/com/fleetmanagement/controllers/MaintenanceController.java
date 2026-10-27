package com.fleetmanagement.controllers;

import com.fleetmanagement.dao.MaintenanceDAO;
import com.fleetmanagement.model.Maintenance;
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

public class MaintenanceController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Button btnHamburger;

    @FXML private TextField maintenanceIdField;
    @FXML private TextField vehicleIdField;
    @FXML private DatePicker maintenanceDatePicker;
    @FXML private ComboBox<String> maintenanceTypeComboBox;
    @FXML private TextField costField;
    @FXML private DatePicker nextServiceDatePicker;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextField descriptionField;

    @FXML private Button btnAddMaintenance, btnUpdate, btnDelete, btnClear, btnClearAll, btnSearch, btnReset;

    @FXML private ComboBox<String> filterCategory1ComboBox;
    @FXML private ComboBox<String> filterCategory2ComboBox;
    @FXML private TextField searchField;

    @FXML private TableView<Maintenance> maintenanceTable;
    @FXML private TableColumn<Maintenance, String> colId;
    @FXML private TableColumn<Maintenance, String> colVehicle;
    @FXML private TableColumn<Maintenance, LocalDate> colDate;
    @FXML private TableColumn<Maintenance, String> colType;
    @FXML private TableColumn<Maintenance, Double> colCost;
    @FXML private TableColumn<Maintenance, LocalDate> colNextService;
    @FXML private TableColumn<Maintenance, String> colStatus;
    @FXML private TableColumn<Maintenance, String> colDescription;

    private final MaintenanceDAO maintenanceDAO = new MaintenanceDAO();
    private final ObservableList<Maintenance> maintenanceList = FXCollections.observableArrayList();
    private FilteredList<Maintenance> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ThemeManager.isDarkMode() && rootPane != null) {
            rootPane.setStyle("-fx-background-color: #0f172a;");
        }

        maintenanceTypeComboBox.setItems(FXCollections.observableArrayList("Service", "Repair", "Inspection", "Tyre Replacement", "Oil Change"));
        statusComboBox.setItems(FXCollections.observableArrayList("In Progress", "Completed", "Pending"));

        filterCategory1ComboBox.setItems(FXCollections.observableArrayList("All Vehicles", "Vehicle ID", "Status"));
        filterCategory1ComboBox.setValue("All Vehicles");

        filterCategory2ComboBox.setItems(FXCollections.observableArrayList("All", "Maintenance ID", "Maintenance Type", "Description"));
        filterCategory2ComboBox.setValue("All");

        colId.setCellValueFactory(new PropertyValueFactory<>("formattedId"));
        colVehicle.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("maintenanceDate"));
        colType.setCellValueFactory(new PropertyValueFactory<>("maintenanceType"));
        colCost.setCellValueFactory(new PropertyValueFactory<>("cost"));
        colNextService.setCellValueFactory(new PropertyValueFactory<>("nextServiceDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        filteredList = new FilteredList<>(maintenanceList, m -> true);
        maintenanceTable.setItems(filteredList);

        loadMaintenanceData();
        setupRowSelectionListener();
    }

    private void loadMaintenanceData() {
        List<Maintenance> list = maintenanceDAO.getAllMaintenance();
        maintenanceList.setAll(list);
    }

    private void setupRowSelectionListener() {
        maintenanceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                maintenanceIdField.setText(newSel.getFormattedId());
                vehicleIdField.setText(newSel.getVehicleId());
                maintenanceDatePicker.setValue(newSel.getMaintenanceDate());
                maintenanceTypeComboBox.setValue(newSel.getMaintenanceType());
                costField.setText(String.valueOf(newSel.getCost()));
                nextServiceDatePicker.setValue(newSel.getNextServiceDate());
                statusComboBox.setValue(newSel.getStatus());
                descriptionField.setText(newSel.getDescription());
            }
        });
    }

    @FXML
    private void showAvailableVehiclesPopup() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Vehicle");
        dialog.setHeaderText("Choose a vehicle for maintenance");

        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        TableView<String[]> table = new TableView<>();
        table.setPrefWidth(400);
        table.setPrefHeight(200);

        TableColumn<String[], String> colVId = new TableColumn<>("Vehicle ID");
        colVId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colVId.setPrefWidth(130);

        TableColumn<String[], String> colReg = new TableColumn<>("Registration No");
        colReg.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colReg.setPrefWidth(140);

        TableColumn<String[], String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        colType.setPrefWidth(110);

        table.getColumns().addAll(colVId, colReg, colType);
        table.getItems().addAll(maintenanceDAO.getAvailableVehicles());

        dialog.getDialogPane().setContent(table);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType && table.getSelectionModel().getSelectedItem() != null) {
                return table.getSelectionModel().getSelectedItem()[0];
            }
            return null;
        });

        dialog.showAndWait().ifPresent(selectedId -> vehicleIdField.setText(selectedId));
    }

    @FXML
    private void handleAddMaintenance() {
        if (!validateForm()) return;

        try {
            Maintenance maintenance = new Maintenance();
            maintenance.setVehicleId(vehicleIdField.getText().trim());
            maintenance.setMaintenanceDate(maintenanceDatePicker.getValue());
            maintenance.setMaintenanceType(maintenanceTypeComboBox.getValue());
            maintenance.setCost(Double.parseDouble(costField.getText().trim()));
            maintenance.setNextServiceDate(nextServiceDatePicker.getValue());
            maintenance.setStatus(statusComboBox.getValue());
            maintenance.setDescription(descriptionField.getText().trim());

            if (maintenanceDAO.addMaintenance(maintenance)) {
                showAlert(AlertType.INFORMATION, "Success", "Maintenance record added successfully!");
                loadMaintenanceData();
                handleClearForm();
            } else {
                showAlert(AlertType.ERROR, "Error", "Failed to add maintenance record.");
            }
        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Invalid Input", "Cost must be a valid numerical value.");
        }
    }

    @FXML
    private void handleUpdateMaintenance() {
        String mIdText = maintenanceIdField.getText().trim();
        if (mIdText.isEmpty()) {
            showAlert(AlertType.WARNING, "No Selection", "Please select a maintenance record to update.");
            return;
        }

        try {
            int realId = Integer.parseInt(mIdText.replaceAll("[^0-9]", ""));

            Maintenance maintenance = new Maintenance();
            maintenance.setMaintenanceId(realId);
            maintenance.setVehicleId(vehicleIdField.getText().trim());
            maintenance.setMaintenanceDate(maintenanceDatePicker.getValue());
            maintenance.setMaintenanceType(maintenanceTypeComboBox.getValue());
            maintenance.setCost(Double.parseDouble(costField.getText().trim()));
            maintenance.setNextServiceDate(nextServiceDatePicker.getValue());
            maintenance.setStatus(statusComboBox.getValue());
            maintenance.setDescription(descriptionField.getText().trim());

            if (maintenanceDAO.updateMaintenance(maintenance)) {
                showAlert(AlertType.INFORMATION, "Success", "Maintenance record updated successfully!");
                loadMaintenanceData();
                handleClearForm();
            } else {
                showAlert(AlertType.ERROR, "Error", "Failed to update record.");
            }
        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Invalid Input", "Cost must be a valid numerical value.");
        }
    }

    @FXML
    private void handleDeleteMaintenance() {
        String mIdText = maintenanceIdField.getText().trim();
        if (mIdText.isEmpty()) {
            showAlert(AlertType.WARNING, "No Selection", "Please select a maintenance record to delete.");
            return;
        }

        try {
            int realId = Integer.parseInt(mIdText.replaceAll("[^0-9]", ""));
            String vId = vehicleIdField.getText().trim();

            // Matches your DAO signature: deleteMaintenance(int, String)
            if (maintenanceDAO.deleteMaintenance(realId, vId)) {
                showAlert(AlertType.INFORMATION, "Success", "Maintenance record deleted successfully!");
                loadMaintenanceData();
                handleClearForm();
            } else {
                showAlert(AlertType.ERROR, "Error", "Failed to delete record.");
            }
        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Error", "Invalid ID format.");
        }
    }

    @FXML
    private void handleClearForm() {
        maintenanceIdField.clear();
        vehicleIdField.clear();
        maintenanceDatePicker.setValue(null);
        maintenanceTypeComboBox.setValue(null);
        costField.clear();
        nextServiceDatePicker.setValue(null);
        statusComboBox.setValue(null);
        descriptionField.clear();
        maintenanceTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleClearAllRecords() {
        Alert confirm = new Alert(AlertType.CONFIRMATION, "Delete ALL maintenance records?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                if (maintenanceDAO.clearAllMaintenance()) {
                    showAlert(AlertType.INFORMATION, "Success", "All maintenance records wiped.");
                    loadMaintenanceData();
                    handleClearForm();
                } else {
                    showAlert(AlertType.ERROR, "Error", "Could not clear records.");
                }
            }
        });
    }

    @FXML
    private void handleSearchMaintenance() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String cat1 = filterCategory1ComboBox.getValue();
        String cat2 = filterCategory2ComboBox.getValue();

        filteredList.setPredicate(m -> {
            boolean matchesCat1 = true;
            if ("Vehicle ID".equals(cat1) && !keyword.isEmpty()) {
                matchesCat1 = m.getVehicleId() != null && m.getVehicleId().toLowerCase().contains(keyword);
            } else if ("Status".equals(cat1) && !keyword.isEmpty()) {
                matchesCat1 = m.getStatus() != null && m.getStatus().toLowerCase().contains(keyword);
            }

            boolean matchesCat2 = true;
            if (!keyword.isEmpty()) {
                switch (cat2) {
                    case "Maintenance ID":
                        matchesCat2 = m.getFormattedId().toLowerCase().contains(keyword);
                        break;
                    case "Maintenance Type":
                        matchesCat2 = m.getMaintenanceType() != null && m.getMaintenanceType().toLowerCase().contains(keyword);
                        break;
                    case "Description":
                        matchesCat2 = m.getDescription() != null && m.getDescription().toLowerCase().contains(keyword);
                        break;
                    case "All":
                    default:
                        matchesCat2 = m.getFormattedId().toLowerCase().contains(keyword) ||
                                (m.getVehicleId() != null && m.getVehicleId().toLowerCase().contains(keyword)) ||
                                (m.getMaintenanceType() != null && m.getMaintenanceType().toLowerCase().contains(keyword)) ||
                                (m.getDescription() != null && m.getDescription().toLowerCase().contains(keyword));
                        break;
                }
            }
            return matchesCat1 && matchesCat2;
        });
    }

    @FXML
    private void handleResetSearch() {
        searchField.clear();
        filterCategory1ComboBox.setValue("All Vehicles");
        filterCategory2ComboBox.setValue("All");
        filteredList.setPredicate(m -> true);
    }

    private boolean validateForm() {
        return !vehicleIdField.getText().isBlank() &&
                maintenanceDatePicker.getValue() != null &&
                maintenanceTypeComboBox.getValue() != null &&
                !costField.getText().isBlank() &&
                nextServiceDatePicker.getValue() != null &&
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
    @FXML private void switchToOrderProof() { loadScene("/orderproof_view.fxml", "Order Proof"); }
    @FXML private void switchToOrderRequests() { loadScene("/orderrequest_view.fxml", "Order Requests"); }
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