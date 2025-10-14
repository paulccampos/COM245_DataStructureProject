package com.example.models;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;

/**
 * Model class representing a Song.
 * Encapsulates song data and provides methods for accessing properties.
 */
public class Song {
    private String title;
    private String artist;
    private String album;
    private String duration;
    private String file;
    private String imagePath;
    private int globalPlayCount;
    private Map<String, Integer> userPlayCounts; // username -> count
    private Object genre; // Can be String or List<String>
    private Object vibe;  // Can be String or List<String>
    private boolean isManualAddition;
    private int priority;

    public Song(Document doc) {
        this.title = doc.getString("title");
        this.artist = doc.getString("artist");
        this.album = doc.getString("album");

        // Handle duration - can be String or Integer
        Object durationObj = doc.get("duration");
        if (durationObj instanceof String) {
            this.duration = (String) durationObj;
        } else if (durationObj instanceof Integer) {
            int seconds = (Integer) durationObj;
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            this.duration = String.format("%d:%02d", minutes, remainingSeconds);
        } else {
            this.duration = "3:45"; // Default duration
        }

        this.file = doc.getString("file");
        this.imagePath = doc.getString("imagePath");
        this.globalPlayCount = doc.getInteger("play_count", 0);
        Object userCountsObj = doc.get("userPlayCounts");
        if (userCountsObj instanceof Map) {
            this.userPlayCounts = (Map<String, Integer>) userCountsObj;
        } else {
            this.userPlayCounts = new HashMap<>();
        }
        this.genre = doc.get("genre");
        this.vibe = doc.get("vibe");
    }

    // Getters
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getDuration() { return duration; }
    public String getFile() { return file; }
    public String getImagePath() { return imagePath; }
    public int getGlobalPlayCount() { return globalPlayCount; }
    public Map<String, Integer> getUserPlayCounts() { return userPlayCounts; }
    public int getUserPlayCount(String username) { return userPlayCounts.getOrDefault(username, 0); }
    public Object getGenre() { return genre; }
    public Object getVibe() { return vibe; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setAlbum(String album) { this.album = album; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setFile(String file) { this.file = file; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setGlobalPlayCount(int globalPlayCount) { this.globalPlayCount = globalPlayCount; }
    public void setUserPlayCounts(Map<String, Integer> userPlayCounts) { this.userPlayCounts = userPlayCounts; }
    public void setGenre(Object genre) { this.genre = genre; }
    public void setVibe(Object vibe) { this.vibe = vibe; }

    // Increment methods
    public void incrementGlobalPlay() { this.globalPlayCount++; }
    public void incrementUserPlay(String username) {
        userPlayCounts.put(username, userPlayCounts.getOrDefault(username, 0) + 1);
    }

    // New getters and setters for manual addition support
    public boolean isManualAddition() { return isManualAddition; }
    public void setManualAddition(boolean manualAddition) { isManualAddition = manualAddition; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    @Override
    public String toString() {
        return "Song{" +
                "title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", duration='" + duration + '\'' +
                ", globalPlayCount=" + globalPlayCount +
                '}';
    }
}
