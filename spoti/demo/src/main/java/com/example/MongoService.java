package com.example;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoService {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "songs"; // change if your DB name is different

    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoService() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        database = mongoClient.getDatabase(DATABASE_NAME);
    }

    // ✅ Function to print all songs from the collection
    public void displaySongsInTerminal() {
        MongoCollection<Document> collection = database.getCollection("songs");
        System.out.println("=== Songs Collection ===");

        for (Document doc : collection.find()) {
            // Extract song details from each document
            String title = doc.getString("title"); // changed from "name" to "title"
            String artist = doc.getString("artist");
            String album = doc.getString("album");
            System.out.println("Song: " + title + " | Artist: " + artist + " | Album: " + album);
        }

        System.out.println("========================");
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void close() {
        mongoClient.close();
    }

    // Method to get all playlists
    public List<Document> getPlaylists() {
        MongoCollection<Document> collection = database.getCollection("playlists");
        return collection.find().into(new ArrayList<>());
    }

    // Method to get songs for a specific playlist by title
    public List<Document> getSongsForPlaylist(String playlistTitle) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document playlist = collection.find(new Document("title", playlistTitle)).first();
        if (playlist != null) {
            return (List<Document>) playlist.get("songs");
        }
        return new ArrayList<>();
    }

    // Method to get all songs
    public List<Document> getAllSongs() {
        MongoCollection<Document> collection = database.getCollection("songs");
        return collection.find().into(new ArrayList<>());
    }

    // Method to add a song to a playlist
    public void addSongToPlaylist(String playlistTitle, Document song) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document playlist = collection.find(new Document("title", playlistTitle)).first();
        if (playlist != null) {
            List<Document> songs = (List<Document>) playlist.get("songs");
            if (songs == null) {
                songs = new ArrayList<>();
            }
            songs.add(song);
            collection.updateOne(new Document("title", playlistTitle), new Document("$set", new Document("songs", songs)));
        }
    }

    // Method to create a new playlist
    public void createPlaylist(String title, String description) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document playlist = new Document("title", title).append("description", description).append("songs", new ArrayList<Document>());
        collection.insertOne(playlist);
    }

    // Method to initialize Top Hits playlist with all songs
    public void initializeTopHitsPlaylist() {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document existing = collection.find(new Document("title", "Top Hits")).first();
        if (existing == null) {
            createPlaylist("Top Hits", "A collection of top hit songs");
        }
        // Add all songs to Top Hits
        List<Document> allSongs = getAllSongs();
        for (Document song : allSongs) {
            addSongToPlaylist("Top Hits", song);
        }
    }

    // Method to delete a playlist
    public void deletePlaylist(String title) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        collection.deleteOne(new Document("title", title));
    }

    // Method to update a playlist's name and description
    public void updatePlaylist(String oldTitle, String newTitle, String newDescription) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document update = new Document("$set", new Document("title", newTitle).append("description", newDescription));
        collection.updateOne(new Document("title", oldTitle), update);
    }

    // Method to delete a song from a playlist
    public void deleteSongFromPlaylist(String playlistTitle, Document song) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document playlist = collection.find(new Document("title", playlistTitle)).first();
        if (playlist != null) {
            List<Document> songs = (List<Document>) playlist.get("songs");
            if (songs != null) {
                songs.removeIf(s -> s.getString("title").equals(song.getString("title")) &&
                                   s.getString("artist").equals(song.getString("artist")));
                collection.updateOne(new Document("title", playlistTitle), new Document("$set", new Document("songs", songs)));
            }
        }
    }

    // Queue management methods
    public void addToQueue(Document song) {
        MongoCollection<Document> collection = database.getCollection("queue");
        // Create a new document without _id to allow duplicates in queue
        Document queueSong = new Document(song);
        queueSong.remove("_id"); // Remove _id so MongoDB generates a new unique one
        collection.insertOne(queueSong);
    }

    public List<Document> getQueue() {
        MongoCollection<Document> collection = database.getCollection("queue");
        return collection.find().into(new ArrayList<>());
    }

    public void removeFromQueue(Document song) {
        MongoCollection<Document> collection = database.getCollection("queue");
        collection.deleteOne(song);
    }

    public void clearQueue() {
        MongoCollection<Document> collection = database.getCollection("queue");
        collection.deleteMany(new Document());
    }

    // Method to search songs by title or artist
    public List<Document> searchSongs(String query) {
        MongoCollection<Document> collection = database.getCollection("songs");
        Document regexQuery = new Document("$or", List.of(
            new Document("title", new Document("$regex", query).append("$options", "i")),
            new Document("artist", new Document("$regex", query).append("$options", "i"))
        ));
        return collection.find(regexQuery).into(new ArrayList<>());
    }
}
