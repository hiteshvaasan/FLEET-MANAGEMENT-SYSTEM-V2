package com.fleetmanagement.controllers;

import com.fleetmanagement.dao.ReportDAO;
import com.fleetmanagement.model.ReportRow;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class ReportController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Button btnHamburger;

    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private ComboBox<String> vehicleFilterComboBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<String> statusFilterComboBox;

    @FXML private Button btnGenerateReport;
    @FXML private Button btnClearFilters;
    @FXML private Button btnDownloadReport;

    @FXML private Label sectionHeaderLabel;
    @FXML private TextField searchField;
    @FXML private Label totalRecordsLabel;

    @FXML private TableView<ReportRow> reportTable;

    private final ReportDAO reportDAO = new ReportDAO();
    private final ObservableList<ReportRow> reportDataList = FXCollections.observableArrayList();
    private FilteredList<ReportRow> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ThemeManager.isDarkMode() && rootPane != null) {
            rootPane.setStyle("-fx-background-color: #0f172a;");
        }

        reportTypeComboBox.setItems(FXCollections.observableArrayList(
                "Trip Report", "Vehicle Report", "Driver Report", "Maintenance Report",
                "Fuel Report", "Toll Report", "Expense Report", "Trip Cost Report"
        ));
        reportTypeComboBox.setValue("Trip Report");

        vehicleFilterComboBox.setItems(FXCollections.observableArrayList("All Vehicles"));
        vehicleFilterComboBox.setValue("All Vehicles");

        statusFilterComboBox.setItems(FXCollections.observableArrayList("All Status", "Completed", "In Progress", "Scheduled", "Available", "Under Maintenance"));
        statusFilterComboBox.setValue("All Status");

        loadVehicleFilterOptions();

        filteredList = new FilteredList<>(reportDataList, r -> true);
        reportTable.setItems(filteredList);

        generateReportData();
        setupSearchFilter();
    }

    private void loadVehicleFilterOptions() {
        List<String> vehicleIds = reportDAO.getAllVehicleIds();
        ObservableList<String> options = FXCollections.observableArrayList(vehicleIds);
        vehicleFilterComboBox.setItems(options);
    }

    @FXML
    private void handleGenerateReport() {
        generateReportData();
    }

    private void generateReportData() {
        String reportType = reportTypeComboBox.getValue();
        String vehicleFilter = vehicleFilterComboBox.getValue();
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        String statusFilter = statusFilterComboBox.getValue();

        sectionHeaderLabel.setText(reportType + " Results");

        // Uses your DAO's exact method signature
        List<ReportRow> data = reportDAO.generateReport(reportType, fromDate, toDate, vehicleFilter, statusFilter);
        reportDataList.setAll(data);
        setupDynamicTableColumns(reportType);

        totalRecordsLabel.setText("Total Records: " + reportDataList.size());
    }

    private void setupDynamicTableColumns(String reportType) {
        reportTable.getColumns().clear();

        if ("Trip Report".equals(reportType)) {
            reportTable.getColumns().addAll(
                    createColumn("Trip ID", "col1", 90),
                    createColumn("Vehicle ID", "col2", 90),
                    createColumn("Driver ID", "col3", 90),
                    createColumn("Source", "col4", 120),
                    createColumn("Destination", "col5", 120),
                    createColumn("Date", "col6", 100),
                    createColumn("Distance", "col7", 90),
                    createColumn("Status", "col8", 110)
            );
        } else if ("Vehicle Report".equals(reportType)) {
            reportTable.getColumns().addAll(
                    createColumn("Vehicle ID", "col1", 100),
                    createColumn("Registration No", "col2", 140),
                    createColumn("Vehicle Type", "col3", 110),
                    createColumn("Model", "col4", 130),
                    createColumn("Capacity (kg)", "col5", 110),
                    createColumn("Fuel Type", "col6", 110),
                    createColumn("Status", "col7", 130)
            );
        } else if ("Driver Report".equals(reportType)) {
            reportTable.getColumns().addAll(
                    createColumn("Driver ID", "col1", 100),
                    createColumn("Driver Name", "col2", 150),
                    createColumn("Phone", "col3", 130),
                    createColumn("License Number", "col4", 150),
                    createColumn("License Expiry", "col5", 130),
                    createColumn("Status", "col6", 130)
            );
        } else if ("Maintenance Report".equals(reportType)) {
            reportTable.getColumns().addAll(
                    createColumn("Maintenance ID", "col1", 110),
                    createColumn("Vehicle ID", "col2", 100),
                    createColumn("Date", "col3", 110),
                    createColumn("Type", "col4", 120),
                    createColumn("Description", "col5", 150),
                    createColumn("Cost (₹)", "col6", 100),
                    createColumn("Next Service", "col7", 110),
                    createColumn("Status", "col8", 110)
            );
        } else if ("Trip Cost Report".equals(reportType)) {
            reportTable.getColumns().addAll(
                    createColumn("Trip ID", "col1", 100),
                    createColumn("Vehicle ID", "col2", 100),
                    createColumn("Total Fuel (₹)", "col3", 120),
                    createColumn("Total Toll (₹)", "col4", 120),
                    createColumn("Total Exp (₹)", "col5", 130),
                    createColumn("Grand Total (₹)", "col6", 140)
            );
        } else {
            // Default generic fallback columns for Fuel, Toll, or Expense Reports
            reportTable.getColumns().addAll(
                    createColumn("Column 1", "col1", 110),
                    createColumn("Column 2", "col2", 110),
                    createColumn("Column 3", "col3", 110),
                    createColumn("Column 4", "col4", 110),
                    createColumn("Column 5", "col5", 110),
                    createColumn("Column 6", "col6", 110)
            );
        }
    }

    private TableColumn<ReportRow, String> createColumn(String title, String propertyName, double width) {
        TableColumn<ReportRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        col.setPrefWidth(width);
        return col;
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(row -> {
                if (newVal == null || newVal.isBlank()) return true;
                String lower = newVal.toLowerCase();
                return (row.getCol1() != null && row.getCol1().toLowerCase().contains(lower)) ||
                        (row.getCol2() != null && row.getCol2().toLowerCase().contains(lower)) ||
                        (row.getCol3() != null && row.getCol3().toLowerCase().contains(lower)) ||
                        (row.getCol4() != null && row.getCol4().toLowerCase().contains(lower)) ||
                        (row.getCol5() != null && row.getCol5().toLowerCase().contains(lower)) ||
                        (row.getCol6() != null && row.getCol6().toLowerCase().contains(lower)) ||
                        (row.getCol7() != null && row.getCol7().toLowerCase().contains(lower)) ||
                        (row.getCol8() != null && row.getCol8().toLowerCase().contains(lower));
            });
            totalRecordsLabel.setText("Total Records: " + filteredList.size());
        });
    }

    @FXML
    private void handleClearFilters() {
        reportTypeComboBox.setValue("Trip Report");
        vehicleFilterComboBox.setValue("All Vehicles");
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        statusFilterComboBox.setValue("All Status");
        searchField.clear();
        generateReportData();
    }

    @FXML
    private void handleDownloadReport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report as CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("Fleet_Report_" + LocalDate.now() + ".csv");

        File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                StringBuilder header = new StringBuilder();
                for (TableColumn<ReportRow, ?> col : reportTable.getColumns()) {
                    header.append("\"").append(col.getText()).append("\",");
                }
                writer.println(header.substring(0, header.length() - 1));

                for (ReportRow row : filteredList) {
                    writer.println(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                            row.getCol1() != null ? row.getCol1() : "",
                            row.getCol2() != null ? row.getCol2() : "",
                            row.getCol3() != null ? row.getCol3() : "",
                            row.getCol4() != null ? row.getCol4() : "",
                            row.getCol5() != null ? row.getCol5() : "",
                            row.getCol6() != null ? row.getCol6() : "",
                            row.getCol7() != null ? row.getCol7() : "",
                            row.getCol8() != null ? row.getCol8() : ""
                    ));
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "Report exported successfully to CSV!");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed", "Could not save file: " + e.getMessage());
            }
        }
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