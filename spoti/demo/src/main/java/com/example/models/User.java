package com.example.models;

import java.util.List;

import org.bson.Document;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model class representing a User.
 * Encapsulates user data and provides methods for accessing properties.
 */
public class User {
    private StringProperty usernameProperty;
    private StringProperty emailProperty;
    private StringProperty passwordProperty;
    private StringProperty statusProperty;
    private java.util.Date createdAt;
    private java.util.Date updatedAt;
    private List<Playlist> playlists;
    private List<Song> favoritePodcasts;

    public User(Document doc) {
        this.usernameProperty = new SimpleStringProperty(doc.getString("username"));
        this.emailProperty = new SimpleStringProperty(doc.getString("email"));
        this.passwordProperty = new SimpleStringProperty(doc.getString("password"));
        this.statusProperty = new SimpleStringProperty(doc.getString("status"));
        this.createdAt = doc.getDate("created_date");
        this.updatedAt = doc.getDate("updated_at");
        // Assuming playlists are stored as List<Document>
        List<Document> playlistDocs = (List<Document>) doc.get("playlists");
        if (playlistDocs != null) {
            this.playlists = new java.util.ArrayList<>();
            for (Document playlistDoc : playlistDocs) {
                this.playlists.add(new Playlist(playlistDoc));
            }
        } else {
            this.playlists = new java.util.ArrayList<>();
        }
        // Assuming favorite_podcasts are stored as List<Document>
        List<Document> podcastDocs = (List<Document>) doc.get("favorite_podcasts");
        if (podcastDocs != null) {
            this.favoritePodcasts = new java.util.ArrayList<>();
            for (Document podcastDoc : podcastDocs) {
                // Assuming Song can represent podcasts for simplicity
                this.favoritePodcasts.add(new Song(podcastDoc));
            }
        } else {
            this.favoritePodcasts = new java.util.ArrayList<>();
        }
    }

    // Getters for properties
    public StringProperty getUsernameProperty() { return usernameProperty; }
    public StringProperty getEmailProperty() { return emailProperty; }
    public StringProperty getPasswordProperty() { return passwordProperty; }
    public StringProperty getStatusProperty() { return statusProperty; }

    // Getters for values
    public String getUsername() { return usernameProperty.get(); }
    public String getEmail() { return emailProperty.get(); }
    public String getPassword() { return passwordProperty.get(); }
    public String getStatus() { return statusProperty.get(); }
    public java.util.Date getCreatedAt() { return createdAt; }
    public java.util.Date getUpdatedAt() { return updatedAt; }
    public List<Playlist> getPlaylists() { return playlists; }
    public List<Song> getFavoritePodcasts() { return favoritePodcasts; }

    // Setters
    public void setUsername(String username) { this.usernameProperty.set(username); }
    public void setEmail(String email) { this.emailProperty.set(email); }
    public void setPassword(String password) { this.passwordProperty.set(password); }
    public void setStatus(String status) { this.statusProperty.set(status); }
    public void setCreatedAt(java.util.Date createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(java.util.Date updatedAt) { this.updatedAt = updatedAt; }
    public void setPlaylists(List<Playlist> playlists) { this.playlists = playlists; }
    public void setFavoritePodcasts(List<Song> favoritePodcasts) { this.favoritePodcasts = favoritePodcasts; }

    public Document getDocument() {
        Document doc = new Document();
        doc.append("username", getUsername());
        doc.append("email", getEmail());
        doc.append("password", getPassword());
        doc.append("status", getStatus());
        doc.append("created_date", createdAt);
        doc.append("updated_at", updatedAt);
        doc.append("playlists", playlists);
        doc.append("favorite_podcasts", favoritePodcasts);
        return doc;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", status='" + getStatus() + '\'' +
                '}';
    }
}
