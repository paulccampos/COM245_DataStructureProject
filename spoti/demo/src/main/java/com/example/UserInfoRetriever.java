package com.example;

import org.bson.Document;

import java.util.List;


public class UserInfoRetriever {

    public static void main(String[] args) {
        MongoService mongoService = new MongoService();

        System.out.println("=== Retrieving User Information from Database ===");

        List<Document> users = mongoService.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("No users found in the database.");
        } else {
            System.out.println("Found " + users.size() + " user(s):");
            System.out.println("--------------------------------------------------");

            for (Document user : users) {
                System.out.println("Username: " + user.getString("username"));
                System.out.println("Email: " + user.getString("email"));
                System.out.println("Status: " + user.getString("status"));
                System.out.println("Created Date: " + user.get("created_date"));
                System.out.println("--------------------------------------------------");
            }
        }

        mongoService.close();
    }
}
