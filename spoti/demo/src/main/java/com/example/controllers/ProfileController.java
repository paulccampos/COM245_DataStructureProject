package com.example.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class ProfileController implements Initializable {

    @FXML
    private Label usernameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label memberSinceLabel;
    @FXML
    private Label lastUpdatedLabel;
    @FXML
    private Label genre1Label;
    @FXML
    private Label genre2Label;
    @FXML
    private Label genre3Label;

    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    private MongoService mongoService;

    public ProfileController() {
        mongoService = App.getMongoServiceStatic();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadUserProfile();
        loadTopGenres();
    }

    private void loadUserProfile() {
        try {
            String currentUsername = App.getCurrentUsername();
            if (currentUsername != null) {
                Document user = mongoService.getUser(currentUsername);
                if (user != null) {
                    String username = user.getString("username") != null ? user.getString("username") : "No data available";
                    String email = user.getString("email") != null ? user.getString("email") : "No data available";
                    String status = user.getString("status") != null ? user.getString("status") : "No data available";

                    usernameLabel.setText(username);
                    usernameLabel.setStyle("-fx-text-fill: #ffffff;");
                    emailLabel.setText(email);
                    emailLabel.setStyle("-fx-text-fill: #ffffff;");
                    statusLabel.setText(status);
                    statusLabel.setStyle("-fx-text-fill: #ffffff;");

                    Object createdDate = user.get("created_date");
                    if (createdDate instanceof java.util.Date) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM yyyy");
                        memberSinceLabel.setText("Member since: " + sdf.format((java.util.Date) createdDate));
                    } else {
                        memberSinceLabel.setText("Member since: Unknown");
                    }
                    memberSinceLabel.setStyle("-fx-text-fill: #ffffff;");

                    Object updatedDate = user.get("updated_at");
                    if (updatedDate instanceof java.util.Date) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm");
                        lastUpdatedLabel.setText("Last updated: " + sdf.format((java.util.Date) updatedDate));
                    } else {
                        lastUpdatedLabel.setText("Last updated: Never");
                    }
                    lastUpdatedLabel.setStyle("-fx-text-fill: #ffffff;");
                } else {
                    usernameLabel.setText("No data available");
                    emailLabel.setText("No data available");
                    statusLabel.setText("No data available");
                    memberSinceLabel.setText("Member since: Unknown");
                    lastUpdatedLabel.setText("Last updated: Never");
                }
            } else {
                usernameLabel.setText("Not logged in");
                emailLabel.setText("Not logged in");
                statusLabel.setText("Not logged in");
                memberSinceLabel.setText("Member since: Unknown");
                lastUpdatedLabel.setText("Last updated: Never");
            }
        } catch (Exception e) {
            System.err.println("Error loading user profile: " + e.getMessage());
            usernameLabel.setText("Error loading data");
            emailLabel.setText("Error loading data");
            statusLabel.setText("Error loading data");
            memberSinceLabel.setText("Member since: Error");
            lastUpdatedLabel.setText("Last updated: Error");
        }
    }

    private void loadTopGenres() {
        try {
            String currentUsername = App.getCurrentUsername();
            if (currentUsername != null) {
                List<String> topGenres = mongoService.getTopGenresForUser(currentUsername, 3);
                if (topGenres.size() > 0) {
                    genre1Label.setText("1. " + topGenres.get(0));
                    genre1Label.setStyle("-fx-text-fill: #ffffff;");
                } else {
                    genre1Label.setText("1. No data available");
                    genre1Label.setStyle("-fx-text-fill: #ffffff;");
                }
                if (topGenres.size() > 1) {
                    genre2Label.setText("2. " + topGenres.get(1));
                    genre2Label.setStyle("-fx-text-fill: #ffffff;");
                } else {
                    genre2Label.setText("2. -");
                    genre2Label.setStyle("-fx-text-fill: #ffffff;");
                }
                if (topGenres.size() > 2) {
                    genre3Label.setText("3. " + topGenres.get(2));
                    genre3Label.setStyle("-fx-text-fill: #ffffff;");
                } else {
                    genre3Label.setText("3. -");
                    genre3Label.setStyle("-fx-text-fill: #ffffff;");
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading top genres: " + e.getMessage());
            genre1Label.setText("1. Error loading data");
            genre1Label.setStyle("-fx-text-fill: #ffffff;");
            genre2Label.setText("2. -");
            genre3Label.setText("3. -");
        }
    }

    @FXML
    private void onChangePassword() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showAlert("Error", "All fields are required", Alert.AlertType.ERROR);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showAlert("Error", "New passwords do not match", Alert.AlertType.ERROR);
            return;
        }

        if (newPassword.length() < 6) {
            showAlert("Error", "New password must be at least 6 characters long", Alert.AlertType.ERROR);
            return;
        }

        try {
            String currentUsername = App.getCurrentUsername();
            if (currentUsername != null) {
                Document user = mongoService.getUser(currentUsername);
                if (user != null) {
                    String storedPassword = user.getString("password");

                    if (!currentPassword.equals(storedPassword)) {
                        showAlert("Error", "Current password is incorrect", Alert.AlertType.ERROR);
                        return;
                    }

                    boolean success = mongoService.updateUserPassword(currentUsername, newPassword);
                    if (success) {
                        showAlert("Success", "Password updated successfully", Alert.AlertType.INFORMATION);
                        currentPasswordField.clear();
                        newPasswordField.clear();
                        confirmPasswordField.clear();
                    } else {
                        showAlert("Error", "Failed to update password", Alert.AlertType.ERROR);
                    }
                }
            }
        } catch (Exception e) {
            showAlert("Error", "An error occurred while updating password", Alert.AlertType.ERROR);
            System.err.println("Error updating password: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onLogout() {
        try {
            App.logout();
        } catch (IOException e) {
            showAlert("Error", "Failed to log out.", Alert.AlertType.ERROR);
        }
    }
}
