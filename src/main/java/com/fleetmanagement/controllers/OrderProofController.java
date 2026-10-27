package com.fleetmanagement.controllers;

import com.fleetmanagement.dao.OrderProofDAO;
import com.fleetmanagement.model.OrderProof;
import com.fleetmanagement.utils.ThemeManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;

public class OrderProofController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Button btnHamburger;

    @FXML private ComboBox<String> filterCategoryComboBox, statusFilterComboBox;
    @FXML private TextField searchField;

    @FXML private TableView<OrderProof> proofTable;
    @FXML private TableColumn<OrderProof, Integer> colReportId;
    @FXML private TableColumn<OrderProof, String> colTrip, colDriver, colReceiver,
            colDriverStatus, colReceiverStatus, colMatch, colAdminStatus;

    @FXML private Label lblTripRoute, lblMatchBanner, lblAdminStatus;
    @FXML private Label lblDriverName, lblVehicle, lblDriverStatus, lblDriverQty,
            lblDriverReceiver, lblDriverTime, lblDriverRemarks;
    @FXML private Label lblReceiverId, lblReceiverPerson, lblReceiverStatus, lblReceiverQty,
            lblReceiverRating, lblReceiverNotes;
    @FXML private ImageView driverImage, receiverImage;
    @FXML private Label lblDriverNoPhoto, lblReceiverNoPhoto;
    @FXML private TextArea adminRemarksArea;

    private final OrderProofDAO dao = new OrderProofDAO();
    private final ObservableList<OrderProof> masterList = FXCollections.observableArrayList();
    private FilteredList<OrderProof> filteredList;
    private Image driverFullImage, receiverFullImage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ThemeManager.isDarkMode() && rootPane != null) {
            rootPane.setStyle("-fx-background-color: #0f172a;");
        }

        filterCategoryComboBox.setItems(FXCollections.observableArrayList(
                "All Fields", "Trip ID", "Driver", "Receiver", "Match Result"));
        filterCategoryComboBox.setValue("All Fields");
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                "All Statuses", "Pending Review", "Approved", "Flagged"));
        statusFilterComboBox.setValue("All Statuses");

        colReportId.setCellValueFactory(new PropertyValueFactory<>("reportId"));
        colTrip.setCellValueFactory(new PropertyValueFactory<>("tripId"));
        colDriver.setCellValueFactory(new PropertyValueFactory<>("driverName"));
        colReceiver.setCellValueFactory(new PropertyValueFactory<>("receiverId"));
        colDriverStatus.setCellValueFactory(new PropertyValueFactory<>("driverStatus"));
        colReceiverStatus.setCellValueFactory(new PropertyValueFactory<>("receiverStatus"));
        colMatch.setCellValueFactory(new PropertyValueFactory<>("matchResult"));
        colAdminStatus.setCellValueFactory(new PropertyValueFactory<>("adminStatus"));
        applyBadge(colMatch);
        applyBadge(colAdminStatus);

        filteredList = new FilteredList<>(masterList, p -> true);
        SortedList<OrderProof> sorted = new SortedList<>(filteredList);
        sorted.comparatorProperty().bind(proofTable.comparatorProperty());
        proofTable.setItems(sorted);

        proofTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> showDetails(newSel));

        searchField.setOnAction(e -> applyFilters());
        statusFilterComboBox.setOnAction(e -> applyFilters());

        clearDetails();
        loadData();
    }

    // ---------------------------------------------------------------- DATA

    private void loadData() {
        OrderProof sel = proofTable.getSelectionModel().getSelectedItem();
        int selectedId = sel != null ? sel.getReportId() : -1;

        masterList.setAll(dao.getAllProofs());

        if (selectedId != -1) {
            for (OrderProof p : proofTable.getItems()) {
                if (p.getReportId() == selectedId) {
                    proofTable.getSelectionModel().select(p);
                    return;
                }
            }
        }
        clearDetails();
    }

    // ---------------------------------------------------------------- DETAILS PANEL

    private void showDetails(OrderProof p) {
        if (p == null) { clearDetails(); return; }

        lblTripRoute.setText("Trip " + n(p.getTripId()) + "   •   " + n(p.getSource()) + "  →  " + n(p.getDestination()));

        lblDriverName.setText(n(p.getDriverName()));
        lblVehicle.setText(n(p.getVehicleId()));
        lblDriverStatus.setText(n(p.getDriverStatus()));
        lblDriverQty.setText(p.getDriverQty() != null ? String.valueOf(p.getDriverQty()) : "-");
        lblDriverReceiver.setText(n(p.getDriverReceiverName()));
        lblDriverTime.setText(n(p.getReportedAt()));
        lblDriverRemarks.setText(n(p.getDriverRemarks()));

        boolean receiverDone = p.getReceiverStatus() != null;
        lblReceiverId.setText(n(p.getReceiverId()));
        lblReceiverPerson.setText(receiverDone ? n(p.getReceiverPerson()) : "Not submitted yet");
        lblReceiverStatus.setText(receiverDone ? p.getReceiverStatus() : "Awaiting receiver");
        lblReceiverQty.setText(p.getReceiverQty() != null ? String.valueOf(p.getReceiverQty()) : "-");
        lblReceiverRating.setText(p.getReceiverRating() != null && p.getReceiverRating() > 0
                ? "★".repeat(p.getReceiverRating()) + "☆".repeat(5 - p.getReceiverRating()) : "-");
        lblReceiverNotes.setText(n(p.getReceiverNotes()));

        styleMatchBanner(p.getMatchResult());
        lblAdminStatus.setText("Current: " + n(p.getAdminStatus()));
        adminRemarksArea.setText(p.getAdminRemarks() != null ? p.getAdminRemarks() : "");

        loadPhotosAsync(p.getReportId());
    }

    private void loadPhotosAsync(int reportId) {
        driverImage.setImage(null);
        receiverImage.setImage(null);
        driverFullImage = receiverFullImage = null;
        lblDriverNoPhoto.setText("Loading photo...");
        lblReceiverNoPhoto.setText("Loading photo...");
        lblDriverNoPhoto.setVisible(true);
        lblReceiverNoPhoto.setVisible(true);

        Task<Image[]> task = new Task<>() {
            @Override protected Image[] call() {
                String[] photos = dao.getPhotos(reportId);
                return new Image[]{decodeImage(photos[0]), decodeImage(photos[1])};
            }
        };
        task.setOnSucceeded(e -> {
            OrderProof cur = proofTable.getSelectionModel().getSelectedItem();
            if (cur == null || cur.getReportId() != reportId) return; // user already moved on
            driverFullImage = task.getValue()[0];
            receiverFullImage = task.getValue()[1];
            driverImage.setImage(driverFullImage);
            receiverImage.setImage(receiverFullImage);
            lblDriverNoPhoto.setText("No photo");
            lblReceiverNoPhoto.setText("No photo");
            lblDriverNoPhoto.setVisible(driverFullImage == null);
            lblReceiverNoPhoto.setVisible(receiverFullImage == null);
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /** Handles data-URLs (data:image/jpeg;base64,...), raw base64, or a file path. */
    private Image decodeImage(String data) {
        if (data == null || data.isBlank()) return null;
        try {
            if (data.startsWith("data:")) {
                String b64 = data.substring(data.indexOf(',') + 1);
                return new Image(new ByteArrayInputStream(Base64.getMimeDecoder().decode(b64)));
            }
            File f = new File(data);
            if (!f.exists()) f = new File("uploads/" + data);
            if (f.exists()) return new Image(f.toURI().toString());
            return new Image(new ByteArrayInputStream(Base64.getMimeDecoder().decode(data)));
        } catch (Exception ex) {
            return null;
        }
    }

    private void clearDetails() {
        lblTripRoute.setText("Select a record from the table to review");
        lblMatchBanner.setText("");
        lblMatchBanner.setStyle("");
        for (Label l : new Label[]{lblDriverName, lblVehicle, lblDriverStatus, lblDriverQty, lblDriverReceiver,
                lblDriverTime, lblDriverRemarks, lblReceiverId, lblReceiverPerson, lblReceiverStatus,
                lblReceiverQty, lblReceiverRating, lblReceiverNotes}) {
            l.setText("-");
        }
        driverImage.setImage(null);
        receiverImage.setImage(null);
        driverFullImage = receiverFullImage = null;
        lblDriverNoPhoto.setText("No photo");
        lblReceiverNoPhoto.setText("No photo");
        lblDriverNoPhoto.setVisible(true);
        lblReceiverNoPhoto.setVisible(true);
        lblAdminStatus.setText("");
        adminRemarksArea.clear();
    }

    private void styleMatchBanner(String match) {
        String bg, fg, text;
        switch (match) {
            case "Matched":
                bg = "#dcfce7"; fg = "#166534"; text = "✔ Driver and receiver evidence MATCH"; break;
            case "Mismatch":
                bg = "#fee2e2"; fg = "#991b1b"; text = "⚠ MISMATCH between driver and receiver (status or quantity differs)"; break;
            case "Awaiting Receiver":
                bg = "#fef3c7"; fg = "#92400e"; text = "⏳ Waiting for the receiver to submit confirmation"; break;
            case "Awaiting Driver":
                bg = "#fef3c7"; fg = "#92400e"; text = "⏳ Waiting for the driver's delivery report"; break;
            default:
                bg = "#e2e8f0"; fg = "#334155"; text = "No evidence submitted yet";
        }
        lblMatchBanner.setText(text);
        lblMatchBanner.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg
                + "; -fx-font-weight: bold; -fx-padding: 8 12 8 12; -fx-background-radius: 6;");
    }

    private void applyBadge(TableColumn<OrderProof, String> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color;
                switch (item) {
                    case "Matched": case "Approved": color = "#16a34a"; break;
                    case "Mismatch": case "Flagged": color = "#dc2626"; break;
                    default: color = "#b45309";
                }
                setStyle("-fx-text-fill:" + color + "; -fx-font-weight: bold;");
            }
        });
    }

    // ---------------------------------------------------------------- ADMIN ACTIONS

    @FXML
    private void handleApprove() {
        OrderProof p = proofTable.getSelectionModel().getSelectedItem();
        if (p == null) { showAlert(AlertType.WARNING, "No Selection", "Please select a record first."); return; }

        String match = p.getMatchResult();
        if (match.startsWith("Awaiting") || match.equals("No Data")) {
            showAlert(AlertType.WARNING, "Cannot Approve",
                    "Both the driver and the receiver must submit their evidence before you can approve.");
            return;
        }
        if (match.equals("Mismatch") && !confirm("Mismatch Detected",
                "Driver and receiver evidence do NOT match.\nDo you still want to approve this delivery?")) {
            return;
        }
        if (dao.updateAdminDecision(p.getReportId(), "Approved", adminRemarksArea.getText().trim())) {
            showAlert(AlertType.INFORMATION, "Approved", "Delivery approved and audit trail closed.");
            loadData();
        } else {
            showAlert(AlertType.ERROR, "Error", "Could not save the decision.");
        }
    }

    @FXML
    private void handleFlag() {
        OrderProof p = proofTable.getSelectionModel().getSelectedItem();
        if (p == null) { showAlert(AlertType.WARNING, "No Selection", "Please select a record first."); return; }
        if (adminRemarksArea.getText().isBlank()) {
            showAlert(AlertType.WARNING, "Remarks Required", "Please explain the discrepancy in the remarks box.");
            return;
        }
        if (dao.updateAdminDecision(p.getReportId(), "Flagged", adminRemarksArea.getText().trim())) {
            showAlert(AlertType.INFORMATION, "Flagged", "Record flagged for follow-up.");
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

        filteredList.setPredicate(p -> {
            if (!st.equals("All Statuses") && !st.equalsIgnoreCase(p.getAdminStatus())) return false;
            if (kw.isEmpty()) return true;
            switch (cat) {
                case "Trip ID":      return has(p.getTripId(), kw);
                case "Driver":       return has(p.getDriverName(), kw) || has(p.getDriverId(), kw);
                case "Receiver":     return has(p.getReceiverId(), kw);
                case "Match Result": return has(p.getMatchResult(), kw);
                default:
                    return has(p.getTripId(), kw) || has(p.getDriverName(), kw) || has(p.getReceiverId(), kw)
                            || has(p.getDriverStatus(), kw) || has(p.getReceiverStatus(), kw)
                            || has(p.getMatchResult(), kw) || has(p.getAdminStatus(), kw);
            }
        });
    }

    @FXML
    private void handleDeleteSelected() {
        OrderProof p = proofTable.getSelectionModel().getSelectedItem();
        if (p == null) { showAlert(AlertType.WARNING, "No Selection", "Please select a record to delete."); return; }
        if (!confirm("Delete Record", "Delete the delivery report for trip " + p.getTripId() + "?\n"
                + "This removes both the driver's and the receiver's evidence.")) return;

        if (dao.deleteReport(p.getReportId())) {
            proofTable.getSelectionModel().clearSelection();
            loadData();
        } else {
            showAlert(AlertType.ERROR, "Error", "Delete failed.");
        }
    }

    @FXML
    private void handleDeleteAll() {
        if (!confirm("Delete ALL Records", "This permanently deletes ALL delivery reports (driver and receiver evidence).\nContinue?")) return;
        if (dao.deleteAllReports()) {
            proofTable.getSelectionModel().clearSelection();
            loadData();
        } else {
            showAlert(AlertType.ERROR, "Error", "Could not clear records.");
        }
    }

    // ---------------------------------------------------------------- IMAGE ENLARGE

    @FXML private void openDriverImage()   { openFullImage(driverFullImage, "Driver's Delivery Photo"); }
    @FXML private void openReceiverImage() { openFullImage(receiverFullImage, "Receiver's Verification Photo"); }

    private void openFullImage(Image img, String title) {
        if (img == null) return;
        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);
        iv.setFitWidth(900);
        iv.setFitHeight(650);
        Stage s = new Stage();
        s.setTitle(title);
        s.setScene(new Scene(new StackPane(iv)));
        s.show();
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

    @FXML private void switchToDashboard()   { loadScene("/dashboard_view.fxml", "Dashboard"); }
    @FXML private void switchToVehicles()    { loadScene("/vehicle_view.fxml", "Vehicle Management"); }
    @FXML private void switchToDrivers()     { loadScene("/driver_view.fxml", "Driver Management"); }
    @FXML private void switchToReceivers()   { loadScene("/receiver_view.fxml", "Receiver Management"); }
    @FXML private void switchToTrips()       { loadScene("/trip_view.fxml", "Trip Management"); }
    @FXML private void switchToMaintenance() { loadScene("/maintenance_view.fxml", "Maintenance Management"); }
    @FXML private void switchToIncidents()   { loadScene("/incident_view.fxml", "Incident Management"); }
    @FXML private void switchToOrderProof()  { loadScene("/orderproof_view.fxml", "Order Proof"); }
    @FXML private void switchToReports()     { loadScene("/report_view.fxml", "Reports"); }
    @FXML private void switchToSettings()    { loadScene("/settings_view.fxml", "Settings"); }

    @FXML private void switchToOrderRequests() { loadScene("/orderrequest_view.fxml", "Order Requests"); }
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