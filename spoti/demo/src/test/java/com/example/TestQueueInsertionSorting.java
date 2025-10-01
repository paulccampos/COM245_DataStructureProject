package com.example;

import org.bson.Document;

import com.example.models.Queue;
import com.example.models.Song;

public class TestQueueInsertionSorting {
    public static void main(String[] args) {
        System.out.println("=== Testing Queue Insertion and Sorting ===\n");

        // Create test songs with different globalPlayCounts
        Song song1 = new Song(new Document("title", "Low Play Song").append("artist", "Artist A").append("globalPlayCount", 10));
        Song song2 = new Song(new Document("title", "High Play Song").append("artist", "Artist B").append("globalPlayCount", 100));
        Song song3 = new Song(new Document("title", "Medium Play Song").append("artist", "Artist C").append("globalPlayCount", 50));
        Song song4 = new Song(new Document("title", "Manual Song 1").append("artist", "Artist D").append("globalPlayCount", 20));
        Song song5 = new Song(new Document("title", "Manual Song 2").append("artist", "Artist E").append("globalPlayCount", 30));
        Song song6 = new Song(new Document("title", "Another High Play").append("artist", "Artist F").append("globalPlayCount", 80));

        Queue queue = new Queue(null); // Empty queue

        System.out.println("1. Testing MANUAL additions:");
        queue.addSong(song4, Queue.Type.MANUAL);
        printQueue(queue, "After adding first MANUAL");

        queue.addSong(song5, Queue.Type.MANUAL);
        printQueue(queue, "After adding second MANUAL");

        System.out.println("\n2. Testing AUTO additions (should insert after MANUALs and sort by play count desc):");
        queue.addSong(song1, Queue.Type.AUTO); // Low play
        printQueue(queue, "After adding AUTO low play");

        queue.addSong(song2, Queue.Type.AUTO); // High play
        printQueue(queue, "After adding AUTO high play");

        queue.addSong(song3, Queue.Type.AUTO); // Medium play
        printQueue(queue, "After adding AUTO medium play");

        queue.addSong(song6, Queue.Type.AUTO); // Another high play
        printQueue(queue, "After adding another AUTO high play");

        System.out.println("\n3. Testing more MANUAL additions:");
        Song song7 = new Song(new Document("title", "Late Manual").append("artist", "Artist G").append("globalPlayCount", 5));
        queue.addSong(song7, Queue.Type.MANUAL);
        printQueue(queue, "After adding late MANUAL");

        System.out.println("\n4. Testing removal (next song):");
        Song removed = queue.removeSong(0);
        System.out.println("Removed: " + (removed != null ? removed.getTitle() : "null"));
        printQueue(queue, "After removing first song");

        System.out.println("\n5. Testing empty queue:");
        Queue emptyQueue = new Queue(null);
        emptyQueue.addSong(song1, Queue.Type.AUTO);
        printQueue(emptyQueue, "Empty queue + AUTO");

        emptyQueue.addSong(song4, Queue.Type.MANUAL);
        printQueue(emptyQueue, "After adding MANUAL to empty");

        System.out.println("\n6. Testing all MANUAL queue:");
        Queue manualQueue = new Queue(null);
        manualQueue.addSong(song4, Queue.Type.MANUAL);
        manualQueue.addSong(song5, Queue.Type.MANUAL);
        manualQueue.addSong(song7, Queue.Type.MANUAL);
        printQueue(manualQueue, "All MANUAL");

        System.out.println("\n7. Testing duplicates:");
        queue.addSong(song2, Queue.Type.AUTO); // Duplicate high play
        printQueue(queue, "After adding duplicate AUTO");

        System.out.println("\n=== Testing Complete ===");
    }

    private static void printQueue(Queue queue, String label) {
        System.out.println(label + ":");
        for (int i = 0; i < queue.size(); i++) {
            Song s = queue.get(i);
            Queue.Type t = queue.getType(i);
            System.out.println("  [" + i + "] " + s.getTitle() + " (Play: " + s.getGlobalPlayCount() + ", Type: " + t + ")");
        }
        System.out.println();
    }
}
