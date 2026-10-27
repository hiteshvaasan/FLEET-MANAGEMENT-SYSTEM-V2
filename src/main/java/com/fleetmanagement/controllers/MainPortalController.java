package com.fleetmanagement.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.net.URL;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.net.InetAddress;

public class MainPortalController {

    @FXML private BorderPane rootPane;

    // Admin loading overlay elements
    @FXML private StackPane adminLoadingOverlay;
    @FXML private Group routeTruck;
    @FXML private Line trailBar;
    @FXML private Line trailStem;
    @FXML private Circle endMarker;
    @FXML private Circle endRing;
    @FXML private Label lblAdminLoadingCaption;
    @FXML private Label lblAdminLoadingSub;
    @FXML private ProgressBar adminLoadingBar;

    private boolean isAdminAnimating = false;

    // The "7" route: start -> corner -> end  (must match the numbers in the FXML)
    private static final double X0 = 96,  Y0 = 64;    // start (top-left of the 7)
    private static final double X1 = 384, Y1 = 64;    // corner (top-right of the 7)
    private static final double X2 = 216, Y2 = 280;   // end (bottom of the 7)

    private static final double LEN1 = Math.hypot(X1 - X0, Y1 - Y0);
    private static final double LEN2 = Math.hypot(X2 - X1, Y2 - Y1);
    private static final double TOTAL = LEN1 + LEN2;

    // direction the truck faces on each leg (degrees, JavaFX rotates clockwise)
    private static final double ANGLE1 = 0;
    private static final double ANGLE2 = Math.toDegrees(Math.atan2(Y2 - Y1, X2 - X1));

    private static final Color MARKER_IDLE = Color.web("#94a3b8", 0.55);
    private static final Color MARKER_DONE = Color.web("#4ade80");

    private final DoubleProperty routeProgress = new SimpleDoubleProperty(0);

    /** Called automatically by JavaFX after the FXML loads. */
    @FXML
    public void initialize() {
        // update the truck + trail whenever the progress value changes
        routeProgress.addListener((obs, oldV, newV) -> applyRouteProgress(newV.doubleValue()));
    }

    @FXML
    private void handleAdminLoginClick() {
        if (isAdminAnimating) return; // ignore double-clicks
        if (adminLoadingOverlay == null) {
            loadScene("/login_view.fxml", "Admin Login");
            return;
        }
        playAdminLoadingAnimation(() -> loadScene("/login_view.fxml", "Admin Login"));
    }

    @FXML
    public void handleDriverLoginClick() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Mobile Web Portal");
        alert.setHeaderText("📱 Driver Portal is Live");

        VBox content = new VBox(10);
        Label instruction = new Label("To access the Driver Portal, scan the QR code or open your mobile browser and go to:");
        Label url = new Label("http://localhost:8081");
        url.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb; -fx-font-size: 16px;");

        content.getChildren().addAll(instruction, url);
        alert.getDialogPane().setContent(content);

        alert.showAndWait();
    }

    @FXML
    private void handleReceiverTrackingClick() {
        loadScene("/receiver_tracking.fxml", "Track Shipment");
    }

    @FXML
    private void showDriverQrCodePopup() {
        try {
            String localIp = InetAddress.getLocalHost().getHostAddress();
            String mobileUrl = "http://" + localIp + ":8081";

            String qrApiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=" + mobileUrl;

            Image qrImage = new Image(qrApiUrl, true);
            ImageView imageView = new ImageView(qrImage);
            imageView.setFitWidth(200);
            imageView.setFitHeight(200);
            imageView.setPreserveRatio(true);

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
            String localIp = InetAddress.getLocalHost().getHostAddress();
            String receiverUrl = "http://" + localIp + ":8081/receiver_login.html";

            String qrApiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=" + receiverUrl;

            Image qrImage = new Image(qrApiUrl, true);
            ImageView imageView = new ImageView(qrImage);
            imageView.setFitWidth(200);
            imageView.setFitHeight(200);
            imageView.setPreserveRatio(true);

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

    // =====================================================================
    //  ADMIN LOADING ANIMATION — aerial view: one truck drives along the
    //  road that forms a "7" and leaves a glowing trail behind it.
    // =====================================================================
    private void playAdminLoadingAnimation(Runnable onFinished) {
        isAdminAnimating = true;

        // reset everything to the starting state
        routeProgress.set(0);
        applyRouteProgress(0);
        endMarker.setFill(MARKER_IDLE);
        endRing.setOpacity(0);
        endRing.setScaleX(1);
        endRing.setScaleY(1);

        lblAdminLoadingCaption.setText("Loading Admin Portal");
        lblAdminLoadingSub.setText("Preparing your dashboard...");
        adminLoadingBar.setProgress(0);

        adminLoadingOverlay.setOpacity(0);
        adminLoadingOverlay.setVisible(true);
        adminLoadingOverlay.setManaged(true);
        adminLoadingOverlay.toFront();

        FadeTransition overlayFadeIn = new FadeTransition(Duration.millis(250), adminLoadingOverlay);
        overlayFadeIn.setFromValue(0);
        overlayFadeIn.setToValue(1);

        // the drive itself (smooth start, smooth stop)
        Timeline drive = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(routeProgress, 0)),
                new KeyFrame(Duration.seconds(3.0), new KeyValue(routeProgress, 1, Interpolator.EASE_BOTH)));

        // arrival: end marker turns green + a ring pulses outward
        PauseTransition arrived = new PauseTransition(Duration.millis(50));
        arrived.setOnFinished(e -> {
            endMarker.setFill(MARKER_DONE);
            lblAdminLoadingCaption.setText("Ready!");
            lblAdminLoadingSub.setText("Opening Admin login...");

            endRing.setOpacity(0.95);
            ScaleTransition grow = new ScaleTransition(Duration.millis(700), endRing);
            grow.setFromX(1);
            grow.setFromY(1);
            grow.setToX(4.5);
            grow.setToY(4.5);
            grow.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition fade = new FadeTransition(Duration.millis(700), endRing);
            fade.setFromValue(0.95);
            fade.setToValue(0);

            new ParallelTransition(grow, fade).play();
        });

        PauseTransition finalHold = new PauseTransition(Duration.millis(850));

        SequentialTransition sequence = new SequentialTransition(overlayFadeIn, drive, arrived, finalHold);
        sequence.setOnFinished(e -> {
            isAdminAnimating = false;
            onFinished.run();
        });
        sequence.play();
    }

    /** p goes from 0 to 1. Places the truck, turns it, and stretches the trail. */
    private void applyRouteProgress(double p) {
        double d = p * TOTAL; // distance travelled along the route
        double x, y, angle;

        if (d <= LEN1) {
            // leg 1: the top bar of the 7
            x = X0 + d;
            y = Y0;
            angle = ANGLE1;

            trailBar.setEndX(x);
            trailBar.setEndY(y);
            trailStem.setEndX(X1);
            trailStem.setEndY(Y1);
        } else {
            // leg 2: the diagonal stem of the 7
            double s = (d - LEN1) / LEN2;
            x = X1 + (X2 - X1) * s;
            y = Y1 + (Y2 - Y1) * s;
            angle = ANGLE2;

            trailBar.setEndX(X1);
            trailBar.setEndY(Y1);
            trailStem.setEndX(x);
            trailStem.setEndY(y);
        }

        // smooth turn: blend the heading for a short distance around the corner
        double turnZone = 22;
        double fromCorner = d - LEN1;
        if (Math.abs(fromCorner) < turnZone) {
            double t = (fromCorner + turnZone) / (2 * turnZone); // 0..1 across the corner
            angle = ANGLE1 + (ANGLE2 - ANGLE1) * t;
        }

        routeTruck.setLayoutX(x);
        routeTruck.setLayoutY(y);
        routeTruck.setRotate(angle);

        adminLoadingBar.setProgress(p);
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