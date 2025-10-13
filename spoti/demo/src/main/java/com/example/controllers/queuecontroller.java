package com.example.controllers;

import java.util.List;
import java.io.IOException;

import com.example.App;
import com.example.models.Song;
import com.example.services.MediaPlayerService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class queuecontroller {

    private MediaPlayerService mediaPlayerService;

    @FXML
    private VBox queueVBox;

    @FXML
    private Label currentSongLabel;

    @FXML
    private ImageView selectedSongImage;

    private static String previousView;

    public queuecontroller() {
        this.mediaPlayerService = App.getMediaPlayerServiceStatic();
    }

    @FXML
    private void initialize() {
        if (previousView == null) {
            previousView = "home";
        }
        loadQueue();
        MediaPlayerHandler.getInstance().registerQueueController(this);
    }

    private void loadQueue() {
        queueVBox.getChildren().clear();

        Song currentSong = mediaPlayerService.getCurrentSong();
        if (currentSong != null) {
            currentSongLabel.setText("Now Playing: " + currentSong.getTitle() + " - " + currentSong.getArtist());

            // Set the selected song image
            if (selectedSongImage != null) {
                String imagePath = currentSong.getImagePath();
                if (imagePath == null) {
                    imagePath = "/com/example/images/vector-picture-icon.jpg";
                }
                try {
                    java.net.URL url = getClass().getResource(imagePath);
                    if (url != null) {
                        selectedSongImage.setImage(new javafx.scene.image.Image(url.toExternalForm()));
                    } else {
                        System.out.println("Image resource not found: " + imagePath);
                    }
                } catch (Exception e) {
                    System.err.println("Exception while loading image: " + imagePath);
                    e.printStackTrace();
                }
            }
        } else {
            currentSongLabel.setText("No song playing");
            if (selectedSongImage != null) {
                selectedSongImage.setImage(null);
            }
        }

        com.example.models.Queue queueModel = mediaPlayerService.getQueue();
        List<Song> queueSongs = queueModel.getSongs();
        
        if (queueSongs.isEmpty()) {
            queueVBox.getChildren().add(new Label("The queue is empty."));
        }
        
        for (int i = 0; i < queueSongs.size(); i++) {
            final int idx = i;
            Song song = queueSongs.get(idx);
            HBox hbox = new HBox(10);
            hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            ImageView albumArt = new ImageView();
            albumArt.setFitHeight(40);
            albumArt.setFitWidth(40);
            String imagePath = song.getImagePath();
            if (imagePath == null) {
                imagePath = "/com/example/images/vector-picture-icon.jpg";
            }
            try {
                java.net.URL url = getClass().getResource(imagePath);
                if (url != null) {
                    albumArt.setImage(new javafx.scene.image.Image(url.toExternalForm()));
                } else {
                    System.out.println("Image resource not found: " + imagePath);
                }
            } catch (Exception e) {
                System.err.println("Exception while loading image: " + imagePath);
                e.printStackTrace();
            }

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

            hbox.getChildren().addAll(albumArt, songLabel, moveUpButton, moveDownButton, deleteButton);
            queueVBox.getChildren().add(hbox);
        }
    }

    private void deleteFromQueue(Song song) {
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
            queueModel.removeSong(index);

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

    @FXML
    private void onBack() throws IOException {
        App.loadViewInMain(previousView != null ? previousView : "home");
    }

    public void refreshQueue() {
        javafx.application.Platform.runLater(this::loadQueue);
    }

}

