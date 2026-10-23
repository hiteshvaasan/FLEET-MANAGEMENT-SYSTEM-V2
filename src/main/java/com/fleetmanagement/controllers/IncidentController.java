package com.fleetmanagement.controllers;

import com.fleetmanagement.db.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.ResourceBundle;

public class IncidentController implements Initializable {

    // ===== NAVIGATION & LAYOUT FIELDS =====
    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Button btnHamburger;

    // ===== INCIDENT MODULE FIELDS =====
    @FXML private TableView<IncidentModel> incidentTable;
    @FXML private TableColumn<IncidentModel, String> colTrip, colDriver, colType, colDate, colStatus;
    @FXML private TextArea descTextArea;
    @FXML private Button btnViewPhoto, btnResolve;

    private final ObservableList<IncidentModel> incidentList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colTrip.setCellValueFactory(new PropertyValueFactory<>("tripId"));
        colDriver.setCellValueFactory(new PropertyValueFactory<>("driverId"));
        colType.setCellValueFactory(new PropertyValueFactory<>("incidentType"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("reportedAt"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Listen for row selection to show the description
        incidentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                descTextArea.setText(newSel.getDescription());
                btnViewPhoto.setDisable(newSel.getPhotoData() == null || newSel.getPhotoData().isEmpty());
                btnResolve.setDisable(newSel.getStatus().equals("Resolved"));
            } else {
                descTextArea.clear();
                btnViewPhoto.setDisable(true);
                btnResolve.setDisable(true);
            }
        });

        loadIncidents();
    }

    @FXML
    private void loadIncidents() {
        incidentList.clear();
        String query = "SELECT * FROM incidents ORDER BY reported_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                incidentList.add(new IncidentModel(
                        rs.getInt("incident_id"), rs.getString("trip_id"), rs.getString("driver_id"),
                        rs.getString("incident_type"), rs.getString("description"),
                        rs.getString("status"), rs.getString("reported_at"), rs.getString("photo_data")
                ));
            }
            incidentTable.setItems(incidentList);
        } catch (Exception e) { e.printStackTrace(); }
    }
    @FXML
    private void handleDeleteIncident() {
        IncidentModel selected = incidentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select an incident row from the table to delete.");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this incident record?");
        confirm.setTitle("Confirm Deletion");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String query = "DELETE FROM incidents WHERE incident_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, selected.getId());
                pstmt.executeUpdate();
                loadIncidents();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleDeleteAllIncidents() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete ALL incident records from the database? This cannot be undone.");
        confirm.setTitle("Confirm Delete All");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String query = "DELETE FROM incidents";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.executeUpdate();
                loadIncidents();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @FXML
    private void handleViewPhoto() {
        IncidentModel selected = incidentTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getPhotoData() == null || selected.getPhotoData().isEmpty()) return;

        try {
            // 1. Clean the Base64 text (remove the HTML prefix if it exists)
            String base64Data = selected.getPhotoData();
            if (base64Data.contains(",")) { base64Data = base64Data.split(",")[1]; }

            // 2. Decode text back into image bytes
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            Image img = new Image(new ByteArrayInputStream(imageBytes));

            // 3. Create a floating window to display it
            ImageView imgView = new ImageView(img);
            imgView.setFitWidth(600); imgView.setPreserveRatio(true);

            StackPane pane = new StackPane(imgView);
            pane.setStyle("-fx-background-color: #0f172a; -fx-padding: 20;");

            Stage stage = new Stage();
            stage.setTitle("Incident Photo Proof");
            stage.setScene(new Scene(pane, 640, 480));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleResolveIncident() {
        IncidentModel selected = incidentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String query = "UPDATE incidents SET status = 'Resolved' WHERE incident_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, selected.getId());
            pstmt.executeUpdate();
            loadIncidents();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ================= SIDEBAR & NAVIGATION =================

    @FXML
    private void toggleSidebar() {
        boolean isVisible = sidebar.isVisible();

        // Toggle visibility and managed state so it collapses/expands cleanly
        sidebar.setVisible(!isVisible);
        sidebar.setManaged(!isVisible);

        // Keep the top header hamburger button visible so you can always click it back open
        if (btnHamburger != null) {
            btnHamburger.setVisible(true);
            btnHamburger.setManaged(true);
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
            stage.setScene(new Scene(root, 1050, 680));
            stage.setTitle("Fleet Management System V2 - " + title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Embedded Model Class ---
    public static class IncidentModel {
        private int id;
        private String tripId, driverId, incidentType, description, status, reportedAt, photoData;

        public IncidentModel(int id, String tripId, String driverId, String incidentType, String description, String status, String reportedAt, String photoData) {
            this.id = id; this.tripId = tripId; this.driverId = driverId; this.incidentType = incidentType;
            this.description = description; this.status = status; this.reportedAt = reportedAt; this.photoData = photoData;
        }
        public int getId() { return id; }
        public String getTripId() { return tripId; }
        public String getDriverId() { return driverId; }
        public String getIncidentType() { return incidentType; }
        public String getDescription() { return description; }
        public String getStatus() { return status; }
        public String getReportedAt() { return reportedAt; }
        public String getPhotoData() { return photoData; }
    }
}