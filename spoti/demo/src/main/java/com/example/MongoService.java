package com.example;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoService {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "songs"; // change if your DB name is different

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> likedPlaylistsCollection;

    public MongoService() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        database = mongoClient.getDatabase(DATABASE_NAME);
        likedPlaylistsCollection = database.getCollection("liked_playlists");
        migrateSongs();
        migratePlaylists();
        insertSampleSongsIfEmpty();
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

    // Method to get all playlists for a user
    public List<Document> getPlaylists(String username) {
        return getPlaylistsForUser(username);
    }

    // Method to get songs for a specific playlist by title and user
    public List<Document> getSongsForPlaylist(String playlistTitle, String username) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document playlist = collection.find(new Document("title", playlistTitle).append("createdBy", username)).first();
        if (playlist == null) {
            // Try to find admin playlist
            playlist = collection.find(new Document("title", playlistTitle).append("createdBy", "admin")).first();
        }
        if (playlist != null) {
            List<Document> songs = (List<Document>) playlist.get("songs");
            if (songs != null) {
                return songs;
            }
        }
        return new ArrayList<>();
    }

    public Document getPlaylist(String title, String username) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document playlist = collection.find(new Document("title", title).append("createdBy", username)).first();
        if (playlist == null) {
            // Try to find admin playlist
            playlist = collection.find(new Document("title", title).append("createdBy", "admin")).first();
        }
        return playlist;
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
            songs.add(0, song); // Add manually added songs at the beginning to prioritize them
            collection.updateOne(new Document("title", playlistTitle), new Document("$set", new Document("songs", songs)));
        }
    }

    // Method to add a song to a playlist for a specific user
    public void addSongToPlaylist(String playlistTitle, Document song, String username) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document playlist = collection.find(new Document("title", playlistTitle).append("createdBy", username)).first();
        if (playlist != null) {
            List<Document> songs = (List<Document>) playlist.get("songs");
            if (songs == null) {
                songs = new ArrayList<>();
            }
            songs.add(0, song); // Add manually added songs at the beginning to prioritize them
            collection.updateOne(new Document("title", playlistTitle).append("createdBy", username), new Document("$set", new Document("songs", songs)));
        }
    }

    // Method to create a new playlist
    public void createPlaylist(String title, String description) {
        createPlaylist(title, description, "admin");
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

    // Method to delete a playlist for a specific user
    public void deletePlaylist(String title, String username) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        collection.deleteOne(new Document("title", title).append("createdBy", username));
    }

    // Method to update a playlist's name and description
    public void updatePlaylist(String oldTitle, String newTitle, String newDescription) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document update = new Document("$set", new Document("title", newTitle).append("description", newDescription));
        collection.updateOne(new Document("title", oldTitle), update);
    }

    // Method to update a playlist's name and description for a specific user
    public void updatePlaylist(String oldTitle, String newTitle, String newDescription, String username) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document update = new Document("$set", new Document("title", newTitle).append("description", newDescription));
        collection.updateOne(new Document("title", oldTitle).append("createdBy", username), update);
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

    // Method to delete a song from a playlist for a specific user
    public void deleteSongFromPlaylist(String playlistTitle, Document song, String username) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document playlist = collection.find(new Document("title", playlistTitle).append("createdBy", username)).first();
        if (playlist != null) {
            List<Document> songs = (List<Document>) playlist.get("songs");
            if (songs != null) {
                songs.removeIf(s -> s.getString("title").equals(song.getString("title")) &&
                                   s.getString("artist").equals(song.getString("artist")));
                collection.updateOne(new Document("title", playlistTitle).append("createdBy", username), new Document("$set", new Document("songs", songs)));
            }
        }
    }

    // Method to check if a song is already in a playlist
    public boolean isSongInPlaylist(String playlistTitle, Document song) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document playlist = collection.find(new Document("title", playlistTitle)).first();
        if (playlist != null) {
            List<Document> songs = (List<Document>) playlist.get("songs");
            if (songs != null) {
                for (Document s : songs) {
                    if (s.getString("title").equals(song.getString("title")) &&
                        s.getString("artist").equals(song.getString("artist"))) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    // Method to increment play count for a song
    public void incrementPlayCount(Document song) {
        try {
            MongoCollection<Document> collection = database.getCollection("songs");
            String title = song.getString("title");
            String artist = song.getString("artist");

            // Find the song by title and artist to get its current play count
            Document existingSong = collection.find(new Document("title", title).append("artist", artist)).first();
            if (existingSong != null) {
                Integer currentPlayCount = existingSong.getInteger("play_count", 0);
                int newPlayCount = currentPlayCount + 1;

                // Update the play count in the database
                collection.updateOne(
                    new Document("title", title).append("artist", artist),
                    new Document("$set", new Document("play_count", newPlayCount))
                );

                System.out.println("Play count incremented for: " + title + " by " + artist + " (now: " + newPlayCount + ")");
            }
        } catch (Exception e) {
            System.err.println("Error incrementing play count: " + e.getMessage());
        }
    }

    // Method to get most played songs
    public List<Document> getMostPlayedSongs(int limit) {
        MongoCollection<Document> collection = database.getCollection("songs");
        return collection.find()
            .sort(new Document("play_count", -1))
            .limit(limit)
            .into(new ArrayList<>());
    }

    // ===== USER AUTHENTICATION METHODS =====

    // Method to create a new user account
    public boolean createUser(String username, String email, String password) {
        try {
            MongoCollection<Document> collection = database.getCollection("users");

            // Check if username already exists
            Document existingUser = collection.find(new Document("username", username)).first();
            if (existingUser != null) {
                return false; // Username already exists
            }

            // Create new user document
            Document newUser = new Document("username", username)
                    .append("email", email)
                    .append("password", password) // In real app, this should be hashed
                    .append("status", "user") // Default status is "user"
                    .append("created_date", new java.util.Date())
                    .append("playlists", new ArrayList<Document>());

            collection.insertOne(newUser);
            System.out.println("User created successfully: " + username);
            return true;
        } catch (Exception e) {
            System.err.println("Error creating user: " + e.getMessage());
            return false;
        }
    }

    // Method to authenticate user login
    public Document authenticateUser(String username, String password) {
        try {
            MongoCollection<Document> collection = database.getCollection("users");
            Document user = collection.find(new Document("username", username)
                    .append("password", password)).first();

            if (user != null) {
                System.out.println("User authenticated successfully: " + username);
                return user;
            } else {
                System.out.println("Authentication failed for user: " + username);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error during authentication: " + e.getMessage());
            return null;
        }
    }

    // Method to get user by username
    public Document getUser(String username) {
        try {
            MongoCollection<Document> collection = database.getCollection("users");
            return collection.find(new Document("username", username)).first();
        } catch (Exception e) {
            System.err.println("Error getting user: " + e.getMessage());
            return null;
        }
    }

    // Method to update user password
    public boolean updateUserPassword(String username, String newPassword) {
        try {
            MongoCollection<Document> collection = database.getCollection("users");
            collection.updateOne(
                new Document("username", username),
                new Document("$set", new Document("password", newPassword))
            );
            System.out.println("Password updated for user: " + username);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating password: " + e.getMessage());
            return false;
        }
    }

    // Method to update user status (admin/user)
    public boolean updateUserStatus(String username, String newStatus) {
        try {
            MongoCollection<Document> collection = database.getCollection("users");
            collection.updateOne(
                new Document("username", username),
                new Document("$set", new Document("status", newStatus))
            );
            System.out.println("Status updated for user: " + username + " to " + newStatus);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating user status: " + e.getMessage());
            return false;
        }
    }

    // Method to get all users (for admin)
    public List<Document> getAllUsers() {
        try {
            MongoCollection<Document> collection = database.getCollection("users");
            return collection.find().into(new ArrayList<>());
        } catch (Exception e) {
            System.err.println("Error getting all users: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Method to delete user (for admin)
    public boolean deleteUser(String username) {
        try {
            MongoCollection<Document> collection = database.getCollection("users");
            collection.deleteOne(new Document("username", username));
            System.out.println("User deleted: " + username);
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting user: " + e.getMessage());
            return false;
        }
    }

    // Method to update a specific user property
    public boolean updateUserProperty(String username, String property, Object value) {
        try {
            MongoCollection<Document> collection = database.getCollection("users");
            Document setDoc = new Document(property, value)
                .append("updated_at", new java.util.Date());
            collection.updateOne(
                new Document("username", username),
                new Document("$set", setDoc)
            );
            System.out.println("Updated " + property + " for user: " + username + " at " + new java.util.Date());
            return true;
        } catch (Exception e) {
            System.err.println("Error updating user property: " + e.getMessage());
            return false;
        }
    }

    // ===== PODCAST METHODS =====

    // Method to get all podcasts
    public List<Document> getAllPodcasts() {
        try {
            MongoCollection<Document> collection = database.getCollection("podcasts");
            return collection.find().into(new ArrayList<>());
        } catch (Exception e) {
            System.err.println("Error getting podcasts: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Method to search podcasts by title or host
    public List<Document> searchPodcasts(String query) {
        try {
            MongoCollection<Document> collection = database.getCollection("podcasts");
            Document regexQuery = new Document("$or", List.of(
                new Document("title", new Document("$regex", query).append("$options", "i")),
                new Document("host", new Document("$regex", query).append("$options", "i"))
            ));
            return collection.find(regexQuery).into(new ArrayList<>());
        } catch (Exception e) {
            System.err.println("Error searching podcasts: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Method to add a podcast to user's favorites
    public void addPodcastToFavorites(String username, Document podcast) {
        try {
            MongoCollection<Document> collection = database.getCollection("users");
            Document user = collection.find(new Document("username", username)).first();
            if (user != null) {
                List<Document> favorites = (List<Document>) user.get("favorite_podcasts");
                if (favorites == null) {
                    favorites = new ArrayList<>();
                }
                favorites.add(podcast);
                collection.updateOne(
                    new Document("username", username),
                    new Document("$set", new Document("favorite_podcasts", favorites))
                );
            }
        } catch (Exception e) {
            System.err.println("Error adding podcast to favorites: " + e.getMessage());
        }
    }

    // Method to get user's favorite podcasts
    public List<Document> getUserFavoritePodcasts(String username) {
        try {
            MongoCollection<Document> collection = database.getCollection("users");
            Document user = collection.find(new Document("username", username)).first();
            if (user != null) {
                List<Document> favorites = (List<Document>) user.get("favorite_podcasts");
                return favorites != null ? favorites : new ArrayList<>();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error getting favorite podcasts: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Method to remove a podcast from user's favorites
    public void removePodcastFromFavorites(String username, Document podcast) {
        try {
            MongoCollection<Document> collection = database.getCollection("users");
            Document user = collection.find(new Document("username", username)).first();
            if (user != null) {
                List<Document> favorites = (List<Document>) user.get("favorite_podcasts");
                if (favorites != null) {
                    favorites.removeIf(p -> p.getString("title").equals(podcast.getString("title")));
                    collection.updateOne(
                        new Document("username", username),
                        new Document("$set", new Document("favorite_podcasts", favorites))
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error removing podcast from favorites: " + e.getMessage());
        }
    }

    // Method to get podcasts for a user (alias for getUserFavoritePodcasts)
    public List<Document> getPodcasts(String username) {
        return getUserFavoritePodcasts(username);
    }

    // Method to get top 3 most listened genres for a user
    public List<String> getTopGenresForUser(String username, int limit) {
        try {
            MongoCollection<Document> songsCollection = database.getCollection("songs");

            // Get all songs and their play counts, group by genre
            List<Document> allSongs = songsCollection.find().into(new ArrayList<>());

            // Create a map to count plays per genre
            java.util.Map<String, Integer> genrePlayCounts = new java.util.HashMap<>();

            for (Document song : allSongs) {
                Integer playCount = song.getInteger("play_count", 0);
                Object genreObj = song.get("genre");

                if (genreObj instanceof List) {
                    List<String> genres = (List<String>) genreObj;
                    for (String genre : genres) {
                        genrePlayCounts.put(genre, genrePlayCounts.getOrDefault(genre, 0) + playCount);
                    }
                } else if (genreObj instanceof String) {
                    String genre = (String) genreObj;
                    genrePlayCounts.put(genre, genrePlayCounts.getOrDefault(genre, 0) + playCount);
                }
            }

            // Sort genres by play count and return top N
            return genrePlayCounts.entrySet()
                .stream()
                .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(java.util.Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error getting top genres: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Method to update song properties (imagePath, globalPlayCount, userPlayCounts)
    public boolean updateSongProperty(String title, String artist, String property, Object value) {
        try {
            MongoCollection<Document> collection = database.getCollection("songs");
            collection.updateOne(
                new Document("title", title).append("artist", artist),
                new Document("$set", new Document(property, value))
            );
            System.out.println("Updated " + property + " for song: " + title + " by " + artist);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating song property: " + e.getMessage());
            return false;
        }
    }

    // Method to update user play counts for a song
    public boolean updateSongPlayCounts(String title, String artist, String username, int increment) {
        try {
            MongoCollection<Document> collection = database.getCollection("songs");
            Document song = collection.find(new Document("title", title).append("artist", artist)).first();
            if (song != null) {
                Document userPlayCounts = (Document) song.get("userPlayCounts");
                if (userPlayCounts == null) {
                    userPlayCounts = new Document();
                }
                int currentCount = userPlayCounts.getInteger(username, 0);
                userPlayCounts.put(username, currentCount + increment);
                collection.updateOne(
                    new Document("title", title).append("artist", artist),
                    new Document("$set", new Document("userPlayCounts", userPlayCounts))
                );
                System.out.println("Updated play count for user " + username + " on song: " + title + " by " + artist);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error updating song play counts: " + e.getMessage());
            return false;
        }
    }

    // Method to update user play counts for a song using ObjectId
    public boolean updateSongPlayCounts(ObjectId songId, String username, int increment) {
        try {
            MongoCollection<Document> collection = database.getCollection("songs");
            Document song = collection.find(new Document("_id", songId)).first();
            if (song != null) {
                Document userPlayCounts = (Document) song.get("userPlayCounts");
                if (userPlayCounts == null) {
                    userPlayCounts = new Document();
                }
                int currentCount = userPlayCounts.getInteger(username, 0);
                userPlayCounts.put(username, currentCount + increment);
                collection.updateOne(
                    new Document("_id", songId),
                    new Document("$set", new Document("userPlayCounts", userPlayCounts))
                );
                System.out.println("Updated play count for user " + username + " on song id: " + songId);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error updating song play counts: " + e.getMessage());
            return false;
        }
    }

    // Method to increment global play count for a song
    public boolean incrementGlobalPlayCount(ObjectId songId) {
        try {
            MongoCollection<Document> collection = database.getCollection("songs");
            Document song = collection.find(new Document("_id", songId)).first();
            if (song != null) {
                int currentCount = song.getInteger("globalPlayCount", 0);
                collection.updateOne(
                    new Document("_id", songId),
                    new Document("$set", new Document("globalPlayCount", currentCount + 1))
                );
                System.out.println("Incremented global play count for song id: " + songId);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error incrementing global play count: " + e.getMessage());
            return false;
        }
    }

    // Method to get top songs for a user based on userPlayCounts
    public List<Document> getTopSongsForUser(String username, int limit) {
        try {
            MongoCollection<Document> collection = database.getCollection("songs");
            List<Document> allSongs = collection.find().into(new ArrayList<>());

            // Sort songs by user's play count
            allSongs.sort((a, b) -> {
                Document aCounts = (Document) a.get("userPlayCounts");
                Document bCounts = (Document) b.get("userPlayCounts");
                int aCount = aCounts != null ? aCounts.getInteger(username, 0) : 0;
                int bCount = bCounts != null ? bCounts.getInteger(username, 0) : 0;
                return Integer.compare(bCount, aCount); // Descending
            });

            return allSongs.subList(0, Math.min(limit, allSongs.size()));
        } catch (Exception e) {
            System.err.println("Error getting top songs for user: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Method to create a new playlist with createdBy
    public void createPlaylist(String title, String description, String createdBy) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        Document playlist = new Document("title", title)
                .append("description", description)
                .append("songs", new ArrayList<Document>())
                .append("createdBy", createdBy);
        collection.insertOne(playlist);
    }

    // Method to get playlists for a specific user
    public List<Document> getPlaylistsForUser(String username) {
        MongoCollection<Document> collection = database.getCollection("playlists");
        return collection.find(new Document("createdBy", username)).into(new ArrayList<>());
    }

    // Method to get all playlists
    public List<Document> getAllPlaylists() {
        MongoCollection<Document> collection = database.getCollection("playlists");
        return collection.find().into(new ArrayList<>());
    }

    // One-time migration method to add imagePath, globalPlayCount, userPlayCounts to songs
    public void migrateSongs() {
        try {
            MongoCollection<Document> collection = database.getCollection("songs");
            List<Document> allSongs = collection.find().into(new ArrayList<>());

            for (Document song : allSongs) {
                String album = song.getString("album");
                String imagePath = "/Albums/" + (album != null ? album.replaceAll("[^a-zA-Z0-9]", "") : "default") + ".jpg";

                // Set imagePath if not present
                if (!song.containsKey("imagePath")) {
                    song.put("imagePath", imagePath);
                }

                // Set globalPlayCount if not present
                if (!song.containsKey("globalPlayCount")) {
                    song.put("globalPlayCount", song.getInteger("play_count", 0));
                }

                // Set userPlayCounts if not present
                if (!song.containsKey("userPlayCounts")) {
                    song.put("userPlayCounts", new Document());
                }

                collection.replaceOne(new Document("_id", song.get("_id")), song);
            }
            System.out.println("Migration completed for songs.");
        } catch (Exception e) {
            System.err.println("Error during song migration: " + e.getMessage());
        }
    }

    // One-time migration method to add createdBy to existing playlists
    public void migratePlaylists() {
        try {
            MongoCollection<Document> collection = database.getCollection("playlists");
            List<Document> allPlaylists = collection.find().into(new ArrayList<>());

            for (Document playlist : allPlaylists) {
                if (!playlist.containsKey("createdBy")) {
                    playlist.put("createdBy", "admin");
                    collection.replaceOne(new Document("_id", playlist.get("_id")), playlist);
                }
            }
            System.out.println("Migration completed for playlists.");
        } catch (Exception e) {
            System.err.println("Error during playlist migration: " + e.getMessage());
        }
    }

    // Method to insert sample songs if the collection is empty
    public void insertSampleSongsIfEmpty() {
        try {
            MongoCollection<Document> collection = database.getCollection("songs");
            long count = collection.countDocuments();
            if (count == 0) {
                List<Document> sampleSongs = new ArrayList<>();
                sampleSongs.add(new Document("title", "Bohemian Rhapsody").append("artist", "Queen").append("album", "A Night at the Opera").append("duration", "5:55").append("genre", List.of("Rock")).append("play_count", 0).append("imagePath", "/Albums/ANightattheOpera.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Stairway to Heaven").append("artist", "Led Zeppelin").append("album", "Led Zeppelin IV").append("duration", "8:02").append("genre", List.of("Rock")).append("play_count", 0).append("imagePath", "/Albums/LedZeppelinIV.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Hotel California").append("artist", "Eagles").append("album", "Hotel California").append("duration", "6:30").append("genre", List.of("Rock")).append("play_count", 0).append("imagePath", "/Albums/HotelCalifornia.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Imagine").append("artist", "John Lennon").append("album", "Imagine").append("duration", "3:03").append("genre", List.of("Pop")).append("play_count", 0).append("imagePath", "/Albums/Imagine.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Billie Jean").append("artist", "Michael Jackson").append("album", "Thriller").append("duration", "4:54").append("genre", List.of("Pop")).append("play_count", 0).append("imagePath", "/Albums/Thriller.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Smells Like Teen Spirit").append("artist", "Nirvana").append("album", "Nevermind").append("duration", "5:01").append("genre", List.of("Grunge", "Rock")).append("play_count", 0).append("imagePath", "/Albums/Nevermind.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Wonderwall").append("artist", "Oasis").append("album", "(What's the Story) Morning Glory?").append("duration", "4:18").append("genre", List.of("Britpop", "Rock")).append("play_count", 0).append("imagePath", "/Albums/WhatsTheStoryMorningGlory.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Hey Jude").append("artist", "The Beatles").append("album", "Hey Jude").append("duration", "7:11").append("genre", List.of("Rock")).append("play_count", 0).append("imagePath", "/Albums/HeyJude.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Let It Be").append("artist", "The Beatles").append("album", "Let It Be").append("duration", "4:03").append("genre", List.of("Rock")).append("play_count", 0).append("imagePath", "/Albums/LetItBe.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Yesterday").append("artist", "The Beatles").append("album", "Help!").append("duration", "2:05").append("genre", List.of("Rock")).append("play_count", 0).append("imagePath", "/Albums/Help.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Shape of You").append("artist", "Ed Sheeran").append("album", "÷ (Divide)").append("duration", "3:53").append("genre", List.of("Pop")).append("play_count", 0).append("imagePath", "/Albums/Divide.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Blinding Lights").append("artist", "The Weeknd").append("album", "After Hours").append("duration", "3:20").append("genre", List.of("Pop", "R&B")).append("play_count", 0).append("imagePath", "/Albums/AfterHours.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Watermelon Sugar").append("artist", "Harry Styles").append("album", "Fine Line").append("duration", "2:54").append("genre", List.of("Pop")).append("play_count", 0).append("imagePath", "/Albums/FineLine.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Levitating").append("artist", "Dua Lipa").append("album", "Future Nostalgia").append("duration", "3:23").append("genre", List.of("Pop", "Dance")).append("play_count", 0).append("imagePath", "/Albums/FutureNostalgia.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Good 4 U").append("artist", "Olivia Rodrigo").append("album", "SOUR").append("duration", "2:58").append("genre", List.of("Pop", "Rock")).append("play_count", 0).append("imagePath", "/Albums/SOUR.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Drivers License").append("artist", "Olivia Rodrigo").append("album", "SOUR").append("duration", "4:02").append("genre", List.of("Pop")).append("play_count", 0).append("imagePath", "/Albums/SOUR.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "As It Was").append("artist", "Harry Styles").append("album", "Harry's House").append("duration", "2:47").append("genre", List.of("Pop")).append("play_count", 0).append("imagePath", "/Albums/HarrysHouse.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Stay").append("artist", "The Kid Laroi & Justin Bieber").append("album", "F*CK LOVE 3: OVER YOU").append("duration", "2:21").append("genre", List.of("Hip-Hop", "Pop")).append("play_count", 0).append("imagePath", "/Albums/FUCKLOVE3OVERYOU.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Peaches").append("artist", "Justin Bieber ft. Daniel Caesar & Giveon").append("album", "Justice").append("duration", "3:18").append("genre", List.of("Pop", "R&B")).append("play_count", 0).append("imagePath", "/Albums/Justice.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Montero (Call Me By Your Name)").append("artist", "Lil Nas X").append("album", "Montero").append("duration", "2:39").append("genre", List.of("Hip-Hop", "Pop")).append("play_count", 0).append("imagePath", "/Albums/Montero.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Industry Baby").append("artist", "Lil Nas X & Jack Harlow").append("album", "Montero").append("duration", "3:32").append("genre", List.of("Hip-Hop")).append("play_count", 0).append("imagePath", "/Albums/Montero.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Permission to Dance").append("artist", "BTS").append("album", "Butter / Permission to Dance").append("duration", "3:07").append("genre", List.of("K-Pop")).append("play_count", 0).append("imagePath", "/Albums/ButterPermissiontoDance.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));
                sampleSongs.add(new Document("title", "Butter").append("artist", "BTS").append("album", "Butter").append("duration", "2:44").append("genre", List.of("K-Pop")).append("play_count", 0).append("imagePath", "/Albums/Butter.jpg").append("globalPlayCount", 0).append("userPlayCounts", new Document()));

                collection.insertMany(sampleSongs);
                System.out.println("Sample songs inserted into the database.");
            }
        } catch (Exception e) {
            System.err.println("Error inserting sample songs: " + e.getMessage());
        }
    }

    // ===== LIKED PLAYLISTS METHODS =====

    // Method to add a playlist to user's liked playlists
    public void addToLikedPlaylists(String playlistTitle, String username) {
        if (likedPlaylistsCollection.find(new Document("username", username).append("playlistTitle", playlistTitle)).first() == null) {
            Document liked = new Document("username", username).append("playlistTitle", playlistTitle);
            likedPlaylistsCollection.insertOne(liked);
        }
    }

    // Method to get liked playlist titles for a user
    public List<String> getLikedPlaylistTitles(String username) {
        List<String> titles = new ArrayList<>();
        for (Document doc : likedPlaylistsCollection.find(new Document("username", username))) {
            titles.add(doc.getString("playlistTitle"));
        }
        return titles;
    }

    // Method to remove a playlist from user's liked playlists
    public void removeFromLikedPlaylists(String playlistTitle, String username) {
        likedPlaylistsCollection.deleteOne(new Document("username", username).append("playlistTitle", playlistTitle));
    }
}
