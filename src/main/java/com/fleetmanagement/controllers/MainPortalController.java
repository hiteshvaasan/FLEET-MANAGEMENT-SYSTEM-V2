package com.fleetmanagement.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.net.URL;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import java.net.InetAddress;

public class MainPortalController {

    @FXML private BorderPane rootPane;

    @FXML
    private void handleAdminLoginClick() {
        loadScene("/login_view.fxml", "Admin Login");
    }

    @FXML
    public void handleDriverLoginClick() {
        // Create a sleek popup alert
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Mobile Web Portal");
        alert.setHeaderText("📱 Driver Portal is Live");

        // Create custom content for the popup
        VBox content = new VBox(10);
        Label instruction = new Label("To access the Driver Portal, scan the QR code or open your mobile browser and go to:");
        Label url = new Label("http://localhost:8081");
        url.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb; -fx-font-size: 16px;");

        content.getChildren().addAll(instruction, url);
        alert.getDialogPane().setContent(content);

        // Show the popup
        alert.showAndWait();
    }
    @FXML
    private void handleReceiverTrackingClick() {
        // We will create receiver_tracking.fxml next!
        loadScene("/receiver_tracking.fxml", "Track Shipment");
    }
    @FXML
    private void showDriverQrCodePopup() {
        try {
            // 1. Automatically get your laptop's local Wi-Fi IP address
            String localIp = InetAddress.getLocalHost().getHostAddress();
            String mobileUrl = "http://" + localIp + ":8081";

            // 2. Create the QR code image URL from an API generator
            // This generates a 220x220 QR code containing your mobile web link
            String qrApiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=" + mobileUrl;

            // 3. Load the image into JavaFX
            Image qrImage = new Image(qrApiUrl, true); // true = load in background so it doesn't freeze UI
            ImageView imageView = new ImageView(qrImage);
            imageView.setFitWidth(200);
            imageView.setFitHeight(200);
            imageView.setPreserveRatio(true);

            // 4. Build a sleek popup dialog UI
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Driver Mobile Portal");
            alert.setHeaderText("📱 Scan to Open Driver Portal on Phone");

            VBox content = new VBox(12);
            content.setAlignment(javafx.geometry.Pos.CENTER);

            Label instruction = new Label("Point your smartphone camera at this QR code:");
            instruction.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

            Label urlLabel = new Label(mobileUrl);
            urlLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb; -fx-font-size: 14px;");

            content.getChildren().addAll(instruction, imageView, urlLabel);
            alert.getDialogPane().setContent(content);

            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(AlertType.ERROR, "Could not generate QR code. Check your network connection.");
            error.show();
        }
    }
    @FXML
    private void showReceiverQrCodePopup() {
        try {
            // 1. Automatically get your laptop's local Wi-Fi IP address
            String localIp = InetAddress.getLocalHost().getHostAddress();

            // CHANGE THIS LINE: Point directly to receiver_login.html instead of receiver.html
            String receiverUrl = "http://" + localIp + ":8081/receiver_login.html";

            // 2. Generate QR code image URL containing the receiver login link
            String qrApiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=" + receiverUrl;

            // ... (keep the rest of your existing dialog code as it is)
            // 3. Load the image into JavaFX
            Image qrImage = new Image(qrApiUrl, true);
            ImageView imageView = new ImageView(qrImage);
            imageView.setFitWidth(200);
            imageView.setFitHeight(200);
            imageView.setPreserveRatio(true);

            // 4. Build the popup dialog UI
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Receiver Web Portal");
            alert.setHeaderText("🎁 Scan to Open Receiver Portal on Phone");

            VBox content = new VBox(12);
            content.setAlignment(javafx.geometry.Pos.CENTER);

            Label instruction = new Label("Scan this QR code to track shipments & view driver details:");
            instruction.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

            Label urlLabel = new Label(receiverUrl);
            urlLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb; -fx-font-size: 14px;");

            content.getChildren().addAll(instruction, imageView, urlLabel);
            alert.getDialogPane().setContent(content);

            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(AlertType.ERROR, "Could not generate QR code. Check your network connection.");
            error.show();
        }
    }




    private void loadScene(String fxmlPath, String title) {
        try {
            URL fileUrl = getClass().getResource(fxmlPath);
            if (fileUrl == null) {
                System.out.println("File not found: " + fxmlPath);
                return;
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(fileUrl);
            Parent root = loader.load();

            // Swap root without flickering!
            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new Scene(root, 1050, 680));
            }

            stage.setTitle("Fleet Management V3 - " + title);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}