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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class searchcontroller {

    @FXML
    private HBox mediaPlayerBar;

    @FXML
    private Label currentSongLabel;

    @FXML
    private Button previousButton;

    @FXML
    private Button playPauseButton;

    @FXML
    private Button nextButton;

    @FXML
    private Button viewQueueButton;

    @FXML
    private Label durationLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private TextField searchTextField;

    @FXML
    private ListView<String> resultsListView;

    private MongoService mongoService;

    @FXML
    private void initialize() {
        mongoService = new MongoService();

        // Register UI components with MediaPlayerHandler
        MediaPlayerHandler.getInstance().registerUIComponents(
            mediaPlayerBar, currentSongLabel, durationLabel, progressBar,
            previousButton, playPauseButton, nextButton);

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

    @FXML
    private void onPrevious() {
        App.previous();
    }

    @FXML
    private void onPlayPause() {
        App.togglePlayPause();
    }

    @FXML
    private void onNext() {
        App.next();
    }

    @FXML
    private void onHome() throws IOException {
        App.setRoot("home");
    }

    @FXML
    private void onSearch() throws IOException {
        App.setRoot("search");
    }

    @FXML
    private void onLibrary() throws IOException {
        App.setRoot("library");
    }

    @FXML
    private void onViewQueue() throws IOException {
        App.setRoot("queue");
    }
}
