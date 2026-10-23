package com.fleetmanagement.controllers;

import com.fleetmanagement.dao.UserDAO;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Button btnLogin;
    @FXML private Button btnOpenCaptcha;

    // Overlay Popup Elements (CAPTCHA)
    @FXML private StackPane captchaOverlay;
    @FXML private ImageView dragSource;
    @FXML private StackPane dropTarget;
    @FXML private Label dropTargetLabel;
    @FXML private ImageView dropTargetImage;
    @FXML private VBox audioPromptBox;
    @FXML private Label lblVerificationStatus;

    // Sign-in animation elements
    @FXML private StackPane signInOverlay;
    @FXML private Pane animScene;
    @FXML private Label truckLabel;
    @FXML private Label destBuilding;
    @FXML private StackPane tickBadge;
    @FXML private Label lblSignInCaption;
    @FXML private Label lblSignInSub;

    private final UserDAO userDAO = new UserDAO();
    private AudioClip audioClip;
    private boolean isHumanVerified = false;

    // Animation helpers
    private Timeline dotsTimeline;
    private boolean isAnimating = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (visiblePasswordField != null && passwordField != null) {
            visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        }
        setupDragAndDrop();
    }

    // =====================================================================
    //  CAPTCHA (unchanged)
    // =====================================================================

    @FXML
    private void handleOpenCaptcha() {
        if (captchaOverlay != null) {
            captchaOverlay.setVisible(true);
            captchaOverlay.setManaged(true);
        }
    }

    @FXML
    private void handleCloseCaptcha() {
        if (captchaOverlay != null) {
            captchaOverlay.setVisible(false);
            captchaOverlay.setManaged(false);
        }
        if (audioClip != null && audioClip.isPlaying()) {
            audioClip.stop();
        }
    }

    private void setupDragAndDrop() {
        if (dragSource == null || dropTarget == null) return;

        // 1. DRAG DETECTED ON LARGE DHONI IMAGE
        dragSource.setOnDragDetected(event -> {
            Dragboard db = dragSource.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString("DHONI_IMAGE");
            db.setContent(content);
            db.setDragView(dragSource.getImage());
            event.consume();
        });

        // 2. DRAG OVER TARGET
        dropTarget.setOnDragOver(event -> {
            if (event.getGestureSource() != dropTarget && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        // 3. DRAG DROPPED
        dropTarget.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                if (dropTargetImage != null) {
                    dropTargetImage.setImage(dragSource.getImage());
                    dropTargetImage.setVisible(true);
                }
                if (dropTargetLabel != null) dropTargetLabel.setVisible(false);
                dragSource.setVisible(false);
                success = true;

                triggerSoundVerification();
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void triggerSoundVerification() {
        if (lblVerificationStatus != null) {
            lblVerificationStatus.setText("Playing 6-second BGM tune...");
            lblVerificationStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #2563eb; -fx-font-weight: bold;");
        }

        try {
            URL soundUrl = getClass().getResource("/audio/bgm.mp3");
            if (soundUrl == null) {
                soundUrl = getClass().getResource("/bgm.mp3");
            }

            if (soundUrl != null) {
                audioClip = new AudioClip(soundUrl.toExternalForm());
                audioClip.play();

                PauseTransition stopTask = new PauseTransition(Duration.seconds(6));
                stopTask.setOnFinished(e -> {
                    if (audioClip != null && audioClip.isPlaying()) {
                        audioClip.stop();
                    }
                });
                stopTask.play();
            }
        } catch (Exception e) {
            System.out.println("Audio play error: " + e.getMessage());
        }

        if (audioPromptBox != null) {
            audioPromptBox.setVisible(true);
            audioPromptBox.setManaged(true);
        }
    }

    @FXML
    private void handleAudioYes() {
        isHumanVerified = true;

        if (audioPromptBox != null) {
            audioPromptBox.setVisible(false);
            audioPromptBox.setManaged(false);
        }

        if (btnLogin != null) {
            btnLogin.setDisable(false);
        }
        if (btnOpenCaptcha != null) {
            btnOpenCaptcha.setText("✅ Security CAPTCHA Verified");
            btnOpenCaptcha.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: #86efac; -fx-border-radius: 6; -fx-background-radius: 6;");
        }

        handleCloseCaptcha();
    }

    @FXML
    private void handleAudioNo() {
        isHumanVerified = false;
        if (audioClip != null && audioClip.isPlaying()) {
            audioClip.stop();
        }

        if (dropTargetImage != null) dropTargetImage.setVisible(false);
        if (dropTargetLabel != null) dropTargetLabel.setVisible(true);
        if (dragSource != null) dragSource.setVisible(true);
        if (audioPromptBox != null) {
            audioPromptBox.setVisible(false);
            audioPromptBox.setManaged(false);
        }
        if (lblVerificationStatus != null) {
            lblVerificationStatus.setText("❌ Verification failed. Try dragging again.");
            lblVerificationStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc2626; -fx-font-weight: bold;");
        }
        if (btnLogin != null) {
            btnLogin.setDisable(true);
        }
    }

    @FXML
    private void handleShowPasswordToggle() {
        if (showPasswordCheckBox.isSelected()) {
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
        } else {
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
        }
    }

    // =====================================================================
    //  LOGIN  (now plays the truck animation before opening the dashboard)
    // =====================================================================

    @FXML
    private void handleLogin() {
        if (isAnimating) return; // prevent double clicks

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Form Incomplete", "Please enter both username and password.");
            return;
        }

        if (!isHumanVerified) {
            showAlert(Alert.AlertType.WARNING, "Verification Required", "Please complete the security CAPTCHA check first.");
            return;
        }

        if (userDAO.verifyPassword(username, password)) {
            // Credentials OK -> play animation, then go to dashboard
            playSignInAnimation(this::navigateToDashboard);
        } else {
            showAlert(Alert.AlertType.ERROR, "Authentication Failed", "Invalid username or password.");
        }
    }

    /**
     * Truck drives from the start building to the destination building,
     * enters it, a tick pops up, caption changes to "Success!", then onFinished runs.
     */
    private void playSignInAnimation(Runnable onFinished) {
        // If the overlay isn't in the FXML for some reason, just continue normally
        if (signInOverlay == null || truckLabel == null || tickBadge == null) {
            onFinished.run();
            return;
        }

        isAnimating = true;
        btnLogin.setDisable(true);

        // ---- reset the scene to its starting state ----
        truckLabel.setTranslateX(0);
        truckLabel.setOpacity(1);
        truckLabel.setVisible(true);
        tickBadge.setVisible(false);
        tickBadge.setScaleX(0);
        tickBadge.setScaleY(0);
        lblSignInCaption.setText("Signing in");
        lblSignInCaption.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        lblSignInSub.setText("Verifying your credentials");

        signInOverlay.setOpacity(0);
        signInOverlay.setVisible(true);
        signInOverlay.setManaged(true);
        signInOverlay.toFront();

        // ---- animated dots: "Signing in." -> ".." -> "..." ----
        final int[] dotCount = {0};
        dotsTimeline = new Timeline(new KeyFrame(Duration.millis(400), e -> {
            dotCount[0] = (dotCount[0] % 3) + 1;
            lblSignInCaption.setText("Signing in" + ".".repeat(dotCount[0]));
        }));
        dotsTimeline.setCycleCount(Timeline.INDEFINITE);
        dotsTimeline.play();

        // ---- 1) overlay fades in ----
        FadeTransition overlayFadeIn = new FadeTransition(Duration.millis(250), signInOverlay);
        overlayFadeIn.setFromValue(0);
        overlayFadeIn.setToValue(1);

        // ---- 2) truck drives to the destination building ----
        // Increase / decrease 175 if you change the scene width or building positions
        TranslateTransition drive = new TranslateTransition(Duration.seconds(2.2), truckLabel);
        drive.setFromX(0);
        drive.setToX(175);
        drive.setInterpolator(Interpolator.EASE_BOTH);

        // ---- 3) truck "enters" the building (fades out) ----
        FadeTransition enterBuilding = new FadeTransition(Duration.millis(350), truckLabel);
        enterBuilding.setFromValue(1);
        enterBuilding.setToValue(0);

        // ---- 4) tick pops in ----
        PauseTransition showTick = new PauseTransition(Duration.millis(50));
        showTick.setOnFinished(e -> {
            if (dotsTimeline != null) dotsTimeline.stop();
            tickBadge.setVisible(true);
            lblSignInCaption.setText("Success!");
            lblSignInCaption.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
            lblSignInSub.setText("Welcome back, opening your dashboard...");
        });

        ScaleTransition tickPop = new ScaleTransition(Duration.millis(450), tickBadge);
        tickPop.setFromX(0);
        tickPop.setFromY(0);
        tickPop.setToX(1);
        tickPop.setToY(1);
        tickPop.setInterpolator(Interpolator.EASE_OUT);

        // ---- 5) short pause so the user can see the success tick ----
        PauseTransition hold = new PauseTransition(Duration.millis(900));

        SequentialTransition sequence = new SequentialTransition(
                overlayFadeIn, drive, enterBuilding, showTick, tickPop, hold);
        sequence.setOnFinished(e -> onFinished.run());
        sequence.play();
    }

    private void hideSignInOverlay() {
        if (dotsTimeline != null) dotsTimeline.stop();
        if (signInOverlay != null) {
            signInOverlay.setVisible(false);
            signInOverlay.setManaged(false);
        }
        isAnimating = false;
        if (btnLogin != null) btnLogin.setDisable(!isHumanVerified);
    }

    private void navigateToDashboard() {
        try {
            URL fileUrl = getClass().getResource("/dashboard_view.fxml");
            if (fileUrl == null) {
                hideSignInOverlay();
                return;
            }

            Stage stage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(fileUrl);
            Parent root = loader.load();

            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new Scene(root, 1050, 680));
            }

            if (!stage.isMaximized()) {
                stage.setWidth(1050);
                stage.setHeight(680);
                stage.centerOnScreen();
            }

            stage.setTitle("Fleet Management System V3 - Dashboard");

        } catch (Exception e) {
            e.printStackTrace();
            hideSignInOverlay();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to load dashboard.");
        }
    }

    // =====================================================================
    //  BACK TO PORTAL (unchanged)
    // =====================================================================

    @FXML
    private void handleBackToPortal() {
        try {
            URL fileUrl = getClass().getResource("/main_portal.fxml");
            if (fileUrl == null) return;

            Stage stage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(fileUrl);
            Parent root = loader.load();

            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new Scene(root, 1050, 680));
            }

            if (!stage.isMaximized()) {
                stage.setWidth(1050);
                stage.setHeight(680);
                stage.centerOnScreen();
            }

            stage.setTitle("Fleet Management System V3 - Main Portal");

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