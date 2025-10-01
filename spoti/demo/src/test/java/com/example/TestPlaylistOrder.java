package com.example;

import org.bson.Document;
import java.util.List;
import java.util.Arrays;

public class TestPlaylistOrder {
    public static void main(String[] args) {
        MongoService mongoService = new MongoService();
        
        // Clean up test data if exists
        mongoService.deletePlaylist("TestUserPlaylist", "testuser");
        mongoService.deletePlaylist("TestSystemPlaylist");
        
        System.out.println("=== Testing Add to Empty User Playlist ===");
        // Create empty user playlist
        mongoService.createPlaylist("TestUserPlaylist", "Test desc", "testuser");
        
        // Add songs
        Document song1 = new Document("title", "Song A").append("artist", "Artist A");
        Document song2 = new Document("title", "Song B").append("artist", "Artist B");
        Document song3 = new Document("title", "Song C").append("artist", "Artist C");
        
        mongoService.addSongToPlaylist("TestUserPlaylist", song1, "testuser");
        mongoService.addSongToPlaylist("TestUserPlaylist", song2, "testuser");
        mongoService.addSongToPlaylist("TestUserPlaylist", song3, "testuser");
        
        // Retrieve and print order
        List<Document> songs = mongoService.getSongsForPlaylist("TestUserPlaylist", "testuser");
        System.out.println("Songs order: ");
        for (Document s : songs) {
            System.out.println(s.getString("title") + " - " + s.getString("artist"));
        }
        // Expected: Song C, Song B, Song A (added at beginning each time)
        
        System.out.println("=== Testing Add to Non-Empty User Playlist ===");
        // Add another song to existing
        Document song4 = new Document("title", "Song D").append("artist", "Artist D");
        mongoService.addSongToPlaylist("TestUserPlaylist", song4, "testuser");
        
        songs = mongoService.getSongsForPlaylist("TestUserPlaylist", "testuser");
        System.out.println("After adding Song D: ");
        for (Document s : songs) {
            System.out.println(s.getString("title") + " - " + s.getString("artist"));
        }
        // Expected: Song D, Song C, Song B, Song A
        
        System.out.println("=== Testing System Playlist (Top Hits) ===");
        // Initialize or use Top Hits
        mongoService.initializeTopHitsPlaylist();
        
        // Add manual song to Top Hits (system, no username)
        Document manualSong = new Document("title", "Manual Song").append("artist", "Manual Artist");
        mongoService.addSongToPlaylist("Top Hits", manualSong);
        
        List<Document> topSongs = mongoService.getSongsForPlaylist("Top Hits", "admin"); // Assuming admin access
        System.out.println("Top Hits after manual add (first few): ");
        for (int i = 0; i < Math.min(5, topSongs.size()); i++) {
            Document s = topSongs.get(i);
            System.out.println(s.getString("title") + " - " + s.getString("artist"));
        }
        // Expected: Manual Song at top, then original songs
        
        System.out.println("=== Testing Edge Cases ===");
        // Add duplicate
        mongoService.addSongToPlaylist("TestUserPlaylist", song1, "testuser"); // Duplicate Song A
        songs = mongoService.getSongsForPlaylist("TestUserPlaylist", "testuser");
        System.out.println("After adding duplicate Song A (count should be 2, order: Song A at top): ");
        int songACount = 0;
        for (Document s : songs) {
            if (s.getString("title").equals("Song A")) songACount++;
            System.out.println(s.getString("title") + " - " + s.getString("artist"));
        }
        System.out.println("Song A count: " + songACount); // Expected: 2
        
        // Non-existent playlist (should do nothing)
        mongoService.addSongToPlaylist("NonExistent", song2, "testuser");
        System.out.println("Non-existent add: No error expected.");
        
        // Verify no corruption: Count total songs
        System.out.println("TestUserPlaylist total songs: " + songs.size()); // Expected: 5 (D,C,B,A,A)
        
        // Clean up
        mongoService.deletePlaylist("TestUserPlaylist", "testuser");
        // Note: Don't delete Top Hits to preserve
        
        mongoService.close();
        System.out.println("=== Testing Complete ===");
    }
}
