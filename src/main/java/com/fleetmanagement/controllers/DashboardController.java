package com.fleetmanagement.controllers;

import com.fleetmanagement.dao.DashboardDAO;
import com.fleetmanagement.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Button btnHamburger;

    @FXML private Label lblTotalVehicles, lblAvailableVehicles, lblTotalDrivers, lblActiveTrips, lblCompletedTrips, lblMaintenanceVehicles;

    @FXML private TableView<DashboardDAO.RecentTrip> tableRecentTrips;
    @FXML private TableColumn<DashboardDAO.RecentTrip, String> colTripId, colVehicle, colDriver, colSource, colDestination, colDate, colStatus;
    @FXML private TableColumn<DashboardDAO.RecentTrip, Double> colDistance;


    @FXML private TableView<IncidentAlert> tableActiveAlerts;
    @FXML private TableColumn<IncidentAlert, String> colAlertTripId;
    @FXML private TableColumn<IncidentAlert, String> colAlertType;
    @FXML private TableColumn<IncidentAlert, String> colAlertTime;

    @FXML private javafx.scene.chart.PieChart vehiclePieChart;

    @FXML private Label lblFuelCost;
    @FXML private Label lblTollCost;
    @FXML private Label lblTotalExpense;



    private final DashboardDAO dashboardDAO = new DashboardDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ThemeManager.isDarkMode() && rootPane != null) {
            rootPane.setStyle("-fx-background-color: #0f172a;");
        }

        colTripId.setCellValueFactory(new PropertyValueFactory<>("tripId"));
        colVehicle.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));
        colDriver.setCellValueFactory(new PropertyValueFactory<>("driverId"));
        colSource.setCellValueFactory(new PropertyValueFactory<>("source"));
        colDestination.setCellValueFactory(new PropertyValueFactory<>("destination"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("tripDate"));
        colDistance.setCellValueFactory(new PropertyValueFactory<>("distance"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));


        colAlertTripId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("tripId"));
        colAlertType.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("type"));
        colAlertTime.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));

        colStatus.setCellFactory(column -> new TableCell<DashboardDAO.RecentTrip, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    String baseStyle = "-fx-padding: 4px 10px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;";

                    if (item.equalsIgnoreCase("Completed") || item.equalsIgnoreCase("Done")) {
                        badge.setStyle(baseStyle + "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a;");
                    } else if (item.equalsIgnoreCase("In Progress") || item.equalsIgnoreCase("Active")) {
                        badge.setStyle(baseStyle + "-fx-background-color: #fef9c3; -fx-text-fill: #ca8a04;");
                    } else if (item.equalsIgnoreCase("Cancelled") || item.equalsIgnoreCase("Failed")) {
                        badge.setStyle(baseStyle + "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
                    } else {
                        badge.setStyle(baseStyle + "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b;");
                    }

                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });

        loadDashboardData();
        loadActiveAlerts();
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

    @FXML
    private void handleRefresh() {
        loadDashboardData();
        showAlert(Alert.AlertType.INFORMATION, "Refreshed", "Dashboard data updated successfully.");
        loadActiveAlerts();
    }

    private void loadDashboardData() {
        lblTotalVehicles.setText(String.valueOf(dashboardDAO.getTotalVehicles()));
        lblAvailableVehicles.setText(String.valueOf(dashboardDAO.getAvailableVehicles()));
        lblTotalDrivers.setText(String.valueOf(dashboardDAO.getTotalDrivers()));
        lblActiveTrips.setText(String.valueOf(dashboardDAO.getActiveTrips()));
        lblCompletedTrips.setText(String.valueOf(dashboardDAO.getCompletedTrips()));
        lblMaintenanceVehicles.setText(String.valueOf(dashboardDAO.getMaintenanceVehicles()));

        tableRecentTrips.setItems(dashboardDAO.getRecentTrips());

        if (vehiclePieChart != null) {
            vehiclePieChart.setData(dashboardDAO.getVehicleStatusDistribution());
        }

        double fuel = dashboardDAO.getMonthlyFuelCost();
        double toll = dashboardDAO.getMonthlyTollCost();

        if (lblFuelCost != null) lblFuelCost.setText(String.format("₹ %,.2f", fuel));
        if (lblTollCost != null) lblTollCost.setText(String.format("₹ %,.2f", toll));
        if (lblTotalExpense != null) lblTotalExpense.setText(String.format("₹ %,.2f", fuel + toll));
    }

    // --- FILE MENU ACTIONS ---
    @FXML
    private void handleFileNewTrip() {
        loadScene("/trip_view.fxml", "Trip Management", 1050, 680);
    }

    @FXML
    private void handleFileRefresh() {
        handleRefresh();
    }

    @FXML
    private void handleFileLogout() {
        handleLogout();
    }

    @FXML
    private void handleFileExit() {
        handleExit();
    }

    // --- HELP MENU ACTIONS ---
    @FXML
    private void handleHelpAbout() {
        showAbout();
    }

    @FXML
    private void showUserGuide() {
        showAlert(Alert.AlertType.INFORMATION, "User Guide", "1. Use the sidebar or top menu to navigate.\n2. Dashboard displays real-time KPI metrics.\n3. Click 'Refresh' to sync with the latest database records.");
    }

    @FXML
    private void showAbout() {
        showAlert(Alert.AlertType.INFORMATION, "About", "Fleet Management System V2\nVersion: 2.0\nDeveloped with JavaFX and MySQL.");
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to logout?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Logout");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                loadScene("/login_view.fxml", "Login", 450, 400);
            }
        });
    }

    @FXML
    private void handleExit() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to exit?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Exit Application");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                System.exit(0);
            }
        });
    }

    @FXML private void switchToDashboard() { loadScene("/dashboard_view.fxml", "Dashboard", 1050, 680); }
    @FXML private void switchToVehicles() { loadScene("/vehicle_view.fxml", "Vehicle Management", 1050, 680); }
    @FXML private void switchToDrivers() { loadScene("/driver_view.fxml", "Driver Management", 1050, 680); }
    @FXML private void switchToTrips() { loadScene("/trip_view.fxml", "Trip Management", 1050, 680); }
    @FXML private void switchToMaintenance() { loadScene("/maintenance_view.fxml", "Maintenance Management", 1050, 680); }
    @FXML private void switchToReports() { loadScene("/report_view.fxml", "Reports", 1050, 680); }
    @FXML private void switchToSettings() { loadScene("/settings_view.fxml", "Settings", 1050, 680); }
    @FXML private void switchToIncidents() { loadScene("/incident_view.fxml", "Incident Management",1050, 680); }
    @FXML private void switchToOrderProof() { loadScene("/orderproof_view.fxml", "Order Proof",1050, 680); }

    @FXML private void switchToOrderRequests() { loadScene("/orderrequest_view.fxml", "Order Requests",1050, 680); }
    @FXML
    private void switchToReceivers() {
        loadScene("/receiver_view.fxml", "Receiver Management",1050, 680);
    }
    private void loadScene(String fxmlPath, String title, double width, double height) {
        try {
            URL fileUrl = getClass().getResource(fxmlPath);
            if (fileUrl == null) return;
            Stage stage = (Stage) rootPane.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(fileUrl);
            Parent root = loader.load();

            if (ThemeManager.isDarkMode() && root instanceof BorderPane) {
                ((BorderPane) root).setStyle("-fx-background-color: #0f172a;");
            }

            // ✅ FIX: Swap the root of the current scene instead of creating a new one
            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new Scene(root, width, height));
            }

            stage.setTitle("Fleet Management System V2 - " + title);

            // Removed stage.show() to prevent the OS window from flashing
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

    public static class IncidentAlert {
        private final javafx.beans.property.SimpleStringProperty tripId;
        private final javafx.beans.property.SimpleStringProperty type;
        private final javafx.beans.property.SimpleStringProperty time;

        public IncidentAlert(String tripId, String type, String time) {
            this.tripId = new javafx.beans.property.SimpleStringProperty(tripId);
            this.type = new javafx.beans.property.SimpleStringProperty(type);
            this.time = new javafx.beans.property.SimpleStringProperty(time);
        }
        public String getTripId() { return tripId.get(); }
        public String getType() { return type.get(); }
        public String getTime() { return time.get(); }
    }

    private void loadActiveAlerts() {
        javafx.collections.ObservableList<IncidentAlert> activeAlertsList = javafx.collections.FXCollections.observableArrayList();
        String alertQuery = "SELECT trip_id, incident_type, TIME(reported_at) as alert_time FROM incidents WHERE status = 'Open' ORDER BY reported_at DESC";

        try (java.sql.Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(alertQuery);
             java.sql.ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                activeAlertsList.add(new IncidentAlert(
                        rs.getString("trip_id"),
                        rs.getString("incident_type"),
                        rs.getString("alert_time")
                ));
            }
            tableActiveAlerts.setItems(activeAlertsList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
