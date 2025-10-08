package com.example.controllers;

import java.util.List;

import com.example.App;
import com.example.MongoService;
import com.example.models.Song;
import com.example.services.MediaPlayerService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class queuecontroller {

    private MediaPlayerService mediaPlayerService;

    @FXML
    private VBox queueVBox;

    @FXML
    private Label currentSongLabel;

    private static String previousView;

    public queuecontroller() {
        // Get services from App instance (dependency injection)
        this.mediaPlayerService = App.getMediaPlayerServiceStatic();
    }

    @FXML
    private void initialize() {
        // If no previous view is set, default to home
        if (previousView == null) {
            previousView = "home";
        }
        loadQueue();

    }

    private void loadQueue() {
        queueVBox.getChildren().clear();

        // Display current song using MediaPlayerService
        Song currentSong = mediaPlayerService.getCurrentSong();
        if (currentSong != null) {
            currentSongLabel.setText("Now Playing: " + currentSong.getTitle() + " - " + currentSong.getArtist());
        } else {
            currentSongLabel.setText("No song playing");
        }

        // Display queue using MediaPlayerService
        com.example.models.Queue queueModel = mediaPlayerService.getQueue();
        List<Song> queueSongs = queueModel.getSongs();
        for (int i = 0; i < queueSongs.size(); i++) {
            final int idx = i;
            Song song = queueSongs.get(idx);
            HBox hbox = new HBox(10);

            // Create song label with manual addition indicator
            String songText = (idx + 1) + ". " + song.getTitle() + " - " + song.getArtist();
            if (song.isManualAddition()) {
                songText += " [MANUAL]";
            }
            Label songLabel = new Label(songText);

            Button deleteButton = new Button("Delete");
            deleteButton.setOnAction(e -> deleteFromQueue(song));

            Button moveUpButton = new Button("↑");
            if (idx > 0) {
                moveUpButton.setOnAction(e -> moveUp(idx));
            } else {
                moveUpButton.setDisable(true);
            }

            Button moveDownButton = new Button("↓");
            if (idx < queueSongs.size() - 1) {
                moveDownButton.setOnAction(e -> moveDown(idx));
            } else {
                moveDownButton.setDisable(true);
            }

            hbox.getChildren().addAll(songLabel, moveUpButton, moveDownButton, deleteButton);
            queueVBox.getChildren().add(hbox);
        }
    }

    private void deleteFromQueue(Song song) {
        // Find the index of the song in the queue
        com.example.models.Queue queueModel = mediaPlayerService.getQueue();
        int index = -1;
        for (int i = 0; i < queueModel.size(); i++) {
            Song queueSong = queueModel.get(i);
            if (queueSong.getTitle().equals(song.getTitle()) &&
                queueSong.getArtist().equals(song.getArtist())) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            // Remove from MediaPlayerService queue
            queueModel.removeSong(index);

            // Remove from MongoDB
            mediaPlayerService.removeSongFromQueue(song);
        }
        loadQueue();
    }

    private void moveUp(int index) {
        mediaPlayerService.moveUp(index);
        loadQueue();
    }

    private void moveDown(int index) {
        mediaPlayerService.moveDown(index);
        loadQueue();
    }

    public static void setPreviousView(String viewName) {
        previousView = viewName;
    }

}
