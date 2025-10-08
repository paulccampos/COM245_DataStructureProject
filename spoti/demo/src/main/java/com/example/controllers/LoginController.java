package com.example.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;

import org.bson.Document;

import com.example.MongoService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink signUpLink;
    @FXML private CheckBox rememberMeCheckBox;

    private MongoService mongoService;
    private static final String REMEMBER_FILE = "user_credentials.dat";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mongoService = new MongoService();
        setupEnterKeyHandler();
        loadRememberedCredentials();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            // Removed error message display
            return;
        }

        // Attempt authentication
        Document user = mongoService.authenticateUser(username, password);

        if (user != null) {
            // Login successful
            System.out.println("Login successful for user: " + username);
            String userStatus = user.getString("status");

            // Save credentials if remember me is checked
            if (rememberMeCheckBox.isSelected()) {
                saveRememberedCredentials(username, password);
            } else {
                clearRememberedCredentials();
            }

            // Navigate to appropriate view based on user status
            if ("admin".equals(userStatus)) {
                // Show admin choice dialog
                showAdminChoiceDialog(user);
            } else {
                // Navigate to home for regular users
                navigateToHome(user);
            }
        } else {
            // Login failed
            // Removed error message display
        }
    }

    @FXML
    private void handleSignUp() {
        try {
            com.example.App.setRoot("signup");
        } catch (Exception e) {
            e.printStackTrace();
            // Removed error message to avoid red error at bottom
            // showError("Error loading sign up page");
        }
    }

    private void showAdminChoiceDialog(Document user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Admin Access");
        alert.setHeaderText("Welcome, Admin!");
        alert.setContentText("Choose how you would like to access the system:");

        ButtonType listenButton = new ButtonType("Listen (User Mode)");
        ButtonType adminButton = new ButtonType("Admin Panel");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(listenButton, adminButton, cancelButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == listenButton) {
                navigateToHome(user);
            } else if (response == adminButton) {
                navigateToAdmin(user);
            }
            // If cancel, do nothing - stay on login page
        });
    }

    private void navigateToHome(Document user) {
        try {
            // Set current user in App class
            com.example.App.setCurrentUser(user);

            // Auto-play queue if not empty
            com.example.services.MediaPlayerService mediaPlayerService = com.example.App.getMediaPlayerServiceStatic();
            if (mediaPlayerService != null && mediaPlayerService.getQueue() != null && !mediaPlayerService.getQueue().isEmpty()) {
                com.example.App.togglePlayPause();
            }

            // Navigate to home page
            com.example.App.setRoot("main"); // Change this to load the new main frame
        } catch (Exception e) {
            e.printStackTrace();
            // Removed error message display
        }
    }

    private void navigateToAdmin(Document user) {
        try {
            // Set current user in App class
            com.example.App.setCurrentUser(user);

            // Navigate to admin panel
            com.example.App.setRoot("admin");
        } catch (Exception e) {
            e.printStackTrace();
            // Removed error message display
        }
    }

    private void setupEnterKeyHandler() {
        // Allow Enter key to trigger login
        usernameField.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                passwordField.requestFocus();
            }
        });

        passwordField.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                handleLogin();
            }
        });
    }

    private void saveRememberedCredentials(String username, String password) {
        try {
            File file = new File(REMEMBER_FILE);
            try (FileOutputStream fos = new FileOutputStream(file);
                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {

                // Encode credentials for basic security
                String encodedUsername = Base64.getEncoder().encodeToString(username.getBytes());
                String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());

                oos.writeObject(encodedUsername);
                oos.writeObject(encodedPassword);
                System.out.println("Credentials saved for remember me functionality");
            }
        } catch (IOException e) {
            System.err.println("Error saving remembered credentials: " + e.getMessage());
        }
    }

    private void loadRememberedCredentials() {
        try {
            File file = new File(REMEMBER_FILE);
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file);
                     ObjectInputStream ois = new ObjectInputStream(fis)) {

                    String encodedUsername = (String) ois.readObject();
                    String encodedPassword = (String) ois.readObject();

                    // Decode credentials
                    String username = new String(Base64.getDecoder().decode(encodedUsername));
                    String password = new String(Base64.getDecoder().decode(encodedPassword));

                    usernameField.setText(username);
                    passwordField.setText(password);
                    rememberMeCheckBox.setSelected(true);
                    System.out.println("Remembered credentials loaded");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading remembered credentials: " + e.getMessage());
            // If there's an error loading, clear the file
            clearRememberedCredentials();
        }
    }

    private void clearRememberedCredentials() {
        try {
            File file = new File(REMEMBER_FILE);
            if (file.exists()) {
                file.delete();
                System.out.println("Remembered credentials cleared");
            }
        } catch (Exception e) {
            System.err.println("Error clearing remembered credentials: " + e.getMessage());
        }
    }
}
