package com.example.controllers;

import java.io.IOException;
import java.util.Comparator;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;


public class AdminController implements Initializable {

    @FXML private javafx.scene.layout.VBox usersVBox;
    @FXML private TextField selectedUserField;

    // Edit fields
    @FXML private TextField usernameEditField;
    @FXML private TextField emailEditField;
    @FXML private TextField statusEditField;
    @FXML private TextField passwordEditField;

    // Filter controls
    @FXML private ComboBox<String> sortComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;

    private MongoService mongoService;
    private ObservableList<User> users;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        try {
            mongoService = new MongoService();
            users = FXCollections.observableArrayList();
            setupFiltering();
            loadUsers();
        } catch (Exception e) {
            showError("Failed to connect to database. Admin panel may not function properly.");
            users = FXCollections.observableArrayList();
        }
    }

    private void setupFiltering() {
        statusFilterComboBox.getItems().addAll("All", "Admin", "User");
        statusFilterComboBox.setValue("All");
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> loadUsers());

        sortComboBox.getItems().addAll("Name", "Email", "Date Added");
        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> loadUsers());
    }

    private void loadUsers() {
        List<Document> userDocs = mongoService.getAllUsers();
        List<User> userList = userDocs.stream().map(User::new).collect(Collectors.toList());

        // Filter by status
        String statusFilter = statusFilterComboBox.getValue();
        if (statusFilter != null && !"All".equals(statusFilter)) {
            userList.removeIf(user -> !user.getStatus().equalsIgnoreCase(statusFilter));
        }

        // Sort
        String sortKey = sortComboBox.getValue();
        if ("Name".equals(sortKey)) {
            quickSort(userList, 0, userList.size() - 1, "username");
        } else if ("Email".equals(sortKey)) {
            quickSort(userList, 0, userList.size() - 1, "email");
        } else if ("Date Added".equals(sortKey)) {
            userList.sort(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        usersVBox.getChildren().clear();
        users.setAll(userList); // Update observable list for selection consistency

        // Add header
        javafx.scene.layout.HBox headerBox = new javafx.scene.layout.HBox(10);
        headerBox.setStyle("-fx-padding: 5; -fx-border-color: -spawtify-subtle-text-color; -fx-border-width: 0 0 1 0;");
        Label userHeader = new Label("Username");
        userHeader.setPrefWidth(150);
        Label emailHeader = new Label("Email");
        emailHeader.setPrefWidth(200);
        Label statusHeader = new Label("Status");
        statusHeader.setPrefWidth(100);
        Label createdHeader = new Label("Date Added");
        createdHeader.setPrefWidth(150);
        headerBox.getChildren().addAll(userHeader, emailHeader, statusHeader, createdHeader);
        usersVBox.getChildren().add(headerBox);

        // Add user rows
        for (User user : userList) {
            javafx.scene.layout.HBox userBox = createUserBox(user);
            usersVBox.getChildren().add(userBox);
        }
    }

    private javafx.scene.layout.HBox createUserBox(User user) {
        javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(10);
        hbox.setStyle("-fx-padding: 8; -fx-cursor: hand;");
        hbox.setOnMouseClicked(e -> {
            selectedUserField.setText(user.getUsername());
            usernameEditField.setText(user.getUsername());
            emailEditField.setText(user.getEmail());
            statusEditField.setText(user.getStatus());
            passwordEditField.setText(user.getPassword());
        });

        Label usernameLabel = new Label(user.getUsername());
        usernameLabel.setPrefWidth(150);
        Label emailLabel = new Label(user.getEmail());
        emailLabel.setPrefWidth(200);
        Label statusLabel = new Label(user.getStatus());
        statusLabel.setPrefWidth(100);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        String createdAt = user.getCreatedAt() != null ? sdf.format(user.getCreatedAt()) : "N/A";
        Label createdAtLabel = new Label(createdAt);
        createdAtLabel.setPrefWidth(150);

        hbox.getChildren().addAll(usernameLabel, emailLabel, statusLabel, createdAtLabel);
        return hbox;
    }

    private void quickSort(List<User> list, int low, int high, String key) {
        if (low < high) {
            int pi = partition(list, low, high, key);
            quickSort(list, low, pi - 1, key);
            quickSort(list, pi + 1, high, key);
        }
    }

    private int partition(List<User> list, int low, int high, String key) {
        String pivot = getUserProperty(list.get(high), key);
        int i = (low - 1); // index of smaller element
        for (int j = low; j < high; j++) {
            if (getUserProperty(list.get(j), key).compareToIgnoreCase(pivot) <= 0) {
                i++;
                // swap arr[i] and arr[j]
                User temp = list.get(i);
                list.set(i, list.get(j));
                list.set(j, temp);
            }
        }

        // swap arr[i+1] and arr[high] (or pivot)
        User temp = list.get(i + 1);
        list.set(i + 1, list.get(high));
        list.set(high, temp);

        return i + 1;
    }

    private String getUserProperty(User user, String key) {
        if ("username".equals(key)) {
            return user.getUsername();
        } else if ("email".equals(key)) {
            return user.getEmail();
        }
        return ""; // Default case
    }

    private void clearEditFields() {
        usernameEditField.clear();
        emailEditField.clear();
        statusEditField.clear();
        passwordEditField.clear();
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
        String selectedUsername = selectedUserField.getText();
        if (selectedUsername == null || selectedUsername.isEmpty()) {
            showError("Please select a user to update");
            return;
        }
        
        // Find the user object to get the original username
        User selectedUser = users.stream().filter(u -> u.getUsername().equals(selectedUsername)).findFirst().orElse(null);

        // Update all fields from the edit fields
        String newUsername = usernameEditField.getText();
        String newEmail = emailEditField.getText();
        String newStatus = statusEditField.getText();
        String newPassword = passwordEditField.getText();

        if (newUsername.isEmpty() || newEmail.isEmpty() || newStatus.isEmpty() || newPassword.isEmpty()) {
            showError("Please fill all fields");
            return;
        }

        String originalUsername = selectedUser.getUsername();
        // Update properties
        mongoService.updateUserProperty(originalUsername, "username", newUsername);
        mongoService.updateUserProperty(newUsername, "email", newEmail); // Use new username for subsequent updates
        mongoService.updateUserProperty(newUsername, "status", newStatus);
        mongoService.updateUserProperty(newUsername, "password", newPassword);

        showInfo("User '" + originalUsername + "' updated successfully.");
        loadUsers();
    }

    @FXML
    private void onDeleteUser() {
        String selectedUsername = selectedUserField.getText();
        if (selectedUsername == null || selectedUsername.isEmpty()) {
            showError("Please select a user to delete");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Are you sure you want to delete this user?");
        alert.setContentText("Username: " + selectedUsername);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean deleted = mongoService.deleteUser(selectedUsername);
                if (deleted) {
                    loadUsers(); // Refresh the list
                    selectedUserField.clear();
                    clearEditFields();
                    showInfo("User deleted successfully");
                } else {
                    showError("Failed to delete user");
                }
            }
        });
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
