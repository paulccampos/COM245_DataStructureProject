package com.example;

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
            String name = doc.getString("name");
            String artist = doc.getString("artist");
            String album = doc.getString("album");
            System.out.println("Song: " + name + " | Artist: " + artist + " | Album: " + album);
        }

        System.out.println("========================");
    }

    public void close() {
        mongoClient.close();
    }
}
