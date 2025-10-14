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

        if (username.isEmpty() || password.isEmpty()) {
            return;
        }

        Document user = mongoService.authenticateUser(username, password);

        if (user != null) {
            System.out.println("Login successful for user: " + username);
            String userStatus = user.getString("status");

            if (rememberMeCheckBox.isSelected()) {
                saveRememberedCredentials(username, password);
            } else {
                clearRememberedCredentials();
            }

            if ("admin".equals(userStatus)) {
                showAdminChoiceDialog(user);
            } else {
                navigateToHome(user);
            }
        } else {
        }
    }

    @FXML
    private void handleSignUp() {
        try {
            com.example.App.setRoot("signup");
        } catch (Exception e) {
            e.printStackTrace();
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
        });
    }

    private void navigateToHome(Document user) {
        try {
            com.example.App.setCurrentUser(user);

            com.example.services.MediaPlayerService mediaPlayerService = com.example.App.getMediaPlayerServiceStatic();
            if (mediaPlayerService != null && mediaPlayerService.getQueue() != null && !mediaPlayerService.getQueue().isEmpty()) {
                com.example.App.togglePlayPause();
            }

            com.example.App.setRoot("main");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToAdmin(Document user) {
        try {
            com.example.App.setCurrentUser(user);

            com.example.App.setRoot("admin");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupEnterKeyHandler() {
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
