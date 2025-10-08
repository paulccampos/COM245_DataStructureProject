package com.example.controllers;

import java.io.IOException;
import java.util.List;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;
import com.example.handlers.MediaPlayerHandler;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class searchcontroller {

    @FXML
    private TextField searchTextField;

    @FXML
    private ListView<String> resultsListView;

    private MongoService mongoService;

    @FXML
    private void initialize() {
        mongoService = new MongoService();

        // Add listener to search text field
        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            performSearch(newValue);
        });

        // Initialize results list view click handler
        resultsListView.setOnMouseClicked(event -> {
            String selected = resultsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                playSelectedSong(selected);
            }
        });
    }

    private void performSearch(String query) {
        if (query == null || query.isEmpty()) {
            resultsListView.setItems(FXCollections.observableArrayList());
            return;
        }
        List<Document> results = mongoService.searchSongs(query);
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Document doc : results) {
            String display = doc.getString("title") + " - " + doc.getString("artist");
            items.add(display);
        }
        resultsListView.setItems(items);
    }

    private void playSelectedSong(String selected) {
        // Parse title and artist from selected string
        String[] parts = selected.split(" - ", 2);
        if (parts.length < 2) {
            return;
        }
        String title = parts[0];
        String artist = parts[1];

        // Find the song document from MongoDB
        List<Document> allSongs = mongoService.getAllSongs();
        for (Document song : allSongs) {
            if (title.equals(song.getString("title")) && artist.equals(song.getString("artist"))) {
                App.playSong(song);
                break;
            }
        }
    }
}
