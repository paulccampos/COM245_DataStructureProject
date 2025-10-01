package com.example.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import com.example.MongoService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

public class SignUpController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button signUpButton;
    @FXML private Button backToLoginLink;
    @FXML private Button backButton;
    @FXML private ProgressBar passwordStrengthBar;
    @FXML private Label passwordStrengthLabel;

    private MongoService mongoService;

    // Password strength patterns
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mongoService = new MongoService();
        setupPasswordStrengthListener();
        setupEnterKeyHandler();
    }

    @FXML
    private void handleSignUp() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate all fields
        if (!validateForm(username, email, password, confirmPassword)) {
            return;
        }

        // Attempt to create user
        boolean success = mongoService.createUser(username, email, password);

        if (success) {
            // Navigate to login
            try {
                com.example.App.setRoot("login");
            } catch (Exception e) {
                e.printStackTrace();
                // Removed error message to avoid red error at bottom
            }
        } else {
            // Removed error message display
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            com.example.App.setRoot("login");
        } catch (Exception e) {
            e.printStackTrace();
            // Removed error message display
        }
    }

    @FXML
    private void handleBack() {
        try {
            com.example.App.setRoot("login");
        } catch (Exception e) {
            e.printStackTrace();
            // Removed error message to avoid red error at bottom
            // showError("Error loading login page");
        }
    }

    private boolean validateForm(String username, String email, String password, String confirmPassword) {
        // Username validation
        if (username.isEmpty()) {
            // Removed error message display
            return false;
        }
        if (username.length() < 3) {
            // Removed error message display
            return false;
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            // Removed error message display
            return false;
        }

        // Email validation
        if (email.isEmpty()) {
            // Removed error message display
            return false;
        }
        if (!isValidEmail(email)) {
            // Removed error message display
            return false;
        }

        // Password validation
        if (password.isEmpty()) {
            // Removed error message display
            return false;
        }
        if (password.length() < 8) {
            // Removed error message display
            return false;
        }
        if (calculatePasswordScore(password) < 41) { // Require "Good" or better
            // Removed error message display
            return false;
        }

        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            // Removed error message display
            return false;
        }
        if (!password.equals(confirmPassword)) {
            // Removed error message display
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    private boolean isPasswordStrong(String password) {
        return LOWERCASE_PATTERN.matcher(password).matches() &&
               UPPERCASE_PATTERN.matcher(password).matches() &&
               DIGIT_PATTERN.matcher(password).matches() &&
               SPECIAL_CHAR_PATTERN.matcher(password).matches();
    }

    private void setupPasswordStrengthListener() {
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            updatePasswordStrength(newValue);
        });
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrengthBar.setProgress(0);
            passwordStrengthLabel.setText("");
            signUpButton.setDisable(true);
            return;
        }

        int score = calculatePasswordScore(password);
        String strengthText = "";
        String barColor = "";

        // Determine strength level based on score
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

        // Update UI
        passwordStrengthBar.setProgress(score / 100.0);
        passwordStrengthLabel.setText(strengthText);
        passwordStrengthLabel.setStyle("-fx-text-fill: " + barColor + ";");

        // Enable/disable sign up button based on password strength (require "Good" or better)
        signUpButton.setDisable(score < 41);
    }

    private int calculatePasswordScore(String password) {
        int score = 0;
        int length = password.length();

        // Length-based scoring
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

        // Adjust for exact length within range
        if (length >= 25) {
            score += Math.min((length - 24) * 2, 9); // Up to 100 for very long
        } else if (length >= 20) {
            score += (length - 19) * 2; // 81-90 for 20-24
        } else if (length >= 16) {
            score += (length - 15) * 2; // 61-70 for 16-19
        } else if (length >= 14) {
            score += (length - 13) * 2; // 51-54 for 14-15
        } else if (length >= 12) {
            score += (length - 11) * 2; // 41-44 for 12-13
        } else if (length >= 10) {
            score += (length - 9) * 2; // 31-34 for 10-11
        } else if (length >= 8) {
            score += (length - 7) * 2; // 21-24 for 8-9
        } else if (length >= 6) {
            score += (length - 5) * 2; // 11-14 for 6-7
        } else {
            score += length * 2; // 0-10 for 1-5
        }

        // Character type bonuses
        boolean hasLower = LOWERCASE_PATTERN.matcher(password).matches();
        boolean hasUpper = UPPERCASE_PATTERN.matcher(password).matches();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).matches();
        boolean hasSpecial = SPECIAL_CHAR_PATTERN.matcher(password).matches();

        int types = 0;
        if (hasLower) types++;
        if (hasUpper) types++;
        if (hasDigit) types++;
        if (hasSpecial) types++;

        // Type bonus
        if (types == 1) {
            // No bonus for single type
        } else if (types == 2) {
            score += 5;
        } else if (types == 3) {
            score += 10;
        } else if (types == 4) {
            score += 15;
        }

        // Penalize patterns
        if (hasSequentialChars(password)) score -= 5;
        if (hasRepeatedChars(password)) score -= 5;
        if (isCommonWord(password)) score -= 10;

        // Bonus for mixed case
        if (hasLower && hasUpper) score += 5;

        return Math.max(0, Math.min(score, 100));
    }

    private boolean isCommonWord(String password) {
        // Simple check for common passwords (can be expanded)
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

    private void setupEnterKeyHandler() {
        // Allow Enter key to move between fields and submit
        usernameField.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                emailField.requestFocus();
            }
        });

        emailField.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                passwordField.requestFocus();
            }
        });

        passwordField.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                confirmPasswordField.requestFocus();
            }
        });

        confirmPasswordField.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                handleSignUp();
            }
        });
    }



}
