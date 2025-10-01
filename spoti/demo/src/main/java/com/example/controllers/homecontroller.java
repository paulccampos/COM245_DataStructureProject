package com.example.controllers;

import java.io.IOException;
import java.util.List;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;
import com.example.handlers.MediaPlayerHandler;
import com.example.models.Song;
import com.example.services.MediaPlayerService;
import com.example.services.UserService;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class homecontroller {

    private MongoService mongoService;
    private MediaPlayerService mediaPlayerService;
    private UserService userService;

    @FXML
    private HBox playlistsVBox;

    @FXML
    private HBox hitsVBox;

    @FXML
    private Label welcomeLabel;

    @FXML
    private javafx.scene.layout.HBox mediaPlayerBar;

    @FXML
    private javafx.scene.control.Label currentSongLabel;

    @FXML
    private javafx.scene.control.Button previousButton;

    @FXML
    private javafx.scene.control.Button playPauseButton;

    @FXML
    private javafx.scene.control.Button nextButton;

    @FXML
    private javafx.scene.control.Label durationLabel;

    @FXML
    private javafx.scene.control.ProgressBar progressBar;

    @FXML
    private javafx.scene.control.Button clearQueueButton;



    public homecontroller() {
        // Use singleton services from App
        App app = new App();
        mongoService = app.getMongoService();
        mediaPlayerService = app.getMediaPlayerService();
        userService = app.getUserService();
    }

    @FXML
    private void initialize() {
        mongoService.initializeTopHitsPlaylist();
        loadPlaylists();
        loadTopHits();
        welcomeLabel.setText("Welcome, " + App.getCurrentUsername() + "!");
        // Register UI components with MediaPlayerHandler
        MediaPlayerHandler.getInstance().registerUIComponents(
            mediaPlayerBar, currentSongLabel, durationLabel, progressBar,
            previousButton, playPauseButton, nextButton);
    }

    private void loadPlaylists() {
        List<Document> playlists = mongoService.getAllPlaylists();
        playlistsVBox.getChildren().clear();
        for (Document playlist : playlists) {
            VBox playlistBox = createPlaylistBox(playlist);
            playlistsVBox.getChildren().add(playlistBox);
        }
        // Add add playlist box
        VBox addBox = createAddPlaylistBox();
        playlistsVBox.getChildren().add(addBox);
    }

    private void loadTopHits() {
        List<Document> topSongsDoc = mongoService.getTopSongsForUser(App.getCurrentUsername(), 5);
        List<Song> topSongs = topSongsDoc.stream().map(Song::new).toList();
        hitsVBox.getChildren().clear();
        if (topSongs.isEmpty()) {
            Label noHitsLabel = new Label("No top hits yet.");
            noHitsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
            hitsVBox.getChildren().add(noHitsLabel);
            return;
        }
        for (Song song : topSongs) {
            VBox songBox = createSongBox(song);
            hitsVBox.getChildren().add(songBox);
        }
    }

    private VBox createSongBox(Song song) {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setPrefHeight(200);
        box.setPrefWidth(180);
        box.setSpacing(15);
        box.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 15; -fx-padding: 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");

        ImageView iv = new ImageView();
        iv.setFitHeight(120);
        iv.setFitWidth(120);
        iv.setPreserveRatio(true);
        iv.setStyle("-fx-background-radius: 10;");
        String imagePath = song.getImagePath();
        if (imagePath == null) imagePath = "/com/example/images/vector-picture-icon.jpg";
        java.net.URL url = getClass().getResource(imagePath);
        if (url != null) {
            iv.setImage(new Image(url.toExternalForm()));
        } else {
            // Default placeholder if image not found
            iv.setImage(null);
        }

        Label titleLabel = new Label(song.getTitle());
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label artistLabel = new Label(song.getArtist());
        artistLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #ccc;");

        box.getChildren().addAll(iv, titleLabel, artistLabel);

        box.setOnMouseClicked(e -> {
            mediaPlayerService.addToQueue(song);
            MediaPlayerHandler.getInstance().updateMediaPlayer();
        });

        return box;
    }

    private VBox createPlaylistBox(Document playlist) {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setPrefHeight(200);
        box.setPrefWidth(180);
        box.setSpacing(15);
        box.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 15; -fx-padding: 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");

        ImageView iv = new ImageView();
        iv.setFitHeight(120);
        iv.setFitWidth(120);
        iv.setPreserveRatio(true);
        iv.setStyle("-fx-background-radius: 10;");
        String imagePath = playlist.getString("imagePath");
        if (imagePath == null) imagePath = "/com/example/images/vector-picture-icon.jpg";
        java.net.URL url = getClass().getResource(imagePath);
        if (url != null) {
            iv.setImage(new Image(url.toExternalForm()));
        } else {
            // Default placeholder if image not found
            iv.setImage(null);
        }

        Label label = new Label(playlist.getString("title"));
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");

        box.getChildren().addAll(iv, label);

        box.setOnMouseClicked(e -> {
            try {
                navigateToPlaylist(playlist.getString("title"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        return box;
    }

    private VBox createAddPlaylistBox() {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setPrefHeight(200);
        box.setPrefWidth(180);
        box.setSpacing(15);
        box.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 15; -fx-padding: 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");

        Label plus = new Label("+");
        plus.setStyle("-fx-font-size: 60px; -fx-text-fill: white;");

        Label label = new Label("Add Playlist");
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");

        box.getChildren().addAll(plus, label);

        box.setOnMouseClicked(e -> {
            try {
                onAddPlaylist();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        return box;
    }

    private void onAddPlaylist() throws IOException {
        App.setRoot("library");
    }

    private void navigateToPlaylist(String playlistName) throws IOException {
        // Set the pending playlist data before navigating
        com.example.controllers.playlistcontroller.setPendingPlaylist(playlistName, "home");
        App.setRoot("playlist");
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
        mediaPlayerService.previous();
        MediaPlayerHandler.getInstance().updateMediaPlayer();
    }

    @FXML
    private void onPlayPause() {
        mediaPlayerService.togglePlayPause();
        MediaPlayerHandler.getInstance().updateMediaPlayer();
    }

    @FXML
    private void onNext() {
        mediaPlayerService.next();
        MediaPlayerHandler.getInstance().updateMediaPlayer();
    }

    @FXML
    private void onViewQueue() throws IOException {
        App.setRoot("queue");
    }

    @FXML
    private void onClearQueue() {
        mediaPlayerService.clearQueue();
        MediaPlayerHandler.getInstance().updateMediaPlayer();
    }
}
