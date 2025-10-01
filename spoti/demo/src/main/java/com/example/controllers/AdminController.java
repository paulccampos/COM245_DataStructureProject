package com.example.controllers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;
import com.example.models.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class AdminController implements Initializable {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TableColumn<User, String> createdAtColumn;
    @FXML private TableColumn<User, String> updatedAtColumn;
    @FXML private TextField selectedUserField;

    // Edit fields
    @FXML private TextField usernameEditField;
    @FXML private TextField emailEditField;
    @FXML private TextField statusEditField;
    @FXML private TextField passwordEditField;

    private MongoService mongoService;
    private ObservableList<User> users;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        mongoService = new MongoService();
        users = FXCollections.observableArrayList();
        setupTable();
        loadUsers();
    }

    private void setupTable() {
        usernameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUsername()));
        emailColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
        statusColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));
        createdAtColumn.setCellValueFactory(cellData -> {
            java.util.Date date = cellData.getValue().getCreatedAt();
            if (date != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return new javafx.beans.property.SimpleStringProperty(sdf.format(date));
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        updatedAtColumn.setCellValueFactory(cellData -> {
            java.util.Date date = cellData.getValue().getUpdatedAt();
            if (date != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return new javafx.beans.property.SimpleStringProperty(sdf.format(date));
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        userTable.setItems(users);
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedUserField.setText(newSelection.getUsername());
                // Populate edit fields
                usernameEditField.setText(newSelection.getUsername());
                emailEditField.setText(newSelection.getEmail());
                statusEditField.setText(newSelection.getStatus());
                passwordEditField.setText(newSelection.getPassword());
            }
        });
    }

    private void loadUsers() {
        List<Document> userList = mongoService.getAllUsers();
        List<User> userObjects = userList.stream().map(doc -> new User(doc)).collect(Collectors.toList());
        users.clear();
        users.addAll(userObjects);
    }

    @FXML
    private void onCreateUser() {
        try {
            App.setRoot("signup");
        } catch (IOException e) {
            showError("Error loading create user page");
        }
    }

    @FXML
    private void onUpdateUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showError("Please select a user to update");
            return;
        }

        // Update all fields from the edit fields
        String newUsername = usernameEditField.getText();
        String newEmail = emailEditField.getText();
        String newStatus = statusEditField.getText();
        String newPassword = passwordEditField.getText();

        if (newUsername.isEmpty() || newEmail.isEmpty() || newStatus.isEmpty() || newPassword.isEmpty()) {
            showError("Please fill all fields");
            return;
        }

        // Update properties
        mongoService.updateUserProperty(selectedUser.getUsername(), "username", newUsername);
        mongoService.updateUserProperty(newUsername, "email", newEmail);
        mongoService.updateUserProperty(newUsername, "status", newStatus);
        mongoService.updateUserProperty(newUsername, "password", newPassword);

        showInfo("User updated successfully");
        loadUsers();
    }

    @FXML
    private void onDeleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showError("Please select a user to delete");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Are you sure you want to delete this user?");
        alert.setContentText("Username: " + selectedUser.getUsername());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String username = selectedUser.getUsername();
                boolean deleted = mongoService.deleteUser(username);
                if (deleted) {
                    loadUsers(); // Refresh the table
                    selectedUserField.clear();
                    showInfo("User deleted successfully");
                } else {
                    showError("Failed to delete user");
                }
            }
        });
    }

    @FXML
    private void onUpdateUsername() {
        String oldUsername = selectedUserField.getText();
        String newUsername = usernameEditField.getText();
        if (oldUsername.isEmpty() || newUsername.isEmpty()) {
            showError("Please select a user and enter new username");
            return;
        }
        if (mongoService.updateUserProperty(oldUsername, "username", newUsername)) {
            showInfo("Username updated successfully");
            loadUsers();
        } else {
            showError("Failed to update username");
        }
    }

    @FXML
    private void onUpdateEmail() {
        String username = selectedUserField.getText();
        String newEmail = emailEditField.getText();
        if (username.isEmpty() || newEmail.isEmpty()) {
            showError("Please select a user and enter new email");
            return;
        }
        if (mongoService.updateUserProperty(username, "email", newEmail)) {
            showInfo("Email updated successfully");
            loadUsers();
        } else {
            showError("Failed to update email");
        }
    }

    @FXML
    private void onUpdateStatus() {
        String username = selectedUserField.getText();
        String newStatus = statusEditField.getText();
        if (username.isEmpty() || newStatus.isEmpty()) {
            showError("Please select a user and enter new status");
            return;
        }
        if (mongoService.updateUserProperty(username, "status", newStatus)) {
            showInfo("Status updated successfully");
            loadUsers();
        } else {
            showError("Failed to update status");
        }
    }

    @FXML
    private void onUpdatePassword() {
        String username = selectedUserField.getText();
        String newPassword = passwordEditField.getText();
        if (username.isEmpty() || newPassword.isEmpty()) {
            showError("Please select a user and enter new password");
            return;
        }
        if (mongoService.updateUserProperty(username, "password", newPassword)) {
            showInfo("Password updated successfully");
            loadUsers();
        } else {
            showError("Failed to update password");
        }
    }

    @FXML
    private void onBackToLogin() {
        try {
            App.setRoot("login");
        } catch (IOException e) {
            showError("Error loading login page");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
