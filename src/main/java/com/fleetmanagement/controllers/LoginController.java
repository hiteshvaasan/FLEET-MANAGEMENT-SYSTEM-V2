package com.fleetmanagement.controllers;

import com.fleetmanagement.dao.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private CheckBox showPasswordCheckBox;

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bind password fields for smooth live toggling
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
    }

    @FXML
    private void handleShowPasswordToggle() {
        boolean show = showPasswordCheckBox.isSelected();
        visiblePasswordField.setVisible(show);
        visiblePasswordField.setManaged(show);
        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = showPasswordCheckBox.isSelected() ? visiblePasswordField.getText().trim() : passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter both username and password.");
            return;
        }

        if (userDAO.verifyPassword(username, password)) {
            try {
                URL fileUrl = getClass().getResource("/dashboard_view.fxml");
                if (fileUrl == null) return;

                Stage stage = (Stage) usernameField.getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(fileUrl);
                Parent root = loader.load();

                stage.setScene(new Scene(root, 1050, 680));
                stage.setTitle("Fleet Management System V2 - Dashboard");
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to load the dashboard view.");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Authentication Failed", "Invalid username or password.");
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

