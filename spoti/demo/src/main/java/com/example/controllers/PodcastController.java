package com.example.controllers;

import java.io.IOException;
import java.util.List;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PodcastController {

    private MongoService mongoService;

    @FXML
    private Label emptyStateLabel;

    @FXML
    private VBox podcastsListVBox;

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

    public PodcastController() {
        mongoService = new App().getMongoService();
    }

    @FXML
    private void initialize() {
        List<Document> podcasts = mongoService.getPodcasts(App.getCurrentUsername());
        podcastsListVBox.getChildren().clear();
        if (podcasts.isEmpty()) {
            emptyStateLabel.setVisible(true);
        } else {
            emptyStateLabel.setVisible(false);
            for (Document podcast : podcasts) {
                HBox podcastBox = createPodcastBox(podcast);
                podcastsListVBox.getChildren().add(podcastBox);
            }
        }
        // Register media player
        com.example.handlers.MediaPlayerHandler.getInstance().registerUIComponents(
            mediaPlayerBar, currentSongLabel, durationLabel, progressBar,
            previousButton, playPauseButton, nextButton);
    }

    private HBox createPodcastBox(Document podcast) {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPrefHeight(80.0);
        hbox.setPrefWidth(600.0);
        hbox.setSpacing(15.0);
        hbox.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 10; -fx-padding: 15;");

        ImageView iv = new ImageView();
        iv.setFitHeight(60.0);
        iv.setFitWidth(60.0);
        iv.setPreserveRatio(true);
        String imagePath = podcast.getString("imagePath");
        if (imagePath == null) imagePath = "/com/example/images/vector-picture-icon.jpg";
        java.net.URL url = getClass().getResource(imagePath);
        if (url != null) {
            iv.setImage(new Image(url.toExternalForm()));
        } else {
            // Default placeholder if image not found
            iv.setImage(null);
        }

        VBox vbox = new VBox();
        vbox.setPrefWidth(400.0);
        Label titleLabel = new Label(podcast.getString("title"));
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label descLabel = new Label(podcast.getString("description"));
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        Label durationLabel = new Label("Duration: " + podcast.getString("duration"));
        durationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");
        vbox.getChildren().addAll(titleLabel, descLabel, durationLabel);

        // Favorite button
        Button favoriteButton = new Button();
        favoriteButton.setPrefSize(30, 30);
        favoriteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff0000; -fx-font-size: 16px;");

        // Check if podcast is in favorites
        List<Document> favorites = mongoService.getUserFavoritePodcasts(App.getCurrentUsername());
        boolean isFavorite = favorites.stream().anyMatch(f -> f.getString("title").equals(podcast.getString("title")));
        favoriteButton.setText(isFavorite ? "♥" : "♡");

        favoriteButton.setOnAction(e -> {
            if (isFavorite) {
                mongoService.removePodcastFromFavorites(App.getCurrentUsername(), podcast);
                favoriteButton.setText("♡");
            } else {
                mongoService.addPodcastToFavorites(App.getCurrentUsername(), podcast);
                favoriteButton.setText("♥");
            }
            // Refresh the list
            initialize();
        });

        hbox.getChildren().addAll(iv, vbox, favoriteButton);

        // On click, add to queue or play
        hbox.setOnMouseClicked(e -> {
            // Assuming podcast has mp3 path or something, but for now, just add a placeholder
            // Since no mp3, perhaps just show message or add to queue if has path
            System.out.println("Podcast clicked: " + podcast.getString("title"));
            // For now, do nothing or add to queue if has filePath
        });

        return hbox;
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
    private void onSongs() throws IOException {
        App.setRoot("songs");
    }

    @FXML
    private void onProfile() throws IOException {
        App.setRoot("userprofile");
    }

    @FXML
    private void onPodcasts() throws IOException {
        App.setRoot("podcasts");
    }

    @FXML
    private void onPrevious() {
        App.getMediaPlayerServiceStatic().previous();
        com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
    }

    @FXML
    private void onPlayPause() {
        App.getMediaPlayerServiceStatic().togglePlayPause();
        com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
    }

    @FXML
    private void onNext() {
        App.getMediaPlayerServiceStatic().next();
        com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
    }

    @FXML
    private void onClearQueue() {
        App.getMediaPlayerServiceStatic().clearQueue();
        com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
    }

    @FXML
    private void onViewQueue() throws IOException {
        App.setRoot("queue");
    }
}
