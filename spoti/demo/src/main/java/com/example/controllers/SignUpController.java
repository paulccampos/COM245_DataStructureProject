package com.example.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import com.example.MongoService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

public class SignUpController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button signUpButton;
    @FXML private Button backToLoginLink;
    @FXML private Button backButton;
    @FXML private ProgressBar passwordStrengthBar;
    @FXML private Label passwordStrengthLabel;
    @FXML private CheckBox showPasswordCheckBox;

    private MongoService mongoService;

    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mongoService = new MongoService();
        setupPasswordStrengthListener();
        setupEnterKeyHandler();
        setupShowPasswordToggle();
    }

    @FXML
    private void handleSignUp() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!validateForm(username, email, password, confirmPassword)) {
            return;
        }

        boolean success = mongoService.createUser(username, email, password);

        if (success) {
            try {
                com.example.App.setRoot("login");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            com.example.App.setRoot("login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        try {
            com.example.App.setRoot("login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean validateForm(String username, String email, String password, String confirmPassword) {
        if (username.isEmpty()) {
            return false;
        }
        if (username.length() < 3) {
            return false;
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return false;
        }

        if (email.isEmpty()) {
            return false;
        }
        if (!isValidEmail(email)) {
            return false;
        }

        if (password.isEmpty()) {
            return false;
        }
        if (password.length() < 8) {
            return false;
        }
        if (calculatePasswordScore(password) < 41) { // Require "Good" or better
            return false;
        }

        if (confirmPassword.isEmpty()) {
            return false;
        }
        if (!password.equals(confirmPassword)) {
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
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

        passwordStrengthBar.setProgress(score / 100.0);
        passwordStrengthLabel.setText(strengthText);
        passwordStrengthLabel.setStyle("-fx-text-fill: " + barColor + ";");

        signUpButton.setDisable(score < 41);
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

    private void setupEnterKeyHandler() {
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

    private void setupShowPasswordToggle() {
        // Bind the text properties
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());

        showPasswordCheckBox.setOnAction(event -> {
            if (showPasswordCheckBox.isSelected()) {
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                passwordTextField.setVisible(true);
                passwordTextField.setManaged(true);
                passwordTextField.requestFocus();
            } else {
                passwordTextField.setVisible(false);
                passwordTextField.setManaged(false);
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                passwordField.requestFocus();
            }
        });
    }

}
