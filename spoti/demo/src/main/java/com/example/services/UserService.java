package com.example.services;

import org.bson.Document;

import com.example.MongoService;
import com.example.models.User;
public class UserService {
    private MongoService mongoService;
    private User currentUser;

    public UserService(MongoService mongoService) {
        this.mongoService = mongoService;
    }

    public boolean createUser(String username, String email, String password) {
        return mongoService.createUser(username, email, password);
    }

    public User authenticateUser(String username, String password) {
        Document userDoc = mongoService.authenticateUser(username, password);
        if (userDoc != null) {
            currentUser = new User(userDoc);
            return currentUser;
        }
        return null;
    }

    public User getUser(String username) {
        Document userDoc = mongoService.getUser(username);
        if (userDoc != null) {
            return new User(userDoc);
        }
        return null;
    }

    public boolean updateUserPassword(String username, String newPassword) {
        return mongoService.updateUserPassword(username, newPassword);
    }

    public boolean updateUserStatus(String username, String newStatus) {
        return mongoService.updateUserStatus(username, newStatus);
    }

    public boolean deleteUser(String username) {
        return mongoService.deleteUser(username);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    public boolean isUserLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdminUser() {
        return currentUser != null && "admin".equals(currentUser.getStatus());
    }

    public void updateCurrentUsername(String newUsername) {
        if (currentUser != null) {
            currentUser.setUsername(newUsername);
        }
    }

    public void logout() {
        currentUser = null;
    }
}
