package com.fleetmanagement.controllers;

import com.fleetmanagement.dao.OrderRequestDAO;
import com.fleetmanagement.model.OrderRequest;
import com.fleetmanagement.utils.ThemeManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import java.util.ResourceBundle;

public class OrderRequestController implements Initializable {

    private static final String PENDING = "Pending Approval";

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Button btnHamburger;

    @FXML private ComboBox<String> filterCategoryComboBox, statusFilterComboBox;
    @FXML private TextField searchField;
    @FXML private Label lblPendingCount;

    @FXML private TableView<OrderRequest> requestTable;
    @FXML private TableColumn<OrderRequest, Integer> colRequestId;
    @FXML private TableColumn<OrderRequest, Double> colWeight;
    @FXML private TableColumn<OrderRequest, String> colReceiver, colCompany, colPickup, colDelivery,
            colPickupDate, colCargo, colStatus;

    @FXML private Label lblReqId, lblRequestedAt, lblReceiver, lblCompany, lblPhone, lblStatus,
            lblPickup, lblDelivery, lblPickupDate, lblCargo, lblWeight, lblReviewedAt, lblCustomerNotes;
    @FXML private TextArea adminRemarksArea;

    private final OrderRequestDAO dao = new OrderRequestDAO();
    private final ObservableList<OrderRequest> masterList = FXCollections.observableArrayList();
    private FilteredList<OrderRequest> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ThemeManager.isDarkMode() && rootPane != null) {
            rootPane.setStyle("-fx-background-color: #0f172a;");
        }

        filterCategoryComboBox.setItems(FXCollections.observableArrayList(
                "All Fields", "Request ID", "Receiver", "Pickup", "Delivery", "Cargo"));
        filterCategoryComboBox.setValue("All Fields");
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                "All Statuses", PENDING, "Approved", "Rejected"));
        statusFilterComboBox.setValue("All Statuses");

        colRequestId.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        colReceiver.setCellValueFactory(new PropertyValueFactory<>("receiverId"));
        colCompany.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        colPickup.setCellValueFactory(new PropertyValueFactory<>("pickupLocation"));
        colDelivery.setCellValueFactory(new PropertyValueFactory<>("deliveryLocation"));
        colPickupDate.setCellValueFactory(new PropertyValueFactory<>("pickupDate"));
        colCargo.setCellValueFactory(new PropertyValueFactory<>("cargoType"));
        colWeight.setCellValueFactory(new PropertyValueFactory<>("weight"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        applyStatusBadge(colStatus);

        filteredList = new FilteredList<>(masterList, r -> true);
        SortedList<OrderRequest> sorted = new SortedList<>(filteredList);
        sorted.comparatorProperty().bind(requestTable.comparatorProperty());
        requestTable.setItems(sorted);

        requestTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> showDetails(newSel));

        searchField.setOnAction(e -> applyFilters());
        statusFilterComboBox.setOnAction(e -> applyFilters());

        clearDetails();
        loadData();
    }

    // ---------------------------------------------------------------- DATA

    private void loadData() {
        OrderRequest sel = requestTable.getSelectionModel().getSelectedItem();
        int selectedId = sel != null ? sel.getRequestId() : -1;

        masterList.setAll(dao.getAllRequests());

        long pending = masterList.stream().filter(r -> PENDING.equalsIgnoreCase(r.getStatus())).count();
        lblPendingCount.setText(pending + " pending");

        if (selectedId != -1) {
            for (OrderRequest r : requestTable.getItems()) {
                if (r.getRequestId() == selectedId) {
                    requestTable.getSelectionModel().select(r);
                    return;
                }
            }
        }
        clearDetails();
    }

    // ---------------------------------------------------------------- DETAILS

    private void showDetails(OrderRequest r) {
        if (r == null) { clearDetails(); return; }
        lblReqId.setText(String.valueOf(r.getRequestId()));
        lblRequestedAt.setText(n(r.getRequestedAt()));
        lblReceiver.setText(n(r.getReceiverName()) + "  (" + n(r.getReceiverId()) + ")");
        lblCompany.setText(n(r.getCompanyName()));
        lblPhone.setText(n(r.getPhone()));
        lblStatus.setText(n(r.getStatus()));
        lblPickup.setText(n(r.getPickupLocation()));
        lblDelivery.setText(n(r.getDeliveryLocation()));
        lblPickupDate.setText(n(r.getPickupDate()));
        lblCargo.setText(n(r.getCargoType()));
        lblWeight.setText(r.getWeight() + " kg");
        lblReviewedAt.setText(n(r.getReviewedAt()));
        lblCustomerNotes.setText(n(r.getRemarks()));
        adminRemarksArea.setText(r.getAdminRemarks() != null ? r.getAdminRemarks() : "");
    }

    private void clearDetails() {
        for (Label l : new Label[]{lblReqId, lblRequestedAt, lblReceiver, lblCompany, lblPhone, lblStatus,
                lblPickup, lblDelivery, lblPickupDate, lblCargo, lblWeight, lblReviewedAt, lblCustomerNotes}) {
            l.setText("-");
        }
        adminRemarksArea.clear();
    }

    private void applyStatusBadge(TableColumn<OrderRequest, String> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = item.equals("Approved") ? "#16a34a"
                        : item.equals("Rejected") ? "#dc2626" : "#b45309";
                setStyle("-fx-text-fill:" + color + "; -fx-font-weight: bold;");
            }
        });
    }

    // ---------------------------------------------------------------- DECISIONS

    @FXML
    private void handleApprove() {
        OrderRequest r = requestTable.getSelectionModel().getSelectedItem();
        if (r == null) { showAlert(AlertType.WARNING, "No Selection", "Please select a request first."); return; }
        if (!PENDING.equalsIgnoreCase(r.getStatus()) && !confirm("Already Reviewed",
                "This request is already '" + r.getStatus() + "'.\nChange the decision to Approved?")) return;

        if (dao.updateDecision(r.getRequestId(), "Approved", adminRemarksArea.getText().trim())) {
            showAlert(AlertType.INFORMATION, "Approved",
                    "Request approved.\nYou can now create the trip for it in the Trips screen.");
            loadData();
        } else {
            showAlert(AlertType.ERROR, "Error", "Could not save the decision.");
        }
    }

    @FXML
    private void handleReject() {
        OrderRequest r = requestTable.getSelectionModel().getSelectedItem();
        if (r == null) { showAlert(AlertType.WARNING, "No Selection", "Please select a request first."); return; }
        if (adminRemarksArea.getText().isBlank()) {
            showAlert(AlertType.WARNING, "Remarks Required", "Please enter a reason for rejecting this request.");
            return;
        }
        if (!PENDING.equalsIgnoreCase(r.getStatus()) && !confirm("Already Reviewed",
                "This request is already '" + r.getStatus() + "'.\nChange the decision to Rejected?")) return;

        if (dao.updateDecision(r.getRequestId(), "Rejected", adminRemarksArea.getText().trim())) {
            showAlert(AlertType.INFORMATION, "Rejected", "Request rejected.");
            loadData();
        } else {
            showAlert(AlertType.ERROR, "Error", "Could not save the decision.");
        }
    }

    // ---------------------------------------------------------------- SEARCH / REFRESH / DELETE

    @FXML private void handleSearch() { applyFilters(); }

    @FXML
    private void handleReset() {
        searchField.clear();
        filterCategoryComboBox.setValue("All Fields");
        statusFilterComboBox.setValue("All Statuses");
        applyFilters();
    }

    @FXML private void handleRefresh() { loadData(); }

    private void applyFilters() {
        String kw = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String cat = filterCategoryComboBox.getValue() == null ? "All Fields" : filterCategoryComboBox.getValue();
        String st = statusFilterComboBox.getValue() == null ? "All Statuses" : statusFilterComboBox.getValue();

        filteredList.setPredicate(r -> {
            if (!st.equals("All Statuses") && !st.equalsIgnoreCase(r.getStatus())) return false;
            if (kw.isEmpty()) return true;
            switch (cat) {
                case "Request ID": return String.valueOf(r.getRequestId()).contains(kw);
                case "Receiver":   return has(r.getReceiverId(), kw) || has(r.getReceiverName(), kw) || has(r.getCompanyName(), kw);
                case "Pickup":     return has(r.getPickupLocation(), kw);
                case "Delivery":   return has(r.getDeliveryLocation(), kw);
                case "Cargo":      return has(r.getCargoType(), kw);
                default:
                    return String.valueOf(r.getRequestId()).contains(kw) || has(r.getReceiverId(), kw)
                            || has(r.getReceiverName(), kw) || has(r.getCompanyName(), kw)
                            || has(r.getPickupLocation(), kw) || has(r.getDeliveryLocation(), kw)
                            || has(r.getCargoType(), kw) || has(r.getStatus(), kw);
            }
        });
    }

    @FXML
    private void handleDeleteSelected() {
        OrderRequest r = requestTable.getSelectionModel().getSelectedItem();
        if (r == null) { showAlert(AlertType.WARNING, "No Selection", "Please select a request to delete."); return; }
        if (!confirm("Delete Request", "Delete request #" + r.getRequestId() + "?")) return;

        if (dao.deleteRequest(r.getRequestId())) {
            requestTable.getSelectionModel().clearSelection();
            loadData();
        } else {
            showAlert(AlertType.ERROR, "Error", "Delete failed.");
        }
    }

    @FXML
    private void handleDeleteAll() {
        if (!confirm("Delete ALL Requests", "This permanently deletes ALL order requests.\nContinue?")) return;
        if (dao.deleteAllRequests()) {
            requestTable.getSelectionModel().clearSelection();
            loadData();
        } else {
            showAlert(AlertType.ERROR, "Error", "Could not clear requests.");
        }
    }

    // ---------------------------------------------------------------- SIDEBAR + NAVIGATION

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

    @FXML private void switchToDashboard()     { loadScene("/dashboard_view.fxml", "Dashboard"); }
    @FXML private void switchToVehicles()      { loadScene("/vehicle_view.fxml", "Vehicle Management"); }
    @FXML private void switchToDrivers()       { loadScene("/driver_view.fxml", "Driver Management"); }
    @FXML private void switchToReceivers()     { loadScene("/receiver_view.fxml", "Receiver Management"); }
    @FXML private void switchToTrips()         { loadScene("/trip_view.fxml", "Trip Management"); }
    @FXML private void switchToMaintenance()   { loadScene("/maintenance_view.fxml", "Maintenance Management"); }
    @FXML private void switchToIncidents()     { loadScene("/incident_view.fxml", "Incident Management"); }
    @FXML private void switchToOrderProof()    { loadScene("/orderproof_view.fxml", "Order Proof"); }
    @FXML private void switchToOrderRequests() { loadScene("/orderrequest_view.fxml", "Order Requests"); }
    @FXML private void switchToReports()       { loadScene("/report_view.fxml", "Reports"); }
    @FXML private void switchToSettings()      { loadScene("/settings_view.fxml", "Settings"); }

    private void loadScene(String fxmlPath, String title) {
        try {
            URL fileUrl = getClass().getResource(fxmlPath);
            if (fileUrl == null) return;

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Parent root = new FXMLLoader(fileUrl).load();

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

    // ---------------------------------------------------------------- HELPERS

    private static String n(String s) { return (s == null || s.isBlank()) ? "-" : s; }
    private static boolean has(String field, String kw) { return field != null && field.toLowerCase().contains(kw); }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void showAlert(AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}