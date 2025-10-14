package com.example.models;

import java.util.List;

import org.bson.Document;

/**
 * Model class representing a Playlist.
 * Encapsulates playlist data and provides methods for accessing properties.
 */
public class Playlist {
    private String title;
    private String description;
    private String createdBy;
    private List<Song> songs;

    public Playlist(Document doc) {
        this.title = doc.getString("title");
        this.description = doc.getString("description");
        this.createdBy = doc.getString("createdBy");
        // Assuming songs are stored as List<Document>, convert to List<Song>
        List<Document> songDocs = (List<Document>) doc.get("songs");
        if (songDocs != null) {
            this.songs = new java.util.ArrayList<>();
            for (Document songDoc : songDocs) {
                this.songs.add(new Song(songDoc));
            }
        } else {
            this.songs = new java.util.ArrayList<>();
        }
    }

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCreatedBy() { return createdBy; }
    public List<Song> getSongs() { return songs; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setSongs(List<Song> songs) { this.songs = songs; }

    @Override
    public String toString() {
        return "Playlist{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", songCount=" + songs.size() +
                '}';
    }
}
