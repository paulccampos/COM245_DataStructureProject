package com.example.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

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
    private TextField newPasswordTextField;
    @FXML
    private CheckBox showNewPasswordCheckBox;
    @FXML
    private ProgressBar newPasswordStrengthBar;
    @FXML
    private Label newPasswordStrengthLabel;
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
        setupShowPasswordToggle();
        setupPasswordStrengthListener();
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

    private void setupShowPasswordToggle() {
        // Bind the text properties
        newPasswordTextField.textProperty().bindBidirectional(newPasswordField.textProperty());

        showNewPasswordCheckBox.setOnAction(event -> {
            if (showNewPasswordCheckBox.isSelected()) {
                newPasswordField.setVisible(false);
                newPasswordField.setManaged(false);
                newPasswordTextField.setVisible(true);
                newPasswordTextField.setManaged(true);
                newPasswordTextField.requestFocus();
            } else {
                newPasswordTextField.setVisible(false);
                newPasswordTextField.setManaged(false);
                newPasswordField.setVisible(true);
                newPasswordField.setManaged(true);
                newPasswordField.requestFocus();
            }
        });
    }

    private void setupPasswordStrengthListener() {
        newPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            updatePasswordStrength(newValue);
        });
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            newPasswordStrengthBar.setProgress(0);
            newPasswordStrengthLabel.setText("");
            return;
        }

        int score = calculatePasswordScore(password);
        String strengthText = "";
        String barColor = "";

        if (score <= 10) {
            strengthText = "Very Weak";
            barColor = "#ff6b6b";
        } else if (score <= 20) {
            strengthText = "Weak";
            barColor = "#ff8a65";
        } else if (score <= 30) {
            strengthText = "Fair";
            barColor = "#ffb74d";
        } else if (score <= 40) {
            strengthText = "Fairly Good";
            barColor = "#ffeb3b";
        } else if (score <= 50) {
            strengthText = "Good";
            barColor = "#cddc39";
        } else if (score <= 60) {
            strengthText = "Very Good";
            barColor = "#8bc34a";
        } else if (score <= 80) {
            strengthText = "Strong";
            barColor = "#4caf50";
        } else {
            strengthText = "Very Strong";
            barColor = "#2e7d32";
        }

        newPasswordStrengthBar.setProgress(score / 100.0);
        newPasswordStrengthLabel.setText(strengthText);
        newPasswordStrengthLabel.setStyle("-fx-text-fill: " + barColor + ";");
    }

    private int calculatePasswordScore(String password) {
        int score = 0;
        int length = password.length();

        if (length >= 25) {
            score = 91;
        } else if (length >= 20) {
            score = 81;
        } else if (length >= 16) {
            score = 61;
        } else if (length >= 14) {
            score = 51;
        } else if (length >= 12) {
            score = 41;
        } else if (length >= 10) {
            score = 31;
        } else if (length >= 8) {
            score = 21;
        } else if (length >= 6) {
            score = 11;
        } else {
            score = 0;
        }

        if (length >= 25) {
            score += Math.min((length - 24) * 2, 9);
        } else if (length >= 20) {
            score += (length - 19) * 2;
        } else if (length >= 16) {
            score += (length - 15) * 2;
        } else if (length >= 14) {
            score += (length - 13) * 2;
        } else if (length >= 12) {
            score += (length - 11) * 2;
        } else if (length >= 10) {
            score += (length - 9) * 2;
        } else if (length >= 8) {
            score += (length - 7) * 2;
        } else if (length >= 6) {
            score += (length - 5) * 2;
        } else {
            score += length * 2;
        }

        boolean hasLower = LOWERCASE_PATTERN.matcher(password).matches();
        boolean hasUpper = UPPERCASE_PATTERN.matcher(password).matches();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).matches();
        boolean hasSpecial = SPECIAL_CHAR_PATTERN.matcher(password).matches();

        int types = 0;
        if (hasLower) types++;
        if (hasUpper) types++;
        if (hasDigit) types++;
        if (hasSpecial) types++;

        if (types == 1) {
        } else if (types == 2) {
            score += 5;
        } else if (types == 3) {
            score += 10;
        } else if (types == 4) {
            score += 15;
        }

        if (hasSequentialChars(password)) score -= 5;
        if (hasRepeatedChars(password)) score -= 5;
        if (isCommonWord(password)) score -= 10;

        if (hasLower && hasUpper) score += 5;

        return Math.max(0, Math.min(score, 100));
    }

    private boolean isCommonWord(String password) {
        String[] commonWords = {"password", "123456", "qwerty", "abc123", "letmein", "welcome", "admin", "user", "guest"};
        String lowerPass = password.toLowerCase();
        for (String word : commonWords) {
            if (lowerPass.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSequentialChars(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char a = password.charAt(i);
            char b = password.charAt(i + 1);
            char c = password.charAt(i + 2);
            if ((b == a + 1 && c == b + 1) || (b == a - 1 && c == b - 1)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRepeatedChars(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            if (password.charAt(i) == password.charAt(i + 1) &&
                password.charAt(i + 1) == password.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }

    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
}
