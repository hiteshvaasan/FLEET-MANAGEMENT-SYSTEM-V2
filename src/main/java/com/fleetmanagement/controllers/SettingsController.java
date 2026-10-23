package com.fleetmanagement.controllers;

import com.fleetmanagement.utils.ThemeManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Button btnHamburger;

    @FXML private TextField usernameField;
    @FXML private PasswordField currentPasswordField;
    @FXML private TextField currentPasswordTextField; // For show password view
    @FXML private PasswordField newPasswordField;
    @FXML private TextField newPasswordTextField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTextField;
    @FXML private CheckBox showPasswordCheckBox;

    @FXML private ComboBox<String> themeComboBox;
    @FXML private CheckBox notificationsCheckBox;

    @FXML private Button btnSave;
    @FXML private Button btnClear;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ThemeManager.isDarkMode() && rootPane != null) {
            rootPane.setStyle("-fx-background-color: #0f172a;");
        }

        themeComboBox.setItems(FXCollections.observableArrayList("Light", "Dark"));
        themeComboBox.setValue(ThemeManager.isDarkMode() ? "Dark" : "Light");

        // Listen for theme changes dynamically
        themeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean dark = "Dark".equals(newVal);
            ThemeManager.setDarkMode(dark);
            if (rootPane != null) {
                rootPane.setStyle(dark ? "-fx-background-color: #0f172a;" : "-fx-background-color: #f1f5f9;");
            }
        });

        setupShowPasswordBinding();
    }

    private void setupShowPasswordBinding() {
        // Bind text visibility fields with PasswordFields
        currentPasswordTextField.managedProperty().bind(showPasswordCheckBox.selectedProperty());
        currentPasswordTextField.visibleProperty().bind(showPasswordCheckBox.selectedProperty());
        currentPasswordField.managedProperty().bind(showPasswordCheckBox.selectedProperty().not());
        currentPasswordField.visibleProperty().bind(showPasswordCheckBox.selectedProperty().not());

        newPasswordTextField.managedProperty().bind(showPasswordCheckBox.selectedProperty());
        newPasswordTextField.visibleProperty().bind(showPasswordCheckBox.selectedProperty());
        newPasswordField.managedProperty().bind(showPasswordCheckBox.selectedProperty().not());
        newPasswordField.visibleProperty().bind(showPasswordCheckBox.selectedProperty().not());

        confirmPasswordTextField.managedProperty().bind(showPasswordCheckBox.selectedProperty());
        confirmPasswordTextField.visibleProperty().bind(showPasswordCheckBox.selectedProperty());
        confirmPasswordField.managedProperty().bind(showPasswordCheckBox.selectedProperty().not());
        confirmPasswordField.visibleProperty().bind(showPasswordCheckBox.selectedProperty().not());

        // Sync text values back and forth
        showPasswordCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                currentPasswordTextField.setText(currentPasswordField.getText());
                newPasswordTextField.setText(newPasswordField.getText());
                confirmPasswordTextField.setText(confirmPasswordField.getText());
            } else {
                currentPasswordField.setText(currentPasswordTextField.getText());
                newPasswordField.setText(newPasswordTextField.getText());
                confirmPasswordField.setText(confirmPasswordTextField.getText());
            }
        });
    }

    @FXML
    private void handleSaveSettings() {
        String username = usernameField.getText().trim();
        String newPass = showPasswordCheckBox.isSelected() ? newPasswordTextField.getText() : newPasswordField.getText();
        String confirmPass = showPasswordCheckBox.isSelected() ? confirmPasswordTextField.getText() : confirmPasswordField.getText();

        if (!newPass.isEmpty() && !newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Password Mismatch", "New password and confirm password do not match!");
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, "Success", "Settings updated successfully!");
    }

    @FXML
    private void handleClearForm() {
        usernameField.clear();
        currentPasswordField.clear();
        currentPasswordTextField.clear();
        newPasswordField.clear();
        newPasswordTextField.clear();
        confirmPasswordField.clear();
        confirmPasswordTextField.clear();
        showPasswordCheckBox.setSelected(false);
        notificationsCheckBox.setSelected(true);
        themeComboBox.setValue("Light");
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

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}