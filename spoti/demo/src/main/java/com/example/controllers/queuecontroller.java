package com.example.controllers;

import java.io.IOException;
import java.util.List;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class queuecontroller {

    private MongoService mongoService;

    @FXML
    private VBox queueVBox;

    @FXML
    private Label currentSongLabel;

    public queuecontroller() {
        mongoService = new MongoService();
    }

    @FXML
    private void initialize() {
        loadQueue();
    }

    private void loadQueue() {
        queueVBox.getChildren().clear();

        // Display current song
        Document currentSong = App.getCurrentSong();
        if (currentSong != null) {
            currentSongLabel.setText("Now Playing: " + currentSong.getString("title") + " - " + currentSong.getString("artist"));
        } else {
            currentSongLabel.setText("No song playing");
        }

        // Display queue
        List<Document> queue = App.getQueue();
        for (int i = 0; i < queue.size(); i++) {
            final int idx = i;
            Document song = queue.get(idx);
            HBox hbox = new HBox(10);
            Label songLabel = new Label((idx + 1) + ". " + song.getString("title") + " - " + song.getString("artist"));

            Button deleteButton = new Button("Delete");
            deleteButton.setOnAction(e -> deleteFromQueue(song));

            Button moveUpButton = new Button("↑");
            if (idx > 0) {
                moveUpButton.setOnAction(e -> moveUp(idx));
            } else {
                moveUpButton.setDisable(true);
            }

            Button moveDownButton = new Button("↓");
            if (idx < queue.size() - 1) {
                moveDownButton.setOnAction(e -> moveDown(idx));
            } else {
                moveDownButton.setDisable(true);
            }

            hbox.getChildren().addAll(songLabel, moveUpButton, moveDownButton, deleteButton);
            queueVBox.getChildren().add(hbox);
        }
    }

    private void deleteFromQueue(Document song) {
        App.getQueue().remove(song);
        mongoService.removeFromQueue(song);
        loadQueue();
    }

    private void moveUp(int index) {
        List<Document> queue = App.getQueue();
        if (index > 0) {
            Document temp = queue.get(index);
            queue.set(index, queue.get(index - 1));
            queue.set(index - 1, temp);
            // Update DB by clearing and re-adding
            mongoService.clearQueue();
            for (Document s : queue) {
                mongoService.addToQueue(s);
            }
            loadQueue();
        }
    }

    private void moveDown(int index) {
        List<Document> queue = App.getQueue();
        if (index < queue.size() - 1) {
            Document temp = queue.get(index);
            queue.set(index, queue.get(index + 1));
            queue.set(index + 1, temp);
            // Update DB
            mongoService.clearQueue();
            for (Document s : queue) {
                mongoService.addToQueue(s);
            }
            loadQueue();
        }
    }

    @FXML
    private void onBack() throws IOException {
        String playlistName = App.getCurrentPlaylistName();
        if (playlistName != null) {
            App.setRootWithPlaylist("playlist", playlistName, "queue");
        } else {
            App.setRoot("home");
        }
    }
}
