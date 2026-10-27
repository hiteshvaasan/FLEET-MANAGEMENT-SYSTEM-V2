package com.fleetmanagement.controllers;

import com.fleetmanagement.dao.ReceiverDAO;
import com.fleetmanagement.model.Receiver;
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

public class ReceiverController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Button btnHamburger;

    @FXML private TextField receiverIdField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField passwordField;
    @FXML private TextField phoneField;
    @FXML private TextField companyNameField;

    @FXML private Button btnAdd, btnSearch, btnUpdate, btnDelete, btnClear, btnClearAll;

    @FXML private ComboBox<String> filterCategoryComboBox;
    @FXML private TextField searchField;

    @FXML private TableView<Receiver> receiverTable;
    @FXML private TableColumn<Receiver, String> colReceiverId;
    @FXML private TableColumn<Receiver, String> colName;
    @FXML private TableColumn<Receiver, String> colEmail;
    @FXML private TableColumn<Receiver, String> colPhone;
    @FXML private TableColumn<Receiver, String> colCompanyName;
    @FXML private TableColumn<Receiver, String> colPassword;

    private final ReceiverDAO receiverDAO = new ReceiverDAO();
    private final ObservableList<Receiver> receiverList = FXCollections.observableArrayList();
    private FilteredList<Receiver> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ThemeManager.isDarkMode() && rootPane != null) {
            rootPane.setStyle("-fx-background-color: #0f172a;");
        }

        filterCategoryComboBox.setItems(FXCollections.observableArrayList("All Fields", "Receiver ID", "Name", "Company Name", "Phone"));
        filterCategoryComboBox.setValue("All Fields");

        colReceiverId.setCellValueFactory(new PropertyValueFactory<>("receiverId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colCompanyName.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        colPassword.setCellValueFactory(new PropertyValueFactory<>("password"));

        filteredList = new FilteredList<>(receiverList, r -> true);
        receiverTable.setItems(filteredList);

        loadReceiverData();
        setupRowSelectionListener();
        autoGenerateIdAndDefaultPassword();
    }

    private void autoGenerateIdAndDefaultPassword() {
        String nextId = receiverDAO.generateNextReceiverId();
        receiverIdField.setText(nextId);
        receiverIdField.setEditable(false);
        passwordField.setText(nextId + "@1228"); // Automatically sets R001@1228 format
    }

    private void loadReceiverData() {
        List<Receiver> receivers = receiverDAO.getAllReceivers();
        receiverList.setAll(receivers);
    }

    private void setupRowSelectionListener() {
        receiverTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                receiverIdField.setText(newSel.getReceiverId());
                nameField.setText(newSel.getName());
                emailField.setText(newSel.getEmail());
                passwordField.setText(newSel.getPassword());
                phoneField.setText(newSel.getPhone());
                companyNameField.setText(newSel.getCompanyName());
                receiverIdField.setEditable(false);
            }
        });
    }

    @FXML
    private void handleAddReceiver() {
        if (!validateForm()) return;

        Receiver receiver = new Receiver(
                receiverIdField.getText().trim(),
                nameField.getText().trim(),
                emailField.getText().trim(),
                passwordField.getText().trim(),
                phoneField.getText().trim(),
                companyNameField.getText().trim()
        );

        if (receiverDAO.addReceiver(receiver)) {
            showAlert(AlertType.INFORMATION, "Success", "Receiver added successfully!");
            loadReceiverData();
            handleClearForm();
        } else {
            showAlert(AlertType.ERROR, "Error", "Failed to add receiver. Check if email or ID already exists.");
        }
    }

    @FXML
    private void handleUpdateReceiver() {
        String receiverId = receiverIdField.getText().trim();
        if (receiverId.isEmpty()) {
            showAlert(AlertType.WARNING, "No Selection", "Please select a receiver to update.");
            return;
        }

        Receiver receiver = new Receiver(
                receiverId,
                nameField.getText().trim(),
                emailField.getText().trim(),
                passwordField.getText().trim(),
                phoneField.getText().trim(),
                companyNameField.getText().trim()
        );

        if (receiverDAO.updateReceiver(receiver)) {
            showAlert(AlertType.INFORMATION, "Success", "Receiver updated successfully!");
            loadReceiverData();
            handleClearForm();
        } else {
            showAlert(AlertType.ERROR, "Error", "Update failed.");
        }
    }

    @FXML
    private void handleDeleteReceiver() {
        String receiverId = receiverIdField.getText().trim();
        if (receiverId.isEmpty()) {
            showAlert(AlertType.WARNING, "No Selection", "Please select a receiver to delete.");
            return;
        }
        if (receiverDAO.deleteReceiver(receiverId)) {
            showAlert(AlertType.INFORMATION, "Success", "Receiver deleted successfully!");
            loadReceiverData();
            handleClearForm();
        } else {
            showAlert(AlertType.ERROR, "Error", "Delete failed. Receiver may be linked to active trips.");
        }
    }

    @FXML
    private void handleSearchReceiver() {
        String searchText = searchField.getText();
        String category = filterCategoryComboBox.getValue();

        filteredList.setPredicate(receiver -> {
            if (searchText == null || searchText.isBlank()) return true;
            String lower = searchText.toLowerCase();
            String cat = category == null ? "All Fields" : category;

            switch (cat) {
                case "Receiver ID":
                    return receiver.getReceiverId() != null && receiver.getReceiverId().toLowerCase().contains(lower);
                case "Name":
                    return receiver.getName() != null && receiver.getName().toLowerCase().contains(lower);
                case "Company Name":
                    return receiver.getCompanyName() != null && receiver.getCompanyName().toLowerCase().contains(lower);
                case "Phone":
                    return receiver.getPhone() != null && receiver.getPhone().toLowerCase().contains(lower);
                case "All Fields":
                default:
                    return (receiver.getReceiverId() != null && receiver.getReceiverId().toLowerCase().contains(lower)) ||
                            (receiver.getName() != null && receiver.getName().toLowerCase().contains(lower)) ||
                            (receiver.getEmail() != null && receiver.getEmail().toLowerCase().contains(lower)) ||
                            (receiver.getPhone() != null && receiver.getPhone().toLowerCase().contains(lower)) ||
                            (receiver.getCompanyName() != null && receiver.getCompanyName().toLowerCase().contains(lower));
            }
        });
    }

    @FXML
    private void handleResetSearchAction() {
        searchField.clear();
        filterCategoryComboBox.setValue("All Fields");
        filteredList.setPredicate(r -> true);
    }

    @FXML
    private void handleClearForm() {
        autoGenerateIdAndDefaultPassword();
        nameField.clear();
        emailField.clear();
        phoneField.clear();
        companyNameField.clear();
        receiverTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleClearAllRecords() {
        if (receiverDAO.deleteAllReceivers()) {
            showAlert(AlertType.INFORMATION, "Success", "All receiver records wiped.");
            loadReceiverData();
            handleClearForm();
        } else {
            showAlert(AlertType.ERROR, "Error", "Could not clear records.");
        }
    }

    private boolean validateForm() {
        return !receiverIdField.getText().isBlank() &&
                !nameField.getText().isBlank() &&
                !emailField.getText().isBlank() &&
                !passwordField.getText().isBlank() &&
                !phoneField.getText().isBlank() &&
                !companyNameField.getText().isBlank();
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
    @FXML private void switchToReceivers() { loadScene("/receiver_view.fxml", "Receiver Management"); }
    @FXML private void switchToTrips() { loadScene("/trip_view.fxml", "Trip Management"); }
    @FXML private void switchToMaintenance() { loadScene("/maintenance_view.fxml", "Maintenance Management"); }
    @FXML private void switchToReports() { loadScene("/report_view.fxml", "Reports"); }
    @FXML private void switchToSettings() { loadScene("/settings_view.fxml", "Settings"); }
    @FXML private void switchToOrderProof() { loadScene("/orderproof_view.fxml", "Order Proof"); }
    @FXML private void switchToOrderRequests() { loadScene("/orderrequest_view.fxml", "Order Requests"); }
    @FXML private void switchToIncidents() { loadScene("/incident_view.fxml", "Incident Management"); }

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